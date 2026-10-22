# Per Ankh: learn to read hieroglyphs

Per Ankh is a free Android app that teaches you to read the hieroglyphs of ancient Egypt, the
way Egyptologists learn them: sign by sign, then words, grammar and real inscriptions. Every sign,
word and phrase is spoken aloud, and a lesson takes a few minutes.

**[Download the latest version](https://github.com/ekoori/PerAnkh/releases/latest)** for Android
8.0 or newer · free, no ads, no account · works offline · [how to install it](#install)

<p align="center">
  <img src="docs/demo.gif" width="320" alt="A lesson in Per Ankh: the path, a new word, a listening question, matching pairs, an explained mistake, sounding out a word never taught, the lesson summary, then the codex, a word, a royal inscription and the amulets">
</p>

## What you learn

Middle Egyptian, the classical language of ancient Egypt: the language of its great stories and
of inscriptions on temples and tombs for two thousand years. You start with the alphabet signs
and how they sound, build up to words, grammar and royal names, and by the end of the first
section you read an offering formula from a tomb stela and the opening lines of two of the great
stories of the Middle Kingdom. What comes after that waits behind a sealed door on your path,
and opens when you get there.

## How it teaches

- **It remembers what you know.** Every sign and word has its own memory in the app. Each one
  comes back just before you would forget it, not on a fixed schedule, and not when you clearly
  still know it.
- **Mistakes are part of it.** No hearts, no game over. Every mistake is explained, and a near
  miss, like the right signs in the wrong order, counts as a near miss rather than a failure.
- **You work things out.** "Sound it out" shows you a word you were never taught, made of signs
  you know, and you read it, the way Champollion deciphered the script.
- **Real texts, real progress.** The library holds real inscriptions with the words you know
  painted in, and the codex tells you how much of a real page of Middle Egyptian you can already
  read.
- **The sounds Egyptian had.** Words are spoken in the pronunciation Egyptologists use, keeping
  the sounds English does not have: the breathy ḥ, the throaty ʿayin, kh and q.
- **Amulets for real abilities**, like telling the four h's apart or reading the offering
  formula, each with a piece of Egyptian lore.

<table>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/path.png" alt="The path"><br><sub>The path, unit by unit</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/new-word.png" alt="A new word"><br><sub>A new word, sign by sign</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/listen.png" alt="A listening question"><br><sub>Which sign spells the sound?</sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/pairs.png" alt="Matching pairs"><br><sub>Matching pairs</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/mistake.png" alt="An explained mistake"><br><sub>A mistake, explained</sub></td>
    <td align="center"><img src="docs/screenshots/sound-it-out.png" alt="Sounding out a new word"><br><sub>Sound out a word never taught</sub></td>
    <td align="center"><img src="docs/screenshots/lesson-complete.png" alt="Lesson summary"><br><sub>The lesson summary</sub></td>
    <td align="center"><img src="docs/screenshots/codex.png" alt="The codex"><br><sub>The codex: how much of a real page you can read</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/word.png" alt="A word sheet"><br><sub>A word: how it is said, how common it is</sub></td>
    <td align="center"><img src="docs/screenshots/reader.png" alt="Royal names in the library"><br><sub>Real inscriptions in the library</sub></td>
    <td align="center"><img src="docs/screenshots/treasury.png" alt="The treasury of amulets"><br><sub>Amulets for abilities</sub></td>
    <td align="center"><img src="docs/screenshots/tester-report.png" alt="Reporting a problem"><br><sub>Report a problem from any page</sub></td>
  </tr>
</table>

## Install

Per Ankh is not on Google Play yet, so you install it from its download page instead. It takes a
minute, on the phone itself:

1. Open **[the latest release](https://github.com/ekoori/PerAnkh/releases/latest)** in your
   phone's browser.
2. Under **Assets**, tap the file whose name ends in **.apk** (such as `PerAnkh-0.3.0.apk`), not
   "Source code". The browser downloads it.
3. Open the downloaded file, from the browser's download message or from the Files app.
4. The first time, Android says your browser (or Files app) is not allowed to install apps. Tap
   **Settings**, switch on **Allow from this source**, then go back.
5. Tap **Install**. If Google Play Protect offers to scan the app first, let it.

You can switch *Allow from this source* off again afterwards. It is usually under Settings →
Apps → Special app access → Install unknown apps; the names differ a little from one phone maker
to another.

### Updating

Open the settings (the gear at the top of the path) and choose **About**: it shows the version
you have and tells you when a newer one is out, with a button that takes you to it. Download and
install it the same way. It installs over the version you have, and your progress stays.

## Questions

**Does it cost anything, or show ads?** No. Per Ankh is free, has no ads and needs no account.

**Does it need the internet?** No, it works offline. It goes online only to send a problem report
you choose to send, and to see whether a newer version is out when you open About.

**What happens to my data?** Your progress stays on your phone. The [privacy policy](PRIVACY.md)
has the details.

**Something in a lesson looks wrong.** Tap the flag in the corner of the page, say what is wrong
and tap Send. The report includes what was on screen, so a few words are enough.

**I have version 0.1.0, the test version.** Later versions are a separate app: install the latest
one and it appears next to 0.1.0. Progress from 0.1.0 does not carry over. Once you have moved,
uninstall 0.1.0.

**Will it be on Google Play?** Yes, with version 1.0.

## About

The name is pr-ꜥnḫ, "House of Life": the temple school where scribes were trained and books were
copied. You learn its two signs, the house 𓉐 and the ankh 𓋹, in Unit 3.

Per Ankh is made by LOVI Tech and is open source under the [MIT licence](LICENSE). The fonts, the
word frequencies and the voice keep their own terms, listed in [NOTICE.md](NOTICE.md). The
changes in each version are in the [changelog](CHANGELOG.md).

**For developers:** how the app is built, the course engine, the tools and the server are
described in [DEVELOPING.md](DEVELOPING.md).
