import Foundation
import Shared
import UniformTypeIdentifiers
import XCTest
@testable import RhythHaus

final class PlaylistBackupDocumentPoliciesTests: XCTestCase {
    func testTypePolicyRequestsVendorMimeAndJsonFallback() {
        let types = PlaylistBackupDocumentTypePolicy.contentTypes()

        XCTAssertEqual(types.first?.preferredMIMEType, PlatformPlaylistBackupDocumentsKt.PlaylistBackupMimeType)
        XCTAssertTrue(types.contains(.json))
    }

    func testBoundedReadAcceptsExactLimitAndRejectsLimitPlusOneAndClosesHandle() throws {
        let limit = Int(PlatformPlaylistBackupDocumentsKt.PlaylistBackupMaxBytes)
        let exactHandle = FakeBoundedReadHandle(data: Data(count: limit))
        let exactScope = FakeSecurityScope()
        let exact = try PlaylistBackupDocumentResourcePolicy.readBounded(
            maxBytes: limit,
            securityScope: exactScope,
            openHandle: { exactHandle }
        )
        XCTAssertEqual(exact.count, limit)
        XCTAssertEqual(exactHandle.requestedCounts, [limit + 1, 1])
        XCTAssertEqual(exactHandle.closeCount, 1)
        XCTAssertEqual(exactScope.startCount, 1)
        XCTAssertEqual(exactScope.stopCount, 1)

        let oversizedHandle = FakeBoundedReadHandle(data: Data(count: limit + 1))
        let oversizedScope = FakeSecurityScope()
        XCTAssertThrowsError(
            try PlaylistBackupDocumentResourcePolicy.readBounded(
                maxBytes: limit,
                securityScope: oversizedScope,
                openHandle: { oversizedHandle }
            )
        ) { error in
            XCTAssertEqual(error as? PlaylistBackupDocumentPolicyError, .tooLarge)
        }
        XCTAssertEqual(oversizedHandle.closeCount, 1)
        XCTAssertEqual(oversizedScope.startCount, 1)
        XCTAssertEqual(oversizedScope.stopCount, 1)
    }

    func testSecurityScopeStopsOnlyWhenStartSucceedsAndHandleClosesOnReadFailure() {
        let scope = FakeSecurityScope(accessGranted: false)
        let handle = FakeBoundedReadHandle(error: TestError.failure)

        XCTAssertThrowsError(
            try PlaylistBackupDocumentResourcePolicy.readBounded(
                maxBytes: 4,
                securityScope: scope,
                openHandle: { handle }
            )
        )
        XCTAssertEqual(scope.startCount, 1)
        XCTAssertEqual(scope.stopCount, 0)
        XCTAssertEqual(handle.closeCount, 1)
    }

    func testBoundedReadRejectsLimitExceededAcrossShortReads() {
        let scope = FakeSecurityScope()
        let handle = ChunkedBoundedReadHandle(chunks: [Data(count: 3), Data(count: 2), Data()])

        XCTAssertThrowsError(
            try PlaylistBackupDocumentResourcePolicy.readBounded(
                maxBytes: 4,
                securityScope: scope,
                openHandle: { handle }
            )
        ) { error in
            XCTAssertEqual(error as? PlaylistBackupDocumentPolicyError, .tooLarge)
        }
        XCTAssertEqual(handle.requestedCounts, [5, 2])
        XCTAssertEqual(handle.closeCount, 1)
        XCTAssertEqual(scope.startCount, 1)
        XCTAssertEqual(scope.stopCount, 1)
    }

    func testOverlapStaleCallbacksAndExactlyOnceCompletion() {
        var firstOutcomes: [PlaylistBackupDocumentPolicyOutcome] = []
        var overlapOutcomes: [PlaylistBackupDocumentPolicyOutcome] = []
        var cleanupCount = 0
        let state = PlaylistBackupDocumentOperationState()
        let first = NSObject()
        let stale = NSObject()

        XCTAssertTrue(state.begin(completion: { firstOutcomes.append($0) }))
        state.attach(picker: first, cleanup: { cleanupCount += 1 })
        XCTAssertFalse(state.begin(completion: { overlapOutcomes.append($0) }))
        XCTAssertEqual(overlapOutcomes, [.overlap])
        XCTAssertFalse(state.finish(picker: stale, outcome: .success))
        XCTAssertTrue(firstOutcomes.isEmpty)
        XCTAssertTrue(state.finish(picker: first, outcome: .cancelled))
        XCTAssertFalse(state.finish(picker: first, outcome: .failure("late")))
        XCTAssertEqual(firstOutcomes, [.cancelled])
        XCTAssertEqual(cleanupCount, 1)
    }

