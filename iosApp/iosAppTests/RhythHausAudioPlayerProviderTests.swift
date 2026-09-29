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
            try? FileManager.default.removeItem(at: firstFile)
            try? FileManager.default.removeItem(at: replacementFile)
        }

        provider.setPlaybackGain(gain: 0.25)
        try await load(provider: provider, file: firstFile)
        XCTAssertEqual(
            try XCTUnwrap(provider.debugPlayerVolume),
            0.25,
            accuracy: 0.0001
        )

        provider.setPlaybackGain(gain: 0.6)
        try await load(provider: provider, file: replacementFile)
        XCTAssertEqual(
            try XCTUnwrap(provider.debugPlayerVolume),
            0.6,
            accuracy: 0.0001
        )

        provider.stop()
    }

    private func load(
        provider: RhythHausAudioPlayerProvider,
        file: URL
    ) async throws {
        try await withCheckedThrowingContinuation {
            (continuation: CheckedContinuation<Void, Error>) in
            provider.loadAsync(
                filePath: file.path,
                handler: AudioLoadHandler(
                    loaded: { continuation.resume() },
                    failed: {
                        continuation.resume(
                            throwing: AudioLoadFailure.unavailable)
                    }
                )
            )
        }
    }
}

private final class AudioLoadHandler: NSObject, IOSAudioPlayerLoadHandler {
    private let loaded: () -> Void
    private let failed: () -> Void

    init(loaded: @escaping () -> Void, failed: @escaping () -> Void) {
        self.loaded = loaded
        self.failed = failed
    }

    func onAudioLoaded() {
        loaded()
    }

    func onAudioLoadFailed() {
        failed()
    }
}

private enum AudioLoadFailure: Error {
    case unavailable
}

private func makeSilentWavFile() throws -> URL {
    let sampleRate: UInt32 = 8_000
    let sampleData = Data(repeating: 0, count: Int(sampleRate) * 2)
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

    let url =
        FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("wav")
    try wav.write(to: url, options: .atomic)
    return url
}

private func appendLittleEndian<T: FixedWidthInteger>(
    _ value: T,
    to data: inout Data
) {
    var littleEndian = value.littleEndian
    withUnsafeBytes(of: &littleEndian) { data.append(contentsOf: $0) }
}
