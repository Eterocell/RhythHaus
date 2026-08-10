# Task 6.1 Brief — Library Feature Extraction

Read the approved executable plan at `docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md` first. This brief is bound to the committed planning baseline below and is the sole requirements source for the first implementation checkpoint.

Planning baseline: 1c7ad370949d778dc202af6cfdbc04e83a8475e2

## Corrective Checkpoint 2A: Library Playback and Holder Correction

This brief is actively bound to corrective Checkpoint 2A from cleanup-isolation successor plan commit `fe9b565de72417a2b1bf584370d2eab29bbfc73e`. Implement only the twelve-path correction allowlist and lifecycle artifacts; the final manifest, progress, roadmap, OpenSpec, evidence report, and unrelated feature moves remain out of scope. This brief is active for Checkpoint 2A and is not an acceptance or review approval. No staging or commit is authorized in this lane; validation remains owned by the orchestrator.

Checkpoint 1 Governance RED is approved through `e8b9934` with `SPEC COMPLIANCE PASS`, `TASK QUALITY PASS`, and `PASS/APPROVED`.

Implement only corrective Checkpoint 2A from the approved plan. The current source snapshot retains the twelve-path correction allowlist changes and the required feature dismissal, architecture, Android host, platform compilation, and Android assemble evidence recorded in the controller ledger. The required Shared lifecycle selector is blocked by the pre-existing stale `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/ui/SearchRouteAdapterJvmTest.kt` `LibraryTrack.toPlayableTrack` reference outside the 2A allowlist; its stale prior XML is not current evidence. The exact approved RED protocol was not followed by the historical execution records, so that protocol mismatch is recorded as a concern and no execution is invented. The dirty tracked report is a frozen pre-existing artifact with SHA-256 `2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5`, materially different from the planning-baseline report SHA-256 `399f575a8ba678b76ddcb64a4be7f690735b26ee3f9fd0b38fa8da91bfb96335`; do not edit it. Preserve the SQLDelight schema, migrations, generated database ownership, and database behavior.

Capture and preserve causal RED/GREEN evidence according to the approved plan's literal corrective RED protocol. Verify the module targets, API tests, Android database host tests, compilation, exact holder declaration identity, absence of `PlayableTrack` from the Library API, and `git diff --check`; current evidence and exact command/status/XML paths are recorded in the controller ledger. Keep every changed production/test path inside the approved 113-path manifest. Do not begin scanner/repository/platform/UI/resource/Shared-adapter work from Checkpoints 3-5. Do not modify progress, roadmap, OpenSpec, or evidence files beyond the ignored controller ledger and this brief; do not stage or commit.

Report exact RED/GREEN commands, XML counts, changed paths, commit SHA, and any blocker in `.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md`.

Closeout evidence: complete
