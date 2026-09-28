# SDD ledger — plan: docs/superpowers/plans/2026-09-24-sorting-filtering.md

Base: dcbbe692ca2e5687f305edbc6d99a7b83c3fc190

## Preflight scan

| Task | Shared file/interface | Producer/consumer | Finding |
|---|---|---|---|
| 1 -> 2 | `LibraryBrowseQuery` and visible projection | Task 1 produces pure query API; Task 2 consumes it in Shared shell | Clean; Task 1 must preserve existing mode membership and grouped pass-through. |
| 2 -> 3 | Query state, source/metadata maps | Task 2 produces callbacks/projections; Task 3 renders controls | Clean; controls remain feature-owned and do not access repository. |
| 2 -> 4 | Query projection and visible IDs | Task 2 wires state; Task 4 reconciles selection/playback | Clean; visible IDs are authoritative at the Home presentation boundary. |
| 3 -> 4 | Home controls and visible list | Task 3 changes query; Task 4 protects selected IDs and queues | Clean; query changes must not mutate playback state. |
| 1 | Pure helper and tests | Internal consistency | Clean; all required sort/filter boundaries are testable without Compose. |
| 2 | Shared state/wiring tests | Internal consistency | Clean; no schema or platform code required. |
| 3 | Controls/resources/tests | Internal consistency | Clean; filtered-empty state must remain passive. |
| 4 | Selection reducer/shell/tests | Internal consistency | Clean; exact visible order feeds queue callback. |
| 5 | Quality/spec gates | Internal consistency | Clean; existing baseline blockers are recorded, not hidden. |

No conflicts or plan-mandated review defects found. Ruling: proceed with pure projection first; if implementation reveals missing metadata at the existing `Track` boundary, add only the narrow immutable map specified by the design rather than widening `Track`.

Task 1: complete
- Commit: 166f762d
- Focused tests: 15 passed.
- Review: Ready; no Critical, Important, or Minor findings.

Task 2: complete
- Commit: 8f4e8bab
- Focused tests: Shared BrowseQueryState 5/0, AppShell 9/0, LibraryHomeContent 18/0.
- Review: Ready; no Critical, Important, or Minor findings.

Task 3: complete
- Commits: cbbdd094, b5a17656; focused controls/Home/resource tests passed.
- Review: five UI findings repaired and re-reviewed Ready.

Task 4: complete
- Commits: e5add1b0, f216fa6e; LibraryHomeContentJvmTest and Shared AppShell/selection/query-state selectors passed.
- Review: three behavioral-evidence gaps repaired; exact real-controller multi-track queue, compact and wide routes, and unchanged playback snapshots covered.
- Existing Home-to-Shared callback already published the complete visible projection; Task 4 adds boundary regressions without a production-source change.

Full-branch review: three Important production defects were found and repaired in 0cef8d5d. Artwork-only now uses non-blob persisted artwork presence; explicit Title/Ascending no longer aliases recent-mode defaults; missing numeric metadata sorts as zero. A Minor recent-mode accessibility-state mismatch was repaired in 4e539ff1, then independently re-reviewed Ready. SQL repository, Library pure/Home UI, Shared query/AppShell, iOS test-code compilation, Spotless, Detekt, architecture, and strict OpenSpec checks passed. Full Shared JVM suite retains the previously established unrelated playback-selection timeout; Android assembly retains the TagLib native toolchain blocker.
