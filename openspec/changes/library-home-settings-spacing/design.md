## Context

The Library home currently owns two page-identification layers: `HeaderSection` inside the list and `NestedScrollBlurChrome` after scrolling. The list already reserves the system status-bar inset, so the overlay is redundant rather than required for safe placement. Settings uses `safeContentPadding()` correctly, but combines it with generous app-owned padding and item spacing.

The change is limited to shared Compose Multiplatform UI and must preserve the distinct album/artist drill-down chrome, navigation, playback, scanning, source management, and platform behavior.

## Goals / Non-Goals

**Goals:**

- Remove the Library home nested top bar without replacing it.
- Preserve the Library header and system-safe list placement.
- Keep Settings safe-area protection while applying the approved compact spacing values.
- Make the Settings spacing contract testable through a pure internal layout policy.
- Remove home-only nested-chrome declarations if they become dead code.

**Non-Goals:**

- Redesigning Library content, Settings controls, typography, colors, or dialogs.
- Changing album/artist drill-down bars or Miuix scroll behavior.
- Changing navigation, playback, scanning, persistence, dependencies, or platform integrations.

## Decisions

### Remove the Library home overlay instead of replacing it

`LibraryHomeContent` will retain its status-bar top content padding and existing `HeaderSection`, but will no longer derive nested-chrome state or render `NestedScrollBlurChrome`. A static title row was considered, but it would preserve the same duplicated page identity the change is intended to remove. Keeping the current bar without animation was also rejected because it would continue occupying visual space above an already sufficient in-content header.

### Preserve system safe areas and reduce only app-owned Settings spacing

`safeContentPadding()` remains the source of status-bar, cutout, navigation-bar, and desktop-safe-region avoidance. The app-owned values become 16 dp horizontal page padding, 8 dp vertical page padding, 12 dp inter-item spacing, and 8 dp final bottom content padding. Removing safe padding was rejected because it would trade visual density for device-specific overlap risk.

### Encode Settings values in an internal layout policy

An immutable internal policy value will expose the four approved `Dp` values. `SettingsScreen` will consume it directly. This creates deterministic common-test coverage without adding screenshot infrastructure or test-only production APIs. Hard-coding the values inline was rejected because it would leave the requested density contract unprotected.

### Remove dead home-only chrome infrastructure

If `NestedScrollBlurChrome`, `NestedScrollChromeState`, and `nestedScrollChromeStateFor` have no remaining production callers after the home overlay is removed, they will be deleted with their dedicated progression tests. Scroll-position and Now Playing bar visibility logic remains because it serves separate behavior.

## Risks / Trade-offs

- **Risk: Library home may feel less anchored after scrolling.** → The in-content header remains the canonical title, and no functional action currently lives in the removed overlay.
- **Risk: Settings may become too dense on a specific device class.** → System safe areas and 44 dp targets remain unchanged; manual visual QA is recorded as required follow-up evidence.
- **Risk: Removing similarly named chrome helpers could affect drill-down screens.** → Only declarations with no production callers are removed; `DrillDownMiuixScrollChrome` and its helpers remain explicitly out of scope.
- **Trade-off: Pure policy tests do not prove rendered pixels.** → They protect the exact approved values, while platform builds and manual visual QA cover integration and perception.
