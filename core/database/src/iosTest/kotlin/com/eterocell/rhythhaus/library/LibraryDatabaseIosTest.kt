package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertFails

class LibraryDatabaseIosTest {
    @Test
    fun productionIosFactoryRejectsInvalidPlaylistEntryForeignKeys() {
        val database = createLibraryDatabase()
        try {
            assertFails {
                database.database.playlistQueries.insertEntry(
                    id = "invalid-entry",
                    playlistId = "missing-playlist",
                    trackId = "missing-track",
                    position = 0,
                    createdAtEpochMillis = 1,
                )
            }
        } finally {
            database.driver.close()
        }
    }
}
