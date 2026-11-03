"""Every table of the live report database, as one Excel workbook.

    python -X utf8 tools/reports_xlsx.py                        # tools/out/reports/perankh-reports-DATE.xlsx
    python -X utf8 tools/reports_xlsx.py --out reports.xlsx
    python -X utf8 tools/reports_xlsx.py --snapshot saved.json  # from a snapshot taken earlier

The database is read on the server by tools/reports_snapshot.py, sent over SSH with the login in
PERANKH_HOST or server/.host, as for tools/reports.sh: opened read-only and read in one
transaction, so every sheet is from the same moment. Nothing is written there, and nothing has to
be deployed.

The sheets: one per table, every column in table order, each millisecond timestamp followed by a
formula giving it as a date and time in UTC; "context (expanded)", each report's context JSON one
key and value per row; "fields", every column with its type, constraints and meaning; "schema",
the CREATE statements; "about", where it all came from and the reports by status and by app.

The file is written with the standard library, each formula with its result filled in, so making
it needs no Excel, LibreOffice or openpyxl, and viewers that do not calculate still show the
values. Report text arrives through a public endpoint, so it is always stored as text, never as a
formula. The workbook holds testers' reports: it stays out of the repository (tools/out/ is ignored).
"""
from __future__ import annotations

import argparse
import collections
import datetime as dt
import json
import math
import os
import re
import subprocess
import sys
import zipfile
from xml.sax.saxutils import escape, quoteattr

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SNAPSHOT = os.path.join(ROOT, "tools", "reports_snapshot.py")
OUT_DIR = os.path.join(ROOT, "tools", "out", "reports")

# Triage states in server/perankh/db.py order; OPEN are the ones still waiting on work.
STATUSES = ["new", "triaged", "in_progress", "fixed", "released", "wontfix", "duplicate", "invalid"]
OPEN = ["new", "triaged", "in_progress"]
# The apps that report here, in server/perankh/db.py order (APPS).
APPS = {"perankh": "Per Ankh", "bushlines": "BushLines"}

TABLES = {
    "reports": "one row per problem report: which app sent it, what the tester wrote, where they "
               "were in the app, and its triage state",
    "report_log": "every event on a report: received, then each status change",
    "sqlite_sequence": "SQLite’s own: the last id handed out in each AUTOINCREMENT table",
    "identify_days": "BushLines’ plant identifications asked of Pl@ntNet, counted per UTC day to stay "
                     "within its free plan",
}
# What each column holds, from server/perankh/db.py; a comment in the live CREATE TABLE wins.
MEANING = {
    ("reports", "id"): "UUID made on the phone, so a retried upload is stored once",
    ("reports", "received_at"): "When the server stored it: server clock, ms since 1970",
    ("reports", "created_at"): "When the tester saved it: phone clock, ms since 1970",
    ("reports", "app"): "Which app sent it: perankh or bushlines (rows from before schema 2 are perankh)",
    ("reports", "app_version"): "App version that sent it",
    ("reports", "android_sdk"): "Android API level of the phone",
    ("reports", "category"): "Kind of problem the tester picked",
    ("reports", "note"): "What the tester wrote (empty text if nothing)",
    ("reports", "screen"): "Copied from context, so triage can filter on it",
    ("reports", "lesson"): "Copied from context, so triage can filter on it",
    ("reports", "exercise"): "Copied from context, so triage can filter on it",
    ("reports", "item"): "Copied from context, so triage can filter on it",
    ("reports", "context"): "Everything on screen when the report was made, a JSON object",
    ("reports", "account_id"): "Reserved for signed-in reports; always NULL so far",
    ("reports", "status"): "Triage state: " + ", ".join(STATUSES),
    ("reports", "resolution"): "Note given with the latest status change",
    ("reports", "resolved_in"): "App version or commit with the fix",
    ("reports", "duplicate_of"): "Id of the report this one repeats",
    ("reports", "updated_at"): "Last change: server clock, ms since 1970",
    ("report_log", "id"): "Event number, never reused (AUTOINCREMENT)",
    ("report_log", "report_id"): "The report (reports.id); deleted with it",
    ("report_log", "at"): "When it happened: server clock, ms since 1970",
    ("report_log", "action"): "“received”, or the status the report moved to",
    ("report_log", "detail"): "Note given, “in VERSION” for a fix, “of ID” for a duplicate",
    ("sqlite_sequence", "name"): "Table that uses AUTOINCREMENT",
    ("sqlite_sequence", "seq"): "Largest id handed out in that table so far",
    ("identify_days", "day"): "UTC date, YYYY-MM-DD",
    ("identify_days", "count"): "Identifications asked of Pl@ntNet that day; the service stops at 450",
}
MS_COLUMNS = {"reports": ("received_at", "created_at", "updated_at"), "report_log": ("at",)}
WRAPPED = {"note": 50, "resolution": 50, "context": 60, "detail": 60}  # column: width

