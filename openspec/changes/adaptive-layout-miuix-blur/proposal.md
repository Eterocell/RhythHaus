# Proposal: Adaptive Layout with Miuix Blur

## Summary

Add a wide-screen adaptive shared Compose layout for RhythHaus tablets/desktops and replace the current Kyant Backdrop glass implementation with Miuix blur.

## Problem

RhythHaus currently uses a phone-first one-pane layout everywhere. On tablets and desktop/macOS windows, the Library and album/artist detail screens waste horizontal space and force full-screen route swaps that feel awkward on computer and pad-sized screens.

The current glass implementation also uses Kyant Backdrop/Shapes while the user wants the Miuix stack. Miuix `miuix-navigation3-ui:0.9.x` does not include adaptive APIs. The separate `miuix-navigation3-adaptive:0.8.5` artifact exists, but final Android verification proved its older transitive Miuix dependency graph is incompatible with the current Android app, so the final implementation removes it and uses an in-project wide two-pane shell.

## Goals

- Add adaptive list-detail UI for wide screens while preserving the current compact phone UI.
- Remove `top.yukonga.miuix.kmp:miuix-navigation3-adaptive:0.8.5` after the Android duplicate-class blocker is proven.
- Keep Miuix UI/blur on the current approved `0.9.3` line.
- Replace Kyant Backdrop/Shapes with Miuix blur, with Android lower-minSdk runtime gating.
- Preserve existing navigation stack semantics, playback, scanner, search, settings, clear-library dialog, Now Playing overlay, gestures, and bottom/top glass behavior.

## Non-goals

- No full Navigation3 ownership migration.
- No native platform UI/navigation rewrite.
- No playback, scanner, database, media control, or Now Playing redesign.
- No Windows/Linux product scope.
- No Miuix downgrade without explicit user approval.
