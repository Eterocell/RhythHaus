# Artwork CursorWindow Fix Report

## Scope

- Route: systematic-debugging + strict RED-GREEN TDD
- Base HEAD: `7a63f2eb227ff2a38c4cdfde068dc9fbe17c8a3a`
- Files in implementation scope:
  - `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
  - `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`
- No schema shape change or migration.

## Root cause

`SqlDelightLibraryRepository.artworkForTrack()` called the generated `selectArtworkForTrack` query, which selected the complete artwork BLOB into one Android cursor row. Android attempted to place the full value in a `CursorWindow` before Kotlin received it, so a newly encountered large artwork BLOB could throw `SQLiteBlobTooBigException` even though routine track-list queries already projected `NULL` artwork.

## RED evidence

Before production edits, `largeArtworkIsLoadedLazilyInMultipleBoundedChunks` was added with a deterministic `3 * 1024 * 1024 + 137` byte BLOB. It referenced the intended `256 KiB` contract, metadata query, chunk query, and chunk-count helper.

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.largeArtworkIsLoadedLazilyInMultipleBoundedChunks' --configuration-cache
```

Expected failure observed:

```text
Unresolved reference 'artworkChunkCount'
Unresolved reference 'ARTWORK_CHUNK_SIZE_BYTES'
Unresolved reference 'selectArtworkMetadataForTrack'
Unresolved reference 'selectArtworkChunkForTrack'
BUILD FAILED in 2s
```

This proved the regression test targeted the missing bounded-read implementation rather than existing behavior.

## GREEN implementation

- Replaced `selectArtworkForTrack` with:
  - `selectArtworkMetadataForTrack`: reads `length(artworkBytes)` and MIME only.
  - `selectArtworkChunkForTrack`: reads `CAST(substr(artworkBytes, startPosition, chunkLength) AS BLOB)` with SQLite's 1-based BLOB offset.
- Added a conservative `ARTWORK_CHUNK_SIZE_BYTES = 256 * 1024` contract.
- Rejected negative or greater-than-`Int.MAX_VALUE` lengths before allocation.
- Preallocated one exact-size `ByteArray` and copied each exact-size chunk into it with `copyInto`.
- Returned `null` for missing metadata, missing chunks, null length, or short chunks rather than returning partial/corrupt artwork.
- Preserved MIME and null/no-artwork behavior.

The regression BLOB requires 13 reads: twelve full 256 KiB chunks plus one 137-byte chunk. The test directly verifies the first and final chunk sizes, exact reassembled bytes, and `image/png` MIME.

## GREEN and verification evidence

Focused lazy test:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.largeArtworkIsLoadedLazilyInMultipleBoundedChunks' --configuration-cache
BUILD SUCCESSFUL in 5s
```

Full repository test class:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache
BUILD SUCCESSFUL in 6s
```

JVM/desktop/Android verification:

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
BUILD SUCCESSFUL in 10s
```

The only build warning was the pre-existing Android `MediaMetadata.Builder.setArtworkData` deprecation.

LSP diagnostics were requested for the changed Kotlin files, but `kotlin-ls` is not installed and the user previously declined installation. Kotlin compilation and all requested Gradle checks provide the available static verification.

## Concerns

- No live Android device was used against the originally crashing database; the SQL contract now guarantees each cursor result contains at most 256 KiB of artwork data.
- Artwork larger than `Int.MAX_VALUE` cannot be represented as a Kotlin `ByteArray`; retrieval safely returns `null` rather than allocating or truncating it.
