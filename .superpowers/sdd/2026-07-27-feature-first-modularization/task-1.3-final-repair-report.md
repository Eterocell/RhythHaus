# Task 1.3 Final Repair Report

Status: complete against policy amendment commit `68154d7`.

## RED/GREEN

- RED: the new 18-case `ArchitectureCheckPluginFunctionalTest` fixture was added before
  production changes. Its first run against the rejected checker failed all 18 cases.
- GREEN: `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache --rerun-tasks`
  passed all 18 cases. The valid fixture and the invalid fixture's second invocation both
  assert `Reusing configuration cache`.

## Implementation

- Package roots use exact-or-dot matching, preserve real packages, and support overlapping
  ownership roots.
- Kotlin declarations provide import ownership; cross-module imports require a direct actual
  project dependency. Wildcard, missing, and ambiguous imports fail.
- Resource and SQLDelight discovery use KMP/Compose and physical SQLDelight layouts. SQLDelight
  generated table models are indexed from real `.sq` artifacts; Native generated cinterop
  bindings are excluded from Kotlin declaration resolution.
- Explicit API reads `KotlinBaseExtension.explicitApi` and requires `Strict`.
- Scanner coverage includes public properties, one-KDoc-per-declaration behavior, multiline
  declarations, raw strings/comments, and backticked import segments. Diagnostics are stable,
  ordered, complete, and duplicate-free.

## Verification

- Focused TestKit suite: pass, 18 tests.
- Full `:build-logic:convention:test --configuration-cache`: pass.
- `architectureCheck --configuration-cache --configuration-cache-problems=fail`: pass twice;
  second invocation reused the configuration cache.
- `spotlessApply --configuration-cache`, independent `spotlessCheck --configuration-cache`, and
  independent `detekt --configuration-cache`: pass.
- `openspec validate feature-first-modularization --strict`: pass.

Residual limitation: declaration indexing intentionally remains bounded and lexical; it does not
use Kotlin compiler internals. SQLDelight table models and Native cinterop are handled only as
their build-generated artifacts require.
