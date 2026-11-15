"""Review the course's pronunciation clips by ear, in a browser.

    python -X utf8 tools/review.py               # serves http://127.0.0.1:8765 and opens it
    python -X utf8 tools/review.py --port 8800 --no-browser
    python -X utf8 tools/review.py status        # how far the review has got
    python -X utf8 tools/review.py apply         # pin the takes approved by ear; list the retakes asked for

The page lists every clip that needs a listener, most urgent first: clips flagged while triaging
(a problem report, a pronunciation rule that changed: tools/out/review/flags.json), clips with a
new take since they were last reviewed, clips that fail the voice tool's own check, and the word
forms recorded for tapping a word in a phrase; then everything else, for browsing. Each clip shows
what it should say (signs, transliteration, respelling, IPA, meaning, the lesson it is first heard
in) and every take recorded for it, with what the transcriber heard. A verdict (sounds right, use
another take, needs a retake), tags and a comment are saved at once to
tools/out/review/review.json.

`apply` turns approvals into the voice tool's pins (tools/voice_pins.json), so a take chosen by
ear stays chosen; retakes are listed with the listener's tags and comments for whoever records
next. Nothing here calls ElevenLabs or costs credit. The server listens on this machine only.

flags.json, written while triaging, maps a clip id to why it needs an ear:
    {"w:nfr": [{"reason": "reported", "note": "what the listener heard", "report": "0a1b2c3d"}],
     "c:future": [{"reason": "rule", "note": "jj now has a y glide: iyet", "was": "<the old take>.mp3"}]}
"was" names the take the app played before, so the page can play the two side by side.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import sys
import threading
import webbrowser
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voice  # noqa: E402  (our tools/voice.py: the clips, their takes and judgements)

ROOT = voice.ROOT
REVIEW_DIR = os.path.join(ROOT, "tools", "out", "review")
REVIEWS = os.path.join(REVIEW_DIR, "review.json")
FLAGS = os.path.join(REVIEW_DIR, "flags.json")
PAGE = os.path.join(ROOT, "tools", "review.html")
SIGNS = os.path.join(ROOT, "core-script", "src", "main", "resources", "com", "krbavcic", "pentalingo", "script", "signs.tsv")
FONT_DIR = os.path.join(ROOT, "core-ui", "src", "main", "res", "font")
FONTS = {
    "hieroglyphs.ttf": "noto_sans_egyptian_hieroglyphs.ttf",
    "serif.ttf": "noto_serif_regular.ttf",
    "serif-bold.ttf": "noto_serif_bold.ttf",
    "serif-italic.ttf": "noto_serif_italic.ttf",
}
AUDIO_NAME = re.compile(r"^[0-9a-f]{32}\.mp3$")
VERDICTS = {"ok", "retake", "skip", None}
# Why a clip is in the queue, most urgent first: new takes of clips already reviewed (the answer
# to a retake asked for), then reports, rule changes, other flags, failed checks, new word forms.
GROUPS = ["changed", "reported", "rule", "flagged", "failing", "form", "other"]

LOCK = threading.Lock()
_cache: dict = {}


def now() -> str:
    return dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def mtime(path: str) -> float:
    return os.path.getmtime(path) if os.path.exists(path) else 0.0


def base_rows() -> list[dict]:
    """Every clip with its takes, from the voice tool's files; cached until one of them changes."""
    stamp = tuple(mtime(p) for p in (voice.SPEECH, voice.CHOICES, voice.TRANSCRIPTS, voice.PINS, voice.VARIANTS))
    if _cache.get("stamp") == stamp:
        return _cache["rows"]
    speech = voice.load_clips()
    choices = voice.load(voice.CHOICES, {})
    transcripts = voice.load(voice.TRANSCRIPTS, {})
    pins = voice.load(voice.PINS, {})
    rows = []
    for c in speech:
        chosen = choices.get(c["id"])
        current = chosen if chosen and chosen["text"] in c["candidates"] else None
        takes = voice.recorded(c, transcripts)
        lesson = c.get("lesson")
        m = re.match(r"l(\d+)", lesson or "")
        rows.append({
            "id": c["id"], "kind": c["id"].split(":")[0], "spoken": c["spoken"], "latin": c["latin"], "ipa": c["ipa"],
            "mdc": c.get("mdc"), "meaning": c.get("meaning") or c.get("gloss"), "lesson": lesson,
            "lessonNo": int(m.group(1)) if m else 10_000, "usedIn": c.get("usedIn"), "usedInText": c.get("usedInText"),
            "tokens": c.get("tokens"), "candidates": c["candidates"],
            "chosen": current["file"] if current else None,
            "passed": bool(current and current["passed"]), "score": current["score"] if current else None,
            "pinned": voice.pinned(c, pins, transcripts) is not None,
            "takes": [{k: t.get(k) for k in ("file", "text", "seed", "lang", "framed", "score", "passed",
                                              "heard_ar", "heard_en", "heard_framed")} for t in takes],
        })
    _cache.update(stamp=stamp, rows=rows)
    return rows


