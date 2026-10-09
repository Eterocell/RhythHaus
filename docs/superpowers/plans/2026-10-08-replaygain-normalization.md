# ReplayGain Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Optional persisted, tag-driven ReplayGain attenuation on Android, iOS and macOS without disturbing playback or sleep timers.

**Architecture:** Native TagLib metadata carries raw optional tags; Library parses and persists audio facts. Core playback combines normalization and sleep attenuation under existing selection/gain ownership. Shared persists mode and Settings renders neutral controls.

**Tech Stack:** Kotlin Multiplatform, Compose/Miuix, SQLDelight, DataStore, TagLib C++/JNI/cinterop.

**Spec:** `openspec/changes/replaygain-normalization/specs/` and `openspec/changes/replaygain-normalization/design.md`.

## Global Constraints

- Default Off; Track and Album; album fallback only when album gain is absent/invalid, never borrow track peak for valid album gain.
- Four nullable Double facts; finite gains, finite positive peaks. No amplification: min(1, 10^(g/20), optional 1/p).
- Application gain = normalization × sleep attenuation; native gain always finite 0..1.
- No media writes, analysis, new dependencies/toolchains, preamp, EQ or crossfade.
- Migration `7.sqm` upgrades v7 to v8; preserve existing user data and version-zero detection.
- Physical UI/listening/system-control acceptance is user-owned; automated tests, smoke and static checks are implementation-owned.

## Review Focus

- Android content URI/FD and iOS native conversion must carry actual tags, not just JVM properties: Task 1 native fixtures and platform compilation.
- Extreme finite dB/peak must not overflow native gain or fail metadata: Tasks 1/3 malformed-field and numeric boundary regressions.
- Cancellation after persistence must not leave mode/controller divergent: Task 4 committed-write cancellation test.
- Queued gain work and metadata reconciliation must not affect a new selection: Task 3 delayed-engine handoff tests.
- Cancellation of sleep fade must restore normalization, never unity burst: Task 3 pre-resume event ordering tests.

---

### Task 1: Native extraction and parsed Library facts

**Files:** Modify `taglib/native/include/rh_taglib.h`, `taglib/native/src/rh_taglib.cpp`, `taglib/native/jni/rh_taglib_jni.cpp`; `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/TagMetadata.kt`; `TagLibReader` platform files in androidMain/jvmMain/iosMain; `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/AudioMetadata.kt`. Create `ReplayGainTagParser.kt` beside AudioMetadata. Tests: `taglib/src/jvmTest/kotlin/com/eterocell/rhythhaus/taglib/JvmTagLibReaderTest.kt`, Library common metadata tests; native FD smoke.

**Interfaces:** Raw nullable String fields in TagMetadata/native metadata as named in design. Library produces nullable Double fields `replayGainTrackGainDb`, `replayGainTrackPeak`, `replayGainAlbumGainDb`, `replayGainAlbumPeak` in AudioMetadata. Internal `parseReplayGainDb(value: String?): Double?`, `parseReplayGainPeak(value: String?): Double?`.

- [ ] Write parsing regressions: `" -6.00 dB " -> -6.0`, `"+3 DB" -> 3.0`, NaN/infinity/junk -> null; peak `2 -> 2.0`, zero/negative/unit-suffixed -> null. Invalid album field must not erase valid track fields.
- [ ] Run `./gradlew :feature:library:impl:jvmTest --tests '*ReplayGainTagParserTest*' --configuration-cache`; observe RED.
- [ ] Extract property values into owned native fields in the existing path/FD result; free every allocation. Update JNI constructor and Kotlin/iOS conversion together. Parse independently in Library, retaining existing metadata fallback behavior without reopening media solely for missing ReplayGain.
- [ ] Generate temporary tagged media and assert actual reader returns all four raw fields through path and native FD reads; compare media checksum before/after. Run TagLib JVM and parser tests, compile Android and iOS readers. Observe GREEN and retain fixture regression if it catches native conversion errors.
- [ ] Review/commit `feat: read ReplayGain metadata across platforms`.

### Task 2: Persist facts and carry them into playback

