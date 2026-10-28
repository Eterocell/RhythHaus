# Studio UI Review Remediation

> **Status:** User-approved design and implementation plan. Implementation and automated evidence are recorded in `progress.md` and the OpenSpec tasks; original redesign manual acceptance remains postponed and open.
>
> Route: `openspec+superpowers`. Archived change: `openspec/changes/archive/2026-10-28-studio-ui-review-remediation/`. Input: independent review of the completed Studio UI redesign and the user's request to plan remediation. Manual GUI/device/audio acceptance for `studio-ui-redesign` remains postponed, unverified, and open.

## 1. Goal and review basis

Remediate all ten P2 findings from the UI/specification and architecture/standards review without expanding the Studio redesign into a new navigation, persistence, playback, or design-system project. Four P3 maintainability refactors are explicitly deferred.

The static review found:

1. Queue-save dialog/draft is lost when the selected playback occurrence changes.
2. Settings clear and remove-missing confirmation actions are explicitly 36dp high, below the existing 48dp mobile / 44pt iOS requirement.
3. Library and Search rows lack a named non-gesture Select entry; desktop has no right-click/menu equivalent.
4. A normal row's accessible name says “Select” even though its primary action plays.
5. Search rows have no named metadata-edit action, despite the active Library/Search presentation contract.
6. The Settings resource ownership ledger names `manage_music`, while both catalogs now use `settings_section_sources`; the feature remains Settings-owned.
7. The 2026-08-10 extracted Library API ledger is stale: the redesign removed some parameters/labels and added others.
8. `PlaybackQueueContent` reads the shell-installed `LocalHausWindowOverlayPublisher` through a service-locator CompositionLocal.
9. Library row/detail density has duplicated or inconsistent breakpoint input, including a hardcoded 720dp and redundant `desktopDensity` parameter threading.
10. Queue production code retains a test-only `executeQueueMutation` helper and an unread `QueueRowLayoutPolicy.reservesMetadataWidth` field.

Deferred P3 findings: duplicated queue/list row-layout policy; repeated navigation destinations/forwarders in `AppNavigationChrome`; repeated `BrowseMode` switches; and the 15-parameter `HausSongRow` data clump. No new abstraction or broad cleanup is authorized for those items.

## 2. Goals and non-goals

### Goals

- Keep the open queue-save operation, original current-plus-upcoming snapshot, and name draft stable when playback advances or changes occurrence.
- Preserve Shared ownership of the shell overlay host while making the publisher dependency explicit at the Shared-to-feature composition boundary.
- Make selection and metadata editing discoverable through named row actions on applicable Library and Search rows, without changing play ordering or selection transaction semantics.
- Give normal row activation an accessible identity that does not describe the wrong action.
- Meet the existing maintenance-action touch-target requirements.
- Use one authoritative window-density projection, remove the redundant parameter chain, and eliminate the two P2 queue dead-weight elements.
- Update canonical API/resource ownership ledgers to the current, explicitly selected redesigned API. No compatibility aliases or unused legacy parameters.

### Non-goals

- No changes to persistence, SQL schema, playback selection/queue authority, scanner/source mutation ownership, route/Back authority, permission behavior, iOS picker ABI, or platform packaging.
- No change to the user-facing “Music sources” wording; only the documented resource key changes to the current key.
- No implementation of the four deferred P3 refactors.
- No GUI/device/audio acceptance claim. The user-owned manual matrix remains postponed and open.

## 3. Decisions

### 3.1 Queue-save lifetime and snapshot

Opening Save captures the existing current-plus-upcoming occurrence snapshot once. While the dialog is open, a playback occurrence replacement MUST NOT dismiss the Queue surface, dialog, captured snapshot, or current name draft. A failed save retains the draft and truthful error; cancel creates nothing. Back continues to dismiss the top dialog before the Queue surface through Shared's existing arbitration. Existing no-current and terminal route invalidation behavior is otherwise unchanged.

Shared supplies an explicit window-overlay publisher to the Queue feature. The feature MUST NOT read a Shared-installed host publisher from `LocalHausWindowOverlayPublisher` or any other shell-owned service locator.

