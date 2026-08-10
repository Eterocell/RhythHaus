# Task 7.1-7.3 Library Feature Extraction Design

**Route:** OpenSpec + Superpowers. This amendment is the literal authority; it is not
an implementation plan. Tasks 7.1-7.3 remain unchecked.

## Graph, API, and Ownership

Create exactly one unexported `:feature:library:impl` beside `:feature:library:api`.
Shared uses `commonMainImplementation(feature:library:impl)` and retains
`api(feature:library:api)`; impl has no iOS/framework export, Shared/app/core-playback/
other-feature-impl edge. Impl `api` is Library API, `core:model`, `core:ui`, Compose artifacts
required by public declarations, and exactly `api(libs.koin.core)` because
`libraryImplementationModule(): Module` exposes Koin `Module`; its `implementation` is core database,
core platform where actually used, TagLib, coroutines, Koin Compose and every other Koin/UI dependency
absent from public signatures. Shared's impl edge is `implementation` only.

Module identity and Library repository/domain contracts remain. Slice 6 intentionally
changes Library API only by deleting `LibraryTrack.toPlayableTrack()`, its `PlayableTrack`
import/dependency, and API playback types; conversion remains Shared. This supersedes the
historical four-files-byte/API-unchanged statement for that declaration only.

### Shared Playback Conversion Amendment

At the approved baseline, the six live production `LibraryTrack`-to-`PlayableTrack` call sites
are Shared `App.kt`, `LibraryAppShell.kt`, moving `LibraryHomeContent.kt`, `LibraryRoutes.kt`,
and `PlaybackSessionCoordinator.kt`, plus playlist impl `PlaylistScreens.kt`. `PlayableTrack` is a
`:core:model` type, not a `:core:playback` type. After extraction, every conversion is retained in
Shared adapters: moving leaf UI reports `Track` callback data and Shared converts it. Library API
continues to depend only on `:core:model` and exposes neither `PlayableTrack` nor a conversion.

For saved-playlist detail, Shared `LibraryRoutes.kt` creates and supplies
`playableTracksById: Map<String, PlayableTrack>` from authoritative library tracks. The exact
`PlaylistDetailScreen` input replaces `libraryTracks: List<LibraryTrack>` with that map; the screen
consumes it directly and contains no `LibraryTrack` mapper. The map is keyed by track ID and has
the current `associate` duplicate-key behavior; it does not preserve duplicate keys. Duplicate
playlist occurrence identity and order remain exclusively in unchanged
`SavedPlaylistPlaybackRequest(occurrences, selectedOccurrenceId)` and unchanged `onPlayEntry`.
Playlist browser overlays may continue to receive `List<LibraryTrack>` for metadata. This requires
no new project edge: `:feature:playlists:impl` retains its existing `api(projects.core.model)`
playback-model visibility and has no feature-impl-to-Shared edge or callback-payload redesign.

Causal controls remove Library API conversion assertions; Shared controls prove exact field and
artwork-byte projection, detail receipt of the Shared projection, occurrence order/selected
occurrence preservation, and callback failure/settlement preservation. Feature controls prove
playlist detail performs no internal `LibraryTrack` mapping. Existing Shared route-adapter and
playlist feature test endpoints are adapted; this authority intentionally assigns no new filename.

Impl owns repository implementations, scanner, metadata/TagLib, platform source/picker/path
actuals, leaf UI/resources, internal grouping/formatting (`formatDuration`), lazy artwork,
collapse/chrome, and row gesture state. No Presenter/ViewModel/Event/Effect is added. Shared
retains App/root shell; route/Back/predictive/navigation identities; `LibraryAppState`; browse
state; selected/current IDs; selection reducer/page mapping; visible-ID reconciliation; scroll,
Now Playing/bottom-bar measurement/clearance; playback/session; scan Job/orchestration/publication;
cross-feature composition; Koin total assembly; and `LibraryPlaybackSelection.kt`.

`RhythHausBackdrop` is an accepted stable public `core:ui` type: Shared owns the root backdrop
instance and Home records it; Detail owns its local backdrop. Generated-resource types never cross.

## Public Signature Authority

### Oracle Conversion Inventory Correction

