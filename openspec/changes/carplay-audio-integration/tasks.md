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

- [ ] 4.1 Add CarPlay audio entitlement declaration and target signing configuration without fabricating Apple's managed capability; document approval/profile prerequisites.
- [ ] 4.2 Run CarPlay Simulator smoke for connect, browse, select, Now Playing, external controls, disconnect/reconnect, and loading/empty states; record simulator limitations.
- [ ] 4.3 Run required focused quality/architecture/OpenSpec checks, independent review, synchronize specs, and update roadmap/progress with external entitlement and physical-device acceptance status.
