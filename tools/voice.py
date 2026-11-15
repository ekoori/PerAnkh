"""Record the course's pronunciation clips with ElevenLabs, checking each one by ear-proxy.

    gradlew :content:run                         # writes tools/out/speech.json from the course
    python -X utf8 tools/voice.py plan           # what is missing, and what it would cost
    python -X utf8 tools/voice.py generate       # record, check, retake; asks first
    python -X utf8 tools/voice.py generate --retry   # also give failed clips their remaining takes
    python -X utf8 tools/voice.py generate --only p:ptah5,c:future --takes 3   # retake these; 3 to choose from
    python -X utf8 tools/voice.py generate --retry --new-takes 2   # failed clips: at most 2 new takes each
    python -X utf8 tools/voice.py report         # every clip, what it should say, what was heard
    python -X utf8 tools/voice.py takes p:ptah5  # every recorded take of a clip, and which one ships
    python -X utf8 tools/voice.py pin p:ptah5 FILE.mp3   # ship this take, chosen by ear, whatever the check says
    python -X utf8 tools/voice.py rescore        # judge recorded takes again after a rule change; free
    python -X utf8 tools/voice.py prune          # move clips nothing points at out of the app

How a clip is made. The app's pronunciation engine (core-script Pronounce.kt) turns each
transliteration into the Egyptological pronunciation with its real consonants, and renders that
as fully vowelled Arabic script — the one input that makes a speech model produce ḥ, ꜥ, ḫ and q
reliably. Each clip lists candidate inputs and the Arabic letters a listener must hear.

A lone syllable is where even an Arabic voice slips: ḥm comes out as English "him", ḥḥ as a
laugh, a final ʿayin or kh fades away. So when a word fails on its own, it is said again inside a
short Arabic frame, "qul: …" ("say: …"), where the voice treats it as Arabic, and the word is cut
out of the frame at the pause before it, found from the character timings the API returns.

How a clip is checked. Each take is transcribed by ElevenLabs Scribe in Arabic, which has
letters for exactly the sounds that matter (ح ع خ ق ش ج), and, for words with a p, in English,
which can tell p from b. A framed take is also transcribed whole, where the Arabic context keeps
the transcriber from hearing a real word ("هم") in place of the nonsense one ("حم"). A take passes
when every hard consonant is heard; otherwise the next seed, candidate or frame is tried, and the
best take is kept.

Takes are content-addressed (text, voice, model, settings, seed, frame, language), so nothing is
billed twice. The key is read by path (see eleven.py) and never printed. ffmpeg cuts framed takes.

The check hears consonants, not accents. When a listener finds a take wrong anyway (an English r,
ḏ said as g), retake the clip with --takes, listen, and pin the good take: a pin outranks the
check, and survives rescore, until the clip's text changes.
"""
from __future__ import annotations

import array
import base64
import hashlib
import json
import math
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from eleven import Client, load_key  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEECH = os.path.join(ROOT, "tools", "out", "speech.json")
AUDIO = os.path.join(ROOT, "app", "src", "main", "assets", "audio")
MANIFEST = os.path.join(AUDIO, "manifest.json")
# Which take was chosen for each clip, with its transcripts: committed, so the choice is reviewable.
CHOICES = os.path.join(ROOT, "tools", "voice_choices.json")
# Takes chosen by ear over the check's choice, {clip id: {"file", "text"}}: committed.
PINS = os.path.join(ROOT, "tools", "voice_pins.json")
# Other ways to write a clip for the voice, {clip id: [Arabic text]}, tried before its own: for a
# clip a listener asked to retake (a swallowed vowel, a lost ʿayin). Committed.
VARIANTS = os.path.join(ROOT, "tools", "voice_variants.json")
TRANSCRIPTS = os.path.join(ROOT, "tools", "out", "transcripts.json")
# Framed takes as recorded, before the word is cut out: kept for checking, never shipped.
FRAMES = os.path.join(ROOT, "tools", "out", "frames")
# Takes that were recorded but not chosen, moved out of the app by `prune` and kept, so a later
# rescore can still choose them without paying for them again.
TAKES = os.path.join(ROOT, "tools", "out", "takes")

