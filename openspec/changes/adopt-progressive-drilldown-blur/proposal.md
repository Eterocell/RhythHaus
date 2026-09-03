## Why

RhythHaus already resolves Miuix Blur `0.9.4-rc01`, but its drill-down chrome still uses a uniform blur. Miuix now supports a true progressive blur with a full-resolution clear endpoint; adopting it at the scroll-adjacent top edge improves content legibility without changing the existing floating-surface treatment.

## What Changes

- Add a RhythHaus-owned blur-style policy to the existing shared glass wrapper: uniform blur remains the default; top-edge progressive blur is an explicit style.
- Render `DrillDownMiuixScrollChrome` with the top-edge progressive style, pairing `ProgressiveBlur.Top` with the matching progressive backdrop composite.
- Retain uniform blur for the floating Now Playing bar and all other existing callers.
- Preserve the current runtime shader/render-effect capability gates and the opaque fallback surface; unsupported renderers receive no progressive-effect attempt.
- Add regression coverage for policy selection and the production drill-down/Now Playing call-site split.
- Regenerate the existing Shared AboutLibraries catalog so its Miuix entries report the already-resolved `0.9.4-rc01` versions.

## Capabilities

### New Capabilities

- `progressive-drilldown-backdrop`: Edge-aware glass rendering for scroll-adjacent drill-down chrome.

### Modified Capabilities

- None.

## Impact

- Affected production source: `:core:ui` glass wrapper and the Library drill-down chrome call site.
- Affected generated metadata: `shared/src/commonMain/composeResources/files/aboutlibraries.json`.
- No new dependency, navigation change, platform ABI, Back-policy, or route-state change.
- Android retains its existing Miuix blur manifest/runtime compatibility policy; the repository’s Android minSdk 29 is already above Miuix’s new 24 floor.
