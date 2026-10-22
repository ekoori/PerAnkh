# Google Play listing

Everything the Play Console asks for, ready to paste. Graphics are in this folder.

## App details

| Field | Value |
|---|---|
| App name (≤30) | Per Ankh: Read Hieroglyphs |
| Package | `app.hrana.perankh` |
| Default language | English (Australia) or English (United States) |
| App or game | App |
| Free or paid | Free |
| Category | Education |
| Tags | Education, Language learning, History |
| Contact email | *(LOVI Tech's support address; required, shown publicly)* |
| Website | https://github.com/ekoori/PerAnkh |
| Privacy policy | https://perankh.hrana.app/privacy |

**Short description (≤80):**
Learn to read Middle Egyptian hieroglyphs: signs, sounds, words and real texts.

**Full description (≤4000):**

> Per Ankh teaches you to read the hieroglyphs of ancient Egypt, the way Egyptologists learn
> them: sign by sign, then words, grammar and real inscriptions.
>
> • 14 units and 75 lessons, from the 25 alphabet signs to royal cartouches, the offering formula,
> Ptahhotep’s maxims on listening and the giant serpent of the Shipwrecked Sailor.
> • Every sign, word and phrase is spoken in the Egyptological pronunciation, with the sounds
> English lacks kept distinct: ḥ, ʿayin, kh and q.
> • Memory, not streaks: each item comes back when you are about to forget it, never on a rota
> and never when you obviously still know it.
> • Mistakes are explained, near misses are recognised, and "sound it out" exercises give you
> words you were never taught.
> • See how much of a real page you can read: the words you know are counted against thousands
> of sentences of real Middle Egyptian.
> • Amulets for real abilities, each with a piece of Egyptian lore.
>
> No ads, no account, no tracking. Works offline. Open source under the MIT licence.
> The name, pr-ꜥnḫ, means "House of Life": the temple school where scribes were trained.

## Data safety form

Answers for version 0.2.0. Update them before any version that adds accounts or sync.

- Does your app collect or share any of the required user data types? **Yes** (problem reports
  only).
- Is all user data collected by your app encrypted in transit? **Yes**.
- Do you provide a way for users to request that their data be deleted? **Yes** (privacy policy:
  contact us; reports are anonymous).
- Data types collected (both **optional**: sent only when the user taps Send in the report form;
  **not shared**; not processed ephemerally; purpose **Analytics**, which is where Play files
  information used to find and fix problems):
  - **App activity → Other user-generated content**: the tester's note.
  - **App info and performance → Diagnostics**: what was on screen, the app version and the
    Android version.
- Not collected: location, personal info (name, email, ids), financial info, health, messages,
  photos or videos, audio, files, calendar, contacts, web browsing, device or other ids.
- Shared with third parties: **none**.

## Content rating (IARC questionnaire)

Category: **Reference, News or Educational**. Violence, sexuality, language, controlled
substances, gambling: **No** to all. User interaction: users cannot interact or share content with
each other; no location sharing; no purchases. Expected rating: Everyone / PEGI 3.

## Target audience and content

- Target age groups: **13–15, 16–17, 18 and over**. Not under 13, so the app stays outside the
  Designed for Families programme (it would qualify, but that adds review obligations without
  changing anything for learners).
- Appeals to children: **No** (a general-audience learning app).
- Ads: **No**. In-app purchases: **No**.
- News app: No. Government app: No. Financial features: None. Health: No.
- Data deletion URL: not needed (no accounts).

## Graphics

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | `icon-512.png` |
| Feature graphic | 1024×500 PNG or JPG | `feature-graphic.png` |
| Phone screenshots | 2–8, 16:9 or 9:16, 320–3840 px | `screenshots/*.png` (1052×2010) |

## Policy notes

- Target API: 36, as required for new apps and updates since 31 August 2026.
- Permissions: INTERNET only, for sending problem reports the user chooses to send and for the
  About page's check for a newer version.
- No external payment links in the Play build (see RELEASING.md, "Donations").
- No updates from outside Play: the About page links to new releases on GitHub, which a Play
  build must not do (Device and Network Abuse policy). The `play` flavour hides that check or
  links to the Play listing instead (DEVELOPING.md, "The newest release").
- App access for review: everything is reachable without an account. The report flag is on every
  lesson page; tester tools are under Settings → Testing.
