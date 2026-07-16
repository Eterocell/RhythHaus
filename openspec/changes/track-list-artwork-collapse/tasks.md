## 1. Collapse geometry and consumption policy

- [x] 1.1 Add failing common tests for expanded, partial, collapsed, clamped, zero-range, inverted-range, and resized artwork geometry.
- [x] 1.2 Implement the pure artwork-collapse geometry, snapshot, and signed scroll-consumption policy.
- [x] 1.3 Run focused common tests and review the policy for one-to-one consumption and safe clamping.

## 2. Shared drill-down integration

- [x] 2.1 Add failing common coverage for artwork/no-artwork branch selection and shared chrome/content geometry.
- [x] 2.2 Add the remembered app-owned nested-scroll adapter with negative pre-scroll collapse and positive post-scroll expansion.
- [x] 2.3 Wire artwork-backed `DrillDownView` and `DrillDownMiuixScrollChrome` to one current geometry snapshot while preserving the no-artwork Miuix path.
- [x] 2.4 Run focused Library UI tests and shared JVM compilation, then review album/artist, navigation, playback, scrollbar, and safe-inset preservation.

## 3. Verification and durable evidence

- [x] 3.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, and diff hygiene.
- [x] 3.2 Perform source-level and runtime visual QA where available for album/artist pages with and without artwork at compact and wide widths.
- [x] 3.3 Run final code-quality and specification reviews; fix only findings introduced by this change and repeat affected verification.
- [x] 3.4 Update roadmap item 21, `progress.md`, this checklist, and change-specific SDD reports with exact evidence and remaining manual limitations.

> Sections 1-3 document the original nested-scroll implementation. Live macOS testing later disproved that architecture; their completed status records historical work, not current acceptance.

## 4. Single-owner architecture replacement

- [x] 4.1 Add failing common tests for list-position collapse progress, zero/inverted geometry, aligned upper/lower artwork slices, and exact item-zero restoration.
- [x] 4.2 Replace the artwork branch with one `LazyColumn` sequence: upper artwork range, sticky collapsed artwork toolbar, section, keyed rows, and existing bottom spacer.
- [x] 4.3 Remove the artwork nested-scroll adapter, sibling `scrollable`, dynamic top padding/translated viewport compensation, and imperative collapse offset while preserving the no-artwork Miuix branch.
- [x] 4.4 Preserve title/scrim/background transitions, a continuously available safe-inset back target, exact scrollbar-to-`scrollToItem(0, 0)`, navigation, playback selection, and Now Playing spacing.
- [x] 4.5 Remove the disposable desktop prototype, its launch task, prototype tests, and dependencies that are no longer needed after production evidence is captured.
- [x] 4.6 Replace the artwork-toolbar liquid-glass/blur and measured sibling overlay with a tested in-sticky progressive solid `HausColors.paper` background, while leaving the bottom/Now Playing bar unchanged.

## 5. Replacement verification and acceptance

- [x] 5.1 Run focused collapse/navigation/artwork tests, shared JVM tests, desktop compilation, Android debug assembly, Xcode availability, iOS simulator tests, strict OpenSpec validation, and diff hygiene.
- [x] 5.2 Perform physical macOS trackpad QA on production album and artist pages for artwork-zone scrolling, partial/full collapse, deep reverse restoration, scrollbar top restoration, and back interaction.
- [x] 5.3 Perform visual QA at compact and wide widths for seamless artwork slices, sticky chrome, title/background transitions, no transient gap, and no-artwork Miuix behavior.
- [x] 5.4 Run final code-quality, specification, and Oracle review gates; update `progress.md`, roadmap/OpenSpec evidence, and change-specific SDD reports without claiming unsupported platform acceptance.
