import AVFAudio
import CryptoKit
import Foundation
import Shared
import XCTest
@testable import RhythHaus

@MainActor
final class RhythHausAudioPlayerProviderTests: XCTestCase {
    func testPlaybackGainAppliesToPreparedReplacementPlayerBeforePlayback() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let firstFile = try makeSilentWavFile()
        let replacementFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: firstFile)
            try? FileManager.default.removeItem(at: replacementFile)
        }

        provider.setPlaybackGain(gain: 0.25)
        try await load(provider: provider, file: firstFile)
        XCTAssertEqual(try XCTUnwrap(provider.debugPlayerVolume), 0.25, accuracy: 0.0001)

        provider.setPlaybackGain(gain: 0.6)
        try await load(provider: provider, file: replacementFile)
        XCTAssertEqual(try XCTUnwrap(provider.debugPlayerVolume), 0.6, accuracy: 0.0001)
    }

    func testTwoRealPlayersAdvanceWithPrePlayGainsAndKeepTheirEnvelopesAcrossPause() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        let outgoingDigest = try digest(outgoingFile)
        let incomingDigest = try digest(incomingFile)
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }

        try await load(provider: provider, file: outgoingFile)
        provider.setPlaybackGain(gain: 0.4)
        try await play(provider: provider)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        try await stage(provider: provider, file: incomingFile)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertEqual(try XCTUnwrap(provider.debugPreparedPlayer).volume, 0, accuracy: 0.0001)
        provider.setCrossfadeGains(outgoingGain: 0.4, incomingGain: 0)
        try await startIncoming(provider: provider)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        XCTAssertTrue(outgoing === provider.debugRetiringPlayer)
        XCTAssertFalse(outgoing === incoming)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertTrue(incoming.isPlaying)
        XCTAssertEqual(incoming.volume, 0, accuracy: 0.0001)
        let outgoingStart = outgoing.currentTime
        let incomingStart = incoming.currentTime
        try await advanceAudio()
        XCTAssertGreaterThan(outgoing.currentTime, outgoingStart + 0.05)
        XCTAssertGreaterThan(incoming.currentTime, incomingStart + 0.05)

        provider.setCrossfadeGains(outgoingGain: 0.2, incomingGain: 0.3)
        provider.pause()
        let outgoingPaused = outgoing.currentTime
        let incomingPaused = incoming.currentTime
        try await advanceAudio()
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertEqual(outgoing.currentTime, outgoingPaused, accuracy: 0.02)
        XCTAssertEqual(incoming.currentTime, incomingPaused, accuracy: 0.02)
        try await play(provider: provider)
        XCTAssertEqual(outgoing.volume, 0.2, accuracy: 0.0001)
        XCTAssertEqual(incoming.volume, 0.3, accuracy: 0.0001)
        try await advanceAudio()
        XCTAssertGreaterThan(outgoing.currentTime, outgoingPaused + 0.05)
        XCTAssertGreaterThan(incoming.currentTime, incomingPaused + 0.05)
        provider.stop()
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertEqual(provider.debugVoiceCount, 0)
        XCTAssertEqual(try digest(outgoingFile), outgoingDigest)
        XCTAssertEqual(try digest(incomingFile), incomingDigest)
    }

    func testRetiringDelegateUsesItsCapturedCompletionInsteadOfIncomingHandler() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile(durationSeconds: 1.2)
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        let incomingCompletion = AudioCompletionRecorder()
        let completed = expectation(description: "The native retiring voice completes its original run")
        outgoingCompletion.onCompleted = { completed.fulfill() }
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let originalDelegate = try XCTUnwrap(outgoing.delegate)
        try await stage(provider: provider, file: incomingFile)
        XCTAssertEqual(outgoingCompletion.completed, 0)
        XCTAssertTrue(outgoing.isPlaying)
        try await startIncoming(provider: provider, completion: incomingCompletion)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)

        await fulfillment(of: [completed], timeout: 3)
        originalDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: true)
        XCTAssertEqual(outgoingCompletion.completed, 1)
        XCTAssertEqual(incomingCompletion.completed, 0)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertTrue(incoming.isPlaying)

        let laterHandler = AudioCompletionRecorder()
        provider.completionHandler = laterHandler
        incoming.delegate?.audioPlayerDidFinishPlaying?(incoming, successfully: false)
        XCTAssertEqual(incomingCompletion.failed, 1)
        XCTAssertEqual(outgoingCompletion.failed, 0)
        XCTAssertEqual(laterHandler.failed, 0)
        XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
        XCTAssertFalse(provider.isPlaying())
        provider.finishCrossfade()
        XCTAssertNil(provider.debugRetiringPlayer)
    }

    func testRejectedNativePlayingRestoresOutgoingPlayerRunGainAndPositionWithoutReplay() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        let incomingCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        provider.setPlaybackGain(gain: 0.45)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoingDelegate = try XCTUnwrap(outgoing.delegate)
        provider.seekTo(positionMillis: 400)
        try await stage(provider: provider, file: incomingFile)
        var rejectedPlayer: AVAudioPlayer?
        var rejectedDelegate: AVAudioPlayerDelegate?
        let rejected = expectation(description: "Controller rejects the native Playing callback")
        provider.startIncomingAsync(
            completionHandler: incomingCompletion,
            handler: AudioStartHandler(
                started: {
                    rejectedPlayer = provider.debugCurrentPlayer
                    rejectedDelegate = rejectedPlayer?.delegate
                    provider.setCrossfadeGains(outgoingGain: 0.1, incomingGain: 0.7)
                    provider.cancelPreparedIncoming()
                    rejected.fulfill()
                },
                failed: { XCTFail("Prepared incoming failed to start"); rejected.fulfill() }
            )
        )
        await fulfillment(of: [rejected], timeout: 5)

        let incoming = try XCTUnwrap(rejectedPlayer)
        XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
        XCTAssertTrue(outgoing.delegate === outgoingDelegate)
        XCTAssertTrue(provider.completionHandler === outgoingCompletion)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertGreaterThanOrEqual(outgoing.currentTime, 0.35)
        XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
        XCTAssertNil(provider.debugPreparedPlayer)
        XCTAssertNil(provider.debugRetiringPlayer)
        XCTAssertEqual(provider.debugVoiceCount, 1)
        rejectedDelegate?.audioPlayerDidFinishPlaying?(incoming, successfully: false)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertEqual(incomingCompletion.failed, 0)
        let restoredPosition = outgoing.currentTime
        try await advanceAudio()
        XCTAssertGreaterThan(outgoing.currentTime, restoredPosition + 0.05)
        outgoingDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: true)
        XCTAssertEqual(outgoingCompletion.completed, 1)
        XCTAssertEqual(incomingCompletion.completed, 0)
    }

    func testQueuedPreviousRunDelegateCannotMutateResumedVoices() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let previousCompletion = AudioCompletionRecorder()
        let resumedCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider, completion: previousCompletion)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoing = try XCTUnwrap(provider.debugRetiringPlayer)
        let previousDelegate = try XCTUnwrap(incoming.delegate)
        provider.setCrossfadeGains(outgoingGain: 0.2, incomingGain: 0.3)
        provider.pause()
        try await play(provider: provider, completion: resumedCompletion)
        let resumedDelegate = try XCTUnwrap(incoming.delegate)
        XCTAssertFalse(previousDelegate === resumedDelegate)

        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            DispatchQueue.global().async {
                previousDelegate.audioPlayerDidFinishPlaying?(incoming, successfully: false)
                DispatchQueue.main.async { continuation.resume() }
            }
        }

        XCTAssertTrue(provider.debugCurrentPlayer === incoming)
        XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertTrue(incoming.isPlaying)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertEqual(incoming.volume, 0.3, accuracy: 0.0001)
        XCTAssertEqual(outgoing.volume, 0.2, accuracy: 0.0001)
        XCTAssertEqual(previousCompletion.failed, 0)
        XCTAssertEqual(resumedCompletion.failed, 0)
        let incomingPosition = incoming.currentTime
        let outgoingPosition = outgoing.currentTime
        try await advanceAudio()
        XCTAssertGreaterThan(incoming.currentTime, incomingPosition + 0.05)
        XCTAssertGreaterThan(outgoing.currentTime, outgoingPosition + 0.05)
        resumedDelegate.audioPlayerDidFinishPlaying?(incoming, successfully: false)
        XCTAssertEqual(resumedCompletion.failed, 1)
        XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
        XCTAssertTrue(outgoing.isPlaying)
        provider.finishCrossfade()
        XCTAssertNil(provider.debugRetiringPlayer)
        XCTAssertFalse(outgoing.isPlaying)
    }

    func testIncomingTerminalEventBeforeControllerSettlementPreservesOutgoingForRollback() async throws {
        for succeeded in [true, false] {
            let provider = RhythHausAudioPlayerProvider()
            let outgoingFile = try makeSilentWavFile()
            let incomingFile = try makeSilentWavFile()
            defer {
                provider.stop()
                try? FileManager.default.removeItem(at: outgoingFile)
                try? FileManager.default.removeItem(at: incomingFile)
            }
            let outgoingCompletion = AudioCompletionRecorder()
            let incomingCompletion = AudioCompletionRecorder()
            try await load(provider: provider, file: outgoingFile)
            provider.setPlaybackGain(gain: 0.45)
            try await play(provider: provider, completion: outgoingCompletion)
            let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
            let outgoingDelegate = try XCTUnwrap(outgoing.delegate)
            try await stage(provider: provider, file: incomingFile)
            try await startIncoming(provider: provider, completion: incomingCompletion)
            let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
            let incomingDelegate = try XCTUnwrap(incoming.delegate)
            provider.setCrossfadeGains(outgoingGain: 0.1, incomingGain: 0.7)

            incomingDelegate.audioPlayerDidFinishPlaying?(incoming, successfully: succeeded)

            XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
            XCTAssertTrue(outgoing.isPlaying)
            XCTAssertFalse(provider.isPlaying())
            XCTAssertEqual(incomingCompletion.completed, succeeded ? 1 : 0)
            XCTAssertEqual(incomingCompletion.failed, succeeded ? 0 : 1)
            provider.cancelPreparedIncoming()
            XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
            XCTAssertTrue(outgoing.delegate === outgoingDelegate)
            XCTAssertTrue(provider.completionHandler === outgoingCompletion)
            XCTAssertTrue(provider.isPlaying())
            XCTAssertTrue(outgoing.isPlaying)
            XCTAssertFalse(incoming.isPlaying)
            XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
            XCTAssertEqual(provider.debugVoiceCount, 1)
        }
    }

    func testPreviousDelegateCannotMutateVoiceWhileItsNewRunIsActivating() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let previousCompletion = AudioCompletionRecorder()
        let resumedCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider, completion: previousCompletion)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoing = try XCTUnwrap(provider.debugRetiringPlayer)
        let previousDelegate = try XCTUnwrap(incoming.delegate)
        provider.pause()
        incoming.currentTime = 0.5
        provider.completionHandler = resumedCompletion
        let resumed = expectation(description: "The new voice run finishes native activation")
        provider.playAsync(
            handler: AudioStartHandler(
                started: { resumed.fulfill() },
                failed: { XCTFail("Paused voice could not resume"); resumed.fulfill() }
            )
        )

        previousDelegate.audioPlayerDidFinishPlaying?(incoming, successfully: false)

        XCTAssertEqual(previousCompletion.failed, 0)
        XCTAssertTrue(provider.debugCurrentPlayer === incoming)
        XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
        XCTAssertEqual(incoming.currentTime, 0.5, accuracy: 0.001)
        await fulfillment(of: [resumed], timeout: 5)
        XCTAssertTrue(incoming.isPlaying)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertGreaterThan(incoming.currentTime, 0.25)
        XCTAssertEqual(resumedCompletion.failed, 0)
    }

    func testExplicitFinishCommitsIncomingAndLaterCancelCannotRestoreOutgoing() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        let incomingCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoingDelegate = try XCTUnwrap(outgoing.delegate)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider, completion: incomingCompletion)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        let incomingDelegate = try XCTUnwrap(incoming.delegate)
        provider.setCrossfadeGains(outgoingGain: 0.2, incomingGain: 0.6)

        provider.finishCrossfade()
        provider.cancelPreparedIncoming()

        XCTAssertTrue(provider.debugCurrentPlayer === incoming)
        XCTAssertTrue(incoming.delegate === incomingDelegate)
        XCTAssertTrue(provider.completionHandler === incomingCompletion)
        XCTAssertTrue(incoming.isPlaying)
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertEqual(incoming.volume, 0.6, accuracy: 0.0001)
        XCTAssertNil(provider.debugRetiringPlayer)
        XCTAssertEqual(provider.debugVoiceCount, 1)
        outgoingDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: false)
        XCTAssertEqual(outgoingCompletion.failed, 0)
        XCTAssertTrue(provider.isPlaying())
    }

    func testInterruptionAndRouteLossPauseBothVoicesUnderOneSessionRegistration() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let interruptions = AudioInterruptionRecorder()
        provider.interruptionHandler = interruptions
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider)
        let current = try XCTUnwrap(provider.debugCurrentPlayer)
        let retiring = try XCTUnwrap(provider.debugRetiringPlayer)
        provider.setCrossfadeGains(outgoingGain: 0.25, incomingGain: 0.35)
        XCTAssertTrue(provider.debugAudioSession === AVAudioSession.sharedInstance())
        XCTAssertEqual(provider.debugNotificationObserverCount, 2)

        NotificationCenter.default.post(
            name: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            userInfo: [AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue]
        )
        XCTAssertEqual(interruptions.events, ["began"])
        XCTAssertFalse(current.isPlaying)
        XCTAssertFalse(retiring.isPlaying)
        XCTAssertEqual(current.volume, 0.35, accuracy: 0.0001)
        XCTAssertEqual(retiring.volume, 0.25, accuracy: 0.0001)
        NotificationCenter.default.post(
            name: AVAudioSession.routeChangeNotification,
            object: AVAudioSession.sharedInstance(),
            userInfo: [AVAudioSessionRouteChangeReasonKey: AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue]
        )
        XCTAssertEqual(interruptions.events, ["began", "route"])
        NotificationCenter.default.post(
            name: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            userInfo: [
                AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.ended.rawValue,
                AVAudioSessionInterruptionOptionKey: AVAudioSession.InterruptionOptions.shouldResume.rawValue,
            ]
        )
        XCTAssertEqual(interruptions.events, ["began", "route", "ended:true"])
        XCTAssertFalse(current.isPlaying)
        XCTAssertFalse(retiring.isPlaying)
        try await play(provider: provider)
        NotificationCenter.default.post(
            name: AVAudioSession.routeChangeNotification,
            object: AVAudioSession.sharedInstance(),
            userInfo: [AVAudioSessionRouteChangeReasonKey: AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue]
        )
        XCTAssertEqual(interruptions.events, ["began", "route", "ended:true", "route"])
        XCTAssertFalse(current.isPlaying)
        XCTAssertFalse(retiring.isPlaying)
        XCTAssertEqual(provider.debugNotificationObserverCount, 2)
    }

    func testFailedAndCancelledPreparationNeverReplaceOrAttenuateOutgoing() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        let invalidFile = incomingFile.appendingPathExtension("invalid")
        try Data([0, 1, 2, 3]).write(to: invalidFile)
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
            try? FileManager.default.removeItem(at: invalidFile)
        }
        try await load(provider: provider, file: outgoingFile)
        provider.setPlaybackGain(gain: 0.45)
        try await play(provider: provider)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        do {
            try await stage(provider: provider, file: invalidFile)
            XCTFail("Invalid media must fail preparation")
        } catch AudioOperationFailure.unavailable {
            // The outgoing voice remains the sole owner after native preparation fails.
        }
        XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
        XCTAssertNil(provider.debugPreparedPlayer)

        let cancelled = expectation(description: "Cancelled native preparation drains its result")
        provider.prepareIncomingAsync(
            filePath: incomingFile.path,
            handler: AudioLoadHandler(
                loaded: { XCTFail("Cancelled preparation installed a player"); cancelled.fulfill() },
                failed: { cancelled.fulfill() }
            )
        )
        provider.cancelPreparedIncoming()
        await fulfillment(of: [cancelled], timeout: 5)
        XCTAssertNil(provider.debugPreparedPlayer)
        XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)

        try await stage(provider: provider, file: incomingFile)
        let outgoingPosition = outgoing.currentTime
        let cancelledStart = expectation(description: "Cancelled activation preserves the outgoing native run")
        provider.startIncomingAsync(
            completionHandler: AudioCompletionRecorder(),
            handler: AudioStartHandler(
                started: { XCTFail("Cancelled incoming voice was promoted"); cancelledStart.fulfill() },
                failed: { cancelledStart.fulfill() }
            )
        )
        provider.cancelPreparedIncoming()
        await fulfillment(of: [cancelledStart], timeout: 5)
        try await advanceAudio()
        XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
        XCTAssertTrue(outgoing.isPlaying)
        XCTAssertGreaterThan(outgoing.currentTime, outgoingPosition + 0.05)
        XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
        XCTAssertNil(provider.debugPreparedPlayer)
        XCTAssertNil(provider.debugRetiringPlayer)
    }

    func testSeekingRetiresOnlyOutgoingAndStopDisposesEveryVoice() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let staleDelegate = try XCTUnwrap(outgoing.delegate)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        provider.setCrossfadeGains(outgoingGain: 0.2, incomingGain: 0.6)
        provider.seekTo(positionMillis: 750)
        XCTAssertNil(provider.debugRetiringPlayer)
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertTrue(provider.debugCurrentPlayer === incoming)
        XCTAssertEqual(incoming.currentTime, 0.75, accuracy: 0.05)
        XCTAssertEqual(incoming.volume, 0.6, accuracy: 0.0001)
        staleDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: true)
        XCTAssertEqual(outgoingCompletion.completed, 0)

        try await stage(provider: provider, file: outgoingFile)
        provider.stop()
        XCTAssertEqual(provider.debugVoiceCount, 0)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertFalse(provider.isPlaying())
        try await play(provider: provider)
        XCTAssertEqual(provider.debugVoiceCount, 1)
        XCTAssertEqual(try XCTUnwrap(provider.debugPlayerVolume), 1, accuracy: 0.0001)
        XCTAssertEqual(provider.debugCurrentPlayer?.url?.path, incomingFile.path)
    }

    func testStopInsideNativePlayingRetainsIncomingMediaForOrdinaryReplay() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        try await stage(provider: provider, file: incomingFile)
        let incoming = try XCTUnwrap(provider.debugPreparedPlayer)
        let stopped = expectation(description: "Ordinary Stop is admitted inside first native Playing")
        provider.startIncomingAsync(
            completionHandler: AudioCompletionRecorder(),
            handler: AudioStartHandler(
                started: { provider.stop(); stopped.fulfill() },
                failed: { XCTFail("Prepared incoming could not start"); stopped.fulfill() }
            )
        )
        await fulfillment(of: [stopped], timeout: 5)

        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertEqual(provider.debugVoiceCount, 0)
        try await play(provider: provider)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertEqual(provider.debugCurrentPlayer?.url?.path, incomingFile.path)
        XCTAssertEqual(try XCTUnwrap(provider.debugPlayerVolume), 1, accuracy: 0.0001)
        XCTAssertEqual(provider.debugVoiceCount, 1)
    }

    func testReplacementDuringRetainedOverlapDisposesOldVoicesAndKeepsReplacementAuthority() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        let replacementFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
            try? FileManager.default.removeItem(at: replacementFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        let incomingCompletion = AudioCompletionRecorder()
        let replacementCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoingDelegate = try XCTUnwrap(outgoing.delegate)
        try await stage(provider: provider, file: incomingFile)
        try await startIncoming(provider: provider, completion: incomingCompletion)
        let incoming = try XCTUnwrap(provider.debugCurrentPlayer)
        let incomingDelegate = try XCTUnwrap(incoming.delegate)
        provider.setCrossfadeGains(outgoingGain: 0.2, incomingGain: 0.6)

        try await load(provider: provider, file: replacementFile)
        try await play(provider: provider, completion: replacementCompletion)
        let replacement = try XCTUnwrap(provider.debugCurrentPlayer)
        outgoingDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: false)
        incomingDelegate.audioPlayerDidFinishPlaying?(incoming, successfully: false)

        XCTAssertTrue(provider.debugCurrentPlayer === replacement)
        XCTAssertEqual(replacement.url?.path, replacementFile.path)
        XCTAssertTrue(replacement.isPlaying)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertFalse(incoming.isPlaying)
        XCTAssertNil(provider.debugPreparedPlayer)
        XCTAssertNil(provider.debugRetiringPlayer)
        XCTAssertEqual(provider.debugVoiceCount, 1)
        XCTAssertEqual(replacement.volume, 0.6, accuracy: 0.0001)
        XCTAssertEqual(outgoingCompletion.failed, 0)
        XCTAssertEqual(incomingCompletion.failed, 0)
        XCTAssertEqual(replacementCompletion.failed, 0)
    }

    func testStoppingPendingIncomingStartCannotPromoteOrResumeAStaleVoice() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider)
        try await stage(provider: provider, file: incomingFile)
        let cancelled = expectation(description: "Stale incoming start reports failure")
        provider.startIncomingAsync(
            completionHandler: AudioCompletionRecorder(),
            handler: AudioStartHandler(
                started: { XCTFail("A stopped source became logical current"); cancelled.fulfill() },
                failed: { cancelled.fulfill() }
            )
        )
        provider.stop()
        await fulfillment(of: [cancelled], timeout: 5)
        XCTAssertEqual(provider.debugVoiceCount, 0)
        XCTAssertFalse(provider.isPlaying())
    }

    func testOutgoingCompletionDuringPendingActivationRetainsIncomingNativeStartOutcome() async throws {
        let provider = RhythHausAudioPlayerProvider()
        let outgoingFile = try makeSilentWavFile()
        let incomingFile = try makeSilentWavFile()
        defer {
            provider.stop()
            try? FileManager.default.removeItem(at: outgoingFile)
            try? FileManager.default.removeItem(at: incomingFile)
        }
        let outgoingCompletion = AudioCompletionRecorder()
        let incomingCompletion = AudioCompletionRecorder()
        try await load(provider: provider, file: outgoingFile)
        try await play(provider: provider, completion: outgoingCompletion)
        let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
        let outgoingDelegate = try XCTUnwrap(outgoing.delegate)
        try await stage(provider: provider, file: incomingFile)
        let incoming = try XCTUnwrap(provider.debugPreparedPlayer)
        let started = expectation(description: "Incoming activation settles after outgoing terminal delivery")
        provider.startIncomingAsync(
            completionHandler: incomingCompletion,
            handler: AudioStartHandler(
                started: { started.fulfill() },
                failed: { XCTFail("Outgoing completion invalidated incoming activation"); started.fulfill() }
            )
        )
        outgoing.stop()
        outgoingDelegate.audioPlayerDidFinishPlaying?(outgoing, successfully: true)
        await fulfillment(of: [started], timeout: 5)

        XCTAssertEqual(outgoingCompletion.completed, 1)
        XCTAssertEqual(incomingCompletion.completed, 0)
        XCTAssertTrue(provider.debugCurrentPlayer === incoming)
        XCTAssertTrue(provider.debugRetiringPlayer === outgoing)
        XCTAssertTrue(provider.isPlaying())
        XCTAssertTrue(incoming.isPlaying)
        XCTAssertFalse(outgoing.isPlaying)
        XCTAssertEqual(incoming.volume, 0, accuracy: 0.0001)
        provider.finishCrossfade()
        XCTAssertEqual(provider.debugVoiceCount, 1)
    }

    func testPendingPauseAndSystemPauseReportIncomingCancellationWithoutPromotingIt() async throws {
        for pauseReason in ["local", "interruption", "route"] {
            let provider = RhythHausAudioPlayerProvider()
            let outgoingFile = try makeSilentWavFile()
            let incomingFile = try makeSilentWavFile()
            defer {
                provider.stop()
                try? FileManager.default.removeItem(at: outgoingFile)
                try? FileManager.default.removeItem(at: incomingFile)
            }
            let interruptions = AudioInterruptionRecorder()
            provider.interruptionHandler = interruptions
            try await load(provider: provider, file: outgoingFile)
            provider.setPlaybackGain(gain: 0.45)
            try await play(provider: provider)
            let outgoing = try XCTUnwrap(provider.debugCurrentPlayer)
            try await stage(provider: provider, file: incomingFile)
            let incoming = try XCTUnwrap(provider.debugPreparedPlayer)
            let cancelled = expectation(description: "\(pauseReason) invalidates pending native activation")
            provider.startIncomingAsync(
                completionHandler: AudioCompletionRecorder(),
                handler: AudioStartHandler(
                    started: { XCTFail("Paused pending incoming was promoted"); cancelled.fulfill() },
                    failed: { cancelled.fulfill() }
                )
            )
            switch pauseReason {
            case "interruption":
                NotificationCenter.default.post(
                    name: AVAudioSession.interruptionNotification,
                    object: AVAudioSession.sharedInstance(),
                    userInfo: [AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue]
                )
            case "route":
                NotificationCenter.default.post(
                    name: AVAudioSession.routeChangeNotification,
                    object: AVAudioSession.sharedInstance(),
                    userInfo: [AVAudioSessionRouteChangeReasonKey: AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue]
                )
            default:
                provider.pause()
            }
            await fulfillment(of: [cancelled], timeout: 5)

            XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
            XCTAssertNil(provider.debugRetiringPlayer)
            XCTAssertFalse(outgoing.isPlaying)
            XCTAssertFalse(incoming.isPlaying)
            XCTAssertFalse(provider.isPlaying())
            XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
            if pauseReason == "interruption" { XCTAssertEqual(interruptions.events, ["began"]) }
            if pauseReason == "route" { XCTAssertEqual(interruptions.events, ["route"]) }
            provider.cancelPreparedIncoming()
            XCTAssertNil(provider.debugPreparedPlayer)
            XCTAssertEqual(provider.debugVoiceCount, 1)
            try await play(provider: provider)
            XCTAssertTrue(outgoing.isPlaying)
            XCTAssertTrue(provider.debugCurrentPlayer === outgoing)
            XCTAssertEqual(outgoing.volume, 0.45, accuracy: 0.0001)
        }
    }

    private func load(provider: RhythHausAudioPlayerProvider, file: URL) async throws {
        try await withCheckedThrowingContinuation {
            (continuation: CheckedContinuation<Void, Error>) in
            provider.loadAsync(filePath: file.path, handler: loadHandler(continuation))
        }
    }

    private func stage(provider: RhythHausAudioPlayerProvider, file: URL) async throws {
        try await withCheckedThrowingContinuation {
            (continuation: CheckedContinuation<Void, Error>) in
            provider.prepareIncomingAsync(filePath: file.path, handler: loadHandler(continuation))
        }
    }

    private func play(
        provider: RhythHausAudioPlayerProvider,
        completion: IOSAudioPlayerCompletionHandler = AudioCompletionRecorder()
    ) async throws {
        provider.completionHandler = completion
        try await withCheckedThrowingContinuation {
            (continuation: CheckedContinuation<Void, Error>) in
            provider.playAsync(handler: startHandler(continuation))
        }
    }

    private func startIncoming(
        provider: RhythHausAudioPlayerProvider,
        completion: IOSAudioPlayerCompletionHandler = AudioCompletionRecorder()
    ) async throws {
        try await withCheckedThrowingContinuation {
            (continuation: CheckedContinuation<Void, Error>) in
            provider.startIncomingAsync(completionHandler: completion, handler: startHandler(continuation))
        }
    }

    private func loadHandler(_ continuation: CheckedContinuation<Void, Error>) -> AudioLoadHandler {
        AudioLoadHandler(
            loaded: { continuation.resume() },
            failed: { continuation.resume(throwing: AudioOperationFailure.unavailable) }
        )
    }

    private func startHandler(_ continuation: CheckedContinuation<Void, Error>) -> AudioStartHandler {
        AudioStartHandler(
            started: { continuation.resume() },
            failed: { continuation.resume(throwing: AudioOperationFailure.unavailable) }
        )
    }

    private func advanceAudio() async throws {
        try await Task.sleep(nanoseconds: 150_000_000)
    }

    private func digest(_ file: URL) throws -> Data {
        Data(SHA256.hash(data: try Data(contentsOf: file)))
    }
}