def status_of(row: dict, flags: list, review: dict | None) -> tuple[str, str]:
    """(status, group): where a clip stands in the review, and why it is in the queue."""
    reasons = {f.get("reason") for f in flags}
    verdict = (review or {}).get("verdict")
    newer = False
    if verdict == "ok":
        # Approved: this take, or another one that `apply` will pin. Any other take wants a new listen.
        newer = row["chosen"] not in (review.get("take"), review.get("chosen_then"))
    elif verdict in ("retake", "skip"):
        then = set(review.get("takes_then") or [])
        newer = row["chosen"] != review.get("chosen_then") or any(t["file"] not in then for t in row["takes"])
    if newer:
        group = "changed"
    elif "reported" in reasons:
        group = "reported"
    elif "rule" in reasons:
        group = "rule"
    elif reasons:
        group = "flagged"
    elif row["chosen"] and not row["passed"]:
        group = "failing"
    elif row["kind"] == "f":
        group = "form"
    else:
        group = "other"
    if row["chosen"] is None:
        return "unrecorded", group
    if newer:
        return "todo", group
    if verdict == "ok":
        return "done", group
    if verdict == "retake":
        return "retake", group
    if verdict == "skip":
        return "skipped", group
    return ("todo" if group != "other" else "fine"), group


def rows_for_page() -> dict:
    flags = voice.load(FLAGS, {})
    reviews = voice.load(REVIEWS, {}).get("clips", {})
    out = []
    for row in base_rows():
        f = flags.get(row["id"], [])
        r = reviews.get(row["id"])
        status, group = status_of(row, f, r)
        out.append({**row, "flags": f, "review": r, "status": status, "group": group})
    out.sort(key=lambda r: (GROUPS.index(r["group"]), r["lessonNo"]))
    return {"clips": out, "groups": GROUPS}


def save_review(body: dict) -> dict:
    cid = body.get("id")
    verdict = body.get("verdict")
    if verdict not in VERDICTS:
        raise ValueError(f"unknown verdict {verdict!r}")
    row = next((r for r in base_rows() if r["id"] == cid), None)
    if row is None:
        raise ValueError(f"no clip {cid!r}")
    take = body.get("take")
    files = {t["file"] for t in row["takes"]} | {f.get("was") for f in voice.load(FLAGS, {}).get(cid, [])}
    if take is not None and take not in files:
        raise ValueError(f"{take!r} is not a take of {cid}")
    tags = [str(t)[:40] for t in body.get("tags") or []][:20]
    comment = str(body.get("comment") or "")[:2000]
    with LOCK:
        data = voice.load(REVIEWS, {"clips": {}})
        old = data["clips"].get(cid)
        entry = {"verdict": verdict, "take": take, "tags": tags, "comment": comment,
                 "chosen_then": row["chosen"], "takes_then": [t["file"] for t in row["takes"]], "at": now()}
        if old:
            # Keep what was said before about earlier takes: it explains the retakes.
            history = old.pop("history", [])
            if old.get("verdict") and old.get("chosen_then") != row["chosen"]:
                history.append(old)
            if history:
                entry["history"] = history
        if verdict is None and not tags and not comment and "history" not in entry:
            data["clips"].pop(cid, None)
        else:
            data["clips"][cid] = entry
        voice.save(REVIEWS, data)
    return data["clips"].get(cid) or {}


