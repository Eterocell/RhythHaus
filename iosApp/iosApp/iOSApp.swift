import SwiftUI
import Foundation

@main
struct iOSApp: App {
    init() {
        let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentsUrl = urls.first else { return }
        try? FileManager.default.createDirectory(at: documentsUrl, withIntermediateDirectories: true)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}