    func testUnavailablePresenterAndAllTerminalOutcomesCleanUuidDirectory() throws {
        for outcome in [
            PlaylistBackupDocumentPolicyOutcome.success,
            .cancelled,
            .failure("failed"),
            .unavailable("presenter")
        ] {
            let storage = FakeTemporaryStorage()
            var outcomes: [PlaylistBackupDocumentPolicyOutcome] = []
            let state = PlaylistBackupDocumentOperationState()
            XCTAssertTrue(state.begin(completion: { outcomes.append($0) }))
            let prepared = try PlaylistBackupDocumentTemporaryExport.prepare(
                fileName: "backup.json",
                data: Data([1]),
                storage: storage,
                uuid: { "fixed-uuid" }
            )
            let picker = NSObject()
            state.attach(picker: picker, cleanup: prepared.cleanup)
            XCTAssertTrue(state.finish(picker: picker, outcome: outcome))
            XCTAssertEqual(storage.removedURLs, [storage.root.appendingPathComponent("fixed-uuid", isDirectory: true)])
            XCTAssertEqual(outcomes, [outcome])
        }
    }

    func testDeinitCleansPreparedUuidDirectoryWithoutCompleting() throws {
        let storage = FakeTemporaryStorage()
        var state: PlaylistBackupDocumentOperationState? = PlaylistBackupDocumentOperationState()
        XCTAssertTrue(state?.begin(completion: { _ in }) == true)
        let prepared = try PlaylistBackupDocumentTemporaryExport.prepare(
            fileName: "backup.json",
            data: Data(),
            storage: storage,
            uuid: { "deinit-uuid" }
        )
        state?.attach(picker: NSObject(), cleanup: prepared.cleanup)

        state = nil

        XCTAssertEqual(storage.removedURLs, [storage.root.appendingPathComponent("deinit-uuid", isDirectory: true)])
    }

    func testTemporaryExportWriteFailureCleansUuidDirectory() {
        let storage = FakeTemporaryStorage(writeError: TestError.failure)

        XCTAssertThrowsError(
            try PlaylistBackupDocumentTemporaryExport.prepare(
                fileName: "backup.json",
                data: Data([1]),
                storage: storage,
                uuid: { "failure-uuid" }
            )
        )

        XCTAssertEqual(storage.removedURLs, [storage.root.appendingPathComponent("failure-uuid", isDirectory: true)])
    }
}

private enum TestError: Error { case failure }

private final class FakeSecurityScope: PlaylistBackupDocumentSecurityScope {
    let accessGranted: Bool
    var startCount = 0
    var stopCount = 0
    init(accessGranted: Bool = true) { self.accessGranted = accessGranted }
    func start() -> Bool { startCount += 1; return accessGranted }
    func stop() { stopCount += 1 }
}

private final class FakeBoundedReadHandle: PlaylistBackupDocumentReadHandle {
    var data: Data?
    let error: Error?
    var requestedCounts: [Int] = []
    var closeCount = 0
    init(data: Data) { self.data = data; error = nil }
    init(error: Error) { data = nil; self.error = error }
    func read(upToCount count: Int) throws -> Data {
        requestedCounts.append(count)
        if let error { throw error }
        defer { data = nil }
        return data ?? Data()
    }
    func close() throws { closeCount += 1 }
}

private final class ChunkedBoundedReadHandle: PlaylistBackupDocumentReadHandle {
    var chunks: [Data]
    var requestedCounts: [Int] = []
    var closeCount = 0
    init(chunks: [Data]) { self.chunks = chunks }
    func read(upToCount count: Int) throws -> Data {
        requestedCounts.append(count)
        return chunks.isEmpty ? Data() : chunks.removeFirst()
    }
    func close() throws { closeCount += 1 }
}

private final class FakeTemporaryStorage: PlaylistBackupDocumentTemporaryStorage {
    let root = URL(fileURLWithPath: "/tmp/rhythhaus-tests", isDirectory: true)
    let writeError: Error?
    var removedURLs: [URL] = []
    init(writeError: Error? = nil) { self.writeError = writeError }
    func temporaryDirectory() -> URL { root }
    func createDirectory(at url: URL) throws { }
    func write(_ data: Data, to url: URL) throws {
        if let writeError { throw writeError }
    }
    func removeItem(at url: URL) throws { removedURLs.append(url) }
}
