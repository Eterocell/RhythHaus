package com.eterocell.rhythhaus.library.ui

/** Immutable native drag presentation; admission remains App-owned. */
data class DesktopDropTargetPresentation(
    val isAvailable: Boolean,
    val isActive: Boolean,
    val feedback: String? = null,
) {
    companion object {
        val Disabled = DesktopDropTargetPresentation(false, false)
    }
}
