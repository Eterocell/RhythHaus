import SwiftUI
import Foundation

@main
struct iOSApp: App {
    init() {
        let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentsUrl = urls.first else {
            print("[RhythHaus] ERROR: no documents URL found")
            return
        }
        print("[RhythHaus] Documents path: \(documentsUrl.path)")
        do {
            try FileManager.default.createDirectory(at: documentsUrl, withIntermediateDirectories: true, attributes: nil)
            print("[RhythHaus] Documents directory ensured at: \(documentsUrl.path)")
        } catch {
            print("[RhythHaus] ERROR creating Documents: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}