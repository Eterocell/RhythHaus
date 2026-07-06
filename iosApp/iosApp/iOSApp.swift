import AVFAudio
import Foundation
import MediaPlayer
import Shared
import SwiftUI

/// Sets lockscreen / Control Center artwork for the now-playing track.
/// Called from the KMP IOSPlaybackEngine via NowPlayingArtworkBridge.
class RhythHausArtworkProvider: NowPlayingArtworkProvider {
    func setArtwork(
        trackTitle _: String,
        artist _: String,
        album _: String?,
        artworkBytes: KotlinByteArray?
    ) {
        guard let bytes = artworkBytes else {
            removeArtworkFromNowPlaying()
            return
        }
        let count = Int(bytes.size)
        var byteArray = [UInt8](repeating: 0, count: count)
        for i in 0 ..< count {
            byteArray[i] = UInt8(truncatingIfNeeded: bytes.get(index: Int32(i)))
        }
        guard let image = UIImage(data: Data(byteArray)) else {
            removeArtworkFromNowPlaying()
            return
        }
        let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info[MPMediaItemPropertyArtwork] = artwork
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func removeArtworkFromNowPlaying() {
        var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
        info.removeValue(forKey: MPMediaItemPropertyArtwork)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}

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

        // Configure AVAudioSession as a long-form audio source (music app).
        // WWDC23 "Tune up your AirPlay audio experience": set route sharing policy
        // to .longFormAudio alongside MPNowPlayingInfoCenter + MPRemoteCommandCenter.
        // Without .longFormAudio, iOS may not treat the app as a primary Now Playing
        // source, causing prev/next and slider to appear greyed despite registered handlers.
        try? AVAudioSession.sharedInstance().setCategory(
            .playback,
            mode: .default,
            policy: .longFormAudio,
            options: []
        )
        try? AVAudioSession.sharedInstance().setActive(true)

        // Tell iOS this app wants to be a Now Playing app.
        UIApplication.shared.beginReceivingRemoteControlEvents()

        // Register the Swift-native artwork bridge so the KMP playback engine
        // can set lockscreen/Control Center artwork via MPMediaItemArtwork.
        NowPlayingArtworkBridge.shared.provider = RhythHausArtworkProvider()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
