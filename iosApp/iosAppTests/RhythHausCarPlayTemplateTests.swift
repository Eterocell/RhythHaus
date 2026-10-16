import CarPlay
import Shared
import XCTest
@testable import RhythHaus

@MainActor
final class RhythHausCarPlayTemplateTests: XCTestCase {
    func testAppBundleRegistersCarPlayTemplateApplicationScene() throws {
        let manifest = try XCTUnwrap(
            Bundle.main.infoDictionary?["UIApplicationSceneManifest"] as? [String: Any])
        let configurations = try XCTUnwrap(
            manifest["UISceneConfigurations"] as? [String: [[String: Any]]])
        let carPlayScenes = try XCTUnwrap(
            configurations["CPTemplateApplicationSceneSessionRoleApplication"])
        let carPlayScene = try XCTUnwrap(carPlayScenes.first)

        XCTAssertEqual(carPlayScenes.count, 1)
        XCTAssertEqual(carPlayScene["UISceneClassName"] as? String, "CPTemplateApplicationScene")
        let delegateName = try XCTUnwrap(
            carPlayScene["UISceneDelegateClassName"] as? String)
        XCTAssertTrue(delegateName.hasSuffix(".RhythHausCarPlaySceneDelegate"))
    }

    func testRootTemplateOffersLocalizedLibraryCategoriesAndNowPlaying() {
        let factory = RhythHausCarPlayTemplateFactory()
        var openedCategories: [CarPlayCategoryId] = []
        var nowPlayingSelections = 0
        var categoryCompletions = 0
        var nowPlayingCompletions = 0
        let root = factory.makeRootTemplate(
            onOpenCategory: { category, completion in
                openedCategories.append(category)
                completion()
                completion()
            },
            onShowNowPlaying: { completion in
                nowPlayingSelections += 1
                completion()
                completion()
            })

        let rootItems = items(in: root)
        XCTAssertEqual(rootItems.map { $0.text ?? "" }, [
            localized("carplay.category.songs", fallback: "Songs"),
            localized("carplay.category.albums", fallback: "Albums"),
            localized("carplay.category.artists", fallback: "Artists"),
            localized("carplay.category.playlists", fallback: "Playlists"),
            localized("carplay.nowPlaying", fallback: "Now Playing"),
        ])
        guard rootItems.count == 5 else { return }
        rootItems[0].handler?(rootItems[0], { categoryCompletions += 1 })
        rootItems[4].handler?(rootItems[4], { nowPlayingCompletions += 1 })

        XCTAssertEqual(openedCategories, [.songs])
        XCTAssertEqual(categoryCompletions, 1)
        XCTAssertEqual(nowPlayingSelections, 1)
        XCTAssertEqual(nowPlayingCompletions, 1)
    }

    func testLoadingAndEmptyCategoriesShowBoundedStates() {
        let factory = RhythHausCarPlayTemplateFactory()
        let loading = factory.makeCategoryTemplate(
            categoryID: .songs,
            snapshot: nil,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { _, _, onResult in onResult(false) })

        XCTAssertEqual(loading.itemCount, 0)
        XCTAssertTrue(loading.showsSpinnerWhileEmpty)
        XCTAssertEqual(
            loading.emptyViewTitleVariants.first,
            localized("carplay.loading", fallback: "Loading library"))

        let empty = factory.makeCategoryTemplate(
            categoryID: .playlists,
            snapshot: snapshot(status: .empty, categories: emptyCategories()),
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { _, _, onResult in onResult(false) })

        XCTAssertEqual(empty.itemCount, 0)
        XCTAssertFalse(empty.showsSpinnerWhileEmpty)
        XCTAssertEqual(
            empty.emptyViewTitleVariants.first,
            localized("carplay.empty.playlists", fallback: "No playlists yet"))
    }

