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

    private var player: AVPlayer?
    private var timeObserver: Any?
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

        print("[AudioEngine] Loading: \(track.title) from \(url.path)")
        do {
            try configureAudioSession()
        } catch {
            state.status = "error"
            print("[AudioEngine] audio session error: \(error)")
            return
        }

        let playerItem = AVPlayerItem(url: url)
        let avPlayer = AVPlayer(playerItem: playerItem)
        self.player = avPlayer
        state.currentTrack = track
        state.positionSeconds = 0

        // Completion via notification
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(playerDidFinishPlaying),
            name: .AVPlayerItemDidPlayToEndTime,
            object: playerItem
        )

        // Periodic time observer (replaces Timer)
        let interval = CMTime(seconds: 0.25, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserver = avPlayer.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self else { return }
            let pos = CMTimeGetSeconds(time)
            self.state.positionSeconds = pos
            let dur = CMTimeGetSeconds(avPlayer.currentItem?.duration ?? .zero)
            if dur > 0, self.state.durationSeconds <= 0 {
                self.state.durationSeconds = dur
            }
            if dur > 0, pos >= dur - 0.25 {
                self.onComplete?()
            }
        }

        state.status = "paused"
        updateNowPlaying(position: 0, rate: 0)
    }

    func play() {
        guard let player else { return }
        player.play()
        player.rate = 1.0
        state.status = "playing"
        MPNowPlayingInfoCenter.default().playbackState = .playing
        updateNowPlaying(position: state.positionSeconds, rate: 1)
    }

    func pause() {
        player?.pause()
        player?.rate = 0
        state.status = "paused"
        MPNowPlayingInfoCenter.default().playbackState = .paused
        updateNowPlaying(position: state.positionSeconds, rate: 0)
    }

    func stop() {
        player?.pause()
        player?.seek(to: .zero)
        state.positionSeconds = 0
        state.status = "stopped"
        MPNowPlayingInfoCenter.default().playbackState = .stopped
        updateNowPlaying(position: 0, rate: 0)
    }

    func seek(to seconds: Double) {
        let time = CMTime(seconds: seconds, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player?.seek(to: time)
        state.positionSeconds = seconds
        updateNowPlaying(position: seconds, rate: state.status == "playing" ? 1 : 0)
    }

    func release() {
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
            timeObserver = nil
        }
        NotificationCenter.default.removeObserver(self)
        player?.pause()
        player = nil
        state.currentTrack = nil
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        MPNowPlayingInfoCenter.default().playbackState = .stopped
    }

    // MARK: - Completion

    @objc private func playerDidFinishPlaying(_ notification: Notification) {
        onComplete?()
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
            guard let self else { return .noSuchContent }
            if self.state.status == "playing" { self.pause() } else { self.play() }
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

    private func urlForSource(source: AudioSource) -> URL? {
        if let fileSource = source as? AudioSourceFilePath {
            let path = fileSource.path
            if path.hasPrefix("/") {
                return URL(fileURLWithPath: path)
            }
            // Resolve relative path via app documents directory (string concat, same as old Kotlin iosUrl)
            let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let docsPath = docs.path
                .replacingOccurrences(of: "/private/var", with: "/var")
                .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            let resolved = "\(docsPath)/\(path)"
            print("[AudioEngine] resolved path: \(resolved)")
            return URL(fileURLWithPath: resolved)
        }
        if let uriSource = source as? AudioSourceUri {
            return URL(string: uriSource.value)
        }
        return nil
    }
}
