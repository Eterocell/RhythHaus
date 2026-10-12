package com.eterocell.rhythhaus.playback

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

class GaplessPreferenceOwnerJvmTest {
    @Test
    fun defaultIsOffAndAcceptedModeSurvivesRestart() = runBlocking {
        val root = Files.createTempDirectory("gapless-preferences").toFile()
        val firstJob = SupervisorJob()
        try {
            val first = DataStoreGaplessPreferenceStore(
                PreferenceDataStoreFactory.createWithPath(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    migrations = emptyList(),
                    scope = CoroutineScope(firstJob + Dispatchers.IO),
                    produceFile = { root.resolve("gapless.preferences_pb").toOkioPath() },
                ),
            )
            assertEquals(GaplessPlaybackMode.Off, first.readMode())
            first.setMode(GaplessPlaybackMode.On)
            firstJob.cancelAndJoin()

            val reopenedJob = SupervisorJob()
            try {
                val reopened = DataStoreGaplessPreferenceStore(
                    PreferenceDataStoreFactory.createWithPath(
                        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                        migrations = emptyList(),
                        scope = CoroutineScope(reopenedJob + Dispatchers.IO),
                        produceFile = { root.resolve("gapless.preferences_pb").toOkioPath() },
                    ),
                )
                val applied = mutableListOf<GaplessPlaybackMode>()
                val owner = GaplessPreferenceOwner(reopened, applied::add)
                assertEquals(GaplessPlaybackMode.Off, owner.state.value.selectedMode)
                owner.restore()
                assertEquals(GaplessPlaybackMode.On, owner.state.value.selectedMode)
                assertEquals(listOf(GaplessPlaybackMode.On), applied)
                assertFalse(owner.state.value.isBusy)
            } finally {
                reopenedJob.cancelAndJoin()
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
