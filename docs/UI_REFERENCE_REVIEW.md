# ADNS and Essentials: reference review

Reviewed on 2026-10-22, following the user's references. The earlier design work
used Now in Android, Reply, Jetchat, and Connect You; these two apps were reviewed
afterward. This document records source and screenshot observations, not a claim
that FirstLog already reproduces their visual quality.

## Reviewed snapshots and scope

- [ADNS at 374e5b1](https://github.com/eyalm2000/adns/tree/374e5b1193491b5b12ed50fb761115bf2ceff020).
  Inspected its home, statistics, and settings screenshots, segmented settings rows,
  screen scaffold, cards, theme, appearance repository, statistics ViewModel, group
  position tests, and build dependencies.
- [Essentials at cff6f9a](https://github.com/sameerasw/essentials/tree/cff6f9ac857acc52e543bd1aa0b690c49dc29414).
  Inspected its published screenshots 1, 2, and 4, group containers, expandable
  categories, hero/action cards, top app bar, floating toolbar, theme, settings
  repository/ViewModel, and build dependencies.

Screenshots are repository marketing assets and can predate the surrounding source.
Neither app was installed or built in this review. Their MIT licenses permit reuse
with the required notices, but this change copies no source, fonts, icons, or images.

## Visual observations and decisions for FirstLog

| Reference pattern | Why it works | FirstLog decision |
| --- | --- | --- |
| ADNS [home screenshot](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png): large headline and one dominant status | The purpose and current state are immediately legible. | Keep screen time as Today's primary observation. Do not add a large decorative shield/device image to a logging dashboard. |
| ADNS [settings screenshot](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png) and [segmented rows](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/app/src/main/java/com/eyalm/adns/ui/components/SegmentedSettingRows.kt) | Shared outer corners and small internal gaps make related settings read as a group. Labels, descriptions, and trailing actions have consistent positions. | Replace stacks of unrelated-looking buttons with connected action/toggle rows. Use separate composables for actions, switches, and selection; keep the shared layout small. |
| Essentials [group container](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/ui/core/containers/RoundedCardContainer.kt) and [screenshots](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpeg) | Repeated row geometry and quiet surface tones create coherence even across many features. | Use theme-derived surfaces and shapes. Secondary connection measurements belong in a readable group; each measurement does not need an equally prominent standalone card. |
| Essentials [stacked action card](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/ui/components/StackedActionCard.kt) | Explanation and action stay together, with enough space for a meaningful label. | Use a clear explanation plus one action for missing tracking access. Keep permanent configuration controls quieter than setup/recovery actions. |
| ADNS [screen scaffold](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/app/src/main/java/com/eyalm/adns/ui/screens/settings/SettingsScreenScaffold.kt) and Essentials [top app bar](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/ui/components/ReusableTopAppBar.kt) | Shared title/back behavior makes screens feel like one application. | Prefer native large/flexible app bars with standard scroll behavior when supported by the pinned dependency. Root navigation owns insets; screen wrappers must not add them twice. |
| Essentials [expandable categories](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/ui/components/CategoryExpandableSection.kt) | Expansion lets a large utility app reveal secondary controls progressively. | Consider expansion for long secondary groups only. Keep screen time, active tracking issues, and the primary action visible. Save expansion state and announce expanded/collapsed state. |
| ADNS [statistics screenshot](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png) | A total, scope controls, a temporal plot, and supporting lists make analytics easier to interpret. | Keep scope/total near a data-appropriate plot. Choose bars, lines, or distribution views from the actual measurement and question; see [chart decisions](DATA_VISUALIZATION.md). Retain exact accessible values. ADNS's custom graph is not a native Material chart component, but that alone is not a reason to reject charts. |
| Essentials [floating toolbar](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/ui/components/EssentialsFloatingToolbar.kt) | It uses the native `HorizontalFloatingToolbar` API and has a recognizable selected state. | Keep FirstLog's native navigation bar/rail for its three persistent destinations. A floating action toolbar is not automatically a better navigation model. Any future floating control needs reserved scroll clearance and accessible labels. |

The reference screenshots show that a restrained palette and repeatable structure
matter more than adding a decorative shape to every metric. FirstLog should retain
native component behavior while improving hierarchy and grouping. Bold primary
typography is appropriate; shrinking body text or truncating measurements is not.

## Engineering practices worth adopting

1. **UI state distinct from queries.** ADNS's [StatsViewModel](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/app/src/main/java/com/eyalm/adns/viewmodel/nextdns/StatsViewModel.kt)
   exposes immutable state and distinguishes graph loading, card loading, refresh,
   and errors. It cancels obsolete requests and checks request generations before
   applying results. FirstLog should keep this behavior when moving Trends into
   a ViewModel; a late result for an old metric/period must not overwrite the new one.
2. **Observe preference changes and release listeners.** Essentials's
   [settings repository](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/src/main/java/com/sameerasw/essentials/data/repository/SettingsRepository.kt#L863)
   bridges preference listeners with `callbackFlow` and unregisters them with
   `awaitClose`. This is a useful migration pattern for settings. It does not by
   itself fix FirstLog's transactional telemetry/rollover problem.
3. **Separate appearance preferences from rendering.** ADNS's
   [appearance repository](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/app/src/main/java/com/eyalm/adns/data/appearance/AppearanceRepository.kt)
   uses the application context and exposes read-only state. FirstLog's future
   settings repository should provide validated persisted preferences; the theme
   should receive the resolved choice through the app's state boundary.
4. **Determine group geometry from visible content.** ADNS has an explicit
   single/first/middle/last group model and tests its edge cases. Compute position
   after permission- or feature-dependent filtering. Prefer native grouping APIs
   and defaults where available, rather than scattering corner calculations.

## Practices to evaluate rather than copy

- Essentials's settings repository is about 4,629 lines at the inspected revision
  and imports `ui.core.pickers.NetworkType`. Its small SettingsViewModel also
  constructs a repository from a context passed into actions. FirstLog should keep
  injected repositories and data models independent of UI; visual polish does not
  establish architectural quality.
- ADNS's statistics ViewModel constructs concrete repositories internally. Adopt
  its state/cancellation behavior while keeping FirstLog's injectable interfaces.
- ADNS's row API and Essentials's top app bar expose many optional flags/slots;
  the latter accepts `Any` titles. Prefer typed, smaller feature contracts. A shared
  visual treatment does not require one universal control for every use case.
- Some reference components use hardcoded radii/font overrides, direct platform
  work, or custom animation. Keep FirstLog's theme tokens, rendering-only Content
  boundary, and platform actions in routes. Do not add blur, custom wave graphs,
  copied font assets, or per-category rainbow colors merely to match screenshots.
- Both inspected themes call `MaterialTheme`. A component or file named
  “Expressive” is not proof that it is a native Expressive API. Check actual imports,
  default motion/shapes, semantics, and dependency availability.
- ADNS's [app build](https://github.com/eyalm2000/adns/blob/374e5b1193491b5b12ed50fb761115bf2ceff020/app/build.gradle.kts#L102)
  explicitly adds Material 3 `1.5.0-alpha16`, beyond its catalog declarations.
  Essentials uses [Material 3 alpha24](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/gradle/libs.versions.toml)
  and [compile SDK 37.1](https://github.com/sameerasw/essentials/blob/cff6f9ac857acc52e543bd1aa0b690c49dc29414/app/build.gradle.kts#L51).
  FirstLog currently builds with SDK 36 and alpha13. Verify the actual API against
  that baseline before using `SegmentedListItem`, `LargeFlexibleTopAppBar`, or other
  newer overloads. Do not upgrade the SDK or duplicate dependency declarations
  incidentally while copying an example.

## FirstLog follow-up and acceptance criteria

These are reviewed design/engineering tasks, not changes already implemented by
this reference review:

1. **Settings first:** replace button-heavy groups with coherent native rows;
   retain one obvious export action, clear restore/reset confirmations, and a
   labelled weekly-backup toggle. A switch row must expose one accessible toggle
   target, have at least a 48 dp hit area, and dispatch exactly one state change
   for a tap on either the row or its switch. Use parent toggle semantics with a
   passive switch, or separate correctly labelled controls; never double-toggle.
2. **Dashboard secondary groups:** preserve the screen-time hero and summary
   action, then reduce equal-weight connection cards. Use aligned labels, values,
   and status within semantic groups; show details on demand without hiding access
   problems. Keep independently denied permissions distinct from measured zero.
3. **Shared screen chrome:** standardize title/back/scroll behavior on native
   components. Check system bars, landscape, large screens, and content clearance.
   Keep the existing navigation bar/rail until there is a concrete UX reason to
   change it.
4. **Settings and Trends state:** migrate the existing integration adapters to
   injected repositories and ViewModels. Use lifecycle-aware collection, observable
   settings, cancellation, and current-query checks. Preserve preference keys and
   tracking data through a tested migration.

Review light/dark, dynamic/fallback color, 320 dp, tablet, RTL, 200% text, and TalkBack.
Use fake data in previews. Runtime tests remain a local/device option; do not add
emulator CI, which the user explicitly excluded. The current review changes only
documentation and agent guidance; it does not supply new FirstLog screenshots or
claim that these follow-ups have shipped.
