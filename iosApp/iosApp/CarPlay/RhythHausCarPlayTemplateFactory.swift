import CarPlay
import Shared
import UIKit

@MainActor
protocol RhythHausCarPlaySnapshotSource: AnyObject {
    func observeSnapshots(_ onSnapshot: @escaping (CarPlaySnapshot) -> Void) -> String
    func removeSnapshotObserver(_ observerID: String)
    func selectEntry(
        _ entryID: String,
        expectedRevision: Int64,
        onResult: @escaping (Bool) -> Void)
}

@MainActor
final class RhythHausCarPlaySceneSession {
    private let source: RhythHausCarPlaySnapshotSource
    private var observerID: String?
    private var generation = 0
    private var connected = false
    private var onSnapshot: ((CarPlaySnapshot) -> Void)?
    private(set) var currentSnapshot: CarPlaySnapshot?

    init(source: RhythHausCarPlaySnapshotSource) {
        self.source = source
    }

    func connect(onSnapshot: @escaping (CarPlaySnapshot) -> Void) {
        guard !connected else { return }
        connected = true
        generation += 1
        let connectionGeneration = generation
        self.onSnapshot = onSnapshot
        let observerID = source.observeSnapshots { [weak self] snapshot in
            Self.deliverOnMain { [weak self] in
                guard let self,
                      self.connected,
                      self.generation == connectionGeneration else { return }
                self.currentSnapshot = snapshot
                self.onSnapshot?(snapshot)
            }
        }
        if connected, generation == connectionGeneration {
            self.observerID = observerID
        } else {
            source.removeSnapshotObserver(observerID)
        }
    }

    func disconnect() {
        guard connected else { return }
        connected = false
        generation += 1
        currentSnapshot = nil
        onSnapshot = nil
        let observerID = self.observerID
        self.observerID = nil
        if let observerID {
            source.removeSnapshotObserver(observerID)
        }
    }

    func selectEntry(
        _ entryID: String,
        expectedRevision: Int64,
        onResult: @escaping (Bool) -> Void
    ) {
        let completion = RhythHausCarPlayCompletionGate(onResult)
        guard connected,
              let snapshot = currentSnapshot,
              snapshot.revision == expectedRevision,
              snapshot.containsPlayableEntry(entryID) else {
            completion.finish(false)
            return
        }
        source.selectEntry(entryID, expectedRevision: expectedRevision) { result in
            Self.deliverOnMain {
                completion.finish(result)
            }
        }
    }

    private nonisolated static func deliverOnMain(
        _ action: @escaping @MainActor () -> Void
    ) {
        if Thread.isMainThread {
            MainActor.assumeIsolated(action)
        } else {
            DispatchQueue.main.async {
                MainActor.assumeIsolated(action)
            }
        }
    }
}

@MainActor
final class RhythHausCarPlayTemplateFactory {
    func makeRootTemplate(
        onOpenCategory: @escaping (CarPlayCategoryId, @escaping () -> Void) -> Void,
        onShowNowPlaying: @escaping (@escaping () -> Void) -> Void
    ) -> CPListTemplate {
        let categories: [(CarPlayCategoryId, String)] = [
            (.songs, localized("carplay.category.songs", fallback: "Songs")),
            (.albums, localized("carplay.category.albums", fallback: "Albums")),
            (.artists, localized("carplay.category.artists", fallback: "Artists")),
            (.playlists, localized("carplay.category.playlists", fallback: "Playlists")),
        ]
        var items = categories.map { categoryID, title in
            let item = CPListItem(text: title, detailText: nil)
            item.userInfo = "category:\(categoryKey(categoryID))"
            item.handler = { _, completionBlock in
                let completion = RhythHausCarPlayCompletionGate<Void> { _ in completionBlock() }
                onOpenCategory(categoryID) { completion.finish(()) }
            }
            return item
        }
        let nowPlaying = CPListItem(
            text: localized("carplay.nowPlaying", fallback: "Now Playing"),
            detailText: nil)
        nowPlaying.userInfo = "now-playing"
        nowPlaying.handler = { _, completionBlock in
            let completion = RhythHausCarPlayCompletionGate<Void> { _ in completionBlock() }
            onShowNowPlaying { completion.finish(()) }
        }
        items.append(nowPlaying)
        return CPListTemplate(
            title: localized("carplay.root.title", fallback: "RhythHaus"),
            sections: [CPListSection(items: items)])
    }