HEAD = {"bold": True, "color": "FFFFFF", "fill": "1F4E78"}                    # a database column
DERIVED = {"bold": True, "italic": True, "color": "1F4E78", "fill": "DDEBF7"}  # made for the workbook
LABEL = {"bold": True}
SECTION = {"bold": True, "size": 11}
DATETIME = "yyyy-mm-dd hh:mm:ss"


# -- the snapshot -------------------------------------------------------------------------------

def login() -> str:
    host = os.environ.get("PERANKH_HOST", "").strip()
    if not host:
        try:
            with open(os.path.join(ROOT, "server", ".host"), encoding="utf-8") as f:
                host = f.readline().strip()
        except OSError:
            pass
    if not host:
        sys.exit("set PERANKH_HOST=user@host, or put that line in server/.host")
    return host


def take_snapshot() -> dict:
    with open(SNAPSHOT, "rb") as f:
        script = f.read()
    # Only the service user can read the database, as in server/deploy/perankh-reports.
    try:
        done = subprocess.run(["ssh", login(), "sudo -n -u perankh /usr/bin/python3 -I -"],
                              input=script, capture_output=True, timeout=300)
    except FileNotFoundError:
        sys.exit("no ssh on PATH")
    except subprocess.TimeoutExpired:
        sys.exit("the server did not answer within five minutes")
    if done.returncode:
        sys.exit("the snapshot failed: " + done.stderr.decode("utf-8", "replace").strip())
    return json.loads(done.stdout)


# -- a minimal xlsx writer ----------------------------------------------------------------------

MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
DOC_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml."
XML = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
NOT_XML = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\ud800-\udfff￾￿]")  # XML 1.0 or UTF-8 cannot hold them
EXCEL_ESCAPE = re.compile(r"_x[0-9A-Fa-f]{4}_")                     # Excel would read _x0041_ as "A"


def column_letter(n: int) -> str:
    letters = ""
    while n:
        n, rest = divmod(n - 1, 26)
        letters = chr(65 + rest) + letters
    return letters


def sheet_ref(name: str) -> str:
    return "'" + name.replace("'", "''") + "'"


def number(value) -> str:
    return str(value) if isinstance(value, int) else repr(float(value))


def excel_time(when: dt.datetime) -> float:
    """A date and time as Excel counts it: days since 30 December 1899."""
    return (when - dt.datetime(1899, 12, 30)).total_seconds() / 86400


def utc(ms: int) -> dt.datetime:
    return dt.datetime(1970, 1, 1) + dt.timedelta(milliseconds=ms)


