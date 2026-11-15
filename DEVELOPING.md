# Developing Per Ankh

How the app is built and tested, how its course engine works, the tools behind the course and its
voice, and how releases are signed. The app is described for learners in the
[README](README.md); versions and release channels are in [RELEASING.md](RELEASING.md), the
report and release server in [server/README.md](server/README.md), and the Google Play listing in
[docs/play](docs/play).

## The project

An Android app in Kotlin and Jetpack Compose: multi-module, with DataStore for progress and a
papyrus-and-paint look, built on the skeleton of `dev/koleej`. The application id is
`app.hrana.perankh`, after hrana.app like its sibling app.hrana.twa; the code's packages and
Gradle modules keep the working name `pentalingo`. The v0.1.0 test build was a GitHub pre-release
under the old id `com.krbavcic.pentalingo`, signed with a debug key, so later versions install
next to it rather than over it.

## Building and testing

You need JDK 17 and the Android SDK with platform 36; Android Studio brings both.

```bash
gradlew :app:installGithubDebug    # a debug build, on the connected phone
gradlew :app:assembleGithubRelease # the release APK for GitHub; see "Release signing"
gradlew :app:bundlePlayRelease     # the release bundle for Google Play
gradlew :core-script:test :core-engine:test :content:test :data:testDebugUnitTest
cd server && python -m unittest discover -s tests
```

The app has two flavours, `github` and `play`: the same app, with the same app id and key, except
that the Play build's About page links to Google Play instead of looking for releases on GitHub
(see "The newest release").

Everything below `data` is plain JVM and tested without a device. The content tests walk the
whole course with a perfect learner (every exercise must be answerable, and arranging answers are
built from their tiles as a learner would) and a careless one (nothing may get stuck).

## Modules

| module | what |
|---|---|
| `core-script` | Gardiner sign table (generated from the font), Manuel de Codage parser, quadrat layout, transliteration, pronunciation |
| `core-model` | items, lessons, sections, course, memory, progress, awards — pure Kotlin, serialisable |
| `core-engine` | FSRS scheduler, exercise factory, answer checker, session planner, recording, awards |
| `content` | the Middle Egyptian course: 7 sections, 42 units, 295 lessons, ~277 signs, 629 words, phrases, texts; word frequencies |
| `data` | `ProgressStore`: one JSON document in DataStore; `ReportStore` and `ReportUploader`: problem reports; `ReleaseCheck`: the newest release, for About |
| `core-ui` | papyrus, palette, hieroglyph renderer, painted buttons, audio, sound effects, the report flag |
| `feature-path` / `feature-lesson` / `feature-codex` | the screens |
| `app` | activity, navigation and the settings sheet |
| `server/` | the report and release API (Python, SQLite), its triage tool and deployment ([server/README.md](server/README.md)) |

## How the course engine works

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
  only from signs you know — Champollion's method — preferring the words sounded out least.
- **Progress that means something.** The path counts signs, words and *texts you can read*. The
  library shows real inscriptions with the words you know painted in. The codex fills with the
  colours of a tomb wall as you learn, and fades back to ink as memory fades, and says how much of
  a real page your words fill — word frequencies counted in the Thesaurus Linguae Aegyptiae's
  Middle Egyptian sentences (the whole course reaches 74%). Amulets mark abilities (telling the
  four h's apart, relearning what you forgot, reading the offering formula, knowing a quarter of
  every page), each with its own piece of Egyptian lore.
- **Sections, sealed until you get there.** `CourseSection`s after the first wait at the end of
  the path as a sealed door with a name and a promise; finishing everything before one breaks its
  seal with a one-time reveal (`Progress.revealed`), and until then its items, texts and amulets
  are held back from the codex, the library and the treasury.
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
it waits on the phone and goes out the next time the app starts. BushLines reports to the same
server, so each upload names its app (`BuildConfig.REPORTS_APP`, `perankh`).

