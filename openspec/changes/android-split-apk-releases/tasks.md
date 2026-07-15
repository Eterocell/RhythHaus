## 1. Shared Android ABI contract

- [ ] 1.1 Add RED unit coverage for approved, trimmed, missing, blank, duplicate, reordered, unsupported, and reduced `rhythhaus.android.abis` values, plus exact lowercase split activation.
- [ ] 1.2 Implement the `build-logic.android.abi-contract` binary plugin, typed contract extension, strict parser, root ABI property, and TestKit plugin-consumer/configuration-cache coverage.
- [ ] 1.3 Make TagLib consume the typed contract and verify invalid repository ABI configuration fails while the three existing Android native helper tasks remain available.
- [ ] 1.4 Review Task 1 tests, exact parser errors, binary-plugin classpath use, and commit scope.

## 2. Opt-in AGP split configuration

- [ ] 2.1 Add RED coverage proving every split property value except exact `true` remains disabled.
- [ ] 2.2 Apply the shared contract in `androidApp` and configure only AGP built-in `splits.abi` with the approved ABIs and universal APK when enabled.
- [ ] 2.3 Build exact split, absent-property, and non-exact-property release outputs under configuration-cache enforcement; confirm AGP metadata reports four split outputs or one unfiltered output as applicable without encoding filenames.
- [ ] 2.4 Review Task 2 for no custom packaging, output names, flavors, density/language splits, or ABI-derived version codes.

## 3. Release APK verification

- [ ] 3.1 Add RED pure tests for metadata-derived output matrix/filter failures, exact TagLib ZIP slices, canonical APK identity, sanitized SDK metadata parsing, and the configured-signing versus unsigned-local matrix.
- [ ] 3.2 Implement pure validators and `VerifyReleaseApksTask` using `BuiltArtifactsLoader`, ZIP native checks, configured SDK tool resolution, canonical numeric version codes, and deterministic credential-free reports.
- [ ] 3.3 Register `verifyReleaseApks` from release `SingleArtifact.APK`, then verify ordinary and exact split release outputs with configuration-cache problem failures enabled.
- [ ] 3.4 Review Task 3 for AGP metadata classification rather than filenames, no secret output, no AAB signing command, and cache-safe task actions.

## 4. Independent AAB verification

- [ ] 4.1 Write a RED/GREEN real-AGP-AAB feasibility probe that copies only `base/manifest/AndroidManifest.xml` and `base/resources.pb` into a temporary root proto archive, converts it with SDK `aapt2`, and reads canonical identity with SDK `apkanalyzer`.
- [ ] 4.2 If the SDK-only feasibility probe cannot pass, stop implementation, record the sanitized failure, and obtain a user-approved design revision. Do not substitute filenames, source configuration, APK metadata, or AGP task inputs as proof.
- [ ] 4.3 After the probe passes, add RED contract tests and implement `VerifyReleaseAabTask` using the proven temporary conversion path, one non-empty AAB, canonical identity, and deterministic report.
- [ ] 4.4 Register `verifyReleaseAab` from release `SingleArtifact.BUNDLE`; prove it passes with and without exact split mode and has no APK/signing dependency.
- [ ] 4.5 Review Task 4 for proven conversion syntax, independent bundle wiring, tool-error sanitization, and no `apksigner` use.

## 5. Default-mode and configuration-cache acceptance

- [ ] 5.1 Run build-logic tests/plugin validation and verify actionable invalid ABI and noninteger version-code failures.
- [ ] 5.2 Run split APK verification twice and independent AAB verification twice, proving configuration-cache storage and reuse.
- [ ] 5.3 Verify non-exact split mode remains ordinary, debug assembly remains ordinary, and the supported JVM/desktop/Android regression matrix passes.
- [ ] 5.4 Attempt `./init.sh`; record exact results and any unchanged unrelated iOS `Thread` compilation blocker without fixing it in this change.
- [ ] 5.5 Add and verify a focused regression fix only if a witnessed acceptance failure requires it, then review its narrow scope.

## 6. Full regression, review, and durable evidence

- [ ] 6.1 Run diff hygiene and strict OpenSpec validation; review the complete change for all approved constraints and scope boundaries.
- [ ] 6.2 Complete task-level specification/build-logic reviews and the final `review-work` gate. Resolve any Critical or Important finding through a new RED test and repeat affected evidence.
- [ ] 6.3 Mark only evidenced task boxes complete, update only roadmap item 22 while preserving item 21, and prepend exact verification/signing/cache/blocker evidence to `progress.md`.
- [ ] 6.4 Create the final atomic durable-evidence commit with the required Sisyphus footer and co-author trailer; do not push or archive without explicit request.
