import Foundation
import Shared
import UIKit
import UniformTypeIdentifiers
import XCTest
@testable import RhythHaus

// MARK: - Pure policy tests

final class LibraryImportPoliciesTests: XCTestCase {
    func testMarkerPolicyUpdatesOnlyKnownLegacyContent() {
        XCTAssertTrue(LibraryImportMarkerPolicy.shouldReplace(content: LibraryImportMarkerPolicy.legacyContent))
        XCTAssertFalse(LibraryImportMarkerPolicy.shouldReplace(content: "User's own note\n"))
    }

    func testMarkerCurrentContentIsLocaleAwareAndKeepsExactLegacyMigration() {
        let english = LibraryImportMarkerPolicy.currentContent(localeIdentifier: "en")
        let chinese = LibraryImportMarkerPolicy.currentContent(localeIdentifier: "zh-Hans")
        XCTAssertTrue(english.contains("Files.app"))
        XCTAssertTrue(chinese.contains("导入"))
        XCTAssertFalse(LibraryImportMarkerPolicy.shouldReplace(content: english))
        XCTAssertFalse(LibraryImportMarkerPolicy.shouldReplace(content: chinese))
    }

    // Supported audio extension filtering (Kotlin `SupportedAudio.kt` parity).
    func testSupportedAudioExtensionsAreCaseInsensitive() {
        for name in ["song.mp3", "Song.FLAC", "TRACK.M4A", "mix.ogg", "clip.wav", "a.wave", "b.aif", "c.aiff", "d.au", "e.aac"] {
            XCTAssertTrue(LibraryImportSupportedAudio.isSupportedAudioName(name), "\(name) should be supported")
        }
        for name in ["cover.jpg", "notes.txt", "song", "song.", "playlist.m3u"] {
            XCTAssertFalse(LibraryImportSupportedAudio.isSupportedAudioName(name), "\(name) should be unsupported")
        }
        // Kotlin parity: a leading-dot name still has an extension segment.
        XCTAssertTrue(LibraryImportSupportedAudio.isSupportedAudioName(".mp3"))
    }

