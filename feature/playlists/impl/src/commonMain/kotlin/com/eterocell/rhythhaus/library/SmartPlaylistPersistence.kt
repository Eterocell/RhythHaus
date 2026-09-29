package com.eterocell.rhythhaus.library

internal data class StoredSmartRule(
    val kind: String,
    val argument: String? = null,
    val secondArgument: String? = null,
    val count: Long? = null
)

internal fun SmartPlaylistRule.encode(): StoredSmartRule =
    when (this) {
        SmartPlaylistRule.Favorites -> StoredSmartRule("favorites")
        is SmartPlaylistRule.RecentlyPlayed ->
            StoredSmartRule("recently-played", count = validCount(count))
        is SmartPlaylistRule.RecentlyAdded ->
            StoredSmartRule("recently-added", count = validCount(count))
        is SmartPlaylistRule.Artist ->
            StoredSmartRule("artist", argument = nonBlank(artist))
        is SmartPlaylistRule.Album ->
            StoredSmartRule("album", nonBlank(artist), nonBlank(album))
        is SmartPlaylistRule.SavedPlaylist ->
            StoredSmartRule("saved-playlist", argument = nonBlank(playlistId))
    }

private fun nonBlank(value: String): String = value.also {
    require(it.isNotBlank())
}

private fun validCount(value: Int): Long =
    value.toLong().also { require(value in setOf(10, 25, 50)) }

internal fun decodeSmartRule(
    kind: String,
    argument: String?,
    secondArgument: String?,
    count: Long?
): SmartPlaylistRule {
    val rule =
        when (kind) {
            "favorites" -> SmartPlaylistRule.Favorites
            "recently-played" ->
                SmartPlaylistRule.RecentlyPlayed(
                    requireNotNull(count)
                        .also { require(it in listOf(10L, 25L, 50L)) }
                        .toInt())
            "recently-added" ->
                SmartPlaylistRule.RecentlyAdded(
                    requireNotNull(count)
                        .also { require(it in listOf(10L, 25L, 50L)) }
                        .toInt())
            "artist" -> SmartPlaylistRule.Artist(requireNotNull(argument))
            "album" ->
                SmartPlaylistRule.Album(
                    requireNotNull(argument), requireNotNull(secondArgument))
            "saved-playlist" ->
                SmartPlaylistRule.SavedPlaylist(requireNotNull(argument))
            else -> error("Invalid smart playlist rule: $kind")
        }
    require(
        rule.encode() ==
            StoredSmartRule(kind, argument, secondArgument, count)) {
            "Invalid smart playlist arguments"
        }
    return rule
}

internal fun PlaylistRepository.validateSmartRule(
    rule: SmartPlaylistRule,
    previous: SmartPlaylistRule? = null
) {
    rule.encode()
    if (rule is SmartPlaylistRule.SavedPlaylist && rule != previous)
        requireNotNull(playlist(rule.playlistId)) {
            "Source playlist not found"
        }
}
