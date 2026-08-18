# Home Library Content Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the folder-management/scan-outcome section from the Library home page so home shows only library content, keep the add-folder import card only for the empty library, show scanning progress on home only while the library is empty, and make Settings the single folder-management surface (including the terminal scan-outcome panel and lost-access recovery).

**Architecture:** Contract-first UI placement change. `:feature:library:impl` keeps the scan-outcome UI (moved to a public composable next to the existing public `ScanningCard`); `:feature:settings` gains one Shared-supplied `scanOutcomeContent` composable slot and one per-source recovery callback; Shared (`LibraryRoutes` / `LibraryAppShell`) owns report-expansion state keyed by scan-session id and resolves source/callback wiring. No dependency changes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Compose UI test (runComposeUiTest) on JVM, Koin (unchanged), OpenSpec.

**Spec:** `openspec/changes/home-library-content-cleanup/specs/home-library-content-cleanup/spec.md` + design at `docs/superpowers/specs/2026-08-18-home-library-content-cleanup-design.md`

## Global Constraints

- Module graph: `:feature:settings` and `:feature:library:impl` MUST NOT depend on `:shared`; `:shared` alone composes feature implementations.
- Public functions in `:feature:library:impl` and `:feature:settings` require KDoc with `@param` for every parameter (architecture processor enforces).
- No database schema, scanner, playback, dependency, or navigation-model changes.
- No Windows/Linux product support.
- Verification: `openspec validate home-library-content-cleanup --strict`, `./gradlew :shared:jvmTest --configuration-cache`, `:desktopApp:compileKotlin`, `:androidApp:assembleDebug`, `:shared:iosSimulatorArm64Test`, `architectureCheck`, `spotlessApply` then `spotlessCheck`, `detekt`, `git diff --check`.
- Semantic commit message; `progress.md` + `roadmap.md` updated.

---

### Task 1: Remove the manager section from home and make the scan-outcome panel public

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` (two `LibraryHomeContent` call sites, ~lines 530 and 640)
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt`
- Modify: `feature/library/impl/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`

**Interfaces:**
- Consumes: existing `LibraryHomeContent` (current signature has `sources`, `onRescanSource`, `onRemoveSource`, `onRemoveMissingTracks`, `scanErrors` to be removed), existing private `ScanOutcomePanel` (moved verbatim).
- Produces: `LibraryHomeContent` without those 5 parameters; public `ScanOutcomePanel(session: ScanSession, source: LibrarySource?, errors: List<ScanError>, reportVisible: Boolean, mutationsEnabled: Boolean, onToggleReport: () -> Unit, onRescanSource: (LibrarySource) -> Unit, onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit)` in `LibraryRows.kt` (consumed by Task 3).

- [ ] **Step 1: Write the failing home condition tests**

Add to `LibraryHomeContentJvmTest` (replacing `emptyManagerAndActiveScanExposeEmptyAndCancelStates`; the three new tests use the retained `session()`, `managerTrack()`, `AvailableStubPicker`, `labels()` helpers). **Pre-flight ruling: the new tests are initially written against the CURRENT signature — they pass the five soon-to-be-removed parameters (`sources = emptyList()`, `scanErrors = emptyList()`, `onRescanSource = {}`, `onRemoveSource = {}`, `onRemoveMissingTracks = { _, _ -> }`) so the RED run compiles. Step 4 drops those five lines from these tests as part of the signature change.**

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun emptyLibraryShowsImportCardAndActiveScanWithCancel() = runComposeUiTest {
    var cancellations = 0
    setContent {
        Box(Modifier.size(420.dp, 900.dp)) {
            LibraryHomeContent(
                title = "Library",
                subtitle = "",
                tracks = emptyList(),
                browseMode = BrowseMode.Songs,
                folderPickerLauncher = AvailableStubPicker,
                sourcePickerActionVisible = true,
                importMessage = null,
                scanProgress = ScanProgress(session(ScanStatus.Scanning)),
                mutationsEnabled = true,
                currentTrackId = null,
                selectionModeActive = false,
                selectedTrackIds = emptySet(),
                labels = labels(),
                homeBackdrop = null,
                artworkLoader = { null },
                onBrowseModeChange = {},
                onClearLibrary = {},
                onCancelScan = { cancellations++ },
                sources = emptyList(),
                scanErrors = emptyList(),
                onRescanSource = {},
                onRemoveSource = {},
                onRemoveMissingTracks = { _, _ -> },
                onOpenAlbum = {},
                onOpenArtist = {},
                onShowPlaylists = {},
                onPlayTrack = { _, _ -> },
                onToggleSelection = {},
                onStartSelection = {},
                onVisibleTrackIdsChanged = {},
                onScrollPositionChanged = { _, _ -> },
                bottomContentPadding = 0.dp,
            )
        }
    }
    waitForIdle()

    onNode(hasText("Add music folder")).assertExists()
    onNode(hasText("Scanning…")).assertExists()
    onNode(hasText("Cancel")).performClick()
    waitForIdle()

    assertEquals(1, cancellations)
}

