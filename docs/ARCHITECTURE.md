# UI independent from device tracking

The app remains a single Android module. Package boundaries make UI changes possible without editing the logging service; Gradle modules can follow if enforcement or build times justify them.

```mermaid
flowchart TD
    Activity[MainActivity: wiring] --> Navigation[Navigation and feature routes]
    VM[DashboardViewModel] --> Contract[DashboardRepository interface]
    AndroidRepo[AndroidDashboardRepository] -. implements .-> Contract
    AndroidRepo --> Platform[Android usage, network and call APIs]
    AndroidRepo --> Storage[DataManager: existing preference schema]
    Service[LoggingService: device monitoring] --> Storage
    NotificationService[Notification listener] --> NotificationState[Notification state and history]
    AndroidRepo --> NotificationState
    VM --> State[Immutable DashboardUiState]
    State --> Content[DashboardContent]
    Content --> Actions[DashboardAction]
    Actions --> Navigation
    Navigation --> VM
```

## Actual boundaries in this change

- `MainActivity` now starts the existing service and wires dependencies. The old activity's duplicated network/radio receivers and telemetry writes are removed.
- `data/device` owns device and provider helpers, `data/local` owns the existing persistence adapter, and `data/model` holds activity records. These packages do not depend on Compose.
- `AndroidDashboardRepository` reads raw snapshots off the UI thread. `DashboardViewModel` owns refresh/error state and maps measurements to presentation state. `DashboardContent` sees neither the repository nor a context.
- The repository is injected through a ViewModel factory. No DI framework is needed for this size of app.
- `AnalyticsScreen` is a small integration adapter using a repository and background queries; `AnalyticsContent` renders the result. Query identity is metric + period, so a selection change cancels the previous effect.
- `SettingsScreen` owns Android account/document/settings launchers and dispatches file work to IO. `SettingsContent` renders state and emits actions. A dedicated settings repository and ViewModel are the next boundary improvement; settings state currently lives in this adapter.
- `AppUsageContent` uses the same supplied app rows as the dashboard; it performs no second platform query.
- Shared formatting lives in `core`, theme and reusable Material controls in `ui/theme` and `ui/components`, and screens are grouped by feature.

## Intentional compatibility adapters

The preference key names, installed application ID, and Android service names remain unchanged. Existing metric histories, notification details/replies, and summary/report exports remain reachable through adapters. Those older dialogs still mix UI and platform access. They must migrate to feature state and repositories before the whole UI can be described as independent of services.

This is a package and presentation boundary refactor, not a Room/DataStore migration. `DataManager` getters can still trigger rollover, and separate instances still synchronize inadequately. Removing activity sensor callbacks reduces duplicate writers but does not make the underlying storage transactional.

## Next storage changes

1. Introduce one process-level transaction owner for session changes and rollover. Inject time and timezone rules and test the current preference behavior first.
2. Store sessions/history in Room with explicit schema versions, indexes, and transactions. Keep live counters as a cache, not the sole event history.
3. Move small settings to DataStore. Migrate legacy values once; never silently reset installed users.
4. Emit repository flows from storage. Remove periodic dashboard rereads and filter call-provider queries to the requested cycle.
5. Move notification state models out of the Android service, then migrate notification/summary/history routes independently.
6. Address recent Android foreground-service limits separately, with physical-device verification and a documented product decision about achievable monitoring.

## Checkable invariants

`python3 scripts/check_architecture.py` prevents data/services from importing Compose or UI, prohibits platform work in Content files, and prevents device monitoring from returning to the activity. This is a narrow dependency guard, not a substitute for compilation, lint, or runtime testing.
