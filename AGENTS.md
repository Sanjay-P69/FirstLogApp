# FirstLog contributor and agent guidelines

Read `docs/ENGINEERING_REVIEW.md`, `docs/ARCHITECTURE.md`, and `docs/DESIGN.md` before changing architecture or UI. User instructions take precedence. Continue routine reversible implementation and verification without asking for repeated approval.

For UI work, also read `docs/UI_REFERENCE_REVIEW.md`. It records the user's ADNS and Essentials references, inspected source revisions/screenshots, and design work that remains unfinished. Do not claim those patterns are implemented merely because they are documented.

## Dependency direction

- `MainActivity` wires dependencies, starts the Android service, and hosts the app. Never add receivers, telemetry updates, content-provider queries, or polling loops there.
- `data/` and Android services must not import Compose or `ui/`. Domain/storage records contain measurements, not colors, text sizes, icons, or composable state.
- Repositories own platform queries and persistence. Inject an interface into ViewModels; use the application context in long-lived objects.
- ViewModels expose immutable `StateFlow<UiState>` and accept user actions. Collect with `collectAsStateWithLifecycle`. Use `SharingStarted.WhileSubscribed` for screen polling; replace polling with observable storage when that migration is implemented.
- `*Content.kt` files only render supplied state and emit actions. No `DataManager`, `Context`, services, launchers, repositories, ViewModels, effects, file operations, or permission checks. Local expansion/focus state is fine.
- `*Route` / integration files own Android launchers and translate actions to navigation or repository calls. Existing notification, metric-history, and summary dialogs are transitional adapters, not examples to copy.
- Keep this app in one Gradle module until real build-time or ownership needs justify more modules. Do not add Hilt, a use-case class for every getter, or generic base ViewModels as ceremony.

## Kotlin and state

- Use four spaces, explicit names, small functions, and explicit imports for new code. Group files by feature and responsibility. Prefer one primary class or screen per file; review files above roughly 300 lines for a sensible split.
- Give screen entry points a single state parameter plus callbacks/actions and a `Modifier`. Avoid parallel lists, dozens of string parameters, and presentation objects with unrelated optional fields. `MetricItem` is a compatibility model; do not extend it for unrelated features.
- Hoist business state. Never mutate preferences or query Android services inside composition or `remember`.
- Run file/network/provider operations on an injected dispatcher or `Dispatchers.IO` at the repository/integration boundary. `LaunchedEffect` alone does not move work off the main thread.
- Preserve `CancellationException`; catch expected failures at the operation boundary and expose loading, empty, denied, stale, and error states. Do not swallow exceptions to show success or invented zero data.
- Prefer typed identifiers and sealed actions over parsing display strings. Only persist a small identifier with `rememberSaveable`; never save API keys or activity payloads in saved-instance state.
- Preserve the installed application ID, preference keys, backup schema, and service component names unless implementing a documented migration with tests. Never silently clear telemetry to fix a parsing error.

## Material 3 Expressive and accessibility