    // Filename sanitization (Kotlin `managedImportFileName` parity).
    func testManagedImportFileNameKeepsOnlyLastSegment() {
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("Track 01.mp3"), "Track 01.mp3")
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("folder/sub/song.flac"), "song.flac")
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("folder\\sub\\song.mp3"), "song.mp3")
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("  padded  .mp3"), "padded  .mp3")
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("Live Session (2024).flac"), "Live Session (2024).flac")
        XCTAssertEqual(LibraryImportNamePolicy.managedImportFileName("song"), "song")
    }

    func testManagedImportFileNameFallsBackForTraversalAndBlankNames() {
        for name in ["", "/", "\\", ".", "..", "   ", "a/..", "../..", "a//", "./"] {
            XCTAssertEqual(
                LibraryImportNamePolicy.managedImportFileName(name),
                LibraryImportNamePolicy.fallbackName,
                "\(name.debugDescription) should fall back"
            )
        }
    }

    // Deterministic numeric collision suffixes (Kotlin `withNumericSuffix` parity).
    func testSuffixInsertedBeforeExtensionPreservingCasing() {
        XCTAssertEqual(LibraryImportNamePolicy.suffixingName("Song.mp3", index: 2), "Song-2.mp3")
        XCTAssertEqual(LibraryImportNamePolicy.suffixingName("live.set.mp3", index: 4), "live.set-4.mp3")
        XCTAssertEqual(LibraryImportNamePolicy.suffixingName("Song.MP3", index: 2), "Song-2.MP3")
        XCTAssertEqual(LibraryImportNamePolicy.suffixingName("song", index: 2), "song-2")
        // Kotlin parity: a leading dot is not a split point for suffixing.
        XCTAssertEqual(LibraryImportNamePolicy.suffixingName(".mp3", index: 2), ".mp3-2")
    }

    // Same-content duplicate detection and collision planning.
    func testFreshDestinationWhenNameIsFree() {
        let managed = LibraryImportMemoryManagedFiles(["other.mp3": Data("other".utf8)])
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "song.mp3",
                sourceContent: Data("bytes".utf8),
                managed: managed
            ),
            .fresh(fileName: "song.mp3")
        )
    }

    func testByteIdenticalCaseInsensitiveNameIsDuplicateReportingManagedIdentity() {
        let managed = LibraryImportMemoryManagedFiles(["Song.mp3": Data("same".utf8)])
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "song.mp3",
                sourceContent: Data("same".utf8),
                managed: managed
            ),
            .duplicate(fileName: "Song.mp3")
        )
    }

    func testByteIdenticalOccupiedSuffixIsDuplicate() {
        let incoming = Data("incoming".utf8)
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "song.mp3",
                sourceContent: incoming,
                managed: LibraryImportMemoryManagedFiles([
                    "song.mp3": Data("different".utf8),
                    "song-2.mp3": incoming,
                ])
            ),
            .duplicate(fileName: "song-2.mp3")
        )
    }

    func testCaseDifferingDifferentContentSelectsSuffixedIncomingCasing() {
        let managed = LibraryImportMemoryManagedFiles(["song.mp3": Data("old".utf8)])
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "Song.mp3",
                sourceContent: Data("new".utf8),
                managed: managed
            ),
            .suffixed(fileName: "Song-2.mp3")
        )
    }

    func testOccupiedSuffixSkippedCaseInsensitively() {
        let managed = LibraryImportMemoryManagedFiles([
            "song.mp3": Data("old".utf8),
            "SONG-2.mp3": Data("also-old".utf8),
        ])
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "Song.mp3",
                sourceContent: Data("new".utf8),
                managed: managed
            ),
            .suffixed(fileName: "Song-3.mp3")
        )
    }

    func testByteIdenticalContentUnderDifferentNameStaysFresh() {
        // Duplicate safety is destination-identity-scoped, exactly like Kotlin.
        let managed = LibraryImportMemoryManagedFiles(["song.mp3": Data("same".utf8)])
        XCTAssertEqual(
            LibraryImportDestinationPolicy.plan(
                sourceFileName: "other.mp3",
                sourceContent: Data("same".utf8),
                managed: managed
            ),
            .fresh(fileName: "other.mp3")
        )
    }

    func testBatchPlanFoldMirrorsKotlinFoldingRules() {
        var files: [String: Data] = [:]
        func plan(_ name: String, _ content: Data) -> LibraryImportDestinationPlan {
            let result = LibraryImportDestinationPolicy.plan(
                sourceFileName: name,
                sourceContent: content,
                managed: LibraryImportMemoryManagedFiles(files)
            )
            // Fold each plan back like the copy runner does between files.
            switch result {
            case let .fresh(fileName), let .suffixed(fileName):
                files[fileName] = content
            case .duplicate:
                break
            }
            return result
        }
        let first = Data("a".utf8)
        XCTAssertEqual(plan("a.mp3", first), .fresh(fileName: "a.mp3"))
        XCTAssertEqual(plan("a.mp3", first), .duplicate(fileName: "a.mp3"))
        let changed = Data("b1".utf8)
        XCTAssertEqual(plan("b.mp3", changed), .fresh(fileName: "b.mp3"))
        let changedAgain = Data("b2".utf8)
        XCTAssertEqual(plan("b.mp3", changedAgain), .suffixed(fileName: "b-2.mp3"))
        // Kotlin parity: an occupied numeric suffix holding byte-identical
        // content is reported as a duplicate of that exact managed file, so a
        // later identical import of b.mp3 recognizes b-2.mp3 and copies
        // nothing.
        XCTAssertEqual(plan("b.mp3", changedAgain), .duplicate(fileName: "b-2.mp3"))
    }

    // Nested-directory enumeration input classification.
    func testEntryClassificationForNestedEnumerationInputs() {
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: "Album", isDirectory: true),
            .folder
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: ".backup", isDirectory: true),
            .hidden
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: ".DS_Store", isDirectory: false),
            .hidden
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: "track.mp3", isDirectory: false),
            .audioFile
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: "live.flac", isDirectory: false),
            .audioFile
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: "cover.jpg", isDirectory: false),
            .unsupportedFile
        )
        XCTAssertEqual(
            LibraryImportEntryPolicy.classify(fileName: "notes", isDirectory: false),
            .unsupportedFile
        )
    }

    // Aggregate count behavior.
    func testCounterAggregationAcrossOutcomes() {
        var counters = LibraryImportCounters.zero
        for outcome: LibraryImportFileOutcome in [
            .imported, .imported, .duplicate, .unsupported, .failed, .failed,
        ] {
            counters.apply(outcome)
        }
        XCTAssertEqual(
            counters,
            LibraryImportCounters(imported: 2, duplicates: 1, unsupported: 1, failed: 2)
        )
    }

    func testManagedDirectoryContainmentExcludesOnlyManagedRootAndDescendants() {
        let managed = URL(fileURLWithPath: "/Documents/RhythHaus", isDirectory: true)

        XCTAssertTrue(
            LibraryImportPathPolicy.isAlreadyManaged(
                URL(fileURLWithPath: "/Documents/RhythHaus"),
                managedDirectory: managed
            )
        )
        XCTAssertTrue(
            LibraryImportPathPolicy.isAlreadyManaged(
                URL(fileURLWithPath: "/Documents/RhythHaus/Album/track.mp3"),
                managedDirectory: managed
            )
        )
        XCTAssertFalse(
            LibraryImportPathPolicy.isAlreadyManaged(
                URL(fileURLWithPath: "/Documents/RhythHaus Archive/track.mp3"),
                managedDirectory: managed
            )
        )
    }
}

