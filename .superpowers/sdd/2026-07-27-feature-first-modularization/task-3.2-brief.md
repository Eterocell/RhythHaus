## Task 3.2: Conditional Core Platform

**Scope:** Slice 3 conditional; do not create an empty module.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Platform.kt`; `shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/Platform.{android,jvm,ios}.kt`; `AudioMetadata.kt` and platform actuals; `library/PlatformSourceAccess.kt` and its actuals.

**Target files:** only if justified, `core/platform/build.gradle.kts`, complete matching source sets, and `core/platform/src/commonTest/kotlin/.../PlatformCapabilityBoundaryTest.kt`; otherwise `docs/adr/` decision added in its tracker task.

- [ ] Run `rg -n '^expect |^actual ' shared/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/{Platform.kt,AudioMetadata.kt,library/PlatformSourceAccess.kt}`. Acceptance inventory must name a candidate and two consuming domains; scanner/source access and backup document access do not count as core candidates.
- [ ] If no candidate meets the two-domain threshold, write the ADR decision and run `test ! -d core/platform`; expected GREEN: no speculative module. Commit `docs: defer core platform extraction`.
- [ ] If a candidate qualifies, first create `PlatformCapabilityBoundaryTest.kt`, run `./gradlew :core:platform:allTests --configuration-cache`; expected RED: absent module/task.
- [ ] Move the complete expect/actual family with the core convention, then run the same command; expected GREEN. Run Android/JVM/iOS compilation and architectureCheck.
- [ ] Commit conditional implementation with `git add core/platform shared settings.gradle.kts build-logic && git commit -m "refactor: extract core platform capability"`.