- Use `FirstLogAppTheme`, Material 3 components, `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes`. Expressive theme defaults provide motion and shape behavior; do not simulate them with decorative gradients or arbitrary bounce animations.
- No hardcoded colors or font sizes in Content composables. Do not use emoji or Unicode arrows as controls. Use Material icons, buttons, list items, chips, segmented buttons, sheets, dialogs, and navigation components.
- Use normal Material type sizes and system font scaling. Let labels and values wrap. Never force an essential value into a single truncated line to make a screenshot look tidy.
- Keep interactive targets at least 48 dp. Let native Material components provide ripple, focus, selection, keyboard, and TalkBack semantics. Decorative icons have null descriptions; icon-only actions have localized labels.
- Use adaptive layouts. Preview at 320 dp, 360 dp, tablet width, light/dark, and at least 1.5x font scale. Large numbers must not hide navigation or actions.
- Put all new user-facing copy in resources, including format strings and permission explanations. Migrate remaining English mapper strings and legacy dialogs when touching them.
- Request permissions for the specific user action. Denied access is not zero usage. Recheck after returning from system settings; never mark access granted optimistically.
- Present recorded values faithfully. Zero means no bar fill. Estimates need an explicit label and method; do not present per-app battery allocation as OS-measured drain. Account selection is not cloud authentication. Background exemptions do not guarantee 24/7 operation.
- Reuse a component when its interaction, semantics, and visual hierarchy are shared. Do not build a universal component with dozens of behavior flags. Put feature-specific layouts in the feature.
- Prefer connected rows for related secondary settings/measurements, with one primary observation per screen. Derive first/middle/last geometry from the visible items after filtering. Use native grouping defaults when supported, and keep any shared spacing/shape tokens in the UI design system.
- Give a switch row one coherent, labelled toggle interaction. A row tap and a switch tap must each dispatch exactly one change; avoid nested handlers that double-toggle. Announce checked, disabled, and expanded states, and preserve at least 48 dp targets. Destructive actions still require the existing confirmations.
- Verify reference APIs against FirstLog's pinned Material dependency and SDK. Keep opt-ins local. Names such as ExpressiveCard do not make a wrapper native Material 3. Do not copy dependency declarations, assets, blur effects, custom navigation, or large wrapper APIs merely to match a reference screenshot.
- Keep important metrics visible; use expansion only for secondary density and retain saveable state. Prefer native app-bar scroll behavior and theme motion defaults. Reserve space for any floating control so it cannot cover the last actionable row.

## Data, privacy, and background operation

- The logging service owns device monitoring. Read-only dashboard code must not add a second writer. Legacy storage getters still perform cycle rollover: treat this as documented debt until there is a single transaction owner.
- Preference read-modify-write is not a transaction. A volatile flag does not synchronize separate storage instances. Changes to rollover/session accounting need deterministic concurrency, midnight, configurable reset-time, timezone, and process-restart tests.
- Never log notification contents, contacts, call numbers, tokens, or API keys. Never add real user activity to fixtures, screenshots, or PRs.
- Do not claim the existing preference key storage is encrypted. Any credential-storage or backup-policy improvement needs a migration; exclude secrets from export and platform backup deliberately.
- Re-evaluate foreground-service types and OS restrictions before changing background behavior. A never-ending `dataSync` foreground service is not a valid assumption on recent Android versions.

## Required verification and reporting

1. Run `python3 scripts/check_architecture.py` and `git diff --check`.
2. Run `bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` when the Android SDK and dependencies are available. Do not replace lint with regex checks or claim compilation based on static checks.
3. For UI/state changes, update meaningful mapper/behavior tests and Compose previews. For runtime permissions and lifecycle changes, run applicable connected tests and a device smoke test.
4. Do not add tests merely mirroring implementation or for trivial cosmetic edits. Test user-visible invariants: denied access, actions, stale/error states, rotation, and real data boundaries.
5. Report exact checks passed, failures, and environment blockers. An unexecuted test is not a passing test. Keep uncompiled changes in a draft PR.
6. Describe the concrete problem, resulting behavior, validation, and remaining risks in the PR. Reference reviewed sources; respect their licenses and do not copy branding or assets.

## Scope of current enforcement

`scripts/check_architecture.py` enforces data/service → UI separation, pure Content files, theme tokens in Content, and a small activity. It does not prove Kotlin compilation or full clean architecture. Notification/history/report adapters and `DataManager` remain migration targets; do not add new debt to them.

## CI and releases

- Follow `docs/CI.md`. Run `spotlessCheck detekt` plus debug/release builds, both unit-test variants, and both lint variants when changing build configuration. Run connected tests for UI changes.
- Use `spotlessApply` before committing touched Kotlin sources. Do not advance the formatting baseline to hide failures.
- Pin Actions to verified commit SHAs and retain least-privilege permissions. Never expose signing secrets to pull-request jobs or use `pull_request_target` to build untrusted code.
- Keep release keys out of Git and caches. Release APK packaging runs on merges/pushes to main and manual dispatch; signing uses the release environment secrets when configured. Publication and signing-key rotation require explicit authorization.
