# Changelog

## Unreleased

- The Path remembers where you were. It opens at the lesson you last opened, not at the next one
  to do, so going back over an old lesson no longer means finding it again each time; and coming
  back from a lesson, another tab or the reader finds the Path where you left it. The Codex, the
  Library and the Treasury keep their place too, and a text opens again where you left it.
- The back button goes back through the tabs you visited before it leaves the app; during a new
  section's reveal it ends the reveal.
- A flag at the top of the Path, for reporting a problem anywhere in the app or making a
  suggestion. Reports from any page can now be marked "Suggestion".
- The hieroglyph colours you choose in Settings apply everywhere outside the Codex: in exercises,
  answer choices and the answer shown after a mistake, on a new sign's card, in the lesson
  summary, on the lesson sheet, in the Library and in the Codex's word and grammar sheets. Signs
  spelled one by one (the palette for spelling a word) take the colour of their own role, so the
  palette never shows which signs belong to the answer.
- Settings explains each colour choice, and how near the tomb paints come to real painted
  hieroglyphs; a new card in lesson 3, "Why the signs are coloured", tells what Egyptian painters
  and scribes did.
- A word's own determinative is shown as one in its breakdown and colours, even a sign that is read
  in other words: the sun disk in hrw "day" marks a time, and is no longer said to be rꜥ. In
  ẖrt-hrw, "daily course", the sun disk alone writes hrw, and the breakdown says so.
- The walking legs' note says which way they walk: forward, the way the signs face.
- Tapping a sign for a hint says, there and then, that a right answer now counts as nearly right,
  so "Right — with a hint" comes as no surprise.
- Cards open their lesson: a grammar table, a note on how something works or a piece of culture
  comes before the words that use it, not after them.
- Lesson 34 teaches all five pronouns its card lists: ntṯ, "you" to a woman, and nts, "she", join
  jnk, ntk and ntf, each with its recording.
- The Treasury gives time studied in hours and minutes ("16 h 39 min").
- In the version from GitHub, Settings → About has a link for supporting Per Ankh on GitHub
  Sponsors.
- The app's code is optimised when it is built (R8, as Google Play asks of apps this size): the
  code is 3.2 MB instead of 21.2 MB, so there is less of it to load, and the APK on GitHub is
  44 MB instead of 50 MB.

## 0.5.0 (2026-11-16)

- Recordings a listener flagged were checked by ear, and 93 are now chosen by ear. Those found
  wrong were recorded again, some by another ElevenLabs model that keeps the short vowels the
  usual one drops: ḏ said as a hard g, jṯ said like the English letter H, the q of mj-qj=f said
  as g.
- Tapping a word in a phrase or a text says the word as it is written there: jt=f is "itef",
  not just the "it" of jt. 556 word forms have their own recordings.
- The doubled reed is heard as two sounds: jj "come" is "iyi", jj.t "iyet", and the "my" of
  dj=j "diyi" no longer disappears into one long "ee". 27 recordings redone.
- Recordings are made with the voice told it is speaking Arabic, which stops it reading some
  lines with an English accent and saying ḏ as a hard g ("gedet" for ḏd.t). The reported clips
  and every clip that failed its check have new takes.
- A word's sign-by-sign breakdown gives each sign the reading it has in that word: the throne is
  ws in Osiris and ꜣs in Isis, the feather šw in Shu, and the village sign is the word njwt,
  "town", not a silent determinative.
- The mouth sign's note: foreign l is written with r or n; the lion writes it only in Ptolemaic
  names such as Cleopatra's.
- After every answer, right or wrong, the answer can be heard again, also slowly.
- A right answer given after tapping a sign for a hint says "Right — with a hint", not
  "Nearly", which read as wrong.
- Long lines of hieroglyphs wrap onto the next row instead of shrinking; signs are never drawn
  smaller than a readable size. A cartouche fits its tile in the pairs exercise.
- Settings → Reading: High contrast (darker text and signs on a plain, pale page) and text size
  (Normal, Large, Larger).
- The path has small buttons under each unit: to the top, the previous and next unit, and the
  current lesson.
- A lesson's preview also shows what it practises again: "The four h's" shows all four.
- The Treasury's ledger: "% right" no longer counts near misses as wrong (they are shown
  apart), "Mastered" counts signs and words that are not fading, as the lines above it do, and
  study time leaves out long pauses with a lesson open.
