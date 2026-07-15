## 1. Collapse geometry and consumption policy

- [ ] 1.1 Add failing common tests for expanded, partial, collapsed, clamped, zero-range, inverted-range, and resized artwork geometry.
- [ ] 1.2 Implement the pure artwork-collapse geometry, snapshot, and signed scroll-consumption policy.
- [ ] 1.3 Run focused common tests and review the policy for one-to-one consumption and safe clamping.

## 2. Shared drill-down integration

- [ ] 2.1 Add failing common coverage for artwork/no-artwork branch selection and shared chrome/content geometry.
- [ ] 2.2 Add the remembered app-owned nested-scroll adapter with negative pre-scroll collapse and positive post-scroll expansion.
- [ ] 2.3 Wire artwork-backed `DrillDownView` and `DrillDownMiuixScrollChrome` to one current geometry snapshot while preserving the no-artwork Miuix path.
- [ ] 2.4 Run focused Library UI tests and shared JVM compilation, then review album/artist, navigation, playback, scrollbar, and safe-inset preservation.

## 3. Verification and durable evidence

- [ ] 3.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, and diff hygiene.
- [ ] 3.2 Perform source-level and runtime visual QA where available for album/artist pages with and without artwork at compact and wide widths.
- [ ] 3.3 Run final code-quality and specification reviews; fix only findings introduced by this change and repeat affected verification.
- [ ] 3.4 Update roadmap item 21, `progress.md`, this checklist, and change-specific SDD reports with exact evidence and remaining manual limitations.
