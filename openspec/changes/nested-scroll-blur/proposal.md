# Change: Nested Scroll Blur

> **Superseded — 2026-11-12.** Do not implement or sync this proposal. The Haze/blur and temporary plain-scrim nested-scroll chrome it describes is no longer part of the current Library UI. The Library-home overlay was removed by [`library-home-settings-spacing`](../library-home-settings-spacing/proposal.md); current `LibraryHomeContent` and `DrillDownView` render no nested-scroll top-bar overlay, and `LibraryChrome.kt` contains only the drill-down scrollbar. The unchecked tasks below are abandoned, not active backlog. Do not revive blur, scrim, or Miuix overlay work from this proposal.

## Why

Library/Home and track-list screens currently scroll as flat lists. The roadmap calls for a Material 3 Expressive-like nested-scroll effect with a Backdrop/Haze-style blur treatment to add depth while preserving the existing RhythHaus look.

## What Changes

- Add shared top scroll chrome that reacts to list scroll position.
- Apply it to Library/Home and album/artist track-list pages.
- Use existing common Compose APIs instead of adding a new dependency in this pass.
