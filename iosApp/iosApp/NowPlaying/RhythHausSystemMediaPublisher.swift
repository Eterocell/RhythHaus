import Foundation
import MediaPlayer
import Shared
import UIKit

enum RhythHausSystemMediaCommand: CaseIterable, Hashable {
    case play
    case pause
    case next
    case previous
    case seek
}

struct RhythHausSystemMediaTrack {
    let id: String
    let title: String
    let artist: String
    let album: String?
    let durationMillis: Int64?
    let artworkData: Data?
    let artworkIdentity: ObjectIdentifier?

    init(
        id: String,
        title: String,
        artist: String,
        album: String?,
        durationMillis: Int64?,
        artworkData: Data?,
        artworkIdentity: ObjectIdentifier? = nil
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.durationMillis = durationMillis
        self.artworkData = artworkData
        self.artworkIdentity = artworkIdentity
    }
}

struct RhythHausSystemMediaSnapshot {
    let revision: Int64
    let selectionGeneration: Int64
    let currentTrack: RhythHausSystemMediaTrack?
    let positionMillis: Int64
    let durationMillis: Int64?
    let isPlaying: Bool
    let canPlay: Bool
    let canPause: Bool
    let canSkipNext: Bool
    let canSkipPrevious: Bool
    let canSeek: Bool
}

@MainActor
protocol RhythHausSystemMediaSnapshotSource: AnyObject {
    func observeSnapshots(
        _ onSnapshot: @escaping (RhythHausSystemMediaSnapshot) -> Void
    ) -> String
    func removeSnapshotObserver(_ observerID: String)
    func dispatch(
        _ command: RhythHausSystemMediaCommand,
        positionMillis: Int64?,
        snapshot: RhythHausSystemMediaSnapshot
    ) -> Bool
}

@MainActor
protocol RhythHausSystemMediaInfoCenter: AnyObject {
    var nowPlayingInfo: [String: Any]? { get set }
}

@MainActor
protocol RhythHausSystemMediaRemoteCommandCenter: AnyObject {
    func setEnabled(_ isEnabled: Bool, for command: RhythHausSystemMediaCommand)
    func addTarget(
        for command: RhythHausSystemMediaCommand,
        handler: @escaping (Int64?) -> Bool
    ) -> UUID
    func removeTarget(_ token: UUID)
}

/// The sole iOS writer for system Now Playing metadata and remote commands.
@MainActor
final class RhythHausSystemMediaPublisher {
    private static let commands = RhythHausSystemMediaCommand.allCases

    private let source: RhythHausSystemMediaSnapshotSource
    private let infoCenter: RhythHausSystemMediaInfoCenter
    private let remoteCommands: RhythHausSystemMediaRemoteCommandCenter
    private var observerID: String?
    private var targetIDs: [UUID] = []
    private var currentSnapshot: RhythHausSystemMediaSnapshot?
    private var currentInfo: [String: Any]?
    private var artworkForCurrentTrack: MPMediaItemArtwork?
    private var enabledCommands: [RhythHausSystemMediaCommand: Bool] = [:]

    init(
        source: RhythHausSystemMediaSnapshotSource,
        infoCenter: RhythHausSystemMediaInfoCenter,
        remoteCommands: RhythHausSystemMediaRemoteCommandCenter
    ) {
        self.source = source
        self.infoCenter = infoCenter
        self.remoteCommands = remoteCommands
    }

    convenience init(facade: CarPlayFacade) {
        self.init(
            source: SharedCarPlaySystemMediaSource(facade: facade),
            infoCenter: MediaPlayerNowPlayingInfoCenter(),
            remoteCommands: MediaPlayerRemoteCommandCenter())
    }

    func start() {
        guard observerID == nil else { return }
        registerRemoteCommands()
        observerID = source.observeSnapshots { [weak self] snapshot in
            guard let self else { return }
            if Thread.isMainThread {
                self.publish(snapshot)
            } else {
                DispatchQueue.main.async { [weak self] in self?.publish(snapshot) }
            }
        }
    }

    func dispose() {
        if let observerID {
            self.observerID = nil
            source.removeSnapshotObserver(observerID)
        }
        targetIDs.forEach(remoteCommands.removeTarget)
        targetIDs.removeAll()
        Self.commands.forEach { setCommandEnabled(false, for: $0) }
        currentSnapshot = nil
        currentInfo = nil
        artworkForCurrentTrack = nil
        infoCenter.nowPlayingInfo = nil
    }

    private func registerRemoteCommands() {
        guard targetIDs.isEmpty else { return }
        for command in Self.commands {
            setCommandEnabled(false, for: command)
            targetIDs.append(
                remoteCommands.addTarget(for: command) { [weak self] positionMillis in
                    guard let self else { return false }
                    return self.dispatch(command, positionMillis: positionMillis)
                })
        }
    }

