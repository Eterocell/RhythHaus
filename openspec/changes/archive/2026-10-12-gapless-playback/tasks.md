# Tasks

## 1. Preference and policy contract

- [x] 1.1 Add RED tests for default Off, persistence, restart restoration before playback readiness, and cancellation-safe policy changes.
- [x] 1.2 Implement the process-owned gapless preference/store and Shared Settings control with EN/ZH accessible semantics.
- [x] 1.3 Review policy ownership and crossfade precedence; commit the checkpoint.

## 2. Controller gapless admission

- [x] 2.1 Add RED tests for preload threshold, effective repeat/shuffle successor selection, duplicate occurrences, unavailable durations, and no-successor fallback.
- [x] 2.2 Implement generation/run/occurrence-fenced gapless preparation and completion-boundary promotion using the existing engine seam.
- [x] 2.3 Add race regressions for explicit selection, seek, pause/stop/skip, stale preparation callbacks, prepared failure, and duplicate completion.
- [x] 2.4 Add ReplayGain, sleep-timer, progress, and completion-count invariants; review the controller boundary.

## 3. Platform engine promotion

- [x] 3.1 Add Android host regressions for prepared successor promotion, Media3 token fencing, cleanup, and service-session identity.
- [x] 3.2 Add iOS provider/engine regressions for prepared successor promotion, interruption/route cleanup, and stale delegate callbacks.
- [x] 3.3 Add macOS native/JVM regressions for two-file preparation, end-boundary promotion, no idle state, and native resource release.
- [x] 3.4 Implement the narrow platform staging/promotion seam and retain ordinary fallback when native preparation fails.
- [x] 3.5 Obtain independent platform-boundary review and repair/re-review findings.

## 4. Integration acceptance

- [x] 4.1 Run affected Core, Settings, Android-host, Shared, desktop, and iOS compile/test checks plus separate formatting, Detekt, architecture, OpenSpec, and diff gates.
- [x] 4.2 Run a permanent temporary-WAV integration regression through controller and native macOS bridge; assert both hashes unchanged, successor promotion, no idle gap state, and cleanup.
- [x] 4.3 Obtain independent stable-revision full-change review and repair/re-review all findings.
- [x] 4.3a Re-review found pause-preparation cleanup and TrackCount completion-accounting gaps; fixed both and verified focused regressions.
- [ ] 4.4 Sync canonical gapless/crossfade/playback-failure specifications, archive the change, update roadmap/progress, and commit. User performs physical audible/device/background/system-control acceptance.