    func testPopulatedHierarchyPreservesOpaqueEntryIDsAndDetailNavigation() {
        let factory = RhythHausCarPlayTemplateFactory()
        let library = populatedSnapshot()
        var openedDetail: (CarPlayCategoryId, String)?
        var selectedEntry: (String, Int64)?
        var detailCompletions = 0
        var selectionCompletions = 0
        let albumID = "album:12:Local Sessions"
        let songID = "track:opaque/0001"
        let albums = factory.makeCategoryTemplate(
            categoryID: .albums,
            snapshot: library,
            onOpenDetail: { category, entryID, completion in
                openedDetail = (category, entryID)
                completion()
            },
            onSelectEntry: { entryID, revision, onResult in
                selectedEntry = (entryID, revision)
                onResult(true)
            })

        XCTAssertEqual(items(in: albums).map { $0.text ?? "" }, ["Local Sessions"])
        guard let albumItem = firstItem(in: albums) else { return }
        XCTAssertEqual(albumItem.userInfo as? String, albumID)
        albumItem.handler?(albumItem, { detailCompletions += 1 })
        XCTAssertEqual(openedDetail?.0, .albums)
        XCTAssertEqual(openedDetail?.1, albumID)
        XCTAssertEqual(detailCompletions, 1)

        let detail = factory.makeDetailTemplate(
            categoryID: .albums,
            entryID: albumID,
            snapshot: library,
            onSelectEntry: { entryID, revision, onResult in
                selectedEntry = (entryID, revision)
                onResult(true)
                onResult(false)
            })
        guard let songItem = firstItem(in: detail) else { return }
        XCTAssertEqual(songItem.userInfo as? String, songID)
        songItem.handler?(songItem, { selectionCompletions += 1 })

        XCTAssertEqual(selectedEntry?.0, songID)
        XCTAssertEqual(selectedEntry?.1, 42)
        XCTAssertEqual(selectionCompletions, 1)
    }

    func testStaleSelectionIsRejectedWithoutDispatchAndCompletesOnce() {
        let source = FakeCarPlaySnapshotSource()
        let session = RhythHausCarPlaySceneSession(source: source)
        let first = populatedSnapshot()
        var results: [Bool] = []
        session.connect(onSnapshot: { _ in })
        source.send(first)

        let factory = RhythHausCarPlayTemplateFactory()
        let songs = factory.makeCategoryTemplate(
            categoryID: .songs,
            snapshot: first,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { entryID, revision, onResult in
                session.selectEntry(
                    entryID,
                    expectedRevision: revision,
                    onResult: { result in
                        results.append(result)
                        onResult(result)
                    })
            })
        guard let staleItem = firstItem(in: songs) else { return }
        source.send(snapshot(status: .ready, revision: 43, categories: emptyCategories()))
        var completions = 0
        staleItem.handler?(staleItem, { completions += 1 })

        XCTAssertEqual(source.selectedEntryIDs, [])
        XCTAssertEqual(results, [false])
        XCTAssertEqual(completions, 1)
        XCTAssertEqual(source.playbackMutationCount, 0)
    }

    func testExistingCategoryTemplateRefreshesFromLatestSnapshot() {
        let factory = RhythHausCarPlayTemplateFactory()
        let initial = populatedSnapshot()
        let existing = factory.makeCategoryTemplate(
            categoryID: .songs,
            snapshot: initial,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { _, _, completion in completion(false) })
        let updatedSong = CarPlayLibraryEntry(
            id: "track:opaque/0002",
            title: "Morning Rain",
            subtitle: nil,
            children: [],
            isPlayable: true)
        let updated = snapshot(
            status: .ready,
            revision: 43,
            categories: [
                CarPlayLibraryCategory(id: .songs, title: "Shared Songs", entries: [updatedSong]),
                CarPlayLibraryCategory(id: .albums, title: "Shared Albums", entries: []),
                CarPlayLibraryCategory(id: .artists, title: "Shared Artists", entries: []),
                CarPlayLibraryCategory(id: .playlists, title: "Shared Playlists", entries: []),
            ])
        let replacement = factory.makeCategoryTemplate(
            categoryID: .songs,
            snapshot: updated,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { _, _, completion in completion(false) })

        factory.refresh(existing, with: replacement)

        XCTAssertEqual(items(in: existing).map { $0.text ?? "" }, ["Morning Rain"])
        XCTAssertEqual(items(in: existing).first?.userInfo as? String, "track:opaque/0002")
    }

