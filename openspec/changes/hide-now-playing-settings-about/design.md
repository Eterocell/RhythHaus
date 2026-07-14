## Context

The Now Playing bottom bar is shell-owned chrome whose current visibility is also affected by Library scroll state. Settings, About, and Open Source Libraries are routes in the same shared navigation stack, so screen-local code cannot cleanly control this shell element. The change must remain shared across Android, iOS, and desktop and must not mutate playback or scroll state.

## Goals / Non-Goals

**Goals:**

- Hide the Now Playing bottom bar exactly on Settings, About, and Open Source Libraries.
- Restore existing behavior after leaving any suppressed route.
- Preserve existing scroll-driven visibility and playback state.
- Make route eligibility a pure, common-testable policy.

**Non-Goals:**

- Hiding the bar on routes outside the settings-information group.
- Changing bar content, animations, playback, navigation structure, or screen layouts.
- Introducing route metadata infrastructure or platform-specific behavior.

## Decisions

Add a pure route predicate in the navigation layer that returns false for `LibraryRoute.Settings`, `LibraryRoute.SettingsAbout`, and `LibraryRoute.OpenSourceLibraries` and true for every other existing route. The app shell will render the bar only when both the existing visibility state and this route predicate are true.

This central policy is preferred over screen-local callbacks because the app shell owns the bar. It is preferred over a generic metadata model because only one binary policy is required and an exhaustive route predicate remains explicit and easy to test.

Route suppression will not write to the existing scroll-derived visibility flag. Consequently, leaving the settings-information route group reveals the bar only if that flag already allows it, and a previously scroll-hidden bar remains hidden.

## Risks / Trade-offs

- [A new route could unintentionally inherit visible behavior] → Keep the route predicate exhaustive so additions require an explicit decision at compile time.
- [Route policy could override scroll behavior] → Combine route eligibility with existing state using logical conjunction and add regression coverage for both inputs.
- [Playback could be confused with UI visibility] → Do not call playback APIs or mutate current-track state; test only the pure presentation policy and shell condition.

## Migration Plan

No data or deployment migration is required. Rollback consists of removing the route predicate and restoring the previous shell condition.

## Open Questions

None.
