# Task 1.1 Report: Canonical Architecture Documents And ADRs

## Status

Completed. The four brief-authorized governance documents were created and
committed. `AGENTS.md`, Gradle/build files, source code, OpenSpec artifacts, and
product behavior were not modified.

## Changed Files

- `skills/kmp-architecture/SKILL.md`: Canonical KMP architecture guidance covering
  the module graph, ownership, package-preserving contract-first migration, Back
  invariants, Koin composition, resources, SQLDelight, iOS exports, scaffolding,
  executable enforcement, and Dependency Analysis Plugin deferral.
- `docs/architecture.md`: Canonical architecture policy and ownership matrix.
- `docs/adr/0001-feature-first-module-boundaries.md`: Accepted module boundary and
  dependency-direction decision.
- `docs/adr/0002-shared-ios-export-policy.md`: Accepted shared facade and narrow
  iOS export allow-list decision.

## Commit

- `55a4f72 docs: define feature-first architecture governance`

## Commands And Output

- RED existence check:
  `test -s skills/kmp-architecture/SKILL.md && test -s docs/architecture.md && test -s docs/adr/0001-feature-first-module-boundaries.md && test -s docs/adr/0002-shared-ios-export-policy.md`
  returned nonzero before creation because all four targets were absent.
- GREEN existence check: the same command returned exit 0 after creation.
- Required concern check:
  `rg -n 'feature-first|ProjectDependency|MainViewController|SQLDelight' skills/kmp-architecture/SKILL.md docs/architecture.md docs/adr`
  returned concrete matches for every required term in the skill, architecture
  document, and ADRs.
- Diff hygiene:
  `git diff --check` returned exit 0 before staging; `git diff --cached --check`
  returned exit 0 before commit.
- Original combined quality command:
  `./gradlew spotlessCheck detekt --configuration-cache` stopped at the
  pre-existing `:shared:spotlessKotlinCheck` failure. It did not establish Detekt
  execution.
- Approved formatting remediation:
  `./gradlew spotlessApply --configuration-cache` succeeded, modifying only the
  eight expected shared Kotlin/test files. The standalone formatting-only change
  was independently reviewed as exact-scope and safe, then committed as
  `61ec5b7 style(shared): apply Spotless formatting`.
- Separate quality verification:
  `./gradlew spotlessCheck --configuration-cache` passed (`BUILD SUCCESSFUL in
  2s`); `./gradlew detekt --configuration-cache` passed (`BUILD SUCCESSFUL in
  10s`; `:shared:detekt NO-SOURCE`); and
  `./gradlew :shared:jvmTest --configuration-cache` passed (`BUILD SUCCESSFUL in
  29s`).

## Concerns

- The task-specific documentation checks, diff hygiene, Spotless, Detekt, and JVM
  tests passed. No waiver remains.
- Future quality verification must run `spotlessApply` first, followed by separate
  `spotlessCheck` and `detekt` commands.