- Grammar at a glance: sixteen grammar cards have a table (the suffix, dependent and
  independent pronouns, sḏm=f and sḏm.n=f, negation, the stative's endings, masculine and
  feminine, numbers, the king's five names, the seasons, "of"), and a lesson that teaches one
  opens with it.
- The codex has a third shelf, Grammar, with every card met so far; a search field for signs
  (by code, name, sound or meaning), words and grammar; and categories: the alphabet,
  determinatives and so on, or nouns, verbs and pronouns. On a tablet they stand in a panel
  beside the shelf.
- Hieroglyphs are coloured by what each sign does, with a key: in a word, signs that are read,
  read another way, repeat a sound, classify (determinatives) or are not read; in a sentence,
  the kind of word, with endings and suffix pronouns marked. Settings → Reading → Hieroglyph
  colours brings back the tomb paints, or plain ink. The codex keeps its paints, which show
  what you remember.
- Twelve new culture cards in the later units: letters to the dead, the cobra on the crown, the
  appeal to the living, Itjtawy, the sailor's fire, gods of gold and lapis lazuli, myrrh from
  Punt, sailing south, the colophon, Ptahhotep's staff of old age, the rowers' bead nets and the
  lector priest.
- About shows the build beside the version (the commit it was made from, and its day), and
  problem reports carry it too, so a test build between releases can be told apart.
- Per Ankh from GitHub can tell you when a new version is out: turn it on in About, and it looks
  once a day and sends one notification per release. Off unless turned on; the Play version
  leaves updates to Google Play.
- The codex on a phone gives the shelf the screen: a slim bar with the shelf's name and a
  search, and the shelves, categories and what the colours mean in a panel that slides in
  from the side. Grammar cards opened from the codex have the report flag.
- Answers may end a word in j or y alike (ḏḥwtj, ḏḥwty); the two strokes' card says the course
  writes j there, as the Thesaurus Linguae Aegyptiae does.
- The offering formula reads "An offering which the king gives, and Osiris, lord of Busiris",
  with a note on the older reading "to Osiris". jmꜣḫw, "the revered one", is spelled as Middle
  Kingdom texts most often spell it.
- wr and ꜥꜣ, both "great", are told apart; the basket's two words, nb "lord" and nb "every",
  are told apart by where they stand; phonetic complements are explained before the first word
  that uses one; and explanations name the sign that repeats a sound.
- A word to sound out says it is one on purpose: "Read it yourself".
- The serpent's question, asked twice in the story, says so where it comes back.
- An arrange exercise keeps each chosen word readable in its place; tapping it there sends it
  back.
- A text an earlier section reads stays in the library when a later, sealed section reads it
  again (the Sailor's opening line had disappeared).

## 0.4.1 (2026-11-05)

- Lessons no longer freeze for several seconds after Continue, sometimes long enough for Android
  to say the app is not responding. Choosing the wrong answers for a "What does this word mean?"
  question weighed every word in the course hundreds of times over, which grew slow once 0.4.0
  brought the course past 600 words; each word is now weighed once.

## 0.4.0 (2026-11-03)

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
- Section 7, The Magicians: units 39–42 read a whole tale from Papyrus Westcar. King Sneferu is
  bored, so his lector priest Djadjaemankh sends him boating on the palace lake, rowed by twenty
  beautiful women with oars of ebony and gold. When the lead rower's fish pendant of new turquoise
  falls into the water she stops rowing and will not take a copy, so Djadjaemankh lays one half
  of the lake on top of the other, finds the pendant on a potsherd and puts the water back. The
  king tells it all again to the magician, and those lines come back as they are. New grammar:
  the new "the", pꜣ, and pꜣy=s, "her"; a card on the tales told at Khufu's court. 62 words, 10
  signs, 2 cards and 76 lines, and an award for reading the whole tale, all voiced.
- The course's words now cover 73.6% of real Middle Egyptian text.
- The full readings follow Ancient Egyptian Sentences (AES), made from the Thesaurus Linguae
  Aegyptiae's database, and Settings → About credits it. Like its texts, the course is now shared
  under CC BY-SA 4.0; the code stays under the MIT licence.
- Problem reports say that they come from Per Ankh: BushLines, a field guide from the same people,
  sends its reports to the same server. The privacy policy says so.
- Fixes from testers' reports:
  - A line put together on its own may leave off the "and" that joins it to the line before:
    "you will die in your town" is right.
  - After a pairs exercise, tap any matched pair to hear it again.
  - Grammar cards with an Egyptian example say it aloud, when taught and after their question.
  - After a word exercise, the explanation says why a word is written in a different order
    from how it is read (nswt "king", ḥtp-dj-nswt) or has a sound no sign writes (the m of
    rmṯ). Such words are no longer asked to be transliterated sign by sign.
  - The card "What will happen" shows the whole of jw dpt r jj.t, with its r; the cards on
    sḏm.n=f and on wishes show the words their questions ask about.
  - jj, "to come", is said in two clear syllables.
  - nswt "king" explains why the sedge comes first, and "for the ka of the revered one" why its
    two n's look alike.

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
