## Why

The Library home adds a scroll-triggered top bar even though its in-content header already identifies the page, creating redundant chrome. Settings also combines system-safe padding with generous app-owned padding and spacing, making the page feel looser than the rest of RhythHaus.

## What Changes

- Remove the Library home's nested, scroll-triggered blur top bar without adding a replacement bar.
- Keep the Library home content below the system status-bar safe area and preserve its existing header and scrolling behavior.
- Keep Settings system-safe padding while reducing app-owned page padding and item spacing to the approved compact values.
- Add a testable shared layout policy for the Settings spacing values.
- Preserve drill-down top bars, navigation, playback, scanning, source management, dialogs, dependencies, persistence, and platform integrations.

## Capabilities

### New Capabilities
- `library-ui-spacing`: Defines Library home chrome behavior and compact, safe-area-aware Settings spacing.

### Modified Capabilities

None.

## Impact

- Shared Compose Multiplatform Library and Settings UI.
- Shared common tests for UI layout policy and obsolete nested-chrome state coverage.
- No API, dependency, database, scanner, playback-engine, or platform-source changes.
