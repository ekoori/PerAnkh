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
    # Unit 15. Each has a look-alike: jw "wrong" (21990), ḫm "be parched" (116860), tm "cease"
    # (171980), jꜣwt "office" (20430). tm "not to" is 171990 in corpus v18 and 854578 in AES; ntj
    # is also lemmatised as a noun, "the one who is" (400647).
    "w:jw2": ["21940"], "w:xm": ["116910"], "w:tm": ["171990", "854578"], "w:jAwt": ["20390"],
    "w:ntj": ["89850", "400647"],
    # Section 3. Look-alikes: the name Ky and ky "other" (400645); jꜣw "praise" and jꜣw "old man";
    # jwj "be boatless" (22260) and jwj "come"; sbꜣ "gate", "pupil" and "star" (131180); ḏt
    # "estate" and ḏt "eternity"; zwꜣ "cut off" (129730) for swꜣ "pass by"; mjtj "copy" (67940);
    # wꜥrt "leg" (44740) and "flight" (44730), where the course means "district", which this
    # corpus lacks: pinned to the AES place name. The rest are spelled differently in the TLA
    # (ḥꜣb for ḥb, mz for ms, ꞽm.ꞽ-rʾ, the enclitic ꞽr for rf) or are names.
    "w:Kay": ["400526"], "w:jAw": ["20360"], "w:jAw2": ["20380"], "w:jwj2": ["22260", "856211"],
    "w:jwj": ["21930", "851688"], "w:sbA": ["131200"], "w:sbA2": ["131230"], "w:Dt2": ["181420"],
    "w:swA": ["129740"], "w:mjtj": ["67930"], "w:gs": ["168250", "92390"], "w:wart": ["859963"],
    "w:mAa": ["66460", "500218"], "w:zni": ["854546"], "w:qn": ["160940"], "w:Dar": ["182760", "854589"],
    "w:Hwj": ["102730", "854530"], "w:nw": ["80730", "851519"], "w:xt2": ["121230", "400152"],
    "w:Drw": ["184990", "92500"], "w:Tz": ["177200", "854581"], "w:Hb": ["103300"], "w:ms2": ["74700"],
    "w:spAt": ["132420", "854544"], "w:jmjrA": ["25550", "855661"], "w:rf": ["28170"],
    "w:Dws": ["183050"], "w:qr": ["860315"], "w:HAy": ["101050"], "w:Mrtj": ["702231"],
    "w:xprkAra": ["400021"], "w:jnjjtf": ["400012", "707683"],
    # Section 4, the Book of the Dead. Look-alikes: jrj "belonging to" and jrj "do" (28550); jmj
    # "who is in" and jmj "give!" (25170); ꜥt "limb" and ꜥt "room" (34540); ḥm "servant" and ḥm
    # "majesty" (104690); mwt "weight" and mwt "mother" (69040); jꜣwt "office" and jꜣwt "old
    # age"; ḫt "fire" and ḫt "wood" (121200); ẖꜣt "marsh" and ẖꜣt "corpse" (122220); prt "the
    # season" and prt "seed". The rest are spelled differently in the TLA or missing from it, and
    # are pinned to their AES lemmas.
    "w:jrj2": ["850583", "851428"], "w:jmj": ["25130"], "w:at": ["34550", "854495"], "w:Hn": ["105900"],
    "w:kAt": ["163010"], "w:kA3": ["162850"], "w:nw2": ["80840"], "w:Hm": ["104680"], "w:jAwt2": ["20430"],
    "w:mnt": ["69770"], "w:mwt3": ["69060"], "w:xAt": ["122270"], "w:xt3": ["113020"], "w:bnw": ["55590"],
    "w:prt2": ["60300"], "w:Dw": ["182840", "182900"], "w:jndHr": ["854522"], "w:rqw": ["96340"],
    "w:Snyt": ["155980"], "w:sxnS": ["142700"], "w:Swbtj": ["153120"], "w:j": ["20030"], "w:smH": ["135570"],
    "w:jmntt": ["26180"], "w:jAbtt": ["20590"], "w:zAwt": ["126390"], "w:sam": ["128840"],
    "w:wnDwt": ["47190"], "w:xAbt": ["122480"], "w:jwtt": ["550302"], "w:nmH": ["84350"],
    "w:sHqr": ["141100"], "w:srmj": ["139510"], "w:sfx": ["133790"], "w:dAdA": ["855348"],
    "w:dbH": ["178770"], "w:snmH": ["137520"], "w:nxn": ["87250"], "w:HAm": ["101400"], "w:abw": ["36740"],
    "w:Hrntt": ["108040"], "w:Abd": ["93"], "w:arqy": ["39740"],
    # Section 5, the Shipwrecked Sailor. Look-alikes: ḥknw "praise" (110860) and the oil (110870);
    # ḥꜥw "body" and ḥꜥw "ships" (101990); jtj "sovereign" (32930) and jtj "father"; ḫt "wood"
    # (121200) and ḫt "fire"; sbꜣ "star" (131180) and sbꜣ "gate"; ky "baboon" (163830) and ky
    # "other"; ꜣbw "elephant" (80) and ꜣbw "ceasing"; ḏꜣ "fire drill" (181490) and ḏꜣ "fishbone";
    # ḫrw "voice" and ḫrw "the fallen"; sšr "overlay" (145460) and sšr "milk"; ꜥrq "rear up"
    # (39600) and ꜥrq "finish"; mr "painful" and mr "bind"; šwj "free of" and šwj "dry"; šdj
    # "take" and šdj "recite"; bjꜣ "mining country" (54510) and bjꜣ "firmament"; qnj "embrace"
    # and qnj "amount to"; mḥ "cubit" (73330) and mḥ "fill"; dwꜣ "praise" and dwꜣ "rise early".
    # The rest are spelled differently in the TLA (mjn.t for mnjt, ꜥš for jꜣš, gjf for gwf, mmj
    # for mꜥmꜥ, nḥḏ.t for nḏḥyt, jbr for jbj, šsꜣ(.w) for sšꜣ, qrr for qrj) or are missing from it, and
    # are pinned to their AES lemmas. ḥḏ-tꜣ "daybreak" is two lemmas in both corpora: unpinned.
    "w:Hknw": ["110860"], "w:Hknw2": ["110870"], "w:Haw": ["101950", "854529"], "w:Haw2": ["101990"],
    "w:jtj": ["32930"], "w:xt4": ["121200"], "w:sbA3": ["131180"], "w:ky2": ["163830"], "w:Abw": ["80"],
    "w:DA": ["181490"], "w:xrw": ["120010"], "w:zxr": ["145460", "854550"], "w:arq": ["39600", "854499"],
    "w:mr": ["851222", "71790"], "w:Swj": ["152670"], "w:Sdj": ["158710", "854561"], "w:bjA": ["54510"],
    "w:qnj": ["161170", "161180"], "w:mH2": ["73330"], "w:dwA": ["177930", "854584"],
    "w:Ds": ["185370", "854591"], "w:stp": ["148170"], "w:xnt": ["119350"],
    "w:dwAw": ["600406", "178000", "400078"], "w:xnms": ["118260"], "w:rd": ["96600"], "w:kfj": ["164200"],
    "w:nbw": ["81680"], "w:bAw": ["53300"], "w:jzwt": ["31080"], "w:jAS": ["40890"], "w:zbt": ["131030"],
    "w:qAj": ["158990", "159110"], "w:mnjt": ["70140"], "w:HAtt": ["100450"], "w:Swyt": ["152880"],
    "w:jArrt": ["20830"], "w:Sps": ["400546", "153940"], "w:ssA": ["143800"], "w:qrj": ["161770"],
    "w:sSA": ["157030", "157090"], "w:jbj": ["23780"], "w:antjw": ["39010"], "w:mama": ["69540"], "w:nDHyt": ["86750"],
    "w:gwf": ["166670"], "w:xbswt": ["115920"], "w:rxnt": ["92280"], "w:mxd": ["65350"], "w:dmA": ["850991"],
    "w:makA": ["850573"], "w:gspr": ["168360"],
}
# Lemmas a course word covers besides its own: the course teaches N35 n as "to, for, of", which
# is both the preposition and the genitive n.ꞽ (79800 in corpus v18, 850787 in AES). jn "by" is
# also taught as the jn that points out a doer, "it is the heart that…" (26670 in corpus v18;
# AES's 851426 counts jn introducing a question too). zp "time" also covers zp 2, "twice", a
# lemma of its own in AES. wn "exist" is pinned above to wnn, of which it is the short form.
# mꜣꜥ-ḫrw is taught with its feminine, mꜣꜥt-ḫrw, which the corpora lemmatise apart.
# The Shipwrecked Sailor teaches compound prepositions through their parts (m-ẖnw "inside" as
# ẖnw, m-ḥrj-jb as ḥrj-jb, r-gs as gs, m-qꜣb-n as m-qꜣb, ḫft-ḥr as ḫft, r-mjtt-jrj as mjtt) and
# forms AES lemmatises apart: ntt and ntjw as ntj, snw "siblings" as sn, msw "children" as ms,
# wḥmyt "repeating" as wḥm, nfrt "good things" as nfr, ꜥꜣ "greatness" and wrr "be great" as ꜥꜣ
# and wr, the auxiliary wn, n=j-jmj "mine", =fj after a dual, mdw "speech" as mdwj. AES also
# spells wdf, rš and qn "be brave" wdfi̯, ršu̯ and qni̯, and reads zẖꜣw "writing" as zš.
EXTRA: dict[str, list[str]] = {
    "w:n": ["79800", "850788", "850787", "400984", "25160"], "w:jn": ["26670", "851426"], "w:zp": ["70011"],
    "w:mAaxrw": ["400178"],
    "w:Xnw": ["65370", "857318"], "w:Hrjjb": ["65190"], "w:gs": ["851526", "854572"], "w:mqAb": ["862797"],
    "w:xft": ["116790"], "w:mjtj": ["600407", "67961"], "w:ntj": ["89760", "89890"], "w:sn": ["136310"],
    "w:msj": ["74750"], "w:wHm": ["48530"], "w:nfr": ["83680"], "w:aA": ["34770"], "w:wr": ["47270", "47320"],
    "w:wn": ["550129"], "w:suff": ["10060"], "w:mdwj": ["78150"], "w:wdf": ["51620"], "w:rS": ["96210"],
    "w:qn": ["854565"], "w:jqr": ["850289"], "w:zXAw": ["144270"],
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
