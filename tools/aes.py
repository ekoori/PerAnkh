"""Coverage of the course against AES, a larger corpus of Ancient Egyptian sentences.

    python -I -X utf8 tools/aes.py tools/out/corpus/aes tools/out/speech.json
    python -I -X utf8 tools/aes.py tools/out/corpus/aes tools/out/speech.json --show TEXT_ID [SENTENCES]

AES (Simon D. Schweitzer, github.com/simondschweitzer/aes, CC BY-SA 4.0) holds 101,796 sentences
from the TLA's 2018 database: lemmatised, dated by period, grouped in sub-corpora, most with
hieroglyphs as Gardiner codes. Its lemma ids are the TLA's, so course words match through the
rules and hand pins of tools/tla.py (imported from this directory, never from the corpus one).

The report, for the Middle Kingdom part of the corpus:
  * how much of the running text the course's words cover, by sub-corpus;
  * the commonest words of the literature that the course does not teach yet;
  * verb forms and constructions by frequency: which grammar to teach next;
  * what each proposed unit (UNITS below) would add, in the literature and in the stories;
  * the stories: how many new words each needs, and the passages closest to readable now.
--show prints a text sentence by sentence, with the words the course does not teach in brackets.

"Covered" counts the course's words and compound nouns and names made only of them (nswt-bj.tj,
nṯr-ꜥꜣ, Jmn-ḥtp), which the corpus lemmatises as single words. Numbers are not counted: the
Counting unit teaches them as signs.

The corpus files are downloaded data: run with -I, from outside the corpus directory.
"""
from __future__ import annotations

import collections
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tla  # noqa: E402  (our own tools/tla.py: matching rules and hand pins)

# AES part-of-speech names, mapped to the UPOS tags the matcher reasons with.
POS = {
    "substantive": "NOUN", "verb": "VERB", "adjective": "ADJ", "preposition": "ADP", "pronoun": "PRON",
    "particle": "PART", "adverb": "ADV", "entity_name": "PROPN", "epitheton_title": "NOUN",
    "numeral": "NUM", "interjection": "INTJ", "undefined": "X",
}
# Compounds of known words that a learner can read: nouns and names. Compound prepositions are
# left out, since m-ḫt "after" is not m "in" plus ḫt "thing".
COMPOUND_TAGS = {"NOUN", "PROPN", "ADJ"}

MIDDLE = "MK & SIP"
LITERATURE = "sawlit"

# The great Middle Kingdom texts, by AES text id (TLA text ids, stable across releases).
STORIES = {
    "Shipwrecked Sailor (P. Hermitage 1115)": "QUFWZTEPLRE4NHKCPAJXGSAOSQ",
    "Eloquent Peasant (P. Berlin 10499)": "2NTZNJ65V5FZBCZ3BM55HZEQDQ",
    "Westcar, Khufu and the magicians": "J4EXGHLCL5DR7JHSPDWTVEKMDY",
    "Ptahhotep (P. Prisse)": "DKAFDVSXYFADJDS3Z3AOISAP4U",
    "Sinuhe (P. Berlin 3022)": "JIPPXMXOTNCQDIKJIQVYWMKOWU",
}

# Units proposed after the present course, in teaching order, as the lemmas each would teach
# (grammar such as sḏm.n=f has no lemma of its own, so it is not counted here). Measured
# cumulatively: each line is what that unit adds to the ones above it.
UNITS = {
    "Every, other, like (nb, ky, mj, jm, ḫr, ẖr, tp, m-ꜥ, wꜥ)": [
        "81660", "163760", "400645", "850796", "24640", "850795", "850794", "850801", "851449",
        "44150", "600041", "400101"],
    "Pronouns II (wj, ṯw, sj, st, =n, =ṯn, =tw, tn, nn, mk, jsṯ, ptr)": [
        "44000", "174900", "127770", "147350", "175650", "10070", "10130", "10120", "170100",
        "172360", "851523", "64440", "851440", "62920"],
    "Telling a story (ꜥḥꜥ.n; see, find, bring, go out, enter, reach...)": [
        "40111", "66270", "167210", "26870", "60920", "41180", "61370", "132830", "154340",
        "33560", "97580", "102060", "851887", "78140", "51970"],
    "Not and do not (n, m, tm, jmi̯, jn)": ["850806", "64410", "854578", "25170", "851706", "851426"],
    "Face, mouth, eye (the body; place, way, time, foreign land)": [
        "107510", "92560", "28250", "122080", "854577", "854540", "42490", "854543", "114300"],
    "Beloved of the god (mri̯, ḥzi̯, mꜣꜥ-ḫrw, n.tj, jr.j-pꜥ.t, dwꜣ)": [
        "72470", "109620", "66750", "400178", "89850", "94060", "854584", "72650"],
    "Lord of Abydos (the full offering formula)": [
        "103", "400976", "119180", "45580", "82090", "113110", "99940", "400114", "138670",
        "71170", "156950"],
}


