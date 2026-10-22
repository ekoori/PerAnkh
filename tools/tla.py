"""Word frequencies from the Thesaurus Linguae Aegyptiae, measured against the course.

    python -I -X utf8 tools/tla.py tools/out/tla/train.jsonl tools/out/speech.json [--write]

The TLA's "Earlier Egyptian" sentences (corpus v18, CC BY-SA 4.0: BBAW / SAW Leipzig) come with
a lemma for every word. This keeps the Middle Egyptian ones — witnesses dated after 2150 BCE —
counts the lemmas, and reports:

  * how much of that real text the course's words cover, token by token;
  * the most frequent lemmas, marking the ones the course teaches;
  * course words it could not find among the lemmas, to map by hand.

With --write it also writes content/.../Frequency.kt: for every course word, how many words in
10,000 of Middle Egyptian text are that word. Only counts and the course's own ids go into the
source; nothing from the corpus's text does.

The corpus file is downloaded data: run this with -I, from outside the corpus directory.
"""
from __future__ import annotations

import collections
import json
import os
import re
import sys
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_KT = os.path.join(ROOT, "content", "src", "main", "kotlin", "com", "krbavcic", "pentalingo", "content",
                      "Frequency.kt")
# After the First Intermediate Period: Middle Kingdom and Second Intermediate Period witnesses.
MIDDLE_FROM = -2150

# Course words pinned by hand to TLA lemma ids: where the spellings differ (rꜥ / Rꜥw, ḫt / ꞽḫ.t),
# and where a homograph must go to the meaning the course teaches (kꜣ "ka" 162870, not kꜣ "bull"
# 162930; zꜣ "son" 125510, not sꜣ "back" 125670). Each was checked against the German
# translations of sentences using it. Spelling alone also catches homographs the course does not
# teach: jt "barley" for jtj "father", ḥmt "copper" for ḥm.t "wife", mn "suffer", wn "open",
# wḏꜣ "go", dp.t "taste" for dpt "ship", rʾ "goose" for r "mouth", jr.t "duty" for jrt "eye",
# wꜣ.t "rope" for wꜣt "way", m "look!" for m "don't"; those words are pinned too, as are words whose lemma
# has a part of speech or spelling the matcher cannot reach (jm "there" is an adverb, mk "look"
# the particle m, sqdw "sailors" the lemma sqd). The two corpora give a few lemmas different ids:
# rḏi̯ "give" is mostly 96700 and 550028 in corpus v18 but 851711 in AES (tools/aes.py), which
# comes from the TLA's 2018 database, and ḫpr "become" 116230 there is 858535 in AES.
MANUAL: dict[str, list[str]] = {
    "w:sA": ["125510"], "w:sAt": ["125630"], "w:mwt": ["69040"], "w:xt": ["30750", "850967"],
    "w:ra": ["400015"], "w:Hmf": ["104690"], "w:dj": ["96700", "550028", "851711"], "w:HtpDjnswt": ["111510"],
    "w:kA": ["162870"], "w:kA2": ["162930"], "w:dd2": ["400140"], "w:Dt": ["181401", "181400"],
    "w:nb": ["81650"], "w:jmAxy": ["25090", "25060"], "w:jnpw": ["27360"], "w:HwtHr": ["99960"],
    "w:xwnjnpw": ["705497"], "w:xwfw": ["400277"], "w:njwt": ["80890"], "w:tAwj": ["168970"],
    "w:ntf": ["90030"], "w:Sw": ["152710"], "w:nwt": ["80940"], "w:Hrw": ["107500", "400034", "400146", "850587"],
    "w:suff": ["10050"], "w:sufk": ["10110"], "w:sufj": ["10030"], "w:sufs": ["10090"], "w:sufsn": ["10100"],
    "w:jt": ["32820"], "w:Hmt": ["104730"], "w:mn": ["69590"], "w:wDA": ["52090", "52100"],
    "w:xpr": ["116230", "858535", "854383"], "w:wn": ["46050", "400006"],
    "w:jm": ["24640"], "w:mk": ["64440"], "w:sqdw": ["146280"], "w:dpt": ["179040"],
    "w:ky": ["163760", "400645"], "w:wa": ["44150", "600041", "400101"],
    "w:jb": ["23290"], "w:jb2": ["23360"], "w:n2": ["850806", "78890", "400261"], "w:mneg": ["64410"],
    "w:nm": ["27410"], "w:rA": ["92560", "92580"], "w:jrt": ["28250", "28290"], "w:wAt": ["42490"],
    "w:a": ["34360", "34320"], "w:st2": ["854540", "125100"], "w:tp2": ["854577", "170860", "170861"],
    "w:zp": ["854543", "132300", "132310", "132320"], "w:Hr2": ["107510"],
}
# Lemmas a course word covers besides its own: the course teaches N35 n as "to, for, of", which
# is both the preposition and the genitive n.ꞽ (79800 in corpus v18, 850787 in AES). jn "by" is
# also taught as the jn that points out a doer, "it is the heart that…" (26670 in corpus v18;
# AES's 851426 counts jn introducing a question too). zp "time" also covers zp 2, "twice", a
# lemma of its own in AES. wn "exist" is pinned above to wnn, of which it is the short form.
EXTRA: dict[str, list[str]] = {
    "w:n": ["79800", "850788", "850787"], "w:jn": ["26670", "851426"], "w:zp": ["70011"],
}

