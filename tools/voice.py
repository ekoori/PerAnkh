"""Generate the course's pronunciation clips with ElevenLabs, and check them.

    gradlew :content:run                         # writes tools/out/speech.json from the course
    python -X utf8 tools/voice.py plan           # what is missing, and what it would cost
    python -X utf8 tools/voice.py generate       # make the missing clips (asks first)
    python -X utf8 tools/voice.py qa             # transcribe every clip and flag mismatches
    python -X utf8 tools/voice.py reroll         # retake flagged clips with other seeds, keep the best

Clips are content-addressed: the file name is a hash of the text, voice, model and settings, so
re-running never re-bills a clip that already exists, and changing any of those regenerates only
what changed. The app reads app/src/main/assets/audio/manifest.json, which maps item ids to files.

The key is read by path (see eleven.py) and never printed.
"""
from __future__ import annotations

import difflib
import hashlib
import json
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from eleven import Client, load_key  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEECH = os.path.join(ROOT, "tools", "out", "speech.json")
AUDIO = os.path.join(ROOT, "app", "src", "main", "assets", "audio")
MANIFEST = os.path.join(AUDIO, "manifest.json")
QA_REPORT = os.path.join(ROOT, "tools", "out", "qa.json")
# Text to the generation seed that gave the best take; committed, so builds are reproducible.
SEEDS_FILE = os.path.join(ROOT, "tools", "voice_seeds.json")

# George: a warm British storyteller, clear on unfamiliar words. Eleven v4 is the newest model
# on the account (October 2026); no model "knows" Middle Egyptian, so it reads Egyptological
# respellings ("nefer"), which is how the language is pronounced in classrooms anyway.
VOICE_ID = "JBFqnCBsd6RMkjVDRZzb"
MODEL_ID = "eleven_v4"
SETTINGS = {"stability": 0.5, "similarity_boost": 0.75}
# Unspent characters this tool must leave on the account, for the user's other projects.
RESERVE = 15_000


def spoken_text(text: str) -> str:
    """What is actually sent: a full stop gives single words a natural falling intonation."""
    t = text.strip()
    return t if t[-1:] in ".!?" else t + "."


def load_seeds() -> dict[str, int]:
    if not os.path.exists(SEEDS_FILE):
        return {}
    with open(SEEDS_FILE, encoding="utf-8") as f:
        return json.load(f)


SEEDS = load_seeds()