The six paths named above are historical/baseline inventory only. Post-extraction exactly four
retained Shared production files own conversion/projection: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`. Moving feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and contains no conversion. Task 7 deletes only `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt` method/import residue, adapts/removes conversion assertions in `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryApiModelsTest.kt`, and adapts `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection. Retain unrelated `Track.toPlayableTrack()` in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt` and its `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt` use.

This is an exact public signature authority: declarations, types, defaults, and parameter names are
binding pseudocode, not compilable production source or its inline KDoc. The only public defaults are exactly those below: `ScanProgress`,
the scanner callbacks, and `Failure.cause`. Every parameter of both composables is required and
has no default.

```kotlin
public interface PlatformSourceAccess {
    /** Returns the caller's current authority to use this source. */
    public fun accessStatus(source: LibrarySource): LibrarySourceAccessStatus
    /** Releases platform authority held for this source. */
    public fun releaseAccess(source: LibrarySource)
}

/** Reports the current scan session and most recently processed display item. */
public data class ScanProgress(
    /** Session whose status determines active progress. */
    public val session: ScanSession? = null,
    /** Latest processed item, if one has been reported. */
    public val latestItem: String? = null,
) {
    /** Is true exactly while session status is Scanning or Cancelling. */
    public val isActive: Boolean
}

/** Retains a prior matching-handle source ID and creation timestamp, else returns picked unchanged. */
public fun normalizePickedSource(
    pickedSource: LibrarySource,
    existingSources: List<LibrarySource>,
): LibrarySource
// First source matching handle contributes id and createdAtEpochMillis; otherwise pickedSource unchanged.

public class LibraryScanner internal constructor(
    /** Repository receiving source, session, error, and track publications. */
    private val repository: LibraryRepository,
    /** Internal event producer for this platform source. */
    private val platformScanner: PlatformAudioScanner,
    /** Internal metadata enrichment reader. */
    private val metadataReader: AudioMetadataReader,
    /** Current epoch-millis provider. */
    private val now: () -> Long,
    /** Prefix-aware identifier factory. */
    private val idFactory: (String) -> String,
) {
    /** Scans one source with polling, terminal publication, cleanup, and throwable mapping. */
    public fun scan(
        source: LibrarySource,
        isCancelled: () -> Boolean = { false },
        onProgress: (ScanProgress) -> Unit = {},
    ): ScanSession
}

public sealed interface PlatformFolderPickResult {
    public data class Success(public val source: LibrarySource) : PlatformFolderPickResult
    public data class Unavailable(public val message: String) : PlatformFolderPickResult
    public data class Failure(public val message: String, public val cause: String? = null) : PlatformFolderPickResult
}
public interface PlatformFolderPickerLauncher {
    public val isAvailable: Boolean
    public val supportsAdditionalSources: Boolean
    public fun launch()
}
@Composable public expect fun rememberPlatformFolderPickerLauncher(
    onResult: (PlatformFolderPickResult) -> Unit,
): PlatformFolderPickerLauncher

