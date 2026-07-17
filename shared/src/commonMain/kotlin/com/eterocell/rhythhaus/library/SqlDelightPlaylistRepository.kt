package com.eterocell.rhythhaus.library

class SqlDelightPlaylistRepository(
    libraryDatabase: LibraryDatabase,
    private val now: () -> Long = ::currentTimeMillis,
    private val idFactory: () -> String = ::uuid4,
) : PlaylistRepository {
    private val database = libraryDatabase.database
    internal var mutationReadObserver: () -> Unit = {}

    override fun playlists(): List<Playlist> = database.playlistQueries.selectAllPlaylists(::playlistFrom).executeAsList()

    override fun playlist(id: String): Playlist? = database.playlistQueries.selectPlaylist(id, ::playlistFrom).executeAsOneOrNull()

    override fun entries(playlistId: String): List<PlaylistEntry> = database.playlistQueries
        .selectEntries(playlistId) { id, playlistId_, trackId, position, createdAtEpochMillis ->
            PlaylistEntry(id, playlistId_, trackId, position.toInt(), createdAtEpochMillis)
        }
        .executeAsList()

    override fun create(name: String): Playlist {
        val timestamp = now()
        val playlist = Playlist(idFactory(), requireName(name), timestamp, timestamp)
        database.playlistQueries.insertPlaylist(playlist.id, playlist.name, timestamp, timestamp)
        return playlist
    }

    override fun rename(id: String, name: String) {
        requireNotNull(playlist(id)) { "Playlist not found: $id" }
        database.playlistQueries.renamePlaylist(requireName(name), now(), id)
    }

    override fun delete(id: String) {
        database.playlistQueries.deletePlaylist(id)
    }

    override fun append(playlistId: String, trackIds: List<String>) {
        database.transaction {
            mutationReadObserver()
            requireNotNull(playlist(playlistId)) { "Playlist not found: $playlistId" }
            val replacement = entries(playlistId).toMutableList()
            trackIds.forEach { trackId ->
                replacement += PlaylistEntry(idFactory(), playlistId, trackId, replacement.size, now())
            }
            replaceEntries(playlistId, replacement)
        }
    }

    override fun removeEntry(entryId: String) {
        database.transaction {
            mutationReadObserver()
            val entry = database.playlistQueries.selectEntry(entryId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Playlist entry not found: $entryId")
            val replacement = entries(entry.playlistId).filterNot { it.id == entryId }
            replaceEntries(entry.playlistId, replacement)
        }
    }

    override fun reorder(playlistId: String, entryIds: List<String>) {
        database.transaction {
            mutationReadObserver()
            val current = entries(playlistId)
            require(entryIds.size == current.size && entryIds.toSet().size == current.size) {
                "Reorder must contain every playlist entry exactly once"
            }
            val byId = current.associateBy(PlaylistEntry::id)
            replaceEntries(
                playlistId,
                entryIds.map { id -> requireNotNull(byId[id]) { "Playlist entry not found: $id" } },
            )
        }
    }

    private fun replaceEntries(playlistId: String, replacement: List<PlaylistEntry>) {
        database.playlistQueries.clearEntriesForPlaylist(playlistId)
        replacement.forEachIndexed { position, entry ->
            database.playlistQueries.insertEntry(
                entry.id,
                playlistId,
                entry.trackId,
                position.toLong(),
                entry.createdAtEpochMillis,
            )
        }
    }
}

private fun playlistFrom(
    id: String,
    name: String,
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long,
) = Playlist(id, name, createdAtEpochMillis, updatedAtEpochMillis)
