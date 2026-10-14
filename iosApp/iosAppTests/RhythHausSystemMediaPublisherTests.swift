import MediaPlayer
import Shared
import XCTest
@testable import RhythHaus
import UIKit

@MainActor
final class RhythHausSystemMediaPublisherTests: XCTestCase {
    func testPublishesCompleteCurrentTrackMetadataAndTimeline() throws {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()

        fixture.source.send(
            snapshot(
                revision: 12,
                track: track(
                    id: "track-current",
                    title: "Night Drive",
                    artist: "Rhyth Haus",
                    album: "Local Sessions",
                    durationMillis: 181_000),
                positionMillis: 42_500,
                isPlaying: true,
                canPlay: false,
                canPause: true,
                canSkipNext: true,
                canSkipPrevious: false,
                canSeek: true))

        let info = try XCTUnwrap(fixture.infoCenter.nowPlayingInfo)
        XCTAssertEqual(info[MPMediaItemPropertyTitle] as? String, "Night Drive")
        XCTAssertEqual(info[MPMediaItemPropertyArtist] as? String, "Rhyth Haus")
        XCTAssertEqual(info[MPMediaItemPropertyAlbumTitle] as? String, "Local Sessions")
        XCTAssertEqual(info[MPMediaItemPropertyPlaybackDuration] as? Double, 181.0)
        XCTAssertEqual(info[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 42.5)
        XCTAssertEqual(info[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 1.0)
    }

    func testNewTrackRemovesMetadataFieldsAbsentFromItsProjection() throws {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()

        fixture.source.send(
            snapshot(
                revision: 1,
                track: track(
                    id: "track-with-album",
                    title: "First",
                    artist: "Artist",
                    album: "Old Album",
                    durationMillis: 91_000),
                positionMillis: 8_000,
                isPlaying: false,
                canPlay: true,
                canPause: false,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: true))
        fixture.source.send(
            snapshot(
                revision: 2,
                track: track(
                    id: "track-without-album",
                    title: "Second",
                    artist: "Another Artist",
                    album: nil,
                    durationMillis: nil),
                positionMillis: 0,
                isPlaying: false,
                canPlay: true,
                canPause: false,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: false,
                durationMillis: nil))

        let info = try XCTUnwrap(fixture.infoCenter.nowPlayingInfo)
        XCTAssertEqual(info[MPMediaItemPropertyTitle] as? String, "Second")
        XCTAssertNil(info[MPMediaItemPropertyAlbumTitle])
        XCTAssertNil(info[MPMediaItemPropertyPlaybackDuration])
    }

    func testUnknownDurationPublishesLiveStreamTimelineMarker() throws {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()
        fixture.source.send(
            snapshot(
                revision: 3,
                track: track(
                    id: "track-unknown-duration",
                    title: "Unmeasured",
                    artist: "Artist",
                    album: nil,
                    durationMillis: nil),
                positionMillis: 0,
                isPlaying: false,
                canPlay: false,
                canPause: false,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: false,
                durationMillis: nil))

        let info = try XCTUnwrap(fixture.infoCenter.nowPlayingInfo)
        XCTAssertNil(info[MPMediaItemPropertyPlaybackDuration])
        XCTAssertEqual(info[MPNowPlayingInfoPropertyIsLiveStream] as? Bool, true)
    }

    func testArtworkMergeChangesOnlyTheArtworkField() {
        let artwork = NSObject()
        let info: [String: Any] = [
            MPMediaItemPropertyTitle: "Current",
            MPMediaItemPropertyArtist: "Artist",
            MPNowPlayingInfoPropertyElapsedPlaybackTime: 19.25,
            MPNowPlayingInfoPropertyPlaybackRate: 1.0,
        ]

        let merged = RhythHausSystemMediaPublisher.mergingArtwork(artwork, into: info)
        XCTAssertTrue(merged[MPMediaItemPropertyArtwork] as AnyObject === artwork)
        XCTAssertEqual(merged[MPMediaItemPropertyTitle] as? String, "Current")
        XCTAssertEqual(merged[MPMediaItemPropertyArtist] as? String, "Artist")
        XCTAssertEqual(merged[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 19.25)
        XCTAssertEqual(merged[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 1.0)

        let withoutArtwork = RhythHausSystemMediaPublisher.mergingArtwork(nil, into: merged)
        XCTAssertNil(withoutArtwork[MPMediaItemPropertyArtwork])
        XCTAssertEqual(withoutArtwork[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 19.25)
    }

    func testArtworkCallbackPreservesThePublishedMetadataAndTimeline() throws {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()
        fixture.source.send(
            snapshot(
                revision: 10,
                track: track(
                    id: "artwork-track",
                    title: "Artwork Track",
                    artist: "Rhyth Haus",
                    album: "Now Playing",
                    durationMillis: 120_000),
                positionMillis: 19_250,
                isPlaying: true,
                canPlay: false,
                canPause: true,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: true))

        let image =
            UIGraphicsImageRenderer(size: CGSize(width: 1, height: 1)).image { context in
                context.cgContext.setFillColor(UIColor.red.cgColor)
                context.cgContext.fill(CGRect(x: 0, y: 0, width: 1, height: 1))
            }
        publisher.updateArtwork(
            kotlinByteArray(try XCTUnwrap(image.pngData())),
            trackTitle: "Artwork Track",
            artist: "Rhyth Haus",
            album: "Now Playing")

        let info = try XCTUnwrap(fixture.infoCenter.nowPlayingInfo)
        XCTAssertEqual(info[MPMediaItemPropertyTitle] as? String, "Artwork Track")
        XCTAssertEqual(info[MPMediaItemPropertyArtist] as? String, "Rhyth Haus")
        XCTAssertEqual(info[MPMediaItemPropertyAlbumTitle] as? String, "Now Playing")
        XCTAssertEqual(info[MPMediaItemPropertyPlaybackDuration] as? Double, 120.0)
        XCTAssertEqual(info[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double, 19.25)
        XCTAssertEqual(info[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 1.0)
        XCTAssertNotNil(info[MPMediaItemPropertyArtwork] as? MPMediaItemArtwork)

        publisher.updateArtwork(
            nil,
            trackTitle: "Artwork Track",
            artist: "Rhyth Haus",
            album: "Now Playing")
        let infoWithoutArtwork = try XCTUnwrap(fixture.infoCenter.nowPlayingInfo)
        XCTAssertNil(infoWithoutArtwork[MPMediaItemPropertyArtwork])
        XCTAssertEqual(
            infoWithoutArtwork[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double,
            19.25)
    }

    func testUnavailableCommandFailsWithoutDispatchingAndCapabilitiesFollowSnapshot() {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()
        fixture.source.send(
            snapshot(
                revision: 4,
                track: track(
                    id: "track-one",
                    title: "One",
                    artist: "Artist",
                    album: nil,
                    durationMillis: 60_000),
                positionMillis: 10_000,
                isPlaying: false,
                canPlay: true,
                canPause: false,
                canSkipNext: false,
                canSkipPrevious: false,
                canSeek: false))

        XCTAssertTrue(fixture.remoteCommands.isEnabled(.play))
        XCTAssertFalse(fixture.remoteCommands.isEnabled(.pause))
        XCTAssertFalse(fixture.remoteCommands.isEnabled(.next))
        XCTAssertFalse(fixture.remoteCommands.isEnabled(.previous))
        XCTAssertFalse(fixture.remoteCommands.isEnabled(.seek))
        XCTAssertFalse(fixture.remoteCommands.invoke(.pause))
        XCTAssertFalse(fixture.remoteCommands.invoke(.next))
        XCTAssertFalse(fixture.remoteCommands.invoke(.previous))
        XCTAssertFalse(fixture.remoteCommands.invoke(.seek, positionMillis: 15_000))
        XCTAssertTrue(fixture.source.commands.isEmpty)

        XCTAssertTrue(fixture.remoteCommands.invoke(.play))
        XCTAssertEqual(fixture.source.commands.map(\.command), [.play])
        XCTAssertEqual(fixture.source.commands.first?.revision, 4)
        XCTAssertEqual(fixture.source.commands.first?.trackID, "track-one")
    }

    func testSupportedCommandsDispatchAgainstTheCurrentFacadeSnapshot() {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()
        publisher.start()
        fixture.source.send(
            snapshot(
                revision: 14,
                track: track(
                    id: "track-two",
                    title: "Two",
                    artist: "Artist",
                    album: nil,
                    durationMillis: 90_000),
                positionMillis: 20_000,
                isPlaying: false,
                canPlay: true,
                canPause: false,
                canSkipNext: true,
                canSkipPrevious: true,
                canSeek: true))

        XCTAssertTrue(fixture.remoteCommands.invoke(.play))
        XCTAssertTrue(fixture.remoteCommands.invoke(.next))
        XCTAssertTrue(fixture.remoteCommands.invoke(.previous))
        XCTAssertTrue(fixture.remoteCommands.invoke(.seek, positionMillis: 35_500))

        fixture.source.send(
            snapshot(
                revision: 15,
                track: track(
                    id: "track-two",
                    title: "Two",
                    artist: "Artist",
                    album: nil,
                    durationMillis: 90_000),
                positionMillis: 35_500,
                isPlaying: true,
                canPlay: false,
                canPause: true,
                canSkipNext: true,
                canSkipPrevious: true,
                canSeek: true))
        XCTAssertTrue(fixture.remoteCommands.invoke(.pause))
        XCTAssertTrue(fixture.remoteCommands.invoke(.next))
        XCTAssertTrue(fixture.remoteCommands.invoke(.previous))
        XCTAssertTrue(fixture.remoteCommands.invoke(.seek, positionMillis: 40_000))

        XCTAssertEqual(
            fixture.source.commands.map(\.command),
            [.play, .next, .previous, .seek, .pause, .next, .previous, .seek])
        XCTAssertEqual(
            fixture.source.commands.map(\.revision),
            [14, 14, 14, 14, 15, 15, 15, 15])
        XCTAssertTrue(fixture.source.commands.allSatisfy { $0.trackID == "track-two" })
        XCTAssertEqual(
            fixture.source.commands.compactMap(\.positionMillis),
            [35_500, 40_000])
    }

    func testStartAndDisposeAreIdempotentForObserverAndCommandTargets() {
        let fixture = Fixture()
        let publisher = fixture.makePublisher()

        publisher.start()
        publisher.start()
        XCTAssertEqual(fixture.source.observerCount, 1)
        XCTAssertEqual(fixture.remoteCommands.targetCount, 5)

        publisher.dispose()
        publisher.dispose()
        XCTAssertEqual(fixture.source.observerCount, 0)
        XCTAssertEqual(fixture.remoteCommands.targetCount, 0)
        XCTAssertNil(fixture.infoCenter.nowPlayingInfo)
    }

    private func snapshot(
        revision: Int64,
        track: RhythHausSystemMediaTrack?,
        positionMillis: Int64,
        isPlaying: Bool,
        canPlay: Bool,
        canPause: Bool,
        canSkipNext: Bool,
        canSkipPrevious: Bool,
        canSeek: Bool,
        durationMillis: Int64? = nil
    ) -> RhythHausSystemMediaSnapshot {
        RhythHausSystemMediaSnapshot(
            revision: revision,
            currentTrack: track,
            positionMillis: positionMillis,
            durationMillis: durationMillis ?? track?.durationMillis,
            isPlaying: isPlaying,
            canPlay: canPlay,
            canPause: canPause,
            canSkipNext: canSkipNext,
            canSkipPrevious: canSkipPrevious,
            canSeek: canSeek)
    }

    private func track(
        id: String,
        title: String,
        artist: String,
        album: String?,
        durationMillis: Int64?
    ) -> RhythHausSystemMediaTrack {
        RhythHausSystemMediaTrack(
            id: id,
            title: title,
            artist: artist,
            album: album,
            durationMillis: durationMillis,
            artworkData: nil)
    }
}

private func kotlinByteArray(_ data: Data) -> KotlinByteArray {
    let bytes = KotlinByteArray(size: Int32(data.count))
    for index in 0 ..< data.count {
        bytes.set(index: Int32(index), value: Int8(bitPattern: data[index]))
    }
    return bytes
}

@MainActor
private struct Fixture {
    let source = FakeSystemMediaSource()
    let infoCenter = FakeSystemMediaInfoCenter()
    let remoteCommands = FakeSystemMediaRemoteCommandCenter()

    func makePublisher() -> RhythHausSystemMediaPublisher {
        RhythHausSystemMediaPublisher(
            source: source,
            infoCenter: infoCenter,
            remoteCommands: remoteCommands)
    }
}

@MainActor
private final class FakeSystemMediaSource: RhythHausSystemMediaSnapshotSource {
    private var observer: ((RhythHausSystemMediaSnapshot) -> Void)?
    private(set) var commands: [DispatchedCommand] = []

    var observerCount: Int { observer == nil ? 0 : 1 }

    func observeSnapshots(
        _ onSnapshot: @escaping (RhythHausSystemMediaSnapshot) -> Void
    ) -> String {
        observer = onSnapshot
        return "observer"
    }

    func removeSnapshotObserver(_ observerID: String) {
        if observerID == "observer" { observer = nil }
    }

    func send(_ snapshot: RhythHausSystemMediaSnapshot) {
        observer?(snapshot)
    }

    func dispatch(
        _ command: RhythHausSystemMediaCommand,
        positionMillis: Int64?,
        snapshot: RhythHausSystemMediaSnapshot
    ) {
        commands.append(
            DispatchedCommand(
                command: command,
                revision: snapshot.revision,
                trackID: snapshot.currentTrack?.id,
                positionMillis: positionMillis))
    }

    struct DispatchedCommand {
        let command: RhythHausSystemMediaCommand
        let revision: Int64
        let trackID: String?
        let positionMillis: Int64?
    }
}

@MainActor
private final class FakeSystemMediaInfoCenter: RhythHausSystemMediaInfoCenter {
    var nowPlayingInfo: [String: Any]?
}

@MainActor
private final class FakeSystemMediaRemoteCommandCenter: RhythHausSystemMediaRemoteCommandCenter {
    private var handlers: [RhythHausSystemMediaCommand: [UUID: (Int64?) -> Bool]] = [:]
    private var enabled: [RhythHausSystemMediaCommand: Bool] = [:]

    var targetCount: Int { handlers.values.reduce(0) { $0 + $1.count } }

    func setEnabled(_ isEnabled: Bool, for command: RhythHausSystemMediaCommand) {
        enabled[command] = isEnabled
    }

    func isEnabled(_ command: RhythHausSystemMediaCommand) -> Bool {
        enabled[command] ?? false
    }

    func addTarget(
        for command: RhythHausSystemMediaCommand,
        handler: @escaping (Int64?) -> Bool
    ) -> UUID {
        let token = UUID()
        handlers[command, default: [:]][token] = handler
        return token
    }

    func removeTarget(_ token: UUID) {
        for command in handlers.keys {
            handlers[command]?.removeValue(forKey: token)
        }
    }

    func invoke(_ command: RhythHausSystemMediaCommand, positionMillis: Int64? = nil) -> Bool {
        handlers[command]?.values.first?(positionMillis) ?? false
    }
}
