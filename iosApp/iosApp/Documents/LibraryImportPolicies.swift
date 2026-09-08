import Foundation
import Shared
import UniformTypeIdentifiers

/// Pure filename/content/destination policy for the iOS Files.app library
/// import, mirroring the Kotlin destination policy in
/// `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/IOSLibraryImport.kt`
/// and the supported-extension list in the library feature's
/// `SupportedAudio.kt`. Kept free of UIKit so the rules are unit-testable.
enum LibraryImportSupportedAudio {
    /// Extensions RhythHaus can play and therefore import.
    static let extensions: Set<String> = [
        "wav", "wave", "aif", "aiff", "au", "mp3", "m4a", "aac", "flac", "ogg",
    ]

    /// Kotlin parity: the extension is everything after the last dot, even a
    /// leading dot (`.mp3` reports `mp3`); a missing or empty extension is
    /// unsupported.
    static func isSupportedAudioName(_ name: String) -> Bool {
        guard let separator = name.lastIndex(of: Character(".")) else { return false }
        return extensions.contains(name[name.index(after: separator)...].lowercased())
    }
}

/// Reduces an external source file name to one safe managed file name,
/// mirroring Kotlin's `managedImportFileName` exactly.
enum LibraryImportNamePolicy {
    /// Fallback managed file name when a source name sanitizes to nothing.
    static let fallbackName = "imported-audio"

    static func managedImportFileName(_ sourceFileName: String) -> String {
        let normalized = sourceFileName.replacingOccurrences(of: "\\", with: "/")
        let segments = normalized.split(separator: "/", omittingEmptySubsequences: false)
        let lastSegment =
            (segments.last.map(String.init) ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        switch lastSegment {
        case "", ".", "..":
            return fallbackName
        default:
            return lastSegment
        }
    }

    /// Kotlin parity with `withNumericSuffix`: insert `-index` before the
    /// extension, or append it when the name has no extension or only a
    /// leading dot. The incoming casing is preserved.
    static func suffixingName(_ name: String, index: Int) -> String {
        guard
            let dot = name.lastIndex(of: Character(".")),
            dot != name.startIndex
        else {
            return "\(name)-\(index)"
        }
        return "\(name[..<dot])-\(index)\(name[dot...])"
    }
}

/// One planned managed destination for an imported file, mirroring Kotlin's
/// `ManagedImportDestinationPlan`.
enum LibraryImportDestinationPlan: Equatable {
    /// The destination name is free and the file should be copied.
    case fresh(fileName: String)

    /// Byte-identical content already exists at the destination name; the
    /// reported name is the managed (existing) file identity that matched.
    case duplicate(fileName: String)

    /// The destination name holds different content; a deterministic numeric
    /// suffix was chosen instead, preserving the incoming name's casing.
    case suffixed(fileName: String)
}

/// Case-insensitive view over managed destination files.
protocol LibraryImportManagedFiles {
    /// Returns the stored managed name matching `name` case-insensitively.
    func occupiedName(ciMatching name: String) -> String?

    /// Returns stored bytes for an exact managed file name, or nil when the
    /// file is gone or cannot be read (treated as different content).
    func content(of fileName: String) -> Data?
}

/// In-memory managed files backing pure planning tests and Kotlin-parity
/// checks; never touches disk.
struct LibraryImportMemoryManagedFiles: LibraryImportManagedFiles {
    private let files: [String: Data]

    init(_ files: [String: Data]) {
        self.files = files
    }

    func occupiedName(ciMatching name: String) -> String? {
        files.keys.first { $0.caseInsensitiveCompare(name) == .orderedSame }
    }

