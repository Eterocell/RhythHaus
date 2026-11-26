# Compose performance audit — Android

**Status: baseline measurement blocked.** Release artifacts and source were inspected; no runtime performance diagnosis, change, or improvement was verified.

## Environment

- Audited `main` at `8beab461`. Compose Multiplatform 1.12.2, Kotlin and Compose Compiler plugin 2.4.21, AGP 9.5.1. The existing release mapping reports R8 9.5.29.
- `adb devices -l` returned no attached device. No physical-device run was attempted. The local `add-android-baseline-profiles` branch was inspected for prior evidence, not merged or used as a substitute for `main`.

## Baseline (Phase 1)

| Check | Evidence |
| --- | --- |
| Optimized release | [`androidApp/build.gradle.kts:93–100`](../androidApp/build.gradle.kts#L93) enables minification and resource shrinking with `proguard-android-optimize.txt`. `:androidApp:assembleRelease --offline --configuration-cache` previously passed at the NDK pin. This audit inspected the existing APK and mapping; it did not rebuild. APK SHA-256: `5fc03ce040ae694a714c6e768731c6afcc88c6bc40fc3c8abe1189dc0f03fd2f`. |
| Packaged ART profile | The existing APK contains `assets/dexopt/baseline.prof` and `baseline.profm`. `profgen dumpProfile --strict` against the APK and release mapping decoded 8,368 method rules. That confirms the artifact is readable, not that it contains app-owned journey coverage or that ART installed it. `main` has no tracked `:baselineprofile` module or generated app-owned rules. |
| Related branch | `267d2995` generated and packaged app-owned startup and Library Songs scroll rules on a disposable Android 14 emulator. The Android 17 Pixel 7 Pro run and isolated retries failed before sampling at `UiAutomationService … already registered!`. Timing acceptance for that change was waived in `d637b1a4`; no baseline was recorded. |
| Cold startup TTID / TTFD median | Not measured. No valid profile-required samples. |
| Scroll CPU frame duration / frame overrun P50/P90/P99 | Not measured. No valid frame samples. |

## Diagnosis (Phase 2) — static candidates only

No release Compose Compiler reports, recomposition counts, or Perfetto timing traces were available. Counts of restartable-but-not-skippable composables, unstable classes, and costly phase reads are unknown, not zero. This table is the order to investigate, not a ranking of measured hotspots.

| Priority | Source evidence | Measurement to confirm or rule out |
| --- | --- | --- |
| 1. Playback updates at shell scope | Android schedules progress at [250 ms](../core/playback/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt#L210) while the active voice is playing. [`LibraryAppShell.kt:359`](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt#L359) reads the whole playback state; its [track lookup](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt#L424) scans `snapshot.tracks` when the scope runs, even if the track ID has not changed. Descendant recomposition is not established. | Fling the same Songs list while paused and while playing. Check whether progress ticks coincide with shell execution, repeated lookup work, or missed frames before narrowing state subscriptions. |
| 2. Scroll offsets read in composition | [Library Home](../feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt#L394), [Library detail](../feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt#L180), [Search](../feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt#L190), and [playlist detail](../feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt#L1038) key effects on index and pixel offset. When Search recomposes, it also [maps the entire result set to IDs](../feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt#L176), although filtering is remembered. | Trace a large Search-result fling first. Separate scroll publication, result-ID allocation, and row/layout work. If the effect is costly, test one long-lived observer while preserving scroll restoration and bottom-bar visibility. |
| 3. Artwork request work on row composition | [`ArtworkImage.kt:25–57`](../core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt#L25) hashes all cover bytes and builds a Coil request on each execution with artwork. The image cache may avoid decode, but not this caller work. The no-artwork path skips it. | Compare the same Songs/Search viewport with embedded covers and without them, including a warm-cache pass. Separate request/key cost from decode, subcomposition, and draw cost before changing the image pipeline. |
| 4. Startup projection scales with catalog size | [`AppLibraryContentHost.kt:391`](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryContentHost.kt#L391) remembers the UI snapshot by published tracks, but computes it in composition when that input changes. [`MusicModels.kt:12–52`](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt#L12) maps the catalog and constructs a fixed accent list for each track. | Compare cold start into an already-imported small and large catalog. Attribute the TTFD difference to restore, projection, or initial rendering rather than assuming the map dominates. |
| 5. Now Playing reads animated geometry in composition | [`LibraryAppShell.kt:1806–1852`](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt#L1806) reads the animated fraction to set sheet height and corner shape; the shell also [reads the closing threshold](../shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt#L643) for interaction gating. | Trace expand and collapse. Distinguish transition composition from required measurement and drawing before changing the sheet; preserve hit targets, focus, and reduced-motion behavior. |

Keep the existing identities: [Songs rows](../feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt#L356) use track IDs; queue rows use occurrence IDs for duplicate tracks. The [queue projection](../feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaybackQueueContent.kt#L299) is remembered by queue and current occurrence, so position-only changes do not invalidate that projection.

## Fixes applied (Phase 3)

None. No source, build configuration, profile, or stability baseline changed before measurement.

## Verification (Phase 4)

Not reached. No before/after timings, profile regeneration, or CI stability gate changes. Audit checks were limited to source, existing APK and mapping inspection, and strict profile decoding.

## Next measurement

1. Make the verified profiling harness available on the revision being measured. Use a disposable low-end physical Android test device with the fixed 256-track catalog. Keep the Pixel/Android 17 harness failure separate from app performance; use a supported target or resolve that failure before sampling.
2. On the same R8 APK, run the profile-required cold startup benchmark for at least 10 valid TTID/TTFD samples and the Songs fling for at least five valid iterations. Retain JSON and Perfetto traces with device, build, APK, and profile identities. Add a paused/playing comparison for the first static candidate.
3. Generate release compiler reports and use traces to rank actual work. Change one verified cause at a time and rerun the same scenarios. Do not use emulator profile collection or a compiler stability count as a performance result.
