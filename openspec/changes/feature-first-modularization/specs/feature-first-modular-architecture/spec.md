## ADDED Requirements

### Requirement: Demand-driven feature-first module graph

The implementation SHALL migrate RhythHaus through buildable contract-first slices to `:core:model`, `:core:ui`, `:core:database`, and narrowly-scoped `:core:platform`; SHALL add `:core:playback` when playback contracts/engine are extracted; and SHALL add `:core:navigation` only when a common destination-scoped Back contract requires it. It SHALL create library and playlists API/implementation modules, keep Now Playing, Search, and Settings single modules initially, and SHALL NOT create `:core:network`, empty modules, or empty pattern types.

#### Scenario: A slice introduces only demanded structure
- **WHEN** an implementation slice adds a module or API/implementation boundary
- **THEN** the slice demonstrates its real owner or stable contract through code and tests
- **AND** no speculative core/network module or empty UI-state pattern is added.

### Requirement: Dependency direction and shared composition boundary

Applications SHALL depend on `:shared`; `:shared` SHALL own `App()`, root shell, cross-feature route/Back arbitration, lifecycle, Koin assembly, and stable `MainViewController`. No core or feature module SHALL depend on `:shared` or an app module; no feature SHALL depend on another feature implementation; cross-feature access SHALL use feature APIs; and only `:shared` SHALL compose implementation modules.

#### Scenario: Graph rejects a forbidden bridge
- **WHEN** a fixture adds a core/feature-to-shared edge, an app edge, a feature-implementation edge, or a feature-to-shared-to-feature bridge
- **THEN** architecture verification fails with the forbidden dependency reported
- **AND** the normal graph remains acyclic.

### Requirement: Ownership and state contracts remain local

Core model SHALL contain only truly cross-feature immutable projections; core UI SHALL contain reusable primitives/theme/artwork abstractions without feature UI state; core database SHALL own the sole SQLDelight schema/driver/migrations/generated DB; and core platform SHALL contain only capabilities reused by at least two domains. Library SHALL own scanner/source/index/repository/UI/transient state, playlists SHALL own playlist repository/edit/backup/UI, playback contracts/engine SHALL belong to core playback, and feature internal state SHALL remain local.

#### Scenario: Stateful feature screen follows its local contract
- **WHEN** a stateful feature screen is migrated
- **THEN** immutable `UiState`, `UiEvent`, and `UiEffect` are coordinated by its Presenter/ViewModel
- **AND** its data flow is UI -> Event -> Presenter -> UseCase -> Repository -> DataSource with boundary-local representation mapping.

### Requirement: Back behavior is preserved through modular moves

Shared root arbitration SHALL preserve the existing ordering modal -> edit -> active-page selection -> Now Playing -> route, with exactly one transition per intent and only the active destination eligible. Predictive Back SHALL latch the exact destination/target. Features SHALL own modal/edit state and publish only foremost dismissal; displayed-playlist deletion SHALL remain destination invalidation rather than Back.

#### Scenario: Modular feature state cannot change Back precedence
- **WHEN** a migrated active destination has a foremost modal, edit state, selection, Now Playing, and a route transition
- **THEN** one Back intent dismisses only the modal
- **AND** inactive destination state and playlist deletion do not consume that intent.

### Requirement: Database, resources, and iOS compatibility are preserved

SQLDelight moves SHALL atomically transfer `.sq` files, existing migrations, drivers, and generated package with no schema/name/history changes. Feature resources SHALL move with a module namespace and be verified for Android packaging, desktop runtime, and iOS linking. iOS SHALL export only modules whose declarations enter the Swift/Objective-C public API, and the existing shared framework entry SHALL remain stable.

#### Scenario: A platform-affecting move preserves compatibility
- **WHEN** a migration moves database, resources, expect/actual code, or public iOS declarations
- **THEN** tests verify existing database/migrations/foreign keys and supported platform startup/resource/DI behavior
- **AND** the iOS export allow-list admits only required public declarations.
