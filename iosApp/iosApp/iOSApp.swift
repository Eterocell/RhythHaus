import SwiftUI
import Foundation

@main
struct iOSApp: App {
    init() {
        ensureMusicFolderExists()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

private func ensureMusicFolderExists() {
    let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
    guard let documentsUrl = urls.first else { return }
    let musicFolder = documentsUrl.appendingPathComponent("RhythHaus Music")
    try? FileManager.default.createDirectory(at: musicFolder, withIntermediateDirectories: true)
}