package com.eterocell.rhythhaus

/** Resolves a relative local-audio path into an iOS-readable file path. */
public interface IOSRelativeFilePathResolver {
    /** Resolves [relativePath] from the app's local music directory. */
    public fun resolve(relativePath: String): String
}
