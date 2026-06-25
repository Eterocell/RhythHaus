import SwiftUI
import Shared

// MARK: - BrowseMode

enum BrowseMode: String, CaseIterable {
    case albums = "Albums"
    case artists = "Artists"
}

// MARK: - AlbumGroupSwift

struct AlbumGroupSwift: Identifiable {
    let album: String
    let tracks: [Track]
    let artist: String?
    var id: String { album }

    static func from(_ tracks: [Track]) -> [AlbumGroupSwift] {
        let grouped = Dictionary(grouping: tracks, by: { $0.album })
        return grouped.map { (album, tracks) in
            let sorted = tracks.sorted {
                ($0.discNumber?.intValue ?? 0, $0.trackNumber?.intValue ?? 0, $0.title.lowercased())
                < ($1.discNumber?.intValue ?? 0, $1.trackNumber?.intValue ?? 0, $1.title.lowercased())
            }
            return AlbumGroupSwift(album: album, tracks: sorted, artist: sorted.first?.artist)
        }.sorted { $0.album.lowercased() < $1.album.lowercased() }
    }
}

// MARK: - ArtistGroupSwift

struct ArtistGroupSwift: Identifiable {
    let artist: String
    let tracks: [Track]
    let albumCount: Int
    var id: String { artist }

    static func from(_ tracks: [Track]) -> [ArtistGroupSwift] {
        let grouped = Dictionary(grouping: tracks, by: { $0.artist })
        return grouped.map { (artist, tracks) in
            let sorted = tracks.sorted {
                ($0.discNumber?.intValue ?? 0, $0.trackNumber?.intValue ?? 0, $0.title.lowercased())
                < ($1.discNumber?.intValue ?? 0, $1.trackNumber?.intValue ?? 0, $1.title.lowercased())
            }
            let albumCount = Set(tracks.map { $0.album }).count
            return ArtistGroupSwift(artist: artist, tracks: sorted, albumCount: albumCount)
        }.sorted { $0.artist.lowercased() < $1.artist.lowercased() }
    }
}

// MARK: - LibraryStore

class LibraryStore: ObservableObject {
    @Published var tracks: [LibraryTrack] = []
    @Published var snapshot = LibrarySnapshot(
        title: "RhythHaus",
        subtitle: "",
        tracks: [],
        nowPlayingTrackId: nil
    )
    @Published var isScanning = false
    @Published var scanProgress: String? = nil
    @Published var showFolderPicker = false
    @Published var showClearAlert = false

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

    func clearLibrary() {
        repository?.clearAll()
        refresh()
    }