    func makeCategoryTemplate(
        categoryID: CarPlayCategoryId,
        snapshot: CarPlaySnapshot?,
        onOpenDetail: @escaping (CarPlayCategoryId, String, @escaping () -> Void) -> Void,
        onSelectEntry: @escaping (String, Int64, @escaping (Bool) -> Void) -> Void
    ) -> CPListTemplate {
        let category = snapshot?.categories.first { $0.id == categoryID }
        let status = snapshot?.libraryStatus ?? .loading
        let entries = status == .ready ? category?.entries ?? [] : []
        let revision = snapshot?.revision ?? 0
        let title = categoryTitle(categoryID)
        return listTemplate(
            title: title,
            entries: entries,
            status: status,
            emptyTitle: categoryID == .playlists && snapshot?.playlistsUnavailable == true
                ? localized("carplay.playlists.unavailable", fallback: "Playlists unavailable")
                : categoryEmptyTitle(categoryID),
            categoryID: categoryID,
            revision: revision,
            allowsDetailNavigation: categoryID != .songs,
            onOpenDetail: onOpenDetail,
            onSelectEntry: onSelectEntry)
    }

    func makeDetailTemplate(
        categoryID: CarPlayCategoryId,
        entryID: String,
        snapshot: CarPlaySnapshot?,
        onSelectEntry: @escaping (String, Int64, @escaping (Bool) -> Void) -> Void
    ) -> CPListTemplate {
        let category = snapshot?.categories.first { $0.id == categoryID }
        let parent = category?.entries.first { $0.id == entryID }
        let status = snapshot?.libraryStatus ?? .loading
        let entries = status == .ready ? parent?.children ?? [] : []
        return listTemplate(
            title: parent?.title ?? categoryTitle(categoryID),
            entries: entries,
            status: status,
            emptyTitle: localized("carplay.empty.tracks", fallback: "No tracks available"),
            categoryID: categoryID,
            revision: snapshot?.revision ?? 0,
            allowsDetailNavigation: false,
            onOpenDetail: { _, _, completion in completion() },
            onSelectEntry: onSelectEntry)
    }

    func refresh(_ template: CPListTemplate, with replacement: CPListTemplate) {
        if let pager = replacement.userInfo as? RhythHausCarPlayListPager {
            let page = (template.userInfo as? RhythHausCarPlayListPager)?.page ?? 0
            pager.attach(to: template, page: page)
        } else {
            template.updateSections(replacement.sections)
        }
        template.emptyViewTitleVariants = replacement.emptyViewTitleVariants
        template.emptyViewSubtitleVariants = replacement.emptyViewSubtitleVariants
        template.showsSpinnerWhileEmpty = replacement.showsSpinnerWhileEmpty
    }

    private func listTemplate(
        title: String,
        entries: [CarPlayLibraryEntry],
        status: CarPlayLibraryStatus,
        emptyTitle: String,
        categoryID: CarPlayCategoryId,
        revision: Int64,
        allowsDetailNavigation: Bool,
        onOpenDetail: @escaping (CarPlayCategoryId, String, @escaping () -> Void) -> Void,
        onSelectEntry: @escaping (String, Int64, @escaping (Bool) -> Void) -> Void
    ) -> CPListTemplate {
        let makeItem: (CarPlayLibraryEntry) -> CPListItem = { entry in
            let item = CPListItem(text: entry.title, detailText: entry.subtitle)
            item.userInfo = entry.id
            if entry.isPlayable {
                item.handler = { _, completionBlock in
                    let completion = RhythHausCarPlayCompletionGate<Bool> { _ in
                        completionBlock()
                    }
                    onSelectEntry(entry.id, revision) { accepted in
                        completion.finish(accepted)
                    }
                }
            } else if allowsDetailNavigation {
                item.handler = { _, completionBlock in
                    let completion = RhythHausCarPlayCompletionGate<Void> { _ in
                        completionBlock()
                    }
                    onOpenDetail(categoryID, entry.id) { completion.finish(()) }
                }
            } else {
                item.isEnabled = false
            }
            return item
        }
        let template = CPListTemplate(
            title: title,
            sections: [])
        RhythHausCarPlayListPager(
            entries: entries,
            makeItem: makeItem,
            previousTitle: localized("carplay.page.previous", fallback: "Previous page"),
            nextTitle: localized("carplay.page.next", fallback: "Next page"))
            .attach(to: template, page: 0)
        template.emptyViewTitleVariants = [
            status == .loading
                ? localized("carplay.loading", fallback: "Loading library")
                : emptyTitle,
        ]
        template.emptyViewSubtitleVariants = []
        template.showsSpinnerWhileEmpty = status == .loading
        return template
    }