// MARK: - Provider operation-state and lifecycle tests

final class LibraryImportProviderTests: XCTestCase {
    private var temporaryRoot: URL!

    override func setUpWithError() throws {
        try super.setUpWithError()
        temporaryRoot = FileManager.default.temporaryDirectory
            .appendingPathComponent("LibraryImportTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(
            at: temporaryRoot,
            withIntermediateDirectories: true
        )
    }

    override func tearDownWithError() throws {
        if let temporaryRoot {
            try? FileManager.default.removeItem(at: temporaryRoot)
        }
        temporaryRoot = nil
        try super.tearDownWithError()
    }

    private func operations() -> LibraryImportFileOperations {
        FileManagerLibraryImportFileOperations(fileManager: .default)
    }

    private func makeManagedFolder(named name: String = "RhythHaus Music") throws -> URL {
        let url = temporaryRoot.appendingPathComponent(name, isDirectory: true)
        try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    @discardableResult
    private func write(_ data: Data, to url: URL) throws -> URL {
        try data.write(to: url, options: .atomic)
        return url
    }

    private func seedFileTree(in root: URL) throws -> URL {
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let albumA = root.appendingPathComponent("Album A", isDirectory: true)
        try FileManager.default.createDirectory(at: albumA, withIntermediateDirectories: true)
        try write(Data("track one bytes".utf8), to: albumA.appendingPathComponent("track1.mp3"))
        try write(Data("flac bytes".utf8), to: albumA.appendingPathComponent("track2.flac"))
        try write(Data("cover".utf8), to: albumA.appendingPathComponent("cover.jpg"))
        let nested = albumA.appendingPathComponent("Sub", isDirectory: true)
        try FileManager.default.createDirectory(at: nested, withIntermediateDirectories: true)
        try write(Data("nested m4a bytes".utf8), to: nested.appendingPathComponent("track3.m4a"))
        try write(Data("notes".utf8), to: root.appendingPathComponent("notes.txt"))
        try write(Data("hidden".utf8), to: root.appendingPathComponent(".hidden.mp3"))
        return root
    }

    private func capturedPicker(provider: RhythHausLibraryImportProvider, presenter: PresenterCaptureViewController) -> UIDocumentPickerViewController {
        presenter.presented as! UIDocumentPickerViewController
    }

    // MARK: Overlap / stale / exactly-once operation state (pure)

    func testOperationStateOverlapStaleAndExactlyOnceCompletion() {
        var firstOutcomes: [LibraryImportOperationOutcome] = []
        var overlapOutcomes: [LibraryImportOperationOutcome] = []
        let state = LibraryImportOperationState()
        let first = NSObject()
        let stale = NSObject()

        XCTAssertTrue(state.begin(completion: { firstOutcomes.append($0) }))
        state.attach(picker: first)
        XCTAssertFalse(state.begin(completion: { overlapOutcomes.append($0) }))
        XCTAssertEqual(overlapOutcomes, [.overlap])
        XCTAssertFalse(state.finish(picker: stale, outcome: .success))
        XCTAssertTrue(firstOutcomes.isEmpty)
        XCTAssertTrue(state.finish(picker: first, outcome: .cancelled))
        XCTAssertFalse(state.finish(picker: first, outcome: .failure("late")))
        XCTAssertEqual(firstOutcomes, [.cancelled])
    }

    // MARK: Picker lifecycle through the provider boundary

    func testCancellationDeliversCancelledExactlyOnce() {
        let recorder = CompletionRecorder()
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        let destination = try! makeManagedFolder()
        provider.importAudio(destinationPath: destination.path, completion: recorder)
        let picker = capturedPicker(provider: provider, presenter: presenter)

        provider.documentPickerWasCancelled(picker)

        XCTAssertEqual(recorder.calls.count, 1)
        XCTAssertEqual(recorder.calls.first?.status, IOSLibraryImportStatus.shared.CANCELLED)
        XCTAssertEqual(recorder.calls.first?.imported, 0)
        XCTAssertEqual(recorder.calls.first?.message, nil)
    }

    func testUnavailablePresenterDeliversUnavailableExactlyOnce() {
        let recorder = CompletionRecorder()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { nil }
        )
        let destination = try! makeManagedFolder()

        provider.importAudio(destinationPath: destination.path, completion: recorder)

        XCTAssertEqual(recorder.calls.count, 1)
        XCTAssertEqual(recorder.calls.first?.status, IOSLibraryImportStatus.shared.UNAVAILABLE)
    }

