## ADDED Requirements

### Requirement: Ordinary shared top bars use Miuix TopAppBar

RhythHaus shared Compose screens SHALL use Miuix `SmallTopAppBar` or `TopAppBar` for ordinary page-level back/title chrome when the Miuix component preserves the existing route and layout behavior.

#### Scenario: Settings top bar uses Miuix
- **WHEN** Settings screen top chrome is rendered
- **THEN** it uses the shared RhythHaus Miuix top app bar wrapper
- **AND** the back action still calls the existing `onDismiss` callback
- **AND** the displayed title still uses the localized Settings resource
- **AND** Settings content, appearance dropdown, scan controls, import message, and clear-library behavior remain unchanged

#### Scenario: Search top bar uses Miuix
- **WHEN** Search screen top chrome is rendered
- **THEN** it uses the shared RhythHaus Miuix top app bar wrapper
- **AND** the back action still calls the existing `onDismiss` callback
- **AND** the displayed title still uses the localized Search resource
- **AND** focus requester, query state, placeholder, clear action, filtering, result selection, now-playing highlight, equalizer, and dismiss behavior remain unchanged

#### Scenario: Library drill-down header uses Miuix for back/subtitle chrome
- **WHEN** an album or artist drill-down view renders its header
- **THEN** the back/subtitle top chrome uses the shared RhythHaus Miuix top app bar wrapper
- **AND** the back action still calls the existing `onBack` callback
- **AND** the large drill-down title remains visible as page content below the top app bar
- **AND** track rows, selected state, scroll reporting, section label, left-edge swipe back, nested-scroll glass chrome, and Now Playing bar behavior remain unchanged

### Requirement: Product-specific chrome remains custom

RhythHaus SHALL NOT replace product-specific chrome or media UI with Miuix TopAppBar when that would change app-specific behavior.

#### Scenario: Nested-scroll glass chrome remains custom
- **WHEN** Library root or drill-down nested-scroll blur chrome is rendered
- **THEN** the existing custom glass/backdrop chrome remains in place
- **AND** status-bar covering, blur gating, title morphing, and backdrop recording behavior are not changed by this migration

#### Scenario: Now Playing remains custom
- **WHEN** Now Playing screen or Now Playing bar chrome is rendered
- **THEN** this migration does not replace its playback controls, artwork, scrubber, gestures, or layout with Miuix TopAppBar

### Requirement: No new Miuix dependency is introduced

The Miuix TopAppBar migration SHALL use already available `miuix-ui` APIs and SHALL NOT add new Miuix modules.

#### Scenario: Dependency graph remains unchanged
- **WHEN** the migration is implemented
- **THEN** no new dependency is added to `gradle/libs.versions.toml` or `shared/build.gradle.kts`
- **AND** `top.yukonga.miuix.kmp:miuix-navigation3-adaptive` remains absent