class Styles:
    """Each distinct cell format once, as Excel keeps them: fonts, fills, number formats, xfs."""

    def __init__(self):
        self.fonts: list[str] = []
        self.fills = ['<fill><patternFill patternType="none"/></fill>',
                      '<fill><patternFill patternType="gray125"/></fill>']  # Excel wants these two first
        self.formats: dict[str, int] = {}
        self.font()  # font 0, the workbook's default: Arial 10
        self.xfs = ['<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>']

    @staticmethod
    def index(items: list[str], xml: str) -> int:
        if xml not in items:
            items.append(xml)
        return items.index(xml)

    def font(self, bold=False, italic=False, size=10, color=None) -> int:
        return self.index(self.fonts, "<font>" + ("<b/>" if bold else "") + ("<i/>" if italic else "")
                          + f'<sz val="{size}"/>' + (f'<color rgb="FF{color}"/>' if color else "")
                          + '<name val="Arial"/><family val="2"/></font>')

    def get(self, bold=False, italic=False, size=10, color=None, fill=None, number=None,
            wrap=False, align=None, quote=False) -> int:
        font = self.font(bold, italic, size, color)
        fill_id = self.index(self.fills, f'<fill><patternFill patternType="solid"><fgColor rgb="FF{fill}"/>'
                                         '<bgColor indexed="64"/></patternFill></fill>') if fill else 0
        # 1 is Excel's own "0"; formats of our own are numbered from 164.
        fmt = 0 if number is None else 1 if number == "0" else self.formats.setdefault(number, 164 + len(self.formats))
        alignment = (f' horizontal="{align}"' if align else "") + ' vertical="top"' + (' wrapText="1"' if wrap else "")
        return self.index(self.xfs, f'<xf numFmtId="{fmt}" fontId="{font}" fillId="{fill_id}" borderId="0" xfId="0"'
                                    + (' applyNumberFormat="1"' if fmt else "") + ' applyFont="1"'
                                    + (' applyFill="1"' if fill_id else "") + ' applyAlignment="1"'
                                    + (' quotePrefix="1"' if quote else "") + f"><alignment{alignment}/></xf>")

    def xml(self) -> str:
        formats = "".join(f'<numFmt numFmtId="{i}" formatCode={quoteattr(code)}/>' for code, i in self.formats.items())
        return (XML + f'<styleSheet xmlns="{MAIN}">'
                + (f'<numFmts count="{len(self.formats)}">{formats}</numFmts>' if self.formats else "")
                + f'<fonts count="{len(self.fonts)}">{"".join(self.fonts)}</fonts>'
                + f'<fills count="{len(self.fills)}">{"".join(self.fills)}</fills>'
                + '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>'
                + '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>'
                + f'<cellXfs count="{len(self.xfs)}">{"".join(self.xfs)}</cellXfs>'
                + '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>'
                + "</styleSheet>")