# Chaouki: a native Modern Standard Arabic narrator ("deep, clear and engaging") from the
# ElevenLabs voice library. A native Arabic voice articulates the pharyngeals and the uvular q
# that an English voice only approximates, and MSA keeps q as q (colloquial Egyptian Arabic would
# not). Chosen over Yousef and George by how often a transcriber heard ḥ in pseudo-words, the
# weakest sound for every voice tried. Eleven v4 is the newest model on the account (Oct 2026).
VOICE_ID = "G1HOkzin3NMwRHSq60UI"
MODEL_ID = "eleven_v4"
SETTINGS = {"stability": 0.5, "similarity_boost": 0.75}
SEEDS = (None, 11, 23)
# The language the model is told it is speaking, best first. Told "ar", it stops drifting into
# English-accented readings of lines and says ج as dj, not the Egyptian g that turned ḏd.t into
# "gedet" (reported, Nov 2026). None is how the clips before that were recorded: their takes keep
# their names, so they are still found, and still chosen where nothing better has been recorded.
LANGS = ("ar", None)
MAX_TAKES = 6
# "qul:" — "say:". Short, and the colon makes the voice pause before the word.
FRAME = "قُلْ:"


# A candidate may name another model than MODEL_ID, written after its text: "إِيبِفْ |model:eleven_multilingual_v2".
# multilingual_v2 keeps short vowels Eleven v4 drops ("ibf" for ibef) but loses ʿayin: offered by ear, clip by clip.
MODEL_MARK = " |model:"


def split_candidate(candidate: str) -> tuple[str, str]:
    """A candidate's text, and the model to speak it with."""
    text, _, model = candidate.partition(MODEL_MARK)
    return text, model or MODEL_ID


def spoken_text(text: str) -> str:
    text = split_candidate(text)[0]
    t = text.strip()
    return t if t[-1:] in ".!?،" else t + "."


def framed_text(text: str) -> str:
    return f"{FRAME} {spoken_text(text)}"


def take_path(name: str) -> str:
    """Where a take is: in the app's assets, or set aside in the cache by `prune`."""
    shipped = os.path.join(AUDIO, name)
    if os.path.exists(shipped):
        return shipped
    cached = os.path.join(TAKES, name)
    return cached if os.path.exists(cached) else shipped


