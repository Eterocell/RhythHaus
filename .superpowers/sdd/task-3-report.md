# Task 3 Report: Settings Configured-Source Management

## Scope

Implemented Task 3 only: configured-source management in shared Settings UI, English/Simplified Chinese localization, pure UI decisions, scan-active mutation gating, source removal confirmation, and picker visibility propagation to the empty-library add card.

No repository persistence, platform implementation, scan orchestration, dependency, navigation, playback, schema, OpenSpec, progress, or roadmap files were changed by this task.

## Implementation

- Settings now uses a scrollable operational layout suitable for multiple configured sources.
- Each persisted source renders a compact row with:
  - display name, falling back to the source handle only when the display name is blank;
  - available/lost-access state;
  - never-scanned/last-scanned state;
  - 44dp Miuix rescan and remove icon actions with localized accessibility descriptions.
- Add, rescan, remove, and removal confirmation are disabled while `ScanProgress.isActive` is true.
- Remove opens a localized confirmation overlay using the existing clear-library dialog's scrim, Miuix card/button composition, RhythHaus liquid-glass backdrop, spacing, and color treatment.
- The existing picker decision remains `supportsAdditionalSources || sourceCount == 0` and now controls both Settings and the empty-library add card. Android/JVM can add more sources; iOS can add its first source only.
- Added matching English and Simplified Chinese copy for configured folders, access state, scan state, actions, and removal confirmation.

## TDD Evidence

RED:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
BUILD FAILED
Unresolved references: SourceAccessLabel, SourceScanLabel, sourceManagementLabels, sourceMutationsEnabled
```

This was the expected failure because the pure source-status mapping and mutation-enabled decisions did not exist.

GREEN:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
BUILD SUCCESSFUL in 5s
```

The tests cover available/lost access, never/previously scanned state, and enabled behavior for null/completed versus scanning/cancelling progress.

## Verification

```text
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
BUILD SUCCESSFUL in 13s
```

Existing warnings only:

- Android `MediaMetadata.Builder.setArtworkData` deprecation.
- AAPT2 unknown `SWIFT_DEBUG_INFORMATION_*` environment variables.
- Existing Gradle deprecation notice.

Additional checks:

- `git diff --check`: pass, no output.
- Kotlin `lsp_diagnostics`: unavailable because `kotlin-ls` is not installed and installation was previously declined; JVM and Android Kotlin compilation passed.
- Scoped diff review: pass; only Task 3 implementation, resources, tests, and this report are intended for staging.

## Visual QA

- Design system: existing `HausColors`, Miuix controls, icon library, 20dp page gutters, established typography weights, and clear-library liquid-glass dialog pattern are reused.
- Layout: Settings is scrollable; source rows use a stable 72dp minimum; source text can wrap to two lines; status text can wrap to two lines; action targets are 44dp.
- States: active-scan controls have disabled interaction and reduced icon/content contrast; dialog confirmation also disables if a scan becomes active.
- CJK: Chinese strings use natural phrases and source/dialog text is not forced into a single line.
- Gap: no live Android/iOS/desktop screenshot or interaction capture was available. Two read-only visual review agents timed out, so rendered spacing, font metrics, blur appearance, and very long source-name behavior remain manual QA items.

## Concerns

- The UI reports whether a source has ever been scanned (`Never scanned` / `Last scanned`) but does not format the raw epoch as a date/time; this follows the brief's pure state-mapping requirement and avoids adding a cross-platform date-formatting dependency.
- Live verification should exercise a narrow mobile viewport with long English and Chinese source names, active scan disabled actions, lost access, and the removal dialog backdrop.
