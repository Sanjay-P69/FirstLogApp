# Future conversational analytics over SQLite

The intended product includes a chat destination that can answer complex questions
about recorded device activity by querying SQLite. This is an architectural target;
the current app still persists telemetry in preferences/JSON, and the existing daily
summary is not a database-querying agent. This UI change does not pretend otherwise.

## One data foundation for charts and conversation

Use Room over SQLite as the transactional source of truth. Chart repositories and
the agent's query tools must use the same metric definitions, time windows, units,
permissions, and coverage semantics. Do not give the chatbot a separately calculated
copy of “screen time” that disagrees with the dashboard.

The current typed `AnalyticsSeries` boundary separates timestamps, millisecond
values, and recorded/partial/missing/future states from UI labels. Replace its legacy
preference adapter with a database-backed implementation after migration; keep
rendering independent of the storage engine. A bounded repository query, not a
Composable, owns the database operation.

## Proposed persisted facts

| Facts | Core fields | Why retain them |
| --- | --- | --- |
| Activity intervals | Stable ID, metric, UTC start/end, source ID, observed/estimated quality | Split sessions across hour/day boundaries and calculate overlap without manufacturing hourly distributions. |
| App sessions | Stable app/package identity, foreground start/end, source, quality | Rank apps and compare historical behavior from actual intervals; keep background service time a distinct measure. |
| Battery samples | UTC sample time, level, charging state, source | Plot observed levels and distinguish remaining percentage from accumulated percentage-point consumption. |
| Charging sessions | UTC start/end, start/end levels, active/completed status | Calculate charging duration without inferring an unseen charging curve. |
| Unlock events/sessions | UTC event/end time, duration, quick-glance rule version, estimated method | Support distributions while preserving the uncertainty of method inference. |
| Call/notification metadata | Stable source/event identity, time, type/category, duration where relevant | Count the defined events instead of treating retained UI lists as complete logs. Keep sensitive content outside default analytics views. |
| Monitoring coverage | Source/metric, UTC observed interval, permission and logger state | Distinguish observed inactivity while asleep from unobserved time while tracking was stopped. |
| Tracking cycles | Cycle ID, start/end instants, reset-rule version, timezone | Make historical “day” definitions reproducible across reset-time, timezone, and DST changes. |
| Legacy aggregates | Original cycle/date, metric, raw total, provenance, known coverage | Preserve existing users' totals without inventing individual events during migration. |

Use explicit units, indexes on metric/app/time, and transactions for interval updates
and rollover. Use stable source IDs for deduplication and documented retention for
raw facts and derived aggregates. Avoid a schema consisting only of display labels
and generic floating-point chart values. Choose SQL features supported on the app's
minimum Android version or deliberately select a compatible SQLite driver.

## Query and chat boundary

The agent may formulate SELECT queries, but executes them through an analytics query
service. That service opens a read-only connection to approved tables/views, binds
parameters, applies row/time/resource limits, and returns typed results with units,
coverage, period, timezone, and provenance. Do not execute model-generated SQL using
the service's write connection or accept arbitrary schema/file-changing operations.
A regex check for SELECT is insufficient enforcement; use the database's read-only
mode and a constrained query surface.

Useful tools include schema/metric discovery, bounded SQL analysis, and a result-to-
chart mapping. Their outputs should include the executed query or query description,
selected date range, sample/record count, and completeness. The chat response can
then show an answer and a chart using the same components as Trends, and the user
can inspect the supporting results. Use page-scoped ViewModel state and normal
Material conversation UI; do not place agent control flow in the chart renderer.

Examples include comparing weekday screen-time distributions, ranking apps across
tracking cycles, and correlating battery changes with observed foreground sessions.
Correlation must not be presented as per-app measured energy consumption. Sleep
must not be inferred merely from a gap in screen activity. If the necessary facts
were never collected, the assistant must say what can and cannot be established.

If a remote model is used, explain the data sent and respect the user's data scope.
Keep credentials out of query views. Default tools should return aggregates and
necessary metadata rather than notification bodies or phone numbers. A read-only
query still needs cancellation, resource bounds, and truthful error/missing-data
reporting.

## Migration and rollout

1. Specify existing metric semantics and add deterministic rollover/concurrency
   fixtures before changing writers.
2. Create the schema and migrations. Import legacy totals with explicit provenance,
   verify totals, and retain recovery backups. Do not synthesize lost sessions from
   a daily total or the last 50 unlock records.
3. Move monitoring to one transactional writer. Record coverage; validate real idle
   hours and process-death recovery. Keep current IDs, preference compatibility,
   backup handling, and installed user data intact through the transition.
4. Implement database-backed chart/history repositories. Compare fixture totals
   against the legacy adapter while avoiding two independent active writers.
5. Add the bounded read-only query tool and chat UI, then validate representative
   multi-step questions and result reproducibility. Test date filters, empty and
   incomplete periods, joins, duplicate events, cancellation, and resource limits.

No database migration, chatbot, or arbitrary SQL execution is introduced by the
current chart/detail implementation. These tasks follow the storage correctness
work rather than being hidden inside a visual refresh.
