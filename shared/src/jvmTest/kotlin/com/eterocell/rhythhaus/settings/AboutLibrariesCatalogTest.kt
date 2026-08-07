package com.eterocell.rhythhaus.settings

import com.mikepenz.aboutlibraries.Libs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import rhythhaus.shared.generated.resources.Res

class AboutLibrariesCatalogTest {
    @Test
    fun checkedInCatalogParsesAndContainsLibraries() = runBlocking {
        val json = Res.readBytes("files/aboutlibraries.json").decodeToString()
        val libraries = Libs.Builder().withJson(json).build()

        assertTrue(libraries.libraries.isNotEmpty())
    }

    @Test
    fun uiConsumedCatalogJsonParsesAndContainsDisplayableLibraries() =
        runBlocking {
            val json =
                Res.readBytes("files/aboutlibraries.json").decodeToString()
            val libraries = Libs.Builder().withJson(json).build()

            assertTrue(libraries.libraries.isNotEmpty())
            assertTrue(libraries.libraries.all { it.name.isNotBlank() })
        }

    @Test
    fun checkedInCatalogAttributesNativeTagLibDependency() = runBlocking {
        val json = Res.readBytes("files/aboutlibraries.json").decodeToString()
        val libraries = Libs.Builder().withJson(json).build()

        val tagLib = libraries.libraries.find { it.uniqueId == "taglib:taglib" }
        assertTrue(
            tagLib != null,
            "expected a manually-attributed taglib:taglib entry")
        assertTrue(
            tagLib.licenses.isNotEmpty(),
            "expected taglib:taglib to declare its license(s)")
        val licenseContents =
            tagLib.licenses.map { it.licenseContent.orEmpty() }
        assertTrue(
            licenseContents.any {
                it.contains("GNU LESSER GENERAL PUBLIC LICENSE")
            },
            "expected taglib:taglib to attribute the full LGPL-2.1 license text",
        )
        assertTrue(
            licenseContents.any { it.contains("MOZILLA PUBLIC LICENSE") },
            "expected taglib:taglib to attribute the full MPL-1.1 license text",
        )
    }
}
