# Playlist Interoperability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import and export static saved playlists through bounded M3U/M3U8 and PLS documents while preserving order, duplicates, preview matching, and atomic confirmation.

**Architecture:** A pure bounded interoperability codec produces format-neutral entries. Existing playlist matching and repository atomic creation consume those entries; the platform launcher and existing backup UI expose format selection without changing JSON recovery semantics.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, existing playlist repository and document launcher seams, JVM/common tests.

**Spec:** `openspec/changes/playlist-interoperability/specs/playlist-interoperability/spec.md`

## Global Constraints

- JSON backup remains the complete RhythHaus recovery format.
- Smart playlist rules are not serialized into M3U/PLS.
- Import paths never grant access or override metadata matching.
- Confirmed imports use one existing atomic ordered-entry repository operation.
- No audio files, source permissions, playback queue, or current playback state are mutated.

## Review Focus

- Duplicate occurrences and source order survive both formats and import confirmation; codec/planner tests pin this to Tasks 1 and 3.
- Malformed, oversized, invalid-integer, and CRLF/Unicode input is rejected without mutation; codec tests pin this to Task 1.
- A path that names a different file cannot override unique/ambiguous metadata matching; planner tests pin this to Task 2.
- Stale library revisions and cancelled previews cannot commit playlists; planner/UI tests pin this to Task 3.
- Smart rules remain excluded and the UI communicates that limit in EN/ZH; UI semantics tests pin this to Task 4.

---

### Task 1: Bounded M3U/PLS codec

**Files:**
- Create: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistInteroperabilityCodec.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupModels.kt`
- Test: `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistInteroperabilityCodecTest.kt`

**Interfaces:**
- Produces `PlaylistInteroperabilityDocument(entries: List<PlaylistInteroperabilityEntry>, format: PlaylistInteroperabilityFormat)` and `encode(document, format): ByteArray`.
- Consumes bounded UTF-8 bytes and returns a typed validation result; no platform or repository access.

- [ ] Write failing tests for M3U/M3U8 and PLS parsing/encoding, order, duplicates, comments, CRLF, Unicode, malformed input, limits, and path non-authority.
- [ ] Implement strict bounded parser/writer with canonical UTF-8 output and format-specific validation.
- [ ] Run `./gradlew :feature:playlists:impl:jvmTest --tests '*PlaylistInteroperabilityCodecTest*' --configuration-cache` and require PASS.
- [ ] Commit `feat: add m3u and pls codec`.

### Task 2: Format-neutral matching planner

**Files:**
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcher.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupModels.kt`
- Test: `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistInteroperabilityMatcherTest.kt`

**Interfaces:**
- Consumes interoperability entries and the existing effective `LibraryTrack` snapshot.
- Produces the existing preview/report/import-plan projections, preserving unique, unmatched, ambiguous, and duplicate source order.

- [ ] Write failing tests for mixed results, metadata matching despite misleading paths, duration matching, duplicate occurrences, and missing-track behavior.
- [ ] Implement the adapter without changing JSON backup matching behavior.
- [ ] Run focused matcher tests and require PASS.
- [ ] Commit `feat: plan interoperable playlist imports`.

### Task 3: Serialized import/export orchestration

**Files:**
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiState.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupService.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.kt`
- Test: existing backup service tests and new interoperability service tests

**Interfaces:**
- Uses `PlaylistInteroperabilityFormat` for operation selection and the existing document launcher callbacks.
- Produces preview/result/error state and commits via existing `PlaylistRepository.createWithEntries` only after revision validation and confirmation.

- [ ] Write failing tests for format selection, complete-byte export, stale revision, cancellation, atomic failure, and smart-rule exclusion.
- [ ] Implement serialized orchestration and platform filter/MIME extensions while preserving JSON behavior.
- [ ] Run playlist backup integration tests plus Android host, iOS test compilation, and desktop compilation.
- [ ] Commit `feat: integrate interoperable playlist documents`.

### Task 4: Accessible UI and localized copy

**Files:**
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt`
- Modify: `feature/playlists/impl/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/playlists/impl/src/commonMain/composeResources/values-zh/strings.xml`
- Test: `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupInteroperabilitySemanticsJvmTest.kt`

**Interfaces:**
- Consumes the orchestration state from Task 3.
- Produces accessible EN/ZH format selection, import/export actions, smart-rule limitation copy, preview issue summaries, and result counts.

- [ ] Write failing compact/split semantic tests for all formats, labels, disabled actions, cancellation, and EN/ZH copy.
- [ ] Implement the UI with existing dialog/back/accessibility conventions.
- [ ] Run focused UI tests and quality gates.
- [ ] Commit `feat: expose playlist interoperability controls`.

### Task 5: Acceptance and closeout

**Files:**
- Modify: `openspec/changes/playlist-interoperability/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`
- Modify: canonical specs during archive

- [ ] Run the focused playlist/shared/database tests, platform compilation, Spotless, Detekt, architecture checks, strict OpenSpec validation, and `git diff --check`.
- [ ] Obtain independent review and repair actionable findings.
- [ ] Synchronize canonical specs, archive the change, record user-owned physical picker/UI acceptance as pending, and commit the final branch.
