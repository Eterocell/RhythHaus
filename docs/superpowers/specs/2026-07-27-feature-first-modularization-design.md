# Feature-First Modularization Design

## Overview

RhythHaus will move gradually from the current monolithic KMP `:shared` module to a feature-first multi-module layout without changing observable behavior. The refactor establishes canonical architecture guidance and executable Gradle enforcement while preserving Kotlin packages during module moves. Package renames are a later, separate effort.

## Goals

- Preserve behavior, UI, Back behavior, scanning, playback, playlists, settings, database identity, and supported-platform behavior throughout migration.
- Use a contract-first strangler strategy: move contracts before implementations, keep every slice buildable, and never introduce a temporary `feature -> shared -> feature` bridge.
- Establish the target KMP module graph, clear ownership boundaries, Koin composition rules, and CI-enforced architectural gates.
- Provide canonical guidance in `skills/kmp-architecture/SKILL.md`, `docs/architecture.md`, ADRs for module boundaries and shared/iOS exports, and feature READMEs.

## Non-Goals

- UI or behavior redesign, or changes to Back, scanning, playback, playlist, or settings behavior.
- SQL schema, database name, migration-history, or data changes.
- Circuit, Decompose, Molecule, a new DI framework, Windows/Linux support, package renames while moving code, empty modules, or empty pattern classes.
- An empty `:core:network` module, broad iOS framework exports, or a Dependency Analysis Gradle Plugin adoption before graph stabilization and separate version/KMP compatibility evaluation.

## Decision

Adopt a demand-driven, feature-first KMP graph using a contract-first strangler migration. Each slice must compile and pass its focused checks independently. A contract moves before an implementation, feature implementations remain isolated, and any failed atomic slice remains incomplete rather than acquiring an illegal bridge dependency.

The final structure is:

```text
:androidApp, :desktopApp, iosApp
        |
     :shared (composition and iOS facade)
        |
  +-----+---------------------+
  |                           |
:core:*                   :feature:*:impl
                              |
                         :feature:*:api
```

Core modules are introduced only when they have real ownership:

- `:core:model`: truly cross-feature immutable projections only.
- `:core:ui`: reusable primitives, theme, and artwork abstractions; never feature UI state.
- `:core:database`: the sole SQLDelight schema, driver, migrations, and generated database.
- `:core:platform`: capabilities used by two or more domains, narrowly scoped.
- `:core:playback`: added when playback engine/contracts are extracted.
- `:core:navigation`: added only when common destination-scoped Back contracts are genuinely required.

There is no `:core:network` until a real cross-feature network concern exists. Features become `:feature:library:api/impl` and `:feature:playlists:api/impl`; Now Playing, Search, and Settings remain single feature modules at first. API/implementation splits happen only for a real stable contract.

`:shared` becomes a thin KMP composition and iOS framework facade. It owns `App()`, the root shell, cross-feature route and Back arbitration, lifecycle, Koin assembly, and the stable `MainViewController` entry. Applications depend on `:shared`. No core or feature module may depend on `:shared` or an app module; a feature may not depend on another feature implementation; cross-feature access is only through API modules; and `:shared` alone composes implementations.

## Ownership And Contracts

Library owns scanner/source access, indexing, repositories, library UI, and its transient UI state. Playlists owns playlist repository/edit/backup/UI, including backup/document behavior. Playback engine and its contracts belong in `:core:playback`. Leaf feature-internal state remains local. Repository/mapping logic stays in feature implementations even though `:core:database` owns the physical SQLDelight database.

### Task 4.1 Library And Playlist API Contracts

Task 4.1 is contract-only: publish the full existing Library and Playlist repository
interfaces while their implementations remain in `:shared`. `:feature:library:api`
owns `LibraryRepository` and every immutable signature type required by its existing
methods. It depends only on `:core:model`; scanner/source access implementations,
transient `ScanProgress`, in-memory and SQLDelight adapters, mappers, UI, and
playback-selection helpers remain implementation-owned in `:shared`.
The published contract retains the complete source, track, artwork, scan-session,
scan-error, and cleanup surface rather than reducing `LibraryRepository` to
`tracks()`. `LibraryTrack` and `TrackArtwork` preserve their existing content-based
`ByteArray` equality and hash semantics.

`:feature:playlists:api` owns `PlaylistRepository`, `PlaylistSummary`, `PlaylistEntry`,
and `PlaylistImportMutation`, with no production project dependency. The handwritten
API type is `PlaylistSummary` because `:core:database` already generates
`com.eterocell.rhythhaus.library.Playlist`; generated `Playlist` remains a
persistence representation owned by the database and `:shared` adapters map it to
`PlaylistSummary`. Every moved declaration preserves the existing
`com.eterocell.rhythhaus.library` Kotlin package. No SQL schema, table name, database
identity, migration history, or physical database ownership changes.
Every `PlaylistRepository` method that currently returns the generated `Playlist`
row returns `PlaylistSummary` at the feature API boundary; ordering, timestamps,
validation, rollback, entry, and import behavior remain unchanged.

