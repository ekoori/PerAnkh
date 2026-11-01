"""Tests for BushLines' plant identification route, with Pl@ntNet stood in for by a function.

    cd server && python -m unittest discover -s tests
"""
from __future__ import annotations

import io
import json
import os
import tempfile
import unittest

from perankh import db
from perankh.app import Api
from perankh.identify import MAX_BODY, Identifier, trim

KEY = "test-key-0123456789abcdef"
BOUNDARY = "----bushlines-test"
TYPE = f"multipart/form-data; boundary={BOUNDARY}"

# What Pl@ntNet sends back, cut down from its documentation.
PLANTNET_REPLY = {
    "query": {"project": "k-australia", "images": ["5440fc78"], "organs": ["auto"]},
    "predictedOrgans": [{"image": "5440fc78", "filename": "1.jpg", "organ": "flower", "score": 0.66}],
    "bestMatch": "Hardenbergia violacea (Schneev.) Stearn",
    "results": [
        {"score": 0.90734, "species": {
            "scientificNameWithoutAuthor": "Hardenbergia violacea", "scientificNameAuthorship": "(Schneev.) Stearn",
            "genus": {"scientificNameWithoutAuthor": "Hardenbergia"},
            "family": {"scientificNameWithoutAuthor": "Fabaceae"},
            "commonNames": ["Purple coral-pea", "False sarsaparilla"],
            "scientificName": "Hardenbergia violacea (Schneev.) Stearn"},
         "gbif": {"id": "2952398"}, "powo": {"id": "509395-1"}},
        {"score": 0.031, "species": {
            "scientificNameWithoutAuthor": "Kennedia rubicunda", "scientificNameAuthorship": "(Schneev.) Vent.",
            "family": {"scientificNameWithoutAuthor": "Fabaceae"}, "commonNames": []},
         "gbif": {"id": "2952103"}},
    ],
    "version": "2025-01-17 (7.3)",
    "remainingIdentificationRequests": 497,
}


def photos(n: int = 1) -> bytes:
    parts = []
    for i in range(n):
        parts.append(f"--{BOUNDARY}\r\nContent-Disposition: form-data; name=\"organs\"\r\n\r\nflower\r\n".encode())
        parts.append(f"--{BOUNDARY}\r\nContent-Disposition: form-data; name=\"images\"; filename=\"{i}.jpg\"\r\n"
                     "Content-Type: image/jpeg\r\n\r\n".encode() + b"\xff\xd8\xff" + bytes(range(256)) * 4 + b"\r\n")
    parts.append(f"--{BOUNDARY}--\r\n".encode())
    return b"".join(parts)


class FakePlantNet:
    """Answers as Pl@ntNet would, and remembers what it was sent."""

    def __init__(self, status=200, body=PLANTNET_REPLY, raises: Exception | None = None):
        self.status, self.body, self.raises = status, body, raises
        self.calls = []

    def __call__(self, body: bytes, content_type: str, key: str):
        self.calls.append((body, content_type, key))
        if self.raises:
            raise self.raises
        raw = self.body if isinstance(self.body, bytes) else json.dumps(self.body).encode()
        return self.status, raw


class IdentifyTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.key_file = os.path.join(self.dir.name, "plantnet.key")
        with open(self.key_file, "w", encoding="utf-8") as f:
            f.write(KEY + "\n")
        self.now = 1_793_600_000.0  # 2026-11-02, UTC
        self.plantnet = FakePlantNet()
        self.make_api(limit=450)

    def make_api(self, limit):
        if hasattr(self, "api"):
            self.api.conn.close()
        self.api = Api(os.path.join(self.dir.name, "reports.db"),
                       identifier=Identifier(self.key_file, send=self.plantnet, limit=limit),
                       clock=lambda: self.now)

    def tearDown(self):
        self.api.conn.close()
        self.dir.cleanup()

    def call(self, body: bytes | None = None, query="app=bushlines", method="POST", content_type=TYPE):
        body = photos() if body is None else body
        environ = {"REQUEST_METHOD": method, "PATH_INFO": "/v1/identify", "QUERY_STRING": query,
                   "CONTENT_TYPE": content_type, "CONTENT_LENGTH": str(len(body)), "wsgi.input": io.BytesIO(body)}
        seen = {}

        def start_response(status, headers):
            seen["status"] = int(status.split()[0])

        raw = b"".join(self.api(environ, start_response))
        # Whatever the outcome, the key never leaves the server.
        self.assertNotIn(KEY.encode(), raw)
        return seen["status"], json.loads(raw)

    def test_passes_the_photos_through_and_trims_the_answer(self):
        body = photos(3)
        status, payload = self.call(body)
        self.assertEqual(200, status)
        sent, content_type, key = self.plantnet.calls[0]
        self.assertEqual(body, sent)
        self.assertEqual(TYPE, content_type)
        self.assertEqual(KEY, key)
        self.assertTrue(payload["found"])
        self.assertEqual(["flower"], payload["organs"])
        best = payload["results"][0]
        self.assertEqual({"score": 0.9073, "name": "Hardenbergia violacea", "authorship": "(Schneev.) Stearn",
                          "family": "Fabaceae", "commonNames": ["Purple coral-pea", "False sarsaparilla"],
                          "gbif": "2952398", "powo": "509395-1"}, best)
        self.assertIsNone(payload["results"][1]["powo"])
        self.assertNotIn("remainingIdentificationRequests", payload)

    def test_only_bushlines_may_identify(self):
        self.assertEqual(400, self.call(query="app=perankh")[0])
        self.assertEqual(400, self.call(query="")[0])
        self.assertEqual(400, self.call(query="app=bushlines&app=perankh")[0])
        self.assertEqual([], self.plantnet.calls)

    def test_refusals(self):
        self.assertEqual(405, self.call(method="GET")[0])
        self.assertEqual(415, self.call(content_type="application/json")[0])
        self.assertEqual(415, self.call(content_type="multipart/form-data")[0])
        self.assertEqual(400, self.call(body=b"")[0])
        self.assertEqual(413, self.call(body=b"x" * (MAX_BODY + 1))[0])
        self.assertEqual([], self.plantnet.calls)

    def test_stops_for_the_day_at_the_limit(self):
        self.make_api(limit=2)
        self.assertEqual(200, self.call()[0])
        self.assertEqual(200, self.call()[0])
        status, payload = self.call()
        self.assertEqual(429, status)
        self.assertIn("tomorrow", payload["error"])
        self.assertEqual(2, len(self.plantnet.calls))
        self.now += 86_400  # the next UTC day
        self.assertEqual(200, self.call()[0])
        days = dict(self.api.conn.execute("SELECT day, count FROM identify_days").fetchall())
        self.assertEqual({"2026-11-02": 2, "2026-11-03": 1}, days)

    def test_without_a_key_it_is_not_set_up(self):
        os.remove(self.key_file)
        status, payload = self.call()
        self.assertEqual(503, status)
        self.assertIn("not set up", payload["error"])
        self.assertEqual([], self.plantnet.calls)

    def test_pl_ntnet_s_answers_become_this_api_s(self):
        cases = [
            (FakePlantNet(404, b'{"statusCode":404,"message":"Species not found"}'), 200),
            (FakePlantNet(400, b"bad"), 400),
            (FakePlantNet(401, b'{"message": "Invalid API key ' + KEY.encode() + b'"}'), 503),
            (FakePlantNet(429, b"quota"), 429),
            (FakePlantNet(500, b"boom"), 502),
            (FakePlantNet(200, b"not json"), 502),
            (FakePlantNet(raises=TimeoutError("timed out")), 504),
        ]
        for fake, expected in cases:
            self.plantnet = fake
            self.make_api(limit=450)
            status, payload = self.call()
            self.assertEqual(expected, status, fake.status)
        self.assertEqual({"found": False, "results": [], "organs": []},
                         Identifier(self.key_file, send=FakePlantNet(404, b"")).identify(b"", TYPE, KEY)[1])

    def test_reports_still_work_beside_it(self):
        status, payload = self.call()
        self.assertEqual(200, status)
        self.assertEqual(len(db.MIGRATIONS), db.schema_version(self.api.conn))

    def test_trim_survives_odd_answers(self):
        self.assertEqual({"found": False, "results": [], "organs": []}, trim({}))
        self.assertEqual({"found": False, "results": [], "organs": []}, trim([]))
        odd = trim({"results": [{"score": "high"}, {"species": {"scientificNameWithoutAuthor": "X y"}, "score": 0.5}]})
        self.assertEqual(["X y"], [r["name"] for r in odd["results"]])


if __name__ == "__main__":
    unittest.main()
