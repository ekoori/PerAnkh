"""Minimal ElevenLabs client for the build tools.

The API key is read from a file by path and never printed, logged or written anywhere else.
Resolution order: $ELEVENLABS_API_KEY, then --key-file, then $ELEVENLABS_KEY_FILE, then
~/dev/11labs/.env — a file outside this repository. It may hold a bare key or KEY=value lines.
"""
from __future__ import annotations

import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

DEFAULT_KEY_FILE = os.environ.get("ELEVENLABS_KEY_FILE") or os.path.join(os.path.expanduser("~"), "dev", "11labs", ".env")
API = "https://api.elevenlabs.io"


def load_key(key_file: str | None = None) -> str:
    env = os.environ.get("ELEVENLABS_API_KEY")
    if env:
        return env.strip()
    path = key_file or DEFAULT_KEY_FILE
    with open(path, encoding="utf-8") as f:
        text = f.read()
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        m = re.match(r"^(?:export\s+)?([A-Z0-9_]+)\s*=\s*(.+)$", line)
        if m:
            if "ELEVEN" in m.group(1) or m.group(1) in ("XI_API_KEY", "API_KEY"):
                return m.group(2).strip().strip("'\"")
            continue
        return line  # a bare key
    raise SystemExit(f"no ElevenLabs key found in {path}")


# Statuses that mean "not now" rather than "no": rate limits and a busy or failing server.
RETRY_STATUS = {429, 500, 502, 503, 504}


def _send(req: urllib.request.Request, what: str, attempts: int = 6) -> bytes:
    """Sends a request, retrying network failures and busy-server answers with growing pauses.

    Any other HTTP error is the server's answer and is raised at once.
    """
    for attempt in range(attempts):
        last = attempt == attempts - 1
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", errors="replace")[:300]
            if e.code not in RETRY_STATUS or last:
                raise RuntimeError(f"{what} -> HTTP {e.code}: {detail}") from None
        except (urllib.error.URLError, TimeoutError, ConnectionError) as e:
            if last:
                raise RuntimeError(f"{what} -> network error: {e}") from None
        time.sleep(5 * 2 ** attempt)
    raise AssertionError("unreachable")


class Client:
    def __init__(self, key: str):
        self._key = key

    def _request(self, method: str, path: str, body: dict | None = None,
                 accept: str = "application/json") -> bytes:
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(API + path, data=data, method=method)
        req.add_header("xi-api-key", self._key)
        req.add_header("accept", accept)
        if data is not None:
            req.add_header("content-type", "application/json")
        return _send(req, f"{method} {path}")

    def get(self, path: str) -> object:
        return json.loads(self._request("GET", path))

    def tts(self, voice_id: str, text: str, model_id: str, voice_settings: dict,
            output_format: str = "mp3_44100_128", language_code: str | None = None,
            seed: int | None = None) -> bytes:
        body: dict = {"text": text, "model_id": model_id, "voice_settings": voice_settings}
        if language_code:
            body["language_code"] = language_code
        if seed is not None:
            body["seed"] = seed
        return self._request(
            "POST", f"/v1/text-to-speech/{voice_id}?output_format={output_format}",
            body, accept="audio/mpeg",
        )

    def tts_timed(self, voice_id: str, text: str, model_id: str, voice_settings: dict,
                  output_format: str = "mp3_44100_128", seed: int | None = None) -> dict:
        """Speech plus the start and end time of every character, for cutting a word out of a phrase.

        Returns the API's JSON: `audio_base64` and `alignment` with `characters`,
        `character_start_times_seconds` and `character_end_times_seconds`.
        """
        body: dict = {"text": text, "model_id": model_id, "voice_settings": voice_settings}
        if seed is not None:
            body["seed"] = seed
        return json.loads(self._request(
            "POST", f"/v1/text-to-speech/{voice_id}/with-timestamps?output_format={output_format}", body,
        ))

    def transcribe(self, audio: bytes, model_id: str = "scribe_v1",
                   language_code: str | None = "en") -> str:
        """Speech-to-text, used only as a QA check that a clip says roughly what was asked."""
        boundary = "----pentalingo7b1f"
        parts: list[bytes] = []

        def field(name: str, value: str) -> None:
            parts.append(
                f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n"
                f"{value}\r\n".encode("utf-8")
            )

        field("model_id", model_id)
        if language_code:
            field("language_code", language_code)
        parts.append(
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
            f"filename=\"clip.mp3\"\r\nContent-Type: audio/mpeg\r\n\r\n".encode("utf-8")
            + audio + b"\r\n"
        )
        parts.append(f"--{boundary}--\r\n".encode("utf-8"))
        req = urllib.request.Request(API + "/v1/speech-to-text", data=b"".join(parts),
                                     method="POST")
        req.add_header("xi-api-key", self._key)
        req.add_header("content-type", f"multipart/form-data; boundary={boundary}")
        return json.loads(_send(req, "speech-to-text")).get("text", "")


def main(argv: list[str]) -> None:
    client = Client(load_key())
    what = argv[1] if len(argv) > 1 else "usage"
    if what == "models":
        for m in client.get("/v1/models"):
            langs = [l.get("language_id") for l in m.get("languages", [])]
            print(f"{m['model_id']:<32} tts={m.get('can_do_text_to_speech')} "
                  f"maxchars={m.get('max_characters_request_subscribed_user')} "
                  f"langs={len(langs)} name={m.get('name')!r}")
            print(f"    {m.get('description', '')[:200]}")
    elif what == "voices":
        data = client.get("/v2/voices?page_size=100")
        for v in data.get("voices", []):
            labels = v.get("labels") or {}
            print(f"{v['voice_id']}  {v['name']:<34} {v.get('category', ''):<12} "
                  f"{labels.get('gender', '')} {labels.get('age', '')} {labels.get('accent', '')} "
                  f"{labels.get('description', '') or labels.get('descriptive', '')} "
                  f"{labels.get('use_case', '') or labels.get('use case', '')}")
    elif what == "usage":
        s = client.get("/v1/user/subscription")
        print(f"tier={s.get('tier')} used={s.get('character_count')} "
              f"limit={s.get('character_limit')} resets={s.get('next_character_count_reset_unix')}")
    else:
        raise SystemExit(f"unknown command {what}")


if __name__ == "__main__":
    main(sys.argv)
