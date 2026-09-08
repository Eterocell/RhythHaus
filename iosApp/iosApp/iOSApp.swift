import Shared
import SwiftUI

@main
struct iOSApp: App {
    private let audioPlayerProvider = RhythHausAudioPlayerProvider()
    private let artworkProvider = RhythHausArtworkProvider()
    private let playlistBackupDocumentProvider = RhythHausPlaylistBackupDocumentProvider()
    private let libraryImportProvider = RhythHausLibraryImportProvider()

    init() {
        RhythHausAppBootstrapper.configure(
            audioPlayerProvider: audioPlayerProvider,
            artworkProvider: artworkProvider,
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