class Sheet:
    def __init__(self, styles: Styles, name: str):
        self.styles, self.name = styles, name
        self.rows: dict[int, dict[int, str]] = {}
        self.widths: list[float] = []
        self.heights: dict[int, float] = {}
        self.freeze = (0, 0)  # columns and rows kept in view
        self.filter = ""      # the range with filter buttons
        self.count_a = 0      # what COUNTA of column A gives

    def cell(self, row: int, col: int, attrs: str, inner: str = "") -> None:
        ref = f"{column_letter(col)}{row}"
        self.rows.setdefault(row, {})[col] = f'<c r="{ref}"{attrs}>{inner}</c>' if inner else f'<c r="{ref}"{attrs}/>'
        if col == 1 and inner:
            self.count_a += 1

    def put(self, row: int, col: int, value, **style) -> None:
        """A value as data: text always as text, even when it starts with "="."""
        if isinstance(value, str) and value:
            text = NOT_XML.sub(lambda m: "\\x%02x" % ord(m.group()), value)
            text = EXCEL_ESCAPE.sub(lambda m: "_x005F" + m.group(), text)
            if len(text) > 32767:
                sys.exit(f"{self.name}!{column_letter(col)}{row}: {len(text)} characters, more than a cell holds")
            # quotePrefix keeps it text when someone edits the cell in Excel, too.
            s = self.styles.get(quote=text[0] in "=+-@\t\r", **style)
            space = ' xml:space="preserve"' if text != text.strip() else ""
            self.cell(row, col, f' s="{s}" t="inlineStr"', f"<is><t{space}>{escape(text)}</t></is>")
            return
        s = self.styles.get(**style)
        if value is None or value == "":  # NULL, or empty text: an empty cell either way
            self.cell(row, col, f' s="{s}"')
        elif isinstance(value, bool):
            self.cell(row, col, f' s="{s}" t="b"', f"<v>{int(value)}</v>")
        elif isinstance(value, dt.datetime):
            self.cell(row, col, f' s="{s}"', f"<v>{number(excel_time(value))}</v>")
        else:
            self.cell(row, col, f' s="{s}"', f"<v>{number(value)}</v>")

    def formula(self, row: int, col: int, text: str, value, **style) -> None:
        """A formula with its result already worked out, for viewers that do not calculate."""
        self.cell(row, col, f' s="{self.styles.get(**style)}"', f"<f>{escape(text)}</f><v>{number(value)}</v>")

    def xml(self, selected: bool) -> str:
        last_row = max(self.rows, default=1)
        last_col = max((col for cells in self.rows.values() for col in cells), default=1)
        cols, rows = self.freeze
        pane = ""
        if cols or rows:
            corner = f"{column_letter(cols + 1)}{rows + 1}"
            active = "bottomRight" if cols and rows else "bottomLeft" if rows else "topRight"
            split = (f' xSplit="{cols}"' if cols else "") + (f' ySplit="{rows}"' if rows else "")
            pane = (f'<pane{split} topLeftCell="{corner}" activePane="{active}" state="frozen"/>'
                    f'<selection pane="{active}" activeCell="{corner}" sqref="{corner}"/>')
        tab = ' tabSelected="1"' if selected else ""
        widths = "".join(f'<col min="{n}" max="{n}" width="{w}" customWidth="1"/>'
                         for n, w in enumerate(self.widths, start=1))
        data = "".join(f'<row r="{r}"' + (f' ht="{self.heights[r]}" customHeight="1"' if r in self.heights else "")
                       + ">" + "".join(self.rows[r][c] for c in sorted(self.rows[r])) + "</row>"
                       for r in sorted(self.rows))
        return (XML + f'<worksheet xmlns="{MAIN}" xmlns:r="{DOC_REL}">'
                + f'<dimension ref="A1:{column_letter(last_col)}{last_row}"/>'
                + f'<sheetViews><sheetView{tab} workbookViewId="0">{pane}</sheetView></sheetViews>'
                # No row heights but those set on purpose: Excel and LibreOffice fit the rest on opening.
                + '<sheetFormatPr defaultRowHeight="12.75"/>'
                + (f"<cols>{widths}</cols>" if widths else "")
                + f"<sheetData>{data}</sheetData>"
                + (f'<autoFilter ref="{self.filter}"/>' if self.filter else "")
                + "</worksheet>")


