# Task 1.3 Policy Amendment Report

Status: Task 1.3 is rejected/in progress. This documentation-only amendment supersedes
the rejected source-ownership model in support commits `bada6d6` and `747d5cc` without
rewriting their historical evidence.

## Decision

- Contract-first moves preserve current Kotlin package declarations. Roots may overlap,
  match only exactly or root-plus-dot, and are permitted per module rather than globally
  unique.
- `com.eterocell` imports resolve through checked-source FQ declarations and require an
  actual declared consumer-to-provider `ProjectDependency` or same-module provider.
  Wildcard, ambiguous, and missing providers fail conservatively.
- Resources use actual KMP/Compose source-set locations. Namespace validation waits for
  real module build configuration.
- SQLDelight has exactly one physical owner: `:shared` until the atomic Task 3.1 transfer
  to `:core:database`. Real configuration/artifact signals distinguish ownership from
  runtime/coroutine driver consumers.
- Explicit API uses KMP/Kotlin metadata across Android/JVM/Native production compilations,
  excluding tests. Fixtures count exact diagnostics and prove valid and invalid
  configuration-cache reuse.

## Next Safe Action

Implement a separately approved third Task 1.3 repair against this policy. OpenSpec 2.1
and 2.3 are unchecked; 2.4 remains unchecked. No Gradle, Kotlin production, or test file
changed in this amendment.
