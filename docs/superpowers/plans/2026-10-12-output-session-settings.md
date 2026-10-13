# Output and Session Settings Implementation Plan

> For agentic workers: execute task-by-task through the established subagent-driven workflow.

**Goal:** Add truthful active-output status, persistent interruption-resume policy, and explicit artwork-cache clearing to shared Settings.

**Architecture:** Shared owns persisted policy, neutral state projection, and Settings orchestration. Platform hosts own route facts and interruption enforcement; a platform without a reliable active-route query reports unavailable. Coil cache clearing is separate and cannot mutate library or playback state.

**Spec:** `openspec/changes/output-session-settings/specs/playback-session-settings/spec.md`; design: `openspec/changes/output-session-settings/design.md`.

## Global Constraints

- Active-output status is reported only when a platform API identifies the active route; otherwise show unavailable.
- Default automatic-resume policy is `FollowSystemRecommendation`; `Never` suppresses system-triggered resume.
- iOS requires OS `shouldResume`; Android resumes only after transient focus loss and subsequent gain; macOS route loss remains paused.
- Explicit pause, stop, play, selection, or session replacement supersedes pending system resume.
- Cache action clears only Coil image memory and disk caches; no database/library/media/playback mutation.
- No new dependencies, schema migration, output selection, or Windows/Linux support.

## Review Focus

- Android transient focus gain must not resurrect playback after explicit pause/selection.
- iOS stale interruption callbacks must not resume a replacement generation.
- Unknown routes must render unavailable, not a connected-device list.
- Cache failure must retain normal Settings state and avoid library/playback callbacks.
- Rapid policy changes while interrupted use the last durably accepted preference.

## Tasks

### 1. Resume policy persistence

Create the policy/store/owner alongside existing playback preference owners; wire DI and add default, persistence/reopen, malformed-value fallback, concurrent-write, and lifecycle tests. Run focused tests and commit the checkpoint.

### 2. Shared policy routing and Settings controls

Thread authoritative state through App and LibraryRoutes. Add EN/ZH controls with non-optimistic selection, write failure, busy/modal inertness, accessibility, and compact/wide reachability tests. Commit the checkpoint.

### 3. Output contract and cache action

Add `Available(name)`/`Unavailable` output state, cache action with explicit success/failure, localized Settings status/action, and no-side-effect tests. Commit the checkpoint.

### 4. Native enforcement

Implement Android transient-focus admission with generation/intent fencing, iOS route/resume-policy handling, and macOS output query without weakening route-loss lifecycle guarantees. Add host/native regression tests and platform compiles. Commit the checkpoint.

### 5. Integrated closeout

Run affected JVM/host/iOS/desktop suites, separate formatting/Detekt/architecture gates, strict OpenSpec validation, and diff checks. Obtain independent review, sync/archive the capability, update roadmap/progress, and commit conventionally.
