## Context

See `proposal.md` for the motivation. The root architecture plugin currently identifies Versions-plugin aggregate dependencies only for the legacy plugin ID, while the root build applies the current plugin ID. Its identity exclusion set is therefore empty. The project resolves Compose Multiplatform 1.12.0 while Miuix 0.9.3 requests older Compose artifacts, yielding a JVM Skiko linkage error during UI rendering. CI has a root quality job and a JVM test job but no platform build matrix.

## Goals / Non-Goals

**Goals:**

- Restore `qualityCheck` as a trustworthy root quality gate without weakening authored-edge validation.
- Resolve the documented JVM UI rendering linkage failure through the user-selected compatible Miuix release.
- Make CI prove Android, desktop, and linked iOS app buildability while retaining JVM tests.
- Run architecture, ktfmt-backed Spotless, and Detekt as independently visible CI checks.

**Non-Goals:**

- Do not change the application module graph, public APIs, database schema, or product behavior.
- Do not add Android Host Test or iOS Simulator Test execution to CI.
- Do not add version-forcing rules, dependency substitutions, or unrelated toolchain upgrades.

## Decisions

### Observe both Versions plugin IDs while retaining identity filtering

The architecture plugin will register the same exact-identity aggregate capture for both `io.github.ben-manes.versions` and `com.github.ben-manes.versions`. The existing identity-based filter remains the authority; only the current plugin ID is added. This preserves the current narrow suppression model and does not use configuration-name filtering or resolved classpath inspection.

The functional fixture will apply the production `io.github.ben-manes.versions` ID and prove that generated aggregate edges no longer enter the graph. The legacy-ID fixture remains as compatibility coverage, and an ordinary forbidden edge still produces the exact normal architecture failure.

### Upgrade Miuix without forcing Compose or Skiko

The version catalog will set the user-selected `0.9.4-rc01` Miuix version. Compose Multiplatform remains `1.12.0`. No explicit Skiko coordinate, forced version, or resolution strategy will be added: the compatible release's transitive metadata is the source of truth. The previously failing production-composable JVM tests are the regression boundary.

### Separate CI responsibilities by job

The existing CI workflow will retain its JVM test job. Its quality job will invoke `architectureCheck`, `spotlessCheck`, and `detekt` as distinct Gradle commands so each required gate is independently observable. Three macOS build-only jobs will run Android debug assembly, desktop Kotlin compilation, and a code-signing-disabled Simulator build of the Xcode `iosApp` target. Each job uses the existing checkout, Java 21, and Gradle setup pattern as applicable.

The iOS job uses Xcode rather than only Gradle Kotlin/Native compilation because the required deliverable includes the Swift wrapper and Kotlin framework linkage. The platform jobs select no Android Host Test or iOS Simulator Test task.

## Risks / Trade-offs

- Miuix `0.9.4-rc01` is a release candidate. Its scope is deliberately limited to the binary-compatibility repair; the complete JVM UI battery and platform build matrix are the acceptance checks.
- macOS-only build jobs duplicate checkout/setup time. Separate jobs keep platform failures attributable and prevent a platform-specific failure from concealing another platform's result.
- Gradle offers no dependency provenance for equal project dependencies. The existing identity-based boundary and authored-aggregate negative regression remain authoritative; adding the current plugin ID does not broaden suppression elsewhere.