private final class AudioLoadHandler: NSObject, IOSAudioPlayerLoadHandler {
    private let loaded: () -> Void
    private let failed: () -> Void

    init(loaded: @escaping () -> Void, failed: @escaping () -> Void) {
        self.loaded = loaded
        self.failed = failed
    }

    func onAudioLoaded() { loaded() }
    func onAudioLoadFailed() { failed() }
}

private final class AudioStartHandler: NSObject, IOSAudioPlayerPlaybackStartHandler {
    private let started: () -> Void
    private let failed: () -> Void

    init(started: @escaping () -> Void, failed: @escaping () -> Void) {
        self.started = started
        self.failed = failed
    }

    func onPlaybackStarted() { started() }
    func onPlaybackStartFailed() { failed() }
}

private final class AudioCompletionRecorder: NSObject, IOSAudioPlayerCompletionHandler {
    var completed = 0
    var failed = 0
    var onCompleted: (() -> Void)?

    func onPlaybackCompleted() {
        completed += 1
        onCompleted?()
    }
    func onPlaybackFailed() { failed += 1 }
}

private final class AudioInterruptionRecorder: NSObject, IOSAudioInterruptionHandler {
    var events: [String] = []

    func onInterruptionBegan() { events.append("began") }
    func onInterruptionEnded(shouldResume: Bool) { events.append("ended:\(shouldResume)") }
    func onRouteDisconnected() { events.append("route") }
}

