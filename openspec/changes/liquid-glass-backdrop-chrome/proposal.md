# Proposal: Liquid Glass Backdrop Chrome

## Summary

Replace the current scrim-only nested-scroll top chrome and solid bottom `NowPlayingBar` panel surface with Kyant0 Backdrop liquid-glass effects, while preserving the current RhythHaus layout and interactions.

## Problem

The nested-scroll top chrome was originally specified as Backdrop/Haze-style frosted depth, but the current implementation uses a bounded translucent scrim. The bottom bar also remains a solid panel. The user requested applying Kyant0 AndroidLiquidGlass / Backdrop instead of haze on both the nested-scroll top bar and bottom bar.

## Goals

- Use the Kyant0 Backdrop library (`io.github.kyant0:backdrop`) for the shared Compose glass effect.
- Apply Backdrop glass to the Library/Home and album/artist track-list nested-scroll top chrome.
- Apply Backdrop glass to the bottom `NowPlayingBar` rounded card.
- Preserve all existing layout, behavior, state, navigation, playback, scroll visibility, and inset handling.

## Non-Goals

- No UI layout redesign of the top chrome or bottom bar.
- No playback, scanner, library persistence, navigation-stack, route-transition, or media-control changes.
- No Haze dependency or Haze API reintroduction.
- No native platform UI rewrite.
