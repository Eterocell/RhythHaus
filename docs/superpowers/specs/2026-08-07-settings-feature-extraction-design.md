# Task 6.4 Settings Feature Extraction Design

**Route:** OpenSpec + Superpowers

## Context

Settings currently lives in Shared and combines leaf presentation with Library source
objects, scanner jobs and progress, folder-launcher behavior, playlist backup embedding,
theme persistence, routes, and AboutLibraries catalog loading. Task 6.4 extracts only the
presentation leaf. Shared remains the facade and policy owner.

This approved design supersedes the stale skeletal Settings wording in the earlier migration
plan. It does not amend that executable implementation plan; `writing-plans` follows this
approved design.

## Selected Boundary

Create exactly one unexported Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`
`:feature:settings` implementation module. It has no API/implementation split, Koin module,
`iosMain` production source, or Shared-framework export. It has one common production
implementation, retains the Kotlin package and Android namespace
`com.eterocell.rhythhaus.settings`, and uses Compose resource namespace
`rhythhaus.feature.settings.generated.resources`.

The only feature project dependency is `api(:core:ui)`. Public Compose/runtime/UI dependencies
needed by the declared boundary use `api`; Compose Foundation, Compose resources, icons, Miuix,
AboutLibraries, and coroutines are implementation dependencies. The feature has no Library API
dependency. Shared maps authoritative `LibrarySource` values to the public projection below.
Shared depends on Settings through `commonMainImplementation` only, never `api`, and does not
export it.

The following edges are forbidden: Settings to Shared, apps, core database/platform/playback,
taglib, all feature modules, Koin, DataStore, or an iOS export. Its public API also forbids
generated foreign `Res`, route/Back, repository, scanner, folder-launcher, controller, and job
types. No public declaration may expose a Shared, Library, or Playlist type.

## Public Boundary

Every public declaration and every public data property has declaration-specific behavioral KDoc.
Function KDoc documents parameter behavior in prose/contracts; this deliberately does not require an
`@param` tag for every public function parameter. The
complete public production surface is exactly `SettingsSharedLabels`, `SettingsSourceItem`,
`SettingsScreen`, `SettingsAboutScreen`, and `OpenSourceLibrariesScreen`; all other production
declarations, including `RhythHausBuildInfo`, layout/policy helpers, catalog parser/load state,
URLs, and test tags, are `internal` or `private`.

```kotlin
/** Shared-owned wording and actions injected into [SettingsScreen]. */
public data class SettingsSharedLabels(
    /** Settings route title resolved by Shared. */
    public val title: String,
    /** Shared-owned add-folder action wording. */
    public val addMusicFolder: String,
    /** Shared-owned unavailable-picker wording. */
    public val folderPickerUnavailable: String,
    /** Shared-owned clear-library action wording. */
    public val clearLibrary: String,
    /** Shared-owned generic cancellation wording. */
    public val cancel: String,
    /** Shared-owned generic removal wording. */
    public val remove: String,
)

/** Immutable, feature-safe rendering projection for one authoritative Library source. */
public data class SettingsSourceItem(
    /** Stable source identifier returned to Shared callbacks. */
    public val id: String,
    /** User-visible source name already selected by Shared. */
    public val displayName: String,
    /** Whether the source remains accessible to the platform. */
    public val accessAvailable: Boolean,
    /** Whether the source has completed at least one scan. */
    public val hasBeenScanned: Boolean,
)

/**
 * Renders Settings from scalar state, source projections, callbacks, and caller-owned slots. The
 * picker obeys [sourcePickerActionVisible], [sourcePickerAvailable], and [mutationsEnabled]; the
 * clear action is rendered only for [hasImportedTracks], requests Shared dialog state through
 * [onRequestClearLibrary] only when enabled, and renders [clearLibraryDialog] only when supplied.
 * Source callbacks emit IDs only; Shared resolves and guards them at invocation.
 */
@Composable
public fun SettingsScreen(
    labels: SettingsSharedLabels,
    currentThemeMode: RhythHausThemeMode,
    sources: List<SettingsSourceItem>,
    sourcePickerActionVisible: Boolean,
    sourcePickerAvailable: Boolean,
    importMessage: String?,
    mutationsEnabled: Boolean,
    hasImportedTracks: Boolean,
    playlistBackupContent: @Composable () -> Unit,
    activeScanContent: (@Composable () -> Unit)?,
    clearLibraryDialog: (@Composable () -> Unit)?,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onAddMusicFolder: () -> Unit,
    onRescanSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onRequestClearLibrary: () -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)

