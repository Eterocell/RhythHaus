# Task 2 Report: Validated Playback Session DataStore

## Scope

Implemented only Task 2 playback-session storage files. Existing Task 1 codec/snapshot files, theme preference storage, playback engines/controller/coordinator/DI/App, dependencies, SQLDelight, OpenSpec, progress, and roadmap were not modified.

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.kt`
  - Adds `PlaybackSessionStore`, `DataStorePlaybackSessionStore`, validated full-snapshot reads, pre-edit save validation, and one atomic Preferences edit.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.android.kt`
  - Adds an independent Android DataStore factory using `playback_session.preferences_pb` and `ReplaceFileCorruptionHandler { emptyPreferences() }`.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.ios.kt`
  - Adds an independent iOS application-support DataStore factory with the required filename and corruption handler.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.jvm.kt`
  - Adds an independent JVM/macOS application-support DataStore factory with the required filename and corruption handler.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStoreJvmTest.kt`
  - Covers defaults, all-field round-trip, negative save clamping, invalid queue/current save preservation, malformed queue/current/position/repeat/shuffle full-default fallback, and current-ID consistency.

## Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache
```

Result: expected `BUILD FAILED` at `:shared:compileTestKotlinJvm`. The new direct test could not resolve `DataStorePlaybackSessionStore`, `save`, or `read` because the session store did not yet exist.

## GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 2s`; focused JVM store suite passed.

## Platform compile evidence

Command:

```bash
./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Result: `BUILD SUCCESSFUL in 20s`. JVM, Android main, and iOS Simulator ARM64 main compiled. The only compiler warning was the pre-existing Android `MediaMetadata.Builder.setArtworkData` deprecation in `PlaybackEngine.android.kt`.

## Diagnostics and diff check

- `lsp_diagnostics` was requested for all five changed Kotlin files, but `kotlin-ls` is not installed and the user previously declined installation. Gradle compilation is the available source diagnostic evidence.
- `GIT_MASTER=1 git diff --check`: pass with no output.

## Commit

- Commit: `cff6d6d93f243705e5dde49888fb5be0dfe0c6b6`
- Message: `feat: persist playback session snapshots`
- Includes the required Sisyphus footer and co-author trailer.

## Self-review

- Storage uses dedicated keys and a dedicated file; it does not share the theme DataStore object or filename.
- Queue and current IDs are encoded before `DataStore.edit`; an inconsistent current ID or codec violation throws before durable state can change.
- All five fields are written in one `edit` transaction.
- Reads validate queue decoding, zero-or-one current ID, current membership in the queue, non-negative persisted position, and exact enum names. Any malformed field returns the full empty/default snapshot rather than a partial snapshot.
- Save-time negative positions are clamped to zero as specified.
- Each platform actual uses the exact filename `playback_session.preferences_pb` and `ReplaceFileCorruptionHandler { emptyPreferences() }`.
- No type suppressions, dependency changes, theme-store changes, Task 1 changes, or out-of-scope integration changes were introduced.

## Review findings follow-up

### Findings addressed

- Added a real corrupt-file recovery test that writes invalid bytes before opening a Preferences DataStore configured with production-equivalent `ReplaceFileCorruptionHandler { emptyPreferences() }`, proves full-default recovery, then saves and reads a valid snapshot to prove continued usability.
- Added storage-isolation coverage using separate real Preferences DataStores and real `theme_mode`/session keys, proving playback reads no theme-file session-like data and playback saves do not overwrite theme data.
- Added an internal `JvmPlaybackSessionStoreFactory` seam used by the actual JVM factory. Tests prove the exact `Library/Application Support/RhythHaus/playback_session.preferences_pb` path and one lazy DataStore instance without exposing a public test API.
- Split malformed-current coverage into truncated encoding, two-ID encoding, and valid single current ID absent from the queue.
- Added a direct non-empty queue round-trip with null current ID and non-default position, repeat, and shuffle.
- Updated every test DataStore fixture to retain its `SupervisorJob`, call `cancelAndJoin()` in `finally`, and only then delete files/directories.

### Review-fix RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache
```

Result: expected `BUILD FAILED` at `:shared:compileTestKotlinJvm` because `JvmPlaybackSessionStoreFactory` was unresolved. This was the meaningful missing-production-seam RED. The new corruption, isolation, malformed-current, null-current, and resource-lifecycle tests target behavior already present or test-fixture discipline, so they did not independently demonstrate missing production behavior before the seam was added.

### Review-fix GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionStoreJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 3s`; the expanded focused Task 2 JVM suite passed.

### Review-fix platform compile and diff evidence

Command:

```bash
./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Result: `BUILD SUCCESSFUL in 416ms`; JVM, Android main, and iOS Simulator ARM64 main compilation passed.

- `lsp_diagnostics` remained unavailable because `kotlin-ls` is not installed and installation was previously declined.
- `GIT_MASTER=1 git diff --check`: pass with no output.

### Review-fix commit

- Commit: `45602c5c6bcae0eebc70d1e585553d1fe9ae78fb`
- Message: `test: harden playback session storage coverage`
- Includes the required Sisyphus footer and co-author trailer; prior commits were not amended.

### Review-fix self-review

- Production behavior changed only in the JVM Task 2 factory file, extracting an internal seam while preserving the same production path, corruption handler, scope, lazy singleton DataStore, and actual factory behavior.
- No Android/iOS production files required changes; their earlier factory behavior compiled unchanged.
- Corruption recovery exercises serialized file bytes rather than preference-key mutation.
- Isolation uses independent files and actual preference operations, not source-text inspection.
- Current-ID malformed cases are now behaviorally distinct and directly named.
- All temporary DataStore actors are cancelled and joined before filesystem cleanup, including two-store isolation and JVM factory tests.
