# iOS SwiftUI — Fill Missing Compose Features

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Bring the iOS SwiftUI app to feature parity with the Compose UI: album/artist browsing, drill-down navigation, folder import, scan progress, full now-playing controls, and UI polish.

**Architecture:** All changes are in `iosApp/iosApp/` — SwiftUI views consuming `Shared.framework`. Shared KMP module unchanged.

## Global Constraints
- Android + desktop must still build (`./gradlew :shared:jvmTest :androidApp:assembleDebug`)
- iOS Xcode build must succeed
- Semantic commits
- All data from Shared.framework — no new Kotlin code in shared
- Kotlin/Native cinterop quirks: sealed class members use concatenated names (`AudioSourceFilePath`, `AudioSourceUri`), top-level Kotlin functions may need Swift inlining

---

### Task 1: Album Card Grid + Artist List + BrowseModePicker + DrillDownView

**Files:**
- Modify: `iosApp/iosApp/LibraryView.swift`

**What to build:**
1. **BrowseModePicker** — Segmented picker (`.pickerStyle(.segmented)`) toggling between Albums and Artists
2. **AlbumCard grid** — 2-column `LazyVGrid` of album cards, each showing accent-gradient artwork placeholder, album name, artist, track count
3. **ArtistRow list** — List of artists, each showing artist name, album count, track count
4. **DrillDownView** — NavigationLink to a detail view showing album/artist tracks with header, back button, track rows, and play functionality
5. **SectionLabel** — `"Library queue"` / `"N tracks · X total"` header
6. **HeaderSection** — Large "RhythHaus" title + subtitle

**Album/Artist grouping** — use the `groupTracksByAlbum`/`groupTracksByArtist` functions from `LibraryBrowser.kt`. These are in the shared module. In Swift, they need cinterop exposure. Instead of fighting cinterop, compute groups in Swift:

```swift
struct AlbumGroup: Identifiable {
    let album: String
    let tracks: [Track]
    let artist: String?
    var id: String { album }
    
    static func from(_ tracks: [Track]) -> [AlbumGroup] {
        let grouped = Dictionary(grouping: tracks, by: { $0.album })
        return grouped.map { (album, tracks) in
            let sorted = tracks.sorted {
                ($0.discNumber?.intValue ?? 0, $0.trackNumber?.intValue ?? 0, $0.title)
                < ($1.discNumber?.intValue ?? 0, $1.trackNumber?.intValue ?? 0, $1.title)
            }
            return AlbumGroup(album: album, tracks: sorted, artist: sorted.first?.artist)
        }.sorted { $0.album.lowercased() < $1.album.lowercased() }
    }
}

struct ArtistGroup: Identifiable {
    let artist: String
    let tracks: [Track]
    let albumCount: Int
    var id: String { artist }
    
    static func from(_ tracks: [Track]) -> [ArtistGroup] {
        let grouped = Dictionary(grouping: tracks, by: { $0.artist })
        return grouped.map { (artist, tracks) in
            let sorted = tracks.sorted {
                ($0.discNumber?.intValue ?? 0, $0.trackNumber?.intValue ?? 0, $0.title)
                < ($1.discNumber?.intValue ?? 0, $1.trackNumber?.intValue ?? 0, $1.title)
            }
            let albumCount = Set(tracks.map { $0.album }).count
            return ArtistGroup(artist: artist, tracks: sorted, albumCount: albumCount)
        }.sorted { $0.artist.lowercased() < $1.artist.lowercased() }
    }
}
```

**DrillDownView** should be a separate view with:
- Back button styled like the Compose version (black pill with "← LIBRARY" or "← BACK")
- Large album/artist title (44sp → `.system(size: 44, weight: .black)`)
- Subtitle (track count, artist)
- Track list with accent artwork, title, artist, duration
- Mini NowPlayingBar at bottom

**Style reference — Compose AlbumCard:**
- `cornerRadius = 20.dp` → `.clipShape(RoundedRectangle(cornerRadius: 20))`
- Accent gradient artwork placeholder with first 3 letters
- Album name below, artist, track count

**Style reference — Compose ArtistRow:**
- Accent circle artwork placeholder
- Artist name + album/track count