    func content(of fileName: String) -> Data? {
        files[fileName]
    }
}

/// Pure destination decision for one imported source file, mirroring Kotlin's
/// `managedImportDestinationPlan`: occupancy and duplicate lookups ignore
/// case (the managed folder lives on APFS), and a different-content occupant
/// selects the smallest deterministic numeric suffix (2, 3, ...) that is free.
enum LibraryImportDestinationPolicy {
    static func planName(sourceFileName: String, managed: LibraryImportManagedFiles) -> String {
        let destinationName = LibraryImportNamePolicy.managedImportFileName(sourceFileName)
        guard managed.occupiedName(ciMatching: destinationName) != nil else { return destinationName }
        var index = 2
        while managed.occupiedName(ciMatching: LibraryImportNamePolicy.suffixingName(destinationName, index: index)) != nil {
            index += 1
        }
        return LibraryImportNamePolicy.suffixingName(destinationName, index: index)
    }

    static func plan(
        sourceFileName: String,
        sourceContent: Data,
        managed: LibraryImportManagedFiles
    ) -> LibraryImportDestinationPlan {
        let destinationName = LibraryImportNamePolicy.managedImportFileName(sourceFileName)
        guard let existing = managed.occupiedName(ciMatching: destinationName) else {
            return .fresh(fileName: destinationName)
        }
        if managed.content(of: existing) == sourceContent {
            return .duplicate(fileName: existing)
        }
        var index = 2
        while true {
            let candidate = LibraryImportNamePolicy.suffixingName(destinationName, index: index)
            if managed.occupiedName(ciMatching: candidate) == nil {
                return .suffixed(fileName: candidate)
            }
            index += 1
        }
    }
}

/// Classification of one enumeration/selection input.
enum LibraryImportEntryKind: Equatable {
    /// A directory that must be enumerated recursively.
    case folder

    /// A regular file with a supported audio extension.
    case audioFile

    /// A regular file that is not supported audio.
    case unsupportedFile

    /// A dot-prefixed entry; skipped silently during import.
    case hidden
}

enum LibraryImportEntryPolicy {
    /// Classifies one picked or enumerated entry. Hidden entries are skipped
    /// entirely (never copied, never counted); folders are descended into.
    static func classify(fileName: String, isDirectory: Bool) -> LibraryImportEntryKind {
        if fileName.hasPrefix(".") {
            return .hidden
        }
        if isDirectory {
            return .folder
        }
        return LibraryImportSupportedAudio.isSupportedAudioName(fileName) ? .audioFile : .unsupportedFile
    }
}

/// One file-level import outcome feeding the aggregate counters.
enum LibraryImportFileOutcome: Equatable {
    case imported
    case duplicate
    case unsupported
    case failed
}

/// Aggregate terminal counters reported through the ABI completion. Every
/// encountered non-hidden regular file lands in exactly one counter, so
/// `imported + duplicates + unsupported + failed` equals the number of
/// regular files the operation processed.
struct LibraryImportCounters: Equatable {
    var imported: Int
    var duplicates: Int
    var unsupported: Int
    var failed: Int

    init(imported: Int = 0, duplicates: Int = 0, unsupported: Int = 0, failed: Int = 0) {
        self.imported = imported
        self.duplicates = duplicates
        self.unsupported = unsupported
        self.failed = failed
    }

    static let zero = LibraryImportCounters()

    mutating func apply(_ outcome: LibraryImportFileOutcome) {
        switch outcome {
        case .imported:
            imported += 1
        case .duplicate:
            duplicates += 1
        case .unsupported:
            unsupported += 1
        case .failed:
            failed += 1
        }
    }
}

/// Terminal operation outcomes of the import provider, mirroring the
/// playlist-backup provider's outcome model.
enum LibraryImportOperationOutcome: Equatable {
    case success
    case cancelled
    case unavailable(String)
    case failure(String)
    case overlap
}

/// Single-flight picker operation state: one active operation at a time, one
/// terminal delivery per operation, stale picker callbacks rejected.
final class LibraryImportOperationState {
    private var picker: AnyObject?
    private var completion: ((LibraryImportOperationOutcome) -> Void)?

    /// Begins an operation. Returns false (and answers `.overlap` on the
    /// supplied completion) when another operation is still active.
    func begin(completion: @escaping (LibraryImportOperationOutcome) -> Void) -> Bool {
        guard self.completion == nil else {
            completion(.overlap)
            return false
        }
        self.completion = completion
        return true
    }

