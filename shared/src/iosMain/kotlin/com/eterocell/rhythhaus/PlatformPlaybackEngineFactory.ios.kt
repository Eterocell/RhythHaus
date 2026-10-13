package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.impl.appLocalMusicFolderPath

actual fun createPlatformPlaybackEngine(): PlatformPlaybackEngine =
    createIOSPlaybackEngine(
            object : IOSRelativeFilePathResolver {
                override fun resolve(relativePath: String): String =
                    "${appLocalMusicFolderPath()}/$relativePath"
            },
        )
        .let { engine ->
            IOSPolicyEngine(engine)
        }

private class IOSPolicyEngine(
    private val delegate: PlatformPlaybackEngine,
) : PlatformPlaybackEngine by delegate, AutomaticResumePolicyTarget {
    override fun setAutomaticResumeAllowed(allowed: Boolean) {
        (delegate as? IOSPlaybackEngine)?.setAutomaticResumeAllowed(allowed)
    }
}
