# RhythHaus AI Workflow Kit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize RhythHaus implementation briefs and acceptance reviews with two repo-owned templates, discoverable from the existing workflow entry points.

**Architecture:** Keep this documentation-only. OpenSpec remains the durable source for requirements and tasks; `progress.md` remains the session handoff/evidence record. Templates reference those records rather than duplicating them. `AGENTS.md` and `docs/harness-engineering.md` get short pointers only.

**Tech Stack:** Markdown documentation; no code, dependencies, scripts, hooks, or generated files.

**Spec:** [Approved workflow-kit design](../specs/2026-11-10-rhythhaus-ai-workflow-kit-design.md).

## Global constraints

- Do not modify `openspec/**`, `.agents/**`, `.claude/**`, `.omp/**`, or `.opencode/**`.
- Preserve the existing OpenSpec + Superpowers route, bug-debugging route, worktree rules, verification requirements, and completion-evidence template.
- Do not create a parallel task tracker or duplicate OpenSpec/SDD task state.
- Keep template pointers short and place detailed reusable instructions in the templates.
- Do not commit unless the user explicitly asks; this workflow-kit change is outside the completed OpenSpec workflow commit.

## Review focus

1. Does each template make its intended use and source-of-truth references clear? Verify all required fields and the boundary against OpenSpec/SDD state.
2. Can an implementer identify target surfaces, non-goals, acceptance criteria, verification, and adjacent owners without relying on implicit context? Verify the assignment template contains these fields.
3. Can a reviewer map every criterion to evidence and report blockers accurately? Verify the checklist has a criterion/evidence/result mapping and disposition for gaps.
4. Does the checklist distinguish automated checks from user-only GUI/device/audio acceptance? Verify unobserved manual acceptance cannot be represented as passed.
5. Do the two workflow entry-point pointers stay concise, resolve to the templates, and avoid conflicting with existing instructions? Inspect both final files and validate referenced paths.

## File map

| File | Responsibility |
| --- | --- |
| `docs/superpowers/templates/implementation-brief.md` | Reusable assignment fields: goal, source artifact/task, targets, boundaries, acceptance, verification, adjacent ownership, handoff. |
| `docs/superpowers/templates/acceptance-review.md` | Reusable review fields: criterion-to-evidence mapping, scope/diff review, automated results, manual acceptance status, blockers, next owner. |
| `AGENTS.md` | Add a short pointer to the brief when assigning implementation and the review checklist at acceptance. |
| `docs/harness-engineering.md` | Add matching concise pointers at execution and lifecycle/acceptance sections; preserve its existing evidence template. |

## Task 1: Create the reusable templates

**Files:**
- Create: `docs/superpowers/templates/implementation-brief.md`
- Create: `docs/superpowers/templates/acceptance-review.md`

- [x] **Step 1: Write the implementation brief template.** Include fields for goal/outcome, OpenSpec change and task identifier (or explicit non-OpenSpec route), exact target surfaces, non-goals/adjacent owners, observable acceptance criteria, narrow verification command/scenario, required evidence, and handoff. Instruct users to link to existing source artifacts rather than copy their contents. Leave only clearly labeled per-use fill-in fields.
- [x] **Step 2: Write the acceptance review checklist.** Provide a per-criterion row for evidence and status, plus scope/diff review, exact automated commands/results, separate user-only GUI/device/audio acceptance status, blockers, and next owner. State that unobserved user-only acceptance is `postponed/unverified`, never passed based on static review or agent-run tests. Point to existing records rather than creating another status source.
- [x] **Step 3: Review both templates against the design.** Check all acceptance criteria, remove repeated process guidance, and ensure no TODOs or example values could be mistaken for real task data.

## Task 2: Wire templates into repository workflow entry points

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/harness-engineering.md`

- [x] **Step 1: Add the assignment pointer in `AGENTS.md`.** Place a short directive in development workflow enforcement or startup/execution guidance: implementation assignments should use the brief, especially for delegated or multi-surface work; existing OpenSpec task records remain authoritative.
- [x] **Step 2: Add execution and acceptance pointers in `docs/harness-engineering.md`.** Point assignment authors to the brief and acceptance owners to the checklist. Preserve the existing completion-evidence template and avoid duplicating template fields.
- [x] **Step 3: Check consistency.** Read the touched sections together; ensure their routing does not change existing policies and the brief/review pointers are not presented as new mandatory task-tracking artifacts.

## Task 3: Verify and hand off

**Files:** all four files above.

- [x] **Step 1: Validate references and scope.** Confirm both linked template paths exist, Markdown links resolve, and the implementation files are the two templates plus `AGENTS.md` and `docs/harness-engineering.md`. The approved design and plan are planning artifacts, not implementation surfaces. Confirm OpenSpec and agent-integration docs are unchanged.
- [x] **Step 2: Review the final diff against the five review-focus checks.** Record that this is documentation-only; do not run unrelated build/test suites.
- [x] **Step 3: Record a session handoff in `progress.md` only if implementation spans sessions.** Do not update `roadmap.md`; this documentation-only workflow kit does not change product priorities, architecture, or refactor status. Any required handoff note is separate from the four implementation files and records only actual evidence and remaining work.
