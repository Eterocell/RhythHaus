import Foundation
import Shared
import UIKit

/// One-time native iOS setup that must happen before the Compose/KMP UI starts.
enum RhythHausAppBootstrapper {
    static func configure(
        audioPlayerProvider: IOSAudioPlayerProvider,
        artworkProvider: NowPlayingArtworkProvider,
        playlistBackupDocumentProvider: IOSPlaylistBackupDocumentProvider
    ) {
        ensureDocumentsContainerIsVisibleInFiles()
        configureRemoteControlEvents()
        registerKotlinBridges(
            audioPlayerProvider: audioPlayerProvider,
            artworkProvider: artworkProvider,
            playlistBackupDocumentProvider: playlistBackupDocumentProvider
        )
    }

    private static func ensureDocumentsContainerIsVisibleInFiles() {
        let urls = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentsUrl = urls.first else { return }
        try? FileManager.default.createDirectory(at: documentsUrl, withIntermediateDirectories: true)

        // Write a visible file so iOS Files app recognizes the documents container.
        let marker = documentsUrl.appendingPathComponent("Put Music Files Here.txt")
        if !FileManager.default.fileExists(atPath: marker.path) {
            try? "Drop your music files (.mp3, .flac, .wav, .m4a) here.\n"
                .write(to: marker, atomically: true, encoding: .utf8)
        }
    }

    private static func configureRemoteControlEvents() {
        // Tell iOS this app wants to be a Now Playing app.
        UIApplication.shared.beginReceivingRemoteControlEvents()
    }

    private static func registerKotlinBridges(
        audioPlayerProvider: IOSAudioPlayerProvider,
        artworkProvider: NowPlayingArtworkProvider,
        playlistBackupDocumentProvider: IOSPlaylistBackupDocumentProvider
    ) {
        // Register Swift-native bridges so the KMP playback engine can use native-only APIs.
        IOSAudioPlayerBridge.shared.provider = audioPlayerProvider
        NowPlayingArtworkBridge.shared.provider = artworkProvider
        IOSPlaylistBackupDocumentBridge.shared.provider = playlistBackupDocumentProvider
    }
}
