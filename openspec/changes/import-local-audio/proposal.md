# Local audio import

## Why

The playback slice can control real platform players, but the shared demo library still contains metadata-only tracks. Users need a first import path that creates playable `AudioSource.FilePath` or `AudioSource.Uri` tracks without implementing a full persistent library scanner yet.

## Scope

- Add a shared import contract and result model.
- Add shared UI affordance to import audio files.
- Implement first platform importers for Android, iOS, and macOS/JVM.
- Convert selected local files/URLs into `Track` values that feed the existing shared playback controller.
- Keep this foreground/manual import only; no background scanning, recursive library indexing, metadata extraction, persistence, or permission policy expansion yet.

## Out of scope

- Full MediaStore library scan.
- Recursive folder scan on macOS.
- iOS media library/MusicKit integration.
- Persistent library database/cache.
- Artwork and rich ID3 metadata extraction.
