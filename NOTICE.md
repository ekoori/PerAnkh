# Third-party material in Per Ankh

The code written for this project is under the MIT licence in [LICENSE](LICENSE). The course
content is shared under CC BY-SA 4.0, as the corpora its texts come from require (below). These
parts come from elsewhere and keep their own terms:

| What | Where | Terms |
|---|---|---|
| The course's texts and spellings | `content/src/main/kotlin/com/krbavcic/pentalingo/content/`: the words, phrases, readings and grammar cards (`Words.kt`, `Phrases.kt`, `TeachingPhrases.kt`, `WestcarPhrases.kt`, `MiddleEgyptian.kt`, `Concepts.kt`) | The readings given in full (the Abydos stelae, the Book of the Dead spells, the Shipwrecked Sailor, Ptahhotep's epilogue and the tale from Papyrus Westcar) and most words' spellings follow [Ancient Egyptian Sentences](https://github.com/simondschweitzer/aes) (AES) by Simon D. Schweitzer, made from the Thesaurus Linguae Aegyptiae's database: hieroglyphs as AES gives them, with scribal variants mapped to signs the course teaches, and translations made from its translations. [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). These files are shared under CC BY-SA 4.0, not MIT. |
| Noto Sans Egyptian Hieroglyphs, Noto Serif | `core-ui/src/main/res/font/` | © Google LLC, [SIL Open Font License 1.1](https://openfontlicense.org). The licence is embedded in each font file's name table. |
| Word frequencies | `content/src/main/kotlin/com/krbavcic/pentalingo/content/Frequency.kt` | Derived from the [Thesaurus Linguae Aegyptiae](https://thesaurus-linguae-aegyptiae.de) (Berlin-Brandenburg Academy of Sciences and Humanities, Saxon Academy of Sciences in Leipzig), Earlier Egyptian corpus v18, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). This file is shared under CC BY-SA 4.0, not MIT. |
| Pronunciation clips | `app/src/main/assets/audio/` | Generated with ElevenLabs text-to-speech (voice "Chaouki", ElevenLabs voice library) on a paid plan, which permits commercial use. LOVI Tech offers the clips under the MIT licence with the rest of the app, as far as its rights in them go; anyone reusing them elsewhere should also check [ElevenLabs' terms](https://elevenlabs.io/terms). |
| Libraries | Gradle dependencies (Kotlin, kotlinx, AndroidX, Jetpack Compose) | Apache License 2.0. Not vendored; fetched at build time. |

Sign codes follow Gardiner's sign list and spellings use the Manuel de Codage conventions;
transliteration follows Allen's *Middle Egyptian*. These are scholarly conventions, cited, not
copied text.
