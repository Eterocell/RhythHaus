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
    static func contentTypes(for format: String? = nil) -> [UTType] {
        switch format?.lowercased() {
        case ".m3u", "m3u", "audio/x-mpegurl", "audio/mpegurl":
            return interchangeContentTypes(
                fileExtension: "m3u",
                mimeTypes: ["audio/x-mpegurl", "audio/mpegurl"]
            )
        case ".m3u8", "m3u8", "application/vnd.apple.mpegurl":
            return interchangeContentTypes(
                fileExtension: "m3u8",
                mimeTypes: ["application/vnd.apple.mpegurl", "audio/x-mpegurl"]
            )
        case ".pls", "pls", "audio/x-scpls":
            return interchangeContentTypes(
                fileExtension: "pls",
                mimeTypes: ["audio/x-scpls"]
            )
        default:
            return [UTType(mimeType: PlatformPlaylistBackupDocumentsKt.PlaylistBackupMimeType), .json].compactMap { $0 }
        }
    }

    static func contentTypes() -> [UTType] {
        contentTypes(for: nil)
    }

    static func fileName(_ fileName: String, format: String) -> String {
        guard let fileExtension = playlistFileExtension(for: format) else {
            return URL(fileURLWithPath: fileName).lastPathComponent
        }
        let safeName = URL(fileURLWithPath: fileName).lastPathComponent
        guard !safeName.lowercased().hasSuffix(".\(fileExtension)") else {
            return safeName
        }
        return "\(safeName).\(fileExtension)"
    }

    private static func playlistFileExtension(for format: String) -> String? {
        switch format.lowercased() {
        case ".m3u", "m3u": return "m3u"
        case ".m3u8", "m3u8": return "m3u8"
        case ".pls", "pls": return "pls"
        case ".rhythhaus-playlists.json", "rhythhaus-playlists.json":
            return "rhythhaus-playlists.json"
        default: return nil
        }
    }

    private static func interchangeContentTypes(
        fileExtension: String,
        mimeTypes: [String]
    ) -> [UTType] {
        var types: [UTType] = []
        if let extensionType = UTType(filenameExtension: fileExtension) {
            types.append(extensionType)
        }
        for mimeType in mimeTypes {
            if let mimeType = UTType(mimeType: mimeType), !types.contains(mimeType) {
                types.append(mimeType)
            }
        }
        return types
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
        var data = Data()
        while data.count <= maxBytes {
            let chunk = try handle.read(upToCount: maxBytes + 1 - data.count)
            if chunk.isEmpty { return data }
            data.append(chunk)
        }
        throw PlaylistBackupDocumentPolicyError.tooLarge
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
