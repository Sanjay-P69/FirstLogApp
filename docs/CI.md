# Build, checks, and release packaging

`Android checks` runs on pull requests, pushes to main, and manual dispatches.
It builds the Kotlin Android debug APK plus unsigned release APK/AAB, runs both
variants' JVM tests and Android Lint, and checks architecture, formatting, and
focused Detekt rules. Independent emulator jobs run the Compose instrumentation
tests on Android API 29 and 35. They do not prove that continuous device tracking,
permission flows, or every legacy dialog works; physical-device testing remains necessary.

Artifacts expire after 14 days. The debug APK is installable. CI's normal release
APK is unsigned and cannot be installed directly; the AAB is for distribution tooling.
Reports are uploaded even when checks fail. Jobs have timeouts, minimum token
permissions, immutable action commit pins, and cancellation of superseded checks.
Gradle's setup action validates the wrapper JAR; the distribution also has a SHA-256
pin. Fork PRs cannot write Gradle caches and receive no signing secrets.
Dependabot proposes weekly Gradle and Actions updates; updates still require review.

AGP 9 defaults to creating unit tests only for the tested build type. We explicitly
set `android.onlyEnableUnitTestForTheTestedBuildType=false` to retain release tests
([Android documentation](https://developer.android.com/build/releases/agp-9-0-0-release-notes)).

## Local commands

```sh
python3 scripts/check_architecture.py
bash gradlew spotlessCheck detekt
bash gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease \
  :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease
bash gradlew :app:connectedDebugAndroidTest
```

Use `bash gradlew spotlessApply` to format changed Kotlin source files. Spotless
ratchets from commit `4b699abb1a2271335578a3c736691d6c3b50c073`: existing untouched
files are grandfathered; any touched or new source file must satisfy ktlint.
Keep that baseline commit in clone history (`fetch-depth: 0` in CI). Formatting
build scripts is not currently enforced. Detekt runs over all Kotlin sources with
explicit correctness rules in `config/detekt/detekt.yml`; its default style and
complexity rules are not enabled. Broaden these as legacy code is migrated.

## Signed packages (manual, main only)

After merging the workflows, create a GitHub environment named `release`. Configure
required reviewers and restrict deployment branches to main in repository settings.
Add these environment secrets through GitHub's UI; never commit or paste keys:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of the existing release keystore.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Use the original signing identity for updates to an existing installation. This
workflow does not generate, rotate, or upload a signing key. Set a new versionCode
and versionName in `app/build.gradle.kts` before distributing a new app version.

Dispatch `Signed release packaging` on main. It checks release sources, builds a
signed APK and AAB, verifies the APK signature, and uploads them with SHA-256 checksums
for seven days. The key is written with restrictive permissions to runner temp and
removed even on failure. Signed tasks disable the configuration cache so signing
credentials are not serialized into a reusable cache. No store upload or GitHub
Release publishing occurs. Signing cannot be validated until real secrets exist.

## Repository settings

After the new jobs pass, configure a branch ruleset requiring the verify job and
both device-test jobs, requiring PR review and resolved conversations, and blocking
force pushes to main. Workflow YAML cannot enforce repository branch protection.
The connected GitHub app lacks administration access, so these settings must be
applied by a repository administrator. Likewise, Dependabot activation depends on
this configuration reaching the default branch.
