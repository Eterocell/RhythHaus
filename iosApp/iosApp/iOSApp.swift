import Shared
import SwiftUI

@main
@MainActor
struct iOSApp: App {
    static let carPlayFacade = RhythHausDiKt.sharedCarPlayFacade()

    private let audioPlayerProvider = RhythHausAudioPlayerProvider()
    private let artworkProvider = RhythHausArtworkProvider()
    private let systemMediaPublisher = RhythHausSystemMediaPublisher(
        facade: iOSApp.carPlayFacade)
    private let playlistBackupDocumentProvider = RhythHausPlaylistBackupDocumentProvider()
    private let libraryImportProvider = RhythHausLibraryImportProvider()

    init() {
        RhythHausAppBootstrapper.configure(
            audioPlayerProvider: audioPlayerProvider,
            artworkProvider: artworkProvider,
            systemMediaPublisher: systemMediaPublisher,
            playlistBackupDocumentProvider: playlistBackupDocumentProvider,
            libraryImportProvider: libraryImportProvider
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
