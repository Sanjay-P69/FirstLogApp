# Choose charts from the data and the user question

The chart type follows the question, measurement type, sampling/aggregation,
coverage, and distribution. Material styling comes afterward. The current Trends
progress rows are an interim view, not a rule that all analytics must use indicators.

This source audit covers the current models/storage. No real user's dataset was
provided or profiled, so skew, outliers, typical ranges, and historical completeness
are unknown. Do not claim to have analyzed an empirical distribution from source
code alone.

## Data inventory and suitable views

| User question | Actual available data | Preferred view | Constraints |
| --- | --- | --- | --- |
| When did I use my phone? | Screen duration aggregated into hour buckets, plus live duration; archived cycle totals | Vertical bars by hour; bars by day for daily comparisons | These are interval totals, not instantaneous samples. Show bucket boundaries and mark the current partial bucket. |
| Is my usage changing over several weeks? | Archived daily/cycle totals | Straight line with point markers for trend; bars when exact daily comparison is the main task | Order real dates, distinguish partial cycles, and leave gaps for unknown coverage. A rolling average is optional only when labelled with its window and missing-data rule. |
| Which apps account for the most recorded use? | Per-app foreground/usage duration and launch counts | Sorted horizontal bars with app names and values | Use a separate unit/view for launches. Do not derive per-app battery consumption from usage shares or add foreground and background counters without checking their semantics. |
| How did battery level change? | Timestamped battery-level samples retained for roughly two hours | A step or straight line over actual timestamps | State the retained window. Remaining battery uses a 0–100% axis. Do not invent a whole-day curve, smooth spikes away, or connect a monitoring gap as observed continuity. |
| How much battery was used in each period? | Hourly accumulated downward battery changes and archived cycle consumption | Vertical bars by hour/day | This is consumption in percentage points, not remaining charge. Cumulative daily consumption can exceed 100 after recharging; do not cap it at 100. |
| When was the phone charging? | Charging session start/end times, levels, and duration | Interval timeline with labelled start/end values | Use recorded sessions and identify an active unfinished interval. Do not infer the charging curve between endpoint levels. |
| Are my phone sessions mostly brief or long? | Unlock-session duration records, currently capped at the latest 50 | Histogram with explicit duration bins and retained sample count | Label it “latest recorded sessions,” not the whole day. Inspect actual samples before selecting bin widths or a log scale. Timestamp is recorded at session end. Unlock-method labels remain heuristic estimates. |
| How many calls happened, and when? | Call-log event timestamps, type, and duration when permission/provider access allows | Count bars by time bucket; separate duration bars; grouped outcome bars where useful | Count and duration have different units. Existing call charts attribute a call's entire duration to its start bucket; split intervals first if the question is time spent within each hour. |
| Which apps generate notifications? | Notification history with app/category/time, reset by calendar day; persisted history capped at 300 | Ranked horizontal count bars, explicitly describing retained history | Notification updates/deduplication and retention affect counts. Active notifications are a separate quantity. Do not present the retained list as a complete delivery log. |
| When were radios/location enabled? | Enabled-duration aggregates and current/last state for Wi-Fi, mobile, Bluetooth, location | Duration bars by time bucket; current state as text/status | Enabled time is not transferred bytes or active GPS work. Wi-Fi/mobile/both-enabled durations overlap; they are not exclusive slices of a day. A full state timeline requires durable interval history. |
| What share belongs to each category? | Only valid when categories have a common, known, mutually exclusive denominator | A labelled 100% stacked bar for a few genuine parts; ordinary bars for precise comparison | Do not use pie/donut/stacked charts for overlapping radio states, incomplete retained histories, or fabricated app battery allocations. |

Relevant code: [records](../app/src/main/java/com/Sanjay/firstlogapp/data/model/ActivityRecords.kt),
[storage](../app/src/main/java/com/Sanjay/firstlogapp/data/local/DataManager.kt),
[call aggregation](../app/src/main/java/com/Sanjay/firstlogapp/data/device/CallLogHelper.kt),
[notification retention](../app/src/main/java/com/Sanjay/firstlogapp/AppNotificationListenerService.kt),
and [analytics contract](../app/src/main/java/com/Sanjay/firstlogapp/data/repository/AnalyticsRepository.kt).

## Data issues to resolve before richer plots

- `getChartData` returns display labels and floating-point hours. It loses actual
  timestamps, bucket boundaries, units, quality, and coverage. Repeated month
  initials are ambiguous. Rendering cannot recover this information from labels.
