import SwiftUI
import Shared

@main
struct iOSApp: App {
    @StateObject private var engine = AudioEngine()
    @StateObject private var libraryStore = LibraryStore()

    init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        try? FileManager.default.createDirectory(at: docs, withIntermediateDirectories: true)
        let marker = docs.appendingPathComponent("Put Music Files Here.txt")
        if !FileManager.default.fileExists(atPath: marker.path) {
            try? "Drop your music files (.mp3, .flac, .wav, .m4a) here.\n"
                .write(to: marker, atomically: true, encoding: .utf8)
        }
    }

    var body: some Scene {
        WindowGroup {
            LibraryView(engine: engine, libraryStore: libraryStore)
        }
    }
}
