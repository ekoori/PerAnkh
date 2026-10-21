"""Tests for the report API and the triage tool. Standard library only:

    cd server && python -m unittest discover -s tests
"""
from __future__ import annotations

import contextlib
import io
import json
import os
import tempfile
import unittest
import uuid

from perankh import cli, db
from perankh.app import MAX_BODY, MAX_REPORTS, Api


def report(**overrides) -> dict:
    base = {
        "id": str(uuid.uuid4()),
        "at": db.now_ms(),
        "category": "Pronunciation",
        "note": "The t is swallowed at the end",
        "context": {"screen": "lesson", "lesson": "l21 · The great gods", "exercise": "TEACH", "item": "s:Q1"},
    }
    base.update(overrides)
    return base


class ApiTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.path = os.path.join(self.dir.name, "reports.db")
        self.api = Api(self.path)

    def tearDown(self):
        self.api.conn.close()
        self.dir.cleanup()

    def call(self, method, path, body: bytes | None = None, content_type="application/json"):
        environ = {"REQUEST_METHOD": method, "PATH_INFO": path, "wsgi.input": io.BytesIO(body or b"")}
        if body is not None:
            environ["CONTENT_TYPE"] = content_type
            environ["CONTENT_LENGTH"] = str(len(body))
        seen = {}

        def start_response(status, headers):
            seen["status"] = int(status.split()[0])
            seen["headers"] = dict(headers)

        payload = json.loads(b"".join(self.api(environ, start_response)))
        return seen["status"], payload

    def upload(self, *reports, app=None):
        body = json.dumps({"app": app or {"version": "0.2.0 (200)", "sdk": 34}, "reports": list(reports)}).encode()
        return self.call("POST", "/v1/reports", body)

    def test_health(self):
        status, payload = self.call("GET", "/v1/health")
        self.assertEqual(200, status)
        self.assertEqual({"ok": True, "schema": 1}, payload)

    def test_accepts_and_stores_a_report(self):
        r = report()
        status, payload = self.upload(r)
        self.assertEqual(200, status)
        self.assertEqual([r["id"]], payload["accepted"])
        row = self.api.conn.execute("SELECT * FROM reports").fetchone()
        self.assertEqual("Pronunciation", row["category"])
        self.assertEqual("s:Q1", row["item"])
        self.assertEqual("0.2.0 (200)", row["app_version"])
        self.assertEqual(34, row["android_sdk"])
        self.assertEqual("new", row["status"])
        self.assertEqual(r["context"], json.loads(row["context"]))

    def test_a_report_keeps_the_version_it_was_written_in(self):
        self.upload(report(app="0.1.0 (1)"))
        self.assertEqual("0.1.0 (1)", self.api.conn.execute("SELECT app_version FROM reports").fetchone()[0])

    def test_uploading_twice_stores_once(self):
        r = report()
        self.upload(r)
        status, payload = self.upload(r)
        self.assertEqual(200, status)
        self.assertEqual([], payload["accepted"])
        self.assertEqual([r["id"]], payload["duplicate"])
        self.assertEqual(1, self.api.conn.execute("SELECT COUNT(*) FROM reports").fetchone()[0])

    def test_a_bad_report_does_not_sink_the_batch(self):
        good, bad = report(), report(id="not-a-uuid")
        status, payload = self.upload(good, bad)
        self.assertEqual(200, status)
        self.assertEqual([good["id"]], payload["accepted"])
        self.assertEqual("not-a-uuid", payload["rejected"][0]["id"])

    def test_validation(self):
        cases = [
            report(at=0), report(at=db.now_ms() + 3 * 86_400_000), report(category=""),
            report(category="x" * 41), report(note="x" * 4001), report(context=["not", "an", "object"]),
            report(context={"k": 5}), report(context={str(i): "v" for i in range(121)}),
        ]
        status, payload = self.upload(*cases)
        self.assertEqual(200, status)
        self.assertEqual(len(cases), len(payload["rejected"]))
        self.assertEqual([], payload["accepted"])

    def test_control_characters_are_removed(self):
        r = report(note="a\x00b\x1bc\nd")
        self.upload(r)
        self.assertEqual("abc\nd", self.api.conn.execute("SELECT note FROM reports").fetchone()[0])

    def test_refusals(self):
        self.assertEqual(404, self.call("GET", "/admin")[0])
        self.assertEqual(405, self.call("GET", "/v1/reports")[0])
        self.assertEqual(405, self.call("POST", "/v1/health", b"{}")[0])
        self.assertEqual(415, self.call("POST", "/v1/reports", b"{}", content_type="text/plain")[0])
        self.assertEqual(400, self.call("POST", "/v1/reports", b"{not json")[0])
        self.assertEqual(400, self.call("POST", "/v1/reports", b'{"reports": []}')[0])
        self.assertEqual(413, self.call("POST", "/v1/reports", b"x" * (MAX_BODY + 1))[0])
        too_many = [report() for _ in range(MAX_REPORTS + 1)]
        self.assertEqual(400, self.upload(*too_many)[0])
        self.assertEqual(400, self.upload(report(), app={"version": "1", "sdk": "34"})[0])


class TriageTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.path = os.path.join(self.dir.name, "reports.db")
        conn = db.connect(self.path)
        self.first, self.second = report(), report(note="same again")
        for r in (self.first, self.second):
            db.insert_report(conn, r, "0.2.0 (200)", 34)
        conn.close()

    def tearDown(self):
        self.dir.cleanup()

    def run_cli(self, *args) -> str:
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            cli.main(["--db", self.path, *args])
        return out.getvalue()

    def test_list_show_resolve(self):
        listing = self.run_cli("list")
        self.assertIn(self.first["id"][:8], listing)
        shown = self.run_cli("show", self.first["id"][:8])
        self.assertIn("The t is swallowed", shown)
        self.assertIn("l21 · The great gods", shown)
        self.run_cli("resolve", self.first["id"][:8], "--in", "0.2.1", "--note", "re-recorded")
        self.run_cli("duplicate", self.second["id"][:8], "--of", self.first["id"][:8])
        self.assertEqual("no reports\n", self.run_cli("list"))
        exported = json.loads(self.run_cli("export", "--all"))
        by_id = {r["id"]: r for r in exported}
        self.assertEqual("fixed", by_id[self.first["id"]]["status"])
        self.assertEqual("0.2.1", by_id[self.first["id"]]["resolved_in"])
        self.assertEqual(self.first["id"], by_id[self.second["id"]]["duplicate_of"])
        self.assertIn("fixed: re-recorded; in 0.2.1", self.run_cli("show", self.first["id"][:8]))

    def test_prune_keeps_open_reports(self):
        self.run_cli("status", self.first["id"][:8], "wontfix")
        conn = db.connect(self.path)
        conn.execute("UPDATE reports SET updated_at = 0 WHERE id = ?", (self.first["id"],))
        conn.close()
        self.assertIn("deleted 1", self.run_cli("prune", "--closed-days", "30"))
        remaining = json.loads(self.run_cli("export", "--all"))
        self.assertEqual([self.second["id"]], [r["id"] for r in remaining])


if __name__ == "__main__":
    unittest.main()
