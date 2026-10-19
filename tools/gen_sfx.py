"""Synthesise the app's sound effects as small WAV files.

Plucked strings by the Karplus-Strong algorithm, tuned to a pentatonic scale: the nearest thing
to an Egyptian harp that needs no samples, no licence and no download. Deterministic — the same
seed gives byte-identical files, so regenerating them changes nothing in git.

    python -X utf8 tools/gen_sfx.py
"""
from __future__ import annotations

import math
import os
import random
import struct
import wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "core-ui", "src", "main", "res", "raw")
SR = 22050


def pluck(freq: float, seconds: float, decay: float = 0.996, brightness: float = 0.5, seed: int = 1) -> list[float]:
    rng = random.Random(seed)
    n = max(2, int(SR / freq))
    buf = [rng.uniform(-1, 1) for _ in range(n)]
    out = []
    for i in range(int(SR * seconds)):
        a = buf[i % n]
        b = buf[(i + 1) % n]
        # Weighted average: lower brightness darkens the tone faster.
        buf[i % n] = (brightness * a + (1 - brightness) * b) * decay
        out.append(a)
    return out


def mix(length: float, *parts: tuple[float, list[float], float]) -> list[float]:
    """Each part is (start seconds, samples, gain)."""
    out = [0.0] * int(SR * length)
    for start, samples, gain in parts:
        offset = int(SR * start)
        for i, s in enumerate(samples):
            if offset + i < len(out):
                out[offset + i] += s * gain
    return out


def fade(samples: list[float], ms: float = 30) -> list[float]:
    n = int(SR * ms / 1000)
    out = list(samples)
    for i in range(min(n, len(out))):
        out[-1 - i] *= i / n
    return out


def write(name: str, samples: list[float], peak: float = 0.8) -> None:
    top = max(1e-9, max(abs(s) for s in samples))
    scale = peak / top
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in fade(samples)))
    print(f"{name}: {len(samples) / SR:.2f}s, {os.path.getsize(path)} bytes")


def note(semitones_from_a4: int) -> float:
    return 440.0 * 2 ** (semitones_from_a4 / 12)


# A minor pentatonic: A C D E G.
A4, C5, D5, E5, G5, A5, C6, E6 = (note(n) for n in (0, 3, 5, 7, 10, 12, 15, 19))
A3, E3 = note(-12), note(-17)

write("sfx_correct.wav", mix(0.7, (0.0, pluck(E5, 0.6, seed=2), 0.8), (0.09, pluck(A5, 0.6, seed=3), 0.9)), 0.7)
write("sfx_near.wav", mix(0.6, (0.0, pluck(D5, 0.5, seed=4), 0.8), (0.1, pluck(E5, 0.5, seed=5), 0.6)), 0.6)
write("sfx_wrong.wav", mix(0.5, (0.0, pluck(A3, 0.5, decay=0.99, brightness=0.6, seed=6), 1.0),
                           (0.07, pluck(E3, 0.45, decay=0.99, brightness=0.6, seed=7), 0.7)), 0.45)
write("sfx_tap.wav", pluck(C6, 0.09, decay=0.97, brightness=0.55, seed=8), 0.35)
arpeggio = [A4, C5, D5, E5, G5, A5]
parts = [(i * 0.11, pluck(f, 1.4, seed=10 + i), 0.75) for i, f in enumerate(arpeggio)]
parts += [(0.75, pluck(A4, 1.6, seed=30), 0.5), (0.75, pluck(E5, 1.6, seed=31), 0.5), (0.75, pluck(A5, 1.6, seed=32), 0.5)]
write("sfx_complete.wav", mix(2.3, *parts), 0.8)
# The award: a quick glittering run, like a sistrum shaken over a harp chord.
run = [(i * 0.045, pluck(f, 0.5, decay=0.994, seed=40 + i), 0.45) for i, f in enumerate([E6, C6, A5, G5, E5, C6, E6])]
run += [(0.35, pluck(A4, 1.5, seed=60), 0.6), (0.35, pluck(C5, 1.5, seed=61), 0.5), (0.35, pluck(E5, 1.5, seed=62), 0.5)]
write("sfx_award.wav", mix(1.9, *run), 0.8)
