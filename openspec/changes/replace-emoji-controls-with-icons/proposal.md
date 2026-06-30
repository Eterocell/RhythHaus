# Proposal: Replace Emoji Controls With Vector Icons

## Summary

Replace emoji/text control glyphs in the mini player and now-playing transport controls with a cohesive vector icon system.

## Problem

The shared UI currently uses emoji/text glyphs for search, settings, play, pause, previous, and next controls. Emoji rendering differs across Android, iOS, and macOS fonts, causing inconsistent size, color, baseline, and style.

## Goals

- Use Material vector icons for transport, search, and settings controls.
- Preserve existing button layout, colors, click behavior, playback behavior, and navigation behavior.
- Keep artwork fallback initials/music-note behavior out of scope unless it is part of a transport/search/settings control.

## Non-goals

- No redesign of the mini player or now-playing screen.
- No playback queue behavior changes.
- No scanner, persistence, route, theme, or platform-specific changes.
- No custom animated equalizer or new visual language beyond replacing control glyphs.
