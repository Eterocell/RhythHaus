import Foundation
import Shared
import UniformTypeIdentifiers

enum PlaylistBackupDocumentPolicyError: Error, Equatable {
    case tooLarge
}

enum PlaylistBackupDocumentPolicyOutcome: Equatable {
    case success
    case cancelled
    case tooLarge
    case unavailable(String)
    case failure(String)
    case overlap
}

enum PlaylistBackupDocumentTypePolicy {
    static func contentTypes() -> [UTType] {
        [UTType(mimeType: PlatformPlaylistBackupDocumentsKt.PlaylistBackupMimeType), .json].compactMap { $0 }
    }
}

protocol PlaylistBackupDocumentSecurityScope {
    func start() -> Bool
    func stop()
}

protocol PlaylistBackupDocumentReadHandle {
    func read(upToCount count: Int) throws -> Data
    func close() throws
}

enum PlaylistBackupDocumentResourcePolicy {
    static func readBounded(
        maxBytes: Int,
        securityScope: PlaylistBackupDocumentSecurityScope,
        openHandle: () throws -> PlaylistBackupDocumentReadHandle
    ) throws -> Data {
        let accessed = securityScope.start()
        defer { if accessed { securityScope.stop() } }
        let handle = try openHandle()
        defer { try? handle.close() }
        let data = try handle.read(upToCount: maxBytes + 1)
        guard data.count <= maxBytes else { throw PlaylistBackupDocumentPolicyError.tooLarge }
        return data
    }
}

final class PlaylistBackupDocumentOperationState {
    private var picker: AnyObject?
    private var completion: ((PlaylistBackupDocumentPolicyOutcome) -> Void)?
    private var cleanup: (() -> Void)?

    func begin(completion: @escaping (PlaylistBackupDocumentPolicyOutcome) -> Void) -> Bool {
        guard self.completion == nil else {
            completion(.overlap)
            return false
        }
        self.completion = completion
        return true
    }

    func attach(picker: AnyObject, cleanup: (() -> Void)? = nil) {
        self.picker = picker
        self.cleanup = cleanup
    }

    func isCurrent(picker: AnyObject) -> Bool {
        picker === self.picker
    }

    @discardableResult
    func finishCurrent(outcome: PlaylistBackupDocumentPolicyOutcome) -> Bool {
        guard let completion else { return false }
        self.completion = nil
        picker = nil
        let cleanup = self.cleanup
        self.cleanup = nil
        cleanup?()
        completion(outcome)
        return true
    }

    @discardableResult
    func finish(picker: AnyObject, outcome: PlaylistBackupDocumentPolicyOutcome) -> Bool {
        guard isCurrent(picker: picker) else { return false }
        return finishCurrent(outcome: outcome)
    }

    deinit {
        cleanup?()
    }
}

protocol PlaylistBackupDocumentTemporaryStorage {
    func temporaryDirectory() -> URL
    func createDirectory(at url: URL) throws
    func write(_ data: Data, to url: URL) throws
    func removeItem(at url: URL) throws
}

struct PlaylistBackupDocumentPreparedExport {
    let fileURL: URL
    let cleanup: () -> Void
}

enum PlaylistBackupDocumentTemporaryExport {
    static func prepare(
        fileName: String,
        data: Data,
        storage: PlaylistBackupDocumentTemporaryStorage,
        uuid: () -> String = { UUID().uuidString }
    ) throws -> PlaylistBackupDocumentPreparedExport {
        let directory = storage.temporaryDirectory().appendingPathComponent(uuid(), isDirectory: true)
        let fileURL = directory.appendingPathComponent(URL(fileURLWithPath: fileName).lastPathComponent)
        do {
            try storage.createDirectory(at: directory)
            try storage.write(data, to: fileURL)
        } catch {
            try? storage.removeItem(at: directory)
            throw error
        }
        return PlaylistBackupDocumentPreparedExport(
            fileURL: fileURL,
            cleanup: { try? storage.removeItem(at: directory) }
        )
    }
}

struct URLPlaylistBackupDocumentSecurityScope: PlaylistBackupDocumentSecurityScope {
    let url: URL
    func start() -> Bool { url.startAccessingSecurityScopedResource() }
    func stop() { url.stopAccessingSecurityScopedResource() }
}

final class FilePlaylistBackupDocumentReadHandle: PlaylistBackupDocumentReadHandle {
    private let handle: FileHandle
    init(url: URL) throws { handle = try FileHandle(forReadingFrom: url) }
    func read(upToCount count: Int) throws -> Data { try handle.read(upToCount: count) ?? Data() }
    func close() throws { try handle.close() }
}

struct FileManagerPlaylistBackupDocumentTemporaryStorage: PlaylistBackupDocumentTemporaryStorage {
    let fileManager: FileManager
    func temporaryDirectory() -> URL { fileManager.temporaryDirectory }
    func createDirectory(at url: URL) throws {
        try fileManager.createDirectory(at: url, withIntermediateDirectories: true)
    }
    func write(_ data: Data, to url: URL) throws { try data.write(to: url, options: .atomic) }
    func removeItem(at url: URL) throws { try fileManager.removeItem(at: url) }
}
