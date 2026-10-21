"""The report store: one SQLite file, migrated in place, safe to read while the service writes.

The schema is versioned so later releases can add tables without touching existing rows. Version
1 holds anonymous reports; `account_id` is reserved for when people can sign in and their reports
(and, later, their progress) can be tied to an account they control. See server/README.md.
"""
from __future__ import annotations

import json
import sqlite3
import time
from typing import Iterable

# Triage states, in the order a report usually moves through them.
STATUSES = ("new", "triaged", "in_progress", "fixed", "released", "wontfix", "duplicate", "invalid")
OPEN = ("new", "triaged", "in_progress")

# Context fields copied into their own columns, so triage can filter on them.
INDEXED_CONTEXT = {"screen": "screen", "lesson": "lesson", "exercise": "exercise", "item": "item"}

MIGRATIONS = [
    # 1: anonymous tester reports and their triage history.
    """
    CREATE TABLE reports (
        id           TEXT PRIMARY KEY,      -- UUID made on the phone: uploads are idempotent
        received_at  INTEGER NOT NULL,      -- server clock, ms
        created_at   INTEGER NOT NULL,      -- when the tester saved it, phone clock, ms
        app_version  TEXT,
        android_sdk  INTEGER,
        category     TEXT NOT NULL,
        note         TEXT NOT NULL DEFAULT '',
        screen       TEXT,
        lesson       TEXT,
        exercise     TEXT,
        item         TEXT,
        context      TEXT NOT NULL,         -- the whole on-screen context, a JSON object
        account_id   TEXT,                  -- reserved for signed-in reports, always NULL in v1
        status       TEXT NOT NULL DEFAULT 'new',
        resolution   TEXT,
        resolved_in  TEXT,                  -- app version or commit with the fix
        duplicate_of TEXT,
        updated_at   INTEGER NOT NULL
    );
    CREATE INDEX reports_by_status ON reports(status, received_at);
    CREATE INDEX reports_by_item ON reports(item);
    CREATE TABLE report_log (
        id        INTEGER PRIMARY KEY AUTOINCREMENT,
        report_id TEXT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
        at        INTEGER NOT NULL,
        action    TEXT NOT NULL,
        detail    TEXT
    );
    """,
]


def now_ms() -> int:
    return int(time.time() * 1000)


def connect(path: str) -> sqlite3.Connection:
    conn = sqlite3.connect(path, timeout=10, isolation_level=None, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.execute("PRAGMA busy_timeout=10000")
    migrate(conn)
    return conn


def schema_version(conn: sqlite3.Connection) -> int:
    return conn.execute("PRAGMA user_version").fetchone()[0]


def migrate(conn: sqlite3.Connection) -> None:
    version = schema_version(conn)
    for number, script in enumerate(MIGRATIONS, start=1):
        if number <= version:
            continue
        # Each migration and its version bump commit together, or not at all.
        try:
            conn.executescript(f"BEGIN IMMEDIATE;\n{script}\nPRAGMA user_version={number};\nCOMMIT;")
        except Exception:
            if conn.in_transaction:
                conn.execute("ROLLBACK")
            raise


def insert_report(conn: sqlite3.Connection, report: dict, app_version: str | None, sdk: int | None) -> bool:
    """Stores one validated report. Returns False if a report with that id is already stored."""
    context = report["context"]
    columns = {column: context.get(key) for key, column in INDEXED_CONTEXT.items()}
    stamp = now_ms()
    cursor = conn.execute(
        """INSERT OR IGNORE INTO reports
           (id, received_at, created_at, app_version, android_sdk, category, note,
            screen, lesson, exercise, item, context, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (report["id"], stamp, report["at"], app_version, sdk, report["category"], report["note"],
         columns["screen"], columns["lesson"], columns["exercise"], columns["item"],
         json.dumps(context, ensure_ascii=False, sort_keys=True), stamp),
    )
    if cursor.rowcount:
        log(conn, report["id"], "received", None)
    return bool(cursor.rowcount)


def log(conn: sqlite3.Connection, report_id: str, action: str, detail: str | None) -> None:
    conn.execute("INSERT INTO report_log (report_id, at, action, detail) VALUES (?, ?, ?, ?)",
                 (report_id, now_ms(), action, detail))


def find(conn: sqlite3.Connection, prefix: str) -> sqlite3.Row | None:
    """A report by full id or a unique prefix of at least 6 characters."""
    rows = conn.execute("SELECT * FROM reports WHERE id LIKE ? ESCAPE '\\' LIMIT 2",
                        (prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%",)).fetchall()
    if len(prefix) < 6 or len(rows) != 1:
        return None
    return rows[0]


def set_status(conn: sqlite3.Connection, report_id: str, status: str, note: str | None = None,
               resolved_in: str | None = None, duplicate_of: str | None = None) -> None:
    if status not in STATUSES:
        raise ValueError(f"unknown status {status!r}; one of {', '.join(STATUSES)}")
    conn.execute("BEGIN IMMEDIATE")
    try:
        conn.execute(
            """UPDATE reports SET status = ?, updated_at = ?,
                   resolution = COALESCE(?, resolution),
                   resolved_in = COALESCE(?, resolved_in),
                   duplicate_of = COALESCE(?, duplicate_of)
               WHERE id = ?""",
            (status, now_ms(), note, resolved_in, duplicate_of, report_id),
        )
        detail = "; ".join(part for part in (note, resolved_in and f"in {resolved_in}",
                                              duplicate_of and f"of {duplicate_of}") if part)
        log(conn, report_id, status, detail or None)
        conn.execute("COMMIT")
    except Exception:
        conn.execute("ROLLBACK")
        raise


def query(conn: sqlite3.Connection, statuses: Iterable[str] | None = None, category: str | None = None,
          item: str | None = None, limit: int = 50) -> list[sqlite3.Row]:
    sql = "SELECT * FROM reports WHERE 1=1"
    args: list = []
    if statuses:
        statuses = list(statuses)
        sql += f" AND status IN ({','.join('?' * len(statuses))})"
        args += statuses
    if category:
        sql += " AND category = ?"
        args.append(category)
    if item:
        sql += " AND item = ?"
        args.append(item)
    sql += " ORDER BY received_at DESC LIMIT ?"
    args.append(limit)
    return conn.execute(sql, args).fetchall()


def history(conn: sqlite3.Connection, report_id: str) -> list[sqlite3.Row]:
    return conn.execute("SELECT * FROM report_log WHERE report_id = ? ORDER BY id", (report_id,)).fetchall()


def prune(conn: sqlite3.Connection, closed_before_ms: int) -> int:
    """Deletes closed reports last changed before the cutoff; open reports are never pruned."""
    conn.execute("BEGIN IMMEDIATE")
    try:
        cursor = conn.execute(
            f"DELETE FROM reports WHERE status NOT IN ({','.join('?' * len(OPEN))}) AND updated_at < ?",
            (*OPEN, closed_before_ms),
        )
        conn.execute("COMMIT")
    except Exception:
        conn.execute("ROLLBACK")
        raise
    return cursor.rowcount