    func testCurrentSelectionDispatchesOnceWhenFacadeCompletesTwice() {
        let source = FakeCarPlaySnapshotSource()
        let session = RhythHausCarPlaySceneSession(source: source)
        let library = populatedSnapshot()
        session.connect(onSnapshot: { _ in })
        source.send(library)
        var results: [Bool] = []
        let factory = RhythHausCarPlayTemplateFactory()
        let songs = factory.makeCategoryTemplate(
            categoryID: .songs,
            snapshot: library,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: { entryID, revision, onResult in
                session.selectEntry(
                    entryID,
                    expectedRevision: revision,
                    onResult: { result in
                        results.append(result)
                        onResult(result)
                    })
            })
        var completions = 0
        guard let songItem = firstItem(in: songs) else { return }
        songItem.handler?(songItem, { completions += 1 })

        XCTAssertEqual(source.selectedEntryIDs, ["track:opaque/0001"])
        XCTAssertEqual(results, [true])
        XCTAssertEqual(completions, 1)
        XCTAssertEqual(source.playbackMutationCount, 1)
    }

    func testDisconnectReconnectCleansObserversWithoutChangingPlayback() {
        let source = FakeCarPlaySnapshotSource()
        let session = RhythHausCarPlaySceneSession(source: source)
        var receivedRevisions: [Int64] = []
        session.connect(onSnapshot: { receivedRevisions.append($0.revision) })
        session.connect(onSnapshot: { receivedRevisions.append($0.revision) })
        XCTAssertEqual(source.observerCount, 1)
        source.send(populatedSnapshot())

        session.disconnect()
        session.disconnect()
        XCTAssertEqual(source.observerCount, 0)
        XCTAssertEqual(source.playbackMutationCount, 0)
        source.send(snapshot(status: .ready, revision: 43, categories: emptyCategories()))
        XCTAssertEqual(receivedRevisions, [42])

        session.connect(onSnapshot: { receivedRevisions.append($0.revision) })
        XCTAssertEqual(source.observerCount, 1)
        source.send(snapshot(status: .ready, revision: 44, categories: emptyCategories()))
        XCTAssertEqual(receivedRevisions, [42, 44])
        session.disconnect()
        XCTAssertEqual(source.observerCount, 0)
        XCTAssertEqual(source.removeCount, 2)
        XCTAssertEqual(source.playbackMutationCount, 0)
    }

    func testLargeListsReachTailAndRefreshClampsPage() throws {
        let factory = RhythHausCarPlayTemplateFactory()
        let entries = (0 ..< Int(CPListTemplate.maximumItemCount) + 3).map { index in
            CarPlayLibraryEntry(id: "song-\(index)", title: "Song \(index)",
                                subtitle: nil, children: [], isPlayable: true)
        }
        var selected: String?
        var selectedRevision: Int64?
        func make(_ entries: [CarPlayLibraryEntry]) -> CPListTemplate {
            factory.makeCategoryTemplate(
                categoryID: .songs,
                snapshot: snapshot(status: .ready, categories: [
                    CarPlayLibraryCategory(id: .songs, title: "Songs", entries: entries),
                ]),
                onOpenDetail: { _, _, completion in completion() },
                onSelectEntry: { id, revision, completion in
                    selected = id
                    selectedRevision = revision
                    completion(true)
                })
        }
        let template = make(entries)
        var completions = 0
        while let next = items(in: template).first(where: {
            $0.userInfo as? String == "carplay-page-next"
        }) {
            XCTAssertLessThanOrEqual(template.itemCount, CPListTemplate.maximumItemCount)
            next.handler?(next, { completions += 1 })
        }
        let tail = try XCTUnwrap(items(in: template).first {
            $0.userInfo as? String == entries.last?.id
        })
        tail.handler?(tail, {})
        XCTAssertEqual(selected, entries.last?.id)
        XCTAssertEqual(selectedRevision, 42)
        XCTAssertGreaterThan(completions, 0)

        factory.refresh(template, with: make([entries[0]]))
        XCTAssertEqual(items(in: template).map { $0.userInfo as? String }, [entries[0].id])
    }