@OptIn(ExperimentalTestApi::class)
@Test
fun populatedLibraryHidesImportScanAndOutcomeUi() = runComposeUiTest {
    setContent {
        Box(Modifier.size(420.dp, 900.dp)) {
            LibraryHomeContent(
                title = "Library",
                subtitle = "",
                tracks = listOf(managerTrack()),
                browseMode = BrowseMode.Songs,
                folderPickerLauncher = AvailableStubPicker,
                sourcePickerActionVisible = true,
                importMessage = null,
                scanProgress = ScanProgress(session(ScanStatus.Scanning)),
                mutationsEnabled = true,
                currentTrackId = null,
                selectionModeActive = false,
                selectedTrackIds = emptySet(),
                labels = labels(),
                homeBackdrop = null,
                artworkLoader = { null },
                onBrowseModeChange = {},
                onClearLibrary = {},
                onCancelScan = {},
                sources = emptyList(),
                scanErrors = emptyList(),
                onRescanSource = {},
                onRemoveSource = {},
                onRemoveMissingTracks = { _, _ -> },
                onOpenAlbum = {},
                onOpenArtist = {},
                onShowPlaylists = {},
                onPlayTrack = { _, _ -> },
                onToggleSelection = {},
                onStartSelection = {},
                onVisibleTrackIdsChanged = {},
                onScrollPositionChanged = { _, _ -> },
                bottomContentPadding = 0.dp,
            )
        }
    }
    waitForIdle()

    onAllNodes(hasText("Add music folder")).assertCountEquals(0)
    onAllNodes(hasText("Scanning…")).assertCountEquals(0)
    onAllNodes(hasText("Cancel")).assertCountEquals(0)
}

@OptIn(ExperimentalTestApi::class)
@Test
fun emptyLibraryWithTerminalSessionHidesOutcomeButShowsImport() =
    runComposeUiTest {
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                LibraryHomeContent(
                    title = "Library",
                    subtitle = "",
                    tracks = emptyList(),
                    browseMode = BrowseMode.Songs,
                    folderPickerLauncher = AvailableStubPicker,
                    sourcePickerActionVisible = true,
                    importMessage = null,
                    scanProgress =
                        ScanProgress(session(ScanStatus.Completed)),
                    mutationsEnabled = true,
                    currentTrackId = null,
                    selectionModeActive = false,
                    selectedTrackIds = emptySet(),
                    labels = labels(),
                    homeBackdrop = null,
                    artworkLoader = { null },
                    onBrowseModeChange = {},
                    onClearLibrary = {},
                    onCancelScan = {},
                    sources = emptyList(),
                    scanErrors = emptyList(),
                    onRescanSource = {},
                    onRemoveSource = {},
                    onRemoveMissingTracks = { _, _ -> },
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onShowPlaylists = {},
                    onPlayTrack = { _, _ -> },
                    onToggleSelection = {},
                    onStartSelection = {},
                    onVisibleTrackIdsChanged = {},
                    onScrollPositionChanged = { _, _ -> },
                    bottomContentPadding = 0.dp,
                )
            }
        }
        waitForIdle()

        onNode(hasText("Add music folder")).assertExists()
        onAllNodes(hasText("Scan complete")).assertCountEquals(0)
        onAllNodes(hasText("Remove missing files")).assertCountEquals(0)
    }
