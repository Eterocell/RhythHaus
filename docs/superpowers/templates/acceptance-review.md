# Acceptance review: [task name]

> Review the approved brief/design and the complete diff. Link to authoritative OpenSpec/SDD and evidence records rather than creating a second task-status source.

## Scope and diff

- Brief / source task: [path and identifier]
- Changed files reviewed: [paths; confirm they match the assigned scope]
- Scope result: [within scope | issue found — describe]
- Required neighboring callers/tests/docs: [updated or intentionally unchanged, with reason]

## Acceptance criteria and evidence

| Criterion | Evidence (exact command/result or observation artifact) | Status |
| --- | --- | --- |
| [criterion from the approved brief] | [path/output and what it demonstrates] | [pass / fail / blocked / unverified / not applicable] |

Every criterion in the approved brief must have a row. A criterion without evidence is not a pass.

## Automated verification

| Command | Result | Evidence / failure details |
| --- | --- | --- |
| [exact command] | [pass / fail / skipped with reason] | [output or artifact path] |

## User-only GUI, device, and audio acceptance

- Required checks: [checks from the brief, or `none`]
- Observed by: [user/authorized operator, or `not observed`]
- Evidence: [screenshot, UI hierarchy, device/audio observation, or artifact path]
- Status: [accepted with evidence | postponed/unverified | blocked — exact reason | not applicable]

Do not infer GUI, device, accessibility, or audible behavior from static review or agent-run builds/tests. If the required interaction was not directly observed, record `postponed/unverified`; never mark it passed.

## Findings and handoff

- Findings: [severity, path, evidence, and required action; or `none`]
- Blockers / unverified criteria: [exact items, or `none`]
- Next owner/action: [specific next lane/action, or none]