### 3.2 Row actions and semantics

On eligible Library and Search rows in normal mode, a localized, named row-action menu exposes `Select` to touch, desktop pointer, and assistive-technology users without requiring long-press. On rows where the existing metadata editor is available, the same menu exposes the named metadata-edit action. These actions remain separate from primary row activation. `Select` selects the row without playback; normal primary activation retains its existing playback order; selection-mode activation toggles exactly once. A normal playing row's accessible name identifies the track and MUST NOT say that primary activation selects it.

### 3.3 Density and touch targets

The root layout's current density projection is the sole source for wide/compact row and detail density. Remove duplicate 720dp switches, recomputation, and `desktopDensity` threading; retain `WIDE_LIBRARY_BREAKPOINT_DP` only for the feature's documented content-grid choice where appropriate. Maintenance buttons use a minimum rather than fixed height so they meet 48dp touch targets (and therefore the 44pt iOS minimum) without clipping enlarged text.

### 3.4 Binding Library contract

The user selected the current redesigned Library API as the intended clean cutover. Update the binding API declaration and resource ledgers; do not restore old playlist-only labels or `title`, `subtitle`, and `onShowPlaylists` parameters as compatibility shims. The 2026-08-10 extraction design remains historical evidence for module boundaries and unaffected contracts; it is not rewritten. The current public signatures below supersede only the named Library UI declarations and injected-label mapping.

### 3.5 Resource ledger

Settings continues to own the source-management section. The EN/ZH resource ledger and `docs/architecture.md` / ADR 0001 must name the current `settings_section_sources` key, not the removed `manage_music` key. Shared-supplied Library labels retain their Shared resolver ownership. `addFavorite` and `removeFavorite` keep their feature-owned defaults.

## 4. Binding Library UI declaration ledger

These declarations are copied from the current implementation baseline. Their types, parameter names, defaults, and field order are binding for the follow-up. Unchanged value types retain their existing declarations and ownership.

```kotlin
public enum class BrowseMode {
    Albums,
    Artists,
    Songs,
    Favorites,
    RecentlyPlayed,
    RecentlyAdded,
}

public sealed interface LibrarySelectionPage {
    public data object HomeSongs : LibrarySelectionPage
    public data class Album(public val name: String) : LibrarySelectionPage
    public data class Artist(public val name: String) : LibrarySelectionPage
}

public sealed interface LibraryDetailSummary {
    public data class Album(
        public val trackCount: Int,
        public val artist: String?,
    ) : LibraryDetailSummary

    public data class Artist(
        public val albumCount: Int,
        public val trackCount: Int,
    ) : LibraryDetailSummary
}

public data class LibrarySharedLabels(
    public val addMusicFolder: String,
    public val folderPickerUnavailable: String,
    public val clearLibrary: String,
    public val cancel: String,
    public val libraryTitle: String,
    public val albumArt: String,
    public val albumArtwork: String,
    public val nowPlayingBadge: String,
    public val selectTrack: @Composable (String) -> String,
    public val trackArtistAlbum: @Composable (String, String) -> String,
    public val addFavorite: @Composable (String) -> String = { title ->
        stringResource(Res.string.favorite_add, title)
    },
    public val removeFavorite: @Composable (String) -> String = { title ->
        stringResource(Res.string.favorite_remove, title)
    },
)

@Composable
public fun LibraryHomeContent(
    tracks: List<Track>,
    browseMode: BrowseMode,
    playHistory: Map<String, TrackPlayHistory>,
    createdAtByTrackId: Map<String, Long>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    mutationsEnabled: Boolean,
    currentTrackId: String?,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    labels: LibrarySharedLabels,
    homeBackdrop: RhythHausBackdrop?,
    artworkLoader: suspend (String) -> ByteArray?,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onClearLibrary: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit,
    onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
    favoriteTrackIds: Set<String>,
    artworkTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    browseQuery: LibraryBrowseQuery,
    sourceOptions: List<LibraryBrowseSourceOption>,
    sourceIdByTrackId: Map<String, String?>,
    modifiedAtByTrackId: Map<String, Long?>,
    onBrowseSortChange: (LibrarySort) -> Unit,
    onBrowseSortDirectionChange: (LibrarySortDirection) -> Unit,
    onBrowseFavoriteOnlyChange: (Boolean) -> Unit,
    onBrowseArtworkOnlyChange: (Boolean) -> Unit,
    onBrowseSourceIdChange: (String?) -> Unit,
    onEditTrackMetadata: ((String) -> Unit)? = null,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
public fun DrillDownView(
    title: String,
    summary: LibraryDetailSummary,
    tracks: List<Track>,
    topBarArtworkTrack: Track?,
    currentTrackId: String?,
    selectionPage: LibrarySelectionPage,
    selectionModeActive: Boolean,
    selectedTrackIds: Set<String>,
    labels: LibrarySharedLabels,
    artworkLoader: suspend (String) -> ByteArray?,
    onBack: () -> Unit,
    onPlayTrack: (orderedTracks: List<Track>, selectedTrack: Track) -> Unit,
    onToggleSelection: (trackId: String) -> Unit,
    onStartSelection: (trackId: String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    bottomContentPadding: Dp,
    favoriteTrackIds: Set<String>,
    onSetTrackFavorite: (String, Boolean) -> Unit,
    onEditTrackMetadata: ((String) -> Unit)? = null,
)
```