    func attach(picker: AnyObject) {
        self.picker = picker
    }

    func isCurrent(picker: AnyObject) -> Bool {
        picker === self.picker
    }

    @discardableResult
    func finishCurrent(outcome: LibraryImportOperationOutcome) -> Bool {
        guard let completion else { return false }
        self.completion = nil
        picker = nil
        completion(outcome)
        return true
    }

    @discardableResult
    func finish(picker: AnyObject, outcome: LibraryImportOperationOutcome) -> Bool {
        guard isCurrent(picker: picker) else { return false }
        return finishCurrent(outcome: outcome)
    }
}

/// Balanced start/stop around security-scoped resource access.
protocol LibraryImportSecurityScope {
    func start() -> Bool
    func stop()
}

struct URLLibraryImportSecurityScope: LibraryImportSecurityScope {
    let url: URL

    func start() -> Bool {
        url.startAccessingSecurityScopedResource()
    }

    func stop() {
        url.stopAccessingSecurityScopedResource()
    }
}

/// Filesystem operations the import copy uses, injectable so tests run
/// against real temporary directories.
protocol LibraryImportFileOperations {
    func isDirectory(at url: URL) -> Bool
    func contentsOfDirectory(at url: URL) throws -> [String]
    func recursiveFiles(in url: URL) throws -> [URL]
    func streamedFileEntries(in url: URL) throws -> [LibraryImportEnumerationEntry]
    func readData(from url: URL) throws -> Data
    func filesEqual(_ lhs: URL, _ rhs: URL) throws -> Bool
    func streamCopy(from source: URL, to destination: URL) throws
    func writeTemporaryFile(data: Data, in directory: URL) throws -> URL
    func moveItem(at source: URL, to destination: URL) throws
    func removeItem(at url: URL) throws
}

struct FileManagerLibraryImportFileOperations: LibraryImportFileOperations {
    let fileManager: FileManager

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
    }

    func isDirectory(at url: URL) -> Bool {
        var isDirectory: ObjCBool = false
        return fileManager.fileExists(atPath: url.path, isDirectory: &isDirectory)
            && isDirectory.boolValue
    }

    func contentsOfDirectory(at url: URL) throws -> [String] {
        try fileManager.contentsOfDirectory(atPath: url.path).sorted()
    }

    func recursiveFiles(in url: URL) throws -> [URL] {
        var files: [URL] = []
        try collect(directory: url, files: &files)
        return files
    }

    func streamedFileEntries(in url: URL) throws -> [LibraryImportEnumerationEntry] {
        var entries: [LibraryImportEnumerationEntry] = []
        try collectEntries(directory: url, entries: &entries)
        return entries
    }

    private func collectEntries(directory: URL, entries: inout [LibraryImportEnumerationEntry]) throws {
        for name in try contentsOfDirectory(at: directory) {
            if name.hasPrefix(".") { continue }
            let child = directory.appendingPathComponent(name)
            if isDirectory(at: child) {
                do { try collectEntries(directory: child, entries: &entries) }
                catch { entries.append(.failed) }
            } else { entries.append(.file(child)) }
        }
    }

    private func collect(directory: URL, files: inout [URL]) throws {
        for name in try contentsOfDirectory(at: directory) {
            if name.hasPrefix(".") {
                continue
            }
            let child = directory.appendingPathComponent(name)
            if isDirectory(at: child) {
                let isPackage =
                    (try? child.resourceValues(forKeys: [.isPackageKey]))?.isPackage ?? false
                if !isPackage {
                    try collect(directory: child, files: &files)
                }
            } else {
                files.append(child)
            }
        }
    }

    func readData(from url: URL) throws -> Data {
        try Data(contentsOf: url)
    }

