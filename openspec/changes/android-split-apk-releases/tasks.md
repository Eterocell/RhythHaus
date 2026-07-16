## 1. Shared Android ABI contract

- [x] 1.1 Add RED unit coverage for approved, trimmed, missing, blank, duplicate, reordered, unsupported, and reduced `rhythhaus.android.abis` values, plus exact lowercase split activation.
- [x] 1.2 Implement the `build-logic.android.abi-contract` binary plugin, typed contract extension, strict parser, root ABI property, and TestKit plugin-consumer/configuration-cache coverage.
- [x] 1.3 Make TagLib consume the typed contract and verify invalid repository ABI configuration fails while the three existing Android native helper tasks remain available.
- [x] 1.4 Review Task 1 tests, exact parser errors, binary-plugin classpath use, and commit scope.

## 2. Opt-in AGP split configuration

- [x] 2.1 Add RED coverage proving every split property value except exact `true` remains disabled.
- [x] 2.2 Apply the shared contract in `androidApp` and configure only AGP built-in `splits.abi` with the approved ABIs and universal APK when enabled.
- [x] 2.3 Build exact split, absent-property, and non-exact-property release outputs under configuration-cache enforcement; confirm AGP metadata reports four split outputs or one unfiltered output as applicable without encoding filenames.
- [x] 2.4 Review Task 2 for no custom packaging, output names, flavors, density/language splits, or ABI-derived version codes.

## 3. Release APK verification

- [x] 3.1 Add RED pure tests for metadata-derived output matrix/filter failures, exact TagLib ZIP slices, canonical APK identity, sanitized SDK metadata parsing, and the configured-signing versus unsigned-local matrix.
- [x] 3.2 Implement pure validators and `VerifyReleaseApksTask` using `BuiltArtifactsLoader`, ZIP native checks, configured SDK tool resolution, canonical numeric version codes, and deterministic credential-free reports.
- [x] 3.3 Register `verifyReleaseApks` from release `SingleArtifact.APK`, then verify ordinary and exact split release outputs with configuration-cache problem failures enabled.
- [x] 3.4 Review Task 3 for AGP metadata classification rather than filenames, no secret output, no AAB signing command, and cache-safe task actions.

## 4. Independent AAB verification

- [x] 4.1 Write a RED/GREEN real-AGP-AAB feasibility probe that copies `base/manifest/AndroidManifest.xml`, `base/resources.pb`, and all packaged `base/res/**` payloads into a temporary proto archive, converts it with SDK `aapt2`, and reads canonical identity with SDK `apkanalyzer`.
- [x] 4.2 Record the failed exact-two-entry AGP 9.3 probe, preserve its sanitized root cause, and apply the user-approved design revision allowing the packaged `base/res/**` payloads required by the resource table without substituting filenames, source configuration, APK metadata, or AGP task inputs as proof.
- [x] 4.3 After the probe passes, add RED contract tests and implement `VerifyReleaseAabTask` using the proven temporary conversion path, one non-empty AAB, canonical identity, and deterministic report.
- [x] 4.4 Register `verifyReleaseAab` from release `SingleArtifact.BUNDLE`; prove it passes with and without exact split mode and has no APK/signing dependency.
- [x] 4.5 Review Task 4 for proven conversion syntax, independent bundle wiring, tool-error sanitization, and no `apksigner` use.

## 5. Default-mode and configuration-cache acceptance

- [x] 5.1 Run build-logic tests/plugin validation and verify actionable invalid ABI and noninteger version-code failures.
- [x] 5.2 Run split APK verification twice and independent AAB verification twice, proving configuration-cache storage and reuse.
- [x] 5.3 Verify non-exact split mode remains ordinary, debug assembly remains ordinary, and the supported JVM/desktop/Android regression matrix passes.
- [x] 5.4 Attempt `./init.sh`; record exact results and any unchanged unrelated iOS `Thread` compilation blocker without fixing it in this change.
- [x] 5.5 Add and verify a focused regression fix only if a witnessed acceptance failure requires it, then review its narrow scope.

Task 5 evidence, including the acceptance-failure diagnosis, witnessed RED/GREEN commands,
explicit real-AAB probe, atomic fix commit, cache storage/reuse, artifact reports, and known
iOS blocker, is recorded in `.superpowers/sdd/android-split-apk-releases-task-5-report.md`.

## 6. Full regression, review, and durable evidence

- [x] 6.1 Run diff hygiene and strict OpenSpec validation; review the complete change for all approved constraints and scope boundaries.
- [x] 6.2 Complete task-level specification/build-logic reviews and the final `review-work` gate. Resolve any Critical or Important finding through a new RED test and repeat affected evidence.
- [x] 6.3 Mark only evidenced task boxes complete, update only roadmap item 22 while preserving item 21, and prepend exact verification/signing/cache/blocker evidence to `progress.md`.
- [x] 6.4 Create the final atomic durable-evidence commit with the required Sisyphus footer and co-author trailer; do not push or archive without explicit request.