- [ ] **Step 1: Add AlbumGroup/ArtistGroup structs to LibraryView.swift**
- [ ] **Step 2: Add BrowseModePicker segmented control**
- [ ] **Step 3: Add 2-column LazyVGrid AlbumCard**
- [ ] **Step 4: Add ArtistRow list**
- [ ] **Step 5: Add DrillDownView with NavigationLink**
- [ ] **Step 6: Add SectionLabel + HeaderSection**
- [ ] **Step 7: Build and verify in Xcode**
- [ ] **Step 8: Commit**

---

### Task 2: ImportAudioCard + ScanningCard + Clear Library Dialog

**Files:**
- Modify: `iosApp/iosApp/LibraryView.swift` or create `iosApp/iosApp/ImportCardView.swift`

**What to build:**
1. **ImportAudioCard** — Card with "Add music folder" button and "Clear Library" button. 
   - Folder picker: iOS uses `UIDocumentPickerViewController` for folder selection. Wrap in a SwiftUI `.fileImporter` modifier.
   - Clear library: alert dialog confirmation → call `repository.clearAll()` → refresh
2. **ScanningCard** — Card showing scan progress (folders visited, files visited, tracks added) with cancel button
3. **Clear library dialog** — `.alert()` SwiftUI modifier

**iOS folder picker:** Use `.fileImporter(isPresented:allowedContentTypes:allowsMultipleSelection:)` with `.folder` content type. On iOS 14+, this opens the native file picker. The selected URL is the folder path.

For scanning — the Swift side doesn't have direct access to `LibraryScanner` (it's a Kotlin class). Instead, use the folder picker result and call `PlatformSourceAccess` for scanning, or build a minimal Swift scanner.

Actually, looking at the Compose code, the scanner needs `PlatformSourceAccess.createPlatformSourceAccess()` and `LibraryScanner`. These are shared Kotlin classes. In Swift, we'd need cinterop access.

**Alternative approach for Task 2:** Since full scanner integration requires Kotlin interop that may be fragile, build the UI cards first (visual + button actions) and wire the scanner callback later. For now:
- "Add music folder" button opens iOS file picker → stores folder path → calls `repository` via Shared.framework
- Scanning progress card is UI-only for now (show placeholder state)
- Clear library calls `repository.clearAll()` directly

- [ ] **Step 1: Create ImportAudioCard with folder picker**
- [ ] **Step 2: Create ScanningCard (UI placeholder)**
- [ ] **Step 3: Create Clear Library alert dialog**
- [ ] **Step 4: Wire to LibraryStore**
- [ ] **Step 5: Build and verify**
- [ ] **Step 6: Commit**

---

### Task 3: Full NowPlayingView with seek bar, artwork, prev track, equalizer

**Files:**
- Modify: `iosApp/iosApp/LibraryView.swift` (replace placeholder NowPlayingView)

**What to build:**
1. **Seek bar slider** — `Slider(value: $position, in: 0...duration)` bound to `engine.state.positionSeconds`/`durationSeconds`
2. **Position/duration labels** — `0:42` / `3:18` style
3. **Artwork display** — Decode `track.artworkBytes` to `UIImage` and display with `ContentScale.crop` in the large artwork area
4. **Prev track button** — Functional `backward.fill` that skips to previous track in queue
5. **Next track button** — Functional `forward.fill` that skips to next track
6. **Play/pause button** — Large circular, already partially done
7. **Equalizer bars** — Animated bars when playing (like Compose `EqualizerStrip`)
8. **Track number display** — "Track N" subtitle when available

**Queue management:** The SwiftUI layer doesn't have access to `PlaybackController.queue`. The `LibraryView` holds the `libraryStore.snapshot.tracks`. Use that as the queue:
- On next: find current track index, get next, call `playTrack(nextTrack)`
- On prev: find current track index, get prev, call `playTrack(prevTrack)`

- [ ] **Step 1: Add seek bar slider with position/duration labels**
- [ ] **Step 2: Add artwork decoding from artworkBytes**
- [ ] **Step 3: Make prev/next track buttons functional**
- [ ] **Step 4: Add equalizer animation**
- [ ] **Step 5: Add track number display**
- [ ] **Step 6: Build and verify**
- [ ] **Step 7: Commit**

---

### Task 4: Full verification

- [ ] **Step 1: Xcode build succeeds**
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17' build
```

- [ ] **Step 2: Android + shared tests pass**
```bash
./gradlew :shared:jvmTest :androidApp:assembleDebug --configuration-cache
```

- [ ] **Step 3: Full harness**
```bash
./init.sh
```

- [ ] **Step 4: Commit any remaining changes**
