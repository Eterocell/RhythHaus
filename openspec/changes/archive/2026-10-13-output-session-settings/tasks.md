# Tasks

## 1. Shared resume policy and Settings

- [x] 1.1 Add `AutomaticResumePolicy` plus an isolated process-owned preference store/owner; test default, persistence, corrupt-value fallback, and owner lifecycle.
- [x] 1.2 Thread the authoritative policy into the platform playback host; test startup restoration and policy updates without rebuilding or changing the playback queue.
- [x] 1.3 Add EN/ZH Settings controls for Follow system recommendation/Never, busy/error/accessibility states; test non-optimistic selection, persistence failure, modal/busy inertness, and compact/wide reachability.

## 2. Platform output and interruption behavior

- [x] 2.1 Add a neutral output-status projection and route refresh contract; test Available/Unavailable mapping and stale route-event rejection.
- [x] 2.2 Implement iOS current-route name and resume-policy veto; test route updates, shouldResume true/false, Never, and user intent superseding pending resume.
- [x] 2.3 Implement Android transient-focus resume admission with generation/intent fencing and truthful output status; test transient gain, permanent loss, user command supersession, session replacement, and unavailable output.
- [x] 2.4 Add macOS active-output name query without changing existing route-loss behavior; test available/unavailable device values and listener lifecycle.

## 3. Artwork cache clearing

- [x] 3.1 Add a testable Coil memory/disk cache-clear action with recoverable result mapping; test both caches are cleared and backend failure is reported without touching library/playback state.
- [x] 3.2 Add localized Settings action, confirmation/status/error semantics, and UI tests covering success, failure, modal/busy behavior, and no scanner/database/playback callbacks.

## 4. Integrated verification

- [x] 4.1 Run affected JVM, Android host, iOS Simulator, Swift provider, desktop/Android/iOS compile, formatting, Detekt, architecture, strict OpenSpec, and diff checks; record exact outputs and known unrelated failures.

## Workflow follow-up

- Archive the change after independent review and canonical spec synchronization.
- Update `roadmap.md` and `progress.md`, commit the completed OpenSpec workflow, and proceed to the next roadmap entry.
