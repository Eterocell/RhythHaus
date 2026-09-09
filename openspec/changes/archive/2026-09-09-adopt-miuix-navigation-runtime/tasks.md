## Current Gate Result

- [x] Upstream source review establishes that `NavDisplay` unconditionally installs `PredictiveBackHandlerWithSessions(enabled = backStack.size > 1)` and has no public platform-Back opt-out. This proposal is blocked on `0.9.4-rc01`; do not execute the conditional tasks below.

## Conditional Tasks After Upstream Adds a Platform-Back Opt-Out

- [ ] 1.1 Add the `miuix-nav` catalog alias and a `:shared` implementation dependency without changing route rendering; verify the dependency resolves and `./gradlew :shared:compileKotlinJvm --configuration-cache` succeeds.
- [ ] 1.2 Characterize `NavDisplay` under the production Shared Navigation Event dispatcher and prove that Shared receives first refusal for modal, edit, selection, Now Playing, and route targets; verify with new production-boundary common/JVM and Android-host tests. Stop the change if Miuix registers a competing platform Back consumer or mutates before Shared settles.

## 2. Authoritative Route Projection

- [ ] 2.1 Implement the internal, in-memory Miuix key projection from `LibraryNavigationEntry`, preserving route values and immutable instance tokens; verify focused tests for equal-route replacement, predecessor restoration, root-pop no-op, and stale playlist-detail deletion.
- [ ] 2.2 Implement the one-way Shared renderer/mirror seam so `LibraryAppState` is the only route mutation authority and Miuix entry content receives the original navigation entry; verify tests reject direct Miuix mutation and cover push, pop, replace, multi-pop, cancellation, rejection, and in-flight settlement.

## 3. Responsive Rendering and Back Preservation

- [ ] 3.1 Replace compact `AnimatedContent` route rendering with the Shared Miuix renderer while preserving tags, semantics, overlays, local dialogs, and existing route callbacks; verify the real Library composable tests cover each compact route and Back precedence path.
- [ ] 3.2 Host the detail renderer in the existing ListDetail pane with strict clipping, correct insets/corner policy, and blocked intermediate input while retaining the interactive master pane; verify wide master-detail and alternate bottom-bar production-composable regressions.
- [ ] 3.3 Retain the existing Navigation Event predictive Back adapter as the sole system Back admission path and explicitly disable Miuix in-content swipe; verify cancelled, completed, rejected, stale, and repeated Back sessions across modal, edit, selection, Now Playing, and route targets.

## 4. Metadata, Verification, and Acceptance

- [ ] 4.1 Regenerate the Shared AboutLibraries export so every Miuix artifact records `0.9.4-rc01`; verify `./gradlew :shared:exportLibraryDefinitions --configuration-cache` leaves only expected generated metadata changes.
- [ ] 4.2 Run targeted Shared/common/JVM/Android-host regression suites, then `./gradlew spotlessApply --configuration-cache`, `./gradlew spotlessCheck --configuration-cache`, `./gradlew detekt --configuration-cache`, `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version`, and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
- [ ] 4.3 Review the final diff against the OpenSpec requirements, update `progress.md` and `roadmap.md` with verification evidence, validate this change with `openspec validate adopt-miuix-navigation-runtime --strict`, and commit the approved implementation with a conventional message.