/** Selects internally grouped album, artist, or authoritative song rendering. */
public enum class BrowseMode { Albums, Artists, Songs }
/** Identifies the Shared-owned selection projection destination. */
public sealed interface LibrarySelectionPage {
    /** Shared songs-home selection page. */
    public data object HomeSongs : LibrarySelectionPage
    /** Exact album-name selection page. */
    public data class Album(public val name: String) : LibrarySelectionPage
    /** Exact artist-name selection page. */
    public data class Artist(public val name: String) : LibrarySelectionPage
}
/** Raw route data whose feature-owned rendering selects localized detail wording. */
public sealed interface LibraryDetailSummary {
    /** Album counts and optional raw artist; null resolves to feature unknown_artist. */
    public data class Album(public val trackCount: Int, public val artist: String?) : LibraryDetailSummary
    /** Artist counts passed unchanged to feature-owned localized subtitle formatting. */
    public data class Artist(public val albumCount: Int, public val trackCount: Int) : LibraryDetailSummary
}
public data class LibrarySharedLabels(
    /** Import-action label. */
    public val addMusicFolder: String, public val folderPickerUnavailable: String,
    public val clearLibrary: String, public val cancel: String, public val playlists: String,
    public val playlistsAccessibility: String, public val libraryQueue: String,
    public val albumArt: String, public val albumArtwork: String, public val nowPlayingBadge: String,
    public val selectTrack: @Composable (String) -> String,
    public val trackArtistAlbum: @Composable (String, String) -> String,
)
@Composable public fun LibraryHomeContent(
    title: String, subtitle: String, tracks: List<Track>, browseMode: BrowseMode,
    folderPickerLauncher: PlatformFolderPickerLauncher, sourcePickerActionVisible: Boolean,
    importMessage: String?, scanProgress: ScanProgress?, mutationsEnabled: Boolean,
    currentTrackId: String?, selectionModeActive: Boolean, selectedTrackIds: Set<String>,
    labels: LibrarySharedLabels, homeBackdrop: RhythHausBackdrop?,
    artworkLoader: suspend (trackId: String) -> ByteArray?,
    onBrowseModeChange: (BrowseMode) -> Unit, onClearLibrary: () -> Unit,
    onCancelScan: () -> Unit, onOpenAlbum: (String) -> Unit, onOpenArtist: (String) -> Unit,
    onShowPlaylists: () -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit, onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
)
/** Renders an already-resolved detail destination using only raw feature inputs and callbacks. */
@Composable public fun DrillDownView(
    title: String, summary: LibraryDetailSummary, tracks: List<Track>, topBarArtworkTrack: Track?,
    currentTrackId: String?, selectionPage: LibrarySelectionPage, selectionModeActive: Boolean,
    selectedTrackIds: Set<String>, labels: LibrarySharedLabels,
    artworkLoader: suspend (trackId: String) -> ByteArray?, onBack: () -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit, onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
)
public fun libraryImplementationModule(): Module
```

Production source MUST add declaration-specific behavioral KDoc immediately to every listed public
top-level declaration, type, member, constructor property, and function. Kotlin does not attach KDoc
to individual value parameters: every public callable parameter and callback MUST therefore be
documented by that declaration's KDoc `@param` entries. The KDoc ledger covers ScanProgress active
statuses; normalization identity/timestamp; source access/release; scanner polling/terminal mapping;
picker results/launcher; browse/selection/detail variants; every label field; each composable input
and callback; and the Koin factory. Generic placeholder documentation is invalid. Architecture KSP
controls inspect production source, not comments in this signature authority.

```kotlin
internal fun interface PlatformAudioScanner { fun scan(source: LibrarySource): Sequence<PlatformScanEvent> }
internal sealed interface PlatformScanEvent {
    data class FolderVisited(val displayPath: String) : PlatformScanEvent
    data class AudioCandidate(val candidate: AudioScanCandidate) : PlatformScanEvent
    data class Skipped(val sourceLocalKey: String, val displayPath: String, val reason: String, val recoverable: Boolean) : PlatformScanEvent
}
internal interface PlatformSourceAccessAndScanner : PlatformSourceAccess, PlatformAudioScanner
internal expect fun createPlatformSourceAccess(): PlatformSourceAccessAndScanner
internal class AudioMetadataReader(
    private val tagLibReader: TagLibReader = createTagLibReader(),
    private val platformMetadataReader: (AudioSource) -> AudioMetadata? = ::readPlatformAudioMetadata,
) { internal fun read(source: AudioSource): AudioMetadata? }
```

`AudioMetadataReader` moves byte-for-byte in behavior to internal impl package visibility: TagLib
first for path/descriptor, platform URI, fallback merge, and platform expect. Public Shared/header
exposure is retired. The public scanner's internal constructor has exactly the displayed repository,
internal scanner, internal reader, clock, and ID-factory types.

`PlatformSourceAccess` does not expose internal `PlatformAudioScanner`. In
`com.eterocell.rhythhaus.library.impl`, internal `PlatformAudioScanner` and
`AudioMetadataReader` make the constructor valid; one internal concrete platform class implements
both roles and is bound as both from the same singleton. Scanner polls before each platform event
and after traversal, publishes Cancelled terminal state/progress and cleanup, rethrows
`CancellationException`, and maps other throwables to Failed. Android picker preserves null-URI
no-callback behavior.

## Literal UI Behavior and State

`Track` is core model. `LibrarySnapshot`, `LibraryTrack`, groups, controller/state, Job, Shared
route/selection/scroll types, and generated resources never cross. `currentTrackId` is the only
playback presentation primitive: rows show current/Now Playing by ID equality; neither leaf renders
`isPlaying`. `onPlayPause` is absent. `play`/`pause` remain Shared-only Now Playing/bottom-bar keys.

`tracks` is Shared's authoritative display/playback sequence. Songs and Home playback retain its
duplicate-preserving order. Detail receives the resolved ordered detail sequence and returns it
unchanged. Internal album/artist grouping first creates groups by exact name in first-seen order,
then sorts groups case-insensitively; tracks sort disc (null 0), track (null 0), lowercase title;
album representative artist is the first sorted track. Equal group names are one group. Internal
list keys/visible callbacks use `(renderedOccurrenceIndex, track.id)` to preserve duplicates.

Home remembers its `LazyListState`, records the supplied backdrop, emits initial/change index/offset
and ordered visible IDs, and Shared reconciles only active `HomeSongs`. Shared clears Songs selection
through its existing route-change reducer after browse change. Home owns playlist button/queue text;
there is no add-to-playlist slot. It receives actual picker and caller-computed mutations state;
only active progress renders. Each leaf has exactly one measured terminal spacer equal to required
padding, whose value Shared measures.

Detail remembers list state, current-track/destination-keyed selected-row fallback, local backdrop,
artwork/collapse/Miuix state, and gestures. Same destination preserves local list/chrome state;
distinct destination resets it. Shared resolves unavailable album/artist before mounting and invokes
its authoritative route-level Back effect; leaf has no unavailable callback.

Shared resolves only route existence/title/tracks and raw `LibraryDetailSummary`. Detail resolves
`unknown_artist`, `album_detail_subtitle_format`, and `artist_detail_subtitle_format` internally:
Album uses localized unknown artist exactly for null, retains non-null artist unchanged, and passes
trackCount unchanged; Artist passes albumCount/trackCount unchanged. No subtitle string, formatter,
or resource handle crosses. Route adapters causally test both variants, null/non-null album artist,
exact counts, removed Shared LibraryRoutes resource usage, and Library EN/ZH rendering.

Shared passes `currentTrackId = playbackState.currentTrack?.id` to both leaves and passes
`artworkLoader = { id -> repository.artworkForTrack(id)?.bytes }`. Leaf playback callbacks carry
only the exact ordered tracks and selected occurrence; Shared retains queue/start/restart policy.

Library owns internal lazy artwork wrappers using public `ArtworkImage`: eager non-null
`Track.artworkBytes` wins without loader; otherwise call loader for non-null ID, key by ID + eager
bytes + loader identity, run off main, preserve cancellation, turn null/other exception into fallback,
and reject stale identity results. Shared supplies `repository.artworkForTrack(id)?.bytes`.
`LocalTrackArtworkLoader` and Shared wrappers are removed after remaining consumers adapt.

## Koin and Resources

The only public Koin-shaped declaration is the module factory. It registers `single` TagLibReader,
internal AudioMetadataReader, LibraryDatabase, public LibraryRepository to internal
SqlDelightLibraryRepository, public PlatformSourceAccess, and public LibraryScanner. Scanner gets
the same repository/metadata reader/concrete scanner role plus `currentTimeMillis`/`uuid4`. Shared
assembles/resolves repository, access, scanner. Any retained direct Shared TagLib use resolves that
same singleton only until adapted; no second reader/scanner/access instance exists.

```kotlin
single<TagLibReader> { createTagLibReader() }
single { AudioMetadataReader(get()) }
single<LibraryDatabase> { createLibraryDatabase() }
single<LibraryRepository> { SqlDelightLibraryRepository(get()) }
single<PlatformSourceAccessAndScanner> { createPlatformSourceAccess() }
single<PlatformSourceAccess> { get<PlatformSourceAccessAndScanner>() }
single<PlatformAudioScanner> { get<PlatformSourceAccessAndScanner>() }
single { LibraryScanner(get(), get<PlatformAudioScanner>(), get(), { currentTimeMillis() }, { uuid4() }) }
```

Identity tests assert `===` across the three platform bindings and exactly one factory invocation;
no wrapper adapter or second object is allowed.

`LibrarySharedLabels` is exactly 10 scalar labels + 2 composable formatters = 12 fields:

| field | Shared key | every leaf consumer | retained Shared consumer |
| --- | --- | --- | --- |
| addMusicFolder | add_music_folder | ImportAudioCard | Settings route adapter |
| folderPickerUnavailable | folder_picker_unavailable | ImportAudioCard | Settings route adapter |
| clearLibrary | clear_library | ImportAudioCard | Settings route adapter and Shared clear-library dialog |
| cancel | cancel | ScanningCard | Settings route adapter, playlist-backup route adapter, and Shared clear-library dialog |
| playlists | playlists | playlists button | Shared playlist-route adapters |
| playlistsAccessibility | playlists_accessibility | button semantics | none |
| libraryQueue | library_queue | queue/playlists-section heading only | none; no action; button calls onShowPlaylists once |
| albumArt | album_art | TrackRow thumbnail | Shared bottom bar |
| albumArtwork | album_artwork | AlbumCard/detail chrome/rows semantics | NowPlayingScreen |
| nowPlayingBadge | now_playing_badge | TrackRow badge/state | Shared Search adapter |
| selectTrack | select_track_format | TrackRow long-click/content description | Shared Search adapter |
| trackArtistAlbum | track_artist_album_format | TrackRow subtitle | Now Playing/bottom bar |

Shared-only: `play`, `pause`, `clear_library_message`, `clear`, `remove`, `scan_complete_format`,
`playlist_backup_imported_suffix`, `adaptive_detail_placeholder`, `close`, `playlist_loading`,
`playlist_load_failed`, `playlist_retry`, `playlist_mutation_failed`, `playlist_changed`; App,
LibraryRoutes, and Now Playing/bottom bar resolve them. Core UI owns `back`. Library moves exactly:
`unknown_artist`, `artist_artwork`, `album_accessibility_format`, `artist_accessibility_format`,
`album_track_count_format`, `track_count_format`, `album_detail_subtitle_format`,
`artist_detail_subtitle_format`, `artist_album_tracks_format`, `browse_mode_albums`,
`browse_mode_artists`, `browse_mode_songs`, `scanning`, `scan_progress_format`, `import_card_title`,
`import_card_title_with_tracks`, `import_card_description`, `folder_picker_error_access`,
`folder_picker_error_select`, `folder_picker_error_prepare`, `folder_picker_no_folder_selected`.
Moved detail keys are feature-only after raw summaries; Shared LibraryRoutes imports/usages are
removed. Shared resolves a localized formatter/value separately for feature injection and retained
Shared consumers; injection never transfers ownership.
Same-key injection never moves ownership. EN/ZH multiset parity, missing/duplicate/wrong-owner/
foreign-key/rendered-resource controls apply. `selected` is absent from both final catalogs and all
production/test references; discovery of a consumer stops for amendment.

## ABI and Android Holder

`f4ae104` intentionally introduced the Swift bridge `readAudioMetadata`/`AudioMetadata`; `30f89ff`
removed its Swift consumer. Slice 6 intentionally retires the now-unused bridge/model/reader after
source/header evidence. Internal metadata model/reader/expect-actual move to
`com.eterocell.rhythhaus.library.impl`; core-model `AudioSource` is unchanged. No facade/export.
Headers omit `SharedAudioMetadata`, `SharedAudioMetadataReader`, `SharedAudioMetadataKt`, Swift
`readAudioMetadata(path:)`, ObjC `readAudioMetadataPath:` and retain `SharedMainViewControllerKt`,
Swift `MainViewController()`, ObjC `MainViewController`.

Retired public declarations are `data class AudioMetadata(val title: String? = null, val artist:
String? = null, val album: String? = null, val durationMillis: Long? = null, val trackNumber: Int? =
null, val discNumber: Int? = null, val artworkBytes: ByteArray? = null, val artworkMimeType: String?
= null)`; `class AudioMetadataReader(private val tagLibReader: TagLibReader = createTagLibReader(),
private val platformMetadataReader: (AudioSource) -> AudioMetadata? = ::readPlatformAudioMetadata)`
with `fun read(source: AudioSource): AudioMetadata?`; and
`fun readAudioMetadata(path: String): AudioMetadata?`.
The model/reader remain internal impl; only public/header bridge exposure is removed.

The sole Android holder is
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`.
Core database's Android-KMP convention compiles its public Android-only object/setter; Shared common
`api(projects.core.database)` exposes the Android variant through androidApp's
`implementation(projects.shared)`. Impl `androidMain` directly uses
`implementation(projects.core.database)`. It is absent Shared/common/iOS/headers; setter stores
applicationContext and calls `setLibraryDatabaseAndroidContext`; app assigns it before
`setRhythHausAndroidContext`, then Koin. Controls cover compile/header, one declaration, old path,
duplicate, visibility, direct dependency, and ordering.

## Acceptance Controls

Tests causally cover literal signatures/default absence/KDoc, grouping/representative/duplicates,
artwork behavior, Home/Detail text/playback/selection/visible IDs/scroll/spacer/unavailable routes,
state identity, source/picker/scanner/repository/metadata/Koin singleton identity, holder controls,
resource ledger, ABI symbols, and real Gradle negative governance. Full platform/quality evidence is
required later; no runtime/device/visual claim follows.