class Book:
    def __init__(self):
        self.styles = Styles()
        self.sheets: list[Sheet] = []

    def sheet(self, name: str) -> Sheet:
        sheet = Sheet(self.styles, re.sub(r"[\[\]:*?/\\]", "_", name)[:31])
        self.sheets.append(sheet)
        return sheet

    def find(self, name: str) -> Sheet:
        return next(s for s in self.sheets if s.name == name)

    def save(self, path: str) -> None:
        n = len(self.sheets)
        names = "".join(f'<sheet name={quoteattr(s.name)} sheetId="{i}" r:id="rId{i}"/>'
                        for i, s in enumerate(self.sheets, start=1))
        filters = "".join(f'<definedName name="_xlnm._FilterDatabase" localSheetId="{i}" hidden="1">'
                          + escape(sheet_ref(s.name)) + "!" + re.sub(r"([A-Z]+)(\d+)", r"$\1$\2", s.filter)
                          + "</definedName>" for i, s in enumerate(self.sheets) if s.filter)
        relations = "".join(f'<Relationship Id="rId{i}" Type="{DOC_REL}/worksheet" Target="worksheets/sheet{i}.xml"/>'
                            for i in range(1, n + 1))
        parts = {
            "[Content_Types].xml": XML + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
            '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
            '<Default Extension="xml" ContentType="application/xml"/>'
            f'<Override PartName="/xl/workbook.xml" ContentType="{TYPE}sheet.main+xml"/>'
            f'<Override PartName="/xl/styles.xml" ContentType="{TYPE}styles+xml"/>'
            + "".join(f'<Override PartName="/xl/worksheets/sheet{i}.xml" ContentType="{TYPE}worksheet+xml"/>'
                      for i in range(1, n + 1)) + "</Types>",
            "_rels/.rels": XML + f'<Relationships xmlns="{PKG_REL}"><Relationship Id="rId1" '
            f'Type="{DOC_REL}/officeDocument" Target="xl/workbook.xml"/></Relationships>',
            "xl/workbook.xml": XML + f'<workbook xmlns="{MAIN}" xmlns:r="{DOC_REL}">'
            '<bookViews><workbookView activeTab="0"/></bookViews>'
            f"<sheets>{names}</sheets>" + (f"<definedNames>{filters}</definedNames>" if filters else "")
            + '<calcPr fullCalcOnLoad="1"/></workbook>',
            "xl/_rels/workbook.xml.rels": XML + f'<Relationships xmlns="{PKG_REL}">{relations}'
            f'<Relationship Id="rId{n + 1}" Type="{DOC_REL}/styles" Target="styles.xml"/></Relationships>',
        }
        for i, s in enumerate(self.sheets, start=1):
            parts[f"xl/worksheets/sheet{i}.xml"] = s.xml(selected=i == 1)
        parts["xl/styles.xml"] = self.styles.xml()
        temporary = path + ".tmp"
        try:
            with zipfile.ZipFile(temporary, "w") as z:
                for name, text in parts.items():
                    # A fixed date, so the same snapshot always gives the same bytes.
                    info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
                    info.external_attr = 0o644 << 16
                    z.writestr(info, text.encode("utf-8"), compress_type=zipfile.ZIP_DEFLATED)
            os.replace(temporary, path)
        finally:
            if os.path.exists(temporary):
                os.remove(temporary)


# -- the sheets ---------------------------------------------------------------------------------

def text_width(value) -> int:
    return 0 if value is None else max((len(line) for line in str(value).split("\n")), default=0)


def line_count(text, width: int) -> int:
    """Lines a wrapped cell needs: Arial 10 fits about 1.1 characters per unit of column width."""
    if not isinstance(text, str) or not text:
        return 1
    per_line = max(1, int(width * 1.1))
    return sum(max(1, math.ceil(len(part) / per_line)) for part in text.split("\n"))


def table_sheet(book: Book, snap: dict, name: str) -> dict[str, str]:
    """One database table: its columns in order, each ms timestamp followed by a UTC formula.
    Returns each header's column letter."""
    t = snap["tables"][name]
    columns = [c["name"] for c in t["columns"]]
    layout = []  # (header, index of the table's column, whether it is the UTC formula)
    for i, column in enumerate(columns):
        layout.append((column, i, False))
        if column in MS_COLUMNS.get(name, ()):
            layout.append((column + " (UTC)", i, True))
    raw = {i: column_letter(n) for n, (_, i, is_utc) in enumerate(layout, start=1) if not is_utc}

    ws = book.sheet(name)
    for n, (head, _, is_utc) in enumerate(layout, start=1):
        ws.put(1, n, head, **(DERIVED if is_utc else HEAD))
    for r, row in enumerate(t["rows"], start=2):
        for n, (_, i, is_utc) in enumerate(layout, start=1):
            value = row[i]
            if is_utc:
                if value is not None:
                    ws.formula(r, n, f"{raw[i]}{r}/86400000+DATE(1970,1,1)", excel_time(utc(value)), number=DATETIME)
                continue
            if isinstance(value, dict):  # a BLOB, as SQL writes one
                value = "x'" + value["$blob"] + "'"
            integer = isinstance(value, int) and not isinstance(value, bool)
            ws.put(r, n, value, number="0" if integer else None, wrap=columns[i] in WRAPPED)

    for head, i, is_utc in layout:
        if is_utc:
            ws.widths.append(max(len(head) + 3, 20))
        elif columns[i] in WRAPPED:
            ws.widths.append(WRAPPED[columns[i]])
        else:
            longest = max((text_width(row[i]) for row in t["rows"]), default=0)
            ws.widths.append(min(max(len(head) + 3, longest + 2), 40))
    if name == "reports":
        # context is long JSON: wrapped, but a row is only as tall as note and resolution need,
        # in whole Arial 10 lines (12.75 pt), so context stops at a line. It reads in full in the
        # formula bar and on "context (expanded)".
        sized = [columns.index(c) for c in ("note", "resolution") if c in columns]
        for r, row in enumerate(t["rows"], start=2):
            ws.heights[r] = 12.75 * max([line_count(row[i], WRAPPED[columns[i]]) for i in sized] or [1])
    ws.freeze = (1, 1)
    ws.filter = f"A1:{column_letter(len(layout))}{len(t['rows']) + 1}"
    return {head: column_letter(n) for n, (head, _, _) in enumerate(layout, start=1)}


