# Proposal: Standardize Back Navigation

## Summary

Unify RhythHaus shared Compose back affordances and make Android system back use the predictive-back gesture pipeline for in-app route pops.

## Problem

Back actions currently use several visual labels:

- Drill-down: `← BACK`
- Now Playing: `← LIBRARY`
- Search and Settings: `< Back`

The same action therefore appears as three different controls, which makes the app feel stitched together and weakens users' spatial expectations. Android also needs explicit predictive-back support so system gesture back pops the in-app route stack instead of behaving like a separate escape path.

## Goals

- Provide one shared visual back affordance for drill-down, now playing, search, and settings.
- Standardize the visible label on `‹ Back`.
- Route visible back controls, left-edge swipe back, and Android system/predictive back through the same in-app pop behavior.
- Explicitly opt the Android app into predictive back in the manifest.

## Non-goals

- No full navigation-library migration.
- No route-stack redesign beyond the existing explicit `LibraryNavigationStack`.
- No platform-specific native toolbar implementation.
- No deep-link, saved-state, or transition-animation work.
