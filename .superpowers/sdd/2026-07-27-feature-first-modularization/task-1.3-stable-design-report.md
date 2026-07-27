# Task 1.3 Stable Design Report

Status: documentation-only. Historical Task 1.3 reports and independent rejection
evidence remain preserved. Task 1.3 is rejected/in progress and has no architecture-gate
acceptance claim.

## Decision

The approved design is `docs/superpowers/specs/2026-07-28-stable-architecture-checker-design.md`.
It separates a configuration-cache-safe public Gradle model checker from KSP semantic
package/declaration/KDoc facts. It rejects lexical Kotlin scanning and arbitrary import
provider inference; Kotlin compilation resolves symbols while Gradle validates actual
project dependencies.

## Status And Next Gate

OpenSpec 2.1 and 2.3 remain unchecked/rejected/in progress; 2.4 remains unchecked. A
separate explicit user review/approval is required before any Task 1.3 implementation,
dependencies, modules, tests, or Task 1.4 wiring changes.
