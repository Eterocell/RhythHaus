package com.eterocell.rhythhaus

expect fun createPlatformPlaybackEngine(): PlatformPlaybackEngine

/** Optional host capability for applying the persisted interruption policy. */
internal interface AutomaticResumePolicyTarget {
    fun setAutomaticResumeAllowed(allowed: Boolean)
}
