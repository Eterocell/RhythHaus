# Task 2 Report: Shared HausDialog Foundation

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausDialog.kt`
  - Added the pure `HausDialogPresentation` and `hausDialogPresentation` policy.
  - Added the internal slot-based `HausDialog` shell with dialog/pane/dismiss semantics, scrim dismissal, panel tap containment, an opaque Miuix `Card`, and bounded scrolling.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt`
  - Added only the layout, explicit palette-pair, and compact text-metric APIs required to compile the Task 1 playlist presentation tests.
  - Did not apply the policy to `PlaylistScreens.kt`; that remains Task 4.
- `.superpowers/sdd/task-2-report.md`
  - Replaced the previous unrelated generic Task 2 report with this task's required evidence.

The Task 1 test files were consumed unchanged. No existing dialog caller was migrated.

## GREEN

Exact command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Exact successful output ending:

```text
> Task :shared:compileKotlinJvm
> Task :shared:compileJvmMainJava NO-SOURCE
> Task :shared:jvmMainClasses
> Task :shared:jvmJar
> Task :shared:compileTestKotlinJvm
> Task :shared:compileJvmTestJava NO-SOURCE
> Task :shared:jvmTestClasses
> Task :shared:jvmTest

BUILD SUCCESSFUL in 7s
26 actionable tasks: 8 executed, 18 up-to-date
Configuration cache entry reused.
```

All 39 focused tests passed.

## Concerns

- The brief's sample dark scrim branch copied `palette.paper`, but the Task 1 contract requires a scrim whose RGB luminance is greater than dark `paper`. The implementation therefore uses dark-palette `ink` at 20% alpha; light mode uses the required `ink` at 36% alpha.
- Kotlin LSP diagnostics could not run because `kotlin-ls` is not installed and installation was previously declined. The focused command compiled both common-main production files and the JVM tests successfully.
- No runtime visual QA is claimed because this task intentionally adds the foundation without migrating or rendering any existing dialog caller.
- `GIT_MASTER=1 git diff --check` passed with no output. No commit was created.

## Reviewer follow-up: fixed action footer

The reviewer found that the original single `content` slot was attached to the same `Column` as `verticalScroll`, so caller-owned confirmation controls would scroll with a long body. The regression now compiles a `HausDialog` call with separate named `body` and `actions` slots.

### RED

After adding only the separate-slot contract to `HausDialogTest.kt`, ran:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Exact failure:

```text
> Task :shared:compileTestKotlinJvm FAILED
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt:44:9 No parameter with name 'body' found.
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt:45:9 No parameter with name 'actions' found.
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogTest.kt:45:9 No value passed for parameter 'content'.

BUILD FAILED in 1s
```

### GREEN

`HausDialog` now accepts a caller-owned `ColumnScope` body and a caller-owned `RowScope` action slot. Only the body column has `verticalScroll`; the action row is its fixed sibling below it inside the same bounded card.

Final exact command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Exact successful output ending:

```text
> Task :shared:compileKotlinJvm
> Task :shared:compileJvmMainJava NO-SOURCE
> Task :shared:jvmMainClasses
> Task :shared:jvmJar UP-TO-DATE
> Task :shared:compileTestKotlinJvm UP-TO-DATE
> Task :shared:compileJvmTestJava NO-SOURCE
> Task :shared:jvmTestClasses UP-TO-DATE
> Task :shared:jvmTest UP-TO-DATE

BUILD SUCCESSFUL in 3s
26 actionable tasks: 5 executed, 21 up-to-date
Configuration cache entry reused.
```

`GIT_MASTER=1 git diff --check` passed with no output. Kotlin LSP remains unavailable because `kotlin-ls` installation was previously declined. No settings or playlist caller was migrated, and no commit was created.
