# CarPlay Audio Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add iOS CarPlay browsing and transport control backed by the existing Shared library and playback session.

**Architecture:** `iosApp` owns CarPlay scene lifecycle and Apple framework templates. A narrow Shared facade owns immutable projections and validates/dispatches commands into the existing library repository and `PlaybackController`; no second queue, player, or persistence path is introduced. A single iOS system-media publisher owns complete Now Playing metadata and remote-command registration for both lock-screen and CarPlay surfaces.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Swift 5, SwiftUI, CarPlay, MediaPlayer, AVFAudio, XCTest, Shared framework.

**Spec:** `openspec/changes/carplay-audio-integration/specs/ios-carplay-audio/spec.md`, `openspec/changes/carplay-audio-integration/specs/playback-session-settings/spec.md`

## Global Constraints

- Shared remains the sole owner of library, queue, playback state, persistence, and playback policy.
- CarPlay MUST NOT create a second audio engine, queue, or persisted catalog.
- Swift-facing Shared ABI MUST be narrow; do not export repositories, SQLDelight rows, `PlaybackController`, feature implementation types, or generated Compose resources.
- CarPlay commands MUST revalidate current authoritative identity/revision at invocation and reject stale entries without mutating playback.
- iOS callbacks and mutable CarPlay/system-media state are main-thread serialized and lifecycle-safe across reconnects.
- Apple-managed `com.apple.developer.carplay-audio` approval and provisioning are external prerequisites; local unsigned/simulator builds must not claim distribution authorization.
- No MusicKit, Apple Music catalog, Siri/App Selection, network source, Android/macOS CarPlay surface, or import behavior.

## Review Focus

- Scene reconnect/disconnect must not duplicate command targets, observers, templates, or callbacks.
- A stale CarPlay list item must not select a different track or mutate the current queue.
- Duplicate queue occurrences must retain visible order and occurrence identity.
- Artwork-only updates must preserve complete Now Playing metadata and elapsed position.
- Loading/empty library and missing entitlement paths must remain bounded, buildable, and truthful.

---

### Task 1: Shared CarPlay contract and projection

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/carplay/CarPlayFacade.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/carplay/CarPlayFacadeTest.kt`

**Interfaces:**
- Produces immutable `CarPlaySnapshot`, library entry/category projections, playback projection, `CarPlayCommandResult`, and callback-first facade methods suitable for Swift export.
- Consumes `LibraryRepository`, authoritative library publication, `PlaybackController`, and existing selection/queue helpers.

- [ ] Write failing common tests for localized category labels, stable revisions, loading/empty/populated snapshots, exact visible queue order including duplicate tracks, stale entry rejection, and transport command routing.
- [ ] Run the focused Shared JVM test and verify RED because the facade/projection does not exist.
- [ ] Implement the narrow facade and an App-owned lifecycle bridge. Use one process-lifetime owner to publish current snapshot and validate revision plus current track identity before dispatch.
- [ ] Run focused Shared tests and iOS framework compilation; verify GREEN and no playback-policy bypass.
- [ ] Mark OpenSpec tasks 1.1–1.4 complete with evidence.
- [ ] Commit: `feat: add shared CarPlay facade`.

### Task 2: Unified iOS Now Playing publisher

**Files:**
- Create: `iosApp/iosApp/NowPlaying/RhythHausSystemMediaPublisher.swift`
- Modify: `iosApp/iosApp/App/RhythHausAppBootstrapper.swift`, `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/NowPlaying/RhythHausArtworkProvider.swift`
- Test: `iosApp/iosAppTests/RhythHausSystemMediaPublisherTests.swift`

**Interfaces:**
- Consumes the Shared CarPlay facade and existing `NowPlayingArtworkProvider`/audio provider bridge.
- Produces complete `MPNowPlayingInfoCenter` updates and idempotent `MPRemoteCommandCenter` registration routed through Shared.

- [ ] Write failing Swift tests for dictionary construction, stale field removal, artwork merge, capability gating, and idempotent register/dispose.
- [ ] Run the focused XCTest and verify RED.
- [ ] Implement main-thread serialized publisher; make artwork updates merge into the complete current dictionary instead of replacing metadata.
- [ ] Route play/pause/next/previous/seek through the Shared facade and ensure unavailable commands return failure without mutation.
- [ ] Run iOS host XCTest and inspect all output.
- [ ] Mark OpenSpec tasks 2.1–2.4 complete and commit `feat: unify iOS system media publishing`.

### Task 3: Native CarPlay scene and templates

**Files:**
- Create: `iosApp/iosApp/CarPlay/RhythHausCarPlaySceneDelegate.swift`
- Create: `iosApp/iosApp/CarPlay/RhythHausCarPlayTemplateFactory.swift`
- Modify: `iosApp/iosApp/Info.plist`, `iosApp/iosApp/iOSApp.swift`
- Test: `iosApp/iosAppTests/RhythHausCarPlayTemplateTests.swift`

**Interfaces:**
- Consumes the Shared CarPlay facade and unified system-media publisher.
- Produces a CarPlay root template with Songs, Albums, Artists, Playlists, detail navigation, and Now Playing; every handler completes exactly once.

- [ ] Write failing Swift policy tests for loading/empty/populated hierarchy, stable opaque IDs, stale selection rejection, and disconnect/reconnect cleanup.
- [ ] Run focused XCTest and verify RED.
- [ ] Implement `CPTemplateApplicationSceneDelegate` and template factory on the main thread. Refresh only from authoritative snapshots; preserve playback on disconnect.
- [ ] Register the CarPlay scene manifest and localize labels without exposing Compose resources to Swift.
- [ ] Run iOS Simulator XCTest and app build; verify scene registration and framework ABI.
- [ ] Mark OpenSpec tasks 3.1–3.4 complete and commit `feat: add iOS CarPlay scene`.

### Task 4: Entitlement, acceptance, and closeout

**Files:**
- Create: `iosApp/iosApp/RhythHaus.entitlements`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`, `roadmap.md`, `progress.md`
- OpenSpec: synchronize canonical specs and archive change.

- [ ] Add `com.apple.developer.carplay-audio` declaration and target signing setting, explicitly documenting that Apple managed capability/profile approval is required.
- [ ] Run CarPlay Simulator smoke where available: connect, browse, select, Now Playing, external playback changes, disconnect/reconnect, empty/loading. Record unavailable entitlement/physical-hardware evidence without overstating it.
- [ ] Run focused JVM/XCTest, iOS compilation, Spotless, Detekt, architecture, OpenSpec, and diff checks.
- [ ] Perform independent whole-branch review and repair any Critical/Important findings through RED→GREEN tests.
- [ ] Synchronize and archive OpenSpec; update roadmap/progress with verification and manual acceptance ownership.
- [ ] Commit `feat: add CarPlay support`.
