## 1. Common overlap capability and policy

- [x] 1.1 Implement the documented dual-voice capability, duration choices, scheduling seam and bounded effective-window policy; verify policy boundaries and gain validation.

## 2. Controller transition authority

- [x] 2.1 Implement fenced preparation, incoming promotion, per-voice envelopes and retiring completion handling without duplicate history or checkpoints.
- [x] 2.2 Verify queue/transport/reconciliation races, failures, ReplayGain changes, pause/resume and sleep-timer admission with behavioral regressions.

## 3. macOS dual-player adapter

- [x] 3.1 Implement real native overlap with one system/route authority, captured callbacks and safe resource retirement.
- [x] 3.2 Verify simultaneous native positions, per-voice gains, transport, failure and cleanup using disposable unchanged media.

## 4. Android service dual-player adapter

- [x] 4.1 Implement secondary ExoPlayer lifecycle and logical-player promotion behind the existing session and internal-command admission.
- [x] 4.2 Verify focus/noisy behavior, both-player transport, captured callbacks, preparation failure and resource cleanup.

## 5. iOS native dual-player adapter

- [x] 5.1 Implement staged/current/retiring native voices, captured completion identity and one AVAudioSession/remote authority; migrate bridge consumers.
- [x] 5.2 Verify native overlap, per-voice gains, interruption/route handling, failure and teardown through bridge tests and Swift XCTest.

## 6. Persisted policy and Settings cutover

- [x] 6.1 Implement process-owned durable preference authority and restore ReplayGain then Crossfade before session playback readiness.
- [x] 6.2 Migrate production routes and callers to localized accessible duration controls with busy/error and exclusive-modal behavior.
- [x] 6.3 Verify corruption/reopen, cancellation-safe writes, recreation and slow startup using real-file and actual-Koin regressions.

## 7. Integration and lifecycle

- [x] 7.1 Exercise true overlap through the actual controller with disposable native media, check history/checkpoint ownership and unchanged media bytes.
- [x] 7.2 Run affected test/platform matrix and separate formatting, lint and architecture gates; record baseline failures and user-owned physical acceptance accurately.
- [x] 7.3 Complete independent whole-change review, repair findings and rerun affected verification.
- [x] 7.4 Synchronize canonical specs, update roadmap/progress, archive the completed change and commit without merging or pushing.
