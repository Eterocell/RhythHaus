package com.eterocell.rhythhaus.library

data class PlaylistEntry(
    val id: String,
    val playlistId: String,
    val trackId: String,
    val position: Int,
    val createdAtEpochMillis: Long,
)

data class PlaylistImportMutation(
    val name: String,
    val trackIds: List<String>,
)

interface PlaylistRepository {
    fun playlists(): List<Playlist>
    fun playlist(id: String): Playlist?
    fun entries(playlistId: String): List<PlaylistEntry>
    fun create(name: String): Playlist
    fun createWithEntries(name: String, trackIds: List<String>): Playlist
    fun importPlaylists(playlists: List<PlaylistImportMutation>): List<Playlist>
    fun rename(id: String, name: String)
    fun delete(id: String)
    fun append(playlistId: String, trackIds: List<String>)
    fun removeEntry(entryId: String)
    fun reorder(playlistId: String, entryIds: List<String>)
}

class InMemoryPlaylistRepository(
    private val now: () -> Long = ::currentTimeMillis,
    private val idFactory: () -> String = ::uuid4,
) : PlaylistRepository {
    private var playlists = linkedMapOf<String, Playlist>()
    private var entries = linkedMapOf<String, PlaylistEntry>()

    override fun playlists(): List<Playlist> = playlists.values.sortedWith(
        compareBy<Playlist> { it.createdAtEpochMillis }.thenBy(Playlist::id),
    )

    override fun playlist(id: String): Playlist? = playlists[id]

    override fun entries(playlistId: String): List<PlaylistEntry> = entries.values
        .filter { it.playlistId == playlistId }
        .sortedBy(PlaylistEntry::position)

    override fun create(name: String): Playlist {
        val timestamp = now()
        val playlist = Playlist(idFactory(), requireName(name), timestamp, timestamp)
        playlists[playlist.id] = playlist
        return playlist
    }

    override fun createWithEntries(name: String, trackIds: List<String>): Playlist {
        val timestamp = now()
        val playlist = Playlist(idFactory(), requireName(name), timestamp, timestamp)
        val initialEntries = trackIds.mapIndexed { position, trackId ->
            PlaylistEntry(idFactory(), playlist.id, trackId, position, now())
        }
        playlists[playlist.id] = playlist
        initialEntries.forEach { entry -> entries[entry.id] = entry }
        return playlist
    }

    override fun importPlaylists(playlists: List<PlaylistImportMutation>): List<Playlist> {
        val validated = validatePlaylistImports(playlists)
        if (validated.isEmpty()) return emptyList()

        val stagedPlaylists = LinkedHashMap(this.playlists)
        val stagedEntries = LinkedHashMap(entries)
        val imported = validated.map { mutation ->
            val timestamp = now()
            val playlist = Playlist(idFactory(), mutation.name, timestamp, timestamp)
            stagedPlaylists[playlist.id] = playlist
            mutation.trackIds.forEachIndexed { position, trackId ->
                val entry = PlaylistEntry(idFactory(), playlist.id, trackId, position, now())
                stagedEntries[entry.id] = entry
            }
            playlist
        }
        this.playlists = stagedPlaylists
        entries = stagedEntries
        return imported
    }

    override fun rename(id: String, name: String) {
        val playlist = requireNotNull(playlists[id]) { "Playlist not found: $id" }
        playlists[id] = playlist.copy(name = requireName(name), updatedAtEpochMillis = now())
    }

    override fun delete(id: String) {
        playlists.remove(id)
        entries.entries.removeAll { it.value.playlistId == id }
    }

    override fun append(playlistId: String, trackIds: List<String>) {
        require(playlists.containsKey(playlistId)) { "Playlist not found: $playlistId" }
        val replacement = entries(playlistId).toMutableList()
        trackIds.forEach { trackId ->
            replacement += PlaylistEntry(idFactory(), playlistId, trackId, replacement.size, now())
        }
        replaceEntries(playlistId, replacement)
    }

    override fun removeEntry(entryId: String) {
        val entry = requireNotNull(entries[entryId]) { "Playlist entry not found: $entryId" }
        replaceEntries(entry.playlistId, entries(entry.playlistId).filterNot { it.id == entryId })
    }

    override fun reorder(playlistId: String, entryIds: List<String>) {
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

    private fun replaceEntries(playlistId: String, replacement: List<PlaylistEntry>) {
        entries.entries.removeAll { it.value.playlistId == playlistId }
        replacement.forEachIndexed { position, entry -> entries[entry.id] = entry.copy(position = position) }
    }
}

internal fun requireName(name: String): String = name.trim().also {
    require(it.isNotEmpty()) { "Playlist name must not be blank" }
}

internal fun validatePlaylistImports(playlists: List<PlaylistImportMutation>): List<PlaylistImportMutation> =
    playlists.map { mutation ->
        val name = requireName(mutation.name)
        require(mutation.trackIds.isNotEmpty()) { "Imported playlist track IDs must not be empty" }
        require(mutation.trackIds.all { it.isNotBlank() }) { "Imported playlist track IDs must not be blank" }
        mutation.copy(name = name, trackIds = mutation.trackIds.toList())
    }