    func testOverlappingLaunchRejectedWhileFirstOperationActive() {
        let firstRecorder = CompletionRecorder()
        let secondRecorder = CompletionRecorder()
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        let destination = try! makeManagedFolder()
        provider.importAudio(destinationPath: destination.path, completion: firstRecorder)
        let firstPicker = capturedPicker(provider: provider, presenter: presenter)

        // A second launch while the first picker is up must answer OVERLAP
        // exactly once and must not disturb the first operation.
        provider.importAudio(destinationPath: destination.path, completion: secondRecorder)
        XCTAssertEqual(secondRecorder.calls.count, 1)
        XCTAssertEqual(secondRecorder.calls.first?.status, IOSLibraryImportStatus.shared.OVERLAP)
        XCTAssertTrue(firstRecorder.calls.isEmpty)

        provider.documentPickerWasCancelled(firstPicker)
        XCTAssertEqual(firstRecorder.calls.count, 1)
        XCTAssertEqual(firstRecorder.calls.first?.status, IOSLibraryImportStatus.shared.CANCELLED)
        XCTAssertEqual(secondRecorder.calls.count, 1)
    }

    func testStalePickerDeliveriesAreIgnored() {
        let recorder = CompletionRecorder()
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        let destination = try! makeManagedFolder()
        provider.importAudio(destinationPath: destination.path, completion: recorder)
        let picker = capturedPicker(provider: provider, presenter: presenter)
        provider.documentPickerWasCancelled(picker)
        XCTAssertEqual(recorder.calls.count, 1)

        // Late deliveries from the finished picker (or an unknown picker)
        // must be ignored: no second completion may cross the boundary.
        provider.documentPicker(picker, didPickDocumentsAt: [temporaryRoot])
        provider.documentPickerWasCancelled(picker)
        XCTAssertEqual(recorder.calls.count, 1)

        let foreignPicker = UIDocumentPickerViewController(forOpeningContentTypes: [.folder], asCopy: false)
        provider.documentPicker(foreignPicker, didPickDocumentsAt: [temporaryRoot])
        provider.documentPickerWasCancelled(foreignPicker)
        XCTAssertEqual(recorder.calls.count, 1)
    }

    // MARK: Managed copy behavior (real file system in temp directory)

    func testNestedFolderImportFlattensSupportedAudioAndCountsUnsupported() throws {
        let source = try seedFileTree(in: temporaryRoot.appendingPathComponent("Source", isDirectory: true))
        let destination = try makeManagedFolder()
        let recorder = CompletionRecorder()
        let expectation = completionExpectation(for: recorder)
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        provider.importAudio(destinationPath: destination.path, completion: recorder)
        let picker = capturedPicker(provider: provider, presenter: presenter)

        provider.documentPicker(picker, didPickDocumentsAt: [source])
        wait(for: [expectation], timeout: 10)

        let call = try XCTUnwrap(recorder.calls.first)
        XCTAssertEqual(call.status, IOSLibraryImportStatus.shared.SUCCESS)
        XCTAssertEqual(call.imported, 3)
        XCTAssertEqual(call.duplicates, 0)
        XCTAssertEqual(call.unsupported, 2)
        XCTAssertEqual(call.failed, 0)
        // Nested audio files land flattened in the managed folder.
        for name in ["track1.mp3", "track2.flac", "track3.m4a"] {
            XCTAssertTrue(FileManager.default.fileExists(atPath: destination.appendingPathComponent(name).path), name)
        }
        // No directory structure and no hidden/temp files are copied over.
        let remaining = try FileManager.default.contentsOfDirectory(atPath: destination.path)
        XCTAssertEqual(Set(remaining), Set(["track1.mp3", "track2.flac", "track3.m4a"]))
    }

