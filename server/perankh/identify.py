"""Plant identification for BushLines, through Pl@ntNet, with the key kept on this server.

    POST /v1/identify?app=bushlines     body: multipart/form-data, `images` and `organs` as
                                        Pl@ntNet takes them (up to 5 photos of one plant)
      -> {"found": true, "results": [{"score", "name", "authorship", "family", "commonNames",
                                      "gbif", "powo"}], "organs": [...]}

The photos are passed to Pl@ntNet's Australia flora unchanged and held in memory only: nothing
about them is stored or logged. The key is read from a file only this service and root can read,
and never appears in a reply. A daily count (UTC) stops before the free plan's 500 a day, so the
account is never over its quota. See server/README.md.
"""
from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request

PLANTNET = "https://my-api.plantnet.org/v2/identify/"
PROJECT = "k-australia"
# The apps that may identify. Per Ankh has no use for it.
APPS = ("bushlines",)
# Five photos of up to ~1 MB each, and the form around them.
MAX_BODY = 6 * 1024 * 1024
# Pl@ntNet's free plan allows 500 a day; the rest is headroom for testing by hand.
DAILY_LIMIT = 450
MAX_RESULTS = 5
TIMEOUT = 45.0
KEY_FILE = "/etc/perankh/plantnet.key"


def to_plantnet(body: bytes, content_type: str, key: str) -> tuple[int, bytes]:
    """Sends the photos on as they came; returns Pl@ntNet's status and body. Raises OSError when
    Pl@ntNet cannot be reached in time."""
    query = urllib.parse.urlencode({"api-key": key, "nb-results": MAX_RESULTS, "lang": "en"})
    request = urllib.request.Request(
        f"{PLANTNET}{PROJECT}?{query}", data=body, method="POST",
        headers={"Content-Type": content_type, "Accept": "application/json", "User-Agent": "perankh-server"},
    )
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            return response.status, response.read(2_000_000)
    except urllib.error.HTTPError as e:
        return e.code, b""


def trim(raw) -> dict:
    """What the app shows: the matches, best first, and the organs Pl@ntNet saw."""
    results = []
    for r in (raw.get("results") or [])[:MAX_RESULTS] if isinstance(raw, dict) else []:
        species = r.get("species") if isinstance(r, dict) else None
        if not isinstance(species, dict) or not isinstance(species.get("scientificNameWithoutAuthor"), str):
            continue
        score = r.get("score")
        family = species.get("family") if isinstance(species.get("family"), dict) else {}
        results.append({
            "score": round(float(score), 4) if isinstance(score, (int, float)) else 0.0,
            "name": species["scientificNameWithoutAuthor"][:200],
            "authorship": str(species.get("scientificNameAuthorship") or "")[:200],
            "family": str(family.get("scientificNameWithoutAuthor") or "")[:100],
            "commonNames": [c[:100] for c in species.get("commonNames") or [] if isinstance(c, str)][:5],
            "gbif": str((r.get("gbif") or {}).get("id") or "")[:20] or None,
            "powo": str((r.get("powo") or {}).get("id") or "")[:40] or None,
        })
    organs = [o.get("organ") for o in (raw.get("predictedOrgans") or []) if isinstance(o, dict)] if isinstance(raw, dict) else []
    return {"found": bool(results), "results": results, "organs": [o for o in organs if isinstance(o, str)]}


class Identifier:
    """Reads the key when asked, so a key put in place later needs no restart; sends through [send]."""

    def __init__(self, key_file: str | None = None, send=to_plantnet, limit: int = DAILY_LIMIT):
        self.key_file = key_file or os.environ.get("PERANKH_PLANTNET_KEY_FILE", KEY_FILE)
        self.send = send
        self.limit = limit

    def key(self) -> str | None:
        try:
            with open(self.key_file, encoding="utf-8") as f:
                key = f.read().strip()
        except OSError:
            return None
        return key or None

    def identify(self, body: bytes, content_type: str, key: str) -> tuple[int, dict]:
        """Pl@ntNet's answer as this API's status and reply. Never echoes Pl@ntNet's own errors,
        which could carry the request."""
        try:
            status, raw = self.send(body, content_type, key)
        except OSError:
            return 504, {"error": "Pl@ntNet did not answer in time. Try again."}
        if status == 200:
            try:
                return 200, trim(json.loads(raw.decode("utf-8")))
            except (UnicodeDecodeError, ValueError):
                return 502, {"error": "Pl@ntNet sent an answer this server did not understand."}
        if status == 404:  # Pl@ntNet's "Species not found": nothing in the photos looked like a plant
            return 200, {"found": False, "results": [], "organs": []}
        if status in (400, 413, 415):
            return 400, {"error": "Pl@ntNet could not read the photos."}
        if status == 429:
            return 429, {"error": "Identification is resting until tomorrow."}
        if status in (401, 403):
            return 503, {"error": "Identification is not available right now."}
        return 502, {"error": f"Pl@ntNet is having trouble ({status}). Try again later."}