```

- [ ] **Step 2: Run the new tests to verify they fail**

Run: `./gradlew :feature:library:impl:jvmTest --tests "com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest" --configuration-cache`
Expected: FAIL — `populatedLibraryHidesImportScanAndOutcomeUi` finds "Add music folder"/"Scanning…"/"Cancel" (the manager card and active scanning card render them) and `emptyLibraryWithTerminalSessionHidesOutcomeButShowsImport` finds "Scan complete" (the manager card renders the outcome panel). `emptyLibraryShowsImportCardAndActiveScanWithCancel` passes already (empty-state import + scanning cards exist today) and serves as a regression guard. Record the RED output.

- [ ] **Step 3: Remove the manager section from `LibraryHomeContent`**

In `LibraryHomeContent.kt`:

1. Delete the parameters `sources: List<LibrarySource>`, `onRescanSource: (LibrarySource) -> Unit`, `onRemoveSource: (LibrarySource) -> Unit`, `onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit`, `scanErrors: List<ScanError>` from the `LibraryHomeContent` signature and its KDoc `@param` entries.
2. Delete the always-rendered lazy item block that calls `LibraryManagerCard(...)` (the block passing `sources`, `scanErrors`, `reportVisible`, `onRescanSource`, `onRemoveSource`, `onRemoveMissingTracks`, `onToggleReport`, `onRecoverSource`).
3. Narrow the scanning-card condition from `if (scanProgress?.isActive == true)` to `if (tracks.isEmpty() && scanProgress?.isActive == true)`.
4. Delete the now-unused private composables `LibraryManagerCard` and `SourceManagerRow` in their entirety (lines ~420-540).
5. Delete the private `ScanOutcomePanel` composable from this file (moved in Step 5) — keep its body verbatim.
6. Remove now-unused imports: `LibrarySource`, `ScanError`, `ScanSession`, `LibrarySourceAccessStatus` (only if no other use remains in the file; `ScanProgress`, `ScanStatus`, `PlatformFolderPickerLauncher`, `LibrarySharedLabels` remain used).

- [ ] **Step 4: Update all `LibraryHomeContent` call sites**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, at BOTH `LibraryHomeContent(` invocations (~lines 530 and 640), delete the five lines `sources = sources,`, `scanErrors = scanErrors,`, `onRescanSource = onRescanSource,`, `onRemoveSource = onRemoveSource,`, `onRemoveMissingTracks = onRemoveMissingTracks,`. (`scanProgress`, `importMessage`, `onCancelScan`, `onClearLibrary`, `folderPickerLauncher`, `sourcePickerActionVisible` stay.)

In `LibraryHomeContentJvmTest.kt`, remove those same five arguments from the five retained direct-call tests (`songsRenderInOrderPreservingDuplicatesAndReportPrimitiveCallbacks`, `homeScrollReportsPrimitivePositions`, `albumsGroupByExactNameSortedCaseInsensitively`, `artistsGroupByExactNameWithAlbumAndTrackCounts`, `browseModePickerDispatchesModeChange`) AND from the three new tests added in Step 1 (the pre-flight ruling: they carried the params only so the RED run compiled).

- [ ] **Step 5: Move `ScanOutcomePanel` to `LibraryRows.kt` as a public composable**

Append to `LibraryRows.kt` (same package `com.eterocell.rhythhaus.library.ui`), with the body copied verbatim from the deleted private composable:

```kotlin
/**
 * Renders the terminal scan-outcome panel with summary, rescan/retry,
 * report toggle, and remove-missing actions.
 *
 * @param session the terminal scan session to display.
 * @param source the source the session scanned, if still configured.
 * @param errors file errors recorded for the displayed scan session.
 * @param reportVisible whether the error report is expanded.
 * @param mutationsEnabled whether scan mutations are currently allowed.
 * @param onToggleReport requests toggling the report expansion.
 * @param onRescanSource requests scanning the source again.
 * @param onRemoveMissingTracks requests removing tracks not seen by the
 *   completed scan.
 */
@Composable
public fun ScanOutcomePanel(
    session: ScanSession,
    source: LibrarySource?,
    errors: List<ScanError>,
    reportVisible: Boolean,
    mutationsEnabled: Boolean,
    onToggleReport: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit,
) {
    // body identical to the deleted private composable
}
```

Ensure `LibraryRows.kt` imports `ScanSession`, `ScanError`, `ScanStatus` (add if missing).

- [ ] **Step 6: Delete obsolete home tests and fixtures**

In `LibraryHomeContentJvmTest.kt`, delete these tests (their behaviors move to Settings/route-adapter tests in Tasks 2-3):
`completedScanExposesReportRescanAndRemoveMissingCallbacks`, `scanReportRemainsExpandedAfterManagerLazyItemIsRecreated`, `scanReportCollapsesWhenDisplayedSessionChanges`, `completedScanManagerStateAndActionsMatchAtCompactAndWideContentWidths`, `addSourceActionIsVisibleAndLaunchesTheAvailablePicker`, `noResultPickerLeavesCallerOwnedManagerStateUnchangedAfterAddSourceAction`, `coordinatorDisabledStateDisablesCompletedScanMutationControls`, `restoredTerminalStatesRenderThroughProductionContentAndRetryWhenRequired`, `managerActionsMatchAtCompactAndWideContentWidths`, `lostAccessSourceOffersRecoveryAndRemovalCallbacks`.

Delete the `managerContent` private composable helper. Delete now-unused private fixtures: `source()`, `scanError()`, `NoResultStubPicker`. Retain: `session()`, `managerTrack()`, `tracks()`, `track()`, `labels()`, `StubPicker`, `AvailableStubPicker`, `twelveTracks()`, `manyAlbumTracks()` (verify each retained helper still has a caller; if `twelveTracks()`/`manyAlbumTracks()` become unused, delete them too).

- [ ] **Step 7: Delete dead source-management strings**

In `feature/library/impl/src/commonMain/composeResources/values/strings.xml` AND `values-zh/strings.xml`, delete: `library_sources`, `library_empty`, `remove_source`, `recover_source`, `source_access_available`, `source_access_lost`. Keep all scan-outcome strings (`scan_completed`, `scan_cancelled`, `scan_failed`, `scan_summary_format`, `rescan`, `retry_scan`, `remove_missing`, `view_scan_report`, `hide_scan_report`, `scan_report_empty`, `scan_report_error_format`) — the public panel still uses them.

- [ ] **Step 8: Run focused home tests to verify they pass**

Run: `./gradlew :feature:library:impl:jvmTest --configuration-cache`
Expected: PASS — new condition tests green, obsolete tests removed, no dangling references.

- [ ] **Step 9: Compile shared JVM target and commit**

Run: `./gradlew :shared:jvmTest --configuration-cache` (compiles shared + feature deps)
Expected: PASS.

```bash
git add -A
git commit -m "refactor: remove folder management section from home page"
```

---

### Task 2: Settings gains the scan-outcome slot and lost-access recovery

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`
- Test: `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`

**Interfaces:**
- Consumes: Task 1's public `ScanOutcomePanel` (via the Shared-supplied slot; settings never calls it directly), existing `SettingsScreen` params.
- Produces: `SettingsScreen(labels, themeMode, sources, sourcePickerActionVisible, sourcePickerAvailable, importMessage, hasImportedTracks, mutationsEnabled, playlistBackupContent, activeScanContent, scanOutcomeContent: (@Composable () -> Unit)?, onThemeModeSelected, onAddMusicFolder, onRescanSource: (String) -> Unit, onRecoverSource: (String) -> Unit, onRemoveSource: (String) -> Unit, onRequestClearLibrary, onShowSettingsAbout, onDismiss)`; internal const `SettingsRecoverPrefix = "settings-recover-"`; settings string `recover_source_format`.

- [ ] **Step 1: Write the failing Settings tests**

In `SettingsScreenSemanticsJvmTest.kt`:

Update the `content` private helper signature to add `scanOutcomeSlot: (@Composable () -> Unit)? = null` (passed after `scanSlot` to the new `scanOutcomeContent` parameter) and `onRecover: (String) -> Unit = {}` (passed to `onRecoverSource`).

Update `slotsRenderInPlaylistScanAndClearOrder` to pass `scanOutcomeSlot = { slot("outcome") }` and assert `assertEquals(listOf("playlist", "scan", "outcome", "clear"), renderedSlots)` (update the filter to include `"outcome"`).

Add:

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
public fun lostAccessSourceRowDispatchesRecoveryAndRemoveById(): Unit =
    runComposeUiTest {
        var recovered = ""
        var removed = ""
        setContent {
            content(
                sources =
                    listOf(
                        SettingsSourceItem(
                            "one", "One", accessAvailable = false, false)),
                onRecover = { recovered = it },
                onRemove = { removed = it })
        }
        onNodeWithTag(
            SettingsRecoverPrefix + "one", useUnmergedTree = true)
            .performClick()
        assertEquals("one", recovered)
        onNodeWithTag(SettingsRemovePrefix + "one", useUnmergedTree = true)
            .performClick()
        onNodeWithTag(SettingsRemoveConfirmTestTag, useUnmergedTree = true)
            .assertExists()
            .performClick()
        assertEquals("one", removed)
    }
```

(`SettingsSourceItem`'s constructor is `(id, displayName, accessAvailable, scanned)` — verify against the existing `item()` helper.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :feature:settings:jvmTest --tests "com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest" --configuration-cache`
Expected: FAIL — `scanOutcomeContent`/`onRecoverSource` parameters and `SettingsRecoverPrefix` do not exist.

- [ ] **Step 3: Add the recovery string**

In `feature/settings/src/commonMain/composeResources/values/strings.xml` AND `values-zh/strings.xml` add:

```xml
<string name="recover_source_format">Choose folder again for %1$s</string>
```

(values-zh: `重新为 %1$s 选择文件夹` — match existing zh file conventions.)

- [ ] **Step 4: Implement the Settings changes**

In `SettingsScreen.kt`:

1. Add parameters after `activeScanContent`: `scanOutcomeContent: (@Composable () -> Unit)? = null` and `onRecoverSource: (String) -> Unit` (place before `onRescanSource` or after `onRemoveSource` per existing order; update KDoc `@param` entries).
2. In the `LazyColumn`, directly after the existing `activeScanContent?.let { item { it() } }` block, render:

```kotlin
scanOutcomeContent?.let { item { it() } }
```

3. Add `internal const val SettingsRecoverPrefix = "settings-recover-"` next to the existing `SettingsRescanPrefix`/`SettingsRemovePrefix` constants.
4. Change `ConfiguredSourceRow` to accept `onRecover: () -> Unit` and branch on lost access — replace the rescan `IconButton` block so that (**pre-flight ruling: use `Icons.Default.FolderOpen` if it is in the module's current icon set; if not, keep `Icons.Default.Refresh` — the recovery semantics are carried by the test tag, content description, and onClick. No dependency changes are allowed**):

```kotlin
val lostAccess = presentation.access == SettingsSourceAccess.Lost
IconButton(
    onClick = if (lostAccess) onRecover else onRescan,
    enabled = mutationsEnabled,
    modifier =
        Modifier.testTag(
            if (lostAccess) SettingsRecoverPrefix + source.id
            else SettingsRescanPrefix + source.id)
            .semantics {
                if (!mutationsEnabled) disabled()
            },
    backgroundColor = Color.Transparent,
    minWidth = 44.dp,
    minHeight = 44.dp) {
        Icon(
            if (lostAccess) Icons.Default.FolderOpen else Icons.Default.Refresh,
            stringResource(
                if (lostAccess) Res.string.recover_source_format
                else Res.string.rescan_source_format, displayName),
            tint = HausColors.current.ink.copy(alpha = alpha),
            modifier = Modifier.size(20.dp))
    }
```

5. Update the `ConfiguredSourceRow` call site to pass `onRecover = { onRecoverSource(source.id) }`.

- [ ] **Step 5: Run focused Settings tests to verify they pass**

Run: `./gradlew :feature:settings:jvmTest --configuration-cache`
Expected: PASS.

```bash
git add -A
git commit -m "feat: add scan outcome slot and lost-access recovery to settings"
```

---

### Task 3: Wire the Shared route layer and route-adapter tests

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` (`RouteOverlays` call site, ~line 408)
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsRouteAdapterJvmTest.kt`

**Interfaces:**
- Consumes: Task 1 public `ScanOutcomePanel`; Task 2 `SettingsScreen` params `scanOutcomeContent`, `onRecoverSource`.
- Produces: `LibraryRouteOverlays` gains `scanErrors: List<ScanError> = emptyList()` and `onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit = { _, _ -> }` (defaults keep unrelated test call sites compiling); report-expansion state `remember(scanProgress?.session?.id)` in the Settings branch; `onRecoverSource = { folderPickerLauncher.launch() }` wiring.

- [ ] **Step 1: Write the failing route-adapter tests**

In `SettingsRouteAdapterJvmTest.kt`:

1. Extend the `Harness` composable with `scanErrors: List<ScanError> = emptyList()` and `onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit = { _, _ -> }`; pass both to `LibraryRouteOverlays`.
2. Add private fixtures:

```kotlin
private fun scanSession(
    status: ScanStatus,
    id: String = "scan",
): ScanSession =
    ScanSession(
        id = id,
        sourceId = "source",
        status = status,
        startedAtEpochMillis = 1L,
        foldersVisited = 2,
        filesVisited = 4,
        tracksAdded = 2,
        tracksUpdated = 1,
        filesSkipped = 1,
        terminalMessage =
            if (status == ScanStatus.Failed) "Folder is unavailable"
            else null,
    )

private fun scanError(session: ScanSession): ScanError =
    ScanError(
        id = "error",
        scanId = session.id,
        sourceLocalKey = "broken.mp3",
        displayPath = "broken.mp3",
        reason = "Unsupported file",
        recoverable = true,
        createdAtEpochMillis = 1L,
    )
```

3. Add these tests (each mounts `Box(Modifier.size(500.dp, 1600.dp)) { Harness(...) }` so the outcome panel is within the semantics tree):

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun settingsRendersTerminalOutcomeWithReportRescanAndRemoveMissing() =
    runComposeUiTest {
        val source = source("source", scanned = true)
        val session = scanSession(ScanStatus.Completed)
        val rescanned = mutableListOf<LibrarySource>()
        val missingRemoved =
            mutableListOf<Pair<LibrarySource, ScanSession>>()
        setContent {
            Box(Modifier.size(500.dp, 1600.dp)) {
                Harness(
                    sources = listOf(source),
                    scanProgress = ScanProgress(session),
                    scanErrors = listOf(scanError(session)),
                    onRescan = { rescanned += it },
                    onRemoveMissingTracks = { s, sess ->
                        missingRemoved += s to sess
                    },
                )
            }
        }
        waitForIdle()

        onNode(hasText("Scan complete")).assertExists()
        onNode(hasText("View scan report")).performClick()
        onNode(hasText("broken.mp3: Unsupported file")).assertExists()
        onAllNodes(hasText("Rescan"))[0].performClick()
        onNode(hasText("Remove missing files")).performClick()
        waitForIdle()

        assertEquals(listOf(source), rescanned)
        assertEquals(listOf(source to session), missingRemoved)
    }

@OptIn(ExperimentalTestApi::class)
@Test
fun settingsOutcomeReportSurvivesListItemRecreation() =
    runComposeUiTest {
        val source = source("source", scanned = true)
        setContent {
            Box(Modifier.size(500.dp, 800.dp)) {
                Harness(
                    sources = listOf(source),
                    scanProgress =
                        ScanProgress(scanSession(ScanStatus.Completed)),
                    scanErrors =
                        listOf(scanError(scanSession(ScanStatus.Completed))),
                )
            }
        }
        waitForIdle()

        onNode(hasText("View scan report")).performClick()
        onNode(hasText("broken.mp3: Unsupported file")).assertExists()
        onNode(hasScrollToIndexAction()).performTouchInput {
            repeat(20) { swipeUp() }
        }
        waitForIdle()
        onAllNodes(hasText("Hide scan report")).assertCountEquals(0)
        onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        waitForIdle()

        onNode(hasText("Hide scan report")).assertExists()
        onNode(hasText("broken.mp3: Unsupported file")).assertExists()
    }

@OptIn(ExperimentalTestApi::class)
@Test
fun settingsOutcomeReportCollapsesWhenDisplayedSessionChanges() =
    runComposeUiTest {
        val source = source("source", scanned = true)
        var displayedSession by
            mutableStateOf(scanSession(ScanStatus.Completed))
        setContent {
            Box(Modifier.size(500.dp, 1600.dp)) {
                Harness(
                    sources = listOf(source),
                    scanProgress = ScanProgress(displayedSession),
                    scanErrors = listOf(scanError(displayedSession)),
                )
            }
        }
        waitForIdle()

        onNode(hasText("View scan report")).performClick()
        onNode(hasText("Hide scan report")).assertExists()
        displayedSession = scanSession(ScanStatus.Completed, id = "next-scan")
        waitForIdle()

        onNode(hasText("View scan report")).assertExists()
    }

@OptIn(ExperimentalTestApi::class)
@Test
fun settingsTerminalStatesRenderAndRetryWhenRequired() = runComposeUiTest {
    val source = source("source", scanned = true)
    var status by mutableStateOf(ScanStatus.Completed)
    val rescanned = mutableListOf<LibrarySource>()
    setContent {
        Box(Modifier.size(500.dp, 1600.dp)) {
            Harness(
                sources = listOf(source),
                scanProgress = ScanProgress(scanSession(status)),
                onRescan = { rescanned += it },
            )
        }
    }
    waitForIdle()

    onNode(hasText("Scan complete")).assertExists()
    status = ScanStatus.Failed
    waitForIdle()
    onNode(hasText("Scan failed")).assertExists()
    onNode(hasText("Retry scan")).performClick()
    status = ScanStatus.Cancelled
    waitForIdle()
    onNode(hasText("Scan cancelled")).assertExists()
    onNode(hasText("Retry scan")).performClick()

    assertEquals(listOf(source, source), rescanned)
}

@OptIn(ExperimentalTestApi::class)
@Test
fun disabledCoordinatorDisablesSettingsOutcomeMutationControls() =
    runComposeUiTest {
        val source = source("source", scanned = true)
        setContent {
            Box(Modifier.size(500.dp, 1600.dp)) {
                Harness(
                    sources = listOf(source),
                    scanProgress =
                        ScanProgress(scanSession(ScanStatus.Completed)),
                    mutationsEnabled = false,
                )
            }
        }
        waitForIdle()

        onNode(hasText("View scan report")).performClick()
        onAllNodes(hasText("Rescan"))[0].assertIsNotEnabled()
        onNode(hasText("Remove missing files")).assertIsNotEnabled()
    }
```

Note: `hasText` matches substring text; the summary line uses `scan_summary_format`, so "Scan complete" is a distinct node. If `onAllNodes(hasText("Rescan"))[0]` is ambiguous with the source-row refresh button, prefer `onAllNodes(hasText("Rescan")).onFirst()` or assert count before clicking.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :shared:jvmTest --tests "com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest" --configuration-cache`
Expected: FAIL — outcome panel not rendered (no `scanOutcomeContent` wiring), `onRecoverSource` missing.

- [ ] **Step 3: Implement the Shared wiring**

In `LibraryRoutes.kt`:

1. Add to `LibraryRouteOverlays` signature: `scanErrors: List<ScanError> = emptyList(),` and `onRemoveMissingTracks: (LibrarySource, ScanSession) -> Unit = { _, _ -> },` (update KDoc `@param` entries; import `ScanError`).
2. In the Settings branch (the `LibraryRoute.Settings ->` arm rendering `SettingsScreen`), add near the top of the branch:

```kotlin
var reportVisible by
    remember(scanProgress?.session?.id) { mutableStateOf(false) }
val terminalSession = scanProgress?.session
val scanOutcomeContent =
    if (terminalSession != null && scanProgress?.isActive != true) {
        {
            ScanOutcomePanel(
                session = terminalSession,
                source =
                    sources.firstOrNull { it.id == terminalSession.sourceId },
                errors = scanErrors,
                reportVisible = reportVisible,
                mutationsEnabled = mutationsEnabled,
                onToggleReport = { reportVisible = !reportVisible },
                onRescanSource = onRescanSource,
                onRemoveMissingTracks = onRemoveMissingTracks,
            )
        }
    } else {
        null
    }
```

3. Pass to the `SettingsScreen` invocation: `scanOutcomeContent = scanOutcomeContent,` and `onRecoverSource = { folderPickerLauncher.launch() },`.
4. Ensure imports for `ScanOutcomePanel` and `ScanError` are present in `LibraryRoutes.kt`.

In `LibraryAppShell.kt`, at the `LibraryRouteOverlays(` invocation in `RouteOverlays` (~line 408), add:

```kotlin
scanErrors = scanErrors,
onRemoveMissingTracks = onRemoveMissingTracks,
```

- [ ] **Step 4: Run focused route-adapter tests to verify they pass**

Run: `./gradlew :shared:jvmTest --tests "com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest" --configuration-cache`
Expected: PASS.

```bash
git add -A
git commit -m "feat: route terminal scan outcome and recovery through settings"
```

---

### Task 4: Verification, evidence, and commit

**Files:**
- Modify: `openspec/changes/home-library-content-cleanup/tasks.md`
- Modify: `progress.md`, `roadmap.md`

- [ ] **Step 1: Run strict OpenSpec validation**

Run: `openspec validate home-library-content-cleanup --strict`
Expected: `Change 'home-library-content-cleanup' is valid`.

- [ ] **Step 2: Run the full verification matrix**

Run:
```bash
./gradlew :shared:jvmTest --configuration-cache
./gradlew :desktopApp:compileKotlin --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
./gradlew architectureCheck --configuration-cache
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
git diff --check
```
Expected: all pass; iOS failure only if `xcodebuild` is unavailable (record as blocker, do not claim iOS passed). Run `spotlessApply` before `spotlessCheck`; run `spotlessCheck` and `detekt` as separate commands so Detekt execution is independently proven.

- [ ] **Step 3: Review the scoped diff**

Review the complete diff against the spec scenarios: home hides all folder/scan UI when tracks exist (compact and wide), home import card only when empty, home scanning card only when empty + active, Settings renders outcome with report/rescan/remove-missing and lost-access recovery, no dependency/database/scanner/playback/navigation-model changes, no Windows/Linux support. Verify no unrelated artifact changes.

- [ ] **Step 4: Record evidence and commit**

Update `openspec/changes/home-library-content-cleanup/tasks.md` — check off all tasks and replace the Verification Evidence placeholder with exact command outputs. Update `progress.md` (handoff record, route `openspec+superpowers`, evidence, next safe action) and `roadmap.md` (concise entry for the completed change).

```bash
git add -A
git commit -m "feat: keep folder management off the home page"
```

## Self-Review Notes

- Spec coverage: every spec requirement maps to Task 1 (home placement), Task 2 (Settings slot + recovery), Task 3 (wiring + preserved callbacks, session-keyed report state), Task 4 (scope diff review).
- No placeholders: all test bodies and production edit instructions are concrete; the only verbatim-copy instruction (Task 1 Step 5 body) references code the executor reads in the same repo.
- Type consistency: `ScanOutcomePanel` signature identical across Tasks 1, 2, 3; `scanOutcomeContent`, `onRecoverSource`, `SettingsRecoverPrefix`, `recover_source_format` names consistent; `LibraryRouteOverlays` defaults keep unrelated call sites compiling.
