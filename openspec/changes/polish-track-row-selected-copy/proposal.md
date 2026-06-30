# Proposal: Polish Track Row Selected Copy

## Summary

Replace debug/prototype copy shown on selected track rows with clear user-facing playback language.

## Problem

`TrackRow` currently renders `queued on shared UI ${(selectionAlpha * 100).toInt()}%` when selected. That exposes implementation/debug language and an animation percentage to users.

## Goals

- Remove the debug/prototype phrase from shared UI.
- Show a meaningful selected-row status: `Now playing`.
- Preserve existing row selection, click behavior, visual highlight, duration display, and playback behavior.

## Non-goals

- No playback-state model changes.
- No queue/reorder semantics.
- No equalizer animation in this change.
- No broad track-row redesign.
