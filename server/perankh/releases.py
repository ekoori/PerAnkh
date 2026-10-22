"""The newest Per Ankh release, for apps that cannot reach GitHub themselves.

    GET /v1/latest -> {"tag_name": "v0.3.0", "html_url": "https://github.com/ekoori/PerAnkh/releases/tag/v0.3.0",
                       "published_at": "2026-11-02T10:00:00Z"}

The answer has the shape of GitHub's own release API, trimmed to what the app reads, so the app
reads both the same way. The service asks GitHub at most every half hour, keeps the last good
answer on disk, and goes on serving it while GitHub cannot be reached. Nothing about the phone
that asks is involved: the question to GitHub comes from this server.
"""
from __future__ import annotations

import json
import os
import re
import threading
import time
import urllib.request

GITHUB = "https://api.github.com/repos/ekoori/PerAnkh/releases/latest"
PAGES = "https://github.com/ekoori/PerAnkh/releases/"
# How long an answer is fresh, and how long to wait after GitHub fails before asking again.
FRESH = 30 * 60
RETRY = 5 * 60
TAG = re.compile(r"^v\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?$")


def from_github(timeout: float = 5.0):
    request = urllib.request.Request(GITHUB, headers={
        "Accept": "application/vnd.github+json",
        "User-Agent": "perankh-server",
        "X-GitHub-Api-Version": "2022-11-28",
    })
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read(1_000_000).decode("utf-8"))


def clean(raw) -> dict | None:
    """What the app needs from a release, if it is a full release of Per Ankh; else None."""
    if not isinstance(raw, dict) or raw.get("draft") or raw.get("prerelease"):
        return None
    tag, url, published = raw.get("tag_name"), raw.get("html_url"), raw.get("published_at")
    if not isinstance(tag, str) or not TAG.match(tag):
        return None
    if not isinstance(url, str) or not url.startswith(PAGES) or len(url) > 200:
        return None
    release = {"tag_name": tag, "html_url": url}
    if isinstance(published, str) and len(published) <= 40:
        release["published_at"] = published
    return release


class Releases:
    """The last good answer from GitHub, refreshed when stale, kept in a JSON file across restarts."""

    def __init__(self, path: str, fetch=from_github, clock=time.time):
        self.path = path
        self.fetch = fetch
        self.clock = clock
        self.lock = threading.Lock()
        self.known = self._load()
        self.next_try = 0.0

    def latest(self) -> dict | None:
        with self.lock:
            now = self.clock()
            stale = self.known is None or now - self.known["checked"] >= FRESH
            if stale and now >= self.next_try:
                try:
                    release = clean(self.fetch())
                except (OSError, ValueError):
                    release = None
                if release is not None:
                    self.known = {"release": release, "checked": now}
                    self._save()
                else:
                    self.next_try = now + RETRY
            return self.known["release"] if self.known else None

    def _load(self) -> dict | None:
        try:
            with open(self.path, encoding="utf-8") as f:
                known = json.load(f)
            release = clean(known.get("release"))
            return {"release": release, "checked": float(known["checked"])} if release else None
        except (OSError, ValueError, KeyError, TypeError, AttributeError):
            return None

    def _save(self) -> None:
        temp = self.path + ".tmp"
        try:
            with open(temp, "w", encoding="utf-8") as f:
                json.dump(self.known, f)
            os.replace(temp, self.path)
        except OSError:
            pass  # Serving from memory is enough; the file only spares GitHub a question after a restart.