private enum AudioOperationFailure: Error {
    case unavailable
}

private func makeSilentWavFile(durationSeconds: Double = 3) throws -> URL {
    let sampleRate: UInt32 = 8_000
    let sampleData = Data(repeating: 0, count: Int(Double(sampleRate) * durationSeconds) * 2)
    var wav = Data()
    wav.append("RIFF".data(using: .ascii)!)
    appendLittleEndian(UInt32(36 + sampleData.count), to: &wav)
    wav.append("WAVEfmt ".data(using: .ascii)!)
    appendLittleEndian(UInt32(16), to: &wav)
    appendLittleEndian(UInt16(1), to: &wav)
    appendLittleEndian(UInt16(1), to: &wav)
    appendLittleEndian(sampleRate, to: &wav)
    appendLittleEndian(sampleRate * 2, to: &wav)
    appendLittleEndian(UInt16(2), to: &wav)
    appendLittleEndian(UInt16(16), to: &wav)
    wav.append("data".data(using: .ascii)!)
    appendLittleEndian(UInt32(sampleData.count), to: &wav)
    wav.append(sampleData)

    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent(UUID().uuidString)
        .appendingPathExtension("wav")
    try wav.write(to: url, options: .atomic)
    return url
}

private func appendLittleEndian<T: FixedWidthInteger>(_ value: T, to data: inout Data) {
    var littleEndian = value.littleEndian
    withUnsafeBytes(of: &littleEndian) { data.append(contentsOf: $0) }
}