/** Renders the feature-owned About page and delegates route actions to Shared. */
@Composable
public fun SettingsAboutScreen(
    onOpenLibraries: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * Reads and renders caller-supplied app-wide attribution JSON, retaining retryable failures and
 * preserving exact injected read/parse callback cancellation identity (parse leaves dispatcher work
 * as data), while dispatcher rejection, prompt cancellation, and Job cancellation propagate without
 * `Loaded`/`Failed` publication or an identity promise.
 */
@Composable
public fun OpenSourceLibrariesScreen(
    readCatalogJson: suspend () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`RhythHausThemeMode` and its palettes remain `:core:ui`; therefore `:core:ui` is the sole
feature project dependency. The signatures contain no Library API dependency because
`SettingsSourceItem` is the boundary representation. The only behavior defaults are the normal
`Modifier` defaults; callbacks, state, and slots remain explicit.

## Ownership And Interaction Flow

Shared maps sources to projections but resolves every emitted ID against the latest authoritative
`librarySources` at callback invocation, never the composition snapshot. A missing or stale ID is a
no-op: no mutation, scanning, access release, or unrelated dismissal. After successful resolution,
Shared reevaluates initial-publication and scan/job guards and routes errors through the existing
import/mutation error path. Shared owns folder-picker orchestration, clear-library dialog visibility
and execution, Settings route dismissal/Back, and all mutation authority. The feature owns visual
source rows, appearance controls, source-removal dialog visibility, About presentation, and an
AboutLibraries retry generation.

The Task 6.4-specific local-state ruling is explicit: do not introduce Presenter, ViewModel,
Event, Effect, or other state scaffolding. This presentation-local dialog/retry state is sufficient
and does not revise the architecture-wide state guidance.

The playlists implementation continues to own playlist backup state/controller/dialog behavior.
Shared embeds its existing content through `playlistBackupContent`; the Settings feature neither
imports nor exposes playlist types. Shared likewise embeds the active scanning card through the
nullable `activeScanContent` slot. Shared owns folder-picker behavior, source mutation
orchestration, and clear-library dialog visibility/content/execution. The feature invokes
`onRequestClearLibrary` only for an enabled, visible clear action and renders the nullable
`clearLibraryDialog` slot. Shared dismissal clears visibility; confirmation invokes the existing
guarded mutation and then clears visibility.

Theme persistence remains Shared: `ThemePreferenceStore`, Android/iOS/JVM actuals,
`systemPrefersDarkTheme`, the Koin binding, selected-mode collection/persistence, and root theme
application do not move. Settings renders the supplied `RhythHausThemeMode` and emits only the
selection callback. No Koin or DataStore dependency is introduced in the feature.

Settings/About routes, Back handling, and Settings dismissal remain Shared. The feature delegates
all such decisions through callbacks.

## AboutLibraries And Build Information

Move Settings/About presentation and the `RhythHausBuildInfo` generator, generated model, and
version-override verification task to the Settings feature. The generated model remains internal.

AboutLibraries catalog generation/configuration, the manual TagLib attribution, and the checked-in
`shared/src/commonMain/composeResources/files/aboutlibraries.json` remain Shared because they are
app-wide attribution facts. This deliberately corrects the stale skeletal plan; the catalog does
not move to Settings. Shared supplies `readCatalogJson: suspend () -> String`; Settings parses and
renders it. A successful parse must be non-empty; malformed or empty catalogs become the same
retryable Failure. A valid fixture has a top-level `libraries` array and `licenses` map. Each retry
immediately enters Loading and calls the current supplied loader on `Dispatchers.Default`. The current
request is an opaque token of current loader identity plus monotonic retry generation; only
exact-current-token completion publishes, so an obsolete cancellation-resistant loader cannot
overwrite a replacement result. An injected read callback `CancellationException` is rethrown with
exact object identity. An injected parse callback `CancellationException` is captured inside
dispatcher work as data/outcome and rethrown outside with the original object identity. Genuine
dispatcher rejection, `withContext` prompt cancellation, and Job cancellation propagate as
cancellation, publish neither `Loaded` nor `Failed`, and have no object-identity guarantee because
coroutine machinery owns them. The dispatcher test proves supplied-dispatcher execution and
rejection/cancellation propagation/cause without publication, not arbitrary token identity.

## Resources

The EN and ZH ledgers have identical ownership and parity. Shared retains `settings`,
`add_music_folder`, `folder_picker_unavailable`, `clear_library`, `clear_library_message`, `clear`,
`cancel`, `remove`, `close`, `scanning`, `scan_progress_format`, `scan_complete_format`,
`folder_picker_error_access`, `folder_picker_error_select`, `folder_picker_error_prepare`, and
`folder_picker_no_folder_selected`. Shared owns these scanning/picker-error values because its
scanning card, App, and platform picker consumers render them; they never cross into Settings.
`SettingsSharedLabels` carries `settings`, `add_music_folder`, `folder_picker_unavailable`,
`clear_library`, `cancel`, and `remove`; Shared resolves playlist `close` and clear-dialog strings
internally. Settings owns exactly once per locale `appearance`,
`theme_system_label`, `theme_light_label`, `theme_dark_label`, `theme_system_description`,
`theme_light_description`, `theme_dark_description`, `manage_music`, `configured_folders`,
`unnamed_folder`, `source_access_available`, `source_access_lost`, `source_never_scanned`,
`source_last_scanned`, `source_status_format`, `rescan_source_format`, `remove_source_format`,
`remove_folder`, `remove_folder_message`, `about`, `about_app_name`, `about_logo_description`,
`about_version_format`, `about_view_source`, `about_open_source_libraries`,
`open_source_libraries_loading`, `open_source_libraries_error`, and `open_source_libraries_retry`,
plus `rhythhaus_logo` exactly once. No generated resource handle crosses the boundary. Per-locale
parity and missing, wrong-owner, duplicate-key, and logo controls are required.

## Test And Acceptance Boundary

Settings feature tests own settings layout/policy, source rows, local dialogs, theme-selection
callback emission, slots, About states, feature resources, and build-information generation.
Shared retains real route/source-ID adaptation, authoritative mutation guards/errors, theme store
and root-theme tests, playlist-backup embedding/Back tests, and app-wide catalog plus TagLib
attribution checks. Characterization covers picker hidden/unavailable, clear hidden,
open/dismiss/confirm/close, disabled mutation actions, current/stale IDs, authoritative replacement
between composition and click, and guard changes. About tests split accordingly: catalog
read/config/attribution remain Shared; feature tests cover malformed/empty Failure, immediate
Loading, current-loader retry, exact read/parse callback identity, dispatcher propagation, loader
replacement, and cancellation-resistant stale-token suppression. The root `Box` retains visual
background/root semantics and adds an input-only full-size non-semantic pointer shield as its first
background child. Opaque Settings `Surface`/`Scaffold`/`LazyColumn` is a later foreground sibling;
clear-library and source-removal dialogs are later siblings above both. The shield consumes at
Initial only when sibling hit testing selects it, is never a controls ancestor, and has no
clickable/focusable semantics; `AboutRow` retains normal clickable. The Library shell and Settings
overlay are layered siblings, so the shield catches uncovered overlay coordinates while foreground
controls win hit testing. Causal tests mount a behind full-size clickable sibling first then
production Settings: physical blank tap proves behind=0, physical picker-center tap proves picker=1
and behind=0, root/shield have no click/focus semantics, and scroll/children still work. Miuix
popup/dialog/top-bar tests use stable tags and unmerged trees; disabled mutations prove no click
action/zero callbacks; add explicit disabled semantics only if an approved accessibility contract
requires it.

Start with architecture and characterization RED controls for the absent module/targets and the
forbidden dependency, export, package/namespace, resource ownership, generated-handle, public
surface/KDoc, empty-Koin, and stale-source adaptation cases. GREEN performs one atomic ownership
move. Required acceptance records focused Settings and retained Shared tests; Android/KMP, JVM,
`iosArm64`, and `iosSimulatorArm64` compilation/tests; architecture RED/GREEN and quality gates;
strict `openspec validate feature-first-modularization --strict` under Node 26.7.0; Xcode version
validation; `./init.sh`; and `git diff --check`. These are required future evidence, not claims of
execution by this design.

## Non-Goals

No behavior or visual redesign, Library API dependency, Shared export, feature Koin module,
platform production source, catalog-generation move, theme-store move, route/Back move, SQLDelight
change, or broad state-architecture rewrite is authorized. This design makes no runtime/device,
visual, picker, scanner, playback, or iOS framework claim.
