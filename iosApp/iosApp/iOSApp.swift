import Shared
import SwiftUI

@main
struct iOSApp: App {
    private let audioPlayerProvider = RhythHausAudioPlayerProvider()
    private let artworkProvider = RhythHausArtworkProvider()

    init() {
        RhythHausAppBootstrapper.configure(
            audioPlayerProvider: audioPlayerProvider,
            artworkProvider: artworkProvider
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