The twelve labels and ownership are: Shared supplies `addMusicFolder`, `folderPickerUnavailable`, `clearLibrary`, `cancel`, `libraryTitle`, `albumArt`, `albumArtwork`, `nowPlayingBadge`, `selectTrack`, and `trackArtistAlbum`; the feature supplies the default `addFavorite` and `removeFavorite` formatters. There are no playlist-only injected labels.

## 5. Acceptance criteria

1. At the production Shared Queue entry point, advancing/replacing the current occurrence while Save is open leaves the dialog, captured original snapshot, and name draft intact. Successful save uses the captured snapshot; cancel mutates nothing; storage failure keeps the draft; Back dismisses the dialog before Queue.
2. Queue feature composes dialogs through the explicitly supplied host publisher; no feature source reads `LocalHausWindowOverlayPublisher`.
3. Library and Search normal rows have a named accessible action menu. Select via that menu works without long-press and does not play; normal row activation plays the same visible ordered sequence; selection mode toggles once. The row's accessible name does not describe the wrong action.
4. Search exposes the existing metadata editor from the named row menu; editing preserves query/result state, cancel persists nothing, and the existing editor validation/restore behavior is unchanged.
5. Clear and remove-missing action targets meet the minimum 48dp mobile size, remain at least 44pt on iOS, and do not clip at enlarged text.
6. Root density is the only source for responsive row/detail presentation. No 720dp magic number or redundant `desktopDensity` forwarding remains outside the documented Library content breakpoint.
7. The current Library declarations and Settings source-management resource key are reflected in canonical docs and OpenSpec. No old API compatibility fields remain.
8. `executeQueueMutation` and `QueueRowLayoutPolicy.reservesMetadataWidth` have no production use after cleanup; actual consumer behavior is tested instead.
9. Focused module tests, `:architectureCheck`, OpenSpec validation, and the repository's prescribed full verification are recorded with exact command output. No automated result is described as GUI/manual acceptance.

## 6. Verification and acceptance boundaries

Expected automated targets include `:core:ui:jvmTest`, `:feature:library:impl:jvmTest`, `:feature:search:jvmTest`, `:feature:playlists:impl:jvmTest`, and `:shared:jvmTest`, followed by `./init.sh` and OpenSpec validation. Existing consumer-boundary tests should be updated rather than replaced by source-text checks or a new duplicate test suite.

The following remain user-performed and outside this plan's acceptance claim: visual inspection across actual supported surfaces, keyboard/screen-reader interaction, physical iOS touch confirmation, audible playback, route-loss/headphone scenarios, and system-media controls. `openspec/changes/studio-ui-redesign/tasks.md` manual/mixed acceptance items remain open and postponed.