    private func publish(_ snapshot: RhythHausSystemMediaSnapshot) {
        let previousTrack = currentSnapshot?.currentTrack
        let nextTrack = snapshot.currentTrack
        if previousTrack?.id != nextTrack?.id ||
            previousTrack?.artworkIdentity != nextTrack?.artworkIdentity {
            artworkForCurrentTrack = Self.makeArtwork(from: nextTrack?.artworkData)
        }
        currentSnapshot = snapshot
        currentInfo = Self.completeInfo(for: snapshot, artwork: artworkForCurrentTrack)
        infoCenter.nowPlayingInfo = currentInfo
        setCommandEnabled(snapshot.canPlay, for: .play)
        setCommandEnabled(snapshot.canPause, for: .pause)
        setCommandEnabled(snapshot.canSkipNext, for: .next)
        setCommandEnabled(snapshot.canSkipPrevious, for: .previous)
        setCommandEnabled(snapshot.canSeek, for: .seek)
    }

    private func dispatch(
        _ command: RhythHausSystemMediaCommand,
        positionMillis: Int64?
    ) -> Bool {
        guard let snapshot = currentSnapshot,
              snapshot.currentTrack != nil,
              isAvailable(command, in: snapshot) else { return false }
        if command == .seek, positionMillis == nil { return false }
        return source.dispatch(
            command,
            positionMillis: positionMillis,
            snapshot: snapshot)
    }

    private func isAvailable(
        _ command: RhythHausSystemMediaCommand,
        in snapshot: RhythHausSystemMediaSnapshot
    ) -> Bool {
        switch command {
        case .play: snapshot.canPlay
        case .pause: snapshot.canPause
        case .next: snapshot.canSkipNext
        case .previous: snapshot.canSkipPrevious
        case .seek: snapshot.canSeek
        }
    }

    private func setCommandEnabled(
        _ isEnabled: Bool,
        for command: RhythHausSystemMediaCommand
    ) {
        guard enabledCommands[command] != isEnabled else { return }
        enabledCommands[command] = isEnabled
        remoteCommands.setEnabled(isEnabled, for: command)
    }

    private static func completeInfo(
        for snapshot: RhythHausSystemMediaSnapshot,
        artwork: MPMediaItemArtwork?
    ) -> [String: Any]? {
        guard let track = snapshot.currentTrack else { return nil }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime:
                Double(max(0, snapshot.positionMillis)) / 1_000.0,
            MPNowPlayingInfoPropertyPlaybackRate: snapshot.isPlaying ? 1.0 : 0.0,
        ]
        if let album = track.album {
            info[MPMediaItemPropertyAlbumTitle] = album
        }
        if let durationMillis = snapshot.durationMillis ?? track.durationMillis {
            info[MPMediaItemPropertyPlaybackDuration] = Double(durationMillis) / 1_000.0
        } else {
            info[MPNowPlayingInfoPropertyIsLiveStream] = true
        }
        if let artwork {
            info[MPMediaItemPropertyArtwork] = artwork
        }
        return info
    }

    private static func makeArtwork(from bytes: Data?) -> MPMediaItemArtwork? {
        guard let bytes,
              let image = UIImage(data: bytes) else { return nil }
        return MPMediaItemArtwork(boundsSize: image.size) { _ in image }
    }

}

@MainActor
private final class SharedCarPlaySystemMediaSource: RhythHausSystemMediaSnapshotSource {
    private let facade: CarPlayFacade
    private var observerID: String?
    private var observerGeneration = 0
    private var lastProjectedTrack: CarPlayTrack?
    private var lastArtworkData: Data?

    init(facade: CarPlayFacade) {
        self.facade = facade
    }

    func observeSnapshots(
        _ onSnapshot: @escaping (RhythHausSystemMediaSnapshot) -> Void
    ) -> String {
        observerGeneration += 1
        let generation = observerGeneration
        let observerID = facade.observeSnapshots { [weak self] snapshot in
            DispatchQueue.main.async { [weak self] in
                guard let self,
                      self.observerGeneration == generation,
                      self.observerID != nil else { return }
                onSnapshot(self.project(snapshot))
            }
        }
        self.observerID = observerID
        return observerID
    }

    func removeSnapshotObserver(_ observerID: String) {
        guard self.observerID == observerID else { return }
        self.observerID = nil
        observerGeneration += 1
        facade.removeSnapshotObserver(observerId: observerID)
        lastProjectedTrack = nil
        lastArtworkData = nil
    }

    func dispatch(
        _ command: RhythHausSystemMediaCommand,
        positionMillis: Int64?,
        snapshot: RhythHausSystemMediaSnapshot
    ) -> Bool {
        guard let track = snapshot.currentTrack else { return false }
        var result: CarPlayCommandResult = .unavailable
        switch command {
        case .play:
            facade.play(
                expectedRevision: snapshot.revision,
                expectedTrackId: track.id,
                expectedSelectionGeneration: snapshot.selectionGeneration,
                onResult: { result = $0 })
        case .pause:
            facade.pause(
                expectedRevision: snapshot.revision,
                expectedTrackId: track.id,
                expectedSelectionGeneration: snapshot.selectionGeneration,
                onResult: { result = $0 })
        case .next:
            facade.skipToNext(
                expectedRevision: snapshot.revision,
                expectedTrackId: track.id,
                expectedSelectionGeneration: snapshot.selectionGeneration,
                onResult: { result = $0 })
        case .previous:
            facade.skipToPrevious(
                expectedRevision: snapshot.revision,
                expectedTrackId: track.id,
                expectedSelectionGeneration: snapshot.selectionGeneration,
                onResult: { result = $0 })
        case .seek:
            guard let positionMillis else { return false }
            facade.seekTo(
                positionMillis: positionMillis,
                expectedRevision: snapshot.revision,
                expectedTrackId: track.id,
                expectedSelectionGeneration: snapshot.selectionGeneration,
                onResult: { result = $0 })
        }
        return result == .applied
    }

