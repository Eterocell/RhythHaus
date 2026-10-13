# Tasks

## 1. Source rebind contract

- [x] 1.1 Add failing repository tests proving successful rebind preserves the original source ID, creation time, tracks, favorites, history, metadata overrides and playlist references; prove duplicate-handle rejection leaves both sources unchanged.
- [x] 1.2 Implement a transactional `rebindSource(sourceId, replacement)` result contract with explicit missing-source, platform-mismatch, and handle-conflict outcomes; verify existing schema stores the new handle and needs no migration.

## 2. Platform recovery selection

- [x] 2.1 Add target-source-aware folder picker results and failing tests for success, cancellation, inaccessible replacement, and platform mismatch.
- [x] 2.2 Implement Android SAF replacement-grant persistence and safe cleanup on rejected admission; retain existing MediaStore permission recovery.
- [x] 2.3 Validate desktop folder replacement readability and preserve dropped-file-set recovery; keep iOS `Documents/RhythHaus` fixed and recoverable only through managed-folder recreation/rescan.
- [x] 2.4 Run focused Android host/JVM picker and source-access tests plus iOS test-code compilation.

## 3. Coordinator-owned recovery

- [x] 3.1 Add failure-first Shared tests for coordinator admission, stale publication, and recovery cancellation without writes.
- [x] 3.2 Ensure inaccessible-root scans fail rather than complete empty; verify failed/cancelled scans cannot authorize remove-missing or replace the latest completed summary.
- [x] 3.3 Implement rebind using the App-owned operation coordinator; publish only after accepted persistence, and do not start scanning automatically.
- [x] 3.4 Run focused Shared operation/publication and Library scanner tests.

## 4. Settings recovery UX

- [x] 4.1 Add localized accessible recovery actions and explanations by source kind; keep lost-access actions separate from remove-missing confirmation.
- [x] 4.2 Add UI regressions for Android SAF, MediaStore, desktop folder/drop-set, iOS managed source, cancellation/conflict, and compact/wide busy states.
- [x] 4.3 Run Library/Settings JVM UI tests and resource ownership verification; existing Settings route/viewport assertions remain baseline failures and are recorded in progress.

## 5. Integrated validation and lifecycle

- [x] 5.1 Run focused repository, scanner, Shared, Settings, Android host, iOS test-code compilation, and desktop compilation.
- [x] 5.2 Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict OpenSpec validation, and `git diff --check` as separate gates.
- [x] 5.3 Run SQLDelight generation/migration and database regression tests if implementation requires any schema change; otherwise record that source ID/handle fields suffice.
- [x] 5.4 Obtain independent full-change review, resolve Critical/Important findings, and rerun affected regressions.
- [x] 5.5 Sync canonical `local-library-scanning` spec; archive and update roadmap/progress with exact verification evidence and user-owned manual-acceptance gaps; commit the completed feature branch.
