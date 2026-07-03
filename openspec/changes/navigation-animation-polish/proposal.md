# Proposal: Navigation Animation Polish

## Summary

Three polish items on top of the base navigation animation: Android predictive back visual progress, fixed bottom bar during route transitions, and a custom expand/collapse animation from the bottom bar to the Now Playing screen.

## Problem

1. Android predictive back currently pops the route on completion with no visual progress during the gesture.
2. The bottom bar slides/fades with route content during `AnimatedContent` transitions instead of staying fixed.
3. Tapping the bottom bar replaces it with the Now Playing screen via a standard route push (fade/slide), not a natural expand animation from the bar.

## Goals

- Provide predictive back visual progress on Android.
- Keep the bottom bar fixed during route transitions.
- Replace the route-push expand with a custom expand/collapse animation from the bar to Now Playing.
- Preserve existing playback, scanner, library, theme, search, settings, and back navigation behavior.
- Avoid adding dependencies.

## Non-goals

- No `SharedTransitionScope`/`SharedTransitionLayout` adoption.
- No native platform navigation migration.
- No navigation-library adoption.
- No deep links or saved-state restoration.
- No screen content redesign beyond the expand/collapse mechanism.