Task 4.1 adds shared-owned internal transitional `libraryImplementationModule()` and
`playlistsImplementationModule()` Koin factories. They are included and composed only
by `rhythHausModule()` in `:shared`; API modules have no Koin dependency, and no
physical feature implementation modules are created in this slice. The factories move
with their implementations in later extraction tasks.

API-local tests cover interfaces, values, explicit API, and KDoc using private fakes;
existing implementation behavior tests remain in `:shared`. Shared DI tests prove both
abstractions resolve to the existing SQLDelight adapters and that only `:shared`
composes the factories. Architecture fixtures reject API-to-database/shared/
implementation and implementation-to-shared/other-implementation bridges.

Rejected alternatives are exposing generated database `Playlist` through the feature
API, renaming the SQLDelight table-generated row without a schema change, and deferring
Playlist API publication.

### Task 4.2 Core Playback Extraction

Task 4.2 atomically moves full playback ownership to `:core:playback`: `PlaybackController`;
playback state and engine contracts; dispatchers; Android engine, service, and transport
bridge; iOS engine, audio, artwork, and Now Playing bridges; JVM/macOS engine and native
bridge; and `rhythhaus_audio.mm`. `FakePlaybackEngine` moves unchanged with `Playback.kt`
into `core/playback/commonMain` production source for compatibility and whole-file/test
consumption. Only a later relocation from core production into test fixtures, visibility
demotion, or package rename is separate work. Core owns both complete session files:
`session/PlaybackSessionController.kt`, containing the
`PlaybackSessionController` behavioral port and `RevisionedPlaybackSessionSnapshot`, and
`session/PlaybackSessionSnapshot.kt`, containing `PlaybackSessionSnapshot`,
`SessionQueueEntry`, `PlaybackSessionCodec`, `PlaybackCheckpoint`,
`ProgressCheckpointKey`, and their normalization/value invariants. Shared consumes that
behavioral port and the required public value/codec contracts. `PlaybackSessionController`
is the only cross-module session behavioral port. Every Kotlin package declaration remains
unchanged. The Android manifest-relative service name remains
`.RhythHausPlaybackService`, and its unchanged FQCN remains
`com.eterocell.rhythhaus.RhythHausPlaybackService`. Objective-C/Swift-visible bridge names
and signatures; the preserved `MacAudioPlayerBridge` class; JNI exports
`Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_*`; dylib
`librhythhaus_audio.dylib`; and native resource roots `/native/macos-aarch64/...` and
`/native/macos-x64/...` remain unchanged.

`:core:playback` uses `api(:core:model)` and `api` coroutine dependencies required by its
public `Flow`/`StateFlow` signatures. It uses implementation dependencies on
`:core:platform` for package-stable `uuid4`, Kermit, and platform libraries only. It does not depend on
`:shared`, features, apps, DataStore, or Koin. Core owns an internal/private playback logger
(for example, `PlaybackLogger.kt` / `playbackLog`) backed by
`Logger.withTag("RhythHaus")`, preserving the tag and log behavior of the moved controller
and iOS engine code. `shared/Logger.kt` remains shared-owned for compatibility; core does not
import shared `log`. The architecture policy permits only
`:core:playback -> :core:model`/`:core:platform`, `:shared -> :core:playback`, and the
narrow shared iOS export described below; preserved root packages are accepted.

`:shared` retains `PlaybackSessionCoordinator`, `PlaybackSessionReconciler`, and the
`LibraryTrack` adapter; `PlaybackSessionStore`, DataStore actuals and factories;
`PlaybackProcessLifecycle`; App/root orchestration; artwork-loader composition;
Koin/process scope; and a thin package-stable `createPlatformPlaybackEngine()`
expect/actual composition family. The moved `PlaybackController` deliberately changes its
constructor/API: it no longer defaults its engine by calling shared-owned
`createPlatformPlaybackEngine()` and instead requires an explicit `PlatformPlaybackEngine`.
Shared Koin calls the retained `createPlatformPlaybackEngine()` factory and injects that
singleton into `PlaybackController`; core never calls shared. Public declarations are public
because shared consumes them; no larger coordinator or store API moves. Koin continues to provide
exactly one `PlatformPlaybackEngine`, exactly one `PlaybackController`, and a
`PlaybackSessionController` that resolves to that same controller.

`:core:playback` exposes public platform-specific factories returning
`PlatformPlaybackEngine`: `createAndroidPlaybackEngine()`, `createJvmPlaybackEngine()`, and
the existing `createIOSPlaybackEngine(relativeFilePathResolver)`. Engine implementation
classes may remain private/internal behind these factories. `:shared` retains its
package-stable `expect`/`actual createPlatformPlaybackEngine()` family solely as a
composition facade: the Android actual delegates to `createAndroidPlaybackEngine()`, the JVM
actual delegates to `createJvmPlaybackEngine()`, and the iOS actual delegates to
`createIOSPlaybackEngine(IOSRelativeFilePathResolver { ... appLocalMusicFolderPath ... })`.