def signs_table() -> dict[str, str]:
    out = {}
    with open(SIGNS, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            parts = line.split("\t")
            out[parts[0]] = parts[1]
    return out


class Handler(BaseHTTPRequestHandler):
    server_version = "PerAnkhReview/1"

    def log_message(self, fmt, *args):  # quiet: one line per request is noise here
        pass

    def send_bytes(self, data: bytes, ctype: str, status: int = 200, extra: dict | None = None) -> None:
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store" if ctype.startswith(("application/json", "text/html")) else "max-age=3600")
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(data)

    def send_json(self, obj, status: int = 200) -> None:
        self.send_bytes(json.dumps(obj, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8", status)

    def send_file(self, path: str, ctype: str) -> None:
        if not os.path.isfile(path):
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        with open(path, "rb") as f:
            data = f.read()
        # Audio elements ask for byte ranges to seek; answer them.
        rng = self.headers.get("Range")
        m = re.match(r"bytes=(\d*)-(\d*)$", rng or "")
        if m and (m.group(1) or m.group(2)):
            size = len(data)
            start = int(m.group(1)) if m.group(1) else size - int(m.group(2))
            end = int(m.group(2)) if m.group(1) and m.group(2) else size - 1
            start, end = max(0, start), min(end, size - 1)
            self.send_bytes(data[start:end + 1], ctype, 206,
                            {"Content-Range": f"bytes {start}-{end}/{size}", "Accept-Ranges": "bytes"})
            return
        self.send_bytes(data, ctype, 200, {"Accept-Ranges": "bytes"})

    def do_GET(self):  # noqa: N802
        path = urlparse(self.path).path
        try:
            if path in ("/", "/index.html"):
                self.send_file(PAGE, "text/html; charset=utf-8")
            elif path == "/api/clips":
                self.send_json(rows_for_page())
            elif path == "/api/signs":
                self.send_json(signs_table())
            elif path == "/api/review.json":
                self.send_json(voice.load(REVIEWS, {"clips": {}}))
            elif path.startswith("/audio/"):
                name = path.rsplit("/", 1)[-1]
                if not AUDIO_NAME.match(name):
                    self.send_error(HTTPStatus.NOT_FOUND)
                    return
                self.send_file(voice.take_path(name), "audio/mpeg")
            elif path.startswith("/font/") and path.rsplit("/", 1)[-1] in FONTS:
                self.send_file(os.path.join(FONT_DIR, FONTS[path.rsplit("/", 1)[-1]]), "font/ttf")
            else:
                self.send_error(HTTPStatus.NOT_FOUND)
        except (ConnectionError, BrokenPipeError):
            pass

    def do_POST(self):  # noqa: N802
        if urlparse(self.path).path != "/api/review":
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        length = int(self.headers.get("Content-Length") or 0)
        if length > 100_000:
            self.send_error(HTTPStatus.REQUEST_ENTITY_TOO_LARGE)
            return
        try:
            body = json.loads(self.rfile.read(length) or b"{}")
            self.send_json(save_review(body))
        except (ValueError, json.JSONDecodeError) as e:
            self.send_json({"error": str(e)}, 400)


def cmd_status() -> None:
    page = rows_for_page()["clips"]
    by = {}
    for r in page:
        by.setdefault(r["status"], {}).setdefault(r["group"], 0)
        by[r["status"]][r["group"]] += 1
    for status in ("todo", "retake", "done", "skipped", "unrecorded", "fine"):
        groups = by.get(status, {})
        if groups:
            detail = ", ".join(f"{g} {n}" for g, n in sorted(groups.items(), key=lambda kv: GROUPS.index(kv[0])))
            print(f"{status:<10} {sum(groups.values()):5}   ({detail})")


def cmd_apply() -> None:
    """Pins every take approved by ear, and lists the retakes asked for with their tags and comments."""
    page = {r["id"]: r for r in rows_for_page()["clips"]}
    reviews = voice.load(REVIEWS, {}).get("clips", {})
    speech = {c["id"]: c for c in voice.load_clips()}
    transcripts = voice.load(voice.TRANSCRIPTS, {})
    pins = voice.load(voice.PINS, {})
    pinned = 0
    for cid, r in sorted(reviews.items()):
        clip, row = speech.get(cid), page.get(cid)
        if not clip or not row or r.get("verdict") != "ok" or not r.get("take"):
            continue
        take = next((t for t in voice.recorded(clip, transcripts) if t["file"] == r["take"]), None)
        if take is None:
            print(f"!! {cid}: approved take {r['take']} is not a take of the clip's current text; listen again")
            continue
        if pins.get(cid) != {"file": take["file"], "text": take["text"]}:
            pins[cid] = {"file": take["file"], "text": take["text"]}
            pinned += 1
    voice.save(voice.PINS, pins)
    choices = voice.load(voice.CHOICES, {})
    voice.apply_pins(list(speech.values()), choices, transcripts)
    voice.save(voice.CHOICES, choices)
    voice.write_manifest()
    print(f"pinned {pinned} takes approved by ear ({len(pins)} pins in all)")
    retakes = [(cid, r) for cid, r in sorted(reviews.items()) if r.get("verdict") == "retake"
               and page.get(cid, {}).get("status") == "retake"]
    if retakes:
        print(f"\n{len(retakes)} clips need a retake:")
        for cid, r in retakes:
            row = page[cid]
            print(f"  {cid:16} {row['latin']:32} {', '.join(r.get('tags') or [])}" + (f" — {r['comment']}" if r.get("comment") else ""))


def main() -> None:
    parser = argparse.ArgumentParser(description="Review Per Ankh's pronunciation clips by ear")
    parser.add_argument("command", nargs="?", default="serve", choices=["serve", "status", "apply"])
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--no-browser", action="store_true")
    args = parser.parse_args()
    if args.command == "status":
        cmd_status()
        return
    if args.command == "apply":
        cmd_apply()
        return
    os.makedirs(REVIEW_DIR, exist_ok=True)
    httpd = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    url = f"http://127.0.0.1:{args.port}/"
    print(f"clip review at {url} (Ctrl+C to stop)", flush=True)
    if not args.no_browser:
        threading.Timer(0.5, lambda: webbrowser.open(url)).start()
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
