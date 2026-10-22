# FirstLog Material 3 Expressive design

## Product hierarchy

FirstLog should answer three questions: how did I use my phone today, when did that activity happen, and what do I need to configure? The three root destinations are **Today**, **Trends**, and **Settings**.

Today begins with the date, a clear title, and screen time as the primary observation. Daily summary is one tonal action below that observation. Activity groups app time, battery usage, and unlocks; Connections groups radios and location; Calls & notifications groups communication. Secondary details sit behind a tap. Wi-Fi and mobile enabled together is labeled as a device state, not proof of simultaneous data transfer. Location enabled is not labeled as time actually spent using GPS.

Trends offers one metric filter and one Day/Month/Year selector. Its current progress rows are an interim implementation. The target visualization follows the user question and the data's units, temporal granularity, coverage, and distribution: interval totals usually need bars, sampled levels can use lines, category rankings need horizontal bars, and session distributions need histograms. See [the data inventory and chart decisions](DATA_VISUALIZATION.md) before implementing a chart. Preserve exact values and an accessible textual/table view alongside the plot. App durations remain available in the app list. The previous fixed-factor app battery allocation is removed because it is not measured per-app battery consumption.

Settings groups appearance, tracking, backups, daily summary, and activity management. Reset and restore show an in-app explanation before modifying user data. Backups use destination-neutral language: a system file picker may save to Drive, but scheduled backups are local. A selected account is only a report label. Background access is described without a guarantee of uninterrupted monitoring.

## Native design system

- Use `MaterialExpressiveTheme`, with dynamic light/dark colors on supported devices and the existing brand fallback on older devices. Keep the explicit Material 3 version override in the catalog: `1.5.0-alpha13` was verified against Google's release notes dated January 28, 2026. This explicit alpha pin keeps Expressive APIs aligned with the SDK 36 / Compose 1.10 baseline; the debug build is verified in CI and device/visual validation remains.
- Use Material typography at its standard sizes; the former 18 sp `titleLarge` and reduced body sizes made the whole app unnecessarily dense. Numeric headline styles use tabular figures.
- Use large Material shapes for grouped cards, tonal surfaces for hierarchy, a primary container for the main metric, and the native navigation indicator. Reserve error color for actionable failures.
- Use native cards, list items, progress indicators, buttons, filter chips, segmented buttons, sheets, dialogs, and navigation. Avoid gradients, glowing icon frames, custom spring/bounce gestures, and unrelated metric accent colors.
- Metric cards show title, value, and status, with wrapping rather than ellipsis for important data. Adaptive columns use a 180 dp minimum, allowing a single column at small widths or large text.
- Use 20 dp page spacing, 12 dp gaps between peers, 20–24 dp inside groups, and native minimum interactive target sizes. Do not globally rescale these to cram in more cards.
- Material components provide the chart's surrounding controls and visual language. A focused, themed chart renderer is allowed for actual data visualization; a progress indicator is not a universal chart substitute. Validate data semantics and accessibility before selecting a renderer or library.

## Interaction and state

- A denied permission has a specific enable action, not a fake zero count. Calls, Bluetooth, app usage access, and notification access have independent states.
- Loading and first-load failure have their own UI. A refresh failure preserves the last snapshot and marks it stale. Every visible control emits an action.
- Root selection and app-list navigation survive activity recreation. Back from a secondary destination returns to Today; back from the app list returns to Today.
- A metric sheet has a working history action. Existing histories, notification replies, and report exports are retained through legacy routes while those screens migrate.
- Read current permission state after returning from settings. Do not optimistically claim a grant.

## References and what to borrow

These were inspected as references, not copied as code or assets:

- The user's [ADNS](https://github.com/eyalm2000/adns) and [Essentials](https://github.com/sameerasw/essentials) references were reviewed afterward in detail, including their published screenshots and implementation. See [the reference review](UI_REFERENCE_REVIEW.md) for pinned source links, patterns to adopt, architectural caveats, dependency compatibility, and the gap between the current FirstLog UI and the next design pass. Prioritize their connected settings rows, tonal grouping, clear primary observations, and consistent screen chrome. These are stronger references for the requested visual direction; the Android samples below remain useful for architecture and adaptive behavior.
- [Now in Android](https://github.com/android/nowinandroid), especially its [theme](https://github.com/android/nowinandroid/blob/main/core/designsystem/src/main/kotlin/com/google/samples/apps/nowinandroid/core/designsystem/theme/Theme.kt): centralized color roles, explicit dynamic-color behavior, and separation of app wiring, data, and design system. This app does not need NiA's full module graph.
- [Reply](https://github.com/android/compose-samples/tree/main/Reply): adaptive Material navigation and a content-first hierarchy. The current implementation uses adaptive dashboard columns; a native navigation rail replaces the bottom bar at expanded widths.
- [Jetchat](https://github.com/android/compose-samples/tree/main/Jetchat): Material 3 theming, UI state, and standard interactions rather than decorative wrappers around every control.
- [Connect You](https://github.com/you-apps/ConnectYou): a Compose Material 3 utility-app reference with light/dark themes and native system data integration. Useful as a restraint reference, not a source of Expressive API guarantees.
- [Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) and [Expressive theme source](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialTheme.kt): verify actual APIs before inventing custom substitutes.

## Review and handoff

Use `DashboardPreviews.kt` for compact light, compact dark, 320 dp / 1.5x text, and tablet configurations. These are executable Compose previews, not rendered screenshots in this environment. Verify TalkBack order, keyboard focus, 200% font size, landscape, display cutouts/insets, and permission-return flows on devices.

The primary screens are redesigned, but the retained metric-history, notification-detail, and graphical-summary dialogs still contain older styling and platform access. Do not treat them as finished Expressive components. Exported report artwork may have its own visual layout; it is not a pattern for interactive app UI.
