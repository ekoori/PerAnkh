# Per Ankh — Middle Egyptian

An Android app for learning to read Middle Egyptian hieroglyphs: signs, sounds, words, grammar,
culture, and real texts. Built on the skeleton of `dev/koleej` (Kotlin, Compose, multi-module,
DataStore), with a papyrus-and-paint look.

The name is pr-ꜥnḫ, "House of Life": the temple school where scribes were trained and books were
copied. It is written with two signs the course teaches in Unit 3, the house 𓉐 and the ankh 𓋹.
The application id is `app.hrana.perankh`, after hrana.app like its sibling app.hrana.twa; the
code's packages and Gradle modules keep the working name `pentalingo`. Open source under the
[MIT licence](LICENSE), published by LOVI Tech; third-party terms in [NOTICE.md](NOTICE.md).

**Download:** [the latest release](https://github.com/ekoori/PerAnkh/releases/latest), an APK for
Android 8.0 or newer; Google Play is planned ([RELEASING.md](RELEASING.md)). It works offline; the
only permission is internet access, used solely to send the problem reports people choose to send
([privacy policy](PRIVACY.md)). The v0.1.0 test build was a pre-release under the old id
`com.krbavcic.pentalingo`, signed with a debug key: later versions install next to it, not over it,
and its progress does not carry over.

<p align="center">
  <img src="docs/demo.gif" width="320" alt="A lesson in Per Ankh: the path, a new word, a listening question, matching pairs, an explained mistake, sounding out a word never taught, the lesson summary, then the codex, a word, a royal inscription and the amulets">
</p>

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
    <td align="center"><img src="docs/screenshots/tester-report.png" alt="A tester report"><br><sub>Tester mode: report what is on screen</sub></td>
  </tr>
</table>

## What makes it not Duolingo

- **Memory, not streaks.** Every sign, word, phrase and idea has its own memory, modelled with
  FSRS (stability + difficulty, power-law forgetting). Items come back when recall is about to
  drop below 85% — not on a rota, and not when you obviously still know them.
- **Repeat, but not too much.** Same-day repetitions are capped by the scheduler
  (`Scheduler.SHORT_TERM_CEILING`): drilling an item ten times in one sitting cannot fake a month
  of memory. A wrong answer brings the item back three exercises later, at most twice per sitting.
- **Difficulty that climbs with the item.** Each exercise kind has a tier — recognise, recall,
  produce. An item moves up the ladder as its memory strengthens (never more than one rung above
  the hardest it has passed), and a struggling learner drops a tier while a cruising one gets
  stretched. Easy exercises earn less memory than hard ones (`Scheduler.tierWeight`).
- **Mistakes are allowed.** No hearts, no game over. Near misses — ḥ written as h, the right
  signs in the wrong order, a spelling missing only its determinative, ten strokes where a scribe
  would write a hobble — are recognised and named, not marked wrong.
- **Distractors that learn.** Wrong options are drawn from what you have actually met, sized like
  the answer, and weighted towards look-alikes and sound-alikes once an item is known. Any pair you
  confuse is recorded and keeps being offered side by side until you stop confusing it.
- **Transfer, not just recall.** "Sound it out" exercises show a word you were never taught, built
  only from signs you know — Champollion's method.
- **Progress that means something.** The path counts signs, words and *texts you can read*. The
  library shows real inscriptions with the words you know painted in. The codex fills with the
  colours of a tomb wall as you learn, and fades back to ink as memory fades, and says how much of
  a real page your words fill — word frequencies counted in the Thesaurus Linguae Aegyptiae's
  Middle Egyptian sentences (the whole course reaches 57%). Amulets mark abilities (telling the
  four h's apart, relearning what you forgot, reading the offering formula, knowing a quarter of
  every page), each with its own piece of Egyptian lore.
- **Sounds Egyptian really had.** Pronunciation follows the Egyptological convention, generated
  from the transliteration by `core-script` `Pronounce.kt`: e between consonants, and ḥ, ꜥ, ḫ, ẖ, q
  kept distinct (ptḥ is "peteḥ", not "Ptah"). A sign on its own is said as its sound: ꜣ and ꜥ as
  "a", j as "i", every other letter with a short e ("be", "he", "ḥe", "khe"). Clips are spoken by
  a native Arabic voice reading fully vowelled Arabic script, the one input that reliably produces
  those consonants.

## Problem reports and tester mode

A flag in the corner of every lesson page, Codex sheet and the reader lets anyone report a
problem. A report holds a category, a note and everything on screen — lesson, exercise, prompt,
options and accepted answers, the item's spelling, transliteration, respelling, IPA, the Arabic
text the voice was given and the clip file, the answer given and the verdict. Tapping **Send**
uploads it at once, over HTTPS, to the Per Ankh server at perankh.hrana.app, where reports wait in
SQLite to be triaged with `tools/reports.sh` (see [server/README.md](server/README.md)). Offline,
it waits on the phone and goes out the next time the app starts.

Settings → Testing → *Tester mode* adds tools for testing the course: *Open any lesson* (start
locked lessons), *Answer button* (a "solve" chip), and the reports kept on the phone, which can be
shared or pulled with `adb pull` from
`/sdcard/Android/data/app.hrana.perankh/files/reports/reports.jsonl`.

## Modules

| module | what |
|---|---|
| `core-script` | Gardiner sign table (generated from the font), Manuel de Codage parser, quadrat layout, transliteration |
| `core-model` | items, lessons, course, memory, progress, awards — pure Kotlin, serialisable |
| `core-engine` | FSRS scheduler, exercise factory, answer checker, session planner, recording, awards |
| `content` | the Middle Egyptian course: 12 units, 66 lessons, ~125 signs, 138 words, phrases, texts; word frequencies |
| `data` | `ProgressStore`: one JSON document in DataStore; `ReportStore`: tester reports |
| `core-ui` | papyrus, palette, hieroglyph renderer, painted buttons, audio, sound effects |
| `feature-path` / `feature-lesson` / `feature-codex` | the screens |
| `app` | activity and navigation |
| `server/` | the report API (Python, SQLite), its triage tool and deployment ([server/README.md](server/README.md)) |

Everything below `data` is plain JVM and tested without a device: `gradlew :core-script:test
:core-engine:test :content:test`. The content tests walk the whole course with a perfect learner
(every exercise must be answerable) and a careless one (nothing may get stuck). The server's tests
run with `cd server && python -m unittest discover -s tests`.

## Tools

- `tools/gen_signs.py` — regenerates `signs.tsv` (Gardiner code → codepoint → ink box) from the
  bundled font, via Python's Unicode database.
- `tools/gen_sfx.py` — synthesises the harp sound effects (Karplus–Strong, pentatonic).
- `tools/voice.py` — records the pronunciation clips with ElevenLabs: Eleven v4, voice Chaouki (a
  native Modern Standard Arabic narrator), fed the Arabic-script rendering that `gradlew
  :content:run` exports to `tools/out/speech.json`. Each take is transcribed back by Scribe in
  Arabic (and English, for p) and passes only if every hard consonant (ḥ ꜥ ḫ q š ḏ) is heard; failing
  takes are retried with other seeds and spellings, and short words that still fail are said inside
  an Arabic frame ("qul: …") and cut out at the pause (needs ffmpeg). Takes are content-addressed,
  so nothing is billed twice; `rescore` re-judges them for free, `report` lists every clip with what
  was heard, and `prune` moves unused takes to `tools/out/takes`. The key comes from
  `$ELEVENLABS_API_KEY`, or from a file outside the repository (`$ELEVENLABS_KEY_FILE`, default
  `~/dev/11labs/.env`), read by path and never stored here.
- `tools/reports.sh` — triages tester reports on the server over SSH: `list`, `show`, `resolve`,
  `export` (see [server/README.md](server/README.md)).
- `tools/tla.py` — counts lemma frequencies in the Thesaurus Linguae Aegyptiae's Earlier Egyptian
  corpus (download it to `tools/out/tla/`; run with `python -I`), reports how much of real Middle
  Egyptian the course's words cover and which frequent words it lacks, and with `--write`
  regenerates `content/.../Frequency.kt`.
- `tools/aes.py` — measures the course against AES, Simon Schweitzer's corpus of 101,796 TLA
  sentences (CC BY-SA 4.0; download it to `tools/out/corpus/aes/`; run with `python -I`): coverage
  by period and kind of text, the commonest words not yet taught, verb forms by frequency, what
  proposed units would add, and passages of the great stories that are nearly readable. With
  `--show TEXT_ID` it prints a text with the words the course does not teach in brackets.

## Release signing

Releases are signed with the project's own key, which is not in the repository: alias `per-ankh` in
a PKCS12 keystore, `~/.android/per-ankh-release.jks` on the machine that builds them. The build
reads where it is and its password from Gradle properties in `~/.gradle/gradle.properties` (never
the repository's own `gradle.properties`):

```properties
perAnkhKeystore=C:/Users/you/.android/per-ankh-release.jks
perAnkhKeystorePassword=…
```

or from the environment variables `PER_ANKH_KEYSTORE` and `PER_ANKH_KEYSTORE_PASSWORD`. Write the
path with forward slashes: a backslash is an escape in a properties file. `perAnkhKeyPassword` /
`PER_ANKH_KEY_PASSWORD` is needed only by a key whose password differs from its keystore's (JKS
allows that; keytool's PKCS12 does not). Without `perAnkhKeystore`, `assembleRelease` still works
but signs with the debug key, and Gradle warns that the APK will not install over a published
release. The key was made with

```
keytool -genkeypair -keystore per-ankh-release.jks -storetype PKCS12 -alias per-ankh -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Igor Krbavcic"
```

Build with `gradlew :app:assembleRelease`, and before attaching
`app/build/outputs/apk/release/app-release.apk` to a release check its signer with `apksigner`
from the SDK's `build-tools` (anyone can check a downloaded APK the same way):

```
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

It must report `CN=Igor Krbavcic` with certificate SHA-256 digest
`f7ba04de0f40ee4ac623e0c61ecea7cccbf877044817cb13439445aae9d95773`, never `CN=Android Debug`.

**Back up the keystore and its password together, away from the build machine** (a password
manager can hold both). Android installs an update only if it is signed with the same key as the
app already installed: without this key no later release could reach an existing install, and
everyone would have to uninstall and lose their progress. That is what v0.1.0 users have to do
once now, because v0.1.0 was signed with a development machine's debug key. Debug builds still
use the debug key, so on a test phone a debug build and a release build cannot replace each other
either: uninstall when switching between them.

## Licences

The code and course content are MIT-licensed, © 2026 LOVI Tech ([LICENSE](LICENSE)). The fonts
(SIL Open Font License 1.1), the word frequencies (CC BY-SA 4.0, from the Thesaurus Linguae
Aegyptiae) and the pronunciation clips (generated with ElevenLabs) keep their own terms, listed in
[NOTICE.md](NOTICE.md). Store listing material for Google Play is in [docs/play](docs/play).
