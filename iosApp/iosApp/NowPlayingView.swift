import SwiftUI
import Shared

// MARK: - KotlinByteArray + Data

private extension KotlinByteArray {
    func toData() -> Data {
        let count = Int(size)
        var data = Data(count: count)
        for i in 0..<count {
            data[i] = UInt8(bitPattern: get(index: Int32(i)))
        }
        return data
    }
}

// MARK: - NowPlayingView

struct NowPlayingView: View {
    let track: Track
    let trackQueue: [Track]
    @ObservedObject var engine: AudioEngine
    let onBack: () -> Void
    let onPlayTrack: (Track) -> Void

    // MARK: Seek state

    @State private var seekDragValue: Double = 0
    @State private var isSeeking = false

    // MARK: Equalizer animation

    @State private var equalizerPhase: Double = 0
    @State private var equalizerTimer: Timer? = nil

    private let barCount = 22

    // MARK: - Computed

    private var accentStartColor: Color {
        Color(hex: UInt(truncatingIfNeeded: track.accent.start))
    }

    private var accentEndColor: Color {
        Color(hex: UInt(truncatingIfNeeded: track.accent.end))
    }

    private var artworkImage: UIImage? {
        guard let bytes = track.artworkBytes else { return nil }
        return UIImage(data: bytes.toData())
    }

    private var displayPosition: Double {
        isSeeking ? seekDragValue : engine.state.positionSeconds
    }

    private var durationSeconds: Double {
        engine.state.durationSeconds
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 24) {
            // Back button
            HStack {
                Button(action: onBack) {
                    Text("← LIBRARY")
                        .font(.caption.weight(.black))
                        .tracking(1.8)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(.black)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                Spacer()
            }
            .padding(.horizontal)
            .padding(.top, 16)

            Spacer()

            // Artwork
            artworkArea
                .padding(.horizontal, 20)

            // Track info
            VStack(spacing: 4) {
                Text(track.title)
                    .font(.system(size: 28, weight: .black))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)

                Text("\(track.artist) · \(track.album)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let tn = track.trackNumber {
                    Text("Track \(tn)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.horizontal)

            // Seek bar
            seekBar
                .padding(.horizontal, 24)

            // Equalizer animation
            equalizerBars
                .frame(height: 44)
                .padding(.horizontal, 20)

            // Transport controls
            transportControls

            Spacer()
        }
        .background(Color(uiColor: .systemBackground))
        .navigationBarHidden(true)
        .onAppear {
            if engine.state.status == "playing" {
                startEqualizerTimer()
            }
        }
        .onDisappear {
            stopEqualizerTimer()
        }
        .onChange(of: engine.state.status) { _, newStatus in
            if newStatus == "playing" {
                startEqualizerTimer()
            } else {
                stopEqualizerTimer()
            }
        }
    }

    // MARK: - Artwork Area

    private var artworkArea: some View {
        Group {
            if let image = artworkImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 32))
            } else {
                // Accent gradient placeholder
                RoundedRectangle(cornerRadius: 32)
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [accentStartColor, accentEndColor]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 300)
                    .overlay(
                        Text(String(track.title.prefix(3)).uppercased())
                            .font(.system(size: 64, weight: .black))
                            .foregroundColor(.white.opacity(0.3))
                    )
            }
        }
    }

    // MARK: - Seek Bar

    private var seekBar: some View {
        VStack(spacing: 8) {
            Slider(
                value: Binding(
                    get: { displayPosition },
                    set: { newValue in
                        seekDragValue = newValue
                        isSeeking = true
                    }
                ),
                in: 0...max(durationSeconds, 1),
                onEditingChanged: { editing in
                    if !editing {
                        isSeeking = false
                        engine.seek(to: seekDragValue)
                    }
                }
            )
            .tint(accentStartColor)

            HStack {
                Text(formatTime(displayPosition))
                    .font(.caption2)
                    .foregroundColor(.secondary)

                Spacer()

                Text(formatTime(durationSeconds))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Equalizer Bars

    private var equalizerBars: some View {
        let isPlaying = engine.state.status == "playing"

        return HStack(spacing: 0) {
            Spacer()
            ForEach(0..<barCount, id: \.self) { index in
                equalizerBar(index: index, active: isPlaying)
            }
            Spacer()
        }
    }

    private func equalizerBar(index: Int, active: Bool) -> some View {
        let normalized: CGFloat = {
            if active {
                // Deterministic pattern matching Compose EqualizerStrip
                let base = CGFloat(((index * 37) % 11 + 4)) / 15.0
                // Add subtle phase animation
                let wave = sin(equalizerPhase + Double(index) * 0.7)
                return base + CGFloat(wave) * 0.08
            } else {
                return 0.34
            }
        }()

        let barHeight: CGFloat = max(4, 44 * normalized)
        let opacity: CGFloat = 0.32 + normalized * 0.48

        return RoundedRectangle(cornerRadius: 2)
            .fill(Color.white.opacity(Double(opacity)))
            .frame(width: 2.5, height: barHeight)
            .animation(.easeInOut(duration: 0.2), value: normalized)
    }

    // MARK: - Transport Controls

    private var transportControls: some View {
        HStack(spacing: 40) {
            Button(action: skipToPrevious) {
                Image(systemName: "backward.fill")
                    .font(.title2)
            }

            Button(action: togglePlayPause) {
                Image(systemName: engine.state.status == "playing"
                    ? "pause.circle.fill"
                    : "play.circle.fill")
                    .font(.system(size: 64))
            }

            Button(action: skipToNext) {
                Image(systemName: "forward.fill")
                    .font(.title2)
            }
        }
        .padding(.bottom, 16)
    }

    // MARK: - Actions

    private func togglePlayPause() {
        if engine.state.status == "playing" {
            engine.pause()
        } else {
            engine.play()
        }
    }

    private func skipToNext() {
        guard let currentIndex = trackQueue.firstIndex(where: { $0.id == track.id }) else { return }
        let nextIndex = currentIndex + 1
        if nextIndex < trackQueue.count {
            onPlayTrack(trackQueue[nextIndex])
        } else if !trackQueue.isEmpty {
            onPlayTrack(trackQueue[0])
        }
    }

    private func skipToPrevious() {
        guard let currentIndex = trackQueue.firstIndex(where: { $0.id == track.id }) else { return }
        let prevIndex = currentIndex - 1
        if prevIndex >= 0 {
            onPlayTrack(trackQueue[prevIndex])
        } else if !trackQueue.isEmpty {
            onPlayTrack(trackQueue[trackQueue.count - 1])
        }
    }

    // MARK: - Equalizer Timer

    private func startEqualizerTimer() {
        guard equalizerTimer == nil else { return }
        equalizerTimer = Timer.scheduledTimer(withTimeInterval: 0.12, repeats: true) { [self] _ in
            DispatchQueue.main.async {
                equalizerPhase += 0.3
            }
        }
    }

    private func stopEqualizerTimer() {
        equalizerTimer?.invalidate()
        equalizerTimer = nil
    }

    // MARK: - Helpers

    private func formatTime(_ seconds: Double) -> String {
        let safe = max(0, seconds)
        let m = Int(safe) / 60
        let s = Int(safe) % 60
        return "\(m):\(String(format: "%02d", s))"
    }
}
