# RhythHaus AI Workflow Kit

> **Status:** Design approved by the user on 2026-11-10. Implementation is complete in `.worktrees/ai-workflow-kit` and awaits local integration.
>
> **Route:** Superpowers design and planning, explicitly excluding OpenSpec artifacts at the user's direction. The OpenSpec agent workflow update is already committed separately and is out of scope here.

## Intent

Make implementation assignments and acceptance reviews consistent across RhythHaus tasks without adding another task tracker or duplicating the project's OpenSpec, SDD, and handoff records.

## Goals

- Provide a reusable brief for implementation assignments, with explicit targets, boundaries, acceptance criteria, verification, and adjacent ownership.
- Provide a review checklist that traces acceptance criteria to evidence and distinguishes automated results from user-only GUI, device, and audio acceptance.
- Make both templates discoverable from the existing repository workflow entry points.
- Keep OpenSpec as the durable source of product requirements/tasks and `progress.md` as session evidence/handoff state.

## Non-goals

- No product-code, build, CI, hook, or agent-skill changes.
- No parallel task tracker or new source of truth for OpenSpec/SDD task status.
- No changes to OpenSpec documentation or files, including `.agents/**`, `.claude/**`, `.omp/**`, `.opencode/**`, or `openspec/**`.
- No replacement or copy of the existing completion-evidence template.

## Approach

Add two concise templates under `docs/superpowers/templates/`: an implementation assignment brief and an acceptance review checklist. Add minimal pointers from `AGENTS.md` and `docs/harness-engineering.md`. Each template should instruct the author to reference existing change/task identifiers and evidence records rather than copying them. The review checklist must explicitly label unobserved user-only acceptance as postponed/unverified, not passed.

## Alternatives considered

- Documentation consolidation alone: smaller, but leaves assignment structure inconsistent.
- Gate-enforcing scripts: more mechanical enforcement, but higher maintenance and risk of treating unavailable manual acceptance as an automated pass.
- **Selected:** templates plus pointers. This provides reusable structure while preserving the current workflow owners and sources of truth.

## Constraints

- Preserve repository startup, route-selection, OpenSpec/Superpowers, systematic-debugging, worktree, verification, safety, and commit rules.
- Keep pointers short; detailed reusable content belongs in the templates.
- Avoid duplicating the completion-evidence template already in `docs/harness-engineering.md`.
- Do not modify the OpenSpec update committed as `485127d6`.

## Acceptance

1. Both templates are complete, task-oriented, concise, and have no unresolved placeholders other than fields intentionally filled by each use.
2. `AGENTS.md` and `docs/harness-engineering.md` point to the templates at the appropriate workflow stages without duplicating their contents.
3. The review checklist requires a criterion-to-evidence mapping and separates automated evidence from user-only acceptance; unobserved acceptance remains unverified.
4. No OpenSpec or OpenSpec integration docs are modified.
5. Markdown links/paths resolve and implementation changes are limited to the two templates and two workflow-document pointers; the approved design and plan remain separate planning artifacts.
