# Sanjay's next steps: Tracewise

## First conversation with Antigravity

Read this file, AGENTS.md, and instructions.md before editing. Explain the current
state in plain English. Devasy's preferred name is **Tracewise**. Ask Sanjay whether
he agrees before making further branding changes; do not block building the current
project on that conversation. Do not ask him to paste passwords, keys, or tokens.

Keep all three naming options available:

| Name | Kotlin namespace/package, including Sanjay's name | Idea |
| --- | --- | --- |
| **Tracewise** (Devasy's choice) | `com.sanjay.tracewise` | Turn recorded traces into useful understanding. |
| Dayweave | `com.sanjay.dayweave` | Weave daily activity into context and insights. |
| Daylens | `com.sanjay.daylens` | A clear lens on each day's activity. |

These are working names, not trademark/domain availability claims. Preserve the
current installed application ID `com.Sanjay.firstlogapp`, storage keys, and Android
component names unless a separate, tested migration is explicitly requested.

## Build before changing features

- [ ] Follow instructions.md to build debug, then the signed release using the
  existing shared key. Never generate a replacement key.
- [ ] Read the latest PR/build results on GitHub. A queued or configured run is not
  a successful build. Keep uncompiled changes in draft.
- [ ] Verify the new icon on a phone: normal, round, and themed launcher modes.

## Product direction

The end goal is an AI-assisted activity and settings companion. SQLite is the
permanent local source of truth. The assistant should answer questions with deep,
multi-step SQL analysis, generate analytics, explain supporting evidence, help
change supported settings, and offer useful proactive observations.

These capabilities are not implemented by the name/icon change. Follow
docs/AGENT_ANALYTICS_ARCHITECTURE.md in this order:

- [ ] Test existing rollover, concurrency, timezones, process restarts, and backup
  behavior before replacing persistence.
- [ ] Introduce versioned Room/SQLite migrations that preserve existing records,
  totals, and measurement coverage. Do not silently expire permanent history.
- [ ] Use one transactional monitoring writer and the same typed metrics for
  dashboard, charts, and conversation.
- [ ] Add bounded read-only SQL analytics tools with units, date ranges, coverage,
  provenance, cancellation, and execution limits.
- [ ] Add conversational settings actions through typed repository/platform tools,
  separately from analytics SQL, preserving destructive-action confirmations.
- [ ] Add opt-in proactive observations with quiet hours, deduplication, rate
  limits, and inspectable evidence. Respect Android background restrictions.
- [ ] Test on a real device and verify that old records survive app updates.