def take_key(text: str, seed: int | None, framed: bool = False, lang: str | None = None) -> str:
    spec = {"text": spoken_text(text), "voice": VOICE_ID, "model": split_candidate(text)[1], "settings": SETTINGS}
    if seed is not None:
        spec["seed"] = seed
    if framed:
        spec["frame"] = FRAME
    if lang:
        spec["language"] = lang
    blob = json.dumps(spec, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(blob.encode("utf-8")).hexdigest()[:32] + ".mp3"


def load(path: str, default):
    if not os.path.exists(path):
        return default
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def load_clips() -> list[dict]:
    """The course's clips (tools/out/speech.json), each with its variants first among its candidates:
    a variant exists because a listener turned the usual spelling's takes down."""
    clips = load(SPEECH, [])
    variants = load(VARIANTS, {})
    for clip in clips:
        # An entry is a text, or {"text", "model"} for a take spoken by another model.
        extra = [v if isinstance(v, str) else v["text"] + MODEL_MARK + v["model"] for v in variants.get(clip["id"], [])]
        clip["candidates"] = extra + [c for c in clip["candidates"] if c not in extra]
    return clips


def save(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=1, sort_keys=True)
    # On Windows another program (an editor, a virus scanner) can hold the file for a moment, and
    # a long recording run must not die of it: try the swap again for a few seconds.
    for attempt in range(20):
        try:
            os.replace(tmp, path)
            return
        except OSError:
            if attempt == 19:
                raise
            time.sleep(0.5)


# The transcriber often answers a short word in Latin letters ("Per", "nib", "Kim") even when told
# to expect Arabic. Those letters still say which consonants it heard, so they are mapped to the
# nearest Arabic ones. Latin has no letter for ḥ or ʿayin: those can only be confirmed in Arabic.
LATIN_DIGRAPHS = [("tch", "چ"), ("sch", "ش"), ("sh", "ش"), ("ch", "چ"), ("kh", "خ"), ("dj", "ج"), ("dg", "ج"),
                  ("gh", "غ"), ("th", "ت"), ("ph", "ف"), ("ck", "ك"), ("qu", "ق"), ("ge", "ج"), ("gi", "ج")]
LATIN_LETTERS = {"b": "ب", "p": "پ", "f": "ف", "v": "ف", "m": "م", "n": "ن", "r": "ر", "l": "ل", "s": "س",
                 "z": "ز", "t": "ت", "d": "د", "k": "ك", "c": "ك", "q": "ق", "g": "گ", "j": "ج", "h": "ه",
                 "w": "و", "y": "ي", "x": "كس"}


def heard_letters(text: str) -> list[str]:
    """The consonant letters in a transcript, Arabic as written and Latin mapped to Arabic."""
    # Audio-event tags such as "[ضحكة]" (laughter) are not speech, and contain letters.
    t = re.sub(r"\[[^\]]*\]|\([^)]*\)", " ", text).lower()
    for a, b in LATIN_DIGRAPHS:
        t = t.replace(a, b)
    out: list[str] = []
    for ch in t:
        if "ء" <= ch <= "ي" or ch in "پچگ":
            out.append(ch)
        elif ch in LATIN_LETTERS:
            out.extend(LATIN_LETTERS[ch])
    return out


def unframe(text: str) -> str:
    """A framed take's transcript without the frame word, so its q and l are not credited to the word."""
    words = text.split()
    if words and ("ق" in words[0] or words[0].lower().startswith(("qu", "ku", "ko"))):
        words = words[1:]
    return " ".join(words)


def match(clip: dict, letters: list[str]) -> tuple[float, bool]:
    """Fraction of the expected consonants heard, in order; hard ones count double and must all be there."""
    got = total = 0.0
    hard_ok = True
    i = 0
    for slot, hard in zip(clip["expect"], clip["critical"]):
        weight = 2.0 if hard else 1.0
        total += weight
        j = next((k for k in range(i, len(letters)) if letters[k] in slot), None)
        if j is not None:
            got += weight
            i = j + 1
        elif hard:
            hard_ok = False
    return (got / total if total else (1.0 if letters else 0.0)), hard_ok


def score(clip: dict, heard: dict) -> tuple[float, bool]:
    """How well a take's transcripts match the clip: the best of every hearing of it."""
    hearings = [heard.get("ar", ""), heard.get("en", ""), unframe(heard.get("framed", ""))]
    value, hard_ok = max((match(clip, heard_letters(t)) for t in hearings if t),
                         key=lambda r: (r[1], r[0]), default=(0.0, False))
    if clip["hasP"] and "p" not in heard.get("en", "").lower():
        value -= 0.15
    return value, hard_ok and value >= 0.6


def pcm(path: str, rate: int = 16_000) -> array.array:
    """Decodes an MP3 to mono 16-bit samples with ffmpeg."""
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-f", "s16le", "-ac", "1", "-ar", str(rate), "-"],
                         check=True, capture_output=True).stdout
    samples = array.array("h")
    samples.frombytes(raw[: len(raw) // 2 * 2])
    return samples


def quietest(path: str, lo: float, hi: float, rate: int = 16_000) -> tuple[float, float]:
    """The middle of the quietest 10 ms between lo and hi seconds, and how quiet it is (0..1 of the loudest)."""
    samples = pcm(path, rate)
    step = rate // 100
    frames = [math.sqrt(sum(x * x for x in samples[i:i + step]) / step)
              for i in range(0, len(samples) - step + 1, step)]
    if not frames:
        return lo, 1.0
    first, last = max(0, int(lo * 100)), min(len(frames) - 1, int(hi * 100))
    if first > last:
        return lo, 1.0
    k = min(range(first, last + 1), key=lambda i: frames[i])
    return (k + 0.5) / 100, frames[k] / (max(frames) or 1.0)


class Recorder:
    def __init__(self) -> None:
        self.client = Client(load_key())
        self.transcripts: dict = load(TRANSCRIPTS, {})
        self.spent = 0

    def take(self, text: str, seed: int | None, framed: bool, lang: str | None) -> str:
        name = take_key(text, seed, framed, lang)
        path = os.path.join(AUDIO, name)
        if os.path.exists(path):
            return name
        if os.path.exists(take_path(name)):
            os.replace(take_path(name), path)
            return name
        os.makedirs(AUDIO, exist_ok=True)
        if not framed:
            audio = self.client.tts(VOICE_ID, spoken_text(text), split_candidate(text)[1], SETTINGS, seed=seed, language_code=lang)
            with open(path, "wb") as f:
                f.write(audio)
            self.spent += len(spoken_text(text))
            return name
        whole = framed_text(text)
        data = self.client.tts_timed(VOICE_ID, whole, split_candidate(text)[1], SETTINGS, seed=seed, language_code=lang)
        self.spent += len(whole)
        os.makedirs(FRAMES, exist_ok=True)
        full = os.path.join(FRAMES, name)
        with open(full, "wb") as f:
            f.write(base64.b64decode(data["audio_base64"]))
        starts = data["alignment"]["character_start_times_seconds"]
        word_at = starts[len(FRAME) + 1]
        # Cut in the pause the colon leaves, not at the timing itself, which can be off by a syllable.
        cut, level = quietest(full, word_at - 0.4, word_at + 0.03)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", f"{cut:.3f}", "-i", full,
                        "-af", "afade=t=in:d=0.015", "-ac", "1", "-ar", "44100", "-c:a", "libmp3lame", "-b:a", "128k",
                        path], check=True)
        heard = self.transcripts.setdefault(name, {})
        heard["cut_at"] = round(cut, 3)
        heard["cut_level"] = round(level, 3)
        return name

    def hear(self, name: str, want_en: bool, framed: bool) -> dict:
        t = self.transcripts.setdefault(name, {})
        with open(take_path(name), "rb") as f:
            audio = f.read()
        if "ar" not in t:
            t["ar"] = self.client.transcribe(audio, "scribe_v2", "ar")
        if want_en and "en" not in t:
            t["en"] = self.client.transcribe(audio, "scribe_v2", "en")
        if framed and "framed" not in t:
            with open(os.path.join(FRAMES, name), "rb") as f:
                t["framed"] = self.client.transcribe(f.read(), "scribe_v2", "ar")
        return t

    def record(self, clip: dict, at_least: int = 1, new_takes: int | None = None) -> dict:
        """Takes in try order until one passes and at least [at_least] were heard; the first that passed, else the best.

        [new_takes] caps how many takes this call may pay for; takes already recorded are free and always judged.
        """
        results = []
        fresh = 0
        for text, seed, framed, lang in tries(clip):
            recorded_before = os.path.exists(take_path(take_key(text, seed, framed, lang)))
            if not recorded_before:
                if new_takes is not None and fresh >= new_takes:
                    continue
                fresh += 1
            name = self.take(text, seed, framed, lang)
            heard = self.hear(name, clip["hasP"], framed)
            results.append(judge(clip, name, text, seed, framed, lang, heard))
            if len(results) >= at_least and any(r["passed"] for r in results):
                break
        return pick(results)


