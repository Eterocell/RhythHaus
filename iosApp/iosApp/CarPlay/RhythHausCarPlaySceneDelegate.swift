import CarPlay
import Shared
import UIKit

@MainActor
final class RhythHausCarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    private enum Route: Hashable {
        case category(String)
        case detail(category: String, entry: String)
    }

    private let templateFactory = RhythHausCarPlayTemplateFactory()
    private var interfaceController: CPInterfaceController?
    private var session: RhythHausCarPlaySceneSession?
    private var currentSnapshot: CarPlaySnapshot?
    private var templatesByRoute: [Route: CPListTemplate] = [:]
    private var connectionGeneration = 0

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController
    ) {
        guard self.interfaceController !== interfaceController else { return }
        clearConnection()
        self.interfaceController = interfaceController
        let generation = connectionGeneration
        let rootTemplate = templateFactory.makeRootTemplate(
            onOpenCategory: { [weak self] categoryID, completion in
                guard let self else {
                    completion()
                    return
                }
                self.openCategory(categoryID, completion: completion)
            },
            onShowNowPlaying: { [weak self] completion in
                guard let self else {
                    completion()
                    return
                }
                self.showNowPlaying(completion: completion)
            })
        let source = RhythHausSharedCarPlaySnapshotSource(facade: iOSApp.carPlayFacade)
        let session = RhythHausCarPlaySceneSession(source: source)
        self.session = session

        interfaceController.setRootTemplate(rootTemplate, animated: false) { [weak self] success, _ in
            guard !success, let self, self.connectionGeneration == generation else { return }
            self.presentSelectionFailure(completion: {})
        }
        session.connect { [weak self] snapshot in
            guard let self, self.connectionGeneration == generation else { return }
            self.currentSnapshot = snapshot
            self.refreshVisibleTemplates(from: snapshot)
        }
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnectInterfaceController interfaceController: CPInterfaceController
    ) {
        guard self.interfaceController === interfaceController else { return }
        clearConnection()
    }

    private func clearConnection() {
        connectionGeneration += 1
        session?.disconnect()
        session = nil
        interfaceController = nil
        currentSnapshot = nil
        templatesByRoute.removeAll()
    }

    private func openCategory(
        _ categoryID: CarPlayCategoryId,
        completion: @escaping () -> Void
    ) {
        let completionGate = RhythHausCarPlayCompletionGate<Void> { _ in completion() }
        guard let interfaceController else {
            completionGate.finish(())
            return
        }
        let template = makeCategoryTemplate(categoryID)
        templatesByRoute[.category(categoryKey(categoryID))] = template
        push(template, on: interfaceController) { [weak self] success in
            guard let self else {
                completionGate.finish(())
                return
            }
            if success {
                completionGate.finish(())
            } else {
                self.presentSelectionFailure {
                    completionGate.finish(())
                }
            }
        }
    }

    private func openDetail(
        categoryID: CarPlayCategoryId,
        entryID: String,
        completion: @escaping () -> Void
    ) {
        let completionGate = RhythHausCarPlayCompletionGate<Void> { _ in completion() }
        guard let interfaceController else {
            completionGate.finish(())
            return
        }
        let template = templateFactory.makeDetailTemplate(
            categoryID: categoryID,
            entryID: entryID,
            snapshot: currentSnapshot,
            onSelectEntry: { [weak self] entryID, revision, onResult in
                guard let self else {
                    onResult(false)
                    return
                }
                self.selectEntry(entryID, expectedRevision: revision, onResult: onResult)
            })
        templatesByRoute[.detail(category: categoryKey(categoryID), entry: entryID)] = template
        push(template, on: interfaceController) { [weak self] success in
            guard let self else {
                completionGate.finish(())
                return
            }
            if success {
                completionGate.finish(())
            } else {
                self.presentSelectionFailure {
                    completionGate.finish(())
                }
            }
        }
    }

    private func makeCategoryTemplate(_ categoryID: CarPlayCategoryId) -> CPListTemplate {
        templateFactory.makeCategoryTemplate(
            categoryID: categoryID,
            snapshot: currentSnapshot,
            onOpenDetail: { [weak self] categoryID, entryID, completion in
                guard let self else {
                    completion()
                    return
                }
                self.openDetail(categoryID: categoryID, entryID: entryID, completion: completion)
            },
            onSelectEntry: { [weak self] entryID, revision, onResult in
                guard let self else {
                    onResult(false)
                    return
                }
                self.selectEntry(entryID, expectedRevision: revision, onResult: onResult)
            })
    }

    private func showNowPlaying(completion: @escaping () -> Void) {
        let completionGate = RhythHausCarPlayCompletionGate<Void> { _ in completion() }
        guard let interfaceController else {
            completionGate.finish(())
            return
        }
        // The app-lifetime system-media publisher feeds CarPlay's shared Now Playing template.
        push(CPNowPlayingTemplate.shared, on: interfaceController) { [weak self] success in
            guard let self else {
                completionGate.finish(())
                return
            }
            if success {
                completionGate.finish(())
            } else {
                self.presentSelectionFailure {
                    completionGate.finish(())
                }
            }
        }
    }

    private func selectEntry(
        _ entryID: String,
        expectedRevision: Int64,
        onResult: @escaping (Bool) -> Void
    ) {
        let resultGate = RhythHausCarPlayCompletionGate<Bool>(onResult)
        guard let session else {
            resultGate.finish(false)
            return
        }
        let generation = connectionGeneration
        session.selectEntry(entryID, expectedRevision: expectedRevision) { [weak self] accepted in
            guard let self else {
                resultGate.finish(accepted)
                return
            }
            guard self.connectionGeneration == generation else {
                resultGate.finish(accepted)
                return
            }
            guard accepted else {
                self.presentSelectionFailure {
                    resultGate.finish(false)
                }
                return
            }
            guard let interfaceController = self.interfaceController else {
                resultGate.finish(true)
                return
            }
            self.push(CPNowPlayingTemplate.shared, on: interfaceController) { success in
                if success {
                    resultGate.finish(true)
                } else {
                    self.presentSelectionFailure {
                        resultGate.finish(true)
                    }
                }
            }
        }
    }

    private func push(
        _ template: CPTemplate,
        on interfaceController: CPInterfaceController,
        completion: @escaping (Bool) -> Void
    ) {
        let completionGate = RhythHausCarPlayCompletionGate<Bool>(completion)
        interfaceController.pushTemplate(template, animated: true) { success, _ in
            completionGate.finish(success)
        }
    }

    private func refreshVisibleTemplates(from snapshot: CarPlaySnapshot) {
        for (route, template) in templatesByRoute {
            switch route {
            case .category(let categoryKey):
                guard let categoryID = categoryID(for: categoryKey) else { continue }
                templateFactory.refresh(template, with: makeCategoryTemplate(categoryID))
            case .detail(let categoryKey, let entryID):
                guard let categoryID = categoryID(for: categoryKey) else { continue }
                let replacement = templateFactory.makeDetailTemplate(
                    categoryID: categoryID,
                    entryID: entryID,
                    snapshot: snapshot,
                    onSelectEntry: { [weak self] entryID, revision, onResult in
                        guard let self else {
                            onResult(false)
                            return
                        }
                        self.selectEntry(entryID, expectedRevision: revision, onResult: onResult)
                    })
                templateFactory.refresh(template, with: replacement)
            }
        }
    }

    private func categoryID(for key: String) -> CarPlayCategoryId? {
        switch key {
        case "songs": .songs
        case "albums": .albums
        case "artists": .artists
        case "playlists": .playlists
        default: nil
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

    private func presentSelectionFailure(completion: @escaping () -> Void) {
        let completionGate = RhythHausCarPlayCompletionGate<Void> { _ in completion() }
        guard let interfaceController else {
            completionGate.finish(())
            return
        }
        let dismissAction = CPAlertAction(
            title: carPlayLocalizedString("carplay.action.ok", fallback: "OK"),
            style: .default) { [weak self] _ in
                guard let interfaceController = self?.interfaceController else { return }
                interfaceController.dismissTemplate(animated: true, completion: { _, _ in })
            }
        let alert = CPAlertTemplate(
            titleVariants: [
                carPlayLocalizedString(
                    "carplay.selectionUnavailable",
                    fallback: "This track is no longer available."),
            ],
            actions: [dismissAction])
        interfaceController.presentTemplate(alert, animated: true) { _, _ in
            completionGate.finish(())
        }
    }
}

@MainActor
private final class RhythHausSharedCarPlaySnapshotSource: RhythHausCarPlaySnapshotSource {
    private let facade: CarPlayFacade
    private var observerID: String?
    private var generation = 0

    init(facade: CarPlayFacade) {
        self.facade = facade
    }

    func observeSnapshots(_ onSnapshot: @escaping (CarPlaySnapshot) -> Void) -> String {
        generation += 1
        let observerGeneration = generation
        let observerID = facade.observeSnapshots { [weak self] snapshot in
            Self.deliverOnMain { [weak self] in
                guard let self, self.generation == observerGeneration else { return }
                onSnapshot(snapshot)
            }
        }
        self.observerID = observerID
        return observerID
    }

    func removeSnapshotObserver(_ observerID: String) {
        guard self.observerID == observerID else { return }
        generation += 1
        self.observerID = nil
        facade.removeSnapshotObserver(observerId: observerID)
    }

    func selectEntry(
        _ entryID: String,
        expectedRevision: Int64,
        onResult: @escaping (Bool) -> Void
    ) {
        facade.selectEntry(entryId: entryID, expectedRevision: expectedRevision) { result in
            Self.deliverOnMain {
                onResult(result == .applied)
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

private func carPlayLocalizedString(_ key: String, fallback: String) -> String {
    NSLocalizedString(
        key,
        tableName: "Localizable",
        bundle: .main,
        value: fallback,
        comment: "CarPlay interface label")
}
