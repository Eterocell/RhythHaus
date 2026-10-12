# Local and embedded lyrics

## Context

RhythHaus is local-first. It already scans embedded tags into the Library database, projects track data through Shared into Now Playing, and exposes playback position without requiring platform-specific Now Playing navigation. It has no lyrics UI. Android MediaStore exposes audio content, while SAF, iOS app-local, and JVM folder sources expose directory siblings.

## Goals

- Show a track's locally available timed or plain lyrics in Now Playing.
- Prefer one readable same-stem `.lrc` file to embedded `LYRICS` metadata.
- Persist the bounded selected text during explicit scans so playback needs no file access.
- Preserve track/source identity and current playback behavior.
- Support English and Simplified Chinese labels and accessible timed-line state.

## Non-goals

- Remote APIs, accounts, lyric search, editing, media writes, multiple language selection, or unsupervised scans.
- Sidecar lookup through Android MediaStore.
- A separate lyrics database/table or source permission lifecycle.

## Contract decisions

1. Persist nullable `lyrics TEXT` on `library_track`, with a schema migration and existing version-zero handling.
2. Normalize non-empty lyric text and allow at most 512 KiB UTF-8. Reject oversize content as unavailable; do not truncate.
3. Match one sibling file whose stem equals the audio stem and whose extension is `.lrc`, case-insensitively. The sidecar wins over embedded lyrics. If it cannot be read or exceeds the limit, use valid embedded text.
4. TagLib's normalized `LYRICS` property is the embedded source. Android/JVM already expose generic property maps; iOS must map the property through the existing native C metadata ABI. URI/fd extraction must reuse the existing open descriptor and close lifecycle.
5. Sidecar access is performed only inside authorized scan paths. iOS and JVM use the exact sibling path; Android SAF matches the exact sibling in the already-listed directory. Android MediaStore never probes arbitrary paths.
6. An explicit scan rereads lyric metadata for each readable audio candidate even when the audio file's known size/time facts are unchanged. A failure to read optional lyrics does not fail or skip audio.
7. The common parser recognizes `[mm:ss.xx]` and `[mm:ss.xxx]`, multiple tags per line, and a signed global millisecond offset. It retains untimed lines, ignores malformed timing syntax as timing, clamps adjusted timestamps to zero, and uses source order for equal-time stability.
8. Now Playing offers an optional localized action only for non-empty lyrics. A scrollable surface renders plain text or timed lines, marks the active timed line from current playback position, and never calls the controller.

## Risks and checks

- Container support for the normalized TagLib `LYRICS` property differs. Verify at least one generated native tag fixture and keep unsupported media readable without lyrics.
- Sidecars can change while audio facts stay constant. Regression must prove explicit rescan refreshes lyrics without a new track ID.
- A 512 KiB per-track bound limits storage growth but still permits larger than ordinary documents; database smoke verifies actual close/reopen.
- Timed highlighting must not auto-seek or mutate queue/session state; semantic tests and a before/after playback snapshot cover this.
- User performs manual compact/wide visual and restart acceptance; automated verification remains implementation-owned.
