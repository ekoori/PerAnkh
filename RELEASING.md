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
| Debug on the test phone | Igor | `gradlew :app:installGithubDebug`, tester mode on | Every change |
| Play **internal testing** | Up to 100 named testers | AAB, `gradlew :app:bundlePlayRelease` | Each beta, minutes after upload |
| Play **closed testing** | A tester group or list | Same AAB, promoted | Before each public release |
| GitHub **pre-release** | Anyone, labelled not ready | Signed APK | Beta tags |
| GitHub **release** | Everyone | Signed APK, `gradlew :app:assembleGithubRelease` | From 0.2.0 |
| Play **production** (staged 10% → 50% → 100%) | Everyone | AAB | From 1.0.0 |
| F-Droid | Everyone, from source | Built by F-Droid | After 1.0, if wanted |

Play gets the `play` flavour and GitHub the `github` one (DEVELOPING.md). They share the app id and
the release key (Play's copy through Play App Signing), so a Play install and a GitHub install can
update each other and keep progress.

## Plan

- **0.2.0, the first public GitHub release (2026-10-22).** New app id `app.hrana.perankh`, the
  project's release key, Android 16 target (API 36, which Play requires since 31 August 2026),
  report upload to perankh.hrana.app, privacy policy, MIT licence, units 10–12. Play internal
  testing to follow. v0.1.0 stays a GitHub pre-release; its 17 installs need one reinstall
  (different key and id), which deletes their progress.
- **0.3.0, GitHub release (2026-10-22).** Sections, units 13–14, the fixes from the first problem
  reports, and Settings → About, which from now on tells people when a newer release is out.
- **0.3.2, the first Google Play build (2026-10-24).** The `play` flavour, for open testing under
  LOVI Tech's Play developer account (created October 2026). Its `github` build behaves exactly
  as 0.3.0 does, so GitHub stayed on 0.3.0 and v0.3.2 is a tag without a GitHub release. There is
  no 0.3.1: its version code went to a Play upload that was deleted, and Play never takes a code
  twice.
- **0.4.0, GitHub release and Play open testing (2026-11-03).** Sections 3–7 (units 15–42: the
  Abydos stelae, the Book of the Dead, the whole Shipwrecked Sailor, Ptahhotep's epilogue, Papyrus
  Westcar), tablets, and the fixes from testers' reports. The course content becomes CC BY-SA 4.0
  (its texts come from AES), and reports name their app now that BushLines shares the server.
- **0.4.x, open testing.** Anyone can join the test from the Play listing. Fix what testers
  report; re-record the weak clips. A couple of weeks of testing before production (new
  *personal* Play accounts would need 12 testers for 14 days in closed testing; LOVI Tech's
  organisation account does not).
- **1.0.0, public.** Play production (staged rollout) and a GitHub release with the same signed
  APK.
- **1.x.** Donations; later, optional accounts and progress sync (`server/README.md`), each with
  a privacy policy update first.

## Before the Play launch

1. **Play developer account for LOVI Tech.** Created in October 2026, as an organisation
   account: it needs LOVI Tech's D-U-N-S number, and must finish verification before it can
   publish.
2. **Android developer verification.** Since 30 September 2026, Android in Brazil, Indonesia,
   Singapore and Thailand installs apps only from verified developers, even outside Play, and this
   goes worldwide in 2027. Creating the app in Play Console registers `app.hrana.perankh` to LOVI
   Tech's account, and Play App Signing with the release key (3.) proves the key. Then check on the
   Play Console home page that the package is registered with the release key's certificate, which
   is what keeps GitHub APKs installing for those users; Play Console also registers apps
   distributed outside Play.
3. **Signing.** Decided: the project's own release key, the same signature everywhere. New apps
   get a Google-generated key by default, so before the first upload open Play app signing ("Test
   and release" or "Protected with Play", depending on the Console's layout), choose *Change the
   app signing key* and *Provide a copy of your app signing key*, and follow the PEPK steps shown
   there with `~/.android/per-ankh-release.jks`, alias `per-ankh`. The bundle is signed with the
   same key, which then also serves as the upload key. Never use *Upgrade key*: Android 13 and
   later would then refuse GitHub APKs as updates of Play installs.
   The release key's certificate reads `CN=Igor Krbavcic`. It was kept for the first public
   release, 0.2.0, so it is now fixed: every later release, on GitHub and, through Play App
   Signing, on Play, must be signed with it, or installs cannot update.
4. **Store listing** from `docs/play/`: texts, Data safety answers, content rating, graphics.

## Donations

The plan is Stripe, through LOVI Tech. Google Play allows payment links outside Play Billing only
for validated tax-exempt charities; a donation to a company must go through Play Billing. In 2025
Google made AnkiDroid remove its donation links from its Play build for exactly this. So:

- **GitHub / F-Droid build**: a "Support Per Ankh" link to a Stripe Payment Link. Fine.
- **Play build**: no external donation link. Either nothing, or "tips" as Play Billing in-app
  products (Google keeps 15%).

The two product flavours exist since 0.3.2, for the About page's check for a newer version: Play
does not allow an app to offer updates from elsewhere, so the `play` flavour links to the Play
listing instead. The donation link goes into the `github` flavour only.

## Release checklist

1. `gradlew :core-script:test :core-engine:test :content:test :data:testDebugUnitTest` and `cd server && python -m unittest discover -s tests` pass.
2. Set `appVersion`; add the CHANGELOG entry.
3. Build: `gradlew :app:bundlePlayRelease :app:assembleGithubRelease`. The bundle is
   `app/build/outputs/bundle/playRelease/app-play-release.aab`, the APK
   `app/build/outputs/apk/github/release/app-github-release.apk`.
4. Check them: `apksigner verify --print-certs` on the APK and `keytool -printcert -jarfile` on the
   bundle show the release certificate, never `CN=Android Debug`; `aapt2 dump badging` shows
   `targetSdkVersion:'36'` and the version.
5. Install on the test phone over the previous release (progress must survive); play a lesson,
   send a test report, then resolve it with `tools/reports.sh`.
6. Upload the AAB to the Play track (Play never accepts a version code twice, even once its upload
   is deleted: replacing a bundle takes a new patch version); tag `vX.Y.Z`; create the GitHub
   release with the APK and its SHA-256 (a full release; only `-beta.N` tags are pre-releases).
7. Resolve the reports fixed in this version: `tools/reports.sh resolve ID --in X.Y.Z` (find them
   with `tools/reports.sh list --app perankh`; BushLines reports to the same server).
8. On a phone with the previous release, Settings → About offers the new version, and within half
   an hour so does `https://perankh.hrana.app/v1/latest`, the copy for phones that cannot reach
   GitHub.
