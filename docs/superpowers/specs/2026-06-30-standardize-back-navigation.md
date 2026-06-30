# Back Navigation Polish Spec

## Decision

Standardize all visible in-app back controls on a shared `‹ Back` chip.

## Rationale

Destination-specific back labels are ambiguous now that Search, Settings, Now Playing, and dialogs can be opened from multiple origins. A single `‹ Back` affordance gives users one spatial rule: back returns to the previous route in the stack.

## Requirements

1. Drill-down, now playing, search, and settings must render the same visible back chip.
2. The chip label must be `‹ Back`.
3. The chip must call the route-stack pop callback already used by other navigation mechanisms.
4. Android system/predictive back must pop one in-app route when possible and must not be consumed on Home.
5. Android predictive-back opt-in must be explicit in the manifest.

## Non-goals

- No navigation-library migration.
- No custom predictive-back progress animation.
- No platform-native toolbars.