Settings → Testing → *Tester mode* adds tools for testing the course: *Open any lesson* (start
locked lessons, and see sealed sections' contents), *Answer button* (a "solve" chip), *Preview
the section reveal*, and the reports kept on the phone, which can be shared or pulled with
`adb pull` from `/sdcard/Android/data/app.hrana.perankh/files/reports/reports.jsonl`.

## The newest release

Settings → About shows the installed version and looks up the newest full release: first from
GitHub's API (`api.github.com/repos/ekoori/PerAnkh/releases/latest`), then, if that fails, from
the Per Ankh server's copy of the same answer (`/v1/latest`). An answer counts only if its page is
under `github.com/ekoori/PerAnkh/releases/`, and the About page links there: a newer release gets
a button, the current one a link to its notes. `data` `ReleaseCheck.kt`; the URLs are build
config fields in `app/build.gradle.kts`. The question carries only the app's version, and
[PRIVACY.md](PRIVACY.md) says so.

That is the `github` flavour. A Google Play build must not offer updates from outside Play (the
Device and Network Abuse policy), so in the `play` flavour, where `BuildConfig.STORE_URL` is set,
About never asks and links to Per Ankh on Google Play instead.

The `github` flavour also offers "Tell me when a new version is out" (off by default): a daily
`JobScheduler` job asks the same question and posts one notification per new release
(`app/src/github`: `UpdateNotices`, its manifest with the notification permission and the job).
The `play` flavour has a stand-in that offers nothing (`app/src/play`).

Every build carries the commit it was made from (`BuildConfig.BUILD_COMMIT`, with `+` when the
working tree had uncommitted changes) and that commit's day (`BUILD_DAY`), read from git when
Gradle configures the app. About shows them under the version, and problem reports send them
with it ("0.4.1 (401, 2ce2a2b)"): a test build installed between releases carries the last
release's version.

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
  was heard, and `prune` moves unused takes to `tools/out/takes`. Takes are made with the model
  told the text is Arabic (`language_code`), which keeps lines from drifting into an English accent
  and ḏ from coming out as a hard g; the clips recorded before that keep their takes. `generate
  --takes N` records several takes to choose from, `--new-takes N` caps what one run may pay for,
  `takes ID` lists a clip's takes, and `pin ID FILE` ships a take chosen by ear
  (`tools/voice_pins.json`), whatever the check says. For a clip a listener turned down,
  `tools/voice_variants.json` lists other ways to write it for the voice, tried first: a
  spelling (a sukun, a split syllable, commas) or another model
  (`{"text": …, "model": "eleven_multilingual_v2"}`, which keeps short vowels Eleven v4 drops but
  loses ʿayin). Besides items, word forms get clips (`f:jt=f`):
  tapping a word in a phrase plays the form as written there. The key comes from
  `$ELEVENLABS_API_KEY`, or from a file outside the repository (`$ELEVENLABS_KEY_FILE`, default
  `~/dev/11labs/.env`), read by path and never stored here.
- `tools/review.py` — the clips checked by ear, in a browser (`python -X utf8 tools/review.py`,
  localhost only): every clip that needs a listener first (reported, changed by a pronunciation
  rule, failing the check, new word forms), with what it should say and every take recorded for
  it. A verdict, tags and a comment per clip go to `tools/out/review/review.json`; `review.py
  apply` pins the takes approved by ear and lists the retakes asked for. Clips flagged while
  triaging are in `tools/out/review/flags.json` (not committed: it quotes testers' reports).
- `tools/reports.sh` — triages problem reports on the server over SSH: `list`, `show`, `resolve`,
  `export` (see [server/README.md](server/README.md)). BushLines reports to the same server, so
  `list` shows both apps' reports; `--app perankh` keeps to Per Ankh's.
- `tools/reports_xlsx.py` — the whole report database as an Excel workbook in `tools/out/reports/`:
  every table and column, timestamps as UTC dates, each report's context one field per row, the
  schema. `tools/reports_snapshot.py` reads the database on the server, sent over SSH (read-only,
  nothing to deploy); the workbook is written with the standard library, formulas with their
  results filled in.
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
allows that; keytool's PKCS12 does not). Without `perAnkhKeystore`, a release build still works
but signs with the debug key, and Gradle warns that the APK will not install over a published
release. The key was made with

```
keytool -genkeypair -keystore per-ankh-release.jks -storetype PKCS12 -alias per-ankh -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Igor Krbavcic"
```

Build with `gradlew :app:assembleGithubRelease`, and before attaching
`app/build/outputs/apk/github/release/app-github-release.apk` to a release check its signer with
`apksigner` from the SDK's `build-tools` (anyone can check a downloaded APK the same way):

```
apksigner verify --print-certs app/build/outputs/apk/github/release/app-github-release.apk
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
[NOTICE.md](NOTICE.md).
