# Proposal: Scroll Bar Visibility for Screens With NowPlayingBar

## Summary

Hide the `NowPlayingBar` when the user scrolls down through any scrollable screen that shows a bar, and show it again when the user scrolls up.

## Problem

The `NowPlayingBar` is currently fixed and always visible on Library browsing surfaces. This keeps playback controls accessible, but it also occupies screen space while the user is browsing downward through the main Library, Search results, or album/artist track lists. The requested interaction is for normal scroll direction on every screen that has a `NowPlayingBar` to control the bar: drag/scroll down hides it; drag/scroll up reveals it.

## Goals

- Hide the `NowPlayingBar` when a scrollable screen that renders a bar scrolls downward into content.
- Show the `NowPlayingBar` when that screen scrolls upward toward earlier content.
- Apply the behavior to the main Library/Home list, Search results, and album/artist track-list screens.
- Animate the bar in/out from the bottom.
- Preserve existing tap-to-expand, drag-up-to-expand, playback controls, Search, Settings, navigation, scanner, library, theme, and Now Playing overlay behavior.
- Avoid adding dependencies.

## Non-goals

- No native navigation migration.
- No new navigation or gesture dependency.
- No change to Now Playing screen layout or playback semantics.
- No change to Android predictive back, visible back chips, left-edge swipe back, or route transition semantics.
- No persisted user setting for bar visibility.
