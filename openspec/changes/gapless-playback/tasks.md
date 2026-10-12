# Tasks

## 1. Preference and policy contract

- [ ] 1.1 Add RED tests for default Off, persistence, restart restoration before playback readiness, and cancellation-safe policy changes.
- [ ] 1.2 Implement the process-owned gapless preference/store and Shared Settings control with EN/ZH accessible semantics.
- [ ] 1.3 Review policy ownership and crossfade precedence; commit the checkpoint.

## 2. Controller gapless admission

- [ ] 2.1 Add RED tests for preload threshold, effective repeat/shuffle successor selection, duplicate occurrences, unavailable durations, and no-successor fallback.
- [ ] 2.2 Implement generation/run/occurrence-fenced gapless preparation and completion-boundary promotion using the existing engine seam.
- [ ] 2.3 Add race regressions for explicit selection, seek, pause/stop/skip, stale preparation callbacks, prepared failure, and duplicate completion.
- [ ] 2.4 Add ReplayGain, sleep-timer, progress, and completion-count invariants; review the controller boundary.

## 3. Platform engine promotion

- [ ] 3.1 Add Android host regressions for prepared successor promotion, Media3 token fencing, cleanup, and service-session identity.
- [ ] 3.2 Add iOS provider/engine regressions for prepared successor promotion, interruption/route cleanup, and stale delegate callbacks.
- [ ] 3.3 Add macOS native/JVM regressions for two-file preparation, end-boundary promotion, no idle state, and native resource release.
- [ ] 3.4 Implement the narrow platform staging/promotion seam and retain ordinary fallback when native preparation fails.
- [ ] 3.5 Obtain independent platform-boundary review and repair/re-review findings.

## 4. Integration acceptance

- [ ] 4.1 Run Core, Settings, Android-host, TagLib/native, Library/Shared, desktop, and iOS compile/test checks plus separate formatting, Detekt, architecture, OpenSpec, and diff gates.
- [ ] 4.2 Run a permanent temporary-WAV integration regression through controller and native macOS bridge; assert both hashes unchanged, successor promotion, no idle gap state, and cleanup.
- [ ] 4.3 Obtain independent stable-revision full-change review and repair/re-review all findings.
- [ ] 4.4 Sync canonical gapless/crossfade/playback-failure specifications, archive the change, update roadmap/progress, and commit. User performs physical audible/device/background/system-control acceptance.
