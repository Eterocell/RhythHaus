## 1. Shared Dialog Foundation

- [x] 1.1 Add a common-main `HausDialog` shell and pure theme/presentation policy for solid panels, theme-aware scrims, semantics, bounded body scrolling, and touch containment.
- [x] 1.2 Add focused common tests for the dialog presentation policy, panel/scrim colors, bounds, and accessible dismissal contract.

## 2. Dialog Migration

- [x] 2.1 Migrate Clear Library and Remove Folder dialogs to `HausDialog`, removing liquid-glass/backdrop-only dialog wiring while preserving callbacks, copy, and source-name semantics.
- [x] 2.2 Migrate create/rename, Add to Playlist, playlist confirmation, and Clear Upcoming dialogs to `HausDialog`, preserving validation, failure notices, selection state, and mutation callbacks.

## 3. Playlist Visual Polish

- [x] 3.1 Introduce playlist layout and action presentation policies that match Library home content insets and provide explicit contrasting tab/action colors and text-safe metrics.
- [x] 3.2 Apply the playlist policies to the hub frame, Saved/Queue tabs, and compact actions; add focused tests for inset parity, palette contrast, and compact text fit.

## 4. Verification and Evidence

- [x] 4.1 Run strict OpenSpec validation, focused and full shared JVM verification, desktop compilation, Android debug assembly, and iOS validation; record exact blockers if any supported check fails.
- [ ] 4.2 Run available compact/wide and light/dark visual QA for all migrated dialogs and playlist controls; update `progress.md` and `roadmap.md` with evidence and remaining manual gaps.

Task 5 evidence is recorded in `.superpowers/sdd/task-5-report.md`. OpenSpec 4.2 remains open: a later desktop retry established interactive compact (800×600) and wide (1728×1084) accessibility snapshots for the Playlists → Create Playlist path, including localized Chinese controls and Cancel dismissal, but the captured PNGs could not be rendered by the available review tooling. Pixel-level opacity, scrim, spacing, clipping, theme, and CJK metrics remain unverified; other dialog families and Android/iOS runtime QA remain outstanding.

## 5. Accessibility Follow-up

- [x] 5.1 Add a JVM-only Compose UI-test harness and execute a `SemanticsActions.Dismiss` regression for `HausDialog` without changing Android, iOS, or production dependencies.
