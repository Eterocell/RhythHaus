package com.eterocell.rhythhaus.taglib

sealed interface TagReadResult {
    data class Found(
        val metadata: TagMetadata,
    ) : TagReadResult

    data class Unsupported(
        val reason: String,
    ) : TagReadResult

    data class Failed(
        val reason: String,
    ) : TagReadResult
}
