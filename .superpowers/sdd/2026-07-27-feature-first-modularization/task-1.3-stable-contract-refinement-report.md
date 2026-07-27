# Task 1.3 Stable Contract Refinement Report

This documentation-only pre-repair amendment preserves the independent rejection and all
historical reports. No architecture gate is accepted.

- `:architecture-processor` is tooling-only and is excluded solely as a direct dependency
  on public production KSP configurations; all other placements remain `ARCH-EDGE`.
- `:core:model` and `:core:database` remain mutually forbidden; a relevant cycle reports
  both forbidden-edge and cycle diagnostics.
- Strict core/API custom Compose roots enter only through the owned registry alongside
  documented defaults and public `ResourcesExtension` namespace.
- SQLDelight uses public configured database properties plus documented/explicit source
  roots, with deterministic unsupported/inconsistent failures.
- Real processor fixtures cover supported targets, initial input only, exact diagnostics,
  resources, SQLDelight, generated rounds, and configuration cache.

Task 1.3 remains rejected/in progress. OpenSpec 2.1, 2.3, and 2.4 remain unchecked pending
user review and separately approved implementation.
