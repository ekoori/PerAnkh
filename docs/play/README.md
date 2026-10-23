# Google Play listing

Everything the Play Console asks for, ready to paste. Graphics are in this folder.

## First upload, step by step

Per Ankh is published from LOVI Tech's organisation account, created in October 2026. An
organisation account does not need the 12 testers for 14 days that new personal accounts need
before production.

1. **Finish the account.** Play Console → Settings → Developer account: verify the contact and
   developer email and phone with their codes, and complete identity and organisation
   verification (D-U-N-S number, organisation website). Play shows an organisation's legal name,
   address, developer email and developer phone on the store, so give a business phone number.
2. **Create the app.** Home → *Create app*: the name below, default language, *App*, *Free*. If it
   asks for the package name, it is `app.hrana.perankh`; creating the app registers that name to
   LOVI Tech for Android developer verification.
3. **Signing, before the first upload.** New apps get a Google-generated key unless this is
   changed first. Open Play app signing (under *Test and release* → *App integrity*, or *Protected
   with Play* → *Play Store distribution*), choose *Change the app signing key* → *Provide a copy
   of your app signing key*, download the PEPK tool and encryption key that page offers, and run
   the command it shows with the release keystore (`~/.android/per-ankh-release.jks`, the path
   written out in full) and `--alias=per-ankh`; it asks for the keystore password. Upload the zip
   it writes, then delete the zip. A separate upload key is optional; without one, the release key also signs uploads, which
   is what the build does. The page must then show the app signing key certificate SHA-256
   `F7:BA:04:DE:0F:40:EE:4A:C6:23:E0:C6:1E:CE:A7:CC:CB:F8:77:04:48:17:CB:13:43:94:45:AA:E9:D9:57:73`,
   the same as the GitHub APKs. Never use *Upgrade key* (RELEASING.md, "Before the Play launch").
4. **Store listing.** *Grow users* → *Store presence* → *Main store listing* and *Store settings*:
   texts from "App details", graphics from "Graphics".
5. **App content.** *Policy and programs* → *App content*: privacy policy, app access (everything
   reachable without an account), ads (none), content rating, target audience, data safety,
   advertising ID (not used), government, financial, health and news declarations, all from the
   sections below.
6. **Internal testing.** *Test and release* → *Testing* → *Internal testing*: add a testers list
   with your own Google account, then *Create new release* and upload
   `app/build/outputs/bundle/playRelease/app-play-release.aab` (`gradlew :app:bundlePlayRelease`),
   release name `0.3.1`, notes from CHANGELOG.md. Roll it out, open the opt-in link on the test
   phone and accept. Play must offer **Update** over the GitHub 0.3.0 already there: that proves
   the signature matches, and progress stays. Settings → About then says "Google Play keeps Per
   Ankh up to date".
7. **Production.** When the plan in RELEASING.md says so: a production release from the same
   bundle (or promoted from testing), countries, then *Publishing overview* → *Send changes for
   review*. The first review of a new account's app can take several days.
8. **After review.** On the Play Console home page, check that `app.hrana.perankh` shows as
   registered for Android developer verification, with the certificate above. That is what keeps
   the GitHub APK installing where Android requires verified developers (Brazil, Indonesia,
   Singapore and Thailand since 30 September 2026; everywhere in 2027).

Console menu names move between Play Console versions; the help pages name the current ones.

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
> • Mistakes are explained, near misses are recognised, and “sound it out” exercises give you
> words you were never taught.
> • See how much of a real page you can read: the words you know are counted against thousands
> of sentences of real Middle Egyptian.
> • Amulets for real abilities, each with a piece of Egyptian lore.
>
> No ads, no account, no tracking. Works offline. Open source under the MIT licence.
>
> The name, pr-ꜥnḫ, means “House of Life”: the temple school where scribes were trained.

## Data safety form

Answers for the Play build from 0.3.1 (the GitHub build's check for a newer version is not in it).
Update them before any version that adds accounts or sync.

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
| Feature graphic | 1024×500 PNG (no alpha) or JPG | `feature-graphic.png` |
| Phone screenshots | 2–8, PNG (no alpha) or JPG, 320–3840 px; for Play's recommendations at least 4 at 9:16 and 1080×1920 or more | `screenshots/*.png` (1080×1920) |

## Policy notes

- Target API: 36, as required for new apps and updates since 31 August 2026.
- Permissions: INTERNET only, for sending problem reports the user chooses to send and for the
  About page's check for a newer version.
- No external payment links in the Play build (see RELEASING.md, "Donations").
- No updates from outside Play: in the GitHub build the About page links to new releases on
  GitHub, which a Play build must not do (Device and Network Abuse policy). The `play` flavour
  never looks for them and links to the Play listing instead (DEVELOPING.md, "The newest
  release").
- App access for review: everything is reachable without an account. The report flag is on every
  lesson page; tester tools are under Settings → Testing.