def tries(clip: dict) -> list[tuple[str, int | None, bool, str | None]]:
    """The takes to try, in order.

    For each language, best first: the word on its own, candidates alternating across seeds so
    each spelling gets a turn early; then, for words rather than phrases (whose tokens have no
    frame to be cut from), the same inside the frame.
    """
    out = []
    for lang in LANGS:
        out += [(c, s, False, lang) for s in SEEDS for c in clip["candidates"]][:MAX_TAKES]
        if not clip["id"].startswith("p:"):
            out += [(c, s, True, lang) for s in SEEDS[:2] for c in clip["candidates"]]
    return out


def judge(clip: dict, name: str, text: str, seed: int | None, framed: bool, lang: str | None, heard: dict) -> dict:
    value, passed = score(clip, heard)
    result = {"file": name, "text": text, "seed": seed, "score": round(value, 2), "passed": passed,
              "heard_ar": heard.get("ar", ""), "heard_en": heard.get("en", ""),
              "latin": clip["latin"], "ipa": clip["ipa"]}
    if lang:
        result["lang"] = lang
    if framed:
        result["framed"] = True
        result["heard_framed"] = heard.get("framed", "")
    return result


def recorded(clip: dict, transcripts: dict) -> list[dict]:
    """Judgements of the takes already recorded, in try order (a take not recorded is skipped)."""
    out = []
    for text, seed, framed, lang in tries(clip):
        name = take_key(text, seed, framed, lang)
        heard = transcripts.get(name)
        if heard is None or "ar" not in heard or not os.path.exists(take_path(name)):
            continue
        out.append(judge(clip, name, text, seed, framed, lang, heard))
    return out