    private func categoryTitle(_ categoryID: CarPlayCategoryId) -> String {
        if categoryID == .songs {
            localized("carplay.category.songs", fallback: "Songs")
        } else if categoryID == .albums {
            localized("carplay.category.albums", fallback: "Albums")
        } else if categoryID == .artists {
            localized("carplay.category.artists", fallback: "Artists")
        } else {
            localized("carplay.category.playlists", fallback: "Playlists")
        }
    }

    private func categoryEmptyTitle(_ categoryID: CarPlayCategoryId) -> String {
        if categoryID == .songs {
            localized("carplay.empty.songs", fallback: "No songs yet")
        } else if categoryID == .albums {
            localized("carplay.empty.albums", fallback: "No albums yet")
        } else if categoryID == .artists {
            localized("carplay.empty.artists", fallback: "No artists yet")
        } else {
            localized("carplay.empty.playlists", fallback: "No playlists yet")
        }
    }

    private func categoryKey(_ categoryID: CarPlayCategoryId) -> String {
        if categoryID == .songs {
            "songs"
        } else if categoryID == .albums {
            "albums"
        } else if categoryID == .artists {
            "artists"
        } else {
            "playlists"
        }
    }

    private func localized(_ key: String, fallback: String) -> String {
        NSLocalizedString(
            key,
            tableName: "Localizable",
            bundle: .main,
            value: fallback,
            comment: "CarPlay interface label")
    }
}

/// Updates one template in place, so paging never consumes navigation stack depth.
@MainActor
private final class RhythHausCarPlayListPager {
    private let entries: [CarPlayLibraryEntry]
    private let makeItem: (CarPlayLibraryEntry) -> CPListItem
    private let previousTitle: String
    private let nextTitle: String
    private weak var template: CPListTemplate?
    private(set) var page = 0

    init(
        entries: [CarPlayLibraryEntry],
        makeItem: @escaping (CarPlayLibraryEntry) -> CPListItem,
        previousTitle: String,
        nextTitle: String
    ) {
        self.entries = entries
        self.makeItem = makeItem
        self.previousTitle = previousTitle
        self.nextTitle = nextTitle
    }

    func attach(to template: CPListTemplate, page: Int) {
        self.template = template
        template.userInfo = self
        show(page)
    }

    private func show(_ requestedPage: Int) {
        let limit = Int(CPListTemplate.maximumItemCount)
        let pageSize = entries.count > limit ? max(1, limit - 2) : max(1, limit)
        let lastPage = max(0, (entries.count - 1) / pageSize)
        page = min(max(0, requestedPage), lastPage)
        let start = page * pageSize
        let end = min(entries.count, start + pageSize)
        var items = entries[start ..< end].map(makeItem)
        if page > 0 {
            items.insert(navigationItem(previousTitle, delta: -1), at: 0)
        }
        if page < lastPage {
            items.append(navigationItem(nextTitle, delta: 1))
        }
        template?.updateSections(items.isEmpty ? [] : [CPListSection(items: items)])
    }

    private func navigationItem(_ title: String, delta: Int) -> CPListItem {
        let item = CPListItem(text: title, detailText: nil)
        item.userInfo = delta > 0 ? "carplay-page-next" : "carplay-page-previous"
        item.handler = { [self] _, completion in
            show(page + delta)
            completion()
        }
        return item
    }
}

@MainActor
final class RhythHausCarPlayCompletionGate<Value> {
    private var completion: ((Value) -> Void)?

    init(_ completion: @escaping (Value) -> Void) {
        self.completion = completion
    }

    func finish(_ value: Value) {
        guard let completion else { return }
        self.completion = nil
        completion(value)
    }
}

private extension CarPlaySnapshot {
    func containsPlayableEntry(_ entryID: String) -> Bool {
        categories.contains { category in
            category.entries.contains { entry in
                (entry.id == entryID && entry.isPlayable) ||
                    entry.children.contains { child in
                        child.id == entryID && child.isPlayable
                    }
            }
        }
    }
}
