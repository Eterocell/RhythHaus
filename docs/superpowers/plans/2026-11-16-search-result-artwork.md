# Search Result Artwork Implementation Plan

> **For agentic workers:** User selected superpowers:subagent-driven-development on 2026-11-16. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show available embedded artwork in Search rows while preserving the neutral fallback and all existing row behavior.

**Architecture:** Search renders `LibraryTrack.artworkBytes` through the existing `core:ui` `ArtworkImage` primitive. Search keeps the current neutral note for absent or undecodable bytes; no loader callback, public API change, or Shared composition change is needed.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Compose UI tests, Coil 3 through `core:ui`.

**Spec:** `docs/superpowers/specs/2026-11-16-search-result-artwork-design.md`; OpenSpec delta: `openspec/changes/studio-ui-redesign/specs/music-library-presentation/spec.md`.

## Global Constraints

- Use the existing `core:ui` artwork renderer and thumbnail role; do not add dependencies.
- Keep Search independent of `feature:library:impl` and do not add a Search loader callback.
- Keep artwork decorative in accessibility and preserve row sizing, focus, selection, playback, and more actions.
- Missing or undecodable artwork continues to use `SearchNeutralArtwork()`.
- Do not change stored artwork data, query behavior, ordering, duplicate occurrences, or platform code.

## Review Focus

- A valid embedded image renders actual pixels in the Search artwork slot; test with a known-color PNG fixture.
- Missing bytes still show the neutral note; cover with an artwork-absent Search result.
- Invalid image bytes resolve to the same fallback after decode failure; cover with corrupt non-empty bytes.
- Artwork remains decorative and does not add a separate accessibility action; verify the Search row's merged semantics.
- Selection, playback, and more-actions remain unchanged; rerun the existing Search JVM suite.

---

### Task 1: Render Search result artwork

**Files:**
- Modify: `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Test: `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchScreenJvmTest.kt`
- Spec: `openspec/changes/studio-ui-redesign/specs/music-library-presentation/spec.md`
- Tasks: `openspec/changes/studio-ui-redesign/tasks.md`

**Interfaces:**
- Consumes: `LibraryTrack.artworkBytes`; `ArtworkImage(artworkBytes, contentDescription, role, modifier, contentScale, fallback)` from `core:ui`.
- Produces: no public API or composition changes. `SearchResultRow` renders embedded bytes via `ArtworkImageRole.Thumbnail` and retains `SearchNeutralArtwork()` as fallback.

- [x] **Step 1: Add the failing rendered-artwork tests**

Add Compose JVM regressions in `SearchScreenJvmTest`:
- `searchResultRendersEmbeddedArtwork`: use a valid solid-color PNG, issue a matching query, capture the tagged artwork slot, and assert its center pixel matches the fixture color.
- `searchResultArtworkFallsBackWhenBytesMissingOrInvalid`: render one result without bytes and another with corrupt non-empty bytes; assert both settle on the existing neutral-note fallback.
- Assert the artwork does not add a separate accessible row action; keep existing row title and action semantics.

- [x] **Step 2: Run the new tests and verify they fail**

Run:

```bash
./gradlew :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchScreenJvmTest.searchResultRendersEmbeddedArtwork' --tests 'com.eterocell.rhythhaus.search.SearchScreenJvmTest.searchResultArtworkFallsBackWhenBytesMissingOrInvalid' --configuration-cache --console=plain
```

Expected: `searchResultRendersEmbeddedArtwork` fails because Search renders the note instead of the supplied image; the missing/corrupt-byte fallback assertions pass on the current implementation.

- [x] **Step 3: Render artwork in `SearchResultRow`**

Replace the unconditional `SearchNeutralArtwork()` slot with `ArtworkImage` using `track.artworkBytes`, `ArtworkImageRole.Thumbnail`, decorative semantics, and `SearchNeutralArtwork()` as the fallback. Add a stable artwork-slot test tag if needed for pixel capture. Do not change `SearchContent` parameters or other row callbacks.

- [x] **Step 4: Run focused regressions and the full Search suite**

Run:

```bash
./gradlew :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchScreenJvmTest.searchResultRendersEmbeddedArtwork' --tests 'com.eterocell.rhythhaus.search.SearchScreenJvmTest.searchResultArtworkFallsBackWhenBytesMissingOrInvalid' --configuration-cache --console=plain
./gradlew :feature:search:jvmTest --configuration-cache --console=plain
./gradlew :desktopApp:compileKotlin --configuration-cache --console=plain
```

Expected: all targeted tests, the complete Search JVM suite, and desktop compilation pass.

- [x] **Step 5: Review scope and OpenSpec state**

Confirm only Search rendering/tests and the approved Studio spec/task records changed; keep unrelated manual acceptance checkboxes untouched. Validate with `openspec validate studio-ui-redesign --strict`.

Execution evidence (2026-11-17): independent review found no blocking production/spec issues. The initial RED failed because test tags were absent; a subsequent note-only negative control retained both tags and failed only the artwork pixel regression while fallback passed. After restoring the reviewed renderer and asserting both fallbacks displayed, all 24 Search JVM tests, desktop compilation and strict OpenSpec validation passed. Progress/roadmap handoffs record the isolated, uncommitted result. Native device/visual acceptance remains unclaimed.
