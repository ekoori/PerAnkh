# Versions and releases

## Version numbers

Semantic versions, `MAJOR.MINOR.PATCH`, set in one place: `appVersion` in
`app/build.gradle.kts`. The Android version code is derived from it, `MAJOR*10000 + MINOR*100 +
PATCH` (0.2.0 is 200, 1.0.0 is 10000), so it always rises and never needs editing by hand.

- **PATCH**: fixes and content corrections (a wrong spelling, a re-recorded clip).
- **MINOR**: new lessons, exercises or features, compatible with existing progress.
- **MAJOR**: 1.0 marks the public launch. After that, a major version only for a change that
  needs migrating saved progress.

Each release is a tag `vX.Y.Z` on `main`, with an entry in [CHANGELOG.md](CHANGELOG.md).
Pre-release builds are `vX.Y.Z-beta.N` tags whose GitHub releases are marked "pre-release".

## Where builds go

| Channel | Who | Build | When |
|---|---|---|---|
| Debug on the test phone | Igor | `gradlew :app:installDebug`, tester mode on | Every change |
| Play **internal testing** | Up to 100 named testers | AAB, `gradlew :app:bundleRelease` | Each beta, minutes after upload |
| Play **closed testing** | A tester group or list | Same AAB, promoted | Before each public release |
| GitHub **pre-release** | Anyone, labelled not ready | Signed APK | Optional, for testers off Play |
| Play **production** (staged 10% → 50% → 100%) and GitHub **release** | Everyone | AAB and APK | From 1.0.0 |
| F-Droid | Everyone, from source | Built by F-Droid | After 1.0, if wanted |

## Plan

- **0.2.0, testers only.** New app id `app.hrana.perankh`, the project's release key, Android 16
  target (API 36, which Play requires since 31 August 2026), report upload to perankh.hrana.app,
  privacy policy, MIT licence. Goes to Play internal testing and the test phone. v0.1.0 stays a
  GitHub pre-release; its 17 installs need one reinstall (different key and id), which deletes
  their progress.
- **0.3.x, closed testing.** Fix what testers report; re-record the three weak clips; finish the
  store listing. Two weeks in closed testing before production (a must for new *personal* Play
  accounts, which need 12 testers for 14 days; good practice for an organisation account too).
- **1.0.0, public.** Play production (staged rollout) and a GitHub release with the same signed
  APK.
- **1.x.** Donations; later, optional accounts and progress sync (`server/README.md`), each with
  a privacy policy update first.

## Before the first public release

1. **Play developer account for LOVI Tech.** An organisation account needs a D-U-N-S number for
   LOVI Tech, and verification takes days to weeks; start early.
2. **Android developer verification.** Since 30 September 2026, Android in Brazil, Indonesia,
   Singapore and Thailand installs apps only from verified developers, even outside Play, and this
   goes worldwide in 2027. Register LOVI Tech in the Android Developer Console with the package
   name `app.hrana.perankh` and the release key's certificate, or GitHub APKs will stop installing
   for those users.
3. **Signing.** Decide once, before the first Play upload:
   - Let Play generate the app signing key (simplest), and Play installs and GitHub installs carry
     different signatures: a user cannot move between them without reinstalling.
   - Or upload the project's own release key to Play App Signing ("export and upload a key from a
     Java keystore"), and keep a separate upload key: the same signature everywhere. Recommended,
     since GitHub (and maybe F-Droid) builds are planned.
   The current release key's certificate reads `CN=Igor Krbavcic`. Every APK shows it. Since LOVI
   Tech publishes, consider replacing it before anything is signed for the public with one reading
   `CN=Per Ankh, O=LOVI Tech, C=AU` (README, "Release signing", shows the keytool command).
4. **Store listing** from `docs/play/`: texts, Data safety answers, content rating, graphics.

## Donations

The plan is Stripe, through LOVI Tech. Google Play allows payment links outside Play Billing only
for validated tax-exempt charities; a donation to a company must go through Play Billing. In 2025
Google made AnkiDroid remove its donation links from its Play build for exactly this. So:

- **GitHub / F-Droid build**: a "Support Per Ankh" link to a Stripe Payment Link. Fine.
- **Play build**: no external donation link. Either nothing, or "tips" as Play Billing in-app
  products (Google keeps 15%).

That needs two product flavors (`play`, `github`), differing only there. Add them with the
donation feature, not before.

## Release checklist

1. `gradlew :core-script:test :core-engine:test :content:test` and `cd server && python -m unittest discover -s tests` pass.
2. Set `appVersion`; add the CHANGELOG entry.
3. Build: `gradlew :app:bundleRelease :app:assembleRelease`.
4. Check the APK: `apksigner verify --print-certs` shows the release certificate, never
   `CN=Android Debug`; `aapt2 dump badging` shows `targetSdkVersion:'36'` and the version.
5. Install on the test phone over the previous release (progress must survive); play a lesson,
   send a test report, then resolve it with `tools/reports.sh`.
6. Upload the AAB to the Play track; tag `vX.Y.Z`; create the GitHub release with the APK and
   its SHA-256 (pre-release until 1.0).
7. Resolve the reports fixed in this version: `tools/reports.sh resolve ID --in X.Y.Z`.
