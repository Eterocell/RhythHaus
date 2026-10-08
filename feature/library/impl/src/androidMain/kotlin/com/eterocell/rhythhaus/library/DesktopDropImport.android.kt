package com.eterocell.rhythhaus.library

/**
 * Android has no desktop drag target and never admits desktop drop paths.
 *
 * @param paths ignored desktop filesystem paths.
 * @param createdAtEpochMillis ignored source creation timestamp.
 */
public actual fun validateDesktopDrop(
    paths: List<String>,
    createdAtEpochMillis: Long,
): DesktopDropResult =
    DesktopDropResult(
        sources = emptyList(),
        failures = emptyList(),
        duplicatePathCount = 0,
    )
