package com.eterocell.rhythhaus.settings

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class SettingsResourceOwnershipJvmTest {
    @Test
    public fun settingsResourcesHaveExactEnZhParityAndOwnership() {
        val root = repositoryRoot()
        val featureEn = resourceNames(requiredFile(root, featureEnPath))
        val featureZh = resourceNames(requiredFile(root, featureZhPath))
        val sharedEn = resourceNames(requiredFile(root, sharedEnPath))
        val sharedZh = resourceNames(requiredFile(root, sharedZhPath))

        assertExactMultiset("feature EN", featureKeys, featureEn)
        assertExactMultiset("feature ZH", featureKeys, featureZh)
        assertEquals(
            featureEn,
            featureZh,
            "Feature EN/ZH resource names must have exact parity.")

        assertExactMultiset(
            "Shared retained EN",
            sharedRetainedKeys,
            sharedEn.filter { it in sharedRetainedKeys })
        assertExactMultiset(
            "Shared retained ZH",
            sharedRetainedKeys,
            sharedZh.filter { it in sharedRetainedKeys })
        assertEquals(
            sharedEn.filter { it in sharedRetainedKeys },
            sharedZh.filter { it in sharedRetainedKeys },
            "Shared retained EN/ZH resource names must have exact parity.")

        val forbiddenInShared =
            featureKeys.intersect(sharedEn.toSet() + sharedZh.toSet())
        assertTrue(
            forbiddenInShared.isEmpty(),
            "Settings-owned keys must not remain in Shared: $forbiddenInShared",
        )
        val sharedNames = sharedEn.toSet() + sharedZh.toSet()
        assertTrue(
            sharedRetainedKeys.all { it in sharedNames },
            "Shared retained ownership keys must remain in both locale files.",
        )
    }

    @Test
    public fun settingsLogoHasOneFeatureOwnerAndNoForeignResImport() {
        val root = repositoryRoot()
        val featureLogo = requiredFile(root, featureLogoPath)
        val sharedLogo = root.resolve(sharedLogoPath)
        assertTrue(
            featureLogo.isRegularFile(), "Feature must own rhythhaus_logo.")
        assertFalse(
            sharedLogo.exists(),
            "Shared must not retain rhythhaus_logo: $sharedLogo")

        val sourceRoot = root.resolve(featureSourcePath)
        assertTrue(
            sourceRoot.isDirectory(),
            "Settings production source directory is missing: $sourceRoot")
        val sourceFiles =
            Files.walk(sourceRoot).use { paths ->
                paths
                    .filter {
                        it.isRegularFile() &&
                            it.fileName.toString().endsWith(".kt")
                    }
                    .toList()
            }
        assertTrue(
            sourceFiles.isNotEmpty(),
            "Settings production source files are missing: $sourceRoot")
        val foreignImports = sourceFiles.flatMap { file ->
            Files.readAllLines(file)
                .filter { line ->
                    val imported = line.trim().removePrefix("import ")
                    imported.contains(".generated.resources") &&
                        approvedGeneratedResourcePrefixes.none {
                            imported.startsWith(it)
                        }
                }
                .map { line -> "$file: ${line.trim()}" }
        }
        assertTrue(
            foreignImports.isEmpty(),
            "Settings production must not import foreign generated Res symbols: $foreignImports",
        )

        assertBuildInfoGeneratorOwnership(
            conventionBuild = requiredFile(root, conventionBuildPath),
            sharedBuild = requiredFile(root, sharedBuildPath),
        )
    }

    private fun repositoryRoot(): Path {
        val configured = System.getProperty("rhythhaus.rootDir")
        require(!configured.isNullOrBlank()) {
            "rhythhaus.rootDir must name the repository root; refusing to inspect an implicit path."
        }
        val root = Path.of(configured).toAbsolutePath().normalize()
        require(root.isDirectory()) {
            "rhythhaus.rootDir is not a directory: $root"
        }
        return root
    }

    private fun requiredFile(root: Path, relativePath: String): Path =
        root.resolve(relativePath).also {
            require(it.isRegularFile()) {
                "Expected repository file is missing: $it"
            }
        }

    private fun resourceNames(file: Path): List<String> {
        val document =
            DocumentBuilderFactory.newInstance()
                .apply {
                    isNamespaceAware = false
                    setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true)
                }
                .newDocumentBuilder()
                .parse(file.toFile())
        return (0 until document.getElementsByTagName("string").length).map {
            index ->
            document
                .getElementsByTagName("string")
                .item(index)
                .attributes
                .getNamedItem("name")
                ?.nodeValue ?: error("String without name in $file")
        }
    }

    private fun assertExactMultiset(
        label: String,
        expected: Set<String>,
        actual: List<String>
    ) {
        assertEquals(
            expected.size,
            actual.size,
            "$label must contain exactly ${expected.size} keys: $actual")
        assertEquals(
            expected,
            actual.toSet(),
            "$label has missing or foreign keys: $actual")
        assertTrue(
            actual.groupingBy { it }.eachCount().values.all { it == 1 },
            "$label has duplicate keys: $actual")
    }

    private fun assertBuildInfoGeneratorOwnership(
        conventionBuild: Path,
        sharedBuild: Path
    ) {
        val conventionCode = buildScriptCode(conventionBuild)
        val missingConventionClauses =
            buildInfoOwnershipClauses.filterNot(conventionCode::contains)
        assertTrue(
            missingConventionClauses.isEmpty(),
            "The build-info convention must own every RhythHausBuildInfo generator clause; missing: $missingConventionClauses",
        )

        val sharedCode = buildScriptCode(sharedBuild)
        val duplicatedSharedClauses =
            buildInfoOwnershipClauses.filter(sharedCode::contains)
        assertTrue(
            duplicatedSharedClauses.isEmpty(),
            "Shared must not own RhythHausBuildInfo generation; remove: $duplicatedSharedClauses",
        )
    }

    private fun buildScriptCode(file: Path): String {
        val text = Files.readString(file)
        return text
            .replace(
                Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL)), "")
            .lineSequence()
            .map { line -> line.substringBefore("//") }
            .joinToString("\n")
    }

    private companion object {
        val featureKeys =
            setOf(
                "appearance",
                "theme_system_label",
                "theme_light_label",
                "theme_dark_label",
                "theme_system_description",
                "theme_light_description",
                "theme_dark_description",
                "manage_music",
                "configured_folders",
                "unnamed_folder",
                "source_access_available",
                "source_access_lost",
                "source_never_scanned",
                "source_last_scanned",
                "source_status_format",
                "rescan_source_format",
                "recover_source_format",
                "remove_source_format",
                "remove_folder",
                "remove_folder_message",
                "about",
                "about_app_name",
                "about_logo_description",
                "about_version_format",
                "about_view_source",
                "about_open_source_libraries",
                "open_source_libraries_loading",
                "open_source_libraries_error",
                "open_source_libraries_retry",
            )
        val sharedRetainedKeys =
            setOf(
                "settings",
                "add_music_folder",
                "folder_picker_unavailable",
                "clear_library",
                "clear_library_message",
                "clear",
                "cancel",
                "remove",
                "close",
                "scan_complete_format",
            )
        val approvedGeneratedResourcePrefixes =
            setOf(
                "rhythhaus.feature.settings.generated.resources.",
                "rhythhaus.core.ui.generated.resources.",
            )
        val buildInfoOwnershipClauses =
            setOf(
                "abstract class GenerateRhythHausBuildInfoTask : DefaultTask()",
                "tasks.register<GenerateRhythHausBuildInfoTask>(",
                "\"generateRhythHausBuildInfo\")",
                "layout.buildDirectory.dir(\"generated/rhythHausBuildInfo/commonMain/kotlin\")",
                "\"com/eterocell/rhythhaus/settings/RhythHausBuildInfo.kt\")",
                "kotlin.srcDir(generatedBuildInfoRoot)",
                "dependsOn(generateRhythHausBuildInfo)",
                "tasks.register<VerifyRhythHausVersionOverrideTask>(",
                "\"verifyRhythHausVersionOverride\")",
                "generatedFile.set(generateRhythHausBuildInfo.flatMap { it.outputFile })",
            )
        const val featureEnPath =
            "feature/settings/src/commonMain/composeResources/values/strings.xml"
        const val featureZhPath =
            "feature/settings/src/commonMain/composeResources/values-zh/strings.xml"
        const val sharedEnPath =
            "shared/src/commonMain/composeResources/values/strings.xml"
        const val sharedZhPath =
            "shared/src/commonMain/composeResources/values-zh/strings.xml"
        const val featureLogoPath =
            "feature/settings/src/commonMain/composeResources/drawable/rhythhaus_logo.xml"
        const val sharedLogoPath =
            "shared/src/commonMain/composeResources/drawable/rhythhaus_logo.xml"
        const val featureSourcePath = "feature/settings/src/commonMain/kotlin"
        const val conventionBuildPath =
            "build-logic/convention/src/main/kotlin/build-logic.build-info.gradle.kts"
        const val sharedBuildPath = "shared/build.gradle.kts"
    }
}