    func testNestedFolderImportDoesNotFollowSymbolicLinkDirectory() throws {
        let source = temporaryRoot.appendingPathComponent("SourceWithLink", isDirectory: true)
        let outside = temporaryRoot.appendingPathComponent("Outside", isDirectory: true)
        let destination = try makeManagedFolder()
        try FileManager.default.createDirectory(at: source, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: outside, withIntermediateDirectories: true)
        try write(Data("outside bytes".utf8), to: outside.appendingPathComponent("escaped.mp3"))
        let link = source.appendingPathComponent("LinkedAlbum", isDirectory: true)
        do {
            try FileManager.default.createSymbolicLink(at: link, withDestinationURL: outside)
        } catch {
            throw XCTSkip("Simulator filesystem does not support symbolic links: \(error)")
        }

        let result = LibraryImportCopyRunner.run(
            selectedURLs: [source],
            destinationDirectory: destination,
            operations: operations(),
            scopeFor: { _ in CountingScope(granted: true) }
        )

        XCTAssertEqual(result.outcome, .success)
        XCTAssertEqual(result.counters.imported, 0)
        XCTAssertFalse(FileManager.default.fileExists(atPath: destination.appendingPathComponent("escaped.mp3").path))
    }

    func testSelectedSymbolicLinkDirectoryRootIsNotEnumerated() throws {
        // The picker can hand back a symbolic link to a directory as a
        // top-level selection. isDirectory follows the link, so without the
        // guard the import would enumerate and copy content that lives
        // outside the selected subtree.
        let outside = temporaryRoot.appendingPathComponent("OutsideRoot", isDirectory: true)
        let destination = try makeManagedFolder()
        try FileManager.default.createDirectory(at: outside, withIntermediateDirectories: true)
        try write(Data("outside bytes".utf8), to: outside.appendingPathComponent("escaped.mp3"))
        let link = temporaryRoot.appendingPathComponent("LinkedRoot", isDirectory: true)
        do {
            try FileManager.default.createSymbolicLink(at: link, withDestinationURL: outside)
        } catch {
            throw XCTSkip("Simulator filesystem does not support symbolic links: \(error)")
        }

        let result = LibraryImportCopyRunner.run(
            selectedURLs: [link],
            destinationDirectory: destination,
            operations: operations(),
            scopeFor: { _ in CountingScope(granted: true) }
        )

        XCTAssertEqual(result.outcome, .success)
        XCTAssertEqual(result.counters, .zero)
        XCTAssertFalse(FileManager.default.fileExists(atPath: destination.appendingPathComponent("escaped.mp3").path))
    }

    func testByteIdenticalReimportCountsDuplicates() throws {
        let source = try seedFileTree(in: temporaryRoot.appendingPathComponent("Source", isDirectory: true))
        let destination = try makeManagedFolder()
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )

        let first = CompletionRecorder()
        let firstExpectation = completionExpectation(for: first)
        provider.importAudio(destinationPath: destination.path, completion: first)
        let firstPicker = capturedPicker(provider: provider, presenter: presenter)
        provider.documentPicker(firstPicker, didPickDocumentsAt: [source])
        wait(for: [firstExpectation], timeout: 10)
        XCTAssertEqual(first.calls.first?.imported, 3)

        let second = CompletionRecorder()
        let secondExpectation = completionExpectation(for: second)
        provider.importAudio(destinationPath: destination.path, completion: second)
        let secondPicker = capturedPicker(provider: provider, presenter: presenter)
        provider.documentPicker(secondPicker, didPickDocumentsAt: [source])
        wait(for: [secondExpectation], timeout: 10)

