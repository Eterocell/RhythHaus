# Proposal: Android notification permission recovery

## Problem

On Android 13+, RhythHaus requests `POST_NOTIFICATIONS` at startup and discards the result. Local playback continues after denial, but notification/lock-screen media controls are unavailable without explanation or recovery.

## Goal

Expose an explicit and recoverable Android notification-permission state without leaking Android APIs into Shared or Settings.

## Scope

- Model unavailable, granted, requestable denial, and settings-required denial.
- Keep Android permission/rationale/intent mechanics in `androidApp`.
- Pass only immutable state and callbacks into Shared/Settings.
- Show a Settings card only on Android denial; explain that in-app playback remains available while notification/lock-screen controls are unavailable.
- Re-request when allowed; otherwise open app notification settings.

## Non-goals

No change to Media3 service lifecycle, notification channels, background policy, playback behavior, persisted state, iOS, or desktop.
