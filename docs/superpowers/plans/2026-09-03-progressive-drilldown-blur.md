# Progressive Drill-Down Blur Implementation Plan

> **For implementation owner:** Execute this plan through the subagent-driven development workflow, one task at a time, preserving the test-first red/green boundary below.

**Goal:** Replace only the drill-down top chrome’s uniform liquid-glass blur with Miuix’s top-edge progressive blur while preserving uniform floating surfaces, capability gates, and generated dependency disclosure.

**Architecture:** `:core:ui` gains a RhythHaus-owned style policy that maps internally to Miuix’s uniform or progressive effect. The existing modifier stays the only blur boundary. The Library feature opts its scroll-adjacent top chrome into the new style; every other caller uses the backward-compatible uniform default. The same Miuix gradient is provided both to `progressiveBlur` and `drawBackdrop` to retain its sharp clear endpoint.

**Tech stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix Blur `0.9.4-rc01`, Compose JVM UI tests, Gradle AboutLibraries export.

---

## Task 1: Define and prove the core glass-effect policy

**Files:**
- Create: `core/ui/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChromeTest.kt`
- Modify: `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt:1-62`

**Step 1: Write the failing common policy test.**

Add tests that exercise the production blur-style mapping. The uniform default must select no progressive composite; the top-edge style must select exactly `ProgressiveBlur.Top`. Keep the assertion at the policy boundary rather than asserting GPU pixels.

**Step 2: Run the focused test to confirm red.**

Run:

```bash
./gradlew :core:ui:jvmTest --tests '*LiquidGlassChromeTest*' --configuration-cache
```

Expected: failure because the blur style and its mapping do not exist yet.

**Step 3: Implement the smallest policy.**

Introduce public `RhythHausGlassBlurStyle` with `Uniform` and `TopEdgeProgressive`. Add `blurStyle: RhythHausGlassBlurStyle = Uniform` as the final optional argument of `Modifier.rhythHausLiquidGlass`. Keep Miuix’s `ProgressiveBlur` type internal: map the local style to the Miuix gradient internally.

For `Uniform`, retain the current `blur(blurRadius.toPx())` effect and no progressive gradient. For `TopEdgeProgressive`, call `progressiveBlur(blurRadius.toPx(), gradient = ProgressiveBlur.Top)` and pass the same gradient through `drawBackdrop(progressiveGradient = ...)`. Do not change `rememberRhythHausBackdrop`, `recordRhythHausBackdrop`, `isRenderEffectSupported`, `isRuntimeShaderSupported`, tint drawing, clipping, or fallback composition.

**Step 4: Run the focused test to confirm green.**

Run the same command from Step 2. Expected: pass.

## Task 2: Use progressive blur only in scroll-adjacent drill-down chrome

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt:113-139`
- Modify: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/DrillDownViewJvmTest.kt`
- Verify unchanged: `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt:110-128`

**Step 1: Add a failing production-composable regression.**

Extend the existing drill-down JVM test fixture with a focused test at an internal production style-policy value used directly by `DrillDownMiuixScrollChrome`. It must prove the detail top chrome requests `TopEdgeProgressive`. The core-ui policy test from Task 1 already proves every unspecified caller, including Now Playing, resolves the `Uniform` default. Keep the existing Back-button and scrolling assertions unchanged so the changed top chrome remains a real composable boundary.

**Step 2: Run the focused test to confirm red.**

Run:

```bash
./gradlew :feature:library:impl:jvmTest --tests '*DrillDownViewJvmTest*' --configuration-cache
```

Expected: failure because the drill-down chrome still takes the uniform default.

**Step 3: Implement the one caller opt-in.**

Import `RhythHausGlassBlurStyle` into `LibraryChrome.kt` and pass `blurStyle = RhythHausGlassBlurStyle.TopEdgeProgressive` only to the full-width rectangular `DrillDownMiuixScrollChrome` backdrop modifier. Do not touch Now Playing’s modifier call or introduce Miuix blur imports to feature code.

**Step 4: Run the focused test to confirm green.**

Run the command from Step 2. Expected: pass, including existing interaction and scroll assertions.

## Task 3: Regenerate disclosure and verify full behavior

**Files:**
- Generated: `shared/src/commonMain/composeResources/files/aboutlibraries.json`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Modify: `openspec/changes/adopt-progressive-drilldown-blur/tasks.md`

**Step 1: Regenerate the dependency catalog.**

Run:

```bash
./gradlew :shared:exportLibraryDefinitions --configuration-cache
```

Accept only generated changes. Confirm all Miuix modules in the JSON report `0.9.4-rc01`; do not hand-edit the JSON.

**Step 2: Run behavioral checks.**

Run focused core-ui and Library suites, then:

```bash
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

**Step 3: Perform visual smoke verification.**

Launch the Desktop application on a renderer that reports supported runtime shaders, open a drill-down detail, and scroll its content. Confirm blur is strongest at the top chrome edge, resolves sharply to clear at its lower edge, and the Now Playing bar remains uniformly blurred. If no supported renderer is available, record that exact blocker and retain focused behavior tests; do not claim visual confirmation.

**Step 4: Complete delivery records.**

Review the diff against the OpenSpec scenarios. Mark completed OpenSpec tasks, update `progress.md` and `roadmap.md` with exact command/visual evidence and blockers, run:

```bash
openspec validate adopt-progressive-drilldown-blur --strict
git diff --check
```

Then create the required conventional commit after all verification succeeds.
