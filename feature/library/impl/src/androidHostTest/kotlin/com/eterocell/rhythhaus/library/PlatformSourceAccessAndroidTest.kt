package com.eterocell.rhythhaus.library

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.eterocell.rhythhaus.library.impl.AndroidSafSourceAccess
import com.eterocell.rhythhaus.library.impl.createPlatformSourceAccess
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertEquals

class PlatformSourceAccessAndroidTest {
    @Test
    fun factoryWiresTheApplicationContextIntoTheSafAccess() {
        val application = Application()
        LibraryDatabaseContext.applicationContext =
            ApplicationContextWrapper(application)

        val access = createPlatformSourceAccess()

        assertIs<AndroidSafSourceAccess>(access)
        val contextField =
            AndroidSafSourceAccess::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        assertSame(application, contextField.get(access))
    }

    @Test
    fun nonSafSourcesReportLostAccessAndGuardScanAndRelease() {
        val application = Application()
        val access = AndroidSafSourceAccess(application)
        val foreign =
            LibrarySource(
                id = "jvm",
                platformKind = LibraryPlatformKind.JvmFolder,
                displayName = "Music",
                handle = "/tmp/music",
                createdAtEpochMillis = 1,
            )

        assertEquals(
            LibrarySourceAccessStatus.LostAccess,
            access.accessStatus(foreign),
        )
        assertFails { access.scan(foreign).toList() }
        access.releaseAccess(foreign)
    }
}

private class ApplicationContextWrapper(
    private val application: Context,
) : ContextWrapper(application) {
    override fun getApplicationContext(): Context = application
}