def pinned(clip: dict, pins: dict, transcripts: dict) -> dict | None:
    """The take pinned for a clip, judged — if the pin is for the clip's current text and its take exists."""
    pin = pins.get(clip["id"])
    if not pin or pin["text"] not in clip["candidates"]:
        return None
    for r in recorded(clip, transcripts):
        if r["file"] == pin["file"]:
            return {**r, "pinned": True}
    return None


def apply_pins(clips: list[dict], choices: dict, transcripts: dict) -> None:
    pins = load(PINS, {})
    for clip in clips:
        p = pinned(clip, pins, transcripts)
        if p:
            choices[clip["id"]] = p


def pick(results: list[dict]) -> dict | None:
    """The first take that passes, else the best."""
    best = None
    for r in results:
        if r["passed"]:
            return r
        if best is None or r["score"] > best["score"]:
            best = r
    return best


def cmd_rescore() -> None:
    """Judges the takes already recorded again, with the current rules, without calling the API."""
    clips = load_clips()
    choices = load(CHOICES, {})
    transcripts = load(TRANSCRIPTS, {})
    changed = 0
    for clip in clips:
        best = pick(recorded(clip, transcripts))
        if best is None:
            continue
        old = choices.get(clip["id"], {})
        if (old.get("file"), old.get("passed")) != (best["file"], best["passed"]):
            changed += 1
        choices[clip["id"]] = best
    apply_pins(clips, choices, transcripts)
    save(CHOICES, choices)
    write_manifest()
    passed = sum(1 for c in clips if choices.get(c["id"], {}).get("passed"))
    print(f"{changed} choices changed; {passed} of {len(clips)} clips pass their check")


def to_record(clips: list[dict], choices: dict, retry: bool, only: set[str] | None) -> list[dict]:
    if only:
        missing = only - {c["id"] for c in clips}
        if missing:
            raise SystemExit(f"no such clips: {', '.join(sorted(missing))}")
        return [c for c in clips if c["id"] in only]
    transcripts = load(TRANSCRIPTS, {})
    pins = load(PINS, {})
    out = []
    for c in clips:
        chosen = choices.get(c["id"])
        if pinned(c, pins, transcripts):
            continue
        if chosen is None or chosen["text"] not in c["candidates"]:
            out.append(c)
        elif retry and not chosen["passed"] and len(recorded(c, transcripts)) < len(tries(c)):
            out.append(c)
    return out


