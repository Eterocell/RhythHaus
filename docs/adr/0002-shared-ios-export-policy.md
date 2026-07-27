# ADR 0002: Shared iOS Export Policy

## Status

Accepted

## Context

RhythHaus must retain a stable iOS framework entry while feature-first modules are
introduced. Broad exports would turn internal dependencies into Swift/Objective-C
surface area and undermine module ownership boundaries.

## Decision

`:shared` remains the sole KMP composition and iOS facade. It owns `App()`, root
shell, cross-feature route and Back arbitration, lifecycle, Koin assembly, and
stable `MainViewController`. The existing `MainViewController` name and entry into
the shared Compose composition remain stable for the iOS wrapper.

The iOS export allow-list is narrow. Export only modules whose declarations enter
the required public Swift/Objective-C API. Do not export core modules or feature
implementation modules merely because `:shared` depends on them. Core and feature
modules never depend on `:shared` or app modules; `:shared` alone composes feature
implementations and starts Koin.

Each export change identifies its required public declarations, updates the
allow-list, and verifies framework linking and public API behavior. The planned
`architectureCheck` inspects actual `ProjectDependency` edges and validates the iOS
export allow-list with dependency, ownership, and SQLDelight rules.

## Consequences

The iOS wrapper keeps stable `MainViewController` integration while implementation
modules remain internal to shared composition. Resources stay feature-owned and
require Android packaging, desktop runtime, and iOS linking checks. SQLDelight
remains one physical database owned by `:core:database`; `.sq` files, migrations,
drivers, and generated package move atomically without schema, name, or migration
history changes.

This decision does not authorize broad exports, feature-to-feature implementation
coupling, `feature -> shared -> feature` bridges, or empty scaffolding. Dependency
Analysis Plugin adoption remains deferred until separate version and KMP
compatibility evaluation after graph stabilization.