def load(folder: str) -> list[dict]:
    """Every sentence, in corpus order (which is text order within a text)."""
    sentences = []
    for name in sorted(os.listdir(folder)):
        if name.startswith("_aes_") and name.endswith(".json"):
            with open(os.path.join(folder, name), encoding="utf-8") as f:
                sentences.extend(json.load(f).values())
    return sentences


def words(sentence: dict) -> list[dict]:
    """The lemmatised word tokens of a sentence (destroyed and unlemmatised ones dropped)."""
    return [t for t in sentence.get("token", []) if t.get("lemmaID")]


def verb_form(t: dict) -> str | None:
    """A readable name for a verb form, from AES's inflection and morphology tags."""
    infl, morph = t.get("inflection"), t.get("morphology")
    if not infl:
        return None
    names = {
        ("suffixConjugation", None): "sḏm=f",
        ("suffixConjugation", "n-morpheme"): "sḏm.n=f (past)",
        ("suffixConjugation", "tw-morpheme"): "sḏm.tw=f (passive)",
        ("suffixConjugation", "jn-morpheme"): "sḏm.jn=f (then he heard)",
        ("suffixConjugation", "ḫr-morpheme"): "sḏm.ḫr=f (then he must hear)",
        ("suffixConjugation", "kꜣ-morpheme"): "sḏm.kꜣ=f (then he will hear)",
        ("suffixConjugation", "t-morpheme"): "sḏm.t=f",
        ("suffixConjugation", "geminated"): "sḏm=f, geminated",
        ("suffixConjugation", "w-morpheme"): "sḏm.w=f (passive)",
        ("pseudoParticiple", None): "stative (jj.kw, I have come)",
        ("participle", None): "participle (sḏm, who hears)",
        ("participle", "prefixed"): "participle, with j- prefix",
        ("participle", "geminated"): "participle, imperfective",
        ("participle", "w-morpheme"): "participle, passive",
        ("relativeform", None): "relative form (sḏm=f, which he hears)",
        ("relativeform", "n-morpheme"): "relative form, past (sḏm.n=f)",
        ("relativeform", "geminated"): "relative form, imperfective",
        ("relativeform", "prefixed"): "relative form, with j- prefix",
        ("imperative", None): "imperative",
        ("infinitive", None): "infinitive",
        ("infinitive", "t-morpheme"): "infinitive",
    }
    return names.get((infl, morph), f"{infl} ({morph})" if morph else infl)


class Lexicon:
    """Lemma spellings, parts of speech and glosses, gathered from the whole corpus."""

    def __init__(self, sentences: list[dict]):
        self.spelling: dict[str, str] = {}
        self.pos: dict[str, collections.Counter] = collections.defaultdict(collections.Counter)
        self.gloss: dict[str, collections.Counter] = collections.defaultdict(collections.Counter)
        for s in sentences:
            for t in words(s):
                lid = t["lemmaID"]
                self.spelling.setdefault(lid, t.get("lemma_form") or t.get("written_form") or "")
                self.pos[lid][POS.get(t.get("pos"), "X")] += 1
                if t.get("cotext_translation"):
                    self.gloss[lid][t["cotext_translation"]] += 1

    def tag(self, lid: str) -> str:
        return self.pos[lid].most_common(1)[0][0] if self.pos[lid] else ""

    def sense(self, lid: str) -> str:
        return self.gloss[lid].most_common(1)[0][0] if self.gloss[lid] else ""

    def readable(self, known: set[str]) -> set[str]:
        """The known lemmas plus the compound nouns and names made only of known words."""
        parts = {tla.loose(self.spelling[lid]) for lid in known if lid in self.spelling}
        return known | {
            lid for lid, sp in self.spelling.items()
            if "-" in sp and self.tag(lid) in COMPOUND_TAGS and all(tla.loose(p) in parts for p in sp.split("-"))
        }


