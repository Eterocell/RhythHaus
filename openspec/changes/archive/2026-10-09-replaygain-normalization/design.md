# ReplayGain design

## Decision

Implement the approved `replaygain-normalization` delta as tag-driven attenuation, not audio analysis. Off is the default; Track uses track gain/peak; Album uses album gain/peak and falls back as a pair to track values only when album gain is invalid or absent. No amplification, preamp, tag writes or new dependencies.

## Ownership and interfaces

- TagLib's existing path/FD native result gains four nullable raw string fields: `replayGainTrackGain`, `replayGainTrackPeak`, `replayGainAlbumGain`, `replayGainAlbumPeak`. Extend `RhTagLibMetadata`, its extraction/free functions, JNI conversion and iOS conversion together. Read the property map during the existing metadata read; do not reopen media for four separate queries.
- Library owns parsing raw tags to four nullable Double facts named `replayGainTrackGainDb`, `replayGainTrackPeak`, `replayGainAlbumGainDb`, `replayGainAlbumPeak`. Gain accepts a trimmed numeric value with optional case-insensitive `dB` suffix; peak accepts a trimmed number only. Reject malformed/non-finite values and nonpositive peaks independently. Missing tags never make an otherwise successful read fail.
- Add those facts to `AudioMetadata`, `LibraryTrack`, and `PlayableTrack`, preserving defaults for untagged media. Include LibraryTrack facts in equality/hashCode and all scan/repository/playback mappings. Display overrides retain them by copying only display fields.
- `:core:database` owns nullable REAL columns and `7.sqm` (v7 to v8). All track queries and upserts preserve them. Version-zero bootstrap distinguishes v7 from v8 via column presence. Old data migrates to NULL; a successful rescan clears removed tags. Recoverably unreadable tracks keep existing facts.
- `:core:playback` owns `ReplayGainMode { Off, Track, Album }`, serialized lowercase values, and pure `replayGainFactor(track: PlayableTrack?, mode: ReplayGainMode): Float`. Compute gain in Double, clamping positive dB before exponentiation; selected peak bounds by `min(1, 1/peak)`. Convert the final bounded finite value to Float.
- `PlaybackController.setReplayGainMode(mode: ReplayGainMode)` updates policy through `selectionGate`. Store sleep attenuation separately from normalization; desired native gain is their product. Use existing version/generation guarded gain requests and engine mutex. Apply composed gain before every play/load handoff, including restored/reconciled sessions and remote commands. Mode/metadata changes must not reload, seek, restart or alter run ownership. Stop/release may reset a stopped native engine to unity, but subsequent play recomputes policy before audio begins.
- Shared owns `ReplayGainPreferenceStore` (`selectedMode: Flow<ReplayGainMode>`, `suspend setSelectedMode(mode: ReplayGainMode)`) and DataStore implementations/factories following theme/onboarding patterns. Serialize writes; publish/controller-apply only after persistence succeeds. Corrupt/unreadable startup defaults Off. A failed write retains last accepted mode and exposes a localized recoverable Settings error. No optimistic preference emission.
- Settings consumes neutral selected mode/busy/error state and callbacks. EN/ZH accessible Off/Track/Album choices and attenuation-only/untagged explanation; no platform intents or database dependency.

## Concurrency and failure policy

Gain requests must carry the existing generation/version owner. A pending old fade/mode update cannot write into replacement playback. Timer cancel/rearm clears only sleep attenuation. Metadata reconciliation refreshes normalization even if queue metadata was already replaced by a scan publication. Preference cancellation must not leave persisted and accepted/controller mode divergent; finish publication after a committed write before propagating cancellation. Invalid optional tags cannot fail scanning/playback.

## Verification

Real tagged disposable media must exercise native metadata extraction, then actual scanner persistence and controller gain, without touching user files. Permanent regressions cover migration/reopen, tag removal, independent malformed fields, selection/queued-gain races, sleep cancellation and pre-resume gain, preference failures and accessible Settings selection. Physical listening and system controls remain user-owned pending evidence. Full baseline/toolchain limits must be recorded rather than reported as success.
