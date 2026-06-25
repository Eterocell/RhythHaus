//
//  ImportCardView.swift
//  iosApp
//
//  Created by Nous Research on 2026-06-25.
//

import SwiftUI

// MARK: - ImportCardView

/// Container that shows ImportAudioCard when idle and ScanningCard when scanning.
struct ImportCardView: View {
    let hasTracks: Bool
    let isScanning: Bool
    let scanProgress: String?
    let onAddFolder: () -> Void
    let onClearLibrary: () -> Void
    let onCancelScan: () -> Void

    var body: some View {
        if isScanning {
            ScanningCard(
                progress: scanProgress,
                onCancel: onCancelScan
            )
        } else {
            ImportAudioCard(
                hasTracks: hasTracks,
                onAddFolder: onAddFolder,
                onClearLibrary: onClearLibrary
            )
        }
    }
}

// MARK: - ImportAudioCard

/// Card shown at the top of the library with import and clear actions.
struct ImportAudioCard: View {
    let hasTracks: Bool
    let onAddFolder: () -> Void
    let onClearLibrary: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Title
            Text(hasTracks ? "Your library" : "Add your music")
                .font(.title3.weight(.black))

            // Description
            Text(hasTracks
                ? "Tap below to scan more folders. RhythHaus watches your folders and keeps the library up to date."
                : "Select a folder of audio files to start building your library. Supported formats: MP3, FLAC, WAV, AAC, M4A, OGG, AIFF."
            )
                .font(.caption)
                .foregroundColor(.secondary)

            // "Add music folder" button
            Button(action: onAddFolder) {
                Text("Add music folder")
                    .font(.body.weight(.black))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }

            // "Clear Library" button — only when tracks exist
            if hasTracks {
                Button(action: onClearLibrary) {
                    Text("Clear Library")
                        .font(.body.weight(.semibold))
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(Color.red.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(18)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal)
    }
}

// MARK: - ScanningCard

/// Card shown during an active scan with progress and a cancel button.
struct ScanningCard: View {
    let progress: String?
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Title
            Text("Scanning")
                .font(.title3.weight(.black))

            // Progress indicator + text
            HStack(spacing: 12) {
                ProgressView()
                    .tint(.primary)

                Text(progress ?? "Scanning folders…")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Cancel button
            Button(action: onCancel) {
                Text("Cancel")
                    .font(.body.weight(.semibold))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                    .background(Color(uiColor: .tertiarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(18)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal)
    }
}
