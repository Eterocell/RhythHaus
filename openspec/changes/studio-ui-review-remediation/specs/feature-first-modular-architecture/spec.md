# Spec Delta

## ADDED Requirements

### Requirement: Queue overlay publishing is an explicit feature contract
The Queue feature SHALL receive the shell-owned window-overlay publisher through its Shared composition entry point. It SHALL NOT resolve `LocalHausWindowOverlayPublisher` or another shell-owned capability through a CompositionLocal service locator.

#### Scenario: Queue presents a host-level modal
- **WHEN** Shared composes Queue content that needs a dialog outside its pane
- **THEN** Shared SHALL supply the required overlay publisher and the Queue feature SHALL present the dialog without reading a shell CompositionLocal

## MODIFIED Requirements

### Requirement: Library literal authority has no selectable alternatives

The Studio UI review remediation design's Kotlin declarations SHALL apply exactly and SHALL
supersede the 2026-08-10 Library extraction design only for the Library UI declarations, their
defaults, and the injected-label membership and ownership represented by the current twelve-member
`LibrarySharedLabels` contract. Every Home/Detail API SHALL use `currentTrackId` and no `isPlaying`
or `onPlayPause`; every parameter SHALL be required except the single trailing nullable
`onEditTrackMetadata: ((String) -> Unit)? = null` default; both SHALL accept the required suspend
artwork loader and preserve its documented behavior. The exact parameter names, types, defaults,
callbacks, and the twelve `LibrarySharedLabels` members SHALL match
`docs/superpowers/specs/2026-10-26-studio-ui-review-remediation-design.md`. The current label
members SHALL be `addMusicFolder`, `folderPickerUnavailable`, `clearLibrary`, `cancel`,
`libraryTitle`, `albumArt`, `albumArtwork`, `nowPlayingBadge`, `selectTrack`, `trackArtistAlbum`,
`addFavorite`, and `removeFavorite`; Shared SHALL provide the first ten, and Library SHALL own the
default `addFavorite`/`removeFavorite` formatters. The removed `playlists`, `playlistsAccessibility`,
and `libraryQueue` members, other removed playlist-only labels, and the old Home `title`, `subtitle`,
and `onShowPlaylists` parameters SHALL NOT remain as compatibility aliases.

Impl SHALL use API core-model/core-UI/Compose signature dependencies and implementation-only
remaining UI dependencies; Shared SHALL use implementation-only impl dependency and no export. The
authoritative track/group ordering, occurrence keys, state/reset, one-spacer, routes, KDoc controls,
and the retained 2026-08-10 consumer resolver mapping SHALL apply. That mapping retains the
album-artwork and track-artist-album consumer rows plus Shared-only `play`/`pause`; its former
library-queue heading row is superseded, and the Queue heading is playlists-owned `queue_title`,
never a `LibrarySharedLabels` member or injected label.

`libraryImplementationModule()` SHALL be the only public Koin-shaped declaration; all stated
bindings SHALL be singletons and use one concrete platform object for public access/internal scan
roles. Metadata retirement is intentional after the documented f4ae104/30f89ff history, not an
accidental-export claim. Android holder compilation/transitivity/direct-impl dependency/init-order/
sole-path controls and exact header absence/retention controls SHALL apply.

#### Scenario: Literal Library boundary rejects substituted plumbing
- **WHEN** implementation adds or renames a UI parameter, changes an explicitly declared default,
  retains an old playlist-only UI parameter as a compatibility alias, restores a removed label
  member, adds an `isPlaying` or `onPlayPause` leaf argument, introduces a second
  scanner/access/TagLib singleton, changes the artwork-loading contract, or uses an alternative
  holder/export path
- **THEN** public-surface, production-composable, Koin, Gradle, and ABI controls fail
- **AND** only the current Studio declarations, the twelve-member injected-label ledger, singleton
  graph, holder path/order, and intentional metadata-retirement symbols are accepted

### Requirement: Library detail formatting and exposed dependency graph are exact

Impl SHALL declare `api` dependencies on Library API, core model, core UI, and Compose artifacts
needed by public signatures. `DrillDownView` SHALL accept required `LibraryDetailSummary`, never a
formatted subtitle; Shared SHALL supply only raw title/route/tracks/counts and feature SHALL resolve
the moved unknown-artist/detail subtitle keys. The dedicated internal scanner/event/access/metadata
signatures, all-single Koin declarations with `===` identity, the retained 2026-08-10 consumer
resolver mapping for album-artwork and track-artist-album plus Shared-only `play`/`pause`, public
metadata retirement, and retained Shared consumers SHALL apply exactly.

