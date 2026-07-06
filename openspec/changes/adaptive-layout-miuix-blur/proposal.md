# Proposal: Adaptive Layout with Miuix Blur

## Summary

Add a wide-screen adaptive shared Compose layout for RhythHaus tablets/desktops and replace the current Kyant Backdrop glass implementation with Miuix blur.

## Problem

RhythHaus currently uses a phone-first one-pane layout everywhere. On tablets and desktop/macOS windows, the Library and album/artist detail screens waste horizontal space and force full-screen route swaps that feel awkward on computer and pad-sized screens.

The current glass implementation also uses Kyant Backdrop/Shapes while the user wants the Miuix stack. Miuix `miuix-navigation3-ui:0.9.x` does not include adaptive APIs, but the separate `miuix-navigation3-adaptive:0.8.5` artifact exists.

## Goals

- Add adaptive list-detail UI for wide screens while preserving the current compact phone UI.
- Try `top.yukonga.miuix.kmp:miuix-navigation3-adaptive:0.8.5` for the adaptive pane scaffold.
- Keep existing Miuix modules at current `0.9.2` versions unless an incompatibility is proven and the user approves a different strategy.
- Replace Kyant Backdrop/Shapes with `top.yukonga.miuix.kmp:miuix-blur:0.9.2`.
- Preserve existing navigation stack semantics, playback, scanner, search, settings, clear-library dialog, Now Playing overlay, gestures, and bottom/top glass behavior.

## Non-goals

- No full Navigation3 ownership migration.
- No native platform UI/navigation rewrite.
- No playback, scanner, database, media control, or Now Playing redesign.
- No Windows/Linux product scope.
- No Miuix downgrade without explicit user approval.
