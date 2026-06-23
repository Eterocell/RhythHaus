package com.eterocell.rhythhaus.taglib

data class WriteMeta(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val comment: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val properties: Map<String, String> = emptyMap(),
)

sealed interface WriteResult {
    data object Success : WriteResult
    data class Unsupported(val reason: String) : WriteResult
    data class Failed(val reason: String) : WriteResult
}

interface TagLibWriter {
    fun writePath(path: String, meta: WriteMeta): WriteResult
}

expect fun createTagLibWriter(): TagLibWriter
