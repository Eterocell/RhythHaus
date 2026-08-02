# Task 2.2 Final Acceptance Report

## Decision

Task 2.2 is accepted. Specification re-review and initial final quality review found no Critical or Important source findings. Cleanup verification accepted both source corrections and required one ledger correction: the private `RhythHausTheme` app composition remains in `:shared`. Final cleanup-ledger re-review PASS found no Critical, Important, or Minor findings. The two nonblocking source findings were corrected before closure: `ArtworkImageRole.keySuffix` is internal, and the core UI JVM ui-test version derives from `libs.versions.compose.multiplatform`.

## Accepted Boundary

`:core:ui` owns generic `ArtworkImage` role/cache-key rendering, the complete `ArtworkDecoder` expect/actual/cache family, `BackChip`, `RhythHausTopAppBar`, `HausClickable`, `RhythHausThemeMode`, `HausColorPalette`, palette resolution/locals, `HausColors`, the `HausDialog` family, localized Back resources, and public generated `Res`.

`:shared` retains the private `RhythHausTheme` app composition, `TrackArtwork` state/loading and `TrackArtworkImage.kt`, `ThemePreferenceStore`, feature UI/state/routes/gestures/scrubber/glass chrome. It uses `api(projects.core.ui)`; Compose Foundation and Compose Resources are API dependencies. The iOS framework does not export `:core:ui`.

`docs/architecture.md` and ADR 0001 already describe this exact boundary. No speculative canonical-document changes were needed. The approved HausDialog ownership and ArtworkImage symbol split are plan amendments; the original RED was the absence of `:core:ui` and its moved declarations. Full RED/GREEN history remains in `task-2.2-implementation-report.md`.

## Verification

Earlier final acceptance gates passed:

- `spotlessApply`
- strict-cache `:core:ui:allTests :shared:jvmTest architectureCheck`
- `:desktopApp:compileKotlin`
- `:androidApp:assembleDebug`, including packaged core UI resources
- `:shared:compileKotlinIosSimulatorArm64`
- `/usr/bin/xcrun xcodebuild -version` reporting Xcode 26.6
- standalone `spotlessCheck` and `detekt`
- strict `openspec validate feature-first-modularization --strict`
- git diff checks

Retained XML aggregates report `:core:ui` 51 tests and shared JVM 562 tests, with zero failures, errors, or skips. `jvmApiElements` is not API-boundary evidence; `commonMainApi` provided the relevant dependency evidence.

After the two minor cleanup corrections, the focused gate was rerun:

```text
./gradlew :core:ui:compileKotlinJvm :core:ui:jvmTest architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
```

It was BUILD SUCCESSFUL with 31 tasks and configuration cache stored. The all-target integration gate was then rerun:

```text
./gradlew :core:ui:allTests :shared:jvmTest architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
```

It was BUILD SUCCESSFUL with 93 tasks and configuration cache stored.

## Residual Limits

Chinese resource rendering and Android/iOS interaction were not runtime-tested. Platform artwork decoders compile but lack platform decode-correctness tests. iOS evidence proves simulator compilation and Xcode availability, not linked-app runtime resource lookup. `./init.sh` was intentionally not rerun after the prior user-directed stop beyond 9000 seconds; the full init matrix is not claimed.

## Commit State And Next Action

The Task 2.2 base commit was `53cc75c`. No Task 2.2 commit exists yet; no SHA is asserted. Planned commit boundary: `refactor: extract core ui`.

Next safe action: Task 2.3 inventory.
