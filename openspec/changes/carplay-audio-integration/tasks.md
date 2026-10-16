## 1. Shared CarPlay facade and projection

- [x] 1.1 Add failing common tests for authoritative CarPlay catalog projection, revisions, localized labels, and queue order.
- [x] 1.2 Implement narrow immutable CarPlay ABI and Shared-owned bridge over current library and playback controller; reject stale IDs and unsupported actions.
- [x] 1.3 Add failing regression tests for queue selection, duplicate occurrences, remote transport, and playback-state publication.
- [x] 1.4 Wire lifecycle-safe snapshots and commands into Shared App composition; run focused Shared JVM tests and iOS framework compilation.

## 2. Native iOS system media publisher

- [x] 2.1 Add Swift policy tests for authoritative Now Playing dictionary construction, stale metadata removal, artwork merge, and supported remote commands.
- [x] 2.2 Implement one main-thread serialized iOS publisher for complete Now Playing metadata and remote commands, routing commands through the Shared facade.
- [x] 2.3 Adapt artwork updates to the single publisher without clobbering playback metadata; test command handler/observer idempotency and cleanup.
- [x] 2.4 Run iOS host XCTest and compile Shared iOS framework.

## 3. CarPlay scene and approved templates

- [x] 3.1 Add Swift policy tests for catalog hierarchy, stale list item rejection, empty/loading states, and disconnect/reconnect lifecycle.
- [x] 3.2 Register the CarPlay template application scene and implement localized Songs, Albums, Artists, and Playlists list/detail templates plus Now Playing.
- [x] 3.3 Connect selection and playback changes to the Shared facade; refresh changed lists, preserve playback on disconnect, and complete each CarPlay action exactly once.
- [x] 3.4 Run iOS Simulator XCTest and app build, including scene registration checks.

## 4. Entitlement, acceptance, and closeout

- [x] 4.1 Add CarPlay audio entitlement declaration and target signing configuration without fabricating Apple's managed capability; document approval/profile prerequisites.
- [ ] 4.2 Run CarPlay Simulator smoke for connect, browse, select, Now Playing, external controls, disconnect/reconnect, and loading/empty states; record simulator limitations.
- [x] 4.3 Run required focused quality/architecture/OpenSpec checks, independent review, synchronize specs, and update roadmap/progress with external entitlement and physical-device acceptance status.

### Acceptance evidence — 2026-10-15

- Shared CarPlay facade: 20 JVM and 20 iOS Simulator cases passed. Headless process initialization/history and initial playlist-read failure: two cases passed on each platform. Core crossfade/gapless controller suite: 34 cases passed, together with focused external transport, manual skip, and shuffle cases.
- Native system-media/template XCTest: 18 cases, zero failures. Includes real application catalog initialization, complete metadata/artwork, command ownership, pagination, and scene callback lifetime policies. Shared iOS Simulator framework linked; unsigned Xcode app/test build passed. Android Kotlin and desktop Kotlin compilation and the full playlist-implementation JVM suite passed.
- Separate `spotlessApply`, `spotlessCheck`, `detekt`, and `architectureCheck` passed. Canonical specs synchronized; strict change and all-spec validation recorded in `progress.md`.
- Final independent native, transport-atomicity, and process/readiness reviews passed without remaining Important/Critical findings. Earlier findings were reproduced and repaired before acceptance.
- **4.2 remains open:** the final app was installed/launched on the iPhone 17 simulator and the settled Chinese empty-library phone surface was captured. This is not CarPlay-display proof. CarPlay-display screenshot attempts timed out; `open -a Simulator` reported no application, and no interactive CarPlay window was available. No connect/browse/select/Now Playing/external-control/disconnect flow is claimed as exercised on the actual CarPlay display.
- **Distribution prerequisite:** obtain Apple's managed CarPlay Audio capability for the App ID, regenerate an authorized provisioning profile, and sign the target with that profile. The checked-in `com.apple.developer.carplay-audio` declaration and Debug/Release `CODE_SIGN_ENTITLEMENTS` settings do not grant capability. Unsigned simulator success does not prove signed device/distribution authorization.
- Physical CarPlay, accessory/system controls, listening, interruptions/routes/background behavior, and visual/accessibility acceptance remain user-owned and pending. Keep this change active and unarchived until 4.2 has real acceptance evidence; no final Task 4 commit, merge, or push is claimed.

### Integration decision — 2026-10-16

- At the user's explicit request, the reviewed implementation was committed as `47f81234` and fast-forward merged into local `main` before interactive acceptance. No push performed.
- This integration does not close 4.2 or change the capability/provisioning prerequisites. The change remains active and unarchived; physical and interactive CarPlay acceptance remains pending.