iOS core defines exactly an iOS-only
`IOSRelativeFilePathResolver.resolve(relativePath: String): String` and the
`createIOSPlaybackEngine(relativeFilePathResolver)` port. Shared iOS supplies the resolver
with the library-owned `appLocalMusicFolderPath`; core handles absolute paths, URIs, and unsupported
descriptors itself and invokes the resolver only for relative `AudioSource.FilePath`.
`:shared` exposes `:core:playback` with `api` and exports exactly it from the sole Shared
iOS framework because existing Swift consumes the moved audio/artwork bridge symbols; no
separate framework is created. The Android app manifest keeps its unchanged service
declaration, while native helper build and resource ownership move unchanged to core
playback.

Task 4.2 begins with characterization and architecture RED tests before relocation. The
characterization/API tests cover the explicit `PlaybackController(PlatformPlaybackEngine)`
constructor shape, and existing `RhythHausDiTest` identity proof continues to show that
`PlaybackSessionController` resolves to the same controller. Core owns
`PlaybackControllerTest`, `PlaybackSessionSnapshotTest`, `AndroidPlaybackMediaSessionTest`,
`RhythHausTransportBridgeTest`, `JvmPlaybackEngineTest`, `IOSAudioPlayerBridgeTest`,
`IOSNowPlayingBridgingTest`, `IOSNowPlayingDiagnosticTest`, `IOSNowPlayingInfoTest`, and
`IOSCommandEnabledAfterTargetTest`; moved core platform tests call their corresponding core
factory rather than shared `createPlatformPlaybackEngine()`. Shared retains coordinator,
store, process, DI, app, library, and playlist integration tests. This extraction does not
broaden later feature implementation tasks.

### Task 5.1 Now Playing Feature Extraction

Task 5.1 creates one implementation module, `:feature:nowplaying`; it creates no API
module, presenter, state/event/effect scaffolding, or Koin module: it owns no injectable
bindings and is composed directly through callable/composable entry points. The actual Compose
implementation moves from shared `nowplaying/NowPlayingScreen.kt`, `NowPlayingBar.kt`, and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt` to
`feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
and moves `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`
to `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`.
The scrubber declarations remain package-stable as `com.eterocell.rhythhaus.ui` despite feature
module ownership; the `com.eterocell.rhythhaus.nowplaying` declarations also remain package-
stable. All observable UI and interaction behavior is preserved. `:shared` retains the public
compatibility facade
`NowPlayingScreen(track, playbackState, playbackController, tagLibReader, currentLibraryTrack, onBack, modifier)`.
Its currently unused `TagLibReader` and `LibraryTrack?` parameters remain compatibility-only;
the facade delegates to distinctly named feature-owned `NowPlayingContent` with only real
dependencies. The feature does not depend on `:shared`, `:taglib`, or Library
implementation/API solely for those unused parameters.

`NowPlayingAdaptiveLayoutMode` and `nowPlayingAdaptiveLayoutModeFor` move from shared Library
navigation into the feature. Shared retains all route state, `LibraryBackTarget.NowPlaying`,
Back precedence/arbitration, predictive Back, expansion state and animation, shell
measurement/visibility, route dispatch, Search/Settings navigation callbacks, and the shell
overlay. Shared owns the `Animatable`, collapse callback/Back, and navigation effects.
`NowPlayingExpandOverlay` remains shared-owned for shell orchestration. `LibraryAppShell.kt`
remains the composition boundary and consumes feature entry points. The feature receives the
shared-owned progress object only for display and gesture mutation. Feature-local `NowPlayingBar`
uses an upward-only adapter and emits only `onExpand`; it cannot invoke collapse, Back, or
navigation. Shared-local `NowPlayingExpandOverlay` uses a downward-only adapter and emits only
the existing shared collapse/Back callback; it cannot route expansion. The feature owns the
expanded content's left-edge swipe and emits only `onBack`.

The stateless drag, rubber-band, spring, and threshold mechanism moves package-stably to
`:core:ui` rather than being duplicated. It accepts direction and terminal callback from these
local adapters, preserves exact existing threshold/rubber-band/spring behavior, and exposes no
domain-navigation API. `verticalSheetGesture` does not remain shared-only. `SwipeBackGesture.kt`
(`leftEdgeSwipeBack`) also moves package-stably to `:core:ui` because Library and Now Playing
use it; its behavior remains generic and callbacks remain caller-owned. `LiquidGlassChrome.kt`
moves package-stably to `:core:ui` for the same shared use. Core UI exposes the opaque public
`RhythHausBackdrop` handle, behavioral-KDoc public `rememberRhythHausBackdrop`,
`recordRhythHausBackdrop`, and `rhythHausLiquidGlass`, plus public
`RhythHausGlassSurfaceAlpha`.
Miuix `LayerBackdrop` storage remains internal to core UI; neither feature nor shared public
signatures expose it. Miuix remains an implementation dependency of core UI. This Task 5.1 slice
atomically includes these reusable core UI moves because the feature otherwise cannot obey
dependency direction; it does not move feature-specific state into core.

