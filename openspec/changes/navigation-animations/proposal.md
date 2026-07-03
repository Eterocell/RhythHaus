# Proposal: Navigation Animations

## Summary

Add shared Compose route transition animations for RhythHaus Home, detail, Now Playing, Search, Settings, and Clear Library dialog routes.

## Problem

RhythHaus now has a centralized shared `LibraryNavigationStack`, consistent back affordances, and system/predictive back wiring. Route changes still render instantly. Instant swaps make navigation feel abrupt and do not visually communicate whether the user moved deeper into the app or returned to a previous screen.

## Goals

- Animate shared Compose route transitions for Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog.
- Make push and pop directions visually distinct.
- Keep the existing `LibraryNavigationStack` as the source of truth.
- Preserve existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.
- Avoid adding dependencies.

## Non-goals

- No native platform navigation migration.
- No navigation-library adoption.
- No deep links or saved-state restoration.
- No custom Android predictive-back progress animation.
- No screen content redesign.
