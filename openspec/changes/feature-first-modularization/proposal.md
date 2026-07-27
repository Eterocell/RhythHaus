## Why

RhythHaus currently concentrates composition, feature behavior, persistence, and platform-facing concerns in `:shared`, making ownership and dependency direction difficult to enforce. A gradual, behavior-preserving modularization is needed now to make future KMP feature work independently testable and maintainable without destabilizing supported platforms.

## What Changes

- Establish a contract-first, feature-first KMP multi-module migration with a thin final `:shared` composition/iOS facade.
- Define demand-driven core modules and feature API/implementation boundaries while preserving Kotlin packages during moves.
- Add canonical architecture documentation, ADRs, feature ownership documentation, convention plugins, and executable Gradle architecture gates.
- Require migration-slice TDD, dependency graph enforcement, SQLDelight/resource/iOS safeguards, and supported-platform verification.

## Capabilities

### New Capabilities
- `feature-first-modular-architecture`: Defines the target module graph, ownership, dependency rules, composition boundary, and migration safeguards.
- `architecture-governance-gates`: Defines architecture documentation and executable Gradle dependency, API, database, resource, and iOS export enforcement.

### Modified Capabilities
- None.

## Impact

The future implementation affects Gradle settings and build logic, the `shared` KMP module, Android/desktop/iOS composition paths, SQLDelight ownership, resources, Koin assembly, tests, CI, architecture documentation, and OpenSpec tracking. It intentionally preserves runtime behavior and public iOS entry stability during each migration slice.
