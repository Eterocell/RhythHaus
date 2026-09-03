## Purpose

Provide an edge-aware blurred drill-down top chrome that preserves clear scrolling content below it while retaining the established uniform glass treatment for floating surfaces.

## ADDED Requirements

### Requirement: Drill-down chrome uses a top-edge blur ramp

When a supported backdrop renderer is available, the drill-down top chrome SHALL be strongest at its top edge and continuously fade to a pixel-sharp clear state at its lower content edge. The surface SHALL retain its existing shape, tint, title, controls, sizing, and scroll behavior.

#### Scenario: Supported detail chrome fades into scrolling content
- **WHEN** a detail screen displays its collapsed top chrome on a renderer supporting the backdrop effect
- **THEN** the chrome is blurred at the system-bar edge
- **AND** the effect progressively clears toward the scrollable content below
- **AND** the lower clear endpoint remains full-resolution.

#### Scenario: Unsupported rendering retains the fallback surface
- **WHEN** backdrop rendering or runtime shaders are unavailable
- **THEN** the drill-down chrome renders its existing opaque fallback surface
- **AND** no blur or progressive-effect work is attempted.

### Requirement: Floating glass remains uniformly blurred

Floating glass surfaces SHALL retain uniform blur across their bounded region. The Now Playing bar SHALL NOT adopt the drill-down top-edge gradient.

#### Scenario: Now Playing is not affected by drill-down blur adoption
- **WHEN** the Now Playing bar renders with a supported backdrop
- **THEN** its full rounded surface retains uniform blur
- **AND** it has no one-directional clear edge.