        let call = try XCTUnwrap(second.calls.first)
        XCTAssertEqual(call.status, IOSLibraryImportStatus.shared.SUCCESS)
        XCTAssertEqual(call.imported, 0)
        XCTAssertEqual(call.duplicates, 3)
        XCTAssertEqual(call.unsupported, 2)
        XCTAssertEqual(call.failed, 0)
    }

    func testCaseInsensitiveCollisionWithDifferentContentSuffixesIncomingName() throws {
        let destination = try makeManagedFolder()
        try write(Data("existing".utf8), to: destination.appendingPathComponent("Song.mp3"))
        try write(Data("existing-2".utf8), to: destination.appendingPathComponent("SONG-2.mp3"))
        let source = temporaryRoot.appendingPathComponent("Source", isDirectory: true)
        try FileManager.default.createDirectory(at: source, withIntermediateDirectories: true)
        try write(Data("incoming".utf8), to: source.appendingPathComponent("song.mp3"))

        let recorder = CompletionRecorder()
        let expectation = completionExpectation(for: recorder)
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        provider.importAudio(destinationPath: destination.path, completion: recorder)
        let picker = capturedPicker(provider: provider, presenter: presenter)
        provider.documentPicker(picker, didPickDocumentsAt: [source])
        wait(for: [expectation], timeout: 10)

        let call = try XCTUnwrap(recorder.calls.first)
        XCTAssertEqual(call.status, IOSLibraryImportStatus.shared.SUCCESS)
        XCTAssertEqual(call.imported, 1)
        XCTAssertEqual(call.failed, 0)
        // Existing bytes are untouched; the incoming file takes the first
        // free suffix preserving its own casing.
        XCTAssertEqual(
            try Data(contentsOf: destination.appendingPathComponent("Song.mp3")),
            Data("existing".utf8)
        )
        XCTAssertEqual(
            try Data(contentsOf: destination.appendingPathComponent("song-3.mp3")),
            Data("incoming".utf8)
        )
    }

    func testCopyFailureIsAggregatedAndLeavesNoTemporaryFiles() throws {
        let destination = try makeManagedFolder()
        let source = temporaryRoot.appendingPathComponent("Source", isDirectory: true)
        try FileManager.default.createDirectory(at: source, withIntermediateDirectories: true)
        try write(Data("good".utf8), to: source.appendingPathComponent("good.mp3"))
        let brokenURL = try write(Data("broken".utf8), to: source.appendingPathComponent("broken.flac"))
        try FileManager.default.setAttributes([.posixPermissions: 0o000], ofItemAtPath: brokenURL.path)

        let recorder = CompletionRecorder()
        let expectation = completionExpectation(for: recorder)
        let presenter = PresenterCaptureViewController()
        let provider = RhythHausLibraryImportProvider(
            operations: operations(),
            presenterProvider: { presenter }
        )
        provider.importAudio(destinationPath: destination.path, completion: recorder)
        let picker = capturedPicker(provider: provider, presenter: presenter)
        provider.documentPicker(picker, didPickDocumentsAt: [source])
        wait(for: [expectation], timeout: 10)

        try FileManager.default.setAttributes([.posixPermissions: 0o644], ofItemAtPath: brokenURL.path)
        let call = try XCTUnwrap(recorder.calls.first)
        XCTAssertEqual(call.status, IOSLibraryImportStatus.shared.SUCCESS)
        XCTAssertEqual(call.imported, 1)
        XCTAssertEqual(call.failed, 1)
        XCTAssertEqual(call.duplicates, 0)
        XCTAssertEqual(call.unsupported, 0)
        let remaining = try FileManager.default.contentsOfDirectory(atPath: destination.path)
        XCTAssertEqual(remaining, ["good.mp3"], "no partial or temporary files may remain")
    }

    // MARK: Copy runner boundary behavior

    func testCopyRunnerBalancesSecurityScopePerSelectedURL() throws {
        let destination = try makeManagedFolder()
        let folder = temporaryRoot.appendingPathComponent("Folder", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        try write(Data("x".utf8), to: folder.appendingPathComponent("a.mp3"))
        let file = try write(Data("y".utf8), to: temporaryRoot.appendingPathComponent("b.mp3"))
        var scopes: [CountingScope] = []
        let scopeFor: (URL) -> LibraryImportSecurityScope = { url in
            let scope = CountingScope(granted: true)
            scopes.append(scope)
            return scope
        }

        let result = LibraryImportCopyRunner.run(
            selectedURLs: [folder, file],
            destinationDirectory: destination,
            operations: operations(),
            scopeFor: scopeFor
        )

        XCTAssertEqual(result.outcome, .success)
        XCTAssertEqual(result.counters, LibraryImportCounters(imported: 2, duplicates: 0, unsupported: 0, failed: 0))
        XCTAssertEqual(scopes.count, 2, "one scope per selected top-level URL")
        for scope in scopes {
            XCTAssertEqual(scope.startCount, 1)
            XCTAssertEqual(scope.stopCount, 1)
        }
    }

    func testCopyRunnerDeniedScopeStillBalancesAndReadsLocalFile() throws {
        let destination = try makeManagedFolder()
        let file = try write(Data("local".utf8), to: temporaryRoot.appendingPathComponent("a.mp3"))
        let scope = CountingScope(granted: false)
        let result = LibraryImportCopyRunner.run(
            selectedURLs: [file],
            destinationDirectory: destination,
            operations: operations(),
            scopeFor: { _ in scope }
        )
        XCTAssertEqual(result.outcome, .success)
        XCTAssertEqual(result.counters.imported, 1)
        // start() returned false: stop() must not be called (balanced).
        XCTAssertEqual(scope.startCount, 1)
        XCTAssertEqual(scope.stopCount, 0)
    }

    func testCopyRunnerSelectsManagedDirectoryWithoutCopyingItsFiles() throws {
        let destination = try makeManagedFolder()
        let managedTrack = try write(
            Data("already managed".utf8),
            to: destination.appendingPathComponent("track.mp3")
        )
        let scope = CountingScope(granted: true)

        let result = LibraryImportCopyRunner.run(
            selectedURLs: [destination, managedTrack],
            destinationDirectory: destination,
            operations: operations(),
            scopeFor: { _ in scope }
        )

        XCTAssertEqual(result.outcome, .alreadyManaged)
        XCTAssertEqual(result.counters, .zero)
        XCTAssertEqual(scope.startCount, 0, "managed selections need no security scope")
        XCTAssertEqual(scope.stopCount, 0)
        XCTAssertEqual(
            try Data(contentsOf: managedTrack),
            Data("already managed".utf8),
            "managed audio must remain in place rather than being copied or suffixed"
        )
        XCTAssertEqual(
            try FileManager.default.contentsOfDirectory(atPath: destination.path),
            ["track.mp3"]
        )
    }

    func testCopyRunnerFailsWhenDestinationFolderIsMissing() throws {
        let file = try write(Data("x".utf8), to: temporaryRoot.appendingPathComponent("a.mp3"))
        let missing = temporaryRoot.appendingPathComponent("NoSuchFolder", isDirectory: true)
        let result = LibraryImportCopyRunner.run(
            selectedURLs: [file],
            destinationDirectory: missing,
            operations: operations(),
            scopeFor: { URLLibraryImportSecurityScope(url: $0) }
        )
        guard case let .failure(message) = result.outcome else {
            return XCTFail("expected failure outcome, got \(result.outcome)")
        }
        XCTAssertFalse(message.isEmpty)
        XCTAssertEqual(result.counters, .zero)
    }

    func testCopyRunnerReportsMoveFailureWithoutLeavingTemporaryOrFinalFile() throws {
        let destination = try makeManagedFolder()
        let source = try write(Data("x".utf8), to: temporaryRoot.appendingPathComponent("a.mp3"))
        let recorder = MoveFailingOperations(base: operations())
        let result = LibraryImportCopyRunner.run(
            selectedURLs: [source], destinationDirectory: destination,
            operations: recorder, scopeFor: { URLLibraryImportSecurityScope(url: $0) })
        XCTAssertEqual(result.counters.failed, 1)
        XCTAssertFalse(FileManager.default.fileExists(atPath: destination.appendingPathComponent("a.mp3").path))
        XCTAssertTrue(recorder.removedTemporary)
        // The actual destination directory must contain zero artifacts: no
        // final file and no hidden temporary file.
        let listing = try FileManager.default.contentsOfDirectory(atPath: destination.path)
        XCTAssertTrue(listing.isEmpty, "destination not clean: \(listing)")
    }

    func testPackageDirectoryContentsAreSkippedWhileSiblingsImport() throws {
        let destination = try makeManagedFolder()
        let source = temporaryRoot.appendingPathComponent("Source", isDirectory: true)
        try FileManager.default.createDirectory(at: source, withIntermediateDirectories: true)
        let package = source.appendingPathComponent("media.bundle", isDirectory: true)
        try FileManager.default.createDirectory(at: package, withIntermediateDirectories: true)
        try write(Data("packed".utf8), to: package.appendingPathComponent("packedsong.mp3"))
        try write(Data("sibling".utf8), to: source.appendingPathComponent("sibling.mp3"))
        let isPackage = (try? package.resourceValues(forKeys: [.isPackageKey]))?.isPackage ?? false
        guard isPackage else {
            throw XCTSkip("Test filesystem does not treat \(package.lastPathComponent) as a package")
        }

        let result = LibraryImportCopyRunner.run(
            selectedURLs: [source], destinationDirectory: destination,
            operations: operations(), scopeFor: { URLLibraryImportSecurityScope(url: $0) })

        XCTAssertEqual(result.counters.imported, 1)
        XCTAssertFalse(FileManager.default.fileExists(atPath: destination.appendingPathComponent("packedsong.mp3").path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: destination.appendingPathComponent("sibling.mp3").path))
    }

    func testCopyRunnerKeepsReadableSiblingsWhenNestedDirectoryErrors() throws {
        // The injectable enumerator returns a failure for one subtree and a
        // readable sibling; the sibling must still be imported.
        let destination = try makeManagedFolder()
        let source = try seedFileTree(in: temporaryRoot.appendingPathComponent("Source", isDirectory: true))
        let recorder = PartialEnumerationOperations(base: operations(), source: source)
        let result = LibraryImportCopyRunner.run(
            selectedURLs: [source], destinationDirectory: destination,
            operations: recorder, scopeFor: { URLLibraryImportSecurityScope(url: $0) })
        XCTAssertEqual(result.counters.imported, 2)
        XCTAssertEqual(result.counters.failed, 1)
    }

    // MARK: Helpers

    private func completionExpectation(for recorder: CompletionRecorder) -> XCTestExpectation {
        let expectation = expectation(description: "import completion")
        recorder.onComplete = { expectation.fulfill() }
        return expectation
    }
}

