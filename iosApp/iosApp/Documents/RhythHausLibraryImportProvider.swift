import Foundation
import Shared
import UIKit
import UniformTypeIdentifiers

/// Swift-owned Files.app import provider registered on the Shared
/// `IOSLibraryImportBridge` before Compose startup.
///
/// Swift owns picker presentation (a single-flight
/// `UIDocumentPickerViewController` for audio + folder selections),
/// security-scoped access (balanced per selected top-level URL), recursive
/// enumeration, supported extension filtering, and the managed copy into the
/// destination folder Kotlin supplies. Only primitive aggregate counters
/// cross the completion; external security-scoped URLs are never returned to
/// Kotlin.
///
/// Threading: all operation-state transitions and ABI completions happen on
/// the main queue; the file copy runs on a private serial queue and hops
/// back to the main queue to finish. Each operation invokes the completion
/// exactly once.
final class RhythHausLibraryImportProvider: NSObject, IOSLibraryImportProvider {
    private let operationState = LibraryImportOperationState()
    private let operations: LibraryImportFileOperations
    private let presenterProvider: () -> UIViewController?
    private let workQueue: DispatchQueue
    private var activeDestinationPath = ""
    private var activeCounters = LibraryImportCounters.zero

    /// Designated initializer with injected dependencies (tests use real
    /// temporary directories plus a capturing presenter).
    init(
        operations: LibraryImportFileOperations,
        presenterProvider: @escaping () -> UIViewController?
    ) {
        self.operations = operations
        self.presenterProvider = presenterProvider
        workQueue = DispatchQueue(label: "com.eterocell.rhythhaus.library-import.\(UUID().uuidString)")
        super.init()
    }

    /// Production initializer: real file operations and the app presenter.
    override convenience init() {
        self.init(
            operations: FileManagerLibraryImportFileOperations(),
            presenterProvider: { RhythHausViewControllerRegistry.presenter }
        )
    }

    // MARK: IOSLibraryImportProvider

    func importAudio(destinationPath: String, completion: IOSLibraryImportCompletion) {
        runOnMain {
            guard
                self.operationState.begin(completion: { outcome in
                    self.deliver(outcome, to: completion)
                })
            else {
                return
            }
            self.activeDestinationPath = destinationPath
            self.activeCounters = .zero
            let picker =
                UIDocumentPickerViewController(
                    forOpeningContentTypes: [.audio, .folder],
                    asCopy: false
                )
            picker.allowsMultipleSelection = true
            self.operationState.attach(picker: picker)
            guard let presenter = self.presenterProvider() else {
                self.operationState.finishCurrent(
                    outcome: .unavailable("Document presenter is unavailable")
                )
                return
            }
            picker.delegate = self
            presenter.present(picker, animated: true)
        }
    }

    // MARK: Terminal delivery

    /// Maps an operation outcome onto the ABI completion. Runs on the main
    /// queue only, and at most once per operation (the operation state
    /// delivers a single terminal outcome).
    private func deliver(
        _ outcome: LibraryImportOperationOutcome,
        to completion: IOSLibraryImportCompletion
    ) {
        let counters = activeCounters
        switch outcome {
        case .success:
            completion.complete(
                status: IOSLibraryImportStatus.shared.SUCCESS,
                imported: Int32(counters.imported),
                duplicates: Int32(counters.duplicates),
                unsupported: Int32(counters.unsupported),
                failed: Int32(counters.failed),
                message: nil
            )
        case .cancelled:
            completion.complete(
                status: IOSLibraryImportStatus.shared.CANCELLED,
                imported: 0,
                duplicates: 0,
                unsupported: 0,
                failed: 0,
                message: nil
            )
        case let .unavailable(message):
            completion.complete(
                status: IOSLibraryImportStatus.shared.UNAVAILABLE,
                imported: 0,
                duplicates: 0,
                unsupported: 0,
                failed: 0,
                message: message
            )
        case let .failure(message):
            completion.complete(
                status: IOSLibraryImportStatus.shared.FAILURE,
                imported: 0,
                duplicates: 0,
                unsupported: 0,
                failed: 0,
                message: message
            )
        case .overlap:
            completion.complete(
                status: IOSLibraryImportStatus.shared.OVERLAP,
                imported: 0,
                duplicates: 0,
                unsupported: 0,
                failed: 0,
                message: nil
            )
        }
    }

    private func runOnMain(_ block: @escaping () -> Void) {
        if Thread.isMainThread {
            block()
        } else {
            DispatchQueue.main.async(execute: block)
        }
    }
}

// MARK: - UIDocumentPickerDelegate

extension RhythHausLibraryImportProvider: UIDocumentPickerDelegate {
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        operationState.finish(picker: controller, outcome: .cancelled)
    }

    func documentPicker(
        _ controller: UIDocumentPickerViewController,
        didPickDocumentsAt urls: [URL]
    ) {
        guard operationState.isCurrent(picker: controller) else { return }
        guard !urls.isEmpty else {
            operationState.finish(picker: controller, outcome: .cancelled)
            return
        }
        let destinationDirectory =
            URL(fileURLWithPath: activeDestinationPath, isDirectory: true)
        let operations = self.operations
        let workQueue = self.workQueue
        workQueue.async { [weak self] in
            guard let self else { return }
            let result =
                LibraryImportCopyRunner.run(
                    selectedURLs: urls,
                    destinationDirectory: destinationDirectory,
                    operations: operations,
                    scopeFor: { URLLibraryImportSecurityScope(url: $0) }
                )
            DispatchQueue.main.async {
                self.activeCounters = result.counters
                self.operationState.finish(picker: controller, outcome: result.outcome)
            }
        }
    }
}