    func importFolder(url: URL) {
        isScanning = true
        scanProgress = "Starting scan…"
        // Placeholder — wire real scanner later
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
    @State private var browseMode: BrowseMode = .albums
    @State private var selectedTrack: Track? = nil
    @State private var showNowPlaying = false

    private var albumGroups: [AlbumGroupSwift] {
        AlbumGroupSwift.from(libraryStore.snapshot.tracks)
    }

    private var artistGroups: [ArtistGroupSwift] {
        ArtistGroupSwift.from(libraryStore.snapshot.tracks)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // HeaderSection
                    HeaderSection(
                        trackCount: libraryStore.snapshot.tracks.count,
                        totalDuration: formatTotalDuration(libraryStore.snapshot.totalDurationSeconds)
                    )

                    // ImportCardView
                    ImportCardView(
                        hasTracks: !libraryStore.tracks.isEmpty,
                        isScanning: libraryStore.isScanning,
                        scanProgress: libraryStore.scanProgress,
                        onAddFolder: { libraryStore.showFolderPicker = true },
                        onClearLibrary: { libraryStore.showClearAlert = true },
                        onCancelScan: { libraryStore.isScanning = false }
                    )

                    // BrowseModePicker
                    Picker("Browse", selection: $browseMode) {
                        ForEach(BrowseMode.allCases, id: \.self) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)

                    // SectionLabel
                    SectionLabel(
                        title: "Library queue",
                        trackCount: libraryStore.snapshot.tracks.count,
                        totalDuration: formatTotalDuration(libraryStore.snapshot.totalDurationSeconds)
                    )
                    .padding(.horizontal)

                    // Content based on browse mode
                    switch browseMode {
                    case .albums:
                        albumGrid
                    case .artists:
                        artistList
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
                        trackQueue: libraryStore.snapshot.tracks,
                        engine: engine,
                        onBack: { showNowPlaying = false },
                        onPlayTrack: { t in playTrack(t) }
                    )
                }
            }
            .overlay(alignment: .bottom) {
                // Mini now playing bar
                if engine.state.currentTrack != nil {
                    NowPlayingBar(engine: engine) {
                        if selectedTrack != nil {
                            showNowPlaying = true
                        }
                    }
                }
            }
        }
        .fileImporter(
            isPresented: $libraryStore.showFolderPicker,
            allowedContentTypes: [.folder],
            allowsMultipleSelection: false
        ) { result in
            if case .success(let urls) = result, let url = urls.first {
                let secured = url.startAccessingSecurityScopedResource()
                libraryStore.importFolder(url: url)
                if !secured {
                    url.stopAccessingSecurityScopedResource()
                }
            }
        }
        .alert("Clear Library", isPresented: $libraryStore.showClearAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Clear", role: .destructive) {
                libraryStore.clearLibrary()
            }
        } message: {
            Text("Remove all imported tracks? This cannot be undone.")
        }
    }

    // MARK: - Album Grid

    private var albumGrid: some View {
        let columns = [
            GridItem(.flexible(), spacing: 12),
            GridItem(.flexible(), spacing: 12)
        ]
        return LazyVGrid(columns: columns, spacing: 12) {
            ForEach(albumGroups) { album in
                NavigationLink {
                    DrillDownView(
                        title: album.album,
                        subtitle: album.artist.map { "\($0) · \(album.tracks.count) tracks" } ?? "\(album.tracks.count) tracks",
                        tracks: album.tracks,
                        engine: engine,
                        libraryStore: libraryStore,
                        playTrack: { track in
                            selectedTrack = track
                            playTrack(track)
                            showNowPlaying = true
                        }
                    )
                } label: {
                    AlbumCardView(album: album)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Artist List

    private var artistList: some View {
        LazyVStack(spacing: 8) {
            ForEach(artistGroups) { artistGroup in
                NavigationLink {
                    DrillDownView(
                        title: artistGroup.artist,
                        subtitle: "\(artistGroup.albumCount) albums · \(artistGroup.tracks.count) tracks",
                        tracks: artistGroup.tracks,
                        engine: engine,
                        libraryStore: libraryStore,
                        playTrack: { track in
                            selectedTrack = track
                            playTrack(track)
                            showNowPlaying = true
                        }
                    )
                } label: {
                    ArtistRowView(artistGroup: artistGroup)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Playback

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

// MARK: - HeaderSection

struct HeaderSection: View {
    let trackCount: Int
    let totalDuration: String

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("RhythHaus")
                    .font(.system(size: 44, weight: .black))
                    .tracking(-1.6)
                Text("\(trackCount) tracks · \(totalDuration)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal)
        .padding(.top, 12)
    }
}

// MARK: - SectionLabel

struct SectionLabel: View {
    let title: String
    let trackCount: Int
    let totalDuration: String

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 20, weight: .black))
            Spacer()
            Text("\(trackCount) tracks · \(totalDuration)")
                .font(.caption.weight(.bold))
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - AlbumCardView

struct AlbumCardView: View {
    let album: AlbumGroupSwift

    private var accentStartColor: Color {
        if let first = album.tracks.first {
            return Color(hex: UInt(truncatingIfNeeded: first.accent.start))
        }
        return Color(hex: 0xFF111018)
    }

    private var accentEndColor: Color {
        if let first = album.tracks.first {
            return Color(hex: UInt(truncatingIfNeeded: first.accent.end))
        }
        return Color(hex: 0xFF776F66)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Artwork placeholder with gradient
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [accentStartColor, accentEndColor]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(height: 120)

                Text(String(album.album.prefix(3)).uppercased())
                    .font(.system(size: 36, weight: .black))
                    .foregroundColor(.white.opacity(0.72))
            }

            // Album name
            Text(album.album)
                .font(.system(size: 14, weight: .black))
                .lineLimit(2)
                .foregroundColor(.primary)

            // Artist + track count
            Text(subtitleText)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(.secondary)
                .lineLimit(1)
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var subtitleText: String {
        if let artist = album.artist {
            return "\(artist) · \(album.tracks.count) tracks"
        }
        return "\(album.tracks.count) tracks"
    }
}

// MARK: - ArtistRowView

struct ArtistRowView: View {
    let artistGroup: ArtistGroupSwift

    private var accentColor: Color {
        if let first = artistGroup.tracks.first {
            return Color(hex: UInt(truncatingIfNeeded: first.accent.start))
        }
        return Color(hex: 0xFF111018)
    }

    private var accentEndColor: Color {
        if let first = artistGroup.tracks.first {
            return Color(hex: UInt(truncatingIfNeeded: first.accent.end))
        }
        return Color(hex: 0xFF776F66)
    }

    var body: some View {
        HStack(spacing: 14) {
            // Accent circle with first letter
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [accentColor, accentEndColor]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 40, height: 40)

                Text(String(artistGroup.artist.prefix(1)).uppercased())
                    .font(.system(size: 18, weight: .black))
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(artistGroup.artist)
                    .font(.system(size: 16, weight: .black))
                    .lineLimit(1)
                    .foregroundColor(.primary)

                Text("\(artistGroup.albumCount) albums · \(artistGroup.tracks.count) tracks")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundColor(.secondary)
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemBackground).opacity(0.54))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(Color(uiColor: .separator), lineWidth: 1)
        )
    }
}

// MARK: - DrillDownView

struct DrillDownView: View {
    let title: String
    let subtitle: String
    let tracks: [Track]
    @ObservedObject var engine: AudioEngine
    @ObservedObject var libraryStore: LibraryStore
    let playTrack: (Track) -> Void

    @State private var selectedTrackId: String? = nil
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            // Header with back button
            DrillDownHeader(title: title, subtitle: subtitle) {
                dismiss()
            }

            // Section label
            SectionLabel(
                title: title,
                trackCount: tracks.count,
                totalDuration: formatTracksDuration(tracks)
            )
            .padding(.horizontal)
            .padding(.top, 12)
            .padding(.bottom, 8)

            // Track list
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(tracks, id: \.id) { track in
                        TrackRowView(track: track) {
                            selectedTrackId = track.id
                            playTrack(track)
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 80)
            }
        }
        .background(Color(uiColor: .systemBackground))
        .navigationBarHidden(true)
        .overlay(alignment: .bottom) {
            if engine.state.currentTrack != nil {
                NowPlayingBar(engine: engine) {
                    // no-op in drill-down; NowPlaying is handled by LibraryView
                }
            }
        }
    }

    private func formatTracksDuration(_ tracks: [Track]) -> String {
        let total = tracks.reduce(0) { $0 + max(0, Int32($1.durationSeconds)) }
        let m = total / 60
        let s = total % 60
        return "\(m):\(String(format: "%02d", s)) total"
    }
}

// MARK: - DrillDownHeader

struct DrillDownHeader: View {
    let title: String
    let subtitle: String
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Button(action: onBack) {
                    Text("← BACK")
                        .font(.system(size: 11, weight: .black))
                        .tracking(1.8)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(.black)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }

                Text(subtitle)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.secondary)
            }

            Text(title)
                .font(.system(size: 44, weight: .black))
                .tracking(-1.6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
        .padding(.top, 18)
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