# Which TLA parts of speech can stand for a course word of each kind.
UPOS = {
    "NOUN": {"NOUN"}, "VERB": {"VERB"}, "ADJECTIVE": {"ADJ", "VERB"}, "PREPOSITION": {"ADP"},
    "PRONOUN": {"PRON"}, "NAME": {"PROPN", "NOUN"}, "PARTICLE": {"PART"}, "EXPRESSION": None,
}
# Nouns the course also accepts as verbs ("life", "to live").
ALSO_VERB = {"w:anx", "w:snb", "w:wDA", "w:Htp"}


def norm(translit: str) -> str:
    """A transliteration reduced for matching: TLA's Leiden conventions to the course's Allen ones."""
    t = unicodedata.normalize("NFD", translit.lower())
    t = t.replace("ı͗", "j").replace("i̯", "j").replace("u̯", "w")
    t = unicodedata.normalize("NFC", t)
    t = t.replace("ꞽ", "j").replace("ı͗", "j").replace("ʾ", "").replace("ḳ", "q")
    return re.sub(r"[\s\-.()⸗~\[\]]", "", t)


def loose(translit: str) -> str:
    """A second, looser key: z and s merged, as the course spells both s."""
    return norm(translit).replace("z", "s")


def course_words(speech_path: str) -> list[tuple[str, str, str | None]]:
    """The course's words as (id, transliteration, part of speech), from the speech export."""
    with open(speech_path, encoding="utf-8") as f:
        clips = json.load(f)
    return [(c["id"], c["spoken"], c.get("pos")) for c in clips if c["id"].startswith("w:")]


def match(words, spelling: dict[str, str], tag) -> tuple[dict[str, list[str]], list[tuple[str, str]]]:
    """Lemma ids for each course word: hand pins first, else by spelling and part of speech.

    `spelling` maps lemma id to its transliteration; `tag(lemma id)` gives its UPOS tag. Shared
    with tools/aes.py, whose corpus uses the same lemma ids.
    """
    by_norm: dict[str, list[str]] = collections.defaultdict(list)
    by_loose: dict[str, list[str]] = collections.defaultdict(list)
    for lid, tr in spelling.items():
        by_norm[norm(tr)].append(lid)
        by_loose[loose(tr)].append(lid)
    found: dict[str, list[str]] = {}
    missing = []
    for wid, tr, kind in words:
        if wid in MANUAL:
            ids = list(MANUAL[wid])
        else:
            ids = by_norm.get(norm(tr)) or by_loose.get(loose(tr)) or []
            allowed = UPOS.get(kind or "EXPRESSION")
            if allowed is not None:
                allowed = allowed | ({"VERB"} if wid in ALSO_VERB else set())
                ids = [i for i in ids if tag(i) in allowed]
        ids += [i for i in EXTRA.get(wid, []) if i not in ids]
        if ids:
            found[wid] = ids
        else:
            missing.append((wid, tr))
    return found, missing