    private func project(_ snapshot: CarPlaySnapshot) -> RhythHausSystemMediaSnapshot {
        let playback = snapshot.playback
        let sourceTrack = playback.currentTrack
        let track: RhythHausSystemMediaTrack?
        if let sourceTrack {
            let artworkData: Data?
            if sourceTrack === lastProjectedTrack {
                artworkData = lastArtworkData
            } else {
                artworkData = Self.data(from: sourceTrack.artworkBytes)
                lastProjectedTrack = sourceTrack
                lastArtworkData = artworkData
            }
            track = RhythHausSystemMediaTrack(
                id: sourceTrack.id,
                title: sourceTrack.title,
                artist: sourceTrack.artist,
                album: sourceTrack.album,
                durationMillis: sourceTrack.durationMillis?.int64Value,
                artworkData: artworkData,
                artworkIdentity: ObjectIdentifier(sourceTrack))
        } else {
            lastProjectedTrack = nil
            lastArtworkData = nil
            track = nil
        }
        return RhythHausSystemMediaSnapshot(
            revision: snapshot.revision,
            selectionGeneration: playback.selectionGeneration,
            currentTrack: track,
            positionMillis: playback.positionMillis,
            durationMillis: playback.durationMillis?.int64Value,
            isPlaying: playback.status == .playing,
            canPlay: playback.canPlay,
            canPause: playback.canPause,
            canSkipNext: playback.canSkipNext,
            canSkipPrevious: playback.canSkipPrevious,
            canSeek: playback.canSeek)
    }

    private static func data(from bytes: KotlinByteArray?) -> Data? {
        guard let bytes, bytes.size > 0 else { return nil }
        var data = Data()
        data.reserveCapacity(Int(bytes.size))
        for index in 0 ..< bytes.size {
            data.append(UInt8(bitPattern: bytes.get(index: index)))
        }
        return data
    }
}

@MainActor
private final class MediaPlayerNowPlayingInfoCenter: RhythHausSystemMediaInfoCenter {
    private let center = MPNowPlayingInfoCenter.default()

    var nowPlayingInfo: [String: Any]? {
        get { center.nowPlayingInfo }
        set { center.nowPlayingInfo = newValue }
    }
}

@MainActor
private final class MediaPlayerRemoteCommandCenter: RhythHausSystemMediaRemoteCommandCenter {
    private let center = MPRemoteCommandCenter.shared()
    private var targets: [UUID: (MPRemoteCommand, Any)] = [:]

    init() {
        center.togglePlayPauseCommand.isEnabled = false
        center.stopCommand.isEnabled = false
        center.skipForwardCommand.isEnabled = false
        center.skipBackwardCommand.isEnabled = false
        center.seekForwardCommand.isEnabled = false
        center.seekBackwardCommand.isEnabled = false
    }

    func setEnabled(_ isEnabled: Bool, for command: RhythHausSystemMediaCommand) {
        remoteCommand(for: command).isEnabled = isEnabled
    }

    func addTarget(
        for command: RhythHausSystemMediaCommand,
        handler: @escaping (Int64?) -> Bool
    ) -> UUID {
        let remoteCommand = remoteCommand(for: command)
        let target = remoteCommand.addTarget { event in
            let positionMillis: Int64?
            if command == .seek {
                guard let event = event as? MPChangePlaybackPositionCommandEvent else {
                    return .commandFailed
                }
                let scaledPosition = event.positionTime * 1_000.0
                guard scaledPosition.isFinite,
                      scaledPosition >= 0,
                      scaledPosition < Double(Int64.max) else { return .commandFailed }
                positionMillis = Int64(scaledPosition.rounded())
            } else {
                positionMillis = nil
            }
            let accepted: Bool
            if Thread.isMainThread {
                accepted = handler(positionMillis)
            } else {
                accepted = DispatchQueue.main.sync {
                    handler(positionMillis)
                }
            }
            return accepted ? .success : .commandFailed
        }
        let token = UUID()
        targets[token] = (remoteCommand, target)
        return token
    }

    func removeTarget(_ token: UUID) {
        guard let (command, target) = targets.removeValue(forKey: token) else { return }
        command.removeTarget(target)
    }

    private func remoteCommand(for command: RhythHausSystemMediaCommand) -> MPRemoteCommand {
        switch command {
        case .play: center.playCommand
        case .pause: center.pauseCommand
        case .next: center.nextTrackCommand
        case .previous: center.previousTrackCommand
        case .seek: center.changePlaybackPositionCommand
        }
    }
}
