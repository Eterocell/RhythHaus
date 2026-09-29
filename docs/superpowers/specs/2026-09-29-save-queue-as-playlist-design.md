# Save Queue as Playlist — Design

## Intent and outcome
The listener can preserve the current playable queue as a named saved playlist without changing playback. The Queue tab already displays the selected occurrence and its upcoming occurrences; saving that visible sequence, including repeated tracks, is the expected result. Manual device/UI verification belongs to the user; the implementation owner verifies behavior automatically.

## Scope
Offer “Save queue as playlist” on the Queue tab when there is a selected current occurrence. Capture the displayed current + upcoming track IDs when opening the name dialog, not the historical queue prefix. The draft remains fixed while the dialog is open; cancelling writes nothing. On confirmation, use the existing name validation and `PlaylistStateOwner.mutate { createWithEntries(name, snapshot) }` path. Successful mutation publishes the authoritative playlist snapshot and closes the modal; failure keeps the name and snapshot available to retry with an error. The saved playlist appears in Saved and persists on restart. Queue entries retain duplicates and their order. No queue, playback position, shuffle/repeat, timer, source, media file, or media tag mutation.

## Boundaries
`:feature:playlists:impl` owns the Queue action, name modal, frozen draft, and localized strings. `:shared` forwards a narrow callback that takes name and immutable ordered track IDs into its existing playlist mutation launcher; no new database schema or repository API. `:core:playback` remains unchanged. Only visible current + upcoming items are copied; an empty or unselected queue offers no save action. A source/track deletion between snapshot and confirmation may cause the existing atomic createWithEntries transaction to fail, leaving no partial playlist and preserving the modal error state.

## Verification
A feature JVM Compose regression exercises empty queue, duplicate tracks and order, dialog snapshot despite queue changes, cancellation, failure retention, success close and accessibility at compact dimensions. A Shared integration regression proves the callback reaches real `PlaylistStateOwner` and saves a persistent playlist without mutating the controller queue. Existing repository transaction tests cover foreign-key failure rollback. Run focused feature/Shared JVM tests, desktop and iOS compilation, formatting, static analysis, architecture check and strict OpenSpec validation; record unavailable Android native build separately. Manual UI/listening/system-control acceptance remains user-owned and pending until reported.