def main(argv: list[str]) -> None:
    corpus_path, speech_path = argv[1], argv[2]
    write = "--write" in argv

    rows = []
    with open(corpus_path, encoding="utf-8") as f:
        for line in f:
            if line.strip():
                rows.append(json.loads(line))
    middle = []
    for r in rows:
        try:
            if int(r.get("dateNotBefore") or -9999) >= MIDDLE_FROM:
                middle.append(r)
        except ValueError:
            continue

    counts: collections.Counter[str] = collections.Counter()
    spelling: dict[str, str] = {}
    pos: dict[str, collections.Counter[str]] = collections.defaultdict(collections.Counter)
    for r in middle:
        lemmas = (r.get("lemmatization") or "").split()
        tags = (r.get("UPOS") or "").split()
        for i, tok in enumerate(lemmas):
            if "|" not in tok:
                continue
            lid, tr = tok.split("|", 1)
            counts[lid] += 1
            spelling.setdefault(lid, tr)
            if i < len(tags):
                pos[lid][tags[i]] += 1
    total = sum(counts.values())

    words = course_words(speech_path)

    def tag(lid: str) -> str:
        return pos[lid].most_common(1)[0][0] if pos[lid] else ""

    found, missing = match(words, spelling, tag)

    covered = {lid for ids in found.values() for lid in ids}
    covered_tokens = sum(counts[lid] for lid in covered)
    print(f"{len(rows)} sentences; {len(middle)} Middle Egyptian (dated after {-MIDDLE_FROM} BCE), "
          f"{total} words, {len(counts)} lemmas")
    print(f"course words: {len(words)}; found {len(found)}; cover {covered_tokens} words = "
          f"{100 * covered_tokens / total:.1f}% of the Middle Egyptian text\n")

    print("course words, most frequent first:")
    for wid, ids in sorted(found.items(), key=lambda kv: -sum(counts[i] for i in kv[1])):
        n = sum(counts[i] for i in ids)
        detail = ", ".join(f"{i}|{spelling[i]}:{counts[i]}" for i in ids if i in spelling)
        print(f"  {wid:12} {10_000 * n / total:7.1f}/10k  {detail}")
    if missing:
        print("\nnot found (map by hand in MANUAL):")
        for wid, tr in missing:
            print(f"  {wid:12} {tr}")

    print("\nmost frequent lemmas (* = taught):")
    cum = 0
    for rank, (lid, n) in enumerate(counts.most_common(120), 1):
        cum += n
        mark = "*" if lid in covered else " "
        tag = pos[lid].most_common(1)[0][0] if pos[lid] else ""
        print(f"  {rank:3} {mark} {spelling[lid]:16} {lid:>7} {tag:6} {n:5}  {100 * n / total:5.2f}%  cum {100 * cum / total:5.1f}%")

    if write:
        lines = [
            "package com.krbavcic.pentalingo.content",
            "",
            "// Generated by tools/tla.py from the Thesaurus Linguae Aegyptiae, Earlier Egyptian corpus v18",
            "// (BBAW / SAW Leipzig, CC BY-SA 4.0): Middle Egyptian sentences, dated after 2150 BCE.",
            "// Do not edit by hand; run the tool again.",
            "",
            "/**",
            " * How common each course word is in real Middle Egyptian: words in 10,000 of running text.",
            f" * Counted over {total} words in {len(middle)} sentences.",
            " */",
            "val FREQUENCY: Map<String, Double> = mapOf(",
        ]
        for wid, ids in sorted(found.items()):
            per = 10_000 * sum(counts[i] for i in ids) / total
            lines.append(f'    "{wid}" to {per:.1f},')
        lines += [")", "", f"const val FREQUENCY_CORPUS_WORDS = {total}", ""]
        with open(OUT_KT, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lines))
        print(f"\nwrote {OUT_KT}")


if __name__ == "__main__":
    main(sys.argv)
