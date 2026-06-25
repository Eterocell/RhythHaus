import SwiftUI
import Shared

// MARK: - LibraryStore

class LibraryStore: ObservableObject {
    @Published var tracks: [LibraryTrack] = []
    @Published var snapshot = LibrarySnapshot(
        title: "RhythHaus",
        subtitle: "",
        tracks: [],
        nowPlayingTrackId: nil
    )

    private var database: LibraryDatabase?
    private var repository: SqlDelightLibraryRepository?

    init() {
        let db = LibraryDatabase()
        database = db
        repository = SqlDelightLibraryRepository(libraryDatabase: db)
        refresh()
    }

    func refresh() {
        guard let repo = repository else { return }
        tracks = repo.tracks()
        snapshot = librarySnapshotFromTracks(tracks)
    }

    private func librarySnapshotFromTracks(_ tracks: [LibraryTrack]) -> LibrarySnapshot {
        let hues: [(Int64, Int64)] = [
            (0xFF111018, 0xFF776F66),
            (0xFF1A1422, 0xFF794A4A),
            (0xFF14202A, 0xFF4B6B7A),
            (0xFF1A1E1A, 0xFF5C784C),
            (0xFF201A16, 0xFF7A6448),
            (0xFF161A24, 0xFF4B5C7A),
            (0xFF1A1420, 0xFF6E4B7A),
        ]
        var uiTracks: [Track] = []
        for (i, t) in tracks.enumerated() {
            let (hueStart, hueEnd) = hues[i % hues.count]
            let accent = TrackAccent(start: hueStart, end: hueEnd)
            let duration = Int32(truncatingIfNeeded: (t.durationMillis?.int64Value ?? 0) / 1000)
            let track = Track(
                id: t.id,
                title: t.title,
                artist: t.artist,
                album: t.album,
                durationSeconds: duration,
                accent: accent,
                source: t.audioSource,
                trackNumber: t.trackNumber,
                discNumber: t.discNumber,
                artworkBytes: t.artworkBytes
            )
            uiTracks.append(track)
        }
        return LibrarySnapshot(
            title: "RhythHaus",
            subtitle: "",
            tracks: uiTracks,
            nowPlayingTrackId: nil
        )
    }
}

// MARK: - LibraryView

struct LibraryView: View {
    @ObservedObject var engine: AudioEngine
    @ObservedObject var libraryStore: LibraryStore
    @State private var selectedTrack: Track? = nil
    @State private var showNowPlaying = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    // Header
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("RhythHaus")
                                .font(.system(size: 44, weight: .black))
                                .tracking(-1.6)
                            Text("\(libraryStore.tracks.count) tracks · \(formatTotalDuration(libraryStore.snapshot.totalDurationSeconds))")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal)
                    .padding(.top, 12)

                    // Track list
                    ForEach(libraryStore.snapshot.tracks, id: \.id) { track in
                        TrackRowView(track: track) {
                            selectedTrack = track
                            playTrack(track)
                            showNowPlaying = true
                        }
                    }
                }
                .padding(.bottom, 80) // space for now playing bar
            }
            .navigationBarHidden(true)
            .background(Color(uiColor: .systemBackground))
            .navigationDestination(isPresented: $showNowPlaying) {
                if let track = selectedTrack {
                    NowPlayingView(
                        track: track,
                        engine: engine,
                        onBack: { showNowPlaying = false }
                    )
                }
            }
            .overlay(alignment: .bottom) {
                // Mini now playing bar
                if engine.state.currentTrack != nil {
                    NowPlayingBar(engine: engine) {
                        if let track = selectedTrack {
                            showNowPlaying = true
                        }
                    }
                }
            }
        }
    }

    private func playTrack(_ track: Track) {
        let duration: KotlinLong? = track.durationSeconds > 0
            ? KotlinLong(integerLiteral: Int(track.durationSeconds) * 1000)
            : nil
        let playable = PlayableTrack(
            id: track.id,
            title: track.title,
            artist: track.artist,
            album: track.album,
            durationMillis: duration,
            source: track.source,
            artworkBytes: track.artworkBytes
        )
        engine.load(track: playable)
        engine.play()
    }

    private func formatTotalDuration(_ seconds: Int32) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return "\(m):\(String(format: "%02d", s)) total"
    }
}

// MARK: - TrackRowView

struct TrackRowView: View {
    let track: Track
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Artwork placeholder
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(hex: UInt(truncatingIfNeeded: track.accent.start)))
                        .frame(width: 48, height: 48)
                    Text(String(track.title.prefix(1)).uppercased())
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white.opacity(0.8))
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(track.title)
                        .font(.body.weight(.semibold))
                        .lineLimit(1)
                        .foregroundColor(.primary)
                    Text("\(track.artist) · \(track.album)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                Spacer()
                Text(formatTrackDuration(track.durationSeconds))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)
            .padding(.vertical, 4)
        }
    }

    private func formatTrackDuration(_ seconds: Int32) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return "\(m):\(String(format: "%02d", s))"
    }
}

// MARK: - NowPlayingBar (mini)

struct NowPlayingBar: View {
    @ObservedObject var engine: AudioEngine
    let onExpand: () -> Void

    var body: some View {
        Button(action: onExpand) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.secondary.opacity(0.3))
                    .frame(width: 40, height: 40)
                    .overlay(Text("♪").font(.title3))

                VStack(alignment: .leading, spacing: 2) {
                    Text(engine.state.currentTrack?.title ?? "")
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Text(engine.state.currentTrack?.artist ?? "")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                Spacer()
                Button(action: {
                    if engine.state.status == "playing" {
                        engine.pause()
                    } else {
                        engine.play()
                    }
                }) {
                    Image(systemName: engine.state.status == "playing" ? "pause.fill" : "play.fill")
                        .font(.title3)
                }
            }
            .padding(12)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 12)
            .padding(.bottom, 8)
        }
    }
}

// MARK: - NowPlayingView (placeholder)

struct NowPlayingView: View {
    let track: Track
    @ObservedObject var engine: AudioEngine
    let onBack: () -> Void

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

            // Artwork placeholder
            RoundedRectangle(cornerRadius: 32)
                .fill(Color(hex: UInt(truncatingIfNeeded: track.accent.start)))
                .frame(height: 300)
                .padding(.horizontal, 20)
                .overlay(Text(String(track.title.prefix(3)).uppercased())
                    .font(.system(size: 64, weight: .black))
                    .foregroundColor(.white.opacity(0.3)))

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

            // Transport
            HStack(spacing: 40) {
                Button(action: { /* skip prev */ }) {
                    Image(systemName: "backward.fill").font(.title2)
                }
                Button(action: {
                    if engine.state.status == "playing" {
                        engine.pause()
                    } else {
                        engine.play()
                    }
                }) {
                    Image(systemName: engine.state.status == "playing" ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 64))
                }
                Button(action: { /* skip next */ }) {
                    Image(systemName: "forward.fill").font(.title2)
                }
            }

            Spacer()
        }
        .background(Color(uiColor: .systemBackground))
        .navigationBarHidden(true)
    }
}

// MARK: - Color helper

extension Color {
    init(hex: UInt) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0
        )
    }
}