def coverage(tokens: list[str], known: set[str]) -> float:
    return 100 * sum(1 for lid in tokens if lid in known) / max(1, len(tokens))


def show(sentences: list[dict], lex: Lexicon, known: set[str], text: str, limit: int) -> None:
    chosen = [s for s in sentences if s.get("text") == text][:limit]
    for i, s in enumerate(chosen):
        line = " ".join(
            t.get("written_form", "") if not t.get("lemmaID") or t["lemmaID"] in known
            else f"[{t.get('written_form', '')}]"
            for t in s.get("token", []))
        print(f"{i:3}  {line}\n     {(s.get('sentence_translation') or '').strip()}")


def main(argv: list[str]) -> None:
    sentences = load(argv[1])
    lex = Lexicon(sentences)
    found, missing = tla.match(tla.course_words(argv[2]), lex.spelling, lex.tag)
    taught = {lid for ids in found.values() for lid in ids}
    known = lex.readable(taught)
    if "--show" in argv:
        at = argv.index("--show")
        show(sentences, lex, known, argv[at + 1], int(argv[at + 2]) if len(argv) > at + 2 else 30)
        return

    print(f"AES: {len(sentences)} sentences, {len(lex.spelling)} lemmas. Course words matched: {len(found)}; "
          f"not in AES: {', '.join(w for w, _ in missing) or 'none'}\n")

    middle = [s for s in sentences if s.get("date") == MIDDLE]
    literature = [s for s in middle if s.get("corpus") == LITERATURE]
    lit_tokens = [t["lemmaID"] for s in literature for t in words(s)]

    print("== Coverage of running text: course words / with compounds of them")
    slices = [("Middle Kingdom & SIP, all", middle)]
    for corpus in sorted({s.get("corpus") for s in middle}, key=lambda c: -sum(1 for s in middle if s.get("corpus") == c)):
        slices.append((f"  {corpus}", [s for s in middle if s.get("corpus") == corpus]))
    slices.append(("Old Kingdom to SIP (Earlier Egyptian)", [s for s in sentences if s.get("date") in ("OK & FIP", MIDDLE)]))
    for name, chosen in slices:
        tokens = [t["lemmaID"] for s in chosen for t in words(s)]
        if len(tokens) >= 1000:
            print(f"  {name:38} {len(chosen):6} sentences {len(tokens):7} words  "
                  f"{coverage(tokens, taught):5.1f}%  {coverage(tokens, known):5.1f}%")

    counts = collections.Counter(lit_tokens)
    total = len(lit_tokens)
    running = sum(n for lid, n in counts.items() if lid in known)
    print(f"\n== Commonest words of Middle Kingdom literature not taught yet ({total} words); "
          f"'then' = coverage once all above are taught")
    for rank, (lid, n) in enumerate(((lid, n) for lid, n in counts.most_common() if lid not in known), 1):
        running += n
        print(f"  {rank:3} {lex.spelling[lid]:16} {lid:>7} {lex.tag(lid):5} {n:5} {100 * n / total:5.2f}%  "
              f"then {100 * running / total:5.1f}%  {lex.sense(lid)[:40]}")
        if rank == 50:
            break

    print("\n== Verb forms in Middle Kingdom literature, per 1,000 words")
    forms = collections.Counter(verb_form(t) for s in literature for t in words(s))
    forms.pop(None, None)
    for form, n in forms.most_common(14):
        print(f"  {form:42} {n:5}  {1000 * n / total:5.1f}")
    pseudo = collections.Counter()
    for s in literature:
        ws = words(s)
        for a, b in zip(ws, ws[1:]):
            if a.get("pos") == "preposition" and b.get("inflection") == "infinitive":
                pseudo[a.get("lemma_form")] += 1
    print("  preposition + infinitive (ḥr sḏm 'is hearing', r sḏm 'will hear'):",
          ", ".join(f"{p} {n}" for p, n in pseudo.most_common(4)))

    texts: dict[str, list[dict]] = collections.defaultdict(list)
    for s in sentences:
        texts[s.get("text")].append(s)
    story_tokens = {name: [t["lemmaID"] for s in texts[tid] for t in words(s)] for name, tid in STORIES.items()}

    print("\n== Proposed units, cumulative: coverage of literature and of each story")
    short = {name: name.split(" (")[0].split(",")[0] for name in STORIES}
    print(f"  {'':62} {'new':>4} {'lit.':>6}" + "".join(f" {short[n][:9]:>9}" for n in STORIES))
    learned = set(taught)

    def row(label: str, new: int) -> None:
        now = lex.readable(learned)
        print(f"  {label[:62]:62} {new:4} {coverage(lit_tokens, now):6.1f}"
              + "".join(f" {coverage(story_tokens[n], now):9.1f}" for n in STORIES))

    row("the course today", 0)
    for name, ids in UNITS.items():
        learned |= set(ids)
        row("+ " + name, len(ids))

    print("\n== The stories: size, coverage today, and new words needed for 60/70/80/90%")
    for name, tid in STORIES.items():
        ss, tokens = texts[tid], story_tokens[name]
        c = collections.Counter(tokens)
        acc, need = sum(n for lid, n in c.items() if lid in known), {}
        for i, (lid, n) in enumerate(((lid, n) for lid, n in c.most_common() if lid not in known), 1):
            acc += n
            for goal in (60, 70, 80, 90):
                if goal not in need and 100 * acc / len(tokens) >= goal:
                    need[goal] = i
        hiero = 100 * sum(1 for s in ss for t in words(s) if t.get("hiero")) / max(1, len(tokens))
        print(f"  {name:40} {len(ss):4} sentences {len(tokens):5} words  hieroglyphs {hiero:3.0f}%  "
              f"covered {coverage(tokens, known):4.1f}%  new words for 60/70/80/90%: "
              + "/".join(str(need.get(g, "-")) for g in (60, 70, 80, 90)))
        windows = []
        for i in range(max(1, len(ss) - 9)):
            ws = [t for s in ss[i:i + 10] for t in words(s)]
            if len(ws) >= 40:
                new = len({t["lemmaID"] for t in ws if t["lemmaID"] not in known})
                windows.append((coverage([t["lemmaID"] for t in ws], known), i, len(ws), new))
        for cov, i, n, new in sorted(windows, reverse=True)[:2]:
            first = (ss[i].get("sentence_translation") or "").strip().replace("\n", " ")[:60]
            print(f"      ten sentences from {i:3}: {n:3} words, {cov:4.1f}% covered, {new:2} new words  | {first}")

    print("\n== Monuments and short texts nearly readable now (40+ words, 90%+ hieroglyphs)")
    rows = []
    for tid, ss in texts.items():
        if ss[0].get("date") != MIDDLE:
            continue
        ws = [t for s in ss for t in words(s)]
        if len(ws) < 40 or sum(1 for t in ws if t.get("hiero")) < 0.9 * len(ws):
            continue
        first = (ss[0].get("sentence_translation") or "").strip().replace("\n", " ")[:70]
        rows.append((coverage([t["lemmaID"] for t in ws], known), len(ws), ss[0].get("corpus"), tid, first))
    for cov, n, corpus, tid, first in sorted(rows, reverse=True)[:10]:
        print(f"  {cov:4.1f}%  {n:4} words  {corpus:16} {tid}  | {first}")


if __name__ == "__main__":
    main(sys.argv)