def context_sheet(book: Book, snap: dict) -> None:
    t = snap["tables"]["reports"]
    names = [c["name"] for c in t["columns"]]
    ws = book.sheet("context (expanded)")
    for n, head in enumerate(("report_id", "key", "value"), start=1):
        ws.put(1, n, head, **DERIVED)
    r, longest_key = 1, 8
    for row in t["rows"]:
        for key, value in json.loads(row[names.index("context")]).items():
            if isinstance(value, (dict, list)):
                value = json.dumps(value, ensure_ascii=False)
            r += 1
            ws.put(r, 1, row[names.index("id")])
            ws.put(r, 2, key)
            ws.put(r, 3, value, wrap=True)
            longest_key = max(longest_key, len(key))
    ws.widths = [38, min(longest_key + 3, 30), 90]
    ws.freeze = (1, 1)
    ws.filter = f"A1:C{r}"


def ddl_comments(sql: str | None) -> dict[str, str]:
    """Column comments written in a CREATE TABLE: `name TYPE ...,  -- comment`."""
    out = {}
    for line in (sql or "").splitlines():
        code, dashes, comment = line.partition("--")
        words = code.split()
        if dashes and comment.strip() and len(words) >= 2 and words[0].upper() not in ("CREATE", "PRIMARY", "FOREIGN"):
            out[words[0]] = comment.strip()
    return out


def fields_sheet(book: Book, snap: dict) -> None:
    """Every column of every table, one per row."""
    ws = book.sheet("fields")
    heads = ("table", "#", "column", "type", "not null", "default", "primary key", "references", "indexed by",
             "meaning")
    for n, head in enumerate(heads, start=1):
        ws.put(1, n, head, **HEAD)
    sql_of = {o["name"]: o["sql"] for o in snap["objects"]}
    r = 1
    for name, t in snap["tables"].items():
        comments = ddl_comments(sql_of.get(name))
        references = {fk["from"]: f"{fk['table']}.{fk['to']}"
                      + ("" if fk["on_delete"] == "NO ACTION" else f" (on delete {fk['on_delete'].lower()})")
                      for fk in t["foreign_keys"]}
        for c in t["columns"]:
            indexed = [ix["name"] + (" (unique)" if ix["unique"] else "")
                       for ix in sorted(t["indexes"], key=lambda ix: ix["name"]) if c["name"] in ix["columns"]]
            meaning = comments.get(c["name"])
            meaning = meaning[0].upper() + meaning[1:] if meaning else MEANING.get((name, c["name"]))
            r += 1
            for n, value in enumerate((name, c["cid"] + 1, c["name"], c["type"] or None,
                                       "yes" if c["notnull"] else None, c["default"],
                                       "yes" if c["pk"] else None, references.get(c["name"]),
                                       ", ".join(indexed) or None, meaning), start=1):
                ws.put(r, n, value, number="0" if n == 2 else None, wrap=n == 10, align="center" if n == 2 else None)
    ws.widths = [17, 5, 14, 10, 10, 10, 13, 30, 36, 60]
    ws.freeze = (3, 1)
    ws.filter = f"A1:J{r}"


