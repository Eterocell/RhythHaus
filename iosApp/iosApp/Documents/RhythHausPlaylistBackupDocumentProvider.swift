import Foundation
import Shared
import UIKit

final class RhythHausPlaylistBackupDocumentProvider: NSObject, IOSPlaylistBackupDocumentProvider {
    private let operationState = PlaylistBackupDocumentOperationState()
    private var importMaxBytes: Int = 0
    private var importedBytes: KotlinByteArray?
    private var isExportOperation = false

    func saveDocument(
        fileName: String,
        bytes: KotlinByteArray,
        completion: IOSPlaylistBackupDocumentCompletion
    ) {
        guard beginOperation(completion: completion) else { return }
        do {
            let prepared = try PlaylistBackupDocumentTemporaryExport.prepare(
                fileName: fileName,
                data: bytes.toData(),
                storage: FileManagerPlaylistBackupDocumentTemporaryStorage(fileManager: .default)
            )
            isExportOperation = true
            present(
                UIDocumentPickerViewController(forExporting: [prepared.fileURL], asCopy: true),
                cleanup: prepared.cleanup
            )
        } catch {
            operationState.finishCurrent(outcome: .failure(error.localizedDescription))
        }
    }

    func openDocument(maxBytes: Int32, completion: IOSPlaylistBackupDocumentCompletion) {
        guard beginOperation(completion: completion) else { return }
        importMaxBytes = Int(maxBytes)
        isExportOperation = false
        present(UIDocumentPickerViewController(forOpeningContentTypes: PlaylistBackupDocumentTypePolicy.contentTypes(), asCopy: false))
    }

    private func beginOperation(completion: IOSPlaylistBackupDocumentCompletion) -> Bool {
        let started = operationState.begin { [weak self] outcome in
            let result = self?.bridgeResult(for: outcome)
                ?? (IOSPlaylistBackupDocumentStatus.shared.FAILURE, nil, "Document provider was released")
            completion.complete(status: result.0, bytes: result.1, message: result.2)
        }
        if started { importedBytes = nil }
        return started
    }

    private func present(_ picker: UIDocumentPickerViewController, cleanup: (() -> Void)? = nil) {
        operationState.attach(picker: picker, cleanup: cleanup)
        guard let presenter = RhythHausViewControllerRegistry.presenter else {
            operationState.finish(picker: picker, outcome: .unavailable("Document presenter is unavailable"))
            return
        }
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    private func bridgeResult(
        for outcome: PlaylistBackupDocumentPolicyOutcome
    ) -> (Int32, KotlinByteArray?, String?) {
        if case .overlap = outcome {
            return (IOSPlaylistBackupDocumentStatus.shared.FAILURE, nil, "Another document operation is already active")
        }
        importMaxBytes = 0
        defer { importedBytes = nil }
        switch outcome {
        case .success:
            return (IOSPlaylistBackupDocumentStatus.shared.SUCCESS, importedBytes, nil)
        case .cancelled:
            return (IOSPlaylistBackupDocumentStatus.shared.CANCELLED, nil, nil)
        case .tooLarge:
            return (IOSPlaylistBackupDocumentStatus.shared.TOO_LARGE, nil, nil)
        case let .unavailable(message):
            return (IOSPlaylistBackupDocumentStatus.shared.UNAVAILABLE, nil, message)
        case let .failure(message):
            return (IOSPlaylistBackupDocumentStatus.shared.FAILURE, nil, message)
        case .overlap:
            fatalError("Handled before terminal operation cleanup")
        }
    }

    private func readBounded(url: URL) throws -> Data {
        try PlaylistBackupDocumentResourcePolicy.readBounded(
            maxBytes: importMaxBytes,
            securityScope: URLPlaylistBackupDocumentSecurityScope(url: url),
            openHandle: { try FilePlaylistBackupDocumentReadHandle(url: url) }
        )
    }
}

extension RhythHausPlaylistBackupDocumentProvider: UIDocumentPickerDelegate {
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        operationState.finish(picker: controller, outcome: .cancelled)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard operationState.isCurrent(picker: controller) else { return }
        guard !isExportOperation else {
            operationState.finish(picker: controller, outcome: .success)
            return
        }
        guard let url = urls.first else {
            operationState.finish(picker: controller, outcome: .cancelled)
            return
        }
        do {
            let data = try readBounded(url: url)
            importedBytes = data.toKotlinByteArray()
            operationState.finish(picker: controller, outcome: .success)
        } catch PlaylistBackupDocumentPolicyError.tooLarge {
            operationState.finish(picker: controller, outcome: .tooLarge)
        } catch {
            operationState.finish(picker: controller, outcome: .failure(error.localizedDescription))
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
