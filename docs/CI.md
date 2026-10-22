# CI and release APKs

## Build repeatability

Windows/Antigravity setup and secret entry steps are in [instructions.md](../instructions.md).
CI uses Ubuntu 24.04, Temurin 21.0.8+9 (setup-java identifier `21.0.8+9.0.LTS`), the checksum-pinned Gradle 9.6.0 wrapper,
SDK platform 36, and explicitly selected Build-Tools 36.0.0. The daemon criteria
now select Temurin 21 instead of the inherited Java 25 download URLs. Install
the documented JDK locally; there are no daemon auto-download URLs in this file.

`python3 scripts/verify_reproducible.py` performs two clean unsigned release builds,
with task reruns and build/configuration caches disabled, then compares APK SHA-256
hashes. Signing credentials and known GitHub tokens are removed from subprocess
environments. CI uploads both APKs and `result.json` as `reproducibility-<commit>`;
a mismatch fails the job. Both outputs intentionally use the same checkout path.
This catches nondeterministic outputs in one toolchain; it does not prove byte
identity across hosts, operating systems, or future SDK repository updates.

Direct dependencies and plugins are version-pinned, but transitive dependency
locks and artifact checksum verification metadata are not yet committed. The
hosted runner image and SDK platform revision are not immutable. A fully hermetic,
independently verified release remains future work. Gradle 9's reproducible archive
defaults alone do not prove Android APK reproducibility.

Local Windows entry points are `scripts/build.ps1 debug`, `release`, `check`, and
`reproduce`. `.env` is parsed as data by that helper, not executed as a script.
`FATHER_ACCOUNT_GH` is ignored by the build helper and must be removed from any
shared copy. Neither `.env` nor `private-signing/` belongs in Git or CI artifacts.

Validation of this change: architecture guard (Python UTF-8 mode), PowerShell
parser, Python syntax, script behavior tests, secret-ignore checks, and
`git diff --check` are recorded locally. Actual Android build/lint/Gradle analysis
and the two-build APK comparison require the documented JDK/SDK; do not treat a
configured workflow as proof it passed. No app UI or runtime permissions changed.
On October 22, 2026, the three verifier behavior tests passed. The Gradle check
command was attempted but blocked downloading Gradle (`UnknownHostException:
services.gradle.org`); the local PC also has only JDK 17 and no configured SDK.
Consequently Spotless, Detekt, both APK builds, both unit-test variants, both lint
variants, and actual APK byte reproducibility remain unverified for these changes.

The follow-up [online run](https://github.com/Sanjay-P69/FirstLogApp/actions/runs/39816848480)
at PR head `a44a32b125ab5d3419b4293bd08e603c79d82814` passed both APK builds,
both unit-test and lint variants, Spotless, Detekt, architecture/verifier checks,
and two matching clean unsigned release builds. GitHub tested merge commit
`3ae12508e01ec4ac6428d9235669fd7db7dcf185`. The first attempted run failed Java
setup because `21.0.8+9` did not match Temurin's full `21.0.8+9.0.LTS` identifier;
both workflows now use the verified identifier. Subsequent Tracewise branding
changes require their own run. CI also compiles the instrumentation APK without
running an emulator; installed-device update and identity tests remain device checks.

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
checks Kotlin files whose contents differ from the reviewed tree at commit `4b699abb1a2271335578a3c736691d6c3b50c073`: untouched existing
files are grandfathered; touched and new Kotlin source files must satisfy ktlint.
CI fetches full history and explicitly fetches that baseline if a squash merge makes it absent locally. Direct tree comparison avoids merge-base changes after squash merges; untracked local Kotlin sources are included too. Build-script formatting
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
