# Track List Artwork Collapse - Task 1 Report

## Status

Implemented Task 1: pure artwork-collapse geometry and signed-consumption policy.

## Files

Created:

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`

Created as the requested task evidence:

- `.superpowers/sdd/track-list-artwork-collapse-task-1-report.md`

No UI integration files, OpenSpec artifacts, `roadmap.md`, `progress.md`, dependencies, or unrelated code were changed.

## RED verification

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Result: expected failure before production code existed.

Relevant output:

```text
> Task :shared:compileTestKotlinJvm FAILED
e: .../ArtworkCollapseTest.kt:7:28 Unresolved reference 'ArtworkCollapseGeometry'.
e: .../ArtworkCollapseTest.kt:22:13 Unresolved reference 'ArtworkCollapseConsumption'.
e: .../ArtworkCollapseTest.kt:54:70 Unresolved reference 'consumeUpward'.
...
FAILURE: Build failed with an exception.
BUILD FAILED in 6s
```

The RED failure was caused by the missing artwork-collapse production types and methods, not by a test setup or syntax error.

## GREEN verification

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Result: pass.

```text
> Task :shared:jvmTest
BUILD SUCCESSFUL in 5s
26 actionable tasks: 7 executed, 19 up-to-date
Configuration cache entry reused.
```

Five focused tests passed. The first GREEN attempt exposed `-0.0` at the already-collapsed upward boundary; the implementation was corrected to return exact `0f` when no additional offset can be consumed, and the focused suite was rerun successfully.

## Self-review: policy boundaries

- `snapshot` clamps offsets to `[0, collapseRangePx]` and derives header height and progress from the same clamped geometry.
- Non-positive collapse ranges render at the collapsed height, report progress `1f`, and consume nothing.
- `consumeUpward` accepts only negative movement and consumes it one-for-one until the collapsed limit; positive or zero movement consumes `0f`.
- `consumeDownward` accepts only positive movement and expands one-for-one until offset zero; negative or zero movement consumes `0f`.
- Consumed values preserve gesture direction, never exceed the remaining collapse/expand range, and normalize exhausted movement to exact `0f` rather than `-0.0`.
- Existing offsets are clamped immediately when geometry is resized.
- The implementation uses only Kotlin `Float` arithmetic and `coerceIn`/`coerceAtLeast`/`coerceAtMost`; it has no Compose or Miuix dependency.

## Diagnostics and diff hygiene

`lsp_diagnostics` was attempted for both modified Kotlin files. Kotlin LSP is unavailable because `kotlin-ls` was previously declined for installation; Gradle compilation and tests were used as the executable language checks.

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output.

The pre-existing unstaged `roadmap.md` change remained untouched and unstaged throughout the work.

## Commit

Commit created after RED/GREEN verification:

- `fix: define coordinated artwork collapse geometry`
- Hash: `01a1011ea9023fb3613d177d090b7704d1495e5d`.

## Concerns

- Full project verification was not run because this task is explicitly limited to the focused pure Kotlin policy and tests.
- Kotlin LSP diagnostics remain unavailable; the focused Gradle compile/test gate passed.
- No runtime Compose adapter or visual behavior was included; those belong to the subsequent task.
