# Playlist Interoperability

## Purpose

Add interoperable M3U/M3U8 and PLS exchange for static saved playlists without weakening the existing RhythHaus JSON recovery format.

## Scope

This change covers local document import and export for static playlists. Smart playlist rules remain excluded from interoperable documents; the UI must explain that limitation. Imported entries are matched against the current library using effective metadata and are committed only after an explicit preview confirmation.
