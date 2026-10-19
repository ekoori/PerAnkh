# Pentalingo — Middle Egyptian

An Android app for learning to read Middle Egyptian hieroglyphs: signs, sounds, words, grammar,
culture, and real texts. Built on the skeleton of `dev/koleej` (Kotlin, Compose, multi-module,
DataStore), with a papyrus-and-paint look.

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
  colours of a tomb wall as you learn, and fades back to ink as memory fades. Amulets mark
  abilities (telling the four h's apart, relearning what you forgot, reading the offering formula),
  each with its own piece of Egyptian lore.

## Modules

| module | what |
|---|---|
| `core-script` | Gardiner sign table (generated from the font), Manuel de Codage parser, quadrat layout, transliteration |
| `core-model` | items, lessons, course, memory, progress, awards — pure Kotlin, serialisable |
| `core-engine` | FSRS scheduler, exercise factory, answer checker, session planner, recording, awards |
| `content` | the Middle Egyptian course: 8 units, 43 lessons, ~100 signs, ~95 words, phrases, texts |
| `data` | `ProgressStore`: one JSON document in DataStore |
| `core-ui` | papyrus, palette, hieroglyph renderer, painted buttons, audio, sound effects |
| `feature-path` / `feature-lesson` / `feature-codex` | the screens |
| `app` | activity and navigation |

Everything below `data` is plain JVM and tested without a device: `gradlew :core-script:test
:core-engine:test :content:test`. The content tests walk the whole course with a perfect learner
(every exercise must be answerable) and a careless one (nothing may get stuck).

## Tools

- `tools/gen_signs.py` — regenerates `signs.tsv` (Gardiner code → codepoint → ink box) from the
  bundled font, via Python's Unicode database.
- `tools/gen_sfx.py` — synthesises the harp sound effects (Karplus–Strong, pentatonic).
- `tools/voice.py` — generates pronunciation clips with ElevenLabs (Eleven v4, voice "George"),
  content-addressed so nothing is billed twice, then transcribes each clip back with Scribe as a
  check. The key is read from `C:\Users\igor\dev\11labs\.env` by path and never stored here.

## Licences

Noto Sans Egyptian Hieroglyphs and Noto Serif: © Google, SIL Open Font License 1.1 (the licence
is embedded in each font file's name table). Course content written for this project.