**Files:** `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`; create `core/database/src/commonMain/sqldelight/migrations/7.sqm`; existing database bootstrap/version owner; `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt`; LibraryScanner, SqlDelightLibraryRepository and metadata override mapping; `core/model/src/commonMain/kotlin/com/eterocell/rhythhaus/ModelContracts.kt`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`, `MusicModels.kt` and other actual mappings. Tests: `ExistingDatabaseMigrationTest.kt`, create `ReplayGainDatabaseTest.kt`, Library scanner/repository tests.

**Interfaces:** Four nullable Double fields with null defaults in LibraryTrack and PlayableTrack; SQL REAL columns with matching names, NULL defaults. No cross-module type dependency added.

- [ ] Write file-backed v7/v0 migration and reopen test: preserve source, track ID, favorite/history/override/playlist, new fields null. Write scan/rescan test: -6/1.2/-4/1.1 persist, removed tags clear, recoverable unreadable event preserves facts, display override leaves them unchanged.
- [ ] Run `./gradlew :core:database:jvmTest :feature:library:impl:jvmTest --configuration-cache`; capture relevant RED.
- [ ] Add schema/migration/bootstrap update and map all queries/upserts/equality/playable projections. Run LSP references before exported contract changes when available. Do not retain obsolete constructor adapters.
- [ ] Run the same database/Library suites and platform test-code compilation; observe GREEN, verify reopened values and unchanged user relationships.
- [ ] Review/commit `feat: persist scanned ReplayGain facts`.

### Task 3: Controller-owned composed gain

**Files:** Create `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/ReplayGain.kt`; modify Playback.kt; tests in commonTest `ReplayGainTest.kt`, `PlaybackControllerTest.kt` and native JVM acceptance as needed.

**Interfaces:** Public `ReplayGainMode { Off, Track, Album }` with serialized lowercase values and safe `fromSerialized`; `replayGainFactor(track: PlayableTrack?, mode: ReplayGainMode): Float`; `PlaybackController.setReplayGainMode(mode: ReplayGainMode): Unit`.

- [ ] Write pure tests: -6 dB ≈ .501187; +6/peak2 -> .5; Off/peak-only ->1; album valid gain+missing peak does not borrow track peak; absent album gain falls back as pair; finite extremes stay 0..1.
- [ ] Write controller regressions: normalization .5 × sleep .4 -> .2; cancel -> .5; disable during fade -> .4 without timer changes; paused resume applies .2 before Play; replacement waiting behind engine lock cannot receive old gain; retry/restore/reconcile/autoadvance and refreshed same-ID facts receive selected factor before playback. Assert no extra Load/Seek and unchanged occurrence/repeat/shuffle/progress for mode changes.
- [ ] Run `./gradlew :core:playback:jvmTest --configuration-cache`; observe relevant RED.
- [ ] Separate sleep attenuation from effective desired gain. Recompute product inside selectionGate at current generation, retain existing versioned requests/engine mutex. Audit all unity reset and direct load/play paths; stopped engine teardown may reset unity but live/resumed playback must apply composed policy before sound. Metadata publication must recompute even when queue projection already changed.
- [ ] Run Core JVM and Android host tests, iOS test-code compilation. Exercise actual temporary-media playback plus gain changes without reload in a disposable smoke runner; remove runner afterward.
- [ ] Review/commit `feat: compose ReplayGain with sleep attenuation`.

### Task 4: Durable mode and accessible Settings

**Files:** Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playback/ReplayGainPreferenceStore.kt` and platform factories under matching androidMain/iosMain/jvmMain paths following existing theme stores; Shared App/Koin composition and LibraryRoutes; `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`; EN/ZH Settings resources; existing resource ownership ledger. Tests: preference JVM reopen/failure tests, Settings semantics tests, Shared mode-publication test.

**Interfaces:** `ReplayGainPreferenceStore.selectedMode: Flow<ReplayGainMode>`; `suspend setSelectedMode(mode: ReplayGainMode)`; `expect createReplayGainPreferenceStore(): ReplayGainPreferenceStore`. Settings receives neutral mode/busy/error and onSelect callback; no Android intents or repository dependencies.

- [ ] Write reopen/default/corrupt/unreadable tests; controlled failed-write test retains accepted Track and controller gain; concurrent selections serialize; cancellation after durable commit finishes authoritative publication. Write EN/ZH selection semantics and recoverable error tests.
- [ ] Run focused preference/Settings/Shared tests; capture RED.
- [ ] Persist lowercase mode in dedicated DataStore following lifecycle conventions; serialize read/write publication, apply only committed values. Wire Settings Off/Track/Album, busy exclusion, polite accessible error and explanation that untagged tracks are unchanged and positive gain is not amplified. Update ledger in owning module.
- [ ] Run focused suites plus desktop/Android/iOS Kotlin compile. Launch isolated real desktop Settings and observe selected mode/restart persistence; user owns physical listening.
- [ ] Review/commit `feat: add persistent ReplayGain settings`.

### Task 5: End-to-end acceptance and lifecycle

**Files:** OpenSpec tasks/specs, roadmap.md, progress.md; no unrelated baseline fixes.

- [ ] Run disposable tagged-media scan -> database reopen -> playable mapping -> controller gain smoke. Verify checksum unchanged, tagged attenuation and untagged unity, timer cancel returns normalized gain. Remove throwaway runners/fixtures.
- [ ] Run database, TagLib, Library, Core playback and Settings JVM suites; focused Shared integration; Android unit/Kotlin, desktop and iOS test-code compile. Run `./init.sh` only where baseline prerequisites permit; record exact unrun/failed gates, never reclassify as pass.
- [ ] Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck` as separate configuration-cache commands. Run strict OpenSpec validation and diff whitespace check.
- [ ] Independent whole-change review; repair concrete findings with regressions and re-review. Record user-owned physical/listening checks separately.
- [ ] Sync specs, archive, update roadmap/progress with evidence and commit conventionally. Retain feature branch pending requested integration; never merge or push implicitly.
