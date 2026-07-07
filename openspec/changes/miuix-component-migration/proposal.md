# Change: Miuix Component Migration

## Why

RhythHaus already uses Miuix for theme, core components, and blur, but parts of the shared Compose UI still rebuild standard controls with bare `Row`, `Box`, `background`, `border`, and custom popup/dropdown logic. This increases UI inconsistency and makes common settings/list/dialog interactions harder to maintain.

The Settings appearance dropdown is the clearest example: it is a custom expanding card even though Miuix provides preference/dropdown-style components that better match the interaction.

## What Changes

- Add Miuix modules as needed, pinned to the existing `miuix` version reference, for components that are actually used.
- Replace the Settings appearance dropdown with a Miuix dropdown/preference component where available.
- Selectively migrate standard Search, Library row/container, and Clear Library dialog pieces to Miuix components when behavior and route animation semantics are preserved.
- Keep product-specific music UI custom where Miuix is not a suitable semantic fit.

## Non-goals

- No full app redesign.
- No native platform UI rewrite.
- No playback, scanner, database, route-stack, media-control, or adaptive-shell rewrite.
- No reintroduction of `miuix-navigation3-adaptive`.
- No Windows/Linux product or packaging scope.
