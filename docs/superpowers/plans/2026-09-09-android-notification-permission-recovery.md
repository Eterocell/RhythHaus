# Android notification permission recovery — implementation plan

> **Route:** OpenSpec + Superpowers. User approved this design on 2026-09-09. Scope is `android-notification-permission-recovery` only; do not alter the Media3 service or claim manual system-control acceptance.

## Goal

On Android 13+, a `POST_NOTIFICATIONS` denial is visible from Settings, offers the appropriate Android recovery action, and never interrupts in-app local playback. The recovery surface is absent on iOS, desktop, Android < 33, and when Android notifications are granted.

## Contracts

- `MainActivity` owns permission/rationale/intent APIs and passes a small controller to `shared.App`.
- Shared owns immutable Android-free `MediaNotificationPermissionState` (`Unavailable`, `Granted`, `Requestable`, `SettingsRequired`) and an action controller. `App()` defaults to unavailable so non-Android callers stay unchanged.
- `:feature:settings` receives only an optional immutable recovery projection and callbacks. It owns text/UI/semantics but cannot import Android APIs.
- Notification denial does not gate the playback controller, queue, library mutations, or in-app controls. The card action remains enabled during a source mutation.

## Task 1 — Android-safe state and host policy

**Files:** Shared app contract; Android `MainActivity`; pure Android policy and focused tests.

1. Add RED tests for API < 33, grant, initial request, requestable denial, permanent denial, active-request suppression, permission-result refresh, and safe settings intent availability.
2. Implement the Android-free Shared state/controller and preserve default `App()` behavior for iOS/desktop.
3. Extract a pure Android classification policy. `MainActivity` owns state, initial one-time request, callback refresh, resume refresh, re-request, and application-notification-settings intent launch.
4. Run the focused test command; require RED before production implementation and GREEN afterward.

## Task 2 — Settings recovery projection and UI

**Files:** Shared App and route composition; feature Settings screen/resources/tests.

1. Add RED route and semantic tests for no recovery, requestable denial, and settings-required denial.
2. Thread the projection/actions from `App` through `LibraryHomeScreen` and `LibraryRouteOverlays`.
3. Render one feature-localized Settings card explaining that in-app playback remains available but Android notification/lock-screen media controls need notification permission. Requestable invokes only request; settings-required invokes only settings. No card/action/accessibility node exists when absent.
4. Register resources in the ownership ledger and run focused tests GREEN.

## Task 3 — complete verification and physical acceptance

1. Run focused Android, Shared, and Settings tests, then `:androidApp:assembleDebug`.
2. Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, and `./init.sh` separately as required.
3. On Android 13+, manually confirm deny → Settings explanation → re-request or notification settings → grant → return, while local playback remains uninterrupted. This is a required closeout gate.
4. Update OpenSpec task evidence, `progress.md`, and `roadmap.md`; commit/archive only after physical acceptance.
