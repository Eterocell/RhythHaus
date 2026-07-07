# Proposal: Adaptive Now Playing Screen

## Summary

Add an adaptive expanded Now Playing layout for wide tablet/desktop windows while preserving the existing compact phone Now Playing screen.

## Problem

The expanded Now Playing screen is currently phone-first: it stacks artwork, metadata, progress, and controls vertically across the full screen. This works on phones, but on tablets and desktop/macOS windows it wastes horizontal space and can make the controls feel disconnected from the artwork.

The Library screen now has a local adaptive shell. Now Playing should follow the same threshold policy and local Compose approach without adding an external adaptive dependency.

## Goals

- Preserve the existing compact vertical Now Playing layout.
- Add a wide split layout: artwork/accent visual on the left, track metadata/progress/playback controls on the right.
- Reuse the same width/height threshold policy as the Library adaptive layout.
- Preserve existing expanded overlay animation, drag-to-collapse, left-edge swipe back, back dismissal, seek, shuffle, repeat, previous, play/pause, and next behavior.
- Keep the implementation in shared Compose common code.

## Non-goals

- No queue/up-next pane.
- No redesign of playback controls or media engine behavior.
- No artwork cache/decoder redesign.
- No platform-native Now Playing UI.
- No `miuix-navigation3-adaptive` or Navigation3 adaptive dependency.