Under explicit API, every declaration has explicit visibility and every public declaration below
has declaration-specific behavioral KDoc. The feature's public surface is public composables
`NowPlayingContent` and `NowPlayingBar`; public `BottomBarMode` and `bottomBarModeFor`; public
immutable `NowPlayingScreenLabels` and `NowPlayingBarLabels`; and public
`NowPlayingAdaptiveLayoutMode` and `nowPlayingAdaptiveLayoutModeFor`. Test tags, padding
constants, `UiState`, helper functions, lazy artwork state, gesture adapters, and implementation
details are internal or private as applicable. `MusicProgressScrubber` and its support helpers
remain internal in the package-stable `com.eterocell.rhythhaus.ui` package.

The complete public Task 5.1 `:core:ui` surface has explicit public visibility and
declaration-specific behavioral KDoc: `ArtworkImageRole`; `ArtworkImage`; opaque
`RhythHausBackdrop`; `rememberRhythHausBackdrop(): RhythHausBackdrop?`; functions accepting a
backdrop accept `RhythHausBackdrop?`, so unavailable render effects are represented by `null`;
`recordRhythHausBackdrop`; `rhythHausLiquidGlass`; generic `leftEdgeSwipeBack`; the stateless
vertical-drag mechanism entry point `verticalSheetGesture`; and its public
`VerticalSheetGestureDirection` direction type. The only public visual constant is
`RhythHausGlassSurfaceAlpha` for existing shared/feature callers; blur and refraction
implementation constants remain internal throughout Task 5.1. Miuix
`LayerBackdrop` remains internal and absent from every public signature.

`NowPlayingArtworkBridge`, `PlaybackEngine.ios.kt`, all iOS Now Playing metadata/bridge tests,
and Swift artwork-provider/bootstrap registration remain in `:core:playback` and `iosApp`.
Task 5.1 does not move, duplicate, rename, or alter them. There is no
`:feature:nowplaying` iOS framework export: Swift continues to use the sole Shared framework,
`MainViewController`, and playback bridge exports. No feature-specific `iosMain` source is
created in Task 5.1; the feature remains unexported.

The module uses the feature implementation KMP convention with controlled Android-KMP and
Compose compiler/resources conventions, targeting exactly Android-KMP, JVM, `iosArm64`, and
`iosSimulatorArm64`. Its direct project dependencies are exactly `api(:core:playback)` and
`api(:core:ui)` because their public types appear in the feature surface. `Track` is available
through the approved playback public surface. The feature has no direct `:core:model`, `:shared`,
`:taglib`, Library API, or Library implementation dependency. Its Android namespace is exactly
`com.eterocell.rhythhaus.nowplaying`, and its Compose resource package is exactly
`rhythhaus.feature.nowplaying.generated.resources`. It exposes a narrow public feature entry
surface. Feature-owned lazy artwork loading state calls an injected
`suspend (String) -> ByteArray?` contract. Shared adapts the Library repository's
`TrackArtwork?` result to bytes and passes that loader through the shared facade/composition.
The feature uses public `:core:ui` `ArtworkImage` and `ArtworkImageRole`; it imports neither
`TrackArtwork`, Library API/implementation, shared `LocalTrackArtworkLoader`, nor shared
`LazyTrackArtworkImage`. Shared retains shared-owned/reused resource keys and resolves them
composably in the shared composition for the current track, passing plain immutable `String`
values, never `Res` handles or resource types, to two narrow feature value objects:
`NowPlayingScreenLabels(play, pause, albumArtwork, currentTrackArtistAlbum)` and
`NowPlayingBarLabels(play, pause, search, settings, albumArt, currentTrackArtistAlbum)`.
`track_artist_album_format` remains shared-owned and shared passes its formatted output;
`album_artwork` also remains shared-owned because Library uses it. Feature-owned EN/ZH keys move
to feature Compose resources and namespace: `mini_player_empty_subtitle`, `next_track`,
`previous_track`, `playback_status_buffering`, `playback_status_error`,
`playback_status_loading`, `playback_status_paused`, `playback_status_playing`,
`playback_status_ready`, `playback_status_stopped`, `repeat_mode_repeat_one`,
`repeat_mode_repeat_playlist`, `repeat_mode_stop_after_current`,
`repeat_mode_stop_after_queue`, `shuffle_off`, `shuffle_on`, and `track_number_format`.
Existing shared keys remain for other consumers without duplication or removal. Resource
behavior, Android packaging, desktop resolution, and iOS linking are verified.