    func filesEqual(_ lhs: URL, _ rhs: URL) throws -> Bool {
        let leftSize = try fileManager.attributesOfItem(atPath: lhs.path)[.size] as? NSNumber
        let rightSize = try fileManager.attributesOfItem(atPath: rhs.path)[.size] as? NSNumber
        guard leftSize?.int64Value == rightSize?.int64Value else { return false }
        let left = try FileHandle(forReadingFrom: lhs)
        let right = try FileHandle(forReadingFrom: rhs)
        defer { try? left.close(); try? right.close() }
        while true {
            let a = try left.read(upToCount: 64 * 1024) ?? Data()
            let b = try right.read(upToCount: 64 * 1024) ?? Data()
            if a != b { return false }
            if a.isEmpty { return true }
        }
    }

    func streamCopy(from source: URL, to destination: URL) throws {
        let input = try FileHandle(forReadingFrom: source)
        let output = try FileHandle(forWritingTo: destination)
        defer { try? input.close(); try? output.close() }
        while true {
            let chunk = try input.read(upToCount: 64 * 1024) ?? Data()
            if chunk.isEmpty { return }
            try output.write(contentsOf: chunk)
        }
    }

    /// Writes into a hidden sibling of the final destination so the later
    /// move is a same-volume rename (atomic). Cleans up on write failure.
    func writeTemporaryFile(data: Data, in directory: URL) throws -> URL {
        let url =
            directory.appendingPathComponent(".rhythhaus-import-\(UUID().uuidString).tmp")
        do {
            try data.write(to: url, options: .atomic)
        } catch {
            try? removeItem(at: url)
            throw error
        }
        return url
    }

    func moveItem(at source: URL, to destination: URL) throws {
        try fileManager.moveItem(at: source, to: destination)
    }

    func removeItem(at url: URL) throws {
        try fileManager.removeItem(at: url)
    }
}

enum LibraryImportEnumerationEntry {
    case file(URL)
    case failed
}

/// Disk-backed managed destination state: occupancy is seeded from the
/// destination listing and folded forward as files land, while duplicate
/// byte comparison reads one existing file on demand so whole-library
/// contents are never held in memory.
final class LibraryImportManagedFolderState: LibraryImportManagedFiles {
    private let directory: URL
    private let operations: LibraryImportFileOperations
    private var namesByLowercase: [String: String]

    init(directory: URL, operations: LibraryImportFileOperations) throws {
        self.directory = directory
        self.operations = operations
        var names: [String: String] = [:]
        for name in try operations.contentsOfDirectory(at: directory) where !name.hasPrefix(".") {
            names[name.lowercased()] = name
        }
        namesByLowercase = names
    }

    func occupiedName(ciMatching name: String) -> String? {
        namesByLowercase[name.lowercased()]
    }

    func content(of fileName: String) -> Data? {
        try? operations.readData(from: directory.appendingPathComponent(fileName))
    }

    func contentEquals(fileName: String, sourceURL: URL) -> Bool {
        guard let existing = occupiedName(ciMatching: fileName) else { return false }
        return (try? operations.filesEqual(directory.appendingPathComponent(existing), sourceURL)) == true
    }

    /// Records an installed or observed destination file as occupied.
    func record(fileName: String) {
        namesByLowercase[fileName.lowercased()] = fileName
    }
}

/// Outcome plus aggregate counters of one copy run.
struct LibraryImportRunResult: Equatable {
    let outcome: LibraryImportOperationOutcome
    let counters: LibraryImportCounters
}

/// Executes the managed copy of one picker selection: balanced security
/// scope per selected top-level URL, recursive folder enumeration, supported
/// extension filtering, byte-identical duplicate detection, deterministic
/// numeric collision suffixes, temporary-file writes followed by atomic
/// same-volume moves, and aggregate counting. Runs on the caller's thread.
enum LibraryImportCopyRunner {
    private static let maxCollisionRetries = 4096

