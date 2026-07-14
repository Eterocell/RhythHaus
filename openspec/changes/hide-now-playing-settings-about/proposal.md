## Why

The shell-owned Now Playing bottom bar overlaps the focused Settings, About, and Open Source Libraries experiences even though those routes provide their own navigation and actions. Hiding the bar across that settings-information route group removes unrelated chrome without interrupting playback.

## What Changes

- Suppress the Now Playing bottom bar while Settings is the current route.
- Suppress the Now Playing bottom bar while About is the current route.
- Suppress the Now Playing bottom bar while Open Source Libraries is the current route.
- Restore the bar according to its existing visibility state after navigating away.
- Retain current behavior on every route outside the settings-information group.

## Capabilities

### New Capabilities

- `route-aware-now-playing-bar`: Defines which navigation routes permit the shell-owned Now Playing bottom bar and how route eligibility composes with existing visibility state.

### Modified Capabilities

None.

## Impact

The change is limited to shared navigation presentation and common navigation tests. It does not affect playback state, queues, persistence, dependencies, platform entry points, or database schemas.
