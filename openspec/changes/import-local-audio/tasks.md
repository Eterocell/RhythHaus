## 1. Shared import model

- [x] 1.1 Add shared imported-audio models and import result/error state.
- [x] 1.2 Add shared mapper from imported audio files to `Track` / `PlayableTrack` values.
- [x] 1.3 Add common tests for title derivation, stable ids, source mapping, and merging demo/imported tracks.

## 2. Platform importers

- [x] 2.1 Add Android Activity Result document picker integration in `androidApp` and pass importer into shared `App`.
- [x] 2.2 Add macOS/JVM Swing `JFileChooser` importer for audio files.
- [x] 2.3 Add iOS unsupported importer state with clear UI copy and leave Swift/UIKit bridge as a follow-up.

## 3. Shared UI integration

- [x] 3.1 Add shared import button/status to the library surface.
- [x] 3.2 Replace metadata-only demo playback queue with imported playable tracks when imports exist.
- [x] 3.3 Keep demo tracks visible when no local files have been imported.
- [x] 3.4 Show user-visible import errors/limitations.

## 4. Verification and handoff

- [x] 4.1 Run `./init.sh`.
- [x] 4.2 Update `progress.md` with evidence, limitations, and next safe action.
- [x] 4.3 Validate this OpenSpec change.
