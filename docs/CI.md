# CI and release APKs

`Android checks` runs architecture checks, Spotless, focused Detekt analysis, the
debug build, JVM unit tests, and Android Lint on pull requests, pushes to main,
and manual dispatch. Emulator tests are not part of CI; connected tests remain
available locally with `bash gradlew :app:connectedDebugAndroidTest`.

`Release APK` runs automatically on pushes to main, including PR merges, and via
Actions → Release APK → Run workflow on main. It checks release unit tests and
lint, builds the release APK, and uploads it with SHA-256 checksums and available
R8 mappings. It does not publish a GitHub Release or upload to a store. Workflow
dispatch becomes available after the workflow reaches the default branch.

## Signing and downloads

Without signing secrets, release builds still succeed and the artifact is named
`release-apk-unsigned-<commit>`. That APK is not directly installable. Use the debug
APK from Android checks for installation, or configure the real release identity.

Create a GitHub environment named `release`, restrict its deployment branches to
main, and add these secrets through GitHub's UI:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of the existing release keystore.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

When all four secrets exist, the same workflow builds and verifies an installable
signed APK, named `release-apk-signed-<commit>`. Partial signing configuration fails
explicitly. Use the original signing identity to update an existing installation.
The workflow does not generate or rotate keys. Update versionCode and versionName
in `app/build.gradle.kts` before distributing a new app version.

Download the artifact ZIP from the workflow run and extract its APK. Artifacts and
reports expire after 14 days; retain signed packages and mappings before distributing
them. The signing key is written to runner temp with restrictive permissions and
removed even on failure. Signed builds disable the configuration cache and the
release job cannot write the Gradle cache.

## Local checks

```sh
python3 scripts/check_architecture.py
bash gradlew spotlessCheck detekt
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
bash gradlew :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
```

AGP 9 defaults to debug-only unit-test components. The project explicitly sets
`android.onlyEnableUnitTestForTheTestedBuildType=false` to retain release tests
([Android documentation](https://developer.android.com/build/releases/agp-9-0-0-release-notes)).

Use `bash gradlew spotlessApply` to format changed Kotlin source files. Spotless
ratchets from commit `4b699abb1a2271335578a3c736691d6c3b50c073`: untouched existing
files are grandfathered; touched and new Kotlin source files must satisfy ktlint.
CI fetches full history and explicitly fetches that baseline if a squash merge makes it absent locally. Build-script formatting
is not currently enforced. Detekt runs over all Kotlin sources with explicit syntax
rules in `config/detekt/detekt.yml`; default style/complexity rules remain disabled
while legacy code is migrated. Android Lint handles Android/API/type correctness.

## Workflow safeguards and repository settings

Actions use immutable commit pins, minimum token permissions, job timeouts, and
14-day artifact retention. Debug checks cancel superseded runs. Gradle validates
the wrapper JAR and its distribution has a SHA-256 pin. External fork PRs cannot
write Gradle caches or receive signing secrets. Dependabot proposes weekly Gradle
and Actions updates after its configuration reaches the default branch.

After checks pass, a repository administrator can require the verify job, PR review,
and resolved conversations, and block force pushes to main through a branch ruleset.
Workflow YAML cannot enforce those settings, and the connected GitHub app lacks
administration access. Signing secrets and environment branch restrictions also
need repository settings; no repository settings were changed by this PR.