// MARK: - Test doubles

private final class CompletionRecorder: IOSLibraryImportCompletion {
    struct Call {
        let status: Int32
        let imported: Int32
        let duplicates: Int32
        let unsupported: Int32
        let failed: Int32
        let message: String?
    }

    var calls: [Call] = []
    var onComplete: (() -> Void)?

    func complete(status: Int32, imported: Int32, duplicates: Int32, unsupported: Int32, failed: Int32, message: String?) {
        XCTAssertTrue(Thread.isMainThread, "ABI completion must be delivered on main queue")
        calls.append(
            Call(
                status: status,
                imported: imported,
                duplicates: duplicates,
                unsupported: unsupported,
                failed: failed,
                message: message
            )
        )
        onComplete?()
    }
}

private final class PresenterCaptureViewController: UIViewController {
    var presented: UIViewController?
    override func present(
        _ viewControllerToPresent: UIViewController,
        animated flag: Bool,
        completion: (() -> Void)? = nil
    ) {
        presented = viewControllerToPresent
        completion?()
    }
}

private final class CountingScope: LibraryImportSecurityScope {
    let granted: Bool
    var startCount = 0
    var stopCount = 0
    init(granted: Bool) { self.granted = granted }
    func start() -> Bool {
        startCount += 1
        return granted
    }
    func stop() { stopCount += 1 }
}