    private func snapshot(
        status: CarPlayLibraryStatus,
        revision: Int64 = 42,
        categories: [CarPlayLibraryCategory],
        playlistsUnavailable: Bool = false
    ) -> CarPlaySnapshot {
        CarPlaySnapshot(
            revision: revision,
            libraryStatus: status,
            categories: categories,
            playback: CarPlayPlaybackProjection(
                selectionGeneration: 0,
                currentTrack: nil,
                status: .idle,
                positionMillis: 0,
                durationMillis: nil,
                canPlay: false,
                canPause: false,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: false),
            playlistsUnavailable: playlistsUnavailable)
    }

    private func emptyCategories() -> [CarPlayLibraryCategory] {
        [
            CarPlayLibraryCategory(id: .songs, title: "Shared Songs", entries: []),
            CarPlayLibraryCategory(id: .albums, title: "Shared Albums", entries: []),
            CarPlayLibraryCategory(id: .artists, title: "Shared Artists", entries: []),
            CarPlayLibraryCategory(id: .playlists, title: "Shared Playlists", entries: []),
        ]
    }

    private func populatedSnapshot() -> CarPlaySnapshot {
        let song = CarPlayLibraryEntry(
            id: "track:opaque/0001",
            title: "Night Drive",
            subtitle: "Rhyth Haus · Local Sessions",
            children: [],
            isPlayable: true)
        let album = CarPlayLibraryEntry(
            id: "album:12:Local Sessions",
            title: "Local Sessions",
            subtitle: "Rhyth Haus",
            children: [song],
            isPlayable: false)
        let artist = CarPlayLibraryEntry(
            id: "artist:10:Rhyth Haus",
            title: "Rhyth Haus",
            subtitle: nil,
            children: [song],
            isPlayable: false)
        let playlist = CarPlayLibraryEntry(
            id: "playlist:9:Favorites",
            title: "Favorites",
            subtitle: nil,
            children: [song],
            isPlayable: false)
        return snapshot(
            status: .ready,
            categories: [
                CarPlayLibraryCategory(id: .songs, title: "Shared Songs", entries: [song]),
                CarPlayLibraryCategory(id: .albums, title: "Shared Albums", entries: [album]),
                CarPlayLibraryCategory(id: .artists, title: "Shared Artists", entries: [artist]),
                CarPlayLibraryCategory(id: .playlists, title: "Shared Playlists", entries: [playlist]),
            ])
    }

    private func items(in template: CPListTemplate) -> [CPListItem] {
        template.sections.flatMap { section in
            section.items.compactMap { $0 as? CPListItem }
        }
    }

    private func localized(_ key: String, fallback: String) -> String {
        Bundle.main.localizedString(forKey: key, value: fallback, table: "Localizable")
    }

    private func firstItem(in template: CPListTemplate) -> CPListItem? {
        guard let item = items(in: template).first else {
            XCTFail("Expected a list item")
            return nil
        }
        return item
    }

}

@MainActor
private final class FakeCarPlaySnapshotSource: RhythHausCarPlaySnapshotSource {
    private var observers: [String: (CarPlaySnapshot) -> Void] = [:]
    private var nextObserver = 0

    private(set) var selectedEntryIDs: [String] = []
    private(set) var removeCount = 0
    private(set) var playbackMutationCount = 0

    var observerCount: Int { observers.count }

    func observeSnapshots(_ onSnapshot: @escaping (CarPlaySnapshot) -> Void) -> String {
        nextObserver += 1
        let observerID = "observer-\(nextObserver)"
        observers[observerID] = onSnapshot
        return observerID
    }

    func removeSnapshotObserver(_ observerID: String) {
        removeCount += 1
        observers.removeValue(forKey: observerID)
    }

    func selectEntry(
        _ entryID: String,
        expectedRevision: Int64,
        onResult: @escaping (Bool) -> Void
    ) {
        selectedEntryIDs.append(entryID)
        playbackMutationCount += 1
        onResult(true)
        onResult(true)
    }

    func send(_ snapshot: CarPlaySnapshot) {
        Array(observers.values).forEach { $0(snapshot) }
    }
}