def cost(clip: dict) -> tuple[int, int]:
    """Characters for the first take, and for every take."""
    every = sum(len(framed_text(t) if framed else spoken_text(t)) for t, _, framed, _ in tries(clip))
    return len(spoken_text(clip["candidates"][0])), every


def cmd_plan(retry: bool) -> None:
    clips = load_clips()
    choices = load(CHOICES, {})
    todo = to_record(clips, choices, retry, None)
    first = sum(cost(c)[0] for c in todo)
    print(f"{len(clips)} clips, {len(todo)} to record, ~{first} characters for first takes "
          f"(up to {sum(cost(c)[1] for c in todo)} if every check fails)")


def cmd_generate(assume_yes: bool, retry: bool, only: set[str] | None = None, at_least: int = 1,
                 new_takes: int | None = None) -> None:
    clips = load_clips()
    choices = load(CHOICES, {})
    todo = to_record(clips, choices, retry, only)
    rec = Recorder()
    sub = rec.client.get("/v1/user/subscription")
    left = sub["character_limit"] - sub["character_count"]
    first = sum(cost(c)[0] * at_least for c in todo)
    worst = sum(cost(c)[1] for c in todo)
    print(f"{len(todo)} clips to record; first takes ~{first} chars, worst case ~{worst}; account has {left} left")
    if worst > left:
        # No reserve: the run goes ahead. If the account runs dry, what was recorded is saved and
        # the next run, after more credit is bought, picks up the rest.
        print("the account may run out before the end; recorded clips are kept either way")
    if not todo:
        write_manifest()
        return
    if not assume_yes and input("record? [y/N] ").strip().lower() != "y":
        return
    try:
        for i, clip in enumerate(todo, 1):
            result = rec.record(clip, at_least, new_takes)
            choices[clip["id"]] = result
            mark = "ok " if result["passed"] else "?? "
            print(f"{mark}[{i}/{len(todo)}] {clip['id']:14} {clip['latin']:30} heard {result['heard_ar']!r}"
                  + (f" / {result['heard_en']!r}" if result["heard_en"] else "")
                  + (f" / framed {result['heard_framed']!r}" if result.get("framed") else ""), flush=True)
            if i % 10 == 0:
                save(CHOICES, choices)
                save(TRANSCRIPTS, rec.transcripts)
    finally:
        apply_pins(clips, choices, rec.transcripts)
        save(CHOICES, choices)
        save(TRANSCRIPTS, rec.transcripts)
        write_manifest()
    passed = sum(1 for c in clips if choices.get(c["id"], {}).get("passed"))
    print(f"spent ~{rec.spent} characters; {passed} of {len(clips)} clips pass their check")


def write_manifest() -> None:
    clips = load_clips()
    choices = load(CHOICES, {})
    for c in clips:
        chosen = choices.get(c["id"])
        if chosen and not os.path.exists(os.path.join(AUDIO, chosen["file"])) and os.path.exists(take_path(chosen["file"])):
            os.replace(take_path(chosen["file"]), os.path.join(AUDIO, chosen["file"]))
    manifest = {c["id"]: choices[c["id"]]["file"] for c in clips
                if c["id"] in choices and os.path.exists(os.path.join(AUDIO, choices[c["id"]]["file"]))}
    save(MANIFEST, manifest)
    print(f"manifest: {len(manifest)} of {len(clips)} items have a clip")


def cmd_prune() -> None:
    """Moves every clip the manifest does not name out of the app, into the take cache."""
    keep = set(load(MANIFEST, {}).values()) | {"manifest.json"}
    gone = [n for n in os.listdir(AUDIO) if n not in keep]
    os.makedirs(TAKES, exist_ok=True)
    for n in gone:
        os.replace(os.path.join(AUDIO, n), os.path.join(TAKES, n))
    print(f"moved {len(gone)} unused files to {TAKES}")


