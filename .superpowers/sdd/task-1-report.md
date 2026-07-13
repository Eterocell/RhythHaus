# Task 1 Report: Bounded playback-session snapshot codec

## Status

DONE

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshot.kt`
  - Added `PlaybackSessionSnapshot` with ID-only queue/current state and repeat/shuffle modes.
  - Added `PlaybackSessionCodec` with exact bounds: `10_000`, `4_096`, `16_384`, and `1_048_576`.
  - Added all-or-null decoding and checkpoint models: `ProgressCheckpointKey` and `PlaybackCheckpoint`.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`
  - Added direct round-trip, invalid-form, bounds, all-or-null, and checkpoint-model tests.
- `.superpowers/sdd/task-1-report.md`
  - This durable evidence report; not included in the implementation commit.

## Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache
```

Result: `BUILD FAILED in 1s` during `:shared:compileTestKotlinJvm` before any production edit. The compiler reported unresolved references for `PlaybackSessionCodec`, `PlaybackSessionSnapshot`, `ProgressCheckpointKey`, and `PlaybackCheckpoint`, confirming the expected missing-symbol RED.

## GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 1s`; `25 actionable tasks: 6 executed, 19 up-to-date`; configuration cache reused. All four focused tests passed.

## Requirement coverage

- Queue snapshots contain IDs only; effective shuffle order is not persisted.
- Encoding rejects empty, duplicate, unpaired-surrogate, per-ID over-bound, count over-bound, and encoded-size over-bound input.
- Decoding parses decimal lengths, requires positive lengths, rejects malformed/truncated/trailing/duplicate/surrogate/over-bound values, and returns `null` for any invalid stored value.
- Checkpoints carry complete snapshots; playing-progress checkpoints carry generation, current-track ID, and second bucket.
- No dependencies, store/platform/controller/coordinator/DI/App/OpenSpec/docs/progress/roadmap files were changed.

## Diff check

Command:

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output.

## Self-review

- Only the exact Task 1 production and direct test files are staged for commit.
- Public names, defaults, constants, and checkpoint shapes match the authoritative brief.
- Codec validation is conservative and never returns a partial decode.
- No type suppression, dependency change, unrelated refactor, exact shuffle-order persistence, or skipped RED phase was introduced.
- `lsp_diagnostics` was attempted for the changed production file but could not run because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation and focused JVM tests passed.

## Commit

`fd21d161e5f426c24697e8f64b3276a5001d98c4` — `feat: add playback session codec`

Commit scope: exactly the two Task 1 implementation/direct-test files. The report remains uncommitted as requested by the exact-file commit command.

## Concerns

None beyond the unavailable Kotlin LSP noted in self-review.

## Review-fix evidence

### Findings addressed

- Expanded `PlaybackSessionSnapshotTest` with table-driven malformed decode cases for non-decimal lengths, missing colons, unpaired surrogates, over-count, over-character, over-UTF-8, over-total-size, trailing data, and truncation.
- Added successful exact-limit coverage and one-over rejection coverage for `maxIds`, `maxIdCharacters`, `maxIdUtf8Bytes`, and `maxEncodedUtf8Bytes`.
- Replaced decoder duplicate checks based on `List.contains` with an ordered `ArrayList` plus `HashSet`, preserving output order with expected O(n) duplicate detection.
- Changed encoding to validate each framed entry's UTF-8 size incrementally before append, using subtraction-based overflow-safe arithmetic and avoiding construction/re-encoding of an oversized final string.

### RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache
```

Result: the newly added boundary suite initially failed at the exact `maxEncodedUtf8Bytes` assertion because the test fixture's computed frame total was incorrect. This was a test-fixture arithmetic error, not fabricated production behavior; it was corrected before production changes. The existing implementation already passed the behavioral boundary assertions, so no meaningful production-behavior RED was available for the complexity/allocation-only fixes. This is recorded honestly per the TDD contract.

### GREEN evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 3s`; `25 actionable tasks: 8 executed, 17 up-to-date`; configuration cache reused. All four focused tests passed.

### Diff check

Command:

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output.

### Self-review

- Changed only the Task 1 production/test files plus this uncommitted report.
- Exact-limit and one-over tests cover all four published bounds.
- Decoder remains all-or-null and preserves ID order.
- Encoding checks malformed surrogate input before UTF-8 sizing and rejects total-size overflow before appending the frame.
- No dependencies, unrelated files, type suppression, amend, or commit rewrite was used.
- Kotlin LSP diagnostics were attempted for both changed Kotlin files but remain unavailable because `kotlin-ls` is not installed and installation was previously declined.

### Review-fix commit

`11af03fcb4af68d377a33115b74315440a4906fa` — `fix: harden playback session codec bounds`

Commit scope: exactly `PlaybackSessionSnapshot.kt` and `PlaybackSessionSnapshotTest.kt`; this report remains uncommitted as requested.

## Approved artifact clarification and remaining review evidence

### Test corrections

- Replaced the prior mislabeled over-count decode fixture with a syntactically valid concatenation of exactly `maxIds + 1` distinct valid frames. Its encoded size remains below `maxEncodedUtf8Bytes`, so decoding reaches the ID-count guard.
- Replaced the prior mislabeled over-total fixture with a syntactically valid unique-frame stream whose UTF-8 size exceeds `maxEncodedUtf8Bytes` while each ID remains individually valid and the count remains within `maxIds`.
- Removed the impossible exact/one-over `maxIdUtf8Bytes` claim. The test now constructs `"\u0800".repeat(maxIdCharacters)`, asserts 4,096 Kotlin characters and 12,288 UTF-8 bytes, and verifies round-trip. Exact/one-over coverage remains for `maxIds`, `maxIdCharacters`, and `maxEncodedUtf8Bytes`.
- Production codec remained unchanged because no real defect was exposed.

### Verification

- Focused JVM test: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionSnapshotTest' --configuration-cache` — `BUILD SUCCESSFUL in 1s`.
- OpenSpec: `openspec validate persist-playback-session --strict` — `Change 'persist-playback-session' is valid`.
- Diff check: `GIT_MASTER=1 git diff --check` — pass with no output.

### TDD note

This was review-driven fixture and artifact correction, not a new production behavior change. The focused suite passed against the unchanged codec after the syntactic fixtures and reachable UTF-8 assertions were corrected; no production RED was fabricated.

### Separate commits

- `d23c3c6b3dc67caa90601ede3594a9639b77c647` — `test: correct playback session codec boundaries`
- `552f7b48241022d72e360c2e3ea3ce750d8bc6d6` — `docs: clarify playback codec bounds`

The first commit contains only the direct Task 1 test correction. The second contains exactly the five approved planning artifacts. This report remains uncommitted.