Mixed tests split by ownership. Feature tests move or recreate Now Playing-specific
`bottomBarModeFor` and `NowPlayingBar` semantics/interaction coverage; shared retains Library
route/Back, playlist edit-mode integration, and shell composition assertions. Characterization
preserves empty and loaded modes; disabled or unmeasured interaction; play/pause, expand,
Search, and Settings actions; progress, status, repeat, and shuffle presentation; Back/swipe
callback identity; artwork behavior; and the adaptive-layout policy tests currently embedded in
`LibraryNavigationTest`, which move or are recreated under feature ownership without weakening
existing assertions. Architecture
policy adds `:shared -> :feature:nowplaying`, the approved feature-to-core edges, package and
resource ownership, and fixtures rejecting feature-to-shared or feature-implementation edges
and iOS export. Package ownership explicitly permits `:feature:nowplaying` declarations in both
`com.eterocell.rhythhaus.nowplaying` and the package-stable
`com.eterocell.rhythhaus.ui` scrubber declarations moved in this slice; no package is renamed.
It adds no broad export or dependency allow-list. This is an ownership extraction with unchanged
runtime behavior; platform compile/link/resource and Swift-consumer evidence do not claim runtime
UI or playback validation.

Rejected alternatives are moving the exact unused parameters or dependencies into the feature,
introducing speculative adapter contracts, moving the iOS lock-screen bridge, giving the feature
root Back/navigation ownership, and exporting the feature to iOS.

Stateful screens use immutable `UiState`, `UiEvent`, and `UiEffect`, coordinated by a Presenter or ViewModel. Stateless UI does not receive empty pattern types. The data flow is:

```text
UI -> Event -> Presenter -> UseCase -> Repository -> DataSource
```

DTOs, database entities, and domain types remain distinct, with mapping close to the boundary that introduces the representation.

A feature implementation publishes a Koin `Module` only when it owns injectable bindings.
UI-only modules are composed through callable/composable entry points and do not create empty
modules. Only `:shared` assembles and starts DI; no service locator back-reference is permitted.

## Back And Navigation Invariants

The existing Back contract is preserved exactly. One intent performs one transition in this order: modal, edit mode, active-page selection, Now Playing, then route. Only the active destination participates. Predictive Back latches the exact destination and target. A feature publishes exactly one already-resolved foremost immutable dismissal surface, modal before edit, with stable identity per appearance and a new identity per re-presentation. Shared owns destination identity, mapping, and registration; only the active destination is accepted, and stale registrations/disposers cannot replace or clear active state. Cancellation does not dismiss. Completion revalidates the exact destination and appearance and dispatches at most once. Rejection or staleness clears the pending session with no same-intent fallthrough.

A dispatched non-predictive transition remains in flight until authoritative state reports that exact latched target inactive or the target explicitly rejects completion. Repeated Back is suppressed while it remains in flight; callback return alone never settles or releases suppression. Explicit rejection releases the in-flight intent without treating the target as settled, and any later Back is a new intent. Deleting a displayed playlist is authoritative exact-destination invalidation after confirmed absence, never Back; failed, stale, or replayed deletion does not invalidate and unrelated state is preserved. The root shell remains in `:shared`; introduce the smallest common navigation contract only if destination-scoped Back behavior requires one.

## Database, Resources, And iOS

During Task 1.3, `:shared` was the transitional physical SQLDelight owner while only Gradle application/configuration moved into a dedicated build-logic convention. Accepted Task 3.1 transferred the configured database/plugin ownership, true-layout `.sq`/`.sqm`/schema artifacts, drivers, and generated package atomically to `:core:database`, which is now the sole physical SQLDelight owner. The Task 1.3 convention change preserved the existing database configuration, schema, migrations, package, database name, and platform driver behavior. Runtime/coroutine consumers, README text, and arbitrary filenames do not identify an owner.

Resources move with their feature through recognized KMP/Compose source-set locations; a namespace is enforced only when exposed by the public module model. Each migration verifies Android packaging, desktop runtime resolution, and iOS linking. iOS exports only modules whose declarations enter the Swift/Objective-C public API; broad exports are forbidden. The existing shared framework entry remains stable.

### Approved Task 5.2 Playlist Implementation Boundary

