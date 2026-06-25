import AVFoundation
import MediaPlayer
import Shared

// MARK: - AudioEngineState

struct AudioEngineState {
    var status: String = "idle"  // "idle", "loading", "playing", "paused", "stopped", "error"
    var positionSeconds: Double = 0
    var durationSeconds: Double = 0
    var currentTrack: PlayableTrack? = nil
}

// MARK: - AudioEngine

class AudioEngine: ObservableObject {
    @Published var state = AudioEngineState()

    // MARK: Public callbacks

    var onComplete: (() -> Void)?
    var onSkipNext: (() -> Void)?
    var onSkipPrevious: (() -> Void)?

    // MARK: Internal state

    private var player: AVAudioPlayer?
    private var timer: Timer?
    private var remoteCommandsRegistered = false

    // MARK: - Public API

    /// Load a track for playback. Does not auto-play.
    func load(track: PlayableTrack) {
        release()
        state.status = "loading"

        guard let url = urlForSource(source: track.source) else {
            state.status = "error"
            return
        }

        do {
            try configureAudioSession()
            let audioPlayer = try AVAudioPlayer(contentsOf: url)
            guard audioPlayer.prepareToPlay() else {
                state.status = "error"
                return
            }
            player = audioPlayer
            state.currentTrack = track
            state.durationSeconds = track.durationMillis?.doubleValue ?? audioPlayer.duration
            if state.durationSeconds <= 0, audioPlayer.duration > 0 {
                state.durationSeconds = audioPlayer.duration
            }
            state.positionSeconds = 0
            updateNowPlaying(position: 0, rate: 0)
            state.status = "paused"
        } catch {
            state.status = "error"
            print("[AudioEngine] load error: \(error)")
        }
    }

    func play() {
        guard let player else { return }
        player.play()
        state.status = "playing"
        MPNowPlayingInfoCenter.default().playbackState = .playing
        updateNowPlaying(position: player.currentTime, rate: 1)
        startTimer()
    }

    func pause() {
        timer?.invalidate()
        player?.pause()
        state.status = "paused"
        MPNowPlayingInfoCenter.default().playbackState = .paused
        if let p = player {
            updateNowPlaying(position: p.currentTime, rate: 0)
        }
    }

    func stop() {
        timer?.invalidate()
        player?.stop()
        player?.currentTime = 0
        state.positionSeconds = 0
        state.status = "stopped"
        MPNowPlayingInfoCenter.default().playbackState = .stopped
        updateNowPlaying(position: 0, rate: 0)
    }

    func seek(to seconds: Double) {
        player?.currentTime = seconds
        if let p = player {
            state.positionSeconds = p.currentTime
            updateNowPlaying(position: p.currentTime, rate: p.isPlaying ? 1 : 0)
        }
    }

    func release() {
        timer?.invalidate()
        player?.stop()
        player = nil
        state.currentTrack = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        MPNowPlayingInfoCenter.default().playbackState = .stopped
    }

    // MARK: - Private helpers

    private func configureAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playback)
        try session.setActive(true)
        if !remoteCommandsRegistered {
            remoteCommandsRegistered = true
            registerRemoteCommands()
        }
    }

    private func registerRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.togglePlayPauseCommand.isEnabled = true
        center.stopCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true

        center.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self, let p = self.player else { return .noSuchContent }
            if p.isPlaying { self.pause() } else { self.play() }
            return .success
        }
        center.stopCommand.addTarget { [weak self] _ in
            self?.stop()
            return .success
        }
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let positionEvent = event as? MPChangePlaybackPositionCommandEvent else {
                return .noSuchContent
            }
            self?.seek(to: positionEvent.positionTime)
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            self?.onSkipPrevious?()
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            self?.onSkipNext?()
            return .success
        }
    }

    private func updateNowPlaying(position: Double, rate: Double) {
        guard let track = state.currentTrack else { return }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: max(0, position),
            MPNowPlayingInfoPropertyPlaybackRate: rate,
        ]
        if let album = track.album, !album.isEmpty {
            info[MPMediaItemPropertyAlbumTitle] = album
        }
        if state.durationSeconds > 0 {
            info[MPMediaItemPropertyPlaybackDuration] = state.durationSeconds
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { [weak self] _ in
            guard let self, let p = self.player else { return }
            let pos = p.currentTime
            self.state.positionSeconds = pos
            if p.isPlaying, self.state.durationSeconds > 0, pos >= self.state.durationSeconds {
                self.onComplete?()
            }
        }
    }

    private func urlForSource(source: AudioSource) -> URL? {
        if let fileSource = source as? AudioSourceFilePath {
            let path = fileSource.path
            if path.hasPrefix("/") {
                return URL(fileURLWithPath: path)
            }
            // Resolve relative path via app documents directory
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            return docs.appendingPathComponent(path)
        }
        if let uriSource = source as? AudioSourceUri {
            return URL(string: uriSource.value)
        }
        return nil
    }
}