def schema_sheet(book: Book, snap: dict) -> None:
    """The CREATE statements as the database stores them, indexes included."""
    ws = book.sheet("schema")
    for n, head in enumerate(("type", "name", "on table", "SQL"), start=1):
        ws.put(1, n, head, **HEAD)
    for r, o in enumerate(snap["objects"], start=2):
        sql = o["sql"] or "(none: SQLite made this index itself, for the primary key)"
        for n, value in enumerate((o["type"], o["name"], o["table"], sql), start=1):
            ws.put(r, n, value, wrap=n == 4)
    ws.widths = [8, 28, 17, 100]
    ws.freeze = (0, 1)


def about_sheet(book: Book, snap: dict, letters: dict[str, str]) -> None:
    """[letters]: each header of the reports sheet and its column letter."""
    status_column = letters.get("status")
    ws = book.sheet("about")
    ws.put(1, 1, "Per Ankh and BushLines problem reports", bold=True, size=14)
    ws.put(2, 1, "Every table and column of the live report database, as one read-only snapshot.")
    facts = [
        ("Source", f"The report service behind perankh.hrana.app: {snap['database']} (SQLite)", None),
        ("Snapshot taken (UTC)", utc(snap["taken_at_ms"]), DATETIME),
        ("How it was read", "Opened read-only and read in one transaction, so every sheet is from the same "
                            "moment. Nothing was written to the database.", None),
        ("Schema version", snap["user_version"], "0"),
        ("SQLite", f"{snap['sqlite_version']}, {snap['journal_mode'].upper()} journal", None),
    ]
    r = 3
    for label, value, fmt in facts:
        r += 1
        ws.put(r, 1, label, **LABEL)
        ws.put(r, 2, value, number=fmt, wrap=True, align="left")

    r += 2
    ws.put(r, 1, "Sheets", **SECTION)
    r += 1
    for n, head in enumerate(("sheet", "what it holds", "rows"), start=1):
        ws.put(r, n, head, **HEAD)
    listed = [(name, "Database table: " + TABLES.get(name, "added by a later migration")) for name in snap["tables"]]
    listed += [
        ("context (expanded)", "Derived: reports.context (a JSON object) one key and value per row"),
        ("fields", "Derived: every column of every table, with its type, constraints, indexes and meaning"),
        ("schema", "The database’s own CREATE statements (sqlite_master), indexes included"),
    ]
    for name, what in listed:
        if not any(s.name == name for s in book.sheets):
            continue
        r += 1
        ws.put(r, 1, name, **LABEL)
        ws.put(r, 2, what, wrap=True)
        ws.formula(r, 3, f"COUNTA({sheet_ref(name)}!$A:$A)-1", book.find(name).count_a - 1, number="0")

    reports = snap["tables"].get("reports")
    names = [c["name"] for c in reports["columns"]] if reports else []
    if reports and status_column:
        # COUNTIF ignores case, so these counts do too.
        counts = collections.Counter(str(row[names.index("status")]).lower() for row in reports["rows"])
        statuses = STATUSES + sorted(set(counts) - set(STATUSES))
        r += 2
        ws.put(r, 1, "Reports by status", **SECTION)
        r += 1
        for n, head in enumerate(("status", "", "reports"), start=1):
            ws.put(r, n, head, **HEAD)
        first = r + 1
        for status in statuses:
            r += 1
            ws.put(r, 1, status, **LABEL)
            ws.formula(r, 3, f"COUNTIF('reports'!${status_column}:${status_column},A{r})", counts[status], number="0")
        last = r
        r += 1
        ws.put(r, 1, "open", **LABEL)
        ws.put(r, 2, "new, triaged and in_progress: still waiting on work")
        ws.formula(r, 3, "+".join(f"C{first + statuses.index(s)}" for s in OPEN), sum(counts[s] for s in OPEN),
                   number="0")
        r += 1
        ws.put(r, 1, "total", **LABEL)
        ws.formula(r, 3, f"SUM(C{first}:C{last})", sum(counts[s] for s in statuses), number="0", bold=True)

    app_column = letters.get("app")
    if reports and status_column and app_column:
        apps = [str(row[names.index("app")]).lower() for row in reports["rows"]]
        opened = collections.Counter(app for app, row in zip(apps, reports["rows"])
                                     if str(row[names.index("status")]).lower() in OPEN)
        counts = collections.Counter(apps)
        r += 2
        ws.put(r, 1, "Reports by app", **SECTION)
        r += 1
        for n, head in enumerate(("app", "", "reports", "open"), start=1):
            ws.put(r, n, head, **HEAD)
        for app in [a for a in APPS if a in counts] + sorted(set(counts) - set(APPS)):
            r += 1
            ws.put(r, 1, app, **LABEL)
            ws.put(r, 2, APPS.get(app))
            ws.formula(r, 3, f"COUNTIF('reports'!${app_column}:${app_column},A{r})", counts[app], number="0")
            ws.formula(r, 4, "+".join(f"COUNTIFS('reports'!${app_column}:${app_column},A{r},"
                                      f"'reports'!${status_column}:${status_column},\"{s}\")" for s in OPEN),
                       opened[app], number="0")

    empty_notes = "note" in names and any(row[names.index("note")] == "" for row in reports["rows"])
    notes = [
        "Columns headed “… (UTC)” are formulas, not database columns: the millisecond timestamp to their "
        "left as a date and time in UTC. The raw values are unchanged.",
        "received_at, updated_at and report_log.at come from the server’s clock; created_at from the phone’s.",
        "A blank cell is NULL in the database." + (" An empty note is stored as empty text, and shows blank "
                                                   "too." if empty_notes else ""),
        "Reports arrive through the app’s public upload endpoint, so their text is whatever the sender typed. "
        "Every text value is stored as text: one that starts with = + - or @ never runs as a formula.",
        "Dark blue headers are database columns; light blue ones were made for this workbook.",
    ]
    r += 2
    ws.put(r, 1, "Reading the sheets", **SECTION)
    for text in notes:
        r += 1
        ws.put(r, 1, "•", align="right")
        ws.put(r, 2, text, wrap=True)
    ws.widths = [22, 90, 10, 10]


