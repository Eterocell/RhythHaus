# KMP Architecture Governance

## Purpose

Use this skill for RhythHaus module boundaries, KMP source ownership, dependencies,
resources, SQLDelight, Koin composition, and iOS framework exports. The canonical
policy is `docs/architecture.md`; ADR 0001 and ADR 0002 are binding decisions.

## Instruction Link Policy

`AGENTS.md` must link to this skill, `docs/architecture.md`, and the ADRs rather
than duplicate architecture policy. A dedicated task owns that link-only update;
do not change `AGENTS.md` while creating or revising this guidance.

## Required Module Graph

```text
:androidApp, :desktopApp, iosApp
        |
     :shared
        |
  +-----+---------------------+
  |                           |
:core:*                   :feature:*:impl
                              |
                         :feature:*:api
```

- Applications depend on `:shared`.
- `:shared` is the sole composition root and iOS facade. It owns `App()`, the root
  shell, cross-feature route and Back arbitration, lifecycle, Koin assembly, and
  stable `MainViewController`.
- Core and feature modules never depend on `:shared` or app modules.
- A feature implementation never depends on another feature implementation.
  Cross-feature access is only through an explicit feature API.
- A feature implementation publishes a Koin `Module` only when it owns injectable
  bindings. UI-only modules use composable/function entry points and do not create
  empty modules. Only `:shared` assembles and starts Koin. No module may use a
  service-locator back-reference to `:shared`.

## Ownership And Migration Rules

- Use buildable, feature-first, contract-first slices. Move a stable contract
  before its implementation and do not introduce a `feature -> shared -> feature`
  bridge. Preserve Kotlin package declarations during module moves; package
  renames are separate work.
- `:core:model` owns only cross-feature immutable projections. `:core:ui` owns
  reusable primitives, theme, and artwork abstractions, never feature UI state.
  `:core:platform` exists only for a capability used by at least two domains.
- `:core:database` owns the one physical SQLDelight schema, drivers, migrations,
  and generated database. Move `.sq` inputs, migrations, drivers, and the generated
  package atomically without changing schema, database name, or migration history.
  Feature repositories and mappings remain feature-owned.
- Library owns scanner, source access, indexing, repositories, UI, and transient
  state. Playlists owns its repository, editing, backup, and UI. Playback engine
  and contracts belong in `:core:playback` when extracted.
- A feature owns its modal/edit state and exposes only its foremost dismissal.
  `:shared` preserves Back arbitration: modal, edit, active-page selection, Now
  Playing, then route. Deleting a displayed playlist is destination invalidation,
  not a Back transition.
- Move resources with their feature and assign a feature-owned module namespace.
  Verify Android packaging, desktop runtime resolution, and iOS linking in the
  affected migration slice.

## API, Scaffold, And Export Rules

- Split a feature into API and implementation modules only for a real stable
  contract. Do not create `:core:network`, speculative modules, or empty
  `UiState`, `UiEvent`, `UiEffect`, or Presenter/ViewModel types.
- Keep iOS exports to a narrow allow-list of modules whose declarations appear in
  the Swift/Objective-C public API. Preserve the shared framework facade and
  `MainViewController`; do not broadly export core or feature implementations.
- The future `architectureCheck` must inspect actual Gradle `ProjectDependency`
  edges and cycles using a code-owned allow-list. It will enforce package, import,
  resource, public API/KDoc, SQLDelight ownership, and iOS export rules.

## Deferred Work

Dependency Analysis Plugin adoption is intentionally deferred until the graph is
stable and a separate version and KMP compatibility evaluation is approved.