New unexported `:feature:playlists:impl` targets Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`. It owns saved-playlist and playback-queue UI; immutable Playlist state/actions/reducer/owner, preserving the already-characterized immutable `PlaylistState`, `PlaylistStateAction`, reducer, `PlaylistStateOwner`, and backup immutable state/reducer as the Task 5.2-specific accepted equivalent without a new Presenter/ViewModel/`UiEffect`/Event scaffold; repository implementations/Koin binding; backup codec/service/state/UI; the neutral common document-launcher contract; public Android/JVM launcher factories; and feature resources. Feature owns no iOS actual/source and has no Shared edge. Shared owns the common `expect`, Android/JVM delegates, and retained iOS actual/ABI adapter. The API remains a clean repository/model contract. Its direct allowed project edges are playlists API, Library API, core model/playback/ui/platform, and implementation-only core database. Other Gradle `api`/`implementation` scopes are derived later from exhaustive public signatures. Generated DB/SQLDelight/generated `Res`/shared navigation or shell types are forbidden in public signatures. Koin is forbidden elsewhere in the public surface; the sole exception is the documented public binding-module factory returning `org.koin.core.module.Module` for shared assembly. Public implementation declarations are limited to that Koin factory, shared-needed state/action/result types, owner, composable entries, dismissal contracts, backup orchestration contracts, and launcher seam, each with declaration-specific behavioral KDoc; all other helpers are internal/private. Governance covers the preserved roots `com.eterocell.rhythhaus.library` and `com.eterocell.rhythhaus.playlistbackup`.

Task 5.2 moves adapters only. `:core:database` remains sole physical owner: no `.sq`, `.sqm`, schema, migration, generated DB, driver, database-name, or FK change. Serialization/revision/cancellation invariants, backup operation exclusivity, exact 4 MiB limits, mappings, stale-library rejection, transactional import, and exactly-once native completion remain intact. Moving queue UI does not move playback engine/session/lifecycle/root playback state.

Shared retains app composition, shell/routes/Back, lifecycle, Koin assembly/start, and Settings layout. The feature owns embeddable backup sections/dialogs and all playlist/queue/backup EN/ZH text once. Shared injects generic `cancel` and composes the feature-owned add-to-playlist plain `String` into the selection bar; no duplicate keys or generated resource handles cross the boundary. The thin shared iOS facade retains the exact Shared framework ABI in the executable ledger below. Shared adapts to the Kotlin-only feature seam; the feature is not exported and Swift app files remain app-owned.

Both `InMemoryPlaylistRepository` and `SqlDelightPlaylistRepository` move to feature implementation ownership; neither implementation is exposed through Shared. `feature/playlists/api/.../PlaylistRepository.kt` remains the public repository contract. Retained Shared tests use that contract plus public feature state ports, never implementation classes or feature-private helpers. `LibrarySourceManagementTest` uses a test-local recording `PlaylistRepository` and `PlaylistStateOwner.refresh()` rather than `loadPlaylistSnapshot`; lifecycle, DI singleton/factory, source-management, and queue/playlist reconciliation behavior remain covered. Shared retains an internal `authoritativePlaylistBackupRevisionGuard(owner: AuthoritativeLibraryPublicationOwner): PlaylistBackupRevisionGuard` factory. Its common-test adapter suite verifies delegation of controller confirmation blocks through `AuthoritativeLibraryPublicationOwner.withCurrentRevision` for current and stale revisions and exact `CancellationException` rethrow.

#### Shared iOS ABI Ledger

The following ABI is literal and must remain in package `com.eterocell.rhythhaus.playlistbackup` in framework `Shared`:

```kotlin
object IOSPlaylistBackupDocumentStatus {
    const val SUCCESS = 0
    const val CANCELLED = 1
    const val TOO_LARGE = 2
    const val FAILURE = 3
    const val UNAVAILABLE = 4
}

interface IOSPlaylistBackupDocumentCompletion {
    fun complete(status: Int, bytes: ByteArray?, message: String?)
}

interface IOSPlaylistBackupDocumentProvider {
    fun saveDocument(
        fileName: String,
        bytes: ByteArray,
        completion: IOSPlaylistBackupDocumentCompletion,
    )

    fun openDocument(
        maxBytes: Int,
        completion: IOSPlaylistBackupDocumentCompletion,
    )
}

object IOSPlaylistBackupDocumentBridge {
    var provider: IOSPlaylistBackupDocumentProvider?
}

const val PlaylistBackupMimeType = "application/vnd.rhythhaus.playlists+json"
const val PlaylistBackupMaxBytes = 4 * 1024 * 1024 // 4,194,304
```

Swift retains singleton access as `IOSPlaylistBackupDocumentBridge.shared.provider`. The top-level constants remain exported through `PlatformPlaylistBackupDocumentsKt.PlaylistBackupMimeType` and `PlatformPlaylistBackupDocumentsKt.PlaylistBackupMaxBytes`. Swift consumers continue to receive Kotlin `Int` as `Int32` where current interop does, notably `openDocument(maxBytes: Int32, ...)`. This ledger is the canonical exact comparison target; it does not authorize Swift source redesign.

### Approved Task 5.3 Search Implementation Boundary

Task 5.3 creates exactly one unexported implementation module, `:feature:search`, with no API split, Koin module, platform source, repository, presenter/state/event/effect scaffolding, or iOS framework export. It targets Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`, has one common implementation, preserves package and Android namespace `com.eterocell.rhythhaus.search`, and uses resource namespace `rhythhaus.feature.search.generated.resources`. `feature/search/README.md` is explicitly out of scope.

Its exact public surface is only explicit-public, declaration-specific-KDoc `SearchSharedLabels` and `SearchContent`:

```kotlin
public data class SearchSharedLabels(
    public val title: String,
    public val clear: String,
    public val nowPlaying: String,
)

@Composable
public fun SearchContent(
    libraryTracks: List<LibraryTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    labels: SearchSharedLabels,
    selectTrackLabel: @Composable (String) -> String,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    onStartSelection: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    onPlayTrack: (orderedResults: List<LibraryTrack>, selectedTrack: LibraryTrack) -> Unit,
    onDismiss: () -> Unit,
    playingIndicator: @Composable () -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
)
```

