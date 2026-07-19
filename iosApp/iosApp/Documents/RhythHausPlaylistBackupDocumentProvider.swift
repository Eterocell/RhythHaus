import Foundation
import Shared
import UIKit
import UniformTypeIdentifiers

final class RhythHausPlaylistBackupDocumentProvider: NSObject, IOSPlaylistBackupDocumentProvider {
    private var activePicker: UIDocumentPickerViewController?
    private var activeCompletion: IOSPlaylistBackupDocumentCompletion?
    private var temporaryExportURL: URL?
    private var temporaryExportDirectoryURL: URL?
    private var importMaxBytes: Int = 0

    func saveDocument(
        fileName: String,
        bytes: KotlinByteArray,
        completion: IOSPlaylistBackupDocumentCompletion
    ) {
        guard beginOperation(completion: completion) else { return }
        do {
            let exportDirectoryURL = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString, isDirectory: true)
            temporaryExportDirectoryURL = exportDirectoryURL
            let safeFileName = URL(fileURLWithPath: fileName).lastPathComponent
            let exportURL = exportDirectoryURL
                .appendingPathComponent(safeFileName)
            try FileManager.default.createDirectory(
                at: exportDirectoryURL,
                withIntermediateDirectories: true
            )
            try bytes.toData().write(to: exportURL, options: .atomic)
            temporaryExportURL = exportURL
            present(UIDocumentPickerViewController(forExporting: [exportURL], asCopy: true))
        } catch {
            finish(status: IOSPlaylistBackupDocumentStatus.shared.FAILURE, message: error.localizedDescription)
        }
    }

    func openDocument(maxBytes: Int32, completion: IOSPlaylistBackupDocumentCompletion) {
        guard beginOperation(completion: completion) else { return }
        importMaxBytes = Int(maxBytes)
        let vendorType = UTType("application/vnd.rhythhaus.playlists+json")
        let contentTypes = [vendorType, .json].compactMap { $0 }
        present(UIDocumentPickerViewController(forOpeningContentTypes: contentTypes, asCopy: false))
    }

    deinit {
        cleanupTemporaryExport()
    }

    private func beginOperation(completion: IOSPlaylistBackupDocumentCompletion) -> Bool {
        guard activePicker == nil, activeCompletion == nil else {
            completion.complete(
                status: IOSPlaylistBackupDocumentStatus.shared.FAILURE,
                bytes: nil,
                message: "Another document operation is already active"
            )
            return false
        }
        activeCompletion = completion
        return true
    }

    private func present(_ picker: UIDocumentPickerViewController) {
        guard let presenter = RhythHausViewControllerRegistry.presenter else {
            finish(status: IOSPlaylistBackupDocumentStatus.shared.UNAVAILABLE, message: "Document presenter is unavailable")
            return
        }
        activePicker = picker
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    private func finish(status: Int32, bytes: KotlinByteArray? = nil, message: String? = nil) {
        let completion = activeCompletion
        activeCompletion = nil
        activePicker = nil
        importMaxBytes = 0
        cleanupTemporaryExport()
        completion?.complete(status: status, bytes: bytes, message: message)
    }

    private func cleanupTemporaryExport() {
        guard let exportDirectoryURL = temporaryExportDirectoryURL else { return }
        temporaryExportDirectoryURL = nil
        temporaryExportURL = nil
        try? FileManager.default.removeItem(at: exportDirectoryURL)
    }

    private func readBounded(url: URL) throws -> Data {
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed { url.stopAccessingSecurityScopedResource() }
        }
        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }
        return try handle.read(upToCount: importMaxBytes + 1) ?? Data()
    }
}

extension RhythHausPlaylistBackupDocumentProvider: UIDocumentPickerDelegate {
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        guard controller === activePicker else { return }
        finish(status: IOSPlaylistBackupDocumentStatus.shared.CANCELLED)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard controller === activePicker else { return }
        guard temporaryExportURL == nil else {
            finish(status: IOSPlaylistBackupDocumentStatus.shared.SUCCESS)
            return
        }
        guard let url = urls.first else {
            finish(status: IOSPlaylistBackupDocumentStatus.shared.CANCELLED)
            return
        }
        do {
            let data = try readBounded(url: url)
            guard data.count <= importMaxBytes else {
                finish(status: IOSPlaylistBackupDocumentStatus.shared.TOO_LARGE)
                return
            }
            finish(status: IOSPlaylistBackupDocumentStatus.shared.SUCCESS, bytes: data.toKotlinByteArray())
        } catch {
            finish(status: IOSPlaylistBackupDocumentStatus.shared.FAILURE, message: error.localizedDescription)
        }
    }
}

private extension KotlinByteArray {
    func toData() -> Data {
        var data = Data(count: Int(size))
        data.withUnsafeMutableBytes { buffer in
            guard let baseAddress = buffer.baseAddress else { return }
            for index in 0 ..< Int(size) {
                baseAddress.storeBytes(of: get(index: Int32(index)), toByteOffset: index, as: Int8.self)
            }
        }
        return data
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        withUnsafeBytes { buffer in
            guard let baseAddress = buffer.baseAddress else { return }
            for index in 0 ..< count {
                result.set(index: Int32(index), value: baseAddress.load(fromByteOffset: index, as: Int8.self))
            }
        }
        return result
    }
}