def build(snap: dict) -> Book:
    book = Book()
    report_letters = {}
    for name in snap["tables"]:  # sqlite_master order: reports, report_log, sqlite_sequence, identify_days
        letters = table_sheet(book, snap, name)
        if name == "reports":
            report_letters = letters
    if "reports" in snap["tables"] and any(c["name"] == "context" for c in snap["tables"]["reports"]["columns"]):
        context_sheet(book, snap)
    fields_sheet(book, snap)
    schema_sheet(book, snap)
    about_sheet(book, snap, report_letters)
    return book


def main() -> None:
    parser = argparse.ArgumentParser(description="Every table of the live report database, as one Excel workbook.")
    parser.add_argument("--out", help="where to write it (default: tools/out/reports/perankh-reports-DATE.xlsx)")
    parser.add_argument("--snapshot", metavar="JSON",
                        help="build from a snapshot saved earlier from tools/reports_snapshot.py")
    args = parser.parse_args()
    if args.snapshot:
        with open(args.snapshot, encoding="utf-8") as f:
            snap = json.load(f)
    else:
        snap = take_snapshot()
    taken = dt.datetime.fromtimestamp(snap["taken_at_ms"] / 1000)  # the date here, for the file's name
    path = args.out or os.path.join(OUT_DIR, f"perankh-reports-{taken:%Y-%m-%d}.xlsx")
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    book = build(snap)
    try:
        book.save(path)
    except PermissionError:
        sys.exit(f"cannot write {path}: is it open in a spreadsheet? Close it and run this again.")
    reports = snap["tables"].get("reports", {"columns": [], "rows": []})
    names = [c["name"] for c in reports["columns"]]
    statuses = [str(row[names.index("status")]).lower() for row in reports["rows"]] if "status" in names else []
    print(f"{path}: {len(reports['rows'])} reports, {sum(s in OPEN for s in statuses)} open; "
          f"snapshot {utc(snap['taken_at_ms']):%Y-%m-%d %H:%M} UTC")


if __name__ == "__main__":
    main()