`SearchSharedLabels` is value-equal and receives Shared-localized title, clear, and Now Playing wording. The callback-first contract carries primitive playback state, selection/visible-ID/scroll/play/dismiss callbacks, a composable label formatter that resolves `select_track_format` through structured Compose `stringResource` while a row is composed, an indicator slot, and layout defaults only. No generated resource handle crosses the boundary. It has no Shared/generated `Res`/playback controller or state/repository/Koin/platform/database/TagLib/queue type. `LibraryTrack` is from Library API; `api` is limited to Library API plus public Compose runtime/UI requirements. Core UI, Foundation, resources, and Miuix are implementation-only. Shared declares exactly `implementation(projects.feature.search)`, never `api`, and does not export Search. There is no core playback, Shared, database, platform, taglib, another implementation, Koin, or iOS-export edge.

Shared is the sole facade and composition owner. `LibraryRoutes` directly composes `SearchContent`, deletes Shared compatibility `SearchScreen`, and removes the unused `TagLibReader` route input. Shared retains route/Back, selection state/reconciliation/clear, scroll storage, playback queue/restart/dismiss policy, bottom-bar/Now Playing policy, and `EqualizerStrip`; Search owns query/filter/render/focus/count/empty presentation and Search row interaction. Blank or whitespace query has no results; nonblank matching is case-insensitive over title, artist, and album and preserves order, duplicate IDs, and empty metadata. Search uses an internal, non-public LazyColumn occurrence identity of filtered occurrence index plus track ID, never `track.id` alone; it is rendering-only and cannot alter `LibraryTrack`, selection IDs, visible-ID sequence, playback queue order, or duplicate semantics. Search focuses once, clear resets empty, emits visible IDs only on sequence changes and primitive scroll position, requests ordered-result playback outside selection, starts selection on long press without playback, and toggles once without playback for selection row/checkbox activation. It invokes the supplied indicator only for a current playing row, renders no artwork/error state, and delegates dismissal/Back.

Search owns exactly `search_placeholder`, `search_results_count_zero`, `search_results_count_one`, `search_results_count_many`, and `search_no_tracks_match_format` in English and Chinese. Shared retains/injects `search`, `clear`, `now_playing_badge`, and a composable `select_track_format` formatter as values. No duplicate key or generated resource handle crosses the boundary. Feature production-composable tests cover Search behavior, including the four moved mixed-suite cases and two equal-ID occurrences that render/activate distinctly, retain keys across unrelated recomposition, and preserve duplicate ordered visible/playback callbacks. Shared retains Home browse selection-clear, real route composition, Back/route, selection reducer/clear, playback adapter queue-order/current-track-restart/dismiss/callback-failure ownership, scroll, and Now Playing policy tests. RED/GREEN rejects an absent `:feature:search` module/target before registration with failure caused solely by absence; feature-to-Shared/core-playback/database/core-platform/taglib/another-implementation/app edges; Koin; iOS export; a Shared `api` or exported Search edge; wrong package/Android/resource namespace; a wrong Search resource-ownership control where a moved key is missing, duplicated, or owned by the wrong module, distinct from wrong namespace and generated-handle controls; resource duplicates/generated handles; and missing public KDoc/public-surface closure. Evidence includes cross-platform, architecture, quality, strict named OpenSpec, Xcode, and `./init.sh`, but no runtime/device/visual/accessibility/playback-engine/desktop-launch/iOS-runtime-resource claim. See [the approved Search design](2026-08-07-search-feature-extraction-design.md); detailed paths and commands remain with the later executable plan.

## Migration Strategy

0. Reconcile and verify prior changes before implementation. `architecture-refactor` is 12/12 complete. Package organization implementation exists in commits `f0310e5`, `06f8a16`, and `adb1e3d` despite stale 0/5 tracking; do not redo its package moves.
1. Establish the governance baseline using failing Gradle TestKit architecture tests before convention plugins and executable gates.
2. Extract core model and core UI.
3. Atomically extract the database and narrow platform capabilities.
4. Create library/playlists APIs and atomically extract core playback ownership as Task 4.2.
5. Extract leaf implementations in order: Now Playing, playlists/backup, Search, then Settings.
6. Extract Library last, separating app shell composition from feature ownership.
7. Finish thin shared cleanup and add a feature scaffold only after successful feature migrations. The scaffold generates real structure only, never empty pattern classes. Package renames remain separate.

## Governance And Gates

Canonical architecture documentation is `skills/kmp-architecture/SKILL.md` and `docs/architecture.md`; ADRs record boundary and shared/iOS export decisions. Feature READMEs explain local ownership. `AGENTS.md` later links to these documents rather than duplicating them.

