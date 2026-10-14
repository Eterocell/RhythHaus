import Shared
import SwiftUI

@main
@MainActor
struct iOSApp: App {
    private let audioPlayerProvider = RhythHausAudioPlayerProvider()
    private let artworkProvider = RhythHausArtworkProvider()
    private let systemMediaPublisher = RhythHausSystemMediaPublisher(
        facade: RhythHausDiKt.sharedCarPlayFacade())
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
