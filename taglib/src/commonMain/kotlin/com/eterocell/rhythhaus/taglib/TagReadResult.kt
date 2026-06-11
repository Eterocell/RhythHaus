package com.eterocell.rhythhaus.taglib

sealed interface TagReadResult {
    data class Found(
        val metadata: TagMetadata,
        val format: TagFormat,
    ) : TagReadResult

    data class Unsupported(
        val reason: String,
    ) : TagReadResult

    data class Failed(
        val reason: String,
    ) : TagReadResult
}
