# FirstLog

Android device-activity tracking and daily summaries, written in Kotlin and Jetpack Compose.

This branch introduces a Material 3 Expressive redesign of the main destinations and separates their rendering from Android tracking and persistence. The existing application ID and storage format are retained. This remains a draft for visual/device review and the documented legacy migrations. Debug APK assembly, unit tests, lint, and the architecture guard passed in [CI](https://github.com/Devasy/FirstLogApp/actions/runs/39770770870).

- [Engineering review and prioritized findings](docs/ENGINEERING_REVIEW.md)
- [Architecture and migration boundaries](docs/ARCHITECTURE.md)
- [UX, design rules, and open-source references](docs/DESIGN.md)
- [Contributor and agent standards](AGENTS.md)
- [CI checks, artifacts, and signed release setup](docs/CI.md)

## Build and check

Use JDK 21, Android SDK platform 36, and the checked-in Gradle wrapper. Keep the project versions in `gradle/libs.versions.toml`; Material 3 is explicitly pinned to `1.5.0-alpha13` for Expressive APIs.

```sh
python3 scripts/check_architecture.py
bash gradlew spotlessCheck detekt
bash gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease \
  :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:lintRelease
```

With a compatible emulator or device:

```sh
bash gradlew :app:connectedDebugAndroidTest
```

Open `DashboardPreviews.kt` in Android Studio for light, dark, compact, tablet, and larger-font previews. Rendered screenshots are not included. A debug APK and reports are available in the passing CI run’s artifacts. The expanded GitHub workflow checks formatting, static analysis, debug/release builds, unit tests, and lint, plus connected UI tests on API 29 and 35 emulators. Normal CI release artifacts are unsigned. Manual signed packaging requires the environment secrets documented in [CI setup](docs/CI.md).

## Boundaries

`MainActivity` wires the application. `LoggingService` monitors device state. `data/` owns platform reads and the legacy preference adapter. A dashboard repository and ViewModel expose immutable state to rendering-only Content composables. Routes own Android launchers and legacy detail adapters. Some detailed notification/history/report screens still require migration; read the architecture document before extending them.

## Data limitations

Scheduled backups are local files. The system export picker can save to a cloud provider chosen by the user; choosing a Google account is not cloud synchronization. Per-app battery drain cannot be inferred accurately from screen-time shares. Modern Android background limits mean tracking may be interrupted. The engineering review records unresolved storage, backup, credential, and service-lifecycle risks.
