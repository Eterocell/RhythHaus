# Track List Artwork Collapse - Task 3 Verification Report

## Status

`DONE_WITH_MANUAL_LIMITATIONS`

The implementation and supported JVM/desktop/Android verification pass. Remaining limitations are the existing iOS common-test `Thread` incompatibility and unverified synthetic runtime gestures and pixel/CJK screenshot inspection. The initial final review's no-artwork finding was fixed in `eeae263`; the post-fix final Oracle gate passed with zero Critical or Important findings.

## Scope and preservation

- Consumed implementation commits `01a1011`, `4ec83e9`, and Oracle-finding fix `eeae263`, plus their task/focused re-reviews.
- Preserved the Task 1 and Task 2 reports without edits.
- Preserved the approved design and plan without edits.
- Changed only roadmap item 21; item 22 remains byte-for-byte `- [ ] build(android): 支持 SplitAPK`.
- Did not modify production code, tests, dependencies, generic SDD reports, or the unrelated iOS test.
- Did not archive OpenSpec, stage, commit, amend, or push.
- `.gitignore:21` ignores `.superpowers/`. Task 2's report is tracked, while `.superpowers/sdd/progress.md`, Task 1's report, and this new Task 3 report appear as ignored (`!!`); the controller must use explicit `GIT_MASTER=1 git add -f` for intended ignored evidence.

## Fresh prescribed command evidence

### Strict OpenSpec validation

```bash
openspec validate track-list-artwork-collapse --strict
```

Exit: `0`.

```text
Change 'track-list-artwork-collapse' is valid
```

### JVM tests, desktop compile, and Android debug assembly

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Exit: `0`.

```text
> Task :shared:jvmTest
> Task :desktopApp:compileKotlin
> Task :androidApp:assembleDebug

BUILD SUCCESSFUL in 37s
110 actionable tasks: 41 executed, 7 from cache, 62 up-to-date
Configuration cache entry stored.
```

The run emitted the existing warning:

```text
w: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt:474:17 'fun setArtworkData(p0: ByteArray?): MediaMetadata.Builder' is deprecated. Deprecated in Java.
```

### Xcode availability

```bash
/usr/bin/xcrun xcodebuild -version
```

Exit: `0`.

```text
Xcode 26.6
Build version 17F113
```

### iOS simulator tests

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Exit: non-zero. iOS main compilation completed first:

```text
> Task :shared:compileKotlinIosSimulatorArm64
> Task :shared:iosSimulatorArm64MainKlibrary
> Task :shared:compileTestKotlinIosSimulatorArm64 FAILED
```

Exact compiler errors and result:

```text
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:64:28 Unresolved reference 'Thread'.
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:340:27 Unresolved reference 'Thread'.

Execution failed for task ':shared:compileTestKotlinIosSimulatorArm64' (registered in build file 'shared/build.gradle.kts').
> Compilation finished with errors

BUILD FAILED in 11s
42 actionable tasks: 14 executed, 28 up-to-date
Configuration cache entry stored.
```

This is the known unrelated common-test JVM-only `Thread` blocker confirmed from fresh output. It was not modified, and no iOS simulator test pass is claimed.

### Diagnostics and initial diff hygiene

`lsp_status` reported `kotlin-ls: missing` and zero active LSP clients. Installation was previously declined, so executable Gradle tests and compilation are the Kotlin language checks.

```bash
GIT_MASTER=1 git diff --check
```

Exit: `0`, no output.

```bash
GIT_MASTER=1 git status --short
```

Initial output before Task 3 evidence edits:

```text
 M openspec/changes/track-list-artwork-collapse/tasks.md
 M roadmap.md
```

These were the coordinator/user-owned completed Task 1/2 checkboxes and roadmap items 21/22. They were preserved and updated only within the Task 3 boundaries.

## Runtime visual-QA attempt

The desktop target was launched with:

```bash
./gradlew :desktopApp:run --configuration-cache
```

Orca reported a ready runtime and granted Accessibility/Screenshot permissions. The existing local library provided representative data without changing user state.

