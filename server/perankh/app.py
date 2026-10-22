"""The Per Ankh API, as a WSGI application.

Public surface, deliberately small:

    GET  /v1/health   -> {"ok": true, "schema": 1}
    POST /v1/reports  -> {"accepted": [...], "duplicate": [...], "rejected": [{"id", "error"}]}
    GET  /v1/latest   -> {"tag_name", "html_url", "published_at"}: the newest release (releases.py)

Reports are anonymous: the body carries the app version, the Android API level and the reports
themselves, nothing that identifies a person or a device. The service logs no request bodies and
no client addresses. Triage happens on the server through `perankh-reports`, never over HTTP.

Everything else is refused. Every limit here is also enforced, more cheaply, by nginx in front.
"""
from __future__ import annotations

import json
import os
import re
import sqlite3
import threading

from . import db
from .releases import Releases

MAX_BODY = 256 * 1024
MAX_REPORTS = 50
MAX_NOTE = 4000
MAX_CATEGORY = 40
MAX_CONTEXT_ENTRIES = 120
MAX_KEY = 64
MAX_VALUE = 4000
MAX_VERSION = 40
# Refuse new reports once the database passes this size, rather than fill the disk.
MAX_DB_BYTES = 512 * 1024 * 1024
# Reports dated before the first build that could make them, or more than a day ahead.
EARLIEST = 1735689600000  # 2025-01-01T00:00:00Z

UUID = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")


class Invalid(ValueError):
    pass


def clean_text(value, limit: int, field: str, allow_empty: bool = True) -> str:
    if not isinstance(value, str):
        raise Invalid(f"{field} must be a string")
    if len(value) > limit:
        raise Invalid(f"{field} is longer than {limit} characters")
    if not allow_empty and not value.strip():
        raise Invalid(f"{field} is empty")
    # Control characters have no business in a report and make terminals misbehave in triage.
    return CONTROL.sub("", value)


def validate_report(raw) -> dict:
    if not isinstance(raw, dict):
        raise Invalid("report must be an object")
    report_id = raw.get("id")
    if not isinstance(report_id, str) or not UUID.match(report_id.lower()):
        raise Invalid("id must be a UUID")
    at = raw.get("at")
    if not isinstance(at, int) or isinstance(at, bool) or not EARLIEST <= at <= db.now_ms() + 86_400_000:
        raise Invalid("at must be a plausible time in milliseconds")
    category = clean_text(raw.get("category"), MAX_CATEGORY, "category", allow_empty=False)
    note = clean_text(raw.get("note", ""), MAX_NOTE, "note")
    # The version the report was written in, which may be older than the one sending it.
    app = raw.get("app")
    app = clean_text(app, MAX_VERSION, "app") if app is not None else None
    context_raw = raw.get("context", {})
    if not isinstance(context_raw, dict):
        raise Invalid("context must be an object")
    if len(context_raw) > MAX_CONTEXT_ENTRIES:
        raise Invalid(f"context has more than {MAX_CONTEXT_ENTRIES} entries")
    context = {}
    for key, value in context_raw.items():
        key = clean_text(key, MAX_KEY, "context key", allow_empty=False)
        context[key] = clean_text(value, MAX_VALUE, f"context[{key}]")
    return {"id": report_id.lower(), "at": at, "category": category, "note": note, "context": context, "app": app}


def parse_upload(body: bytes) -> tuple[str | None, int | None, list]:
    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise Invalid("body is not JSON")
    if not isinstance(payload, dict):
        raise Invalid("body must be an object")
    app = payload.get("app", {})
    if not isinstance(app, dict):
        raise Invalid("app must be an object")
    version = app.get("version")
    version = clean_text(version, MAX_VERSION, "app.version") if version is not None else None
    sdk = app.get("sdk")
    if sdk is not None and (not isinstance(sdk, int) or isinstance(sdk, bool) or not 1 <= sdk <= 1000):
        raise Invalid("app.sdk must be an Android API level")
    reports = payload.get("reports")
    if not isinstance(reports, list) or not reports:
        raise Invalid("reports must be a non-empty list")
    if len(reports) > MAX_REPORTS:
        raise Invalid(f"at most {MAX_REPORTS} reports per upload")
    return version, sdk, reports


