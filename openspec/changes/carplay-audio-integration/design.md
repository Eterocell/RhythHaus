# Design: iOS CarPlay Audio Integration

## Goal and ownership

Add a native CarPlay audio scene while preserving Shared as the sole owner of the library, queue, playback state, persistence, and playback policy. Swift in `iosApp` owns CarPlay scene lifecycle and `CarPlay` framework templates; Swift must call a narrow public Shared facade for snapshots and actions. CarPlay never reads SQLDelight directly and never creates a second player or queue.

## Existing boundaries

- `iosApp/iOSApp.swift` configures Swift providers in `RhythHausAppBootstrapper` and creates the SwiftUI app.
- `iosApp/ContentView.swift` hosts the stable Shared `MainViewController`.
- `RhythHausAudioPlayerProvider` owns two native AVAudioPlayer voices and the AVAudioSession; Shared `PlaybackController` owns selection, queue, generations, transitions, timers, and session persistence.
- Shared App composition currently creates the controller, repository, and authoritative `libraryContent` locally inside Compose. Swift cannot safely query these objects through the current public ABI.
- The iOS target uses a filesystem-synchronized Xcode group, generated Info.plist, and iOS deployment target 26.0.
- Existing artwork integration mutates only the artwork field in `MPNowPlayingInfoCenter`; metadata/command ownership needs one coordinated iOS system-media publisher to avoid clobbering fields.

## Proposed boundary

Add a narrow `iosCarPlay` public facade in Shared commonMain and an iOS actual/adapter that the Swift CarPlay scene can call through the exported `Shared.framework`. The facade exposes immutable CarPlay-specific localized snapshots and callback-first async actions only; it does not export `PlaybackController`, SQLDelight, library repositories, or feature implementation types.

The Shared App composition owns a process-lifetime bridge owner connected to the existing `PlaybackController`, `LibraryRepository`/authoritative publication, and playlist state. It publishes immutable snapshots containing a revision, localized library sections/entries, current track metadata/artwork bytes, playback status/position/duration, and capability booleans. Incoming Swift commands are validated against the latest snapshot/repository identity at invocation and dispatched through the same controller operations used by the app. Track selection uses the exact ordered visible context, preserving duplicate occurrences and existing occurrence IDs where applicable.

Swift adds `CPTemplateApplicationScene` configuration and a scene delegate in the iOS app target. It renders list templates for the v1 chosen hierarchy: Songs, Albums, Artists, and Playlists, with detail drilling as required by the system templates; categories unsupported by the available Shared projection are not fabricated. CarPlay list identifiers are opaque stable IDs, and every action resolves against the latest facade revision before invoking a command. A single Now Playing template presents existing Now Playing metadata and delegates supported commands to Shared.

The Swift iOS system-media provider becomes the single writer for the complete `MPNowPlayingInfoCenter` dictionary and `MPRemoteCommandCenter` targets. Existing `RhythHausArtworkProvider` is folded into this coordination boundary or adapted so artwork refreshes cannot replace metadata/elapsed state. System commands dispatch through a Shared-owned action bridge; callbacks and mutable CarPlay scene state are serialized on the main thread. Observer and command-target installation/removal is idempotent across scene reconnects and app lifetime.

The existing Shared framework export policy stays narrow. Export only the CarPlay facade/DTO ABI required to Swift; add declarations to commonMain when a stable generated Swift owner is required. No CarPlay code or dependency enters Android, desktop, or core playback modules.

## Entitlement and scenes

Register the `CPTemplateApplicationScene` role in the generated Info.plist scene manifest, with the CarPlay audio scene delegate class. Add a checked-in entitlements plist containing `com.apple.developer.carplay-audio = true`, and set `CODE_SIGN_ENTITLEMENTS` for the iOS app target. The declaration does not grant capability: Apple must approve the CarPlay Audio managed capability and the App ID/provisioning profile must include it. Simulator builds without signing remain possible; successful signing/distribution is explicitly conditional on that external approval.

## Localization and failure behavior

CarPlay-visible labels use system locale-aware localized strings from the iOS app bundle or injected Shared labels; do not expose Compose resources. Loading and empty states are bounded CPListTemplate content. Failed/stale commands complete their CarPlay handler exactly once, preserve playback state, and publish a recoverable message/template update. Scene disconnect removes scene-local templates and observers only; shared playback continues.

## Testing

- Pure Swift policy tests cover localized category projection, stable identity/revision resolution, stale action rejection, duplicate occurrence preservation, and command availability.
- Shared common tests cover bridge owner snapshot projection, exact queue order, stale library/playback command rejection, and failure/cancellation semantics.
- iOS Simulator build/test verifies scene manifest, delegate, framework ABI, remote command ownership and lifecycle registration without entitlement signing.
- CarPlay Simulator smoke verifies root, drilldown, selection, Now Playing, external app playback updates, disconnect/reconnect, and empty/loading behavior.
- Physical CarPlay hardware and Apple entitlement approval/provisioning remain user-owned acceptance gates.

## Explicit non-goals

- No MusicKit, Apple Music catalog, Siri media-intent/App Selection integration, network sources, or media import changes.
- No independent CarPlay queue persistence, second player, or policy bypass.
- No Android/macOS CarPlay abstractions or cross-platform CarPlay surface.
- No claim that an entitlement plist alone authorizes App Store or vehicle distribution.
