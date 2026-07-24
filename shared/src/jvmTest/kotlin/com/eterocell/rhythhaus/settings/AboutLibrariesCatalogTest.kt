package com.eterocell.rhythhaus.settings

import com.mikepenz.aboutlibraries.Libs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
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
            val json = readAboutLibrariesCatalogJson()
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

    @Test
    fun loadAboutLibrariesReturnsLoadedWhenReadAndParseSucceed() = runBlocking {
        val state =
            loadAboutLibraries(readJson = { readAboutLibrariesCatalogJson() })

        val loaded = assertIs<AboutLibrariesLoadState.Loaded>(state)
        assertTrue(loaded.libraries.libraries.isNotEmpty())
    }

    @Test
    fun loadAboutLibrariesReturnsFailedWhenResourceReadThrows() = runBlocking {
        val readFailure = IllegalStateException("resource missing")
        val state = loadAboutLibraries(readJson = { throw readFailure })

        val failed = assertIs<AboutLibrariesLoadState.Failed>(state)
        assertTrue(failed.cause === readFailure)
    }

    @Test
    fun loadAboutLibrariesReturnsFailedWhenParsedCatalogIsEmpty() =
        runBlocking<Unit> {
            // com.mikepenz.aboutlibraries.Libs.Builder.build() catches its own
            // internal parse
            // failures and yields an empty Libs rather than throwing on
            // malformed JSON. An empty
            // attribution catalog is not a valid successful screen, so
            // loadAboutLibraries must
            // treat it as Failed rather than Loaded.
            val state = loadAboutLibraries(readJson = { "not valid json" })

            assertIs<AboutLibrariesLoadState.Failed>(state)
        }

    @Test
    fun loadAboutLibrariesRethrowsCancellationInsteadOfWrappingAsFailed() =
        runBlocking<Unit> {
            assertFailsWith<CancellationException> {
                loadAboutLibraries(
                    readJson = { throw CancellationException("cancelled") })
            }
        }
}
