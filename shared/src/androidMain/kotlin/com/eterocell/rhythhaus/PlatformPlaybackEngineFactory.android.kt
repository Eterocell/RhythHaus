package com.eterocell.rhythhaus

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine =
    AndroidPolicyEngine(createAndroidPlaybackEngine())

private class AndroidPolicyEngine(
    private val delegate: PlatformPlaybackEngine,
) : PlatformPlaybackEngine by delegate, AutomaticResumePolicyTarget {
    override fun setAutomaticResumeAllowed(allowed: Boolean) {
        RhythHausPlaybackService.setAutomaticResumePolicyEnabled(allowed)
    }
}
