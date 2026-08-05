# Task 5.2 Final Acceptance Report

Status: DONE_WITH_CONCERNS

## Reviewed snapshot

The reviewed Task 5.2 source snapshot is uncommitted and unstaged. The index was
empty when the exact committed-plan 95-path parser ran. That parser passed after
`spotlessApply`: it verified the planning baseline, exact category counts, 95
unique manifest paths, the required tracked `SettingsScreenTest.kt` deletion,
and the three explicit retained baseline paths. No formatter-introduced path
outside the approved 95 implementation paths was observed.

## Final observed evidence

- Convention rebuild passed with strict configuration-cache problems and
  `--no-parallel`.
- Focused `ArchitectureCheckPluginFunctionalTest` passed; retained XML is 71
  tests, 6 skipped, 0 failures, 0 errors.
- The requested unfiltered feature/shared matrix passed: feature JVM XML
  115/0/0/0, Android-host 93/0/0/0, iOS-simulator 83/0/0/0; Shared JVM
  307/0/0/0 and iOS-simulator 236/0/0/0 (tests/skipped/failures/errors).
  Settings has five retained methods: `SettingsPlaylistBackupEmbeddingTest`
  XML is 5/0/0/0.
- Root strict `architectureCheck` passed twice. Both runs reported
  `Reusing configuration cache.`
- `spotlessApply`, separate strict `spotlessCheck`, and separate strict `detekt`
  passed.
- Strict named OpenSpec validation passed under explicit NVM Node v26.7.0.
  `git diff --check` passed with no output.
- `/usr/bin/xcrun xcodebuild -version` reported Xcode 26.6, build 17F113.

Historical evidence is retained in `task-5.2-report.md`. The earlier 46
class-loading failures and the earlier eight-TestKit failures are superseded as
final-snapshot test evidence by the later clean convention/TestKit and
unfiltered feature/platform runs above. They remain historical failures, not
retroactive passes.

## Acceptance blockers

Full acceptance is not granted.

- `:androidApp:assembleDebug` failed at `:androidApp:mergeLibDexDebug`: D8 found
  `com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocuments_androidKt`
  in both Shared and `:feature:playlists:impl` dex archives. Desktop compilation
  in the same command completed.
- The documented unsigned generic iOS Simulator Swift consumer build failed:
  `PlaylistBackupDocumentPolicies.swift:20:27: cannot find
  'PlatformPlaylistBackupDocumentsKt' in scope`, followed by `** BUILD FAILED **`.
- Mandatory `./init.sh` ran once and ended with the same Android D8 duplicate
  class failure. It did not hang or exceed the 20-minute timeout.

Compile/test evidence does not establish runtime, device, simulator interaction,
visual behavior, playback behavior, or document-picker runtime behavior; none is
claimed.

## Remaining gates

The following remain independent acceptance gates: repair and rerun the failed
Android and Swift consumer gates; obtain independent behavioral review and exact
path/ownership audit; stage only after approval; create the implementation
commit; independently review the evidence/ledger; stage the ledger package; and
create the evidence closeout commit. No tracked ledger, plan, OpenSpec,
`progress.md`, or `roadmap.md` file was changed. No staging or commit was
performed.

## Platform-facade correction - 2026-08-07

Status: DONE_WITH_CONCERNS

The prior acceptance RED conditions were corrected without touching the plan,
brief, tracked ledgers, or unrelated paths. The feature Android and JVM facade
files now have feature-specific filenames while their public factory names and
behavior remain unchanged. Common Shared now owns the literal public
`PlaylistBackupMimeType` and `PlaylistBackupMaxBytes` constants with
declaration-specific KDoc; iOS retains the adapter, ABI declarations, and
internal terminal mapping without duplicate constant declarations.

Exact rerun evidence:

```text
:feature:playlists:impl:jvmTest --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 115/0/0/0.
:feature:playlists:impl:testAndroidHostTest --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 93/0/0/0.
:androidApp:assembleDebug --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 227 actionable tasks; duplicate D8 facade absent.
:shared:compileKotlinJvm :desktopApp:compileKotlin --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 98 actionable tasks.
:shared:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 98 actionable tasks.
:shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.playlistbackup.IOSPlaylistBackupAbiFacadeTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, 1/0/0/0.
:shared:linkDebugFrameworkIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel: PASS, constants owned under PlatformPlaylistBackupDocumentsKt only.
/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build: PASS, ** BUILD SUCCEEDED **.
Rebound exact 95-path parser: PASS, 95 manifest paths and 95 unique; explicit deletion present; 3 retained baseline paths present; index empty; old feature facade paths absent; path-set equality passed.
git diff --check: PASS, no output.
```

