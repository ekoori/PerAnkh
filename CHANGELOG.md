# Changelog

## Unreleased

- Tablets. On a tablet Per Ankh turns with the device, and each page keeps to a column in the
  middle of the screen instead of stretching across it; sheets and dialogs keep to the same width.
  Phones stay upright, as before.
- A lesson in progress is no longer lost when the screen turns or the window changes size (on
  Android 16 tablets turn every app, whatever it asks).
- On a short screen, such as a tablet on its side, the codex's heading scrolls away with the signs
  instead of leaving them a single row.
- The Settings sheet no longer fills most of a tall screen.
- The app says "this device" rather than "this phone".
- rmṯ "people" is written as Middle Kingdom scribes wrote it, without the owl: the m is read but
  not written.
- Unit 15, I Have Come: the stative ("I have come", "I was unharmed") and ḥr with a verb ("I am
  listening"). Two readings grow to their full passages. "The serpent speaks" now has the
  serpent's whole threat: ashes, a sailor too frightened to hear, and a ride in the serpent's
  mouth. "Ptahhotep on hearing" reads to the end of its praise, with old age for the son who
  listens.
- A reading that grows is read again, whole, at the end of the lesson that completes it. Until
  its new lines are learned the Library no longer marks it Read.
- Section 3, Lord of Abydos: units 16–19 read three Middle Kingdom stelae, every line. The herald
  Ky asks Osiris for offerings and lists the wrongs he never did. The general Imeny praises Osiris
  at his festivals and asks to walk with the god's followers forever. "A good man's stela" grows
  into the whole stela of Antef, chamberlain of Senusret I, from his appeal to passers-by to the
  king who made him a courtier. New grammar: relative forms ("what the king praises"), wishes
  ("may they say") and how a stela names its owner. 77 words, 37 signs, 4 grammar cards and 59
  lines, and two new awards, all voiced.
- Section 4, Going Forth by Day: units 20–25 read three Book of the Dead spells in full. The heart
  scarab spell (30B) begs the heart not to testify against its owner. The shabti spell (6) makes
  the little figure answer "Here I am!". Spell 125 takes the dead man into the Hall of the Two
  Truths, where he greets the gods and declares, wrong by wrong, what he has not done, until "I
  am pure, I am pure, I am pure, I am pure!". New grammar: jr "if" with the passive, and the s
  that makes a verb cause something ("to make weep"). 111 words, 44 signs, 8 cards and 86 lines,
  and three new awards, all voiced.
- Two awards no longer share an id with older ones, and the course tests now check that texts
  and awards have unique ids.
- Section 5, The Shipwrecked Sailor: units 26–34 read the whole story, every line, in five
  chapters. A follower cheers his gloomy master home from the south and tells his own tale: the
  voyage, the storm and the wave that left him alone on an island; the serpent of gold and lapis
  lazuli that asks him twice who brought him there; the star that fell; and the ruler of Punt,
  who sends him home laden with myrrh and ivory. The scribe who copied the papyrus signs it at the
  end. "The Shipwrecked Sailor", "The serpent speaks" and "The serpent's promise" grow into
  chapters 1, 3 and 4, and where the papyrus says a line again, the same line comes back. New
  grammar: sḏm.jn=f, "then he said", and n sḏm.t=f, "before it came"; four more cards on the sea,
  Punt, a story told twice and a tale within a tale. 155 words, 38 signs, 6 cards and 172 lines,
  and an award for reading the whole work, all voiced.
- The Library shows a long work's chapters together, under the work's name and in story order,
  with how much of the whole you can read.
- Section 6, The Teachings: units 35–38 read Ptahhotep's last words, from "if you listen to these
  things I have told you" to the end of the papyrus. "Ptahhotep on hearing" grows at both ends into
  the whole epilogue: good words that last for ever, the official whose lips are exact and whose
  ears hear what is useful, the fool who will not listen, "a dead man, every day", and the son who
  listens and grows old in honour. Ptahhotep closes with his own life, a hundred and ten years, and
  the papyrus with the same closing words as the Sailor's. New grammar: the infinitive ("hearing
  is good"), nfr.wj, "how good!", and jwtj, "who does not"; a card on the hundred-and-ten-year
  life. 54 words, 10 signs, 4 cards and 106 lines, and an award for reading the whole epilogue,
  all voiced.
- The course's words now cover 72.3% of real Middle Egyptian text.

## 0.3.2 (2026-10-24)

- The first version on Google Play. Google Play keeps its copies up to date, so their About page
  links to Per Ankh on Google Play instead of looking for releases on GitHub. The copy from GitHub
  works as before. Nothing else changes, and there is no 0.3.1.

## 0.3.0 (2026-10-22)

- Sections. Units 1–9 are Section 1, The Scribe’s School; units 10–14 are Section 2, The
  Storytellers, which waits at the end of the path as a sealed door until Section 1 is finished.
  The seal then breaks with a short ceremony, shown once, and the section’s units, readings, words
  and amulets appear. Tester mode can preview the ceremony from Settings.
- Unit 13, Not and Don’t: n “did not” and n … .n “cannot”, orders and m “don’t”, ending with a
  good man’s stela (“I gave bread to the hungry and beer to the thirsty”).
- Unit 14, Face, Mouth, Eye: body words and the prepositions built on them, place, way, time and
  foreign land, ending with the serpent of the Shipwrecked Sailor: “Who brought you, little one?”
- The course’s words now cover 60% of real Middle Egyptian text, up from 57%.
- Settings has three tabs: Settings, Testing and About. About shows the version you have and
  whether a newer one is out, asking GitHub (or the Per Ankh server, when GitHub cannot be
  reached), with a link to the newest release. The privacy policy describes the question.
- Writing a phrase in hieroglyphs is marked right again: the lesson compared the
  transliterations under the tiles with the signs, so every answer counted as wrong.
- Number exercises no longer play a numeral sign’s own sound (“medj”) after the answer.
- Sound-it-out picks the words you have sounded out least, not the same few every lesson.
- ṯ is said as the one sound it is: ṯw “you” had come out as “te-shu”. A compound with a
  one-letter part is said as one word, so m-ꜥ “in the hand of” no longer stops in the middle,
  and the pronunciation guide writes it as one (“imenemḥat”). All 27 clips with ṯ or such a
  compound are re-recorded.

## 0.2.0 (2026-10-22)

A new app as far as Android is concerned: the app id is now `app.hrana.perankh` and builds are
signed with the project's own key. It installs next to v0.1.0 rather than replacing it, and
progress made in v0.1.0 does not carry over; uninstall v0.1.0 once you have moved.

- Three new units, 19 lessons: little words (of, every, other, like, before, under, upon),
  pronouns for “me” and “you” and for the plural, and how stories are told (the past, ꜥḥꜥ.n
  “then”, verbs of going, seeing, bringing and speaking). Two new readings: Ptahhotep’s praise of
  listening, and the serpent’s promise from the Shipwrecked Sailor. The course’s words now cover
  57% of real Middle Egyptian text, up from 50%.
- Word frequencies no longer count look-alike words: jt “father” had been counted as jt “barley”,
  ḥmt “woman” as ḥmt “copper”.
- Targets Android 16 (API 36), as Google Play requires.
- Anyone can report a problem with the flag on a page; tapping Send uploads it to the Per Ankh
  server over HTTPS (or as soon as the phone is back online). Reports carry no name, account or
  device id. New privacy policy (PRIVACY.md). Tester mode keeps the testing tools.
- Alphabet signs are said as their Egyptological sounds ("a", "i", "be", "ḥe", "khe") instead
  of Arabic letter names.
- Matching pairs no longer plays the wrong word after the last pair.
- MIT licence (LOVI Tech); third-party terms in NOTICE.md.

## 0.1.0 (2026-10-21, GitHub pre-release)

First build: nine units, 47 lessons, pronunciation by an Arabic voice reading Egyptological
renderings, tester mode with local reports, word frequencies from the Thesaurus Linguae
Aegyptiae.
