# Tasks

## 1. Playback timer authority

- [ ] 1.1 Write failing controller behavior tests for deadline, completions, fade and stale-event safety.
- [ ] 1.2 Implement process-local authoritative timer state, commands, and required engine gain contract; migrate test engines and pass focused core tests.
- [ ] 1.3 Independently review controller and gain contract races.

## 2. Platform output gain

- [ ] 2.1 Implement and test Android Media3 playback-only gain, including service connection and reset.
- [ ] 2.2 Implement and test iOS serialized Swift-provider gain and reset, retaining existing teardown fade.
- [ ] 2.3 Implement and test macOS AVAudioPlayer gain through JNI, including replacement and release.
- [ ] 2.4 Independently review platform gain ownership and ABI across all three engines.

## 3. Now Playing controls

- [ ] 3.1 Write failing UI behavior and accessibility tests for timer controls in compact and split layouts and Shared route wiring.
- [ ] 3.2 Implement localized Now Playing controls, active state, fade choice and Shared timer actions; pass focused UI tests.
- [ ] 3.3 Independently review route reachability, accessibility, localization and playback isolation.

## 4. Verification and lifecycle

- [ ] 4.1 Run relevant cross-platform automated checks, native smoke, formatting, static/architecture gates, and strict OpenSpec validation; record exact blockers.
- [ ] 4.2 Complete full-branch independent review and resolve actionable findings.
- [ ] 4.3 Synchronize canonical spec, record roadmap/progress evidence and outstanding user-owned physical acceptance, archive the change and commit lifecycle artifacts.
