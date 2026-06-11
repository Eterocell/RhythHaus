## 1. OpenSpec and dependency setup

- [ ] 1.1 Validate this OpenSpec change before implementation starts.
- [ ] 1.2 Add SQLDelight or equivalent KMP database dependencies and generated database configuration.
- [ ] 1.3 Verify dependency setup with focused Gradle tasks before feature code.

## 2. Shared library domain and persistence

- [ ] 2.1 Add shared models for `LibrarySource`, `LibraryTrack`, `ScanSession`, `ScanProgress`, `ScanError`, and audio candidates.
- [ ] 2.2 Add database schema for sources, tracks, scan sessions, and scan errors.
- [ ] 2.3 Add `LibraryRepository` API and SQL-backed implementation.
- [ ] 2.4 Add repository tests for upsert/no-duplicates, scan-session state, scan errors, and remove-missing behavior.

## 3. Shared scanner orchestration

- [ ] 3.1 Add platform scanner and folder/source picker contracts.
- [ ] 3.2 Add recursive scan orchestration with progressive progress updates.
- [ ] 3.3 Add cancellation behavior that preserves already imported tracks.
- [ ] 3.4 Add metadata enrichment fallback through the existing metadata reader.
- [ ] 3.5 Add scanner tests with fake platform scanners.

## 4. Platform source implementations

- [ ] 4.1 Add Android SAF tree source picker, persisted URI permission, and recursive document traversal.
- [ ] 4.2 Add macOS/JVM native folder picker and recursive filesystem traversal.
- [ ] 4.3 Add iOS app-local music folder provisioning and recursive scanner.
- [ ] 4.4 Add platform-focused tests where practical.

## 5. Shared library manager UI

- [ ] 5.1 Replace primary import card copy/action with add/manage music folder UI.
- [ ] 5.2 Show empty, scanning, completed, cancelled, failed, and lost-access states.
- [ ] 5.3 Add scan actions: cancel, retry/rescan, add source, remove missing, view scan report.
- [ ] 5.4 Keep playback wired through persisted scan-produced tracks.

## 6. Migration, verification, and handoff

- [ ] 6.1 Decide whether to keep the old file-import path as secondary or remove it after folder scanning is functional.
- [ ] 6.2 Run focused tests and full `./init.sh`.
- [ ] 6.3 Validate `openspec validate scan-local-audio-folders --strict`.
- [ ] 6.4 Update `progress.md` with evidence, limitations, and next safe action.
- [ ] 6.5 Commit completed OpenSpec + Superpowers workflow changes with semantic commit messages.