Captured states:

- compact `800x600` album with artwork: `A Thousand Suns`;
- compact `800x600` album without artwork: `Apologize`;
- wide `1728x1084` artist with artwork: `7!!`;
- attempted compact/wide forward collapse and wide reverse expansion captures.

The accessibility trees confirmed real Compose content for artwork, title/subtitle, selectable tracks, back button, and scrollbar. The window zoom took effect asynchronously, changing the window from `800x600` to `1728x1084` and allowing compact/wide state coverage.

Runtime limitation: Orca reported pointer scroll/drag delivery as synthetic and unverified, and the accessibility scroll value remained `0`. Therefore the attempted sequence is not evidence that partial collapse, fully collapsed list scrolling, or reverse expansion occurred. Those gestures remain a manual/controller validation requirement.

No reference mock or known-good baseline exists, so the visual-QA image-diff command and hotspot JSON are not applicable.

## Dual visual review

### Initial Pass A - design-system and functional integrity

Verdict: `PASS`; medium overall confidence, high source confidence.

Confirmed from source/tests:

- artwork and list placement derive from one `ArtworkCollapseSnapshot.headerHeightPx`;
- exactly one artwork/Miuix nested-scroll owner is attached;
- negative pre-scroll collapses one-for-one and positive post-scroll expands the same offset;
- resize uses current constraints and clamps persisted offset;
- real `LazyTrackArtworkImage`, scrim, title chips, paper fade, safe-start 44 dp back target, scrollbar, and Now Playing wiring remain;
- no-artwork pages retain the Miuix glass/title/divider path.

No source-level blocker was found in that initial pass, and the reviewer explicitly did not claim runtime gesture success or pixel inspection. The later broad Oracle review superseded the initial no-artwork conclusion by finding that representative identity selected artwork mode before lazy loading resolved; `eeae263` fixes that Important issue and the focused re-review below passes.

### Pass B - visual fidelity and CJK precision

Verdict: `REVISE/RETEST`; low confidence because pixel inspection was unavailable.

OCR/metadata confirmed representative content in the captures, including `A Thousand Suns`, `Apologize`, `Ado`, `Aimer`, `Alan Walker`, Chinese UI labels, and Japanese track/album text. It could not validate exact pixel gaps, alpha/fade states, CJK glyph clipping/baselines, scrollbar placement, Now Playing placement, or differences between attempted gesture frames.

This is an evidence limitation, not a located product defect. An image-capable reviewer or human must inspect the captures and repeat gestures with verified pointer input before a visual PASS can be claimed.

## OpenSpec completion state

- `3.1`: complete; every prescribed command ran and exact outcomes are recorded.
- `3.2`: pending; source review and runtime state capture were attempted, but runtime gestures and pixel/CJK inspection are incomplete.
- `3.3`: pending; Task 1/2 reviews are clean, but the controller final broad review is intentionally deferred.
- `3.4`: pending; evidence is prepared, but controller completion/review and the final evidence commit remain outstanding.

## Controller handoff

Final review status:

- Initial Oracle gate found one Important no-artwork classification defect.
- Commit `eeae263` added explicit lazy artwork `Loading`, `Available`, and `Unavailable` states; loading, absent, and failed states remain Miuix-owned, while resolved bytes activate coordinated collapse.
- Focused re-review: specification PASS and quality APPROVED, with two non-blocking Minor test-hardening notes.
- Fresh post-fix focused and full supported verification passed; iOS remained blocked only by the existing `Thread` references.
- Final post-fix Oracle gate: PASS with zero Critical and zero Important findings; safe to deliver with the documented manual visual and iOS limitations.

Next actions:

1. Use an image-capable or human reviewer on compact/wide album/artist/no-artwork captures.
2. Repeat forward collapse, fully collapsed list scrolling, and reverse expansion with verified pointer/trackpad input; check no transient/full gap, title chips, paper fade, back target, scrollbar, Now Playing, and CJK text.
3. Archive the OpenSpec change only on explicit request.
4. Continue roadmap item 22 through a separate approved spec and plan.
5. Use verified pointer input and an image-capable reviewer for final manual visual acceptance.