private final class MoveFailingOperations: LibraryImportFileOperations {
    let base: LibraryImportFileOperations
    var removedTemporary = false
    init(base: LibraryImportFileOperations) { self.base = base }
    func isDirectory(at url: URL) -> Bool { base.isDirectory(at: url) }
    func contentsOfDirectory(at url: URL) throws -> [String] { try base.contentsOfDirectory(at: url) }
    func recursiveFiles(in url: URL) throws -> [URL] { try base.recursiveFiles(in: url) }
    func streamedFileEntries(in url: URL) throws -> [LibraryImportEnumerationEntry] { try base.streamedFileEntries(in: url) }
    func readData(from url: URL) throws -> Data { try base.readData(from: url) }
    func filesEqual(_ lhs: URL, _ rhs: URL) throws -> Bool { try base.filesEqual(lhs, rhs) }
    func streamCopy(from source: URL, to destination: URL) throws { try base.streamCopy(from: source, to: destination) }
    func writeTemporaryFile(data: Data, in directory: URL) throws -> URL { try base.writeTemporaryFile(data: data, in: directory) }
    func moveItem(at source: URL, to destination: URL) throws {
        throw NSError(domain: "MoveFailingOperations", code: 1)
    }
    func removeItem(at url: URL) throws { removedTemporary = true; try base.removeItem(at: url) }
}

private final class PartialEnumerationOperations: LibraryImportFileOperations {
    let base: LibraryImportFileOperations
    let source: URL
    init(base: LibraryImportFileOperations, source: URL) { self.base = base; self.source = source }
    func isDirectory(at url: URL) -> Bool { base.isDirectory(at: url) }
    func contentsOfDirectory(at url: URL) throws -> [String] { try base.contentsOfDirectory(at: url) }
    func recursiveFiles(in url: URL) throws -> [URL] { try base.recursiveFiles(in: url) }
    func streamedFileEntries(in url: URL) throws -> [LibraryImportEnumerationEntry] {
        let album = source.appendingPathComponent("Album A")
        return [.file(album.appendingPathComponent("track1.mp3")), .failed, .file(album.appendingPathComponent("track2.flac"))]
    }
    func readData(from url: URL) throws -> Data { try base.readData(from: url) }
    func filesEqual(_ lhs: URL, _ rhs: URL) throws -> Bool { try base.filesEqual(lhs, rhs) }
    func streamCopy(from source: URL, to destination: URL) throws { try base.streamCopy(from: source, to: destination) }
    func writeTemporaryFile(data: Data, in directory: URL) throws -> URL { try base.writeTemporaryFile(data: data, in: directory) }
    func moveItem(at source: URL, to destination: URL) throws { try base.moveItem(at: source, to: destination) }
    func removeItem(at url: URL) throws { try base.removeItem(at: url) }
}