    static func run(
        selectedURLs: [URL],
        destinationDirectory: URL,
        operations: LibraryImportFileOperations,
        scopeFor: (URL) -> LibraryImportSecurityScope
    ) -> LibraryImportRunResult {
        guard operations.isDirectory(at: destinationDirectory) else {
            return LibraryImportRunResult(
                outcome: .failure("Import destination folder is unavailable"),
                counters: .zero
            )
        }
        let managed: LibraryImportManagedFolderState
        do {
            managed =
                try LibraryImportManagedFolderState(
                    directory: destinationDirectory,
                    operations: operations
                )
        } catch {
            return LibraryImportRunResult(
                outcome: .failure("Could not read the import destination folder"),
                counters: .zero
            )
        }

        var counters = LibraryImportCounters.zero
        for url in selectedURLs {
            let scope = scopeFor(url)
            let accessed = scope.start()
            defer {
                if accessed {
                    scope.stop()
                }
            }
            if operations.isDirectory(at: url) {
                let entries: [LibraryImportEnumerationEntry]
                do {
                    entries = try operations.streamedFileEntries(in: url)
                } catch {
                    counters.apply(.failed)
                    continue
                }
                for entry in entries {
                    if case .failed = entry {
                        counters.apply(.failed)
                        continue
                    }
                    guard case let .file(file) = entry else { continue }
                    if let outcome = process(
                        file,
                        destinationDirectory: destinationDirectory,
                        operations: operations,
                        managed: managed
                    ) {
                        counters.apply(outcome)
                    }
                }
            } else if let outcome = process(
                url,
                destinationDirectory: destinationDirectory,
                operations: operations,
                managed: managed
            ) {
                counters.apply(outcome)
            }
        }
        return LibraryImportRunResult(outcome: .success, counters: counters)
    }

    /// Classifies one regular-file URL and returns its outcome; returns nil
    /// for entries that are skipped without counting (hidden files).
    private static func process(
        _ url: URL,
        destinationDirectory: URL,
        operations: LibraryImportFileOperations,
        managed: LibraryImportManagedFolderState
    ) -> LibraryImportFileOutcome? {
        switch LibraryImportEntryPolicy.classify(fileName: url.lastPathComponent, isDirectory: false) {
        case .audioFile:
            return copy(
                url,
                destinationDirectory: destinationDirectory,
                operations: operations,
                managed: managed
            )
        case .unsupportedFile:
            return .unsupported
        case .hidden, .folder:
            return nil
        }
    }

    /// Copies one supported audio file through a temporary sibling file and
    /// an atomic move. Re-plans when the destination appears between plan
    /// and move so batch imports stay deterministic and never overwrite.
    private static func copy(
        _ url: URL,
        destinationDirectory: URL,
        operations: LibraryImportFileOperations,
        managed: LibraryImportManagedFolderState
    ) -> LibraryImportFileOutcome {
        let sourceFileName = url.lastPathComponent
        var retries = 0
        while retries < maxCollisionRetries {
            let originalName = LibraryImportNamePolicy.managedImportFileName(sourceFileName)
            if let existing = managed.occupiedName(ciMatching: originalName), managed.contentEquals(fileName: existing, sourceURL: url) {
                return .duplicate
            }
            let fileName = LibraryImportDestinationPolicy.planName(sourceFileName: sourceFileName, managed: managed)
            do {
                let destination = destinationDirectory.appendingPathComponent(fileName)
                let temporary = try operations.writeTemporaryFile(data: Data(), in: destinationDirectory)
                do {
                    try operations.streamCopy(from: url, to: temporary)
                    try operations.moveItem(at: temporary, to: destination)
                    managed.record(fileName: fileName)
                    return .imported
                } catch let cocoaError as NSError
                    where cocoaError.domain == NSCocoaErrorDomain
                        && cocoaError.code == NSFileWriteFileExistsError
                {
                    // A file landed at the planned name after planning; fold
                    // it in as occupied and pick the next suffix.
                    managed.record(fileName: fileName)
                    try? operations.removeItem(at: temporary)
                    retries += 1
                } catch {
                    try? operations.removeItem(at: temporary)
                    return .failed
                }
            } catch { return .failed }
        }
        return .failed
    }
}