def cmd_takes(ids: list[str]) -> None:
    """Every recorded take of these clips, with what was heard, and which one the app plays."""
    clips = {c["id"]: c for c in load_clips()}
    choices = load(CHOICES, {})
    pins = load(PINS, {})
    transcripts = load(TRANSCRIPTS, {})
    for cid in ids:
        clip = clips.get(cid)
        if clip is None:
            print(f"-- {cid}: no such clip")
            continue
        chosen = choices.get(cid, {}).get("file")
        print(f"{cid}  {clip['latin']}  ({clip['ipa']})" + ("  [pinned]" if pinned(clip, pins, transcripts) else ""))
        for r in recorded(clip, transcripts):
            mark = "->" if r["file"] == chosen else "  "
            print(f"  {mark} {r['file']}  {'ok' if r['passed'] else '??'} {r['score']:.2f}  lang={r.get('lang') or '-':2} "
                  f"seed={r['seed']!s:4} {'framed ' if r.get('framed') else ''}{r['text']}  heard {r['heard_ar']!r}"
                  + (f" / {r['heard_en']!r}" if r["heard_en"] else ""))


def cmd_pin(cid: str, name: str | None) -> None:
    """Pins a take by ear (or, without a file, removes the pin), and ships it."""
    clips = {c["id"]: c for c in load_clips()}
    clip = clips.get(cid) or sys.exit(f"no such clip {cid}")
    pins = load(PINS, {})
    transcripts = load(TRANSCRIPTS, {})
    if name is None:
        pins.pop(cid, None)
    else:
        take = next((r for r in recorded(clip, transcripts) if r["file"] == name), None)
        if take is None:
            sys.exit(f"{name} is not a recorded take of {cid}; see `voice.py takes {cid}`")
        pins[cid] = {"file": name, "text": take["text"]}
    save(PINS, pins)
    choices = load(CHOICES, {})
    if name is None:
        best = pick(recorded(clip, transcripts))
        if best:
            choices[cid] = best
    apply_pins(list(clips.values()), choices, transcripts)
    save(CHOICES, choices)
    write_manifest()
    print(f"{cid} -> {choices.get(cid, {}).get('file')}" + (" (pinned)" if name else " (unpinned)"))


def cmd_report() -> None:
    clips = load_clips()
    choices = load(CHOICES, {})
    for c in clips:
        r = choices.get(c["id"])
        if not r:
            print(f"-- {c['id']:14} not recorded")
            continue
        print(f"{'ok' if r['passed'] else '??'} {r['score']:.2f} {c['id']:14} {c['latin']:30} "
              f"{r['text']:30} {'framed ' if r.get('framed') else ''}heard {r['heard_ar']!r}"
              + (f" / {r['heard_en']!r}" if r["heard_en"] else "")
              + (f" / framed {r['heard_framed']!r}" if r.get("framed") else ""))


if __name__ == "__main__":
    args = sys.argv[1:]
    cmd = args[0] if args else "plan"
    if cmd == "plan":
        cmd_plan("--retry" in args)
    elif cmd == "generate":
        only = None
        if "--only" in args:
            only = set(args[args.index("--only") + 1].split(","))
        at_least = int(args[args.index("--takes") + 1]) if "--takes" in args else 1
        new_takes = int(args[args.index("--new-takes") + 1]) if "--new-takes" in args else None
        cmd_generate("--yes" in args, "--retry" in args, only, at_least, new_takes)
    elif cmd == "report":
        cmd_report()
    elif cmd == "takes":
        cmd_takes(args[1].split(","))
    elif cmd == "pin":
        cmd_pin(args[1], args[2])
    elif cmd == "unpin":
        cmd_pin(args[1], None)
    elif cmd == "rescore":
        cmd_rescore()
    elif cmd == "manifest":
        write_manifest()
    elif cmd == "prune":
        cmd_prune()
    else:
        raise SystemExit(f"unknown command {cmd}")
