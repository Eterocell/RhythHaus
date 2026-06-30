# Proposal: UI/UX Fixes Batch

## Summary

Implement the first batch of shared Compose UI/UX fixes from the current source-level review: first-run onboarding, adaptive album grid, normal-user removal of developer panels, Search polish, larger compact controls, and a direct Songs browse mode.

## Problem

The app has working local library and playback surfaces, but several visible flows still feel prototype-like or high-friction:

- empty-library users must discover import through Settings;
- album browsing is fixed at two columns regardless of screen width;
- user-facing Now Playing exposes developer TagLib metadata panels;
- Search result taps start playback but leave the user inside Search;
- some compact navigation/action controls are below comfortable mobile touch-target size;
- users cannot browse all tracks directly because browsing only supports Albums and Artists.

## Goals

- Show a primary Add music folder path on Home when the library is empty.
- Adapt album cards to phone/tablet/desktop widths.
- Remove developer TagLib panels from normal UI.
- Add Search clear behavior and dismiss Search after selecting a result.
- Increase compact Back/Search/Settings hit targets to at least 44dp.
- Add Songs as a browse mode with direct playback.
- Keep changes shared-first in common Compose code with tests for pure behavior.

## Non-goals

- No native platform UI rewrite.
- No new dependencies.
- No playback engine, scanner, MediaSession, audio-session, or database schema changes.
- No playlists, genres, folders/sources, recently added, queue redesign, or stable album identity redesign in this batch.
- No claim of manual visual validation unless performed separately.
