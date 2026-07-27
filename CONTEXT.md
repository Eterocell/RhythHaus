# RhythHaus

RhythHaus is a local-first music application whose shared Library experience coordinates browsing, playback, saved playlists, and navigation across supported platforms.

## Language

**Back intent**:
A user request to leave or dismiss the foremost Library experience, resolved in the order playlist modal, playlist edit mode, page-scoped selection, Now Playing, then route navigation. One Back intent performs exactly one dismissal or navigation transition.
_Avoid_: Route invalidation, deletion completion

**Displayed playlist invalidation**:
The condition where the current playlist-detail destination no longer exists after its playlist is deleted. It atomically leaves that destination and discards state owned by it, but is not a Back intent and does not discard unrelated Library state.
_Avoid_: Back, deletion Back

**Active destination**:
The Library destination currently presented as the user's navigation target. Retained, hidden, stale, or outgoing destinations are not active destinations and cannot consume a Back intent.
_Avoid_: Any composed destination, any registered destination

**Predictive Back target**:
The single dismissal or navigation transition chosen when a predictive Back gesture begins, identified by its active destination and target instance. The target remains stable through the gesture; cancellation performs no transition, and completion performs no transition if that same target is no longer foremost and valid.
_Avoid_: Completion-time Back target

**Unhandled Back intent**:
A Back intent for which the Library has no modal, edit mode, selection, Now Playing, or route transition to perform. The invoking adapter retains responsibility for any platform or interaction default.
_Avoid_: Consumed no-op

**Back transition**:
The single dismissal or navigation change selected for a Back intent. It remains in flight until authoritative state confirms that its target is no longer active or explicitly reports that the transition could not complete; further Back intents are ignored while it is in flight.
_Avoid_: Callback completion, fall-through dismissal

**Page-scoped selection**:
A track selection owned by one eligible Library page. It can consume a Back intent only while its owning page is the active destination; selection retained elsewhere is stale state to reconcile.
_Avoid_: App-wide selection

**Foremost dismissal**:
The one modal dismissal an active destination makes available to Library Back resolution. Ordering among confirmations or other modal experiences remains owned by that destination.
_Avoid_: Global modal stack