Build logic provides convention plugins for core, feature API, feature implementation, Android,
Compose Resources, and SQLDelight ownership. Shared build logic owns a normalized immutable
`ArchitectureModelRegistry`: controlled conventions apply plugins on their compatible
classpaths and publish public API facts; root `architectureCheck` consumes only its records
plus existing model inputs. Android records main-production static roots through public
Android Components callbacks, while module namespace comes from concrete public Android DSL
`CommonExtension.namespace` (`ApplicationExtension` or `LibraryExtension`), never an AGP 9.3
`Variant`. It excludes test/`androidTest`/test-fixture variants, casts, reflection,
task/artifact internals, and per-variant namespace claims.
Compose records public standard roots, explicit project-owned declared custom roots, and its
convention-declared configured namespace, so the root never reads nested
`ResourcesExtension` or internal maps. A blank declaration remains an invalid registry fact for
deterministic `ARCH-RESOURCE` diagnostics and is not passed to Compose Resources. The
SQLDelight convention retains `app.cash.sqldelight:gradle-plugin:2.3.2` on build-logic
`implementation`, applies/configures SQLDelight, and publishes typed public model facts while
preserving the accepted `:core:database` physical ownership (with `:shared` only the
historical transitional owner during Task 1.3). The root JVM `:architecture-processor` is not the
convention-plugin JAR. Core/API conventions record KSP only after applying it and registering
the real production consumer; generic `ksp` is tooling only for single-platform JVM/Android,
never an inferred/spoofed name. The processor receives normalized module/root arguments,
processes compilation-local initial inputs only, excludes generated/test/local boundaries as
supported, and emits sorted deduplicated relative paths; it does not aggregate a project.
Fixtures cover the exact Android, Compose, KSP, declaration, diagnostic, and cache cases in
the stable checker design. No reflection, internals, classloader probing, build-script
parsing, KSP-output consumption, or Task 1.4 entrypoint wiring is permitted.

Android application conventions publish exact module/project and configuration identities only
from public AGP 9.3.1 `ApplicationVariant` test-component
`Component.compileConfiguration`/`Component.runtimeConfiguration`. Android-KMP library
conventions publish them only from public `KotlinMultiplatformAndroidLibraryTarget.compilations`
and public `KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName`/`runtimeDependencyConfigurationName`. After normalization,
root collection suppresses a direct self `ProjectDependency` only when that identity exactly
matches and the configuration contains exactly one distinguishable direct self record; otherwise
it suppresses none.
`Configuration.isCanBeDeclared` is not a predicate because it supplies no dependency provenance.
Authored dependencies in supported declarable buckets, including explicit self edges, remain
checked and emit `ARCH-EDGE` plus one-node `ARCH-CYCLE`. An equal authored mutation on the exact
AGP-owned configuration may collapse into its set record and is unsupported/outside the checker
guarantee because no public provenance exists. No name inference, blanket test filtering,
attribute guessing, reflection, AGP internals, or task/artifact/output/resolved-classpath
inspection is permitted. The Android RED fixture captures the known three synthetic self edges
and cycle; GREEN removes only those records while retaining canonical main resource records,
the authored self-edge negative control, and the fail-closed cardinality control.

## Testing And Acceptance

Every task starts with a characterization or architecture RED test, makes the minimal move or implementation, then runs focused GREEN checks followed by architecture, Detekt, and Spotless checks. Run full `./init.sh` for graph, expect/actual, SQLDelight, resource changes, and final validation. Update `progress.md`, `roadmap.md`, and relevant ADRs during implementation. Make conventional commits per independently reviewable migration slice.

Task 5.2/OpenSpec 6.2 is accepted and completed by implementation commit `fc1b96f858408c8dfd07221d5fe85ae3e20ced63` and evidence closeout `6e885ef75ada0d6e48b2832cb3852b460a6c62ed`. Its retained evidence does not claim runtime/device/visual, picker, or playback behavior from compile/link/tests.

Task 6.3 remains unchecked until the complete Task 5.3 Search module, public boundary, dependencies, ownership, behavior, resources, test split, RED/GREEN controls, and bounded cross-platform/architecture/quality/strict-OpenSpec/Xcode/`./init.sh` evidence are accepted. The later executable plan owns detailed exact paths and commands.

Acceptance requires actual dependency-graph and TestKit illegal-fixture coverage; a thin shared inventory; explicit public APIs; Back regressions; SQLDelight migration/integration verification; Android, desktop, and iOS startup/resource/DI coverage plus key playback and scanning paths; `qualityCheck`; `./init.sh`; strict OpenSpec validation; and `git diff --check`. Documentation and trackers must match the evidence.

## Risks And Mitigations

- Current `App.kt` composition coupling and `LibraryAppState` shell/feature split: characterize before extraction and preserve shell ownership in shared.
- Centralized DI and feature UI internal imports: move contracts first, enforce graph checks, and let shared assemble implementation modules only.
- SQLDelight cross-feature FKs, resources, expect/actual declarations, and Swift exports: use atomic database moves and platform-specific verification in the affected slice.
- Stale OpenSpec tracking: reconcile the known architecture/package changes before migration work.
- Illegal bridge dependencies: prohibit them; leave a failed atomic slice incomplete rather than weakening the graph.

## Approved Task 5.2 Non-Goals

No visual/product redesign, state-framework rewrite, navigation/core-navigation, generic document module, package rename, database change, playback ownership change, illegal bridge/service locator/implementation coupling, feature export, Swift redesign, resource duplication, or runtime/device claim from compile/link/tests. ADR 0002 already matches and remains untouched.