def clip_key(text: str, seed: int | None = None) -> str:
    seed = SEEDS.get(text) if seed is None else seed
    spec = {"text": spoken_text(text), "voice": VOICE_ID, "model": MODEL_ID, "settings": SETTINGS}
    if seed is not None:
        spec["seed"] = seed
    blob = json.dumps(spec, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(blob.encode("utf-8")).hexdigest()[:32]


def synthesise(client: Client, text: str, seed: int | None = None) -> bytes:
    seed = SEEDS.get(text) if seed is None else seed
    for attempt in range(3):
        try:
            return client.tts(VOICE_ID, spoken_text(text), MODEL_ID, SETTINGS, seed=seed)
        except RuntimeError as e:
            print(f"  retry {attempt + 1}: {e}")
            time.sleep(2 + 3 * attempt)
    raise SystemExit(f"failed on {text!r}")


def load_lines() -> list[dict]:
    with open(SPEECH, encoding="utf-8") as f:
        return json.load(f)


def plan(lines: list[dict]) -> tuple[dict[str, str], list[str]]:
    """Item id to file name for every line, and the distinct texts still to generate."""
    manifest, missing = {}, []
    for line in lines:
        name = clip_key(line["text"]) + ".mp3"
        manifest[line["id"]] = name
        if not os.path.exists(os.path.join(AUDIO, name)) and line["text"] not in missing:
            missing.append(line["text"])
    return manifest, missing


def write_manifest(manifest: dict[str, str]) -> None:
    os.makedirs(AUDIO, exist_ok=True)
    present = {k: v for k, v in manifest.items() if os.path.exists(os.path.join(AUDIO, v))}
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        json.dump(present, f, ensure_ascii=False, indent=1, sort_keys=True)
    print(f"manifest: {len(present)} of {len(manifest)} items have a clip")


def cmd_plan() -> None:
    lines = load_lines()
    _, missing = plan(lines)
    chars = sum(len(spoken_text(t)) for t in missing)
    print(f"{len(lines)} spoken items, {len(missing)} distinct texts missing, {chars} characters to generate")
    for t in missing[:20]:
        print("  ", t)


def cmd_generate(limit: int | None, assume_yes: bool) -> None:
    lines = load_lines()
    manifest, missing = plan(lines)
    if limit is not None:
        missing = missing[:limit]
    chars = sum(len(spoken_text(t)) for t in missing)
    client = Client(load_key())
    sub = client.get("/v1/user/subscription")
    left = sub["character_limit"] - sub["character_count"]
    print(f"to generate: {len(missing)} clips, {chars} characters; account has {left} left this cycle")
    if chars > left - RESERVE:
        raise SystemExit(f"refusing: would leave less than {RESERVE} characters")
    if not missing:
        write_manifest(manifest)
        return
    if not assume_yes and input("generate? [y/N] ").strip().lower() != "y":
        return
    os.makedirs(AUDIO, exist_ok=True)
    for i, text in enumerate(missing, 1):
        name = clip_key(text) + ".mp3"
        audio = synthesise(client, text)
        with open(os.path.join(AUDIO, name), "wb") as f:
            f.write(audio)
        print(f"  [{i}/{len(missing)}] {text!r} -> {name} ({len(audio)} bytes)")
    write_manifest(manifest)
    sub = client.get("/v1/user/subscription")
    print(f"account now: {sub['character_count']} / {sub['character_limit']} used")


def norm(s: str) -> str:
    return re.sub(r"[^a-z ]", "", s.lower().replace("-", " ")).strip()


# English spellings of the consonants the respellings start with, longest first.
_ONSETS = [("tch", "tch"), ("ch", "tch"), ("dj", "dj"), ("kh", "kh"), ("sh", "sh"), ("ph", "f"),
           ("th", "t"), ("j", "dj"), ("c", "k"), ("q", "k"), ("z", "s")]


def onset(text: str) -> str:
    words = norm(text).split()
    if not words:
        return ""
    w = words[0]
    for spelling, sound in _ONSETS:
        if w.startswith(spelling):
            return sound
    return "" if w[0] in "aeiou" else w[0]


def score(want: str, heard: str) -> float:
    """How well a transcript matches the respelling it should be.

    A transcriber knows English, not Egyptological respellings, so it writes what it hears as
    the nearest English: a British "beh" comes back as "Bear". For single syllables the vowel is
    unreliable evidence and the consonant is what matters, so they are judged on their opening
    consonant; longer texts on overall similarity.
    """
    w, h = norm(want), norm(heard)
    if not h:
        return 0.0
    base = difflib.SequenceMatcher(None, w, h).ratio()
    if len(w.replace(" ", "")) <= 5:
        if onset(w) != onset(h):
            return min(base, 0.4)
        return max(base, 0.8) if onset(w) else base
    return base


def cmd_qa(only_missing: bool = False) -> None:
    """Transcribe each distinct clip and compare with what it was meant to say."""
    lines = load_lines()
    manifest, _ = plan(lines)
    by_file: dict[str, list[str]] = {}
    texts: dict[str, str] = {}
    for line in lines:
        name = manifest[line["id"]]
        by_file.setdefault(name, []).append(line["id"])
        texts[name] = line["text"]
    report = {}
    if os.path.exists(QA_REPORT):
        with open(QA_REPORT, encoding="utf-8") as f:
            report = json.load(f)
    client = Client(load_key())
    flagged = []
    for name, ids in sorted(by_file.items(), key=lambda kv: kv[1][0]):
        path = os.path.join(AUDIO, name)
        if not os.path.exists(path):
            continue
        if only_missing and name in report:
            heard = report[name]["heard"]
        else:
            with open(path, "rb") as f:
                heard = client.transcribe(f.read(), "scribe_v2", "en")
        want = texts[name]
        value = score(want, heard)
        report[name] = {"ids": ids, "want": want, "heard": heard, "score": round(value, 2)}
        mark = "ok " if value >= 0.6 else "?? "
        if value < 0.6:
            flagged.append(name)
        print(f"{mark}{value:.2f}  {want!r:34} heard {heard!r}")
    os.makedirs(os.path.dirname(QA_REPORT), exist_ok=True)
    with open(QA_REPORT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(report, f, ensure_ascii=False, indent=1, sort_keys=True)
    print(f"{len(report)} clips checked, {len(flagged)} flagged for a listen")


def cmd_reroll(seeds: tuple[int, ...] = (11, 23, 37)) -> None:
    """Retake each flagged clip with a few other seeds and keep whichever take scores best."""
    with open(QA_REPORT, encoding="utf-8") as f:
        report = json.load(f)
    client = Client(load_key())
    chosen = dict(SEEDS)
    for name, entry in sorted(report.items(), key=lambda kv: kv[1]["want"]):
        if entry["score"] >= 0.6:
            continue
        text = entry["want"]
        best = (entry["score"], SEEDS.get(text), name, entry["heard"])
        for seed in seeds:
            audio = synthesise(client, text, seed)
            heard = client.transcribe(audio, "scribe_v2", "en")
            value = score(text, heard)
            candidate = clip_key(text, seed) + ".mp3"
            with open(os.path.join(AUDIO, candidate), "wb") as f:
                f.write(audio)
            print(f"  {text!r:12} seed {seed:>3}: heard {heard!r} -> {value:.2f}")
            if value > best[0]:
                best = (value, seed, candidate, heard)
            if value >= 0.8:
                break
        value, seed, keep, heard = best
        if seed is not None:
            chosen[text] = seed
        # Drop the takes that lost, so only clips the manifest points at ship in the APK.
        for other in {name} | {clip_key(text, s) + ".mp3" for s in seeds}:
            if other != keep and os.path.exists(os.path.join(AUDIO, other)):
                os.remove(os.path.join(AUDIO, other))
        print(f"{'ok ' if value >= 0.6 else '?? '}{text!r}: kept {keep} ({value:.2f}, heard {heard!r})")
    with open(SEEDS_FILE, "w", encoding="utf-8", newline="\n") as f:
        json.dump(chosen, f, ensure_ascii=False, indent=1, sort_keys=True)
    SEEDS.clear()
    SEEDS.update(chosen)
    write_manifest(plan(load_lines())[0])


def cmd_prune() -> None:
    """Delete clips nothing points at any more, so superseded takes do not ship in the APK."""
    manifest, _ = plan(load_lines())
    keep = set(manifest.values()) | {"manifest.json"}
    gone = [n for n in os.listdir(AUDIO) if n not in keep]
    for n in gone:
        os.remove(os.path.join(AUDIO, n))
    write_manifest(manifest)
    print(f"pruned {len(gone)} unused clips")


if __name__ == "__main__":
    args = sys.argv[1:]
    cmd = args[0] if args else "plan"
    if cmd == "plan":
        cmd_plan()
    elif cmd == "generate":
        lim = None
        if "--limit" in args:
            lim = int(args[args.index("--limit") + 1])
        cmd_generate(lim, "--yes" in args)
    elif cmd == "qa":
        cmd_qa("--cached" in args)
    elif cmd == "reroll":
        cmd_reroll()
    elif cmd == "prune":
        cmd_prune()
    elif cmd == "manifest":
        write_manifest(plan(load_lines())[0])
    else:
        raise SystemExit(f"unknown command {cmd}")
