import SwiftUI
import Foundation

@main
struct iOSApp: App {
    init() {
        let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentsUrl = urls.first else { return }
        try? FileManager.default.createDirectory(at: documentsUrl, withIntermediateDirectories: true)
        // Write a visible file so iOS Files app recognizes the documents container
        let marker = documentsUrl.appendingPathComponent("Put Music Files Here.txt")
        if !FileManager.default.fileExists(atPath: marker.path) {
            try? "Drop your music files (.mp3, .flac, .wav, .m4a) here.\n"
                .write(to: marker, atomically: true, encoding: .utf8)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
