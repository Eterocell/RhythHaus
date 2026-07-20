# Playlist edit mode Task 5 report

## Scope

Implemented only the shared dialog exterior policy for `HausDialog` and `HausLazyDialog`. Both variants already resolve their panel and scrim from the single `hausDialogPresentation` result, so no variant-specific rendering branch was added.

## RED evidence

The existing common policy test was changed first from its luminance assertion to an exact active-palette contract:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --configuration-cache --rerun-tasks
```

Result: `HausDialogTest.dialogPresentationUsesExactThemeAwareExteriorPolicy` failed at `HausDialogTest.kt:17`; `3 tests completed, 1 failed`; `BUILD FAILED in 16s`. This was expected: the prior policy produced `DarkHausPalette.ink.copy(alpha = 0.20f)` rather than the required dark-paper scrim.

## Production change

- Added the named `DarkHausDialogScrimAlpha = 0.72f` policy constant.
- `hausDialogPresentation(DarkHausPalette)` now returns `DarkHausPalette.panel` with `DarkHausPalette.paper.copy(alpha = 0.72f)`.
- The light policy remains `LightHausPalette.ink.copy(alpha = 0.36f)`.
- Removed the luminance-derived exterior decision. `HausDialog` and `HausLazyDialog` continue to use the same presentation object's `panelColor` and `scrimColor` while retaining their existing composable dismissal semantics.

## GREEN evidence

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 11s`; `35 actionable tasks: 35 executed`.

## Visual policy

Dark dialogs now keep the opaque dark `DarkHausPalette.panel` card against a genuinely dark, 72%-opaque `DarkHausPalette.paper` exterior. Light dialogs retain the established restrained 36%-opaque ink scrim. Exact color equality, rather than luminance, is the durable contract.

## Scope and concerns

- No playlist, Task 4, OpenSpec, progress, roadmap, dependency, or `.cortexkit/` files were changed.
- This task validates the common color policy and existing JVM dismiss semantics. Runtime pixel review across desktop, Android, and iOS remains outside Task 5's automated scope.
