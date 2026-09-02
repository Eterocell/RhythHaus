## 1. Architecture quality repair

- [x] 1.1 Add current-ID and authored-current-ID TestKit regressions for Versions aggregate identity filtering; verify the current-ID clean fixture is RED before implementation and that legacy coverage remains intact.
- [x] 1.2 Register the existing exact-identity aggregate capture for both supported Versions plugin IDs without changing KSP or Android synthetic-self suppression; verify all Versions aggregation functional cases and the full architecture functional test class.

## 2. Compose UI compatibility

- [x] 2.1 Upgrade the catalog Miuix version to `0.9.4-rc01` without changing Compose or adding a Skiko workaround; verify `:core:ui:jvmTest`, `:feature:search:jvmTest`, and dependency insight.
- [x] 2.2 Run the complete existing JVM test battery and verify the former Squircle `Image.makeShader` linkage failure is absent.

## 3. CI build gates

- [x] 3.1 Make `architectureCheck`, `spotlessCheck`, and `detekt` independent named GitHub Actions quality steps; verify YAML retains the existing JVM test job.
- [x] 3.2 Add separate Android debug, desktop Kotlin, and unsigned Xcode Simulator iOS app build jobs; verify no Android Host Test, iOS Simulator Test, or `xcodebuild test` command is selected.

## 4. Acceptance and handoff

- [x] 4.1 Run quality, formatting, Detekt, JVM, Android, desktop, Xcode, OpenSpec, and repository baseline verification; record exact evidence.
- [x] 4.2 Update `progress.md`, `roadmap.md`, and OpenSpec task status with verified results; review the final diff, preserve user changes, and create the required conventional commit.
