# Task 3 Report: Remove the disposable desktop prototype

## Status

DONE_WITH_CONCERNS

The disposable desktop artwork-scroll prototype was removed after its evidence was confirmed as preserved. Normal desktop startup, compilation, tests, and macOS packaging configuration were retained.

## Evidence check

Before deletion, `progress.md` and the amended OpenSpec design were inspected.

`progress.md` preserves:

- Focused prototype test RED/GREEN evidence.
- Full desktop verification: `./gradlew :desktopApp:compileKotlin :desktopApp:test --configuration-cache` with `BUILD SUCCESSFUL`.
- Prototype launch reaching `:desktopApp:runArtworkScrollPrototype` with `ArtworkScrollPrototypeKt`.
- User-confirmed physical macOS acceptance for artwork-zone scrolling, deep reverse restoration, `Restore top`, and back-button interaction.
- The prototype's role as disposable diagnostic evidence rather than production functionality.

The amended design preserves the rationale that the one-`LazyColumn` topology passed physical macOS routing/restoration checks and that the prototype validated routing/restoration rather than final visual fidelity.

## Changes

Modified only the prototype additions in `desktopApp/build.gradle.kts`:

- Removed `implementation(libs.compose.material3)`.
- Removed `implementation(libs.compose.material.icons.extended)`.
- Removed `testImplementation(libs.kotlin.testJunit)`.
- Removed the `runArtworkScrollPrototype` `JavaExec` task and its `ArtworkScrollPrototypeKt` main-class reference.

Deleted only these prototype files:

- `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototype.kt`
- `desktopApp/src/test/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototypeTest.kt`

Preserved unchanged:

- `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`.
- `compose.desktop.application.mainClass = "com.eterocell.rhythhaus.MainKt"`.
- Koin startup through `main.kt`.
- Existing shared/Desktop dependencies and `compose.uiToolingPreview`.
- macOS DMG packaging, package name, and version configuration.
- All shared production/test files, OpenSpec/docs/progress files, and unrelated dirty work.

## Verification

Exact required command:

```text
./gradlew :desktopApp:compileKotlin :desktopApp:test --configuration-cache
```

Result:

- `BUILD SUCCESSFUL in 1s`
- `38 actionable tasks: 7 executed, 31 up-to-date`
- Configuration cache entry stored.
- `:desktopApp:compileKotlin` passed.
- `:desktopApp:test` passed.

Additional checks:

- `git diff --check` passed with no output.
- Both prototype files were confirmed absent after deletion.
- No remaining live source/build references to `ArtworkScrollPrototype`, `ArtworkPrototypeVisualState`, `artworkPrototypeVisualState`, or `runArtworkScrollPrototype` were found.
- Remaining textual matches are intentional historical references in `progress.md` and the approved Task 3 plan, which were not modified.

## Self-review

- The build diff removes exactly the 11 prototype-added lines and retains all pre-existing desktop build/package lines.
- The normal `MainKt` entry point remains configured and `main.kt` was not modified.
- No dependency used by remaining desktop sources was removed; the exact prototype-only Material 3, extended-icons, and Kotlin JUnit dependencies were removed with their only consumers.
- No commit, reset, revert, suppression, or unrelated cleanup was performed.

## Concerns

- The Gradle run emitted existing repository warnings about an incubating parallel configuration-cache feature, a `taglib` Android host-test configuration, and deprecated Gradle features. None failed the requested desktop compile/test command.
- Kotlin LSP diagnostics were not available because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests provide the executable Kotlin verification for this cleanup.
- Historical evidence intentionally continues to mention the removed prototype so the accepted physical macOS result remains auditable.

Route: openspec+superpowers / disposable prototype cleanup
Owner: implementation
Input: `.superpowers/sdd/task-3-brief.md` and preserved prototype evidence
Output: prototype build/source/test removed; desktop compile/test passed; this report
Next owner: OpenSpec/Superpowers for Task 3 ledger completion or archival when explicitly requested
Blockers: none for the requested desktop cleanup