Full Task 5.2 acceptance is not claimed. Runtime/device behavior, simulator
interaction, visual behavior, playback behavior, and document-picker runtime
remain unverified. Independent behavioral review, exact path/ownership audit,
staging/commit, and evidence-closeout gates remain outstanding. No staging or
commit was performed.

## Authoritative final-snapshot acceptance - 2026-08-07

Status: DONE_WITH_CONCERNS

Snapshot: current uncommitted Task 5.2 source against approved planning baseline
`c0e1e7b9d07679d7beecd530d1958e50b58b1e3e`, including the independently
approved platform-facade correction. Historical RED evidence remains above and
is explicitly superseded, not deleted, by this final snapshot.

All requested automated acceptance gates now pass:

```text
Convention boundary rebuild + full ArchitectureCheckPluginFunctionalTest: PASS.
Exact plan focused feature/Shared/IOS ABI selectors: PASS.
Unfiltered XML totals: feature JVM 115/0/0/0; feature Android host 93/0/0/0; feature iOS Simulator 83/0/0/0; Shared JVM 307/0/0/0; Shared iOS Simulator 236/0/0/0; core database JVM 3/0/0/0.
Feature Android/iOS arm64/iOS Simulator, Shared JVM/iOS arm64/iOS Simulator, desktop compile, and Android debug assemble: PASS.
Architecture processor test: PASS (no test source).
architectureCheck twice: PASS; both runs reused configuration cache.
spotlessApply: PASS; no out-of-manifest implementation status paths; 95 status paths; index empty.
spotlessCheck and detekt: PASS.
Strict OpenSpec under Node v26.7.0: PASS.
Rebound exact 95-path parser: PASS; 9 categories, 95 unique paths, explicit deletion, 3 retained baseline paths, old feature facades absent, path-set equality, index empty.
git diff --check: PASS.
Xcode 26.6 (17F113): PASS; unsigned generic Simulator build PASS; exact iPhone 17 Simulator test PASS, 8 tests/0 failures.
./init.sh with 20-minute timeout: PASS; no duplicate platform facade failure.
Current Shared.framework header: PlatformPlaylistBackupDocumentsKt exclusively owns PlaylistBackupMimeType and PlaylistBackupMaxBytes; no _iosKt constant ownership.
```

This is evidence only, not a runtime/device/visual/playback/picker-interaction
claim. Independent review has approved the platform-facade correction, but full
combined independent review, staging, implementation commit, evidence review,
and evidence-closeout commit remain pending. No staging or commit was
performed.

## Implementation and evidence closeout reconciliation - 2026-08-07

Status: DONE_WITH_CONCERNS

The implementation commit is
`fc1b96f858408c8dfd07221d5fe85ae3e20ced63` (`refactor: extract playlists
feature`), based on approved planning commit
`c0e1e7b9d07679d7beecd530d1958e50b58b1e3`. Final combined independent review is
`PASS / APPROVED` after one evidence-only Objective-C/Swift owner wording
correction. Historical failures remain explicitly superseded, not deleted.

Closeout evidence is authoritative: feature JVM/Android-host/iOS
`115/93/83`, Shared JVM/iOS `307/236`, core database JVM `3`, Settings `5`,
Xcode `8/0`, and architecture functional `71` with `6` expected skips; all
stated failures/errors are zero and relevant skips are zero. Convention,
focused/unfiltered platform, compilation/assembly, architecture, twice-reused
cache, Spotless, Detekt, strict named OpenSpec, exact 95-path, diff, Xcode
generic build/exact iPhone 17 test, and `./init.sh` gates pass.

Header ownership wording remains exact: Objective-C interface
`SharedPlatformPlaylistBackupDocumentsKt` exports Swift owner
`PlatformPlaylistBackupDocumentsKt`, which owns both constants; no
`PlatformPlaylistBackupDocuments_iosKt` constant owner exists.

No Android/iOS physical-device runtime, desktop UI launch, rendered visual QA,
live picker/document interaction, or playback runtime is claimed. The full
combined independent review is complete, but the separate documentation/evidence
closeout commit remains pending. No closeout SHA is claimed; no staging or
commit was performed.