- Hourly reads use zero as the preference default. Monthly reads fill missing past
  days and future days with zero. A generated set of 24 buckets does not establish
  24 hours of observation. Existing zeroes may be valid or coverage-unknown; do not
  silently relabel all of them either way.
- The day path adds a difference between cycle total and hourly sum into the
  current hour. That reconciliation is not an observed hourly measurement. Expose
  unattributed time separately until it can be assigned from reliable intervals.
- Hourly keys use calendar dates, while totals can use a configurable reset cycle.
  Call charts and notification history have their own calendar/cutoff behavior.
  Resolve the requested interval and timezone once, including DST, before comparing
  series or generating a heatmap. A day need not have exactly 24 clock hours.
- Battery samples are trimmed to two hours. Session details and persisted
  notifications are capped. Aggregate totals and retained events therefore do not
  necessarily describe the same population. Keep that distinction in the UI.
- Several helpers swallow query/parse failures and return empty results. Permission
  denial, read failure, unobserved intervals, recorded zero, partial/live buckets,
  estimates, and truncated history need distinct states.

## Typed boundary before chart rendering

Replace `List<Pair<String, Float>>` / label-only `TrendPoint` with a typed analytics
result at the repository boundary. This is a target contract, not a shipped change:

- Identify the metric, physical unit, aggregation (sum/count/sample), query interval,
  timezone/cycle definition, and provenance.
- For interval data, provide bucket start/end instants, raw values, and known
  coverage. For level samples, provide sample timestamp/value. For sessions, provide
  start/end or a clearly named end timestamp and duration.
- Represent missing, denied, failed, partial/live, estimated, and truncated data
  explicitly. Preserve sample counts and retention windows. Carry a selected metric
  and period identity so stale responses cannot replace a newer query.
- Keep milliseconds, seconds, bytes, counts, and percentage points precise until
  presentation. Format labels separately. Repositories do not return chart colors,
  pixel positions, Compose objects, or a UI chart class.
- Choose the renderer in presentation from the question and this typed data. Do
  not add a chart-type picker or silently switch chart types because a value crossed
  a threshold. Keep a view stable enough for users to learn and compare.

Profile representative, consented or synthetic datasets before implementing a
distribution view: sample count, retained/observed interval, missingness, zeros,
min/max, quantiles, skew, outliers, and category cardinality. Do not remove genuine
outliers or normalize away differences merely to make a balanced screenshot.

## Material styling and accessible rendering

Use Material 3 cards, app bars, controls, typography, color roles, and motion around
the plot. Chart rendering may use a focused Compose chart library or a small
isolated renderer when needed; it must not be labelled a native Material chart
without an actual native API. Theme a reusable chart surface through the UI layer
and review its accessibility, license, maintenance, and SDK/dependency compatibility
before selecting an implementation. No library is selected by this review.

Every plot needs a meaningful title/question, selected interval, unit, legible axes,
and exact values through accessible selection and an equivalent textual/table view.
Do not require precision dragging, hover, or color vision to read it. Give TalkBack
a concise summary and access to individual values without flooding the focus order
with decorative primitives. Keep targets at least 48 dp and respect font/motion
settings. A data table complements the chart; accessibility is not a reason to
replace every chart with a progress indicator.

Start comparison bars at zero. Keep line scales honest and clearly labelled; retain
timestamps and data gaps. Avoid decorative curve interpolation, dual axes for
unrelated units, rainbow categories, and gradients implying extra measurements.
For comparable side-by-side views use compatible scales and label any normalization.
Heatmaps require sufficiently complete, comparable buckets and an accessible table;
they are a future option, not evidence that multiweek history currently exists.

## Implementation order and verification

1. Define the query/quality contract and reconcile cycle/calendar/retention semantics.
2. Move selection/loading into a ViewModel with cancellation/current-query checks.
3. Add hourly/daily duration bars and ranked app bars from validated aggregates.
4. Add recent battery-level lines and charging intervals from actual sample/session
   records. Add a labelled session histogram only after profiling retained samples.
5. Verify meaningful invariants: units, no fabricated future zeros, gap preservation,
   partial cycles, DST boundaries, overlapping categories, and truncated samples.
   Use previews for tiny/empty/large/skewed data, long labels, light/dark, and large
   text. Connected tests remain local; do not add emulator CI.

This review changes documentation and agent rules. It does not claim the typed data
contract, richer charts, or collection/retention fixes are already implemented.
