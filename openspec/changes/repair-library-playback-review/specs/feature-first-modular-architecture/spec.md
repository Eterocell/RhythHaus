## MODIFIED Requirements

### Requirement: Search is a callback-first unexported leaf feature

Task 5.3 SHALL create exactly one unexported `:feature:search` with Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64` targets; one common implementation; Kotlin package and Android namespace `com.eterocell.rhythhaus.search`; and resource namespace `rhythhaus.feature.search.generated.resources`. It SHALL have no API split, Koin module, platform source, iOS export, Shared/core playback/database/platform/taglib/another-implementation/app edge, repository, empty state abstraction, or `feature/search/README.md`. Its `api` SHALL be only Library API plus public Compose runtime/UI requirements; core UI, Foundation, resources, and Miuix SHALL be implementation-only. Shared SHALL depend through exactly `implementation(projects.feature.search)`, SHALL NOT use `api`, and SHALL NOT export Search.

Its only explicit-public, declaration-specific-KDoc boundary SHALL include this value-equal Shared-label declaration:

```kotlin
/**
 * Shared-owned wording consumed by [SearchContent]. Value equality keeps unchanged
 * labels stable across recomposition; callers provide already-localized text.
 *
 * @property clear Label for the query-clear action.
 * @property nowPlaying Accessibility state for the current result.
 */
public data class SearchSharedLabels(
    /** Query-clear action label resolved by Shared. */
    public val clear: String,
    /** Current-result accessibility state resolved by Shared. */
    public val nowPlaying: String,
)
```

The other and final public declaration SHALL be:

```kotlin
@Composable
public fun SearchContent(
    libraryTracks: List<LibraryTrack>, currentTrackId: String?, isPlaying: Boolean,
    labels: SearchSharedLabels, selectTrackLabel: @Composable (String) -> String,
    selectionModeActive: Boolean, selectedTrackIds: Set<String>,
    onStartSelection: (String) -> Unit, onToggleSelection: (String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    onPlayTrack: (orderedResults: List<LibraryTrack>, selectedTrack: LibraryTrack) -> Unit,
    onDismiss: () -> Unit, playingIndicator: @Composable () -> Unit,
    artworkLoader: suspend (String) -> ByteArray?,
    bottomContentPadding: Dp = 0.dp, modifier: Modifier = Modifier,
    initialFirstVisibleItemIndex: Int = 0, initialFirstVisibleItemScrollOffset: Int = 0,
    interactive: Boolean = true, onEditTrackMetadata: ((String) -> Unit)? = null,
)
```

No Shared/generated resource/playback controller or state/repository/Koin/platform/database/TagLib/queue type SHALL occur in that boundary. `selectTrackLabel` SHALL composably resolve Shared's `select_track_format` with structured Compose `stringResource` while Search composes a row; no generated resource handle crosses the boundary. `LibraryRoutes` SHALL compose `SearchContent` directly and delete Shared compatibility `SearchScreen` and unused `TagLibReader`. Shared SHALL retain route/Back, selection/reconciliation/clear, scroll storage, playback queue/restart/dismiss, bottom-bar/Now Playing policy, and `EqualizerStrip`; Search SHALL retain query/filter/render/focus/count/empty/row interaction. The ordinary Search route title and Back action SHALL come from the Shared route header, not `SearchSharedLabels` or Search content. Search SHALL preserve blank-query no results; case-insensitive title/artist/album matching; order, duplicates, and empty metadata; and a private rendering-only LazyColumn occurrence identity of filtered occurrence index plus track ID, never `track.id` alone. It SHALL be unique without altering public `LibraryTrack`, selection IDs, visible-ID sequence, playback queue order, or duplicate semantics. Search SHALL focus exactly once; clear reset to blank; visible-ID emission only on sequence change; normal activation requesting ordered filtered playback; long press never playing; selection-mode row and checkbox each toggling once without playback; current-row highlight and Now Playing semantics; an indicator only for current+playing; and no competing artwork action or error state. Search SHALL retain the five required EN/ZH keys and may add Search-owned keys with complete EN/ZH parity; Shared SHALL inject clear/Now Playing/composable select-track formatting without duplicate keys or resource handles. The approved design is [2026-08-07-search-feature-extraction-design.md](../../../../../docs/superpowers/specs/2026-08-07-search-feature-extraction-design.md).

#### Scenario: Search ownership and behavior are verified
- **WHEN** Task 5.3 is implemented
- **THEN** feature production-composable tests cover Search behavior and the four moved mixed-suite cases, including two equal-ID occurrences rendering/activating distinctly with keys surviving unrelated recomposition and visible/playback callbacks preserving duplicate order, while real Shared route-adapter tests prove queue order, current-track restart, dismissal, and callback-failure ownership
- **AND** RED/GREEN rejects an absent `:feature:search` module/target before registration with failure caused solely by absence; feature-to-Shared/core-playback/database/core-platform/taglib/another-implementation/app edges; Koin; iOS export; Shared `api` or exported Search exposure; wrong package/Android/resource namespace; a wrong Search resource-ownership control where a moved key is missing, duplicated, or owned by the wrong module, distinct from wrong namespace and generated-handle controls; resource duplicates/generated handles; and missing public KDoc/public-surface closure
- **AND** supported-platform, architecture, quality, strict named OpenSpec, Xcode, and `./init.sh` evidence are recorded without runtime/device/visual claims.
