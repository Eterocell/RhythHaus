import Shared
import SwiftUI

@main
struct iOSApp: App {
    private let audioPlayerProvider = RhythHausAudioPlayerProvider()
    private let artworkProvider = RhythHausArtworkProvider()
    private let playlistBackupDocumentProvider = RhythHausPlaylistBackupDocumentProvider()

    init() {
        RhythHausAppBootstrapper.configure(
            audioPlayerProvider: audioPlayerProvider,
            artworkProvider: artworkProvider,
            playlistBackupDocumentProvider: playlistBackupDocumentProvider
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
