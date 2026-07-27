# SDD ledger — plan: docs/superpowers/plans/2026-07-27-feature-first-modularization.md

Workspace: /Users/eterocell/Git/self/RhythHaus/.worktrees/feature-first-modularization
Baseline: `./gradlew :shared:jvmTest --configuration-cache` passed on 2026-07-27.
Preflight ruling: Task 0.1 includes the complete approved design, plan, and feature-first OpenSpec artifact set in its scoped docs commit.
Task 0.1: fix round 1/5 (2 addressed, 0 open; executable duplicate-move guard and authoritative commit-state evidence; commits 8b8c8cf..51c479b)
Task 0.1: complete (commits fabf1fa..51c479b, review clean)
Task 1.1: complete (canonical governance commit 55a4f72; approved Spotless-only remediation commit 61ec5b7; separate spotlessCheck, detekt, and :shared:jvmTest passed; no waiver remains)
Task 1.1a: complete (AGENTS link-only section added; prerequisites, separate spotlessApply/spotlessCheck/detekt, strict OpenSpec validation, and diff checks passed)
Task 1.2: complete (three KMP convention plugins and TestKit fixtures; strict RED for missing IDs followed by focused/full GREEN, spotlessApply/spotlessCheck/detekt, strict OpenSpec, and diff checks passed; OpenSpec 2.1 remains unchecked for Task 1.3)
Task 1.3: complete (standalone root architectureCheck, code-owned allow-list, and all required TestKit fixtures; strict RED/GREEN, full convention tests, root architectureCheck, spotlessApply/spotlessCheck/detekt, strict OpenSpec, and diff checks passed; OpenSpec 2.1 and 2.3 complete, 2.4 remains unchecked for Task 1.4)
Task 1.3: rejected/in progress (support commits bada6d6 and 747d5cc retained; policy amendment rejects synthetic package/resource assumptions and requires package-preserving declaration-index ownership before a third repair)
Task 1.3: rejected/in progress (independent review rejected implementation commit b2e2700 on six architecture-enforcement findings; no architecture-gate acceptance is claimed. See task-1.3-independent-rejection-report.md. OpenSpec 2.1/2.3 are unchecked; 2.4 remains unchecked.)
Task 1.3 design: processor-boundary correction documented in task-1.3-architecture-processor-boundary-report.md; planned KSP artifact is root JVM :architecture-processor, while architectureCheck remains public-model-only. Implementation awaits separate explicit user review/approval. OpenSpec 2.1/2.3 remain unchecked and 2.4 remains unchecked.
Task 1.3 plan-scope correction: after `dd772ed`, corrected the plan's build-logic-only scope, inventories, and staging instruction to include only the approved `:architecture-processor` boundary and related permitted paths. See task-1.3-plan-scope-correction-report.md. Task 1.3 remains rejected/in progress; explicit user review is still required and OpenSpec 2.1/2.3/2.4 remain unchecked.
Task 1.3 RED-contract correction: retain the rejected checker as baseline and add focused TestKit/KSP regressions first; RED targets the absent `:architecture-processor`/core-API KSP wiring, while model RED is limited to stable-contract violations. See task-1.3-red-contract-correction-report.md. Task 1.3 remains rejected/in progress; OpenSpec 2.1/2.3/2.4 remain unchecked.