No final evidence commit was made in this task.

## Post-Oracle fix verification - commit `eeae263`

The initial Oracle broad review identified one Important defect: a representative track ID was treated as proof that artwork existed, so lazy-loading no-artwork pages could select the artwork-owned collapse path and lose the required Miuix fallback. Commit `eeae263` (`fix: classify drill-down artwork after lazy loading`) corrected that behavior without changing the intended geometry:

- `TrackArtworkLoadState` explicitly distinguishes `Loading`, `Available(bytes)`, and `Unavailable`;
- eager or successfully lazy-loaded bytes resolve to `Available` and select coordinated artwork collapse;
- loading, absent, failed, or decoded-unavailable artwork selects Miuix;
- cancellation is rethrown rather than converted to `Unavailable`;
- the chrome receives resolved bytes only when the shared classifier selected artwork mode.

### Fresh strict validation

```bash
openspec validate track-list-artwork-collapse --strict
```

Exit: `0`.

```text
Change 'track-list-artwork-collapse' is valid
```

### Fresh focused four-suite verification

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' \
  --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
```

Exit: `0`.

```text
> Task :shared:jvmTest UP-TO-DATE

BUILD SUCCESSFUL in 471ms
26 actionable tasks: 4 executed, 22 up-to-date
Configuration cache entry reused.
```

### Fresh supported matrix

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Exit: `0`.

```text
> Task :desktopApp:compileKotlin
> Task :shared:jvmTest
> Task :androidApp:assembleDebug

BUILD SUCCESSFUL in 9s
101 actionable tasks: 12 executed, 89 up-to-date
Configuration cache entry reused.
```

The only source warning remained the existing Android `MediaMetadata.Builder.setArtworkData` deprecation at `PlaybackEngine.android.kt:474:17`.

### Fresh Xcode and iOS attempt

```bash
/usr/bin/xcrun xcodebuild -version
```

Exit: `0`.

```text
Xcode 26.6
Build version 17F113
```

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Exit: non-zero after iOS main compilation completed.

```text
> Task :shared:compileKotlinIosSimulatorArm64
> Task :shared:iosSimulatorArm64MainKlibrary
> Task :shared:compileTestKotlinIosSimulatorArm64 FAILED

e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:64:28 Unresolved reference 'Thread'.
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:340:27 Unresolved reference 'Thread'.

BUILD FAILED in 5s
33 actionable tasks: 6 executed, 27 up-to-date
Configuration cache entry reused.
```

This remains the same unrelated common-test blocker. It was not modified, and no iOS simulator test pass is claimed.

### Fresh diff hygiene

```bash
GIT_MASTER=1 git diff --check
```

Exit: `0`, no output.

### Focused post-fix re-review

- Specification compliance: `PASS`.
- Code quality: `APPROVED`.
- Non-blocking Minor 1: `loadTrackArtworkState` correctly rethrows `CancellationException`, but there is no direct cancellation regression test.
- Non-blocking Minor 2: `albumAndArtistRepresentativeIdentityDoesNotOwnScrollWithoutResolvedArtwork` exercises the shared classifier with route-shaped inputs; its name overstates route-level integration coverage.
- No Critical, Important, or blocking quality finding remains in the focused fix scope.

The controller final broad Oracle/post-implementation review remains pending and is not claimed complete.

### Reconciled OpenSpec and visual status

- `3.1` remains complete: all required automated checks were rerun after `eeae263`, with exact outcomes above.
- `3.2` remains pending: the fix restores correct source behavior for resolved no-artwork pages, but synthetic gestures and pixel/CJK inspection are still unverified; no runtime visual PASS is claimed.
- `3.3` remains pending: the focused fix re-review passed, but the controller final broad review has not run after this evidence refresh.
- `3.4` remains pending until the controller completes final review/evidence handling and the deferred evidence commit.