Because the sole public factory returns Koin `Module`, impl SHALL declare exactly
`api(libs.koin.core)`; Koin Compose and non-signature Koin/UI dependencies SHALL remain
implementation-only. The dedicated signature authority is binding pseudocode. Production source
SHALL contain declaration-specific KDoc on every listed public declaration/member/constructor
property/function and `@param` entries for each public callable parameter/callback; KSP SHALL inspect
source and reject missing or generic-placeholder documentation.

#### Scenario: Detail formatting remains feature-owned
- **WHEN** album and artist detail routes are adapted
- **THEN** Shared supplies both raw summary variants and no detail formatter/resource handle
- **AND** feature tests render EN/ZH detail copy for null/non-null artist and unchanged counts while
  Koin/resource controls reject second objects, wrong API scope, or a moved-key Shared import.

#### Scenario: Public Koin type receives API scope
- **WHEN** Gradle and KDoc governance inspect Library impl
- **THEN** `libs.koin.core` is an `api` dependency because factory `Module` is public
- **AND** implementation-only Koin core, Koin Compose public leakage, source-block-comment KDoc, or
  missing callable `@param` documentation fails.

### Requirement: Settings is a callback-first unexported leaf feature

Task 6.4 SHALL create exactly one unexported Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`
`:feature:settings` with one common implementation, no API/implementation split, no Koin module,
no `iosMain` production source, and no Shared-framework export. Its Kotlin package and Android
namespace SHALL remain `com.eterocell.rhythhaus.settings`; its resource namespace SHALL be
`rhythhaus.feature.settings.generated.resources`. Its only project edge SHALL be
`api(:core:ui)`; public Compose/runtime/UI dependencies required by public signatures SHALL be API,
and Foundation/resources/icons/Miuix/AboutLibraries/coroutines SHALL be implementation-only.
Shared SHALL use only `commonMainImplementation`, never `api` or export. Settings SHALL NOT depend
on Shared, apps, core database/platform/playback, taglib, any feature module, Koin, DataStore, or
an iOS export; it SHALL NOT use Library API.

Its exact KDoc-complete public declarations SHALL be `SettingsSharedLabels(title, addMusicFolder,
folderPickerUnavailable, clearLibrary, cancel, remove)`, value-equal
`SettingsSourceItem(id, displayName, accessAvailable, hasBeenScanned, trackCount)`, `SettingsScreen`,
`SettingsAboutScreen`, and `OpenSourceLibrariesScreen(readCatalogJson: suspend () -> String, ...)`.
No other Settings production declaration SHALL be public. No public signature SHALL contain Shared,
Library, Playlist, generated foreign `Res`, route/Back, repository, scanner, launcher, controller,
job, Koin, or DataStore types.

The exact `SettingsScreen` boundary SHALL include `sourcePickerActionVisible: Boolean`,
`sourcePickerAvailable: Boolean`, `mutationsEnabled: Boolean`, `hasImportedTracks: Boolean`,
`onRequestClearLibrary: () -> Unit`, and nullable
`clearLibraryDialog: (@Composable () -> Unit)?` for Shared-owned library-maintenance confirmation,
along with scalar theme/source inputs, source-ID
callbacks, playlist-backup and nullable scan slots, About/dismiss callbacks, and `Modifier`.
Function KDoc SHALL document parameter behavior in prose/contracts, while declaration-specific KDoc
is required for every public declaration and every public data property. The picker SHALL hide when
not visible, use unavailable wording when unavailable, and respect mutation disablement; the clear
action SHALL render only with imported tracks, request Shared visibility only when enabled, and
render the nullable slot.

#### Scenario: Settings composes presentation without receiving Library ownership
- **WHEN** Shared composes Settings
- **THEN** it maps authoritative current Library sources and their current per-source track counts
  to `SettingsSourceItem`, supplies scalar values, callbacks, `playlistBackupContent`, nullable
  `activeScanContent`, and a `clearLibraryDialog` library-maintenance confirmation slot
- **AND** Settings owns only presentation-local source-removal dialog visibility and About retry
  generation, without Presenter/ViewModel/Event/Effect scaffolding
- **AND** Shared retains authoritative source-ID adaptation, mutation guards/errors, scanner/picker/
  clear-library/remove-missing confirmation orchestration, routes/Back/dismissal,
  playlist-backup controller/dialog behavior,
  theme persistence/actuals/Koin/root theme application, and `RhythHausThemeMode`/palettes in core UI.

#### Scenario: Settings source callbacks resolve current authority
- **WHEN** Settings emits a source ID
- **THEN** Shared resolves it against latest authoritative `librarySources` at invocation and then
  reevaluates initial-publication and scan/job guards
- **AND** a missing/stale ID is a no-op with no mutation, scan, access release, or unrelated
  dismissal; errors use the existing Shared import/mutation path
- **AND** tests cover current/stale IDs, authoritative replacement between composition and click,
  and changed guards.

#### Scenario: Settings preserves app-wide attribution and resource ownership
- **WHEN** Task 6.4 moves Settings/About presentation and `RhythHausBuildInfo` generation/model/
  verification to Settings
- **THEN** Shared retains AboutLibraries generation/configuration/manual TagLib attribution and its
  checked-in `aboutlibraries.json`, and Settings parses/renders caller-supplied JSON with retryable
  read/parse failures; injected read and parse callback cancellation preserve exact object identity (parse is carried as data out of dispatcher work), while dispatcher rejection, prompt cancellation, and Job cancellation propagate without `Loaded`/`Failed` publication or identity promise
- **AND** Settings owns appearance/theme, source-management-only, About/AboutLibraries, logo, and
  remove-source-dialog resources, while Shared retains/injects its clear-dialog and generic wording;
  no generated resource handle crosses the boundary.

The Shared EN/ZH ledger SHALL retain `settings`, `add_music_folder`,
`folder_picker_unavailable`, `clear_library`, `clear_library_message`, `clear`, `cancel`, `remove`,
and `close`, plus `scan_complete_format` and `folder_picker_error_prepare` for Shared App and
platform-import consumers. Library SHALL own `scanning`, `scan_progress_format`,
`folder_picker_error_access`, `folder_picker_error_select`, and `folder_picker_no_folder_selected`
for its scanning-card and picker consumers; neither owner's keys SHALL cross into Settings.
Labels carry `settings`, `add_music_folder`,
`folder_picker_unavailable`, `clear_library`, `cancel`, and `remove`, while Shared resolves playlist
`close` and clear-dialog strings internally. Settings SHALL own exactly once in each locale
`settings_section_sources`, all appearance/theme, source-row/remove-dialog, About/AboutLibraries keys, and
`rhythhaus_logo` listed by the approved Settings design. Architecture controls SHALL reject parity,
missing, wrong-owner, duplicate, and logo violations. Non-empty catalog parsing is required; the
fixture has a top-level `libraries` array and `licenses` map, and malformed/empty data is the same
retryable Failure. Retry immediately enters Loading and uses `Dispatchers.Default`. An injected read
`CancellationException` SHALL rethrow with exact identity; injected parse cancellation SHALL be
captured inside dispatcher work and rethrown outside with its original identity. Genuine dispatcher
rejection, `withContext` prompt cancellation, and Job cancellation SHALL propagate without
`Loaded`/`Failed` publication and without an identity guarantee. The current request is an opaque
token of current loader identity plus monotonic retry generation; only exact-current-token completion
publishes, so an obsolete cancellation-resistant loader cannot overwrite a newer replacement result.
The root SHALL retain visual background/root semantics and use an input-only full-size non-semantic
pointer shield as its first background child. Opaque Settings `Surface`/`Scaffold`/`LazyColumn` SHALL
be a later foreground sibling and clear-library/source-removal dialogs later siblings above both. The
shield SHALL consume at Initial only when sibling hit testing selects it, SHALL never be a controls
ancestor, and SHALL have no clickable/focusable semantics; `AboutRow` remains normally clickable. The
lower Library shell and Settings overlay are layered siblings, so the shield catches uncovered overlay
coordinates while foreground controls win sibling hit testing. Causal tests SHALL mount a behind
full-size clickable sibling first then production Settings: a physical blank-coordinate tap proves
behind=0; a physical picker-center tap proves picker=1 and behind=0; root/shield have no click/focus
semantics; and scroll/children work. Tests SHALL use stable tags/unmerged Miuix trees; disabled
mutations prove no click action and zero callbacks unless an approved accessibility contract explicitly
requires disabled semantics. No parent-pass inference, background-ancestor test, `nestedScroll`,
`pointerInterop`, disabled clickable, or custom sibling-sharing node is allowed.

### Requirement: Corrected Library authority is executable

The Library requirements above are amended by the dedicated approved Library design. The retained
API module SHALL depend only on core model and SHALL contain no playback type after
`LibraryTrack.toPlayableTrack()` moves to Shared. Shared SHALL use `commonMainImplementation` for
one unexported impl and retain App/root shell, route/Back/predictive/navigation identities,
`LibraryAppState`, Back-required browse state, selection/page mapping, visible-ID reconciliation,
scroll and bottom-bar policy, playback/session, scan Job/orchestration/publication, cross-feature
composition, Koin startup/total assembly, and `LibraryPlaybackSelection.kt`. Impl SHALL own only
repository/scanner/metadata/TagLib/platform seams/leaf UI/local rendering/resources; no
Presenter/ViewModel/Event/Effect layer is permitted.

Its exact public surface SHALL be the KDoc-complete `PlatformSourceAccess` access/release methods,
internal-constructor `LibraryScanner.scan(source, isCancelled, onProgress)`, `ScanProgress`, folder
picker result/launcher/expect, neutral browse/group/selection-page projections, callback-first
`LibraryHomeContent`/`DrillDownView`, and exactly `public fun libraryImplementationModule(): Module`.
Public signatures SHALL satisfy the dedicated design's prohibited-type and callback rules. The
Android `LibraryDatabaseContext` holder SHALL move physically to core database without changing its
package/name/public setter or application-before-Koin initialization order. The exhaustive EN/ZH
ledger, `selected` unused-removal proof, public KDoc/boundary controls, and intentional metadata
ABI contraction based on the documented `f4ae104`/`30f89ff` consumer history SHALL be enforced.

The exact public UI declarations for `BrowseMode`, `LibrarySelectionPage`, `LibrarySharedLabels`,
`LibraryHomeContent`, and `DrillDownView`, including every parameter name/type/default and label
ownership, SHALL follow the current Studio remediation authority in `Library literal authority has
no selectable alternatives`. That authority supersedes only the 2026-08-10 design's UI declarations
and injected-label membership/ownership; its other contracts remain binding. The declarations SHALL
NOT be generalized into unnamed parameter groups. Home/detail SHALL use only `Track`, `RhythHausBackdrop`, Compose,
feature values, and primitive callbacks; private occurrence-index-plus-ID keys preserve duplicate
rendering and ordered visible reports; each emits primitive index/offset and has exactly one
required-padding terminal spacer. Shared SHALL resolve unavailable detail before composition and
perform existing route-level Back, while local state follows the exact dedicated state ledger and
`formatDuration` is internal Library code.

#### Scenario: Oracle conversion inventory correction
- **WHEN** Slice 6 is implemented
- **THEN** the six paths previously named are historical/baseline inventory only
- **AND** post-extraction exactly four retained Shared production files own conversion/projection:
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`.
- **AND** moving feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and contains no conversion.
- **AND** Task 7 deletes only `LibraryTrack.kt` method/import residue, adapts/removes `LibraryApiModelsTest.kt` conversion assertions, adapts `PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection, and retains unrelated `MusicModels.kt` `Track.toPlayableTrack()` plus `SearchRouteAdapterJvmTest.kt` use.

#### Scenario: Shared owns every Library playback projection
- **WHEN** Slice 6 removes the Library API conversion
- **THEN** the six listed paths are historical/baseline inventory only:
  `App.kt`, `LibraryAppShell.kt`, moving `LibraryHomeContent.kt`, `LibraryRoutes.kt`,
  `PlaybackSessionCoordinator.kt`, and playlist impl `PlaylistScreens.kt`
- **AND** `PlayableTrack` remains a core-model type; Library API remains core-model-only and exposes
  neither it nor a conversion, while moving leaf UI returns `Track` callback data.
- **AND** Shared `LibraryRoutes.kt` passes `PlaylistDetailScreen` an authoritative
  `playableTracksById: Map<String, PlayableTrack>` with current ID-keyed `associate` behavior;
  playlist detail performs no local map, the map does not represent duplicate occurrences, and
  unchanged `SavedPlaylistPlaybackRequest(occurrences, selectedOccurrenceId)` and `onPlayEntry`
  preserve queue occurrence order, identity, selected occurrence, and callback settlement.
- **AND** playlists impl retains existing core-model API visibility only; no new project/Shared edge
  or callback-payload redesign is introduced, and browser overlays may retain `List<LibraryTrack>`
  metadata input.

The ledger SHALL list the twelve label members with their Shared-supplied or Library-default
ownership, all Shared-only non-injected keys, all Library-moved keys, and core UI `back`; it SHALL require EN/ZH multiset equality plus duplicate,
missing, wrong-owner, foreign-key, rendered-resource, and selected-absence controls. Metadata SHALL
be internal at `com.eterocell.rhythhaus.library.impl`; headers SHALL omit `SharedAudioMetadata`,
`SharedAudioMetadataReader`, `SharedAudioMetadataKt`, `readAudioMetadata(path:)`, and
`readAudioMetadataPath:` while retaining all named MainViewController symbols. The only Android
holder declaration SHALL be the dedicated core-database path, with old-path/duplicate/visibility/
direct-impl-dependency/init-order negatives.

#### Scenario: Corrected Library implementation is accepted only with causal evidence
- **WHEN** Tasks 7.1-7.3 are implemented
- **THEN** causal production-composable, scanner/source/picker/repository/metadata, process-init,
  header-contraction, dependency/namespace/resource/Koin, and malformed-negative controls prove the
  dedicated design across required targets
- **AND** no task is checked until all bounded platform/quality/ABI/OpenSpec evidence is recorded.
