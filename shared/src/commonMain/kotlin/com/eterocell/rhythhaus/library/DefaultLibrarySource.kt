package com.eterocell.rhythhaus.library

/**
 * Returns the platform's built-in library source, if it has one.
 *
 * The iOS implementation exposes RhythHaus's Files-visible Documents directory
 * as one managed source. Other platforms leave their existing
 * user-selected-source lifecycle unchanged.
 */
internal expect fun defaultPlatformLibrarySource(): LibrarySource?

/**
 * Persists [defaultSource] only when that source identity is absent.
 *
 * Returning the inserted source gives the App owner the exact source to scan;
 * returning null preserves the existing source handle and avoids a redundant
 * first-run scan after process restart.
 */
internal fun registerMissingDefaultLibrarySource(
    repository: LibraryRepository,
    defaultSource: LibrarySource,
): LibrarySource? {
    if (repository.sources().any { it.id == defaultSource.id }) return null
    repository.upsertSource(defaultSource)
    return defaultSource
}