class Api:
    """The WSGI callable. One SQLite connection, serialised by a lock: traffic is tiny."""

    def __init__(self, db_path: str, releases: Releases | None = None):
        self.db_path = db_path
        self.conn = db.connect(db_path)
        self.lock = threading.Lock()
        # Kept beside the database: the one directory the service may write.
        self.releases = releases or Releases(os.path.join(os.path.dirname(os.path.abspath(db_path)), "latest.json"))

    def __call__(self, environ, start_response):
        method = environ.get("REQUEST_METHOD", "GET")
        path = environ.get("PATH_INFO", "")
        try:
            if path == "/v1/health":
                if method not in ("GET", "HEAD"):
                    return self.reply(start_response, 405, {"error": "method not allowed"}, allow="GET")
                return self.reply(start_response, 200, {"ok": True, "schema": db.schema_version(self.conn)})
            if path == "/v1/reports":
                if method != "POST":
                    return self.reply(start_response, 405, {"error": "method not allowed"}, allow="POST")
                return self.upload(environ, start_response)
            if path == "/v1/latest":
                if method not in ("GET", "HEAD"):
                    return self.reply(start_response, 405, {"error": "method not allowed"}, allow="GET")
                release = self.releases.latest()
                if release is None:
                    return self.reply(start_response, 503, {"error": "the latest release is not known yet"})
                return self.reply(start_response, 200, release)
            return self.reply(start_response, 404, {"error": "not found"})
        except Invalid as e:
            return self.reply(start_response, 400, {"error": str(e)})
        except sqlite3.Error:
            return self.reply(start_response, 503, {"error": "storage unavailable"})

    def upload(self, environ, start_response):
        if not environ.get("CONTENT_TYPE", "").lower().startswith("application/json"):
            return self.reply(start_response, 415, {"error": "send application/json"})
        try:
            length = int(environ.get("CONTENT_LENGTH") or 0)
        except ValueError:
            raise Invalid("bad Content-Length")
        if length <= 0:
            raise Invalid("empty body")
        if length > MAX_BODY:
            return self.reply(start_response, 413, {"error": f"body larger than {MAX_BODY} bytes"})
        body = environ["wsgi.input"].read(length)
        version, sdk, raw_reports = parse_upload(body)
        if os.path.exists(self.db_path) and os.path.getsize(self.db_path) > MAX_DB_BYTES:
            return self.reply(start_response, 507, {"error": "report store is full"})
        accepted, duplicate, rejected = [], [], []
        with self.lock:
            self.conn.execute("BEGIN IMMEDIATE")
            try:
                for raw in raw_reports:
                    try:
                        report = validate_report(raw)
                    except Invalid as e:
                        raw_id = raw.get("id") if isinstance(raw, dict) else None
                        rejected.append({"id": raw_id if isinstance(raw_id, str) else None, "error": str(e)})
                        continue
                    stored = db.insert_report(self.conn, report, report["app"] or version, sdk)
                    (accepted if stored else duplicate).append(report["id"])
                self.conn.execute("COMMIT")
            except Exception:
                self.conn.execute("ROLLBACK")
                raise
        return self.reply(start_response, 200, {"accepted": accepted, "duplicate": duplicate, "rejected": rejected})

    @staticmethod
    def reply(start_response, status: int, payload: dict, allow: str | None = None):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        reasons = {200: "OK", 400: "Bad Request", 404: "Not Found", 405: "Method Not Allowed",
                   413: "Payload Too Large", 415: "Unsupported Media Type", 503: "Service Unavailable",
                   507: "Insufficient Storage"}
        headers = [
            ("Content-Type", "application/json; charset=utf-8"),
            ("Content-Length", str(len(body))),
            ("Cache-Control", "no-store"),
            ("X-Content-Type-Options", "nosniff"),
        ]
        if allow:
            headers.append(("Allow", allow))
        start_response(f"{status} {reasons.get(status, 'Error')}", headers)
        return [body]
