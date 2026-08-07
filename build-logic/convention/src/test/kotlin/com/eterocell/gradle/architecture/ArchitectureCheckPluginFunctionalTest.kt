package com.eterocell.gradle.architecture

import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume.assumeTrue

class ArchitectureCheckPluginFunctionalTest {
    @Test
    fun validPreservedGraphSucceeds() {
        runner(fixture()).build()
    }

    @Test
    fun validGraphRunsArchitectureCheckThroughCheckAndQualityCheck() {
        listOf("check", "qualityCheck").forEach { entrypoint ->
            val result = runner(qualityEntrypointFixture(), entrypoint).build()

            assertTrue(result.output.contains(":architectureCheck"), result.output)
        }
    }

    @Test
    fun illegalGraphFailsWithStableDiagnosticThroughCheckAndQualityCheck() {
        listOf("check", "qualityCheck").forEach { entrypoint ->
            assertExactFailure(
                qualityEntrypointFixture(Mutation.ForbiddenEdge),
                entrypoint,
                listOf("ARCH-EDGE :feature:library:impl [architecture] -> :feature:playlists:impl"),
            )
        }
    }

    @Test
    fun qualityCheckRunsChildDetektAndSpotlessChecks() {
        val fixture = qualityAggregationFixture()
        val projectDir = fixture.projectDir
        val firstResult = qualityAggregationRunner(projectDir).build()

        assertEquals(TaskOutcome.SUCCESS, firstResult.task(":architectureCheck")?.outcome, firstResult.output)
        listOf("detekt/child-detekt.marker", "spotless/child-spotless-check.marker").forEach { marker ->
            assertTrue(fixture.markerDirectory.resolve(marker).isFile, "Missing execution marker: $marker")
        }

        val secondResult = qualityAggregationRunner(projectDir).build()
        assertTrue(secondResult.output.contains("Reusing configuration cache."), secondResult.output)
    }

    @Test
    fun dedicatedQualityWorkflowInvokesCanonicalQualityEntrypoint() {
        val workflow = File(System.getProperty("rhythhaus.rootDir"), ".github/workflows/quality.yml")
        assertTrue(workflow.isFile, "Missing dedicated quality workflow at ${workflow.path}")

        val content = workflow.readText()
        assertTrue(Regex("(?m)^  pull_request:").containsMatchIn(content), content)
        assertTrue(
            Regex("(?m)^  push:\\R(?:^    .+\\R)*?^    branches:\\R      - main$").containsMatchIn(content),
            content,
        )
        assertTrue(!Regex("(?m)^\\s+paths:").containsMatchIn(content), content)

        val gradleCommands = Regex("(?m)^        run: (\\./gradlew [^\\r\\n]*)").findAll(content).map { it.groupValues[1] }.toList()
        val qualityCommands = gradleCommands.filter { Regex("(?:^|\\s)qualityCheck(?:\\s|$)").containsMatchIn(it) }
        assertEquals(1, qualityCommands.size, gradleCommands.joinToString("\\n"))
        assertEquals(
            "./gradlew qualityCheck --configuration-cache --configuration-cache-problems=fail --no-parallel",
            qualityCommands.single(),
        )
        listOf("architectureCheck", "spotlessCheck", "detekt").forEach { task ->
            assertTrue(gradleCommands.none { Regex("(?:^|\\s)$task(?:\\s|$)").containsMatchIn(it) }, gradleCommands.joinToString("\\n"))
        }
    }

    @Test
    fun configurationCacheIsReusedForValidAndInvalidFixtures() {
        val validProject = fixture()
        runner(validProject).build()
        assertTrue(runner(validProject).build().output.contains("Reusing configuration cache"))

        val invalidProject = fixture(Mutation.ForbiddenEdge)
        assertFailure(invalidProject, listOf("ARCH-EDGE"), ":feature:library:impl")
        val secondRun = runner(invalidProject).buildAndFail()
        assertTrue(secondRun.output.contains("Reusing configuration cache"), secondRun.output)
    }

    @Test fun dependencyCycleIsCanonicalAndDoesNotWeakenTheDag() =
        assertFailure(Mutation.DependencyCycle, listOf("ARCH-CYCLE", "ARCH-EDGE", "ARCH-EDGE"), ":core:database -> :core:model -> :core:database")

    @Test
    fun selfDependencyRemainsAnEdgeAndOneNodeCycle() =
        assertExactFailure(
            fixture(Mutation.SelfDependency),
            listOf(
                "ARCH-CYCLE :core:model -> :core:model",
                "ARCH-EDGE :core:model [architecture] -> :core:model",
            ),
        )

    @Test fun manuallyCreatedKmpKspConfigurationIsNotTooling() =
        assertFailure(Mutation.ProductionKspProcessor, listOf("ARCH-EDGE"), ":architecture-processor")

    @Test
    fun unregisteredProductionKspTargetsRemainArchitectureEdges() =
        assertExactFailure(
            targetRegistrationFixture(applyConvention = false),
            listOf(
                "ARCH-EDGE :core:model [kspAndroid] -> :architecture-processor",
                "ARCH-EDGE :core:model [kspIosArm64] -> :architecture-processor",
                "ARCH-EDGE :core:model [kspJvm] -> :architecture-processor",
            ),
        )

    @Test
    fun conventionOwnedProductionKspTargetsExcludeOnlyProcessorToolingEdges() {
        val result = runner(targetRegistrationFixture(applyConvention = true)).build()

        assertTrue(!result.output.contains("ARCH-EDGE"), result.output)
        assertEquals(
            "[:shared|architecture|:core:model]",
            result.output.dependencyEdges(),
            result.output,
        )
    }

    @Test fun processorOutsideProductionKspConfigurationRemainsGraphEdge() =
        assertFailure(Mutation.ImplementationProcessor, listOf("ARCH-EDGE"), ":architecture-processor")

    @Test fun forbiddenEdgeFails() = assertFailure(Mutation.ForbiddenEdge, listOf("ARCH-EDGE"), ":feature:library:impl")

    @Test
    fun playlistsApiCannotDependOnCoreModel() =
        assertFailure(
            Mutation.PlaylistsApiDependsOnCoreModel,
            listOf("ARCH-EDGE"),
            ":feature:playlists:api",
        )

    @Test
    fun apiCannotDependOnCoreDatabase() =
        assertFailure(
            Mutation.LibraryApiDependsOnCoreDatabase,
            listOf("ARCH-EDGE"),
            ":feature:library:api",
        )

    @Test
    fun apiCannotDependOnShared() =
        assertFailure(
            Mutation.LibraryApiDependsOnShared,
            listOf("ARCH-CYCLE", "ARCH-CYCLE", "ARCH-EDGE"),
            ":feature:library:api",
        )

    @Test
    fun apiCannotDependOnImplementation() =
        assertFailure(
            Mutation.LibraryApiDependsOnImplementation,
            listOf("ARCH-CYCLE", "ARCH-EDGE"),
            ":feature:library:api",
        )

    @Test
    fun implementationCannotDependOnShared() =
        assertFailure(
            Mutation.LibraryImplementationDependsOnShared,
            listOf("ARCH-CYCLE", "ARCH-EDGE"),
            ":feature:library:impl",
        )

    @Test fun modelDiscoveredKmpResourcesAreAccepted() {
        runner(fixture()).build()
    }

    @Test
    fun composeConventionReportsInvalidNamespacesExactly() {
        val projectDir = composeFixture(namespace = "")
        assertExactFailure(
            projectDir,
            listOf(
                "ARCH-RESOURCE :shared [commonMain] root=shared/src/commonMain/composeResources namespace=<invalid>",
                "ARCH-RESOURCE :shared [jvmMain] root=shared/src/jvmMain/composeResources namespace=<invalid>",
            ),
        )
    }

    @Test
    fun composeConventionSupportsRegisteredCustomRootsAndRejectsExternalRoots() {
        val supported = composeFixture(namespace = "com.example.resources", customRoot = "src/commonMain/extraResources")
        resource(supported, ":shared", "src/commonMain/extraResources/values.xml")
        val supportedResult = runner(supported).build()
        val composeRecords = supportedResult.output.composeResourceRecords()
        assertEquals(
            listOf(
                ":shared|commonMain|COMPOSE|shared/src/commonMain/composeResources|com.example.resources",
                ":shared|commonMain|COMPOSE|shared/src/commonMain/extraResources|com.example.resources",
                ":shared|jvmMain|COMPOSE|shared/src/jvmMain/composeResources|com.example.resources",
            ),
            composeRecords,
            supportedResult.output,
        )
        assertEquals(composeRecords.distinct(), composeRecords, supportedResult.output)

        val externalRoot = Files.createTempDirectory("architecture-compose-external").toFile()
        val unsupported = composeFixture(
            namespace = "com.example.resources",
            customRoot = externalRoot.invariantSeparatorsPath,
        )
        assertExactFailure(
            unsupported,
            listOf("ARCH-RESOURCE :shared [commonMain] root=${externalRoot.invariantSeparatorsPath} unsupported"),
        )
    }

    @Test
    fun composeConventionConfiguresDeclaredCustomRootForResourceGeneration() {
        val projectDir = composeFixture(
            namespace = "com.example.resources",
            customRoot = "src/commonMain/extraResources",
        )
        val strings = moduleDir(projectDir, ":shared").resolve("src/commonMain/extraResources/values/strings.xml")
        strings.parentFile.mkdirs()
        strings.writeText("<resources><string name=\"custom_message\">Custom message</string></resources>")

        val result = runner(projectDir, ":shared:generateResourceAccessorsForCommonMain").build()
        val generatedAccessors =
            moduleDir(projectDir, ":shared").resolve("build/generated").walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()

        assertTrue(
            generatedAccessors.any { it.readText().contains("custom_message") },
            "$result\n${generatedAccessors.joinToString("\n") { "${it.path}: ${it.readText()}" }}",
        )
    }

    @Test
    fun androidApplicationSyntheticTestSelfEdgesAreObservedWithoutControlledPublisher() =
        assertExactFailure(
            androidApplicationFixture(applyControlledPublisher = false),
            listOf(
                "ARCH-CYCLE :androidApp -> :androidApp",
                "ARCH-EDGE :androidApp [debugAndroidTestCompileClasspath] -> :androidApp",
                "ARCH-EDGE :androidApp [debugUnitTestCompileClasspath] -> :androidApp",
                "ARCH-EDGE :androidApp [debugUnitTestRuntimeClasspath] -> :androidApp",
            ),
        )

    @Test
    fun androidApplicationSyntheticTestSelfEdgesAreExcludedWhileMainResourcesRemainCanonical() {
        val result = runner(androidApplicationFixture()).build()

        assertTrue(!result.output.contains("ARCH-CYCLE :androidApp -> :androidApp"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :androidApp [debugAndroidTestCompileClasspath] -> :androidApp"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :androidApp [debugUnitTestCompileClasspath] -> :androidApp"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :androidApp [debugUnitTestRuntimeClasspath] -> :androidApp"), result.output)
        assertEquals(
            listOf(
                ":androidApp|main|ANDROID|androidApp/src/main/res|com.example.androidfixture",
                ":core:database|commonMain|KOTLIN|core/database/src/commonMain/resources|",
                ":core:database|jvmMain|KOTLIN|core/database/src/jvmMain/resources|",
                ":shared|commonMain|KOTLIN|shared/src/commonMain/resources|",
                ":shared|jvmMain|KOTLIN|shared/src/jvmMain/resources|",
            ),
            result.output.resourceRecords().removeSurrounding("[", "]").split(", "),
            result.output,
        )
        assertTrue(result.output.contains("TEST_ANDROID_TEST_CONFIGURATIONS="), result.output)
        assertEquals(
            listOf(
                ":androidApp|debugAndroidTestCompileClasspath",
                ":androidApp|debugAndroidTestRuntimeClasspath",
                ":androidApp|debugUnitTestCompileClasspath",
                ":androidApp|debugUnitTestRuntimeClasspath",
            ),
            result.output.androidTestConfigurations(),
            result.output,
        )
    }

    @Test
    fun controlledAndroidKmpTestComponentsPublishAndSuppressOnlyTheirSyntheticSelfDependencies() {
        val result = runner(controlledAndroidKmpTestComponentsFixture()).build()

        assertEquals(
            listOf(
                ":core:model|androidDeviceTestCompileClasspath",
                ":core:model|androidDeviceTestRuntimeClasspath",
                ":core:model|androidHostTestCompileClasspath",
                ":core:model|androidHostTestRuntimeClasspath",
            ),
            result.output.androidTestConfigurations(),
            result.output,
        )
        assertTrue(!result.output.contains("ARCH-CYCLE :core:model -> :core:model"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :core:model [androidHostTestCompileClasspath] -> :core:model"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :core:model [androidHostTestRuntimeClasspath] -> :core:model"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :core:model [androidDeviceTestCompileClasspath] -> :core:model"), result.output)
        assertTrue(!result.output.contains("ARCH-EDGE :core:model [androidDeviceTestRuntimeClasspath] -> :core:model"), result.output)
    }

    @Test
    fun controlledAndroidKmpTestComponentsRetainAuthoredSelfDependencyOutsidePublishedIdentities() =
        assertExactFailure(
            controlledAndroidKmpTestComponentsFixture(authoredArchitectureSelfDependency = true),
            listOf(
                "ARCH-CYCLE :core:model -> :core:model",
                "ARCH-EDGE :core:model [architecture] -> :core:model",
            ),
        )

    @Test
    fun controlledAndroidKmpTestComponentsFailClosedForDistinguishableSelfDependencyMultiplicity() =
        assertExactFailure(
            controlledAndroidKmpTestComponentsFixture(addDistinctHostTestSelfDependency = true),
            listOf(
                "ARCH-CYCLE :core:model -> :core:model",
                "ARCH-EDGE :core:model [androidHostTestCompileClasspath] -> :core:model",
            ),
        )

    @Test
    fun androidKmpLibraryReportsOnlyEnabledMainProductionResourceRoot() {
        val result = runner(androidKmpResourceFixture()).build()
        val androidRecords = result.output.resourceRecords().removeSurrounding("[", "]")
            .split(", ")
            .filter { "|ANDROID|" in it }

        assertEquals(
            listOf(
                ":core:model|main|ANDROID|core/model/src/androidMain/res|com.example.androidkmpfixture",
            ),
            androidRecords,
            result.output,
        )
        assertEquals(androidRecords.distinct(), androidRecords, result.output)
    }

    @Test
    fun androidKmpLibraryDoesNotPublishResourcesWhenDisabled() {
        val result = runner(androidKmpResourceFixture(resourcesEnabled = false)).build()
        val androidRecords = result.output.resourceRecords().removeSurrounding("[", "]")
            .split(", ")
            .filter { "|ANDROID|" in it }

        assertEquals(emptyList(), androidRecords, result.output)
    }

    @Test
    fun androidKmpLibraryExcludesTaskGeneratedResourcesFromNormalizedRecordsAndReusesConfigurationCache() {
        val projectDir = androidKmpResourceFixture(addTaskGeneratedResource = true)

        val firstRun = runner(projectDir).build()
        val androidRecords = firstRun.output.resourceRecords().removeSurrounding("[", "]")
            .split(", ")
            .filter { "|ANDROID|" in it }

        assertEquals(
            listOf(
                ":core:model|main|ANDROID|core/model/src/androidMain/res|com.example.androidkmpfixture",
            ),
            androidRecords,
            firstRun.output,
        )
        assertTrue(
            androidRecords.none { "build/generated/taskAndroidResources" in it },
            firstRun.output,
        )

        val secondRun = runner(projectDir).build()
        assertTrue(secondRun.output.contains("Reusing configuration cache"), secondRun.output)
    }

    @Test
    fun configuredPhysicalSqlDelightArtifactsMakeSharedTheSqlDelightOwner() {
        runner(fixture()).build()
    }

    @Test
    fun sqlDelightConventionSupportsDefaultAndExplicitProductionRoots() {
        runner(fixture()).build()
        runner(fixture(Mutation.ExplicitSupportedSqlDelightRoot)).build()
    }

    @Test
    fun compileOnlySqlDelightFixtureWithoutConsumerPluginReportsMissingRuntimeApi() {
        val result = isolatedSqlDelightClasspathRunner(SqlDelightFixtureDependency.CompileOnly).buildAndFail()

        assertTrue(result.output.contains(SQLDELIGHT_FIXTURE_APPLIED_MARKER), result.output)
        assertTrue(result.output.contains(SQLDELIGHT_RUNTIME_API_MISSING_SENTINEL), result.output)
    }

    @Test
    fun implementationSqlDelightFixtureWithoutConsumerPluginReportsAvailableRuntimeApiAndAbsentExtension() {
        val result = isolatedSqlDelightClasspathRunner(SqlDelightFixtureDependency.Implementation).build()

        assertSqlDelightFixtureMarkers(
            result.output,
            SQLDELIGHT_RUNTIME_API_AVAILABLE_MARKER,
            SQLDELIGHT_EXTENSION_ABSENT_MARKER,
        )
    }

    @Test
    fun compileOnlySqlDelightFixtureCoApplicationVisibilityTopologyReportsAvailableRuntimeApiAndPresentExtension() {
        val result =
            isolatedSqlDelightClasspathRunner(
                dependency = SqlDelightFixtureDependency.CompileOnly,
                consumerAppliesSqlDelight = true,
            ).build()

        assertSqlDelightFixtureMarkers(
            result.output,
            SQLDELIGHT_RUNTIME_API_AVAILABLE_MARKER,
            SQLDELIGHT_EXTENSION_PRESENT_MARKER,
        )
    }

    @Test
    fun implementationSqlDelightFixtureCoApplicationControlReportsAvailableRuntimeApiAndPresentExtension() {
        val result =
            isolatedSqlDelightClasspathRunner(
                dependency = SqlDelightFixtureDependency.Implementation,
                consumerAppliesSqlDelight = true,
            ).build()

        assertSqlDelightFixtureMarkers(
            result.output,
            SQLDELIGHT_RUNTIME_API_AVAILABLE_MARKER,
            SQLDELIGHT_EXTENSION_PRESENT_MARKER,
        )
    }

    @Test
    fun externalSqlDelightRootIsReportedWithoutConfigurationFailure() {
        val projectDir = fixture()
        val externalRoot = Files.createTempDirectory("architecture-check-external-sqldelight").toFile()
        externalRoot.resolve("rogue.sq").writeText("fixture")
        append(
            projectDir,
            ":core:database",
            "sqldelight { databases.named(\"RhythHausDatabase\") { srcDirs.from(file(\"${externalRoot.invariantSeparatorsPath}\")) } }",
        )

        assertExactFailure(
            projectDir,
            listOf(
                "ARCH-SQLDELIGHT :core:database [RhythHausDatabase] root=${externalRoot.invariantSeparatorsPath} unsupported",
            ),
        )
    }

    @Test fun runtimeAndReadmeDoNotMakeAnotherSqlDelightOwner() {
        runner(fixture(Mutation.SqlDelightRuntimeAndReadme)).build()
    }

    @Test
    fun sqlDelightArtifactsAreDerivedAtExecutionAcrossConfigurationCacheReuse() {
        val projectDir = fixture()
        val artifact = moduleDir(projectDir, ":core:database").resolve("src/commonMain/sqldelight/fixture.sq")

        runner(projectDir).build()
        artifact.delete()
        val missing = runner(projectDir).buildAndFail()
        assertTrue(missing.output.contains("Reusing configuration cache"), missing.output)
        assertTrue(missing.output.contains("ARCH-SQLDELIGHT expected=:core:database owners=<none>"), missing.output)

        artifact.writeText("fixture")
        val restored = runner(projectDir).build()
        assertTrue(restored.output.contains("Reusing configuration cache"), restored.output)
    }

    @Test
    fun sqlDelightDriverInSpoofedConfigurationIsNotAnOwnerSignal() =
        assertExactFailure(
            fixture(Mutation.SpoofedSqlDelightDriver),
            listOf("ARCH-SQLDELIGHT expected=:core:database owners=<none>"),
        )

    @Test fun sqlDelightOwnershipViolationsFail() {
        assertExactFailure(
            fixture(Mutation.MissingSqlDelightOwner),
            listOf("ARCH-SQLDELIGHT expected=:core:database owners=<none>"),
        )
        assertExactFailure(
            fixture(Mutation.TwoSqlDelightOwners),
            listOf("ARCH-SQLDELIGHT expected=:core:database owners=:core:database,:shared"),
        )
        assertExactFailure(
            fixture(Mutation.ArbitrarySqlDelightOwner),
            listOf("ARCH-SQLDELIGHT expected=:core:database owners=:shared"),
        )
    }

    @Test fun explicitApiUsesTypedKotlinExtensionInsteadOfTaskNamesOrCompilerArgs() =
        assertFailure(Mutation.ExplicitApiWarningWithStrictCompilerArgs, listOf("ARCH-EXPLICIT-API"), ":core:model")

    @Test fun productionCompilationRejectsDisallowedPackageThroughKsp() =
        assertKspCompilationFailure(
            source = "package com.eterocell.rhythhausx\n/** Wrong root. */\npublic class WrongRoot",
            expectedDiagnostic = "ARCH-PACKAGE",
        )

    @Test
    fun productionKspDiagnosticsUseNormalizedRelativePaths() {
        val result = runner(
            kspFixture("package com.eterocell.rhythhausx\n/** Wrong root. */\npublic class WrongRoot"),
            "compileKotlinJvm",
        ).buildAndFail()

        assertTrue(
            result.output.contains("ARCH-PACKAGE :core:model:Fixture.kt (com.eterocell.rhythhausx)"),
            result.output,
        )
        assertTrue(!result.output.contains("ARCH-PACKAGE :core:model:/"), result.output)
    }

    @Test fun productionCompilationRejectsUndocumentedPublicDeclarationThroughKsp() =
        assertKspCompilationFailure(
            source = "package com.eterocell.rhythhaus\npublic class MissingKDoc",
            expectedDiagnostic = "ARCH-KDOC",
        )

    @Test
    fun kspReportsOnlyAuthoredUndocumentedMembersFromDocumentedDataClasses() {
        val result =
            runner(
                kspFixture(
                    """
                    package com.eterocell.rhythhaus

                    /** A documented data class. */
                    public data class DocumentedData(
                        /** A documented constructor property. */
                        public val name: String,
                    ) {
                        public fun undocumentedMember(): String = name
                    }
                    """.trimIndent(),
                ),
                "compileKotlinJvm",
            ).buildAndFail()

        assertEquals(
            listOf(
                "ARCH-KDOC :core:model:Fixture.kt:8 (com.eterocell.rhythhaus.DocumentedData.undocumentedMember)",
            ),
            kspDiagnostics(result.output),
            result.output,
        )
    }

    @Test
    fun kspReportsOverloadsWithDistinctLocationsAndQualifiedNames() {
        val result =
            runner(
                kspFixture(
                    mapOf(
                        "overloads/Overloads.kt" to
                            """
                            package com.eterocell.rhythhaus

                            public fun duplicate(value: String): String = value
                            public fun duplicate(value: Int): Int = value
                            """.trimIndent(),
                    ),
                ),
                "compileKotlinJvm",
            ).buildAndFail()

        val diagnostics = kspDiagnostics(result.output).filter { it.contains("com.eterocell.rhythhaus.duplicate") }
        assertEquals(2, diagnostics.size, result.output)
        assertEquals(diagnostics.sorted(), diagnostics, result.output)
        assertEquals(
            2,
            Regex("ARCH-KDOC :core:model:overloads/Overloads\\.kt:[0-9]+ \\(com\\.eterocell\\.rhythhaus\\.duplicate\\)")
                .findAll(result.output)
                .count(),
            result.output,
        )
    }

    @Test
    fun kspReportsPublicDeclarationsWithDefaultParameters() {
        val result =
            runner(
                kspFixture(
                    mapOf(
                        "defaults/DefaultParameter.kt" to
                            """
                            package com.eterocell.rhythhaus

                            public fun defaultParameter(value: String = "default"): String = value
                            """.trimIndent(),
                    ),
                ),
                "compileKotlinJvm",
            ).buildAndFail()

        assertEquals(
            1,
            Regex("ARCH-KDOC :core:model:defaults/DefaultParameter\\.kt:[0-9]+ \\(com\\.eterocell\\.rhythhaus\\.defaultParameter\\)")
                .findAll(result.output)
                .count(),
            result.output,
        )
    }

    @Test
    fun kspExcludesPreexistingGeneratedInputWhileReportingOrdinaryInput() {
        val projectDir =
            kspFixture(
                mapOf(
                    "Fixture.kt" to
                        """
                        package com.eterocell.rhythhaus

                        public class OrdinaryViolation
                        """.trimIndent(),
                ),
            )
        append(
            projectDir,
            ":core:model",
            "kotlin { sourceSets.named(\"jvmMain\") { kotlin.srcDir(\"build/generated/fixture/jvmMain/kotlin\") } }",
        )
        preexistingGeneratedViolation(projectDir).apply {
            parentFile.mkdirs()
            writeText("package outside.generated\npublic class PreexistingGeneratedViolation")
        }

        val result = runner(projectDir, "compileKotlinJvm").buildAndFail()

        assertTrue(!result.output.contains("PreexistingGeneratedViolation"), result.output)
        assertTrue(!result.output.contains("outside.generated"), result.output)
        assertEquals(
            1,
            Regex("ARCH-KDOC :core:model:Fixture\\.kt:[0-9]+ \\(com\\.eterocell\\.rhythhaus\\.OrdinaryViolation\\)")
                .findAll(result.output)
                .count(),
            result.output,
        )
    }

    @Test
    fun kspAcceptsDocumentedPublicOrdinaryDeclarationsAndEscapedIdentifiers() {
        val result =
            runner(
                kspFixture(
                    mapOf(
                        "valid/DocumentedForms.kt" to
                            """
                            package com.eterocell.rhythhaus.`valid-segment`

                            /** A documented class. */
                            public class `valid-class`

                            /** A documented function. */
                            public fun
                                `valid-function`(
                                    value: String,
                                ): String = value

                            /** A documented property. */
                            public val `valid-property`: String = "valid"

                            /** A documented annotation. */
                            public annotation class `valid-annotation`

                            /** A documented annotated declaration. */
                            @`valid-annotation`
                            public class AnnotatedDeclaration

                            /** A documented containing declaration. */
                            public class DocumentedContainer {
                                /** A documented nested public property. */
                                public val nestedProperty: String = "valid"
                            }
                            """.trimIndent(),
                    ),
                ),
                "compileKotlinJvm",
            ).build()

        assertKspTaskRan(result.output)
    }

    @Test
    fun kspReportsExactOrderedDeduplicatedPackageAndKdocDiagnostics() {
        val result =
            runner(
                kspFixture(
                    mapOf(
                        "zeta/PackageOnly.kt" to
                            """
                            package com.eterocell.rhythhausx

                            /** Documented declaration. */
                            public class PackageOnly
                            """.trimIndent(),
                        "alpha/UndocumentedForms.kt" to
                            """
                            package com.eterocell.rhythhausx

                            public class MissingClass
                            public fun missingFunction() = Unit
                            public val missingProperty = "missing"
                            public annotation class MissingAnnotation
                            """.trimIndent(),
                    ),
                ),
                "compileKotlinJvm",
            ).buildAndFail()

        assertKspTaskRan(result.output)
        assertEquals(
            listOf(
                "ARCH-KDOC :core:model:alpha/UndocumentedForms.kt:3 (com.eterocell.rhythhausx.MissingClass)",
                "ARCH-KDOC :core:model:alpha/UndocumentedForms.kt:4 (com.eterocell.rhythhausx.missingFunction)",
                "ARCH-KDOC :core:model:alpha/UndocumentedForms.kt:5 (com.eterocell.rhythhausx.missingProperty)",
                "ARCH-KDOC :core:model:alpha/UndocumentedForms.kt:6 (com.eterocell.rhythhausx.MissingAnnotation)",
                "ARCH-PACKAGE :core:model:alpha/UndocumentedForms.kt (com.eterocell.rhythhausx)",
                "ARCH-PACKAGE :core:model:zeta/PackageOnly.kt (com.eterocell.rhythhausx)",
            ),
            kspDiagnostics(result.output),
            result.output,
        )
    }

    @Test
    fun kspExcludesLocalDeclarationsAndKotlinLookingCommentsAndRawStrings() {
        val result =
            runner(
                kspFixture(
                    """
                    package com.eterocell.rhythhaus

                    /** A documented outer declaration. */
                    public fun outer() {
                        // public class CommentOnly
                        val source = ${"\"\"\"package com.eterocell.rhythhausx\npublic class RawStringOnly\"\"\""}
                        class LocalDeclaration
                        check(source.isNotEmpty())
                    }
                    """.trimIndent(),
                ),
                "compileKotlinJvm",
            ).build()

        assertKspTaskRan(result.output)
    }

    @Test
    fun kspExcludesTestSourceDeclarationsWhileItsProductionTaskRuns() {
        val result =
            runner(
                kspFixture(
                    source = "/** Production declaration. */\npublic class Production",
                    testSources =
                        mapOf(
                            "InvalidTest.kt" to
                                """
                                package com.eterocell.rhythhausx
                                public class InvalidTestSource
                                """.trimIndent(),
                        ),
                ),
                "compileTestKotlinJvm",
            ).build()

        assertKspTaskRan(result.output)
        assertTrue(!result.output.contains("ARCH-PACKAGE"), result.output)
        assertTrue(!result.output.contains("ARCH-KDOC"), result.output)
    }

    @Test
    fun kspReportsOnlyInitialInputWhenLaterRoundGeneratorEmitsAnInvalidSource() {
        val projectDir =
            kspFixture(
                source = "public class InitialViolation",
                generateLaterRound = true,
            )
        val result =
            runner(
                projectDir,
                "compileKotlinJvm",
            ).buildAndFail()

        assertKspTaskRan(result.output)
        assertTrue(generatedViolation(projectDir).isFile, generatedViolation(projectDir).absolutePath)
        assertEquals(
            "package outside.generated\npublic class GeneratedViolation",
            generatedViolation(projectDir).readText(),
        )
        assertEquals(
            listOf("ARCH-KDOC :core:model:Fixture.kt:2 (com.eterocell.rhythhaus.InitialViolation)"),
            kspDiagnostics(result.output),
            result.output,
        )
    }

    @Test
    fun kmpTaskGeneratedStandardResourcesAreExcludedWithoutProducerDependencies() {
        val projectDir = kmpTaskGeneratedResourceFixture()

        val firstRun = runner(projectDir, "architectureCheck", ":core:model:copyGeneratedResources").build()
        assertEquals(TaskOutcome.SUCCESS, firstRun.task(":core:model:copyGeneratedResources")?.outcome, firstRun.output)
        val records = firstRun.output.resourceRecords()
        assertTrue(!records.contains("build/generated/fixtureResources/commonMain"), firstRun.output)
        assertTrue(!firstRun.output.resourceInputs().contains("build/generated/fixtureResources/commonMain"), firstRun.output)
        assertTrue(records.contains(":core:model|commonMain|KOTLIN|core/model/src/commonMain/customResources|"), firstRun.output)

        val secondRun = runner(projectDir, "architectureCheck", ":core:model:copyGeneratedResources").build()
        assertTrue(secondRun.output.contains("Reusing configuration cache."), secondRun.output)
    }

    @Test
    fun kmpJvmKspConsumesExternallyProvidedRepositoryProcessorJar() {
        val processorJar = externallyProvidedProcessorJarOrSkip()
        val result = runner(binaryProcessorFixture(processorJar), ":core:model:compileKotlinJvm").buildAndFail()

        assertKspTaskRan(result.output)
        assertTrue(
            result.output.contains(
                "ARCH-PACKAGE :core:model:InvalidProduction.kt (outside.fixture)",
            ),
            result.output,
        )
        assertTrue(result.output.contains("TEST-KSP-JAR=$processorJar"), result.output)
    }

    @Test fun unapprovedIosFrameworkExportFails() =
        assertFailure(Mutation.UnapprovedIosExport, listOf("ARCH-IOS-EXPORT"), ":core:model")

    @Test
    fun corePlaybackPositivePolicyAcceptsPreservedPackagesAndIosExport() {
        runner(corePlaybackFixture(), ":core:playback:compileKotlinJvm", "architectureCheck").build()
    }

    @Test
    fun corePlaybackCannotDependOnShared() =
        assertExactFailure(
            corePlaybackFixture(dependsOnShared = true),
            listOf(
                "ARCH-CYCLE :core:playback -> :shared -> :core:playback",
                "ARCH-EDGE :core:playback [architecture] -> :shared",
            ),
        )

    @Test
    fun corePlaybackIsTheOnlyNewAllowedIosExport() =
        assertExactFailure(
            corePlaybackFixture(exportModel = true),
            listOf("ARCH-IOS-EXPORT :shared -> :core:model"),
        )

    @Test
    fun nowPlayingProductionRootAcceptsApprovedEdgesAndResourceNamespace() {
        val result = nowPlayingRunner(
            nowPlayingFixture(),
            ":feature:nowplaying:compileKotlinJvm",
            "architectureCheck",
        ).build()

        assertTrue(
            result.output.contains(
                ":feature:nowplaying|commonMain|COMPOSE|feature/nowplaying/src/commonMain/composeResources|rhythhaus.feature.nowplaying.generated.resources",
            ),
            result.output,
        )
        assertTrue(result.output.contains(":feature:nowplaying:kspKotlinJvm"), result.output)
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":feature:nowplaying:kspKotlinJvm")?.outcome,
            result.output,
        )
        assertTrue(
            result.output.contains(":feature:nowplaying:compileKotlinJvm"),
            result.output,
        )
    }

    @Test
    fun nowPlayingProductionRootRejectsForbiddenEdgesNamespacesAndExports() {
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnShared),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :shared"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnTagLib),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :taglib"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnLibraryApi),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :feature:library:api"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnLibraryImplementation),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :feature:library:impl"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnApp),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :androidApp"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnCoreModel),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :core:model"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.DependsOnPlaylistsImplementation),
            listOf("ARCH-EDGE :feature:nowplaying [architecture] -> :feature:playlists:impl"),
        )
        assertRequiredDiagnostics(
            nowPlayingFixture(NowPlayingMutation.InvalidResourceNamespace),
            listOf(
                "ARCH-RESOURCE :feature:nowplaying [commonMain] root=feature/nowplaying/src/commonMain/composeResources namespace=<invalid>",
            ),
        )
        assertExactFailure(
            nowPlayingFixture(NowPlayingMutation.IosExport),
            listOf("ARCH-IOS-EXPORT :shared -> :feature:nowplaying"),
        )
    }

    @Test
    fun nowPlayingProductionRootRejectsMissingPackageRootThroughTheRealProcessor() {
        val projectDir = nowPlayingFixture()
        writeKotlinSource(
            projectDir,
            ":feature:nowplaying",
            "src/commonMain/kotlin/InvalidFeature.kt",
            "package outside.fixture\n/** Invalid package. */\npublic class InvalidFeature",
        )
        assertKspCompilationFailure(
            projectDir,
            ":feature:nowplaying:compileKotlinJvm",
            "ARCH-PACKAGE :feature:nowplaying:InvalidFeature.kt (outside.fixture)",
        )
    }

    @Test
    fun nowPlayingProductionRootRejectsEmptyConfiguredPackageRootsThroughTheRealProcessor() {
        val projectDir = nowPlayingFixture()
        append(
            projectDir,
            ":feature:nowplaying",
            """
            extensions.configure<com.google.devtools.ksp.gradle.KspExtension> {
                arg("architecture.packageRoots", "")
            }
            """.trimIndent(),
        )

        assertKspCompilationFailure(
            projectDir,
            ":feature:nowplaying:compileKotlinJvm",
            "ARCH-PACKAGE :feature:nowplaying:NowPlaying.kt (com.eterocell.rhythhaus.nowplaying)",
        )
    }

    @Test
    fun nowPlayingProductionRootRejectsMissingPublicKDocThroughTheRealProcessor() {
        val projectDir = nowPlayingFixture()
        writeKotlinSource(
            projectDir,
            ":feature:nowplaying",
            "src/commonMain/kotlin/MissingFeatureKDoc.kt",
            "package com.eterocell.rhythhaus.nowplaying\npublic class MissingFeatureKDoc",
        )
        assertKspCompilationFailure(
            projectDir,
            ":feature:nowplaying:compileKotlinJvm",
            "ARCH-KDOC :feature:nowplaying:MissingFeatureKDoc.kt:2 (com.eterocell.rhythhaus.nowplaying.MissingFeatureKDoc)",
        )
    }

    @Test
    fun searchFeatureConventionPublishesRootsAndKspRegistrations() {
        val result = searchRunner(
            searchFixture(),
            ":feature:search:kspAndroidMain",
            ":feature:search:kspKotlinJvm",
            ":feature:search:kspKotlinIosArm64",
            ":feature:search:kspKotlinIosSimulatorArm64",
            ":feature:search:verifySearchFeatureConvention",
            "architectureCheck",
        ).build()
        assertEquals(
            "KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.search",
            result.output.lineSequence().single { it.startsWith("KSP_PACKAGE_ROOTS=") },
            result.output,
        )
        assertEquals(
            listOf(
                "KSP_REGISTRATION=:feature:search|kspAndroid|:architecture-processor",
                "KSP_REGISTRATION=:feature:search|kspIosArm64|:architecture-processor",
                "KSP_REGISTRATION=:feature:search|kspIosSimulatorArm64|:architecture-processor",
                "KSP_REGISTRATION=:feature:search|kspJvm|:architecture-processor",
            ),
            result.output.lineSequence().filter { it.startsWith("KSP_REGISTRATION=") }.toList(),
            result.output,
        )
        assertTrue(result.output.contains(":feature:search:kspKotlinJvm"), result.output)
        listOf("AndroidMain", "KotlinJvm", "KotlinIosArm64", "KotlinIosSimulatorArm64").forEach { target ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":feature:search:ksp$target")?.outcome, result.output)
            assertFalse(result.output.contains(":feature:search:ksp$target SKIPPED"), result.output)
            assertFalse(result.output.contains(":feature:search:ksp$target NO-SOURCE"), result.output)
        }
    }

    @Test
    fun searchFeatureConventionRejectsMissingKspProjectDependencyWhileRegistryRemains() {
        val result = searchRunner(
            searchFixture(SearchMutation.RemoveKspJvmProcessor),
            ":feature:search:verifySearchFeatureConvention",
        ).buildAndFail()

        assertTrue(result.output.contains("KSP registry/configuration mismatch: kspJvm"), result.output)
    }

    @Test
    fun searchFeatureProcessorRejectsOneMalformedCommonSourceOnEveryTargetAndRestoresGreen() {
        val projectDir = searchFixture(
            sourceOverride = "package outside.fixture\n/** Invalid package. */\npublic class SearchFeature",
        )
        val expectedDiagnostic = "ARCH-PACKAGE :feature:search:SearchFeature.kt (outside.fixture)"

        searchKspTasks.forEach { task ->
            val result = searchRunner(projectDir, task).buildAndFail()
            assertSearchKspTaskOutcome(result, task, TaskOutcome.FAILED, expectedDiagnostic)
        }

        source(
            projectDir,
            ":feature:search",
            "SearchFeature.kt",
            "package com.eterocell.rhythhaus.search\n/** A documented Search feature declaration. */\npublic class SearchFeature",
        )
        searchKspTasks.forEach { task ->
            val result = searchRunner(projectDir, task).build()
            assertSearchKspTaskOutcome(result, task, TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun searchFeatureProcessorRejectsUndocumentedPublicKDocClosure() {
        assertSearchKdocFailureThenRestoresGreen(
            "package com.eterocell.rhythhaus.search\npublic class SearchFeature",
            "ARCH-KDOC :feature:search:SearchFeature.kt:2 (com.eterocell.rhythhaus.search.SearchFeature)",
        )
        assertSearchKdocFailureThenRestoresGreen(
            """
            package com.eterocell.rhythhaus.search

            /** A documented Search feature. */
            public class SearchFeature {
                public fun undocumentedMember(): Unit = Unit
            }
            """.trimIndent(),
            "ARCH-KDOC :feature:search:SearchFeature.kt:5 (com.eterocell.rhythhaus.search.SearchFeature.undocumentedMember)",
        )
        assertSearchKdocFailureThenRestoresGreen(
            """
            package com.eterocell.rhythhaus.search

            /** A documented Search feature. */
            public data class SearchFeature
            /** A documented constructor. */
            public constructor(
                public val undocumentedProperty: String,
            )
            """.trimIndent(),
            "ARCH-KDOC :feature:search:SearchFeature.kt:7 (com.eterocell.rhythhaus.search.SearchFeature.undocumentedProperty)",
        )
    }

    @Test
    fun searchFeatureKoinAuditUsesActualRepositoryProductionRootsAndCopiedMutation() {
        val repositoryRoots = searchRepositoryProductionRoots()
        assertTrue(repositoryRoots.isNotEmpty(), "Missing repository Search production roots")
        assertSearchProductionRootsHaveNoKoin(repositoryRoots)

        val copiedRoots = copySearchProductionRoots(repositoryRoots)
        val copiedSource = copiedRoots.asSequence().flatMap { root -> root.walkTopDown().asSequence() }
            .first { it.isFile && it.extension == "kt" }
        copiedSource.writeText("import org.koin.core.Koin\n${copiedSource.readText()}")
        val failure = assertFailsWith<AssertionError> { assertSearchProductionRootsHaveNoKoin(copiedRoots) }
        assertEquals("SEARCH-KOIN feature source imports Koin: ${copiedSource.path}", failure.message)
    }

    @Test
    fun searchFeatureRejectsEmptyConfiguredPackageRootsThroughRealProcessor() =
        assertSearchKspFailure(
            searchFixture(SearchMutation.EmptyPackageRoots),
            "ARCH-PACKAGE :feature:search:SearchFeature.kt (com.eterocell.rhythhaus.search)",
        )

    @Test
    fun searchFeatureRejectsForbiddenEdgesAndSharedExposure() {
        mapOf(
            SearchMutation.DependsOnShared to "ARCH-EDGE :feature:search [architecture] -> :shared",
            SearchMutation.DependsOnPlayback to "ARCH-EDGE :feature:search [architecture] -> :core:playback",
            SearchMutation.DependsOnTagLib to "ARCH-EDGE :feature:search [architecture] -> :taglib",
            SearchMutation.DependsOnDatabase to "ARCH-EDGE :feature:search [architecture] -> :core:database",
            SearchMutation.DependsOnPlatform to "ARCH-EDGE :feature:search [architecture] -> :core:platform",
            SearchMutation.DependsOnImplementation to "ARCH-EDGE :feature:search [architecture] -> :feature:library:impl",
            SearchMutation.DependsOnApp to "ARCH-EDGE :feature:search [architecture] -> :androidApp",
        ).forEach { (mutation, diagnostic) ->
            assertRequiredDiagnostics(searchFixture(mutation), listOf(diagnostic))
        }
    }

    @Test
    fun searchFeatureRejectsWrongPackageNamespaceKoinAndIosExport() {
        assertSearchKspFailure(searchFixture(SearchMutation.WrongPackage), "ARCH-PACKAGE :feature:search:SearchFeature.kt (outside.fixture)")
        assertExactFailure(
            searchFixture(SearchMutation.IosExport),
            listOf(
                "ARCH-EDGE :shared [iosArm64DebugFrameworkExport] -> :feature:search",
                "ARCH-EDGE :shared [iosArm64ReleaseFrameworkExport] -> :feature:search",
                "ARCH-IOS-EXPORT :shared -> :feature:search",
            ),
        )
        assertSearchKspFailure(searchFixture(SearchMutation.MissingKDoc), "ARCH-KDOC :feature:search:SearchFeature.kt:2 (com.eterocell.rhythhaus.search.SearchFeature)")
    }

    @Test
    fun searchFeatureRejectsSharedCommonMainApiExposure() =
        assertExactFailure(
            searchFixture(SearchMutation.SharedCommonMainApiExposure),
            listOf("ARCH-EDGE :shared [commonMainApi] -> :feature:search"),
        )

    @Test
    fun searchFeatureRejectsWrongExpectedNamespaces() {
        assertExactFailure(
            searchFixture(SearchMutation.WrongAndroidNamespace),
            listOf("ARCH-RESOURCE :feature:search [main] root=feature/search/src/androidMain/res namespace=com.example.wrong"),
        )
        assertExactFailure(
            searchFixture(SearchMutation.WrongComposeNamespace),
            listOf(
                "ARCH-RESOURCE :feature:search [androidMain] root=feature/search/src/androidMain/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:search [commonMain] root=feature/search/src/commonMain/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:search [iosArm64Main] root=feature/search/src/iosArm64Main/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:search [iosSimulatorArm64Main] root=feature/search/src/iosSimulatorArm64Main/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:search [jvmMain] root=feature/search/src/jvmMain/composeResources namespace=example.wrong.resources",
            ),
        )
    }

    @Test
    fun searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports() {
        val root = File(System.getProperty("rhythhaus.rootDir"))
        assertSearchResourceAudit(SearchResourceFixture.fromRepository(root))
        mapOf(
            SearchMutation.MissingMovedResource to "SEARCH-RESOURCE missing moved key",
            SearchMutation.DuplicateMovedResource to "SEARCH-RESOURCE duplicate key in Search EN",
            SearchMutation.DuplicateMovedResourceZh to "SEARCH-RESOURCE duplicate key in Search ZH",
            SearchMutation.ExtraMovedResource to "SEARCH-RESOURCE unexpected Search key",
            SearchMutation.CrossOwnerDuplicateResource to "SEARCH-RESOURCE duplicate key across Shared/Search",
            SearchMutation.WrongResourceOwner to "SEARCH-RESOURCE wrong owner for moved key",
            SearchMutation.InvalidResourceNamespace to "SEARCH-RESOURCE wrong namespace",
            SearchMutation.ForeignFeatureResourceImport to "SEARCH-RESOURCE feature imports Shared generated resources",
            SearchMutation.ForeignSharedResourceImport to "SEARCH-RESOURCE Shared imports Search generated resources",
        ).forEach { (mutation, diagnostic) ->
            val failure = assertFailsWith<AssertionError> { assertSearchResourceAudit(searchResourceFixture(mutation)) }
            assertTrue(failure.message.orEmpty().contains(diagnostic), failure.message)
        }
    }

    @Test
    fun settingsFeatureConventionPublishesFinalTargetsAndDirectProcessorRegistrations() {
        val result = settingsRunner(
            settingsFixture(),
            ":feature:settings:kspAndroidMain",
            ":feature:settings:kspKotlinJvm",
            ":feature:settings:kspKotlinIosArm64",
            ":feature:settings:kspKotlinIosSimulatorArm64",
            ":feature:settings:verifySettingsFeatureConvention",
            "architectureCheck",
        ).build()

        assertEquals(
            "KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.settings",
            result.output.lineSequence().single { it.startsWith("KSP_PACKAGE_ROOTS=") },
            result.output,
        )
        settingsKspTasks.forEach { task ->
            assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome, result.output)
            assertFalse(result.output.contains("$task SKIPPED"), result.output)
            assertFalse(result.output.contains("$task NO-SOURCE"), result.output)
        }
    }

    @Test
    fun settingsFeatureRejectsForbiddenEdgesAndSharedApiExposure() {
        mapOf(
            SettingsMutation.DependsOnShared to "ARCH-EDGE :feature:settings [architecture] -> :shared",
            SettingsMutation.DependsOnApp to "ARCH-EDGE :feature:settings [architecture] -> :androidApp",
            SettingsMutation.DependsOnDatabase to "ARCH-EDGE :feature:settings [architecture] -> :core:database",
            SettingsMutation.DependsOnPlatform to "ARCH-EDGE :feature:settings [architecture] -> :core:platform",
            SettingsMutation.DependsOnPlayback to "ARCH-EDGE :feature:settings [architecture] -> :core:playback",
            SettingsMutation.DependsOnTagLib to "ARCH-EDGE :feature:settings [architecture] -> :taglib",
            SettingsMutation.DependsOnLibraryApi to "ARCH-EDGE :feature:settings [architecture] -> :feature:library:api",
        ).forEach { (mutation, diagnostic) ->
            assertRequiredDiagnostics(settingsFixture(mutation), listOf(diagnostic))
        }
        mapOf(
            SettingsMutation.DependsOnKoin to "SETTINGS-DIRECT-DEPENDENCY forbidden org.koin:koin-core:4.2.2",
            SettingsMutation.DependsOnDataStore to "SETTINGS-DIRECT-DEPENDENCY forbidden androidx.datastore:datastore-core:1.2.1",
        ).forEach { (mutation, diagnostic) ->
            val result = settingsRunner(settingsFixture(mutation), ":feature:settings:verifySettingsFeatureConvention").buildAndFail()
            assertTrue(result.output.contains(diagnostic), result.output)
        }
        assertExactFailure(
            settingsFixture(SettingsMutation.SharedCommonMainApiExposure),
            listOf("ARCH-EDGE :shared [commonMainApi] -> :feature:settings"),
        )
        assertRequiredDiagnostics(
            settingsFixture(SettingsMutation.IosExport),
            listOf("ARCH-IOS-EXPORT :shared -> :feature:settings"),
        )
    }

    @Test
    fun settingsFeatureProcessorRejectsPackageNamespaceAndPublicKdocMutations() {
        assertSettingsKspFailure(
            settingsFixture(SettingsMutation.WrongPackage),
            "ARCH-PACKAGE :feature:settings:SettingsFeature.kt (outside.fixture)",
        )
        assertSettingsKspFailure(
            settingsFixture(SettingsMutation.EmptyPackageRoots),
            "ARCH-PACKAGE :feature:settings:SettingsFeature.kt (com.eterocell.rhythhaus.settings)",
        )
        assertSettingsKspFailure(
            settingsFixture(SettingsMutation.MissingKDoc),
            "ARCH-KDOC :feature:settings:SettingsFeature.kt:2 (com.eterocell.rhythhaus.settings.SettingsFeature)",
        )
        assertExactFailure(
            settingsFixture(SettingsMutation.WrongAndroidNamespace),
            listOf("ARCH-RESOURCE :feature:settings [main] root=feature/settings/src/androidMain/res namespace=com.example.wrong"),
        )
        assertExactFailure(
            settingsFixture(SettingsMutation.WrongComposeNamespace),
            listOf(
                "ARCH-RESOURCE :feature:settings [androidMain] root=feature/settings/src/androidMain/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:settings [commonMain] root=feature/settings/src/commonMain/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:settings [iosArm64Main] root=feature/settings/src/iosArm64Main/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:settings [iosSimulatorArm64Main] root=feature/settings/src/iosSimulatorArm64Main/composeResources namespace=example.wrong.resources",
                "ARCH-RESOURCE :feature:settings [jvmMain] root=feature/settings/src/jvmMain/composeResources namespace=example.wrong.resources",
            ),
        )
    }

    @Test
    fun settingsResourceOwnershipIsProvenThroughFinalLedgerFixtureOnly() {
        assertSettingsResourceAudit(settingsResourceFixture())
        mapOf(
            SettingsMutation.MissingMovedResource to "SETTINGS-RESOURCE missing Settings key",
            SettingsMutation.DuplicateMovedResource to "SETTINGS-RESOURCE duplicate key in Settings EN",
            SettingsMutation.DuplicateMovedResourceZh to "SETTINGS-RESOURCE duplicate key in Settings ZH",
            SettingsMutation.ExtraMovedResource to "SETTINGS-RESOURCE unexpected Settings key",
            SettingsMutation.CrossOwnerDuplicateResource to "SETTINGS-RESOURCE duplicate key across Shared/Settings",
            SettingsMutation.WrongResourceOwner to "SETTINGS-RESOURCE wrong owner for moved key",
            SettingsMutation.LocaleParityDivergence to "SETTINGS-RESOURCE locale parity differs",
            SettingsMutation.InvalidResourceNamespace to "SETTINGS-RESOURCE wrong namespace",
            SettingsMutation.ForeignFeatureResourceImport to "SETTINGS-RESOURCE Settings imports Shared generated resources",
            SettingsMutation.ForeignSharedResourceImport to "SETTINGS-RESOURCE Shared imports Settings generated resources",
            SettingsMutation.MissingLogo to "SETTINGS-RESOURCE Settings logo owner mismatch",
        ).forEach { (mutation, diagnostic) ->
            val failure = assertFailsWith<AssertionError> { assertSettingsResourceAudit(settingsResourceFixture(mutation)) }
            assertTrue(failure.message.orEmpty().contains(diagnostic), failure.message)
        }
    }

    @Test
    fun playlistsFeatureConventionPublishesRootsAndKspRegistrations() {
        val result = playlistsRunner(
            playlistsFeatureFixture(),
            ":feature:playlists:impl:kspAndroidMain",
            ":feature:playlists:impl:kspKotlinJvm",
            ":feature:playlists:impl:kspKotlinIosArm64",
            ":feature:playlists:impl:kspKotlinIosSimulatorArm64",
            ":feature:playlists:impl:verifyPlaylistsFeatureConvention",
            "architectureCheck",
        ).build()

        assertEquals(
            "KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.library,com.eterocell.rhythhaus.library.ui,com.eterocell.rhythhaus.playlistbackup",
            result.output.lineSequence().single { it.startsWith("KSP_PACKAGE_ROOTS=") },
            result.output,
        )
        assertEquals(
            listOf(
                "KSP_REGISTRATION=:feature:playlists:impl|kspAndroid|:architecture-processor",
                "KSP_REGISTRATION=:feature:playlists:impl|kspIosArm64|:architecture-processor",
                "KSP_REGISTRATION=:feature:playlists:impl|kspIosSimulatorArm64|:architecture-processor",
                "KSP_REGISTRATION=:feature:playlists:impl|kspJvm|:architecture-processor",
            ),
            result.output.lineSequence().filter { it.startsWith("KSP_REGISTRATION=") }.toList(),
            result.output,
        )
        listOf("AndroidMain", "KotlinJvm", "KotlinIosArm64", "KotlinIosSimulatorArm64").forEach { target ->
            val task = ":feature:playlists:impl:ksp$target"
            assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome, result.output)
            assertTrue(!result.output.contains("$task SKIPPED"), result.output)
            assertTrue(!result.output.contains("$task NO-SOURCE"), result.output)
        }
        assertEquals(
            setOf(
                ":core:database", ":core:model", ":core:platform", ":core:playback", ":core:ui",
                ":feature:library:api", ":feature:playlists:api",
            ),
            result.output.dependencyEdges().split(", ")
                .filter { it.startsWith(":feature:playlists:impl|") }
                .map { it.substringAfterLast('|') }
                .toSet(),
            result.output,
        )
    }

    @Test
    fun playlistsResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports() {
        val root = File(System.getProperty("rhythhaus.rootDir"))
        val sharedEn = root.resolve("shared/src/commonMain/composeResources/values/strings.xml")
        val sharedZh = root.resolve("shared/src/commonMain/composeResources/values-zh/strings.xml")
        val featureEn = root.resolve("feature/playlists/impl/src/commonMain/composeResources/values/strings.xml")
        val featureZh = root.resolve("feature/playlists/impl/src/commonMain/composeResources/values-zh/strings.xml")

        val sharedEnglish = stringNames(sharedEn)
        val sharedChinese = stringNames(sharedZh)
        val featureEnglish = stringNames(featureEn)
        val featureChinese = stringNames(featureZh)
        val ownershipKeys = { names: List<String> ->
            names.filter { it.startsWith("playlist_") || it in setOf("cancel", "close") }.toSet()
        }

        assertNoDuplicateStringNames(sharedEn)
        assertNoDuplicateStringNames(sharedZh)
        assertNoDuplicateStringNames(featureEn)
        assertNoDuplicateStringNames(featureZh)
        assertEquals(ownershipKeys(sharedEnglish), ownershipKeys(sharedChinese), "Shared EN/ZH playlist ownership differs")
        assertEquals(ownershipKeys(featureEnglish), ownershipKeys(featureChinese), "Feature EN/ZH playlist ownership differs")
        assertTrue(ownershipKeys(sharedEnglish).intersect(ownershipKeys(featureEnglish)).isEmpty(), "Playlist resource keys have multiple owners")

        root.resolve("shared/src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                assertFalse(
                    source.readText().contains("rhythhaus.feature.playlists.generated.resources"),
                    "Shared imports feature generated resources: ${source.relativeTo(root)}",
                )
            }
        root.resolve("feature/playlists/impl/src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                assertFalse(
                    source.readText().contains("rhythhaus.shared.generated.resources"),
                    "Feature imports Shared generated resources: ${source.relativeTo(root)}",
                )
            }
    }

    @Test
    fun playlistsFeatureRejectsForbiddenSharedAndImplementationEdges() {
        mapOf(
            ":shared" to listOf(
                "ARCH-CYCLE :feature:playlists:impl -> :shared -> :feature:playlists:impl",
                "ARCH-EDGE :feature:playlists:impl [architecture] -> :shared",
            ),
            ":feature:library:impl" to listOf(
                "ARCH-EDGE :feature:playlists:impl [architecture] -> :feature:library:impl",
            ),
        ).forEach { (forbidden, expectedDiagnostics) ->
            assertExactFailure(
                playlistsFeatureFixture(forbiddenDependency = forbidden),
                expectedDiagnostics,
            )
        }
    }

    @Test
    fun rejectsPlaylistsFeatureOutsidePackageRoot() =
        assertPlaylistsKspFailure(
            playlistsFeatureFixture("package outside.fixture\n/** Invalid package. */\npublic class InvalidFeature"),
            "ARCH-PACKAGE :feature:playlists:impl:PlaylistFeature.kt (outside.fixture)",
        )

    @Test
    fun playlistsFeatureRejectsEmptyConfiguredPackageRoots() =
        assertPlaylistsKspFailure(
            playlistsFeatureFixture(emptyPackageRoots = true),
            "ARCH-PACKAGE :feature:playlists:impl:PlaylistFeature.kt (com.eterocell.rhythhaus.library)",
        )

    @Test
    fun rejectsPlaylistsFeatureUndocumentedPublicDeclaration() =
        assertPlaylistsKspFailure(
            playlistsFeatureFixture("package com.eterocell.rhythhaus.library\npublic class MissingFeatureKDoc"),
            "ARCH-KDOC :feature:playlists:impl:PlaylistFeature.kt:2 (com.eterocell.rhythhaus.library.MissingFeatureKDoc)",
        )

    private fun assertFailure(mutation: Mutation, expectedRules: List<String>, relevantText: String) =
        assertFailure(fixture(mutation), expectedRules, relevantText)

    private fun assertFailure(projectDir: File, expectedRules: List<String>, relevantText: String) {
        val result = runner(projectDir).buildAndFail()
        assertTrue(result.output.contains("Execution failed for task ':architectureCheck'"), result.output)
        val diagnosticBlock = result.output.substringAfter("Execution failed for task ':architectureCheck'").substringBefore("* Try:")
        val diagnosticLines = diagnosticBlock.lineSequence().filter { DIAGNOSTIC_RULE.containsMatchIn(it) }.toList()
        val actualRules = diagnosticLines.map { line -> DIAGNOSTIC_RULE.find(line)!!.groups[1]?.value ?: DIAGNOSTIC_RULE.find(line)!!.groups[2]!!.value }
        assertEquals(expectedRules, actualRules, result.output)
        assertEquals(diagnosticLines.distinct(), diagnosticLines, result.output)
        assertTrue(result.output.contains(relevantText), result.output)
    }

    private fun assertExactFailure(
        projectDir: File,
        expectedDiagnostics: List<String>,
    ) = assertExactFailure(projectDir, "architectureCheck", expectedDiagnostics)

    private fun assertExactFailure(
        projectDir: File,
        task: String,
        expectedDiagnostics: List<String>,
    ) {
        val result = nowPlayingRunner(projectDir, task).buildAndFail()
        assertExactDiagnostics(result.output, expectedDiagnostics)
    }

    private fun stringNames(file: File): List<String> =
        Regex("""<string\\s+name=\"([^\"]+)\"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toList()

    private fun searchStringNames(file: File): List<String> =
        Regex("""<string\s+name=\"([^\"]+)\"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toList()

    private fun assertNoDuplicateStringNames(file: File) {
        val names = stringNames(file)
        assertEquals(names.size, names.toSet().size, "Duplicate resource keys in ${file.path}")
    }

    private fun assertExactDiagnostics(output: String, expectedDiagnostics: List<String>) {
        assertTrue(output.contains("Execution failed for task ':architectureCheck'"), output)
        val diagnosticBlock = output.substringAfter("Execution failed for task ':architectureCheck'").substringBefore("* Try:")
        val diagnostics = diagnosticBlock.lineSequence()
            .filter { DIAGNOSTIC_RULE.containsMatchIn(it) }
            .map { it.trimStart().removePrefix("> ") }
            .toList()
        assertEquals(expectedDiagnostics, diagnostics, output)
        assertEquals(diagnostics.distinct(), diagnostics, output)
    }

    private fun assertRequiredDiagnostics(
        projectDir: File,
        requiredDiagnostics: List<String>,
    ) {
        val result = runner(projectDir).buildAndFail()
        assertTrue(result.output.contains("Execution failed for task ':architectureCheck'"), result.output)
        requiredDiagnostics.forEach { diagnostic ->
            assertTrue(result.output.contains(diagnostic), result.output)
        }
    }

    private fun assertKspCompilationFailure(source: String, expectedDiagnostic: String) {
        val projectDir = kspFixture(source)
        val result = runner(projectDir, "compileKotlinJvm").buildAndFail()
        assertTrue(result.output.contains(expectedDiagnostic), result.output)
    }

    private fun assertKspCompilationFailure(projectDir: File, task: String, expectedDiagnostic: String) {
        val processorJar = externallyProvidedProcessorJarOrSkip()
        append(
            projectDir,
            ":feature:nowplaying",
            "dependencies.add(\"kspJvm\", files(\"${processorJar.invariantSeparatorsPath}\"))",
        )
        val result = nowPlayingRunner(projectDir, task).buildAndFail()
        assertTrue(result.output.contains(expectedDiagnostic), result.output)
        assertTrue(result.output.contains(":feature:nowplaying:kspKotlinJvm"), result.output)
        assertEquals(
            TaskOutcome.FAILED,
            result.task(":feature:nowplaying:kspKotlinJvm")?.outcome,
            result.output,
        )
    }

    private fun assertKspTaskRan(output: String) {
        assertTrue(output.contains(":core:model:kspKotlinJvm"), output)
        assertTrue(!output.contains(":core:model:kspKotlinJvm SKIPPED"), output)
        assertTrue(!output.contains(":core:model:kspKotlinJvm NO-SOURCE"), output)
    }

    private fun assertPlaylistsKspFailure(projectDir: File, expectedDiagnostic: String) {
        val result = playlistsRunner(projectDir, ":feature:playlists:impl:compileKotlinJvm").buildAndFail()

        assertTrue(result.output.contains(expectedDiagnostic), result.output)
        assertTrue(result.output.contains(":feature:playlists:impl:kspKotlinJvm"), result.output)
        assertTrue(!result.output.contains(":feature:playlists:impl:kspKotlinJvm SKIPPED"), result.output)
        assertTrue(!result.output.contains(":feature:playlists:impl:kspKotlinJvm NO-SOURCE"), result.output)
        assertEquals(TaskOutcome.FAILED, result.task(":feature:playlists:impl:kspKotlinJvm")?.outcome, result.output)
    }

    private fun assertSearchKspFailure(projectDir: File, expectedDiagnostic: String) {
        val result = searchRunner(projectDir, ":feature:search:compileKotlinJvm").buildAndFail()
        assertEquals(listOf(expectedDiagnostic), searchKspDiagnostics(result.output), result.output)
        assertEquals(TaskOutcome.FAILED, result.task(":feature:search:kspKotlinJvm")?.outcome, result.output)
    }

    private fun assertSettingsKspFailure(projectDir: File, expectedDiagnostic: String) {
        val result = settingsRunner(projectDir, ":feature:settings:compileKotlinJvm").buildAndFail()
        assertEquals(listOf(expectedDiagnostic), settingsKspDiagnostics(result.output), result.output)
        assertEquals(TaskOutcome.FAILED, result.task(":feature:settings:kspKotlinJvm")?.outcome, result.output)
    }

    private fun assertSearchKdocFailureThenRestoresGreen(sourceText: String, expectedDiagnostic: String) {
        val projectDir = searchFixture(sourceOverride = sourceText)
        assertSearchKspFailure(projectDir, expectedDiagnostic)
        source(
            projectDir,
            ":feature:search",
            "SearchFeature.kt",
            "package com.eterocell.rhythhaus.search\n/** A documented Search feature declaration. */\npublic class SearchFeature",
        )
        val green = searchRunner(projectDir, ":feature:search:kspKotlinJvm").build()
        assertSearchKspTaskOutcome(green, ":feature:search:kspKotlinJvm", TaskOutcome.SUCCESS)
    }

    private fun assertSearchKspTaskOutcome(
        result: org.gradle.testkit.runner.BuildResult,
        task: String,
        outcome: TaskOutcome,
        expectedDiagnostic: String? = null,
    ) {
        assertEquals(expectedDiagnostic?.let(::listOf).orEmpty(), searchKspDiagnostics(result.output), result.output)
        assertEquals(outcome, result.task(task)?.outcome, result.output)
        assertFalse(result.output.contains("$task SKIPPED"), result.output)
        assertFalse(result.output.contains("$task NO-SOURCE"), result.output)
        assertFalse(result.output.contains("$task UP-TO-DATE"), result.output)
    }

    private fun kspDiagnostics(output: String): List<String> =
        output
            .lineSequence()
            .mapNotNull { line ->
                Regex("(ARCH-(?:PACKAGE|KDOC) .*)").find(line)?.groupValues?.get(1)
            }
            .toList()

    private fun searchKspDiagnostics(output: String): List<String> =
        output.lineSequence()
            .mapNotNull { line ->
                Regex("(ARCH-(?:PACKAGE|KDOC) :feature:search:[^\\r\\n]+)").find(line)?.groupValues?.get(1)
            }
            .toList()

    private fun settingsKspDiagnostics(output: String): List<String> =
        output.lineSequence()
            .mapNotNull { line ->
                Regex("(ARCH-(?:PACKAGE|KDOC) :feature:settings:[^\\r\\n]+)").find(line)?.groupValues?.get(1)
            }
            .toList()

    private fun fixture(mutation: Mutation? = null): File {
        val projectDir = architectureFixture("architecture-check")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-check-consumer"
            include(":androidApp", ":desktopApp", ":shared", ":taglib", ":architecture-processor", ":core:model", ":core:database", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("plugins { id(\"build-logic.architecture-check\") }")
        modules.forEach { module(projectDir, it) }
        kmpModule(projectDir, ":core:model", strict = true)
        kmpModule(projectDir, ":core:database", strict = true)
        kmpModule(projectDir, ":shared", strict = false)
        kmpModule(projectDir, ":feature:library:api", strict = true)
        dependency(projectDir, ":androidApp", ":shared")
        dependency(projectDir, ":desktopApp", ":shared")
        dependency(projectDir, ":shared", ":taglib")
        dependency(projectDir, ":shared", ":feature:library:api")
        dependency(projectDir, ":shared", ":feature:playlists:api")
        dependency(projectDir, ":shared", ":feature:library:impl")
        dependency(projectDir, ":shared", ":feature:playlists:impl")
        dependency(projectDir, ":feature:library:impl", ":feature:library:api")
        dependency(projectDir, ":feature:playlists:impl", ":feature:playlists:api")
        dependency(projectDir, ":feature:library:api", ":core:model")
        source(projectDir, ":core:model", "DocumentedModel.kt", documentedModelSource)
        source(projectDir, ":feature:library:api", "LibraryRepository.kt", "package com.eterocell.rhythhaus.library\n/** A library repository. */\npublic interface LibraryRepository")
        source(projectDir, ":feature:playlists:api", "PlaylistSummary.kt", "package com.eterocell.rhythhaus.library\n/** A playlist summary. */\npublic data class PlaylistSummary(public val id: String)")
        source(projectDir, ":shared", "UsesLibraryRepository.kt", "package com.eterocell.rhythhaus\nimport com.eterocell.rhythhaus.library.LibraryRepository\ninternal fun use(repository: LibraryRepository) = repository")
        resource(projectDir, ":core:model", "src/commonMain/resources/model.txt")
        sqlDelightPluginModule(projectDir, ":core:database")
        driver(projectDir, ":core:database")
        sqlArtifact(projectDir, ":core:database", "src/commonMain/sqldelight/fixture.sq")
        append(projectDir, ":core:database", "kotlin { explicitApi() }")

        when (mutation) {
            Mutation.DependencyCycle -> {
                dependency(projectDir, ":core:model", ":core:database")
                dependency(projectDir, ":core:database", ":core:model")
            }
            Mutation.SelfDependency -> dependency(projectDir, ":core:model", ":core:model")
            Mutation.ProductionKspProcessor -> processorDependency(projectDir, "kspJvm")
            Mutation.ImplementationProcessor -> processorDependency(projectDir, "implementation")
            Mutation.ForbiddenEdge -> dependency(projectDir, ":feature:library:impl", ":feature:playlists:impl")
            Mutation.PlaylistsApiDependsOnCoreModel -> dependency(projectDir, ":feature:playlists:api", ":core:model")
            Mutation.LibraryApiDependsOnCoreDatabase -> dependency(projectDir, ":feature:library:api", ":core:database")
            Mutation.LibraryApiDependsOnShared -> dependency(projectDir, ":feature:library:api", ":shared")
            Mutation.LibraryApiDependsOnImplementation -> dependency(projectDir, ":feature:library:api", ":feature:library:impl")
            Mutation.LibraryImplementationDependsOnShared -> dependency(projectDir, ":feature:library:impl", ":shared")
            Mutation.SqlDelightRuntimeAndReadme -> {
                dependencyNotation(projectDir, ":core:database", "app.cash.sqldelight:runtime:2.3.2")
                dependencyNotation(projectDir, ":core:database", "app.cash.sqldelight:coroutines-extensions:2.3.2")
                resource(projectDir, ":core:database", "src/commonMain/sqldelight/README.md")
            }
            Mutation.MissingSqlDelightOwner -> removeDriver(projectDir, ":core:database")
            Mutation.TwoSqlDelightOwners -> {
                sqlDelightPluginModule(projectDir, ":shared")
                driver(projectDir, ":shared")
                sqlArtifact(projectDir, ":shared", "fixture.sq")
            }
            Mutation.ArbitrarySqlDelightOwner -> {
                removeDriver(projectDir, ":core:database")
                sqlDelightPluginModule(projectDir, ":shared")
                driver(projectDir, ":shared")
                sqlArtifact(projectDir, ":shared", "database.sq")
            }
            Mutation.SpoofedSqlDelightDriver -> {
                removeDriver(projectDir, ":core:database")
                append(
                    projectDir,
                    ":core:database",
                    "configurations.create(\"spoofedDriver\"); dependencies.add(\"spoofedDriver\", \"app.cash.sqldelight:sqlite-driver:2.3.2\")",
                )
            }
            Mutation.ExplicitSupportedSqlDelightRoot -> {
                sqlArtifact(projectDir, ":core:database", "src/jvmMain/sqldelight/configured.sqm")
                append(
                    projectDir,
                    ":core:database",
                    "sqldelight { databases.named(\"RhythHausDatabase\") { srcDirs.from(file(\"src/jvmMain/sqldelight\")) } }",
                )
            }
            Mutation.ExplicitApiWarningWithStrictCompilerArgs -> kmpModule(projectDir, ":core:model", strict = false, strictCompilerArgs = true)
            Mutation.UnapprovedIosExport -> iosExport(projectDir)
            null -> Unit
        }
        return projectDir
    }

    private fun corePlaybackFixture(
        dependsOnShared: Boolean = false,
        exportModel: Boolean = false,
    ): File {
        val projectDir = fixture()
        val settings = projectDir.resolve("settings.gradle.kts")
        settings.writeText(
            settings.readText().replace(
                ":core:ui\", \":feature",
                ":core:ui\", \":core:platform\", \":core:playback\", \":architecture-processor\", \":feature",
            ),
        )
        val processorDir = moduleDir(projectDir, ":architecture-processor")
        processorProject().resolve("src").copyRecursively(processorDir.resolve("src"), overwrite = true)
        processorDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("org.jetbrains.kotlin.jvm") }
            repositories { mavenCentral() }
            dependencies { implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10") }
            """.trimIndent(),
        )
        module(projectDir, ":core:platform")
        kmpModule(projectDir, ":core:platform", strict = true)
        module(projectDir, ":core:playback")
        buildFile(projectDir, ":core:playback").writeText(
            "plugins { id(\"build-logic.kmp.core\") }\nconfigurations.maybeCreate(\"architecture\")\nkotlin { jvm() }",
        )
        dependency(projectDir, ":core:playback", ":core:model")
        dependency(projectDir, ":core:playback", ":core:platform")
        dependency(projectDir, ":shared", ":core:playback")
        source(
            projectDir,
            ":core:playback",
            "PlaybackContract.kt",
            """
            package com.eterocell.rhythhaus

            /** Package-stable playback contract fixture. */
            public class PlaybackContract
            """.trimIndent(),
        )
        source(
            projectDir,
            ":core:playback",
            "session/PlaybackSessionController.kt",
            """
            package com.eterocell.rhythhaus.session

            /** A package-stable playback session port. */
            public interface PlaybackSessionController
            """.trimIndent(),
        )
        val exports =
            buildString {
                append("export(project(\":core:playback\"))")
                if (exportModel) append("; export(project(\":core:model\"))")
            }
        append(projectDir, ":shared", "kotlin { iosArm64().binaries.framework { $exports } }")
        if (dependsOnShared) dependency(projectDir, ":core:playback", ":shared")
        return projectDir
    }

    private fun nowPlayingFixture(mutation: NowPlayingMutation? = null): File {
        val projectDir = fixture()
        val settings = projectDir.resolve("settings.gradle.kts")
        settings.writeText(
            settings.readText().replace(
                ":feature:playlists:impl\")",
                ":feature:playlists:impl\", \":core:playback\", \":architecture-processor\", \":feature:nowplaying\")",
            ),
        )
        val processorJar = externallyProvidedProcessorJarOrSkip()
        module(projectDir, ":architecture-processor")
        buildFile(projectDir, ":architecture-processor").writeText(
            "configurations.create(\"default\")\nartifacts { add(\"default\", file(\"${processorJar.invariantSeparatorsPath}\")) }",
        )
        module(projectDir, ":core:playback")
        kmpModule(projectDir, ":core:playback", strict = true)
        module(projectDir, ":core:ui")
        kmpModule(projectDir, ":core:ui", strict = true)
        module(projectDir, ":feature:nowplaying")
        val resourceNamespace =
            if (mutation == NowPlayingMutation.InvalidResourceNamespace) "<invalid>"
            else "rhythhaus.feature.nowplaying.generated.resources"
        buildFile(projectDir, ":feature:nowplaying").writeText(
            """
            import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget
            plugins {
                id("build-logic.kmp.feature.impl")
                id("build-logic.android.kmp.library")
                id("build-logic.compose-resources")
                id("org.jetbrains.kotlin.plugin.compose")
            }
            configurations.maybeCreate("architecture")
            extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
                namespace("$resourceNamespace")
            }
            kotlin {
                android {
                    namespace = "com.eterocell.rhythhaus.nowplaying"
                    compileSdk = 37
                    minSdk = 29
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                    withHostTest {}
                    androidResources { enable = true }
                }
                jvm()
                iosArm64()
                iosSimulatorArm64()
            }
            dependencies {
                add("architecture", project(":core:playback"))
                add("architecture", project(":core:ui"))
                add("commonMainImplementation", "org.jetbrains.compose.runtime:runtime:1.11.1")
            }
            """.trimIndent(),
        )
        source(projectDir, ":feature:nowplaying", "NowPlaying.kt", "package com.eterocell.rhythhaus.nowplaying\n/** A documented feature declaration. */\npublic class NowPlaying")
        writeKotlinSource(projectDir, ":feature:nowplaying", "src/commonMain/kotlin/com/eterocell/rhythhaus/ui/NowPlayingUi.kt", "package com.eterocell.rhythhaus.ui\n/** A documented feature UI declaration. */\npublic class NowPlayingUi")
        val featureStrings = moduleDir(projectDir, ":feature:nowplaying")
            .resolve("src/commonMain/composeResources/values/strings.xml")
        featureStrings.parentFile.mkdirs()
        featureStrings.writeText("<resources><string name=\"fixture\">fixture</string></resources>")
        dependency(projectDir, ":shared", ":feature:nowplaying")
        dependency(projectDir, ":feature:nowplaying", ":core:playback")
        dependency(projectDir, ":feature:nowplaying", ":core:ui")
        projectDir.resolve("build.gradle.kts").appendText(
            """

            tasks.named("architectureCheck") {
                doFirst {
                    logger.lifecycle("TEST-NOWPLAYING-RESOURCES=" + inputs.properties["resourceRecords"])
                }
            }
            """.trimIndent(),
        )
        when (mutation) {
            NowPlayingMutation.DependsOnShared -> dependency(projectDir, ":feature:nowplaying", ":shared")
            NowPlayingMutation.DependsOnTagLib -> dependency(projectDir, ":feature:nowplaying", ":taglib")
            NowPlayingMutation.DependsOnLibraryApi -> dependency(projectDir, ":feature:nowplaying", ":feature:library:api")
            NowPlayingMutation.DependsOnLibraryImplementation -> dependency(projectDir, ":feature:nowplaying", ":feature:library:impl")
            NowPlayingMutation.DependsOnApp -> dependency(projectDir, ":feature:nowplaying", ":androidApp")
            NowPlayingMutation.DependsOnCoreModel -> dependency(projectDir, ":feature:nowplaying", ":core:model")
            NowPlayingMutation.DependsOnPlaylistsImplementation -> dependency(projectDir, ":feature:nowplaying", ":feature:playlists:impl")
            NowPlayingMutation.IosExport -> append(
                projectDir,
                ":shared",
                "kotlin { iosArm64().binaries.framework { export(project(\":feature:nowplaying\")) } }",
            )
            NowPlayingMutation.InvalidResourceNamespace,
            null,
            -> Unit
        }
        return projectDir
    }

    private fun searchFixture(
        mutation: SearchMutation? = null,
        sourceOverride: String? = null,
    ): File {
        val projectDir = fixture()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-search-feature"
            include(":androidApp", ":desktopApp", ":shared", ":taglib", ":architecture-processor", ":core:model", ":core:database", ":core:platform", ":core:playback", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl", ":feature:search")
            """.trimIndent(),
        )
        val processorJar = externallyProvidedProcessorJarOrSkip()
        module(projectDir, ":architecture-processor")
        buildFile(projectDir, ":architecture-processor").writeText("configurations.create(\"default\")\nartifacts { add(\"default\", file(\"${processorJar.invariantSeparatorsPath}\")) }")
        listOf(":core:platform", ":core:playback", ":core:ui").forEach { module(projectDir, it); kmpModule(projectDir, it, strict = true) }
        module(projectDir, ":feature:search")
        val composeNamespace =
            if (mutation == SearchMutation.WrongComposeNamespace) "example.wrong.resources"
            else "rhythhaus.feature.search.generated.resources"
        val androidNamespace =
            if (mutation == SearchMutation.WrongAndroidNamespace) "com.example.wrong"
            else "com.eterocell.rhythhaus.search"
        buildFile(projectDir, ":feature:search").writeText(
            """
            import com.eterocell.gradle.architecture.ArchitectureModelRegistry
            import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
            import org.gradle.api.artifacts.ProjectDependency
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget
            plugins {
                id("build-logic.kmp.feature.impl")
                id("build-logic.android.kmp.library")
                id("build-logic.compose-resources")
                id("org.jetbrains.kotlin.plugin.compose")
            }
            configurations.maybeCreate("architecture")
            extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") { namespace("$composeNamespace") }
            kotlin {
                android { namespace = "$androidNamespace"; compileSdk = 37; minSdk = 29; compilerOptions.jvmTarget.set(JvmTarget.JVM_11); withHostTest {}; androidResources { enable = true } }
                jvm(); iosArm64(); iosSimulatorArm64()
            }
            dependencies {
                add("commonMainImplementation", "org.jetbrains.compose.runtime:runtime:1.11.1")
            }

            val searchKspExpected = listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspJvm")
            afterEvaluate {
                @Suppress("UNCHECKED_CAST")
                val searchKspArguments = extensions.getByName("ksp")
                    .javaClass.getMethod("getArguments")
                    .invoke(extensions.getByName("ksp")) as Map<String, String>
                val searchKspRegistrations = ArchitectureModelRegistry.forRoot(project).snapshot().kspRegistrations
                    .filter { it.module == project.path }
                    .map { it.module + "|" + it.configuration + "|" + it.processor }
                    .sorted()
                val searchKspConfigurationMatches = searchKspExpected.associateWith { configurationName ->
                    val dependencies = configurations.getByName(configurationName).dependencies
                        .withType(ProjectDependency::class.java)
                        .filter { it.path == ":architecture-processor" }
                    dependencies.size == 1 && configurations.getByName(configurationName).dependencies.size == 1
                }
                tasks.register("verifySearchFeatureConvention") {
                    inputs.property("packageRoots", searchKspArguments["architecture.packageRoots"])
                    inputs.property("registrations", searchKspRegistrations)
                    inputs.property("configurationMatches", searchKspConfigurationMatches)
                    doLast {
                        val packageRoots = inputs.properties.getValue("packageRoots")
                        val registrations = inputs.properties.getValue("registrations") as List<*>
                        val configurationMatches = inputs.properties.getValue("configurationMatches") as Map<*, *>
                        check(packageRoots == "com.eterocell.rhythhaus.search") {
                            "KSP package roots mismatch: ${'$'}packageRoots"
                        }
                        check(registrations == searchKspExpected.map { ":feature:search|" + it + "|:architecture-processor" }) {
                            "KSP registry/configuration mismatch: ${'$'}registrations"
                        }
                        configurationMatches.forEach { (configurationName, matches) ->
                            check(matches == true) { "KSP registry/configuration mismatch: ${'$'}configurationName" }
                        }
                        println("KSP_PACKAGE_ROOTS=" + packageRoots)
                        registrations.forEach { println("KSP_REGISTRATION=" + it) }
                    }
                }
            }
            """.trimIndent(),
        )
        val source = sourceOverride ?: when (mutation) {
            SearchMutation.WrongPackage -> "package outside.fixture\n/** Invalid package. */\npublic class SearchFeature"
            SearchMutation.MissingKDoc -> "package com.eterocell.rhythhaus.search\npublic class SearchFeature"
            else -> "package com.eterocell.rhythhaus.search\n/** A documented Search feature declaration. */\npublic class SearchFeature"
        }
        source(projectDir, ":feature:search", "SearchFeature.kt", source)
        val strings = moduleDir(projectDir, ":feature:search").resolve("src/commonMain/composeResources/values/strings.xml")
        strings.parentFile.mkdirs()
        strings.writeText("<resources><string name=\"search_placeholder\">Search</string></resources>")
        append(projectDir, ":shared", "dependencies.add(\"commonMainImplementation\", project(\":feature:search\"))")
        dependency(projectDir, ":feature:search", ":feature:library:api")
        dependency(projectDir, ":feature:search", ":core:ui")
        when (mutation) {
            SearchMutation.DependsOnShared -> dependency(projectDir, ":feature:search", ":shared")
            SearchMutation.DependsOnPlayback -> dependency(projectDir, ":feature:search", ":core:playback")
            SearchMutation.DependsOnTagLib -> dependency(projectDir, ":feature:search", ":taglib")
            SearchMutation.DependsOnDatabase -> dependency(projectDir, ":feature:search", ":core:database")
            SearchMutation.DependsOnPlatform -> dependency(projectDir, ":feature:search", ":core:platform")
            SearchMutation.DependsOnImplementation -> dependency(projectDir, ":feature:search", ":feature:library:impl")
            SearchMutation.DependsOnApp -> dependency(projectDir, ":feature:search", ":androidApp")
            SearchMutation.IosExport -> append(projectDir, ":shared", "kotlin { iosArm64().binaries.framework { export(project(\":feature:search\")) } }")
            SearchMutation.SharedCommonMainApiExposure -> {
                removeText(projectDir, ":shared", "dependencies.add(\"commonMainImplementation\", project(\":feature:search\"))")
                append(projectDir, ":shared", "dependencies.add(\"commonMainApi\", project(\":feature:search\"))")
            }
            SearchMutation.RemoveKspJvmProcessor -> append(
                projectDir,
                ":feature:search",
                "configurations.named(\"kspJvm\") { dependencies.removeIf { it is ProjectDependency && it.path == \":architecture-processor\" } }",
            )
            SearchMutation.EmptyPackageRoots -> append(
                projectDir,
                ":feature:search",
                "afterEvaluate { extensions.configure<com.google.devtools.ksp.gradle.KspExtension> { arg(\"architecture.packageRoots\", \"\") } }",
            )
            else -> Unit
        }
        projectDir.resolve("build.gradle.kts").writeText("plugins { id(\"build-logic.architecture-check\") }")
        return projectDir
    }

    private fun settingsFixture(mutation: SettingsMutation? = null): File {
        val projectDir = fixture()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-settings-feature"
            include(":androidApp", ":desktopApp", ":shared", ":taglib", ":architecture-processor", ":core:model", ":core:database", ":core:platform", ":core:playback", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl", ":feature:settings")
            """.trimIndent(),
        )
        val processorJar = externallyProvidedProcessorJarOrSkip()
        module(projectDir, ":architecture-processor")
        buildFile(projectDir, ":architecture-processor").writeText("configurations.create(\"default\")\nartifacts { add(\"default\", file(\"${processorJar.invariantSeparatorsPath}\")) }")
        listOf(":core:platform", ":core:playback", ":core:ui").forEach { module(projectDir, it); kmpModule(projectDir, it, strict = true) }
        module(projectDir, ":feature:settings")
        val composeNamespace = if (mutation == SettingsMutation.WrongComposeNamespace) "example.wrong.resources" else "rhythhaus.feature.settings.generated.resources"
        val androidNamespace = if (mutation == SettingsMutation.WrongAndroidNamespace) "com.example.wrong" else "com.eterocell.rhythhaus.settings"
        buildFile(projectDir, ":feature:settings").writeText(
            """
            import com.eterocell.gradle.architecture.ArchitectureModelRegistry
            import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
            import com.google.devtools.ksp.gradle.KspExtension
            import org.gradle.api.artifacts.ProjectDependency
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget
            plugins {
                id("build-logic.kmp.feature.impl")
                id("build-logic.android.kmp.library")
                id("build-logic.compose-resources")
                id("org.jetbrains.kotlin.plugin.compose")
            }
            configurations.maybeCreate("architecture")
            extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") { namespace("$composeNamespace") }
            kotlin {
                android { namespace = "$androidNamespace"; compileSdk = 37; minSdk = 29; compilerOptions.jvmTarget.set(JvmTarget.JVM_11); withHostTest {}; androidResources { enable = true } }
                jvm(); iosArm64(); iosSimulatorArm64()
            }
            dependencies { add("commonMainImplementation", "org.jetbrains.compose.runtime:runtime:1.11.1") }
            val expected = listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspJvm")
            afterEvaluate {
                @Suppress("UNCHECKED_CAST")
                val arguments = extensions.getByName("ksp").javaClass.getMethod("getArguments").invoke(extensions.getByName("ksp")) as Map<String, String>
                val registrations = ArchitectureModelRegistry.forRoot(project).snapshot().kspRegistrations
                    .filter { it.module == project.path }.map { it.module + "|" + it.configuration + "|" + it.processor }.sorted()
                val configurationsMatch = expected.associateWith { name ->
                    val dependencies = configurations.getByName(name).dependencies.withType(ProjectDependency::class.java)
                        .filter { it.path == ":architecture-processor" }
                    dependencies.size == 1 && configurations.getByName(name).dependencies.size == 1
                }
                tasks.register("verifySettingsFeatureConvention") {
                    doLast {
                        check(arguments["architecture.packageRoots"] == "com.eterocell.rhythhaus.settings")
                        check(registrations == expected.map { ":feature:settings|" + it + "|:architecture-processor" })
                        configurationsMatch.forEach { (name, matches) -> check(matches == true) { "KSP registry/configuration mismatch: ${'$'}name" } }
                        val forbiddenDirectDependencies = configurations
                            .flatMap { configuration -> configuration.dependencies }
                            .map { dependency -> "${'$'}{dependency.group}:${'$'}{dependency.name}:${'$'}{dependency.version}" }
                            .filter { it in setOf("org.koin:koin-core:4.2.2", "androidx.datastore:datastore-core:1.2.1") }
                        check(forbiddenDirectDependencies.isEmpty()) {
                            "SETTINGS-DIRECT-DEPENDENCY forbidden " + forbiddenDirectDependencies.sorted().joinToString()
                        }
                        println("KSP_PACKAGE_ROOTS=" + arguments["architecture.packageRoots"])
                    }
                }
            }
            """.trimIndent(),
        )
        val source = when (mutation) {
            SettingsMutation.WrongPackage -> "package outside.fixture\n/** Invalid package. */\npublic class SettingsFeature"
            SettingsMutation.MissingKDoc -> "package com.eterocell.rhythhaus.settings\npublic class SettingsFeature"
            else -> "package com.eterocell.rhythhaus.settings\n/** A documented Settings feature declaration. */\npublic class SettingsFeature"
        }
        source(projectDir, ":feature:settings", "SettingsFeature.kt", source)
        val strings = moduleDir(projectDir, ":feature:settings").resolve("src/commonMain/composeResources/values/strings.xml")
        strings.parentFile.mkdirs()
        strings.writeText("<resources><string name=\"appearance\">Appearance</string></resources>")
        append(projectDir, ":shared", "dependencies.add(\"commonMainImplementation\", project(\":feature:settings\"))")
        dependency(projectDir, ":feature:settings", ":core:ui")
        when (mutation) {
            SettingsMutation.DependsOnShared -> dependency(projectDir, ":feature:settings", ":shared")
            SettingsMutation.DependsOnApp -> dependency(projectDir, ":feature:settings", ":androidApp")
            SettingsMutation.DependsOnDatabase -> dependency(projectDir, ":feature:settings", ":core:database")
            SettingsMutation.DependsOnPlatform -> dependency(projectDir, ":feature:settings", ":core:platform")
            SettingsMutation.DependsOnPlayback -> dependency(projectDir, ":feature:settings", ":core:playback")
            SettingsMutation.DependsOnTagLib -> dependency(projectDir, ":feature:settings", ":taglib")
            SettingsMutation.DependsOnLibraryApi -> dependency(projectDir, ":feature:settings", ":feature:library:api")
            SettingsMutation.DependsOnKoin -> dependencyNotation(projectDir, ":feature:settings", "org.koin:koin-core:4.2.2")
            SettingsMutation.DependsOnDataStore -> dependencyNotation(projectDir, ":feature:settings", "androidx.datastore:datastore-core:1.2.1")
            SettingsMutation.IosExport -> append(projectDir, ":shared", "kotlin { iosArm64().binaries.framework { export(project(\":feature:settings\")) } }")
            SettingsMutation.SharedCommonMainApiExposure -> {
                removeText(projectDir, ":shared", "dependencies.add(\"commonMainImplementation\", project(\":feature:settings\"))")
                append(projectDir, ":shared", "dependencies.add(\"commonMainApi\", project(\":feature:settings\"))")
            }
            SettingsMutation.EmptyPackageRoots -> append(projectDir, ":feature:settings", "afterEvaluate { extensions.configure<KspExtension> { arg(\"architecture.packageRoots\", \"\") } }")
            else -> Unit
        }
        projectDir.resolve("build.gradle.kts").writeText("plugins { id(\"build-logic.architecture-check\") }")
        return projectDir
    }

    private fun settingsResourceFixture(mutation: SettingsMutation? = null): SettingsResourceFixture {
        val root = Files.createTempDirectory("settings-resource-audit").toFile()
        val shared = listOf(root.resolve("shared-en.xml"), root.resolve("shared-zh.xml"))
        val feature = listOf(root.resolve("settings-en.xml"), root.resolve("settings-zh.xml"))
        val sharedSource = root.resolve("Shared.kt")
        val featureSource = root.resolve("Settings.kt")
        val sharedKeys = listOf("settings", "add_music_folder", "folder_picker_unavailable", "clear_library", "clear_library_message", "clear", "cancel", "remove", "close", "scanning", "scan_progress_format", "scan_complete_format", "folder_picker_error_access", "folder_picker_error_select", "folder_picker_error_prepare", "folder_picker_no_folder_selected")
        val settingsKeys = listOf("appearance", "theme_system_label", "theme_light_label", "theme_dark_label", "theme_system_description", "theme_light_description", "theme_dark_description", "manage_music", "configured_folders", "unnamed_folder", "source_access_available", "source_access_lost", "source_never_scanned", "source_last_scanned", "source_status_format", "rescan_source_format", "remove_source_format", "remove_folder", "remove_folder_message", "about", "about_app_name", "about_logo_description", "about_version_format", "about_view_source", "about_open_source_libraries", "open_source_libraries_loading", "open_source_libraries_error", "open_source_libraries_retry")
        fun xml(names: List<String>) = "<resources>${names.joinToString("") { "<string name=\"$it\">x</string>" }}</resources>"
        shared.forEach { it.writeText(xml(sharedKeys)) }
        feature.forEach { it.writeText(xml(settingsKeys)) }
        val sharedLogo = root.resolve("shared-logo.xml")
        val featureLogo = root.resolve("settings-logo.xml").apply { writeText("<vector />") }
        sharedSource.writeText("package fixture")
        featureSource.writeText("package fixture")
        when (mutation) {
            SettingsMutation.MissingMovedResource -> feature[0].writeText(xml(settingsKeys - "about"))
            SettingsMutation.DuplicateMovedResource -> feature[0].writeText(xml(settingsKeys + "appearance"))
            SettingsMutation.DuplicateMovedResourceZh -> feature[1].writeText(xml(settingsKeys + "appearance"))
            SettingsMutation.ExtraMovedResource -> feature[0].writeText(xml(settingsKeys + "extra"))
            SettingsMutation.CrossOwnerDuplicateResource -> shared[0].writeText(xml(sharedKeys + "appearance"))
            SettingsMutation.WrongResourceOwner -> { feature[0].writeText(xml(settingsKeys + "clear")); shared[0].writeText(xml(sharedKeys - "clear")) }
            SettingsMutation.LocaleParityDivergence -> feature[1].writeText(xml(settingsKeys.reversed()))
            SettingsMutation.InvalidResourceNamespace -> featureSource.writeText("import example.wrong.resources.Res")
            SettingsMutation.ForeignFeatureResourceImport -> featureSource.writeText("import rhythhaus.shared.generated.resources.Res")
            SettingsMutation.ForeignSharedResourceImport -> sharedSource.writeText("import rhythhaus.feature.settings.generated.resources.Res")
            SettingsMutation.MissingLogo -> featureLogo.delete()
            null -> Unit
            else -> error("Unsupported resource mutation: $mutation")
        }
        return SettingsResourceFixture(shared, feature, sharedLogo, featureLogo, sharedSource, featureSource)
    }

    private fun assertSettingsResourceAudit(fixture: SettingsResourceFixture) {
        val sharedExpected = listOf("settings", "add_music_folder", "folder_picker_unavailable", "clear_library", "clear_library_message", "clear", "cancel", "remove", "close", "scanning", "scan_progress_format", "scan_complete_format", "folder_picker_error_access", "folder_picker_error_select", "folder_picker_error_prepare", "folder_picker_no_folder_selected")
        val expected = listOf("appearance", "theme_system_label", "theme_light_label", "theme_dark_label", "theme_system_description", "theme_light_description", "theme_dark_description", "manage_music", "configured_folders", "unnamed_folder", "source_access_available", "source_access_lost", "source_never_scanned", "source_last_scanned", "source_status_format", "rescan_source_format", "remove_source_format", "remove_folder", "remove_folder_message", "about", "about_app_name", "about_logo_description", "about_version_format", "about_view_source", "about_open_source_libraries", "open_source_libraries_loading", "open_source_libraries_error", "open_source_libraries_retry")
        val shared = fixture.shared.map(::searchStringNames)
        val settings = fixture.feature.map(::searchStringNames)
        shared.zip(listOf("EN", "ZH")).forEach { (names, locale) ->
            assertEquals(names.size, names.toSet().size, "SETTINGS-RESOURCE duplicate key in Shared $locale")
        }
        assertTrue(shared.flatten().intersect(expected.toSet()).isEmpty(), "SETTINGS-RESOURCE duplicate key across Shared/Settings")
        shared.zip(listOf("EN", "ZH")).forEach { (names, _) ->
            assertEquals(sharedExpected.multiset(), names.multiset(), "SETTINGS-RESOURCE wrong owner for moved key")
        }
        settings.zip(listOf("EN", "ZH")).forEach { (names, locale) ->
            assertEquals(names.size, names.toSet().size, "SETTINGS-RESOURCE duplicate key in Settings $locale")
            assertTrue(names.all { it in expected }, "SETTINGS-RESOURCE unexpected Settings key")
            assertEquals(expected.multiset(), names.multiset(), "SETTINGS-RESOURCE missing Settings key")
        }
        assertEquals(settings[0], settings[1], "SETTINGS-RESOURCE locale parity differs")
        assertTrue(fixture.featureLogo.isFile && !fixture.sharedLogo.isFile, "SETTINGS-RESOURCE Settings logo owner mismatch")
        assertFalse(fixture.featureSource.readText().contains("example.wrong.resources"), "SETTINGS-RESOURCE wrong namespace")
        assertFalse(fixture.featureSource.readText().contains("rhythhaus.shared.generated.resources"), "SETTINGS-RESOURCE Settings imports Shared generated resources")
        assertFalse(fixture.sharedSource.readText().contains("rhythhaus.feature.settings.generated.resources"), "SETTINGS-RESOURCE Shared imports Settings generated resources")
    }

    private fun List<String>.multiset(): Map<String, Int> = groupingBy { it }.eachCount()

    private fun searchResourceFixture(mutation: SearchMutation): SearchResourceFixture {
        val root = Files.createTempDirectory("search-resource-audit").toFile()
        val shared = listOf(root.resolve("shared-en.xml"), root.resolve("shared-zh.xml"))
        val feature = listOf(root.resolve("feature-en.xml"), root.resolve("feature-zh.xml"))
        val sharedSource = root.resolve("Shared.kt")
        val featureSource = root.resolve("Search.kt")
        val keys = listOf("search_placeholder", "search_results_count_zero", "search_results_count_one", "search_results_count_many", "search_no_tracks_match_format")
        fun xml(names: List<String>) = "<resources>${names.joinToString("") { "<string name=\"$it\">x</string>" }}</resources>"
        shared.forEach { it.writeText(xml(listOf("search", "clear", "now_playing_badge", "select_track_format"))) }
        feature.forEach { it.writeText(xml(keys)) }
        sharedSource.writeText("package fixture")
        featureSource.writeText("package fixture")
        when (mutation) {
            SearchMutation.MissingMovedResource -> feature[0].writeText(xml(keys - "search_results_count_one"))
            SearchMutation.DuplicateMovedResource -> feature[0].writeText(xml(keys + "search_placeholder"))
            SearchMutation.DuplicateMovedResourceZh -> feature[1].writeText(xml(keys + "search_placeholder"))
            SearchMutation.ExtraMovedResource -> feature[0].writeText(xml(keys + "extra"))
            SearchMutation.CrossOwnerDuplicateResource -> shared[0].writeText(xml(listOf("search", "clear", "now_playing_badge", "select_track_format", "search_placeholder")))
            SearchMutation.WrongResourceOwner -> {
                feature[0].writeText(xml(keys + "clear"))
                shared[0].writeText(xml(listOf("search", "now_playing_badge", "select_track_format")))
            }
            SearchMutation.InvalidResourceNamespace -> featureSource.writeText("import example.wrong.resources.Res")
            SearchMutation.ForeignFeatureResourceImport -> featureSource.writeText("import rhythhaus.shared.generated.resources.Res")
            SearchMutation.ForeignSharedResourceImport -> sharedSource.writeText("import rhythhaus.feature.search.generated.resources.Res")
            else -> error("Unsupported resource mutation: $mutation")
        }
        return SearchResourceFixture(shared, feature, sharedSource, featureSource)
    }

    private fun assertSearchResourceAudit(fixture: SearchResourceFixture) {
        val expected = listOf("search_placeholder", "search_results_count_zero", "search_results_count_one", "search_results_count_many", "search_no_tracks_match_format")
        val sharedRequired = listOf("search", "clear", "now_playing_badge", "select_track_format")
        val shared = fixture.shared.map(::searchStringNames)
        val feature = fixture.feature.map(::searchStringNames)
        shared.zip(listOf("EN", "ZH")).forEach { (names, locale) ->
            assertEquals(names.size, names.toSet().size, "SEARCH-RESOURCE duplicate key in Shared $locale")
            assertTrue(names.containsAll(sharedRequired), "SEARCH-RESOURCE wrong owner for moved key")
        }
        assertTrue(feature.flatten().intersect(sharedRequired.toSet()).isEmpty(), "SEARCH-RESOURCE wrong owner for moved key")
        assertTrue(shared.flatten().intersect(expected.toSet()).isEmpty(), "SEARCH-RESOURCE duplicate key across Shared/Search")
        feature.zip(listOf("EN", "ZH")).forEach { (names, locale) ->
            assertEquals(names.size, names.toSet().size, "SEARCH-RESOURCE duplicate key in Search $locale")
            assertTrue(names.all { it in expected }, "SEARCH-RESOURCE unexpected Search key")
            assertEquals(expected, names, "SEARCH-RESOURCE missing moved key")
        }
        assertEquals(feature[0], feature[1], "SEARCH-RESOURCE locale parity differs")
        assertFalse(fixture.featureSource.readText().contains("example.wrong.resources"), "SEARCH-RESOURCE wrong namespace")
        assertFalse(fixture.featureSource.readText().contains("rhythhaus.shared.generated.resources"), "SEARCH-RESOURCE feature imports Shared generated resources")
        assertFalse(fixture.sharedSource.readText().contains("rhythhaus.feature.search.generated.resources"), "SEARCH-RESOURCE Shared imports Search generated resources")
    }

    private fun searchRepositoryProductionRoots(): List<File> {
        val repositoryRoot = File(System.getProperty("rhythhaus.rootDir")).canonicalFile
        val searchSource = repositoryRoot.resolve("feature/search/src")
        return searchSource.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.endsWith("Main") }
            .map { it.resolve("kotlin") }
            .filter(File::isDirectory)
            .sortedBy { it.path }
    }

    private fun copySearchProductionRoots(roots: List<File>): List<File> {
        val copyRoot = Files.createTempDirectory("search-production-koin-audit").toFile()
        return roots.map { root ->
            val destination = copyRoot.resolve(root.name)
            check(root.copyRecursively(destination, overwrite = false)) { "Could not copy Search production root: ${root.path}" }
            destination
        }
    }

    private fun assertSearchProductionRootsHaveNoKoin(roots: List<File>) {
        roots.forEach { root -> check(root.isDirectory) { "SEARCH-KOIN missing production root: ${root.path}" } }
        roots.asSequence().flatMap { root -> root.walkTopDown().asSequence() }
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                assertFalse(source.readText().contains("org.koin"), "SEARCH-KOIN feature source imports Koin: ${source.path}")
            }
    }

    private fun qualityEntrypointFixture(mutation: Mutation? = null): File {
        val projectDir = fixture(mutation)
        projectDir.resolve("settings.gradle.kts").appendText(
            """

            dependencyResolutionManagement {
                versionCatalogs {
                    create("libs") {
                        from(files("${File(System.getProperty("rhythhaus.rootDir"), "gradle/libs.versions.toml").invariantSeparatorsPath}"))
                    }
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.root-project") }

            gradle.allprojects {
                tasks.configureEach {
                    if (name !in setOf("architectureCheck", "check", "qualityCheck")) {
                        enabled = false
                    }
                }
            }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun playlistsFeatureFixture(
        source: String = "package com.eterocell.rhythhaus.library\n/** A documented feature declaration. */\npublic class PlaylistFeature",
        forbiddenDependency: String? = null,
        emptyPackageRoots: Boolean = false,
    ): File {
        val projectDir = fixture()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-playlists-feature"
            include(":androidApp", ":desktopApp", ":shared", ":taglib", ":architecture-processor", ":core:model", ":core:database", ":core:platform", ":core:playback", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl")
            """.trimIndent(),
        )
        val processorJar = processorProject().resolve("build/libs/architecture-processor.jar")
        require(processorJar.isFile) { "Architecture processor JAR is required at $processorJar" }
        buildFile(projectDir, ":architecture-processor").apply {
            parentFile.mkdirs()
            writeText("configurations.create(\"default\")\nartifacts { add(\"default\", file(\"${processorJar.invariantSeparatorsPath}\")) }")
        }
        listOf(":core:platform", ":core:playback", ":core:ui").forEach { module ->
            module(projectDir, module)
            kmpModule(projectDir, module, strict = true)
        }
        buildFile(projectDir, ":feature:playlists:impl").apply {
            parentFile.mkdirs()
            writeText(
                """
                import com.eterocell.gradle.architecture.ArchitectureModelRegistry
                import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
                import com.google.devtools.ksp.gradle.KspExtension

                plugins {
                    id("build-logic.kmp.feature.impl")
                    id("build-logic.android.kmp.library")
                    id("build-logic.compose-resources")
                    id("org.jetbrains.kotlin.plugin.compose")
                }

                configurations.maybeCreate("architecture")
                extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
                    namespace("rhythhaus.feature.playlists.generated.resources")
                }

                kotlin {
                    android {
                        namespace = "com.eterocell.rhythhaus.playlists"
                        compileSdk = 37
                        minSdk = 29
                        withHostTest {}
                        androidResources { enable = true }
                    }
                    jvm()
                    iosArm64()
                    iosSimulatorArm64()
                }

                dependencies {
                    add("commonMainImplementation", "org.jetbrains.compose.runtime:runtime:1.11.1")
                    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach {
                        add(it, files("${processorJar.invariantSeparatorsPath}"))
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val kspOptions = extensions.getByName("ksp")
                    .javaClass.getMethod("getArguments")
                    .invoke(extensions.getByName("ksp")) as Map<String, String>
                val kspRegistrations = ArchitectureModelRegistry.forRoot(project).snapshot().kspRegistrations
                    .map { it.module + "|" + it.configuration + "|" + it.processor }
                tasks.register("verifyPlaylistsFeatureConvention") {
                    inputs.property("packageRoots", kspOptions.getValue("architecture.packageRoots"))
                    inputs.property("registrations", kspRegistrations)
                    doLast {
                        println("KSP_PACKAGE_ROOTS=" + inputs.properties.getValue("packageRoots"))
                        (inputs.properties.getValue("registrations") as List<*>).forEach { println("KSP_REGISTRATION=" + it) }
                    }
                }

                ${if (emptyPackageRoots) "extensions.configure<KspExtension> { arg(\"architecture.packageRoots\", \"\") }" else ""}
                """.trimIndent(),
            )
        }
        source(projectDir, ":feature:playlists:impl", "PlaylistFeature.kt", source)
        listOf(
            ":feature:playlists:api", ":feature:library:api", ":core:model", ":core:playback",
            ":core:ui", ":core:platform", ":core:database",
        ).forEach { dependency(projectDir, ":feature:playlists:impl", it) }
        forbiddenDependency?.let { dependency(projectDir, ":feature:playlists:impl", it) }
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.architecture-check") }

            tasks.named("architectureCheck") {
                doLast { logger.lifecycle("TEST_DEPENDENCY_EDGES=" + inputs.properties.getValue("dependencyEdges")) }
            }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun qualityAggregationFixture(): QualityAggregationFixture {
        val projectDir = architectureFixture("quality-aggregation")
        val markerDirectory = Files.createTempDirectory("quality-markers").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement {
                repositories { mavenCentral() }
                versionCatalogs {
                    create("libs") {
                        from(files("${File(System.getProperty("rhythhaus.rootDir"), "gradle/libs.versions.toml").invariantSeparatorsPath}"))
                    }
                }
            }
            rootProject.name = "quality-aggregation"
            include(":core:database", ":shared")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.root-project") }

            allprojects {
                tasks.configureEach {
                    if (name == "spotlessCheck") {
                        setDependsOn(emptyList<Any>())
                    }
                    if (name !in setOf("architectureCheck", "qualityCheck", "detekt", "spotlessCheck", "sharedDetektSentinel", "sharedSpotlessCheckSentinel")) {
                        enabled = false
                    }
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("shared/child-detekt.marker").apply {
            parentFile.mkdirs()
            writeText("detekt sentinel input")
        }
        projectDir.resolve("shared/child-spotless-check.marker").writeText("spotless sentinel input")
        module(projectDir, ":core:database")
        projectDir.resolve("shared/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                import org.gradle.api.tasks.Copy

                plugins {
                    id("org.jetbrains.kotlin.multiplatform")
                }

                configurations.maybeCreate("architecture")
                kotlin { jvm() }

                val sharedDetektSentinel = tasks.register<Copy>("sharedDetektSentinel") {
                    from(layout.projectDirectory.file("child-detekt.marker"))
                    into(file("${markerDirectory.invariantSeparatorsPath}/detekt"))
                }
                val sharedSpotlessCheckSentinel = tasks.register<Copy>("sharedSpotlessCheckSentinel") {
                    from(layout.projectDirectory.file("child-spotless-check.marker"))
                    into(file("${markerDirectory.invariantSeparatorsPath}/spotless"))
                }

                tasks.named("detekt") { dependsOn(sharedDetektSentinel) }
                tasks.named("spotlessCheck") { dependsOn(sharedSpotlessCheckSentinel) }
                """.trimIndent(),
            )
        }
        sqlDelightPluginModule(projectDir, ":core:database")
        driver(projectDir, ":core:database")
        sqlArtifact(projectDir, ":core:database", "fixture.sq")
        append(projectDir, ":core:database", "kotlin { explicitApi() }")
        return QualityAggregationFixture(projectDir, markerDirectory)
    }

    private fun kspFixture(source: String): File = kspFixture(mapOf("Fixture.kt" to source))

    private fun kspFixture(
        source: String,
        testSources: Map<String, String> = emptyMap(),
        generateLaterRound: Boolean = false,
    ): File = kspFixture(mapOf("Fixture.kt" to "package com.eterocell.rhythhaus\n$source"), testSources, generateLaterRound)

    private fun kspFixture(
        mainSources: Map<String, String>,
        testSources: Map<String, String> = emptyMap(),
        generateLaterRound: Boolean = false,
    ): File {
        val projectDir = architectureFixture("architecture-ksp")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-ksp-consumer"
            include(":architecture-processor", ":core:model"${if (generateLaterRound) ", \":later-round-generator\"" else ""})
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("")
        val processorDir = moduleDir(projectDir, ":architecture-processor")
        processorProject().resolve("src").copyRecursively(processorDir.resolve("src"), overwrite = true)
        processorDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("org.jetbrains.kotlin.jvm") version "2.4.10" }
            repositories { mavenCentral() }
            dependencies { implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10") }
            """.trimIndent(),
        )
        moduleDir(projectDir, ":core:model").resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText("plugins { id(\"build-logic.kmp.core\") }\nkotlin { jvm() }")
        }
        mainSources.forEach { (path, contents) ->
            writeKotlinSource(projectDir, ":core:model", "src/jvmMain/kotlin/$path", contents)
        }
        testSources.forEach { (path, contents) ->
            writeKotlinSource(projectDir, ":core:model", "src/jvmTest/kotlin/$path", contents)
        }
        if (generateLaterRound) {
            laterRoundGenerator(projectDir)
            append(
                projectDir,
                ":core:model",
                "dependencies { add(\"kspJvm\", project(\":later-round-generator\")) }",
            )
        }
        return projectDir
    }

    private fun kmpTaskGeneratedResourceFixture(): File {
        val projectDir = fixture()
        projectDir.resolve("build.gradle.kts").appendText(
            """

            tasks.named("architectureCheck") {
                doLast {
                    logger.lifecycle("TEST_RESOURCE_RECORDS=" + inputs.properties["resourceRecords"])
                    logger.lifecycle(
                        "TEST_RESOURCE_INPUTS=" + inputs.files.files.joinToString(",") { it.invariantSeparatorsPath },
                    )
                }
            }
            """.trimIndent(),
        )
        append(
            projectDir,
            ":core:model",
            """
            val generatedResources = layout.buildDirectory.dir("generated/fixtureResources/commonMain")
            val copyGeneratedResources = tasks.register<org.gradle.api.tasks.Copy>("copyGeneratedResources") {
                from(layout.projectDirectory.dir("fixture-resources"))
                into(generatedResources)
            }

            kotlin {
                sourceSets.named("commonMain") {
                    resources.srcDir(generatedResources)
                    resources.srcDir("src/commonMain/customResources")
                }
            }
            """.trimIndent(),
        )
        resource(projectDir, ":core:model", "fixture-resources/generated.txt")
        resource(projectDir, ":core:model", "src/commonMain/customResources/authored.txt")
        return projectDir
    }

    private fun binaryProcessorFixture(processorJar: File): File {
        val projectDir = architectureFixture("architecture-ksp-binary")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-ksp-binary-consumer"
            include(":core:model")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("")
        moduleDir(projectDir, ":core:model").resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                import com.google.devtools.ksp.gradle.KspExtension

                plugins {
                    id("org.jetbrains.kotlin.multiplatform") version "2.4.10"
                    id("com.google.devtools.ksp") version "2.3.10"
                }

                kotlin { jvm() }

                extensions.configure<KspExtension> {
                    arg("architecture.module", project.path)
                    arg("architecture.packageRoots", "com.eterocell.rhythhaus")
                    arg("architecture.sourceRoots", file("src/jvmMain/kotlin").absolutePath)
                }

                dependencies {
                    add("kspJvm", files("${processorJar.invariantSeparatorsPath}"))
                }

                val verifiedKspJvmClasspath = configurations.create("verifiedKspJvmClasspath") {
                    isCanBeResolved = true
                    extendsFrom(configurations.getByName("kspJvm"))
                }

                tasks.register("verifyKspJvmResolution") {
                    doLast {
                        val resolved = verifiedKspJvmClasspath.resolve().single().canonicalFile
                        check(resolved == file("${processorJar.invariantSeparatorsPath}").canonicalFile) {
                            "Expected only supplied processor JAR, resolved: ${'$'}resolved"
                        }
                        logger.lifecycle("TEST-KSP-JAR=${processorJar.invariantSeparatorsPath}")
                    }
                }

                tasks.configureEach {
                    if (name == "kspKotlinJvm") {
                        dependsOn("verifyKspJvmResolution")
                    }
                }
                """.trimIndent(),
            )
        }
        writeKotlinSource(
            projectDir,
            ":core:model",
            "src/jvmMain/kotlin/InvalidProduction.kt",
            "package outside.fixture\n/** Invalid package. */\npublic class InvalidProduction",
        )
        return projectDir
    }

    private fun laterRoundGenerator(projectDir: File) {
        val generatorDir = moduleDir(projectDir, ":later-round-generator")
        generatorDir.resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins { id("org.jetbrains.kotlin.jvm") version "2.4.10" }
                repositories { mavenCentral() }
                dependencies { implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10") }
                """.trimIndent(),
            )
        }
        generatorDir.resolve("src/main/kotlin/fixture/LaterRoundGeneratorProvider.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                package fixture

                import com.google.devtools.ksp.processing.CodeGenerator
                import com.google.devtools.ksp.processing.Dependencies
                import com.google.devtools.ksp.processing.Resolver
                import com.google.devtools.ksp.processing.SymbolProcessor
                import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
                import com.google.devtools.ksp.processing.SymbolProcessorProvider
                import com.google.devtools.ksp.symbol.KSAnnotated

                class LaterRoundGeneratorProvider : SymbolProcessorProvider {
                    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                        LaterRoundGenerator(environment.codeGenerator)
                }

                private class LaterRoundGenerator(
                    private val codeGenerator: CodeGenerator,
                ) : SymbolProcessor {
                    private var generated = false

                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (!generated) {
                            generated = true
                            codeGenerator
                                .createNewFile(Dependencies(false), "outside.generated", "GeneratedViolation")
                                .bufferedWriter()
                                .use { it.write("package outside.generated\npublic class GeneratedViolation") }
                        }
                        return emptyList()
                    }
                }
                """.trimIndent(),
            )
        }
        generatorDir
            .resolve("src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider")
            .apply {
                parentFile.mkdirs()
                writeText("fixture.LaterRoundGeneratorProvider")
            }
    }

    private fun generatedViolation(projectDir: File): File =
        moduleDir(projectDir, ":core:model")
            .resolve("build/generated/ksp/jvm/jvmMain/kotlin/outside/generated/GeneratedViolation.kt")

    private fun preexistingGeneratedViolation(projectDir: File): File =
        moduleDir(projectDir, ":core:model")
            .resolve("build/generated/fixture/jvmMain/kotlin/outside/generated/PreexistingGeneratedViolation.kt")

    private fun targetRegistrationFixture(applyConvention: Boolean): File {
        val projectDir = architectureFixture("architecture-ksp-target-registration")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-ksp-target-registration"
            include(":architecture-processor", ":core:model", ":core:database", ":shared")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.architecture-check") }

            tasks.named("architectureCheck") {
                doLast {
                    logger.lifecycle("TEST_DEPENDENCY_EDGES=" + inputs.properties.getValue("dependencyEdges"))
                }
            }
            """.trimIndent(),
        )
        val processorDir = moduleDir(projectDir, ":architecture-processor")
        processorDir.mkdirs()
        processorProject().resolve("src").copyRecursively(processorDir.resolve("src"), overwrite = true)
        processorDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("org.jetbrains.kotlin.jvm") }
            repositories { mavenCentral() }
            dependencies { implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10") }
            """.trimIndent(),
        )
        module(projectDir, ":core:database")
        module(projectDir, ":shared")
        kmpModule(projectDir, ":shared", strict = false)
        sqlDelightPluginModule(projectDir, ":core:database")
        driver(projectDir, ":core:database")
        sqlArtifact(projectDir, ":core:database", "fixture.sq")
        append(projectDir, ":core:database", "kotlin { explicitApi() }")
        append(
            projectDir,
            ":shared",
            """
            dependencies { add("architecture", project(":core:model")) }
            """.trimIndent(),
        )
        moduleDir(projectDir, ":core:model").resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins {
                    id("${if (applyConvention) "build-logic.kmp.core" else "org.jetbrains.kotlin.multiplatform"}")
                    ${if (applyConvention) "" else "id(\"com.google.devtools.ksp\")"}
                    id("com.android.kotlin.multiplatform.library")
                }

                kotlin {
                    ${if (applyConvention) "" else "explicitApi()"}
                    jvm()
                    android {
                        namespace = "com.example.targetregistration"
                        compileSdk = 37
                        minSdk = 29
                    }
                    iosArm64()
                }

                dependencies {
                    ${if (applyConvention) "" else "add(\"kspJvm\", project(\":architecture-processor\")); add(\"kspAndroid\", project(\":architecture-processor\")); add(\"kspIosArm64\", project(\":architecture-processor\"))"}
                }
                """.trimIndent(),
            )
        }
        return projectDir
    }

    private fun composeFixture(namespace: String, customRoot: String? = null): File {
        val projectDir = fixture()
        append(projectDir, ":shared", "apply(plugin = \"org.jetbrains.kotlin.plugin.compose\")")
        append(projectDir, ":shared", "apply(plugin = \"build-logic.compose-resources\")")
        append(
            projectDir,
            ":shared",
            "dependencies.add(\"commonMainImplementation\", \"org.jetbrains.compose.components:components-resources:1.11.1\")",
        )
        projectDir.resolve("build.gradle.kts").appendText(
            """

            tasks.named("architectureCheck") {
                doLast {
                    logger.lifecycle("TEST_RESOURCE_RECORDS=" + inputs.properties["resourceRecords"])
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("gradle.properties").appendText(
            buildString {
                appendLine("architecture.compose.namespace=$namespace")
                customRoot?.let { appendLine("architecture.compose.commonMain.roots=$it") }
            },
        )
        return projectDir
    }

    private fun androidApplicationFixture(applyControlledPublisher: Boolean = true): File {
        val projectDir = architectureFixture("architecture-android")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "architecture-android-consumer"
            include(":androidApp", ":shared", ":core:database")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.architecture-check") }

            tasks.named("architectureCheck") {
                doFirst {
                    logger.lifecycle("TEST_RESOURCE_RECORDS=" + inputs.properties["resourceRecords"])
                    logger.lifecycle("TEST_ANDROID_TEST_CONFIGURATIONS=" + inputs.properties["androidTestConfigurations"])
                }
            }
            """.trimIndent(),
        )
        val androidProject = moduleDir(projectDir, ":androidApp")
        androidProject.resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins { id("${if (applyControlledPublisher) "build-logic.android.application" else "com.android.application"}") }

                android {
                    namespace = "com.example.androidfixture"
                    compileSdk = 37

                    defaultConfig {
                        applicationId = "com.example.androidfixture"
                        minSdk = 29
                    }
                }
                """.trimIndent(),
            )
        }
        listOf("src/main/res", "src/androidTest/res", "src/testFixtures/res", "src/test/res").forEach { path ->
            androidProject.resolve(path).mkdirs()
        }
        module(projectDir, ":shared")
        kmpModule(projectDir, ":shared", strict = false)
        module(projectDir, ":core:database")
        sqlDelightPluginModule(projectDir, ":core:database")
        driver(projectDir, ":core:database")
        sqlArtifact(projectDir, ":core:database", "fixture.sq")
        append(projectDir, ":core:database", "kotlin { explicitApi() }")
        return projectDir
    }

    private fun androidKmpResourceFixture(
        resourcesEnabled: Boolean = true,
        addTaskGeneratedResource: Boolean = false,
    ): File {
        val projectDir = fixture()
        projectDir.resolve("build.gradle.kts").appendText(
            """

            tasks.named("architectureCheck") {
                doLast {
                    logger.lifecycle("TEST_RESOURCE_RECORDS=" + inputs.properties["resourceRecords"])
                }
            }
            """.trimIndent(),
        )
        moduleDir(projectDir, ":core:model").resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("build-logic.android.kmp.library")
            }

            kotlin {
                explicitApi()
                jvm()
                android {
                    namespace = "com.example.androidkmpfixture"
                    compileSdk = 37
                    minSdk = 29
                    androidResources.enable = $resourcesEnabled
                }
            }

            ${
                if (addTaskGeneratedResource)
                    """
                    abstract class GenerateAndroidResources : org.gradle.api.DefaultTask() {
                        @get:org.gradle.api.tasks.OutputDirectory
                        abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

                        @org.gradle.api.tasks.TaskAction
                        fun generate() {
                            outputDirectory.get().file("values/generated.xml").asFile.apply {
                                parentFile.mkdirs()
                                writeText("<resources><string name=\"generated\">generated</string></resources>")
                            }
                        }
                    }

                    val generateAndroidResources = tasks.register<GenerateAndroidResources>("generateAndroidResources") {
                        outputDirectory.set(layout.buildDirectory.dir("generated/taskAndroidResources"))
                    }
                    extensions
                        .getByType<com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension>()
                        .onVariants { variant ->
                            variant.sources.res?.addGeneratedSourceDirectory(
                                generateAndroidResources,
                                GenerateAndroidResources::outputDirectory,
                            )
                        }
                    """.trimIndent()
                else ""
            }
            """.trimIndent(),
        )
        val modelProject = moduleDir(projectDir, ":core:model")
        modelProject.resolve("src/androidMain/res/values/strings.xml").apply {
            parentFile.mkdirs()
            writeText("<resources><string name=\"production\">production</string></resources>")
        }
        modelProject.resolve("src/androidTest/res/values/strings.xml").apply {
            parentFile.mkdirs()
            writeText("<resources><string name=\"test\">test</string></resources>")
        }
        modelProject.resolve("src/commonTest/resources").mkdirs()
        return projectDir
    }

    private fun controlledAndroidKmpTestComponentsFixture(
        authoredArchitectureSelfDependency: Boolean = false,
        addDistinctHostTestSelfDependency: Boolean = false,
    ): File {
        val projectDir = fixture()
        projectDir.resolve("build.gradle.kts").appendText(
            """

            tasks.named("architectureCheck") {
                doLast {
                    logger.lifecycle("TEST_ANDROID_TEST_CONFIGURATIONS=" + inputs.properties["androidTestConfigurations"])
                }
            }
            """.trimIndent(),
        )
        moduleDir(projectDir, ":core:model").resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("build-logic.android.kmp.library")
            }

            configurations.maybeCreate("architecture")

            kotlin {
                explicitApi()
                jvm()
                android {
                    namespace = "com.example.controlledandroidkmptest"
                    compileSdk = 37
                    minSdk = 29
                    withHostTest {}
                    withDeviceTest {}
                }
            }

            ${
                if (authoredArchitectureSelfDependency)
                    "dependencies.add(\"architecture\", project(\":core:model\"))"
                else ""
            }

            ${
                if (addDistinctHostTestSelfDependency)
                    """
                    afterEvaluate {
                        dependencies.add(
                            "androidHostTestCompileClasspath",
                            dependencies.project(
                                mapOf(
                                    "path" to ":core:model",
                                    "configuration" to "architecture",
                                ),
                            ),
                        )
                    }
                    """.trimIndent()
                else ""
            }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun publishedAndroidTestConfigurationFixture(selfDependencyCount: Int): File {
        val projectDir = fixture()
        append(
            projectDir,
            ":core:model",
            """
            val controlledAndroidTestComponent = configurations.create("controlledAndroidTestComponent")
            com.eterocell.gradle.architecture.ArchitectureModelRegistry
                .forRoot(project)
                .publishAndroidTestConfiguration(project, controlledAndroidTestComponent)
            dependencies.add("controlledAndroidTestComponent", project(":core:model"))
            if ($selfDependencyCount > 1) {
                dependencies.add(
                    "controlledAndroidTestComponent",
                    dependencies.project(mapOf("path" to ":core:model", "configuration" to "architecture")),
                )
            }
            """.trimIndent(),
        )
        return projectDir
    }


    private fun processorProject(): File =
        File(System.getProperty("rhythhaus.rootDir"), "architecture-processor")

    private fun externallyProvidedProcessorJarOrSkip(): File {
        val providedPath = System.getProperty("rhythhaus.architectureProcessorJar")
        assumeTrue(
            "Dedicated repository processor binary integration requires -Prhythhaus.architectureProcessorJar=<path>",
            !providedPath.isNullOrBlank(),
        )
        val providedJar = File(checkNotNull(providedPath)).canonicalFile
        val expectedJar = File(processorProject(), "build/libs/architecture-processor.jar").canonicalFile
        assertEquals(expectedJar, providedJar, "Processor JAR must be the repository-built artifact")
        assertTrue(providedJar.isFile && providedJar.length() > 0, "Missing or empty processor JAR: $providedJar")
        JarFile(providedJar).use { jar ->
            assertTrue(
                jar.getJarEntry("com/eterocell/rhythhaus/architecture/ArchitectureProcessorProvider.class") != null,
                "Provider class missing from $providedJar",
            )
            val providers =
                jar.getInputStream(
                    checkNotNull(
                        jar.getJarEntry("META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"),
                    ) { "KSP SPI descriptor missing from $providedJar" },
                )
                    .bufferedReader()
                    .use { reader ->
                        reader.lineSequence()
                            .map(String::trim)
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .toList()
                    }
            assertEquals(
                listOf("com.eterocell.rhythhaus.architecture.ArchitectureProcessorProvider"),
                providers,
                "Unexpected KSP SPI providers in $providedJar",
            )
        }
        return providedJar
    }

    private fun module(projectDir: File, path: String) {
        projectDir.resolve(path.removePrefix(":").replace(':', '/')).apply {
            mkdirs()
            resolve("build.gradle.kts").writeText("configurations.create(\"architecture\")")
        }
    }

    private fun dependency(projectDir: File, from: String, to: String) =
        append(projectDir, from, "dependencies.add(\"architecture\", project(\"$to\"))")

    private fun processorDependency(projectDir: File, configuration: String) {
        append(projectDir, ":core:model", "configurations.maybeCreate(\"$configuration\")")
        append(projectDir, ":core:model", "dependencies.add(\"$configuration\", project(\":architecture-processor\"))")
    }

    private fun removeDependency(projectDir: File, from: String, to: String) {
        val file = buildFile(projectDir, from)
        file.writeText(file.readText().replace("dependencies.add(\"architecture\", project(\"$to\"))", ""))
    }

    private fun dependencyNotation(projectDir: File, module: String, notation: String) =
        append(projectDir, module, "dependencies.add(\"architecture\", \"$notation\")")

    private fun driver(projectDir: File, module: String) =
        append(projectDir, module, "dependencies.add(\"jvmMainImplementation\", \"app.cash.sqldelight:sqlite-driver:2.3.2\")")

    private fun removeDriver(projectDir: File, module: String) =
        removeText(projectDir, module, "dependencies.add(\"jvmMainImplementation\", \"app.cash.sqldelight:sqlite-driver:2.3.2\")")

    private fun source(projectDir: File, module: String, fileName: String, contents: String) {
        val file = moduleDir(projectDir, module).resolve("src/commonMain/kotlin/$fileName")
        file.parentFile.mkdirs()
        file.writeText(contents)
    }

    private fun writeKotlinSource(projectDir: File, module: String, path: String, contents: String) {
        val file = moduleDir(projectDir, module).resolve(path)
        file.parentFile.mkdirs()
        file.writeText(contents)
    }

    private fun resource(projectDir: File, module: String, relativePath: String) {
        val file = moduleDir(projectDir, module).resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText("fixture")
    }

    private fun sqlArtifact(projectDir: File, module: String, fileName: String) =
        resource(projectDir, module, if (fileName.startsWith("src/")) fileName else "src/commonMain/sqldelight/$fileName")

    private fun kmpModule(projectDir: File, module: String, strict: Boolean, strictCompilerArgs: Boolean = false) {
        buildFile(projectDir, module).writeText(
            """
            import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
            plugins { id("org.jetbrains.kotlin.multiplatform") }
            configurations.maybeCreate("architecture")
            kotlin {
                jvm()
                explicitApi = ExplicitApiMode.${if (strict) "Strict" else "Warning"}
            }
            ${if (strictCompilerArgs) "tasks.configureEach { if (name.startsWith(\"compileKotlin\")) { (this as org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>).compilerOptions.freeCompilerArgs.add(\"-Xexplicit-api=strict\") } }; tasks.register(\"compileKotlinAndroidLike\")" else ""}
            """.trimIndent(),
        )
    }

    private fun iosExport(projectDir: File) {
        kmpModule(projectDir, ":shared", strict = false)
        dependency(projectDir, ":shared", ":feature:library:api")
        append(projectDir, ":shared", "kotlin { iosArm64().binaries.framework { export(project(\":core:model\")) } }")
    }

    private fun sqlDelightPluginModule(projectDir: File, module: String) {
        buildFile(projectDir, module).writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("build-logic.sqldelight")
            }
            configurations.maybeCreate("architecture")
            kotlin { jvm() }
            """.trimIndent(),
        )
    }

    private fun assertSqlDelightFixtureMarkers(output: String, vararg expectedMarkers: String) {
        assertTrue(output.contains(SQLDELIGHT_FIXTURE_APPLIED_MARKER), output)
        expectedMarkers.forEach { marker -> assertTrue(output.contains(marker), output) }
        assertTrue(!output.contains(SQLDELIGHT_RUNTIME_API_MISSING_SENTINEL), output)
    }

    private fun isolatedSqlDelightClasspathRunner(
        dependency: SqlDelightFixtureDependency,
        consumerAppliesSqlDelight: Boolean = false,
    ): GradleRunner {
        val projectDir = architectureFixture("architecture-sqldelight-classpath")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                includeBuild("fixture-plugin")
                repositories { gradlePluginPortal(); mavenCentral() }
            }
            dependencyResolutionManagement { repositories { mavenCentral() } }
            rootProject.name = "architecture-sqldelight-classpath-consumer"
            ${if (consumerAppliesSqlDelight) "include(\":shared\")" else ""}
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            if (consumerAppliesSqlDelight) {
                ""
            } else {
                "plugins { id(\"fixture.sqldelight-classpath\") }"
            },
        )
        if (consumerAppliesSqlDelight) {
            moduleDir(projectDir, ":shared").resolve("build.gradle.kts").apply {
                parentFile.mkdirs()
                writeText(
                    """
                    plugins {
                        id("org.jetbrains.kotlin.multiplatform") version "2.4.10"
                        id("app.cash.sqldelight") version "2.3.2"
                        id("fixture.sqldelight-classpath")
                    }

                    kotlin { jvm() }

                    sqldelight {
                        databases {
                            create("FixtureDatabase") {
                                packageName.set("fixture")
                            }
                        }
                    }
                    """.trimIndent(),
                )
            }
        }

        val fixturePluginDir = projectDir.resolve("fixture-plugin")
        fixturePluginDir.mkdirs()
        fixturePluginDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-plugin\"")
        fixturePluginDir.resolve("build.gradle.kts").writeText(
            """
            plugins { `java-gradle-plugin` }

            repositories { gradlePluginPortal(); mavenCentral() }

            dependencies {
                ${dependency.configuration}("app.cash.sqldelight:gradle-plugin:2.3.2")
            }

            gradlePlugin {
                plugins {
                    create("sqlDelightClasspathFixture") {
                        id = "fixture.sqldelight-classpath"
                        implementationClass = "fixture.SqlDelightClasspathFixturePlugin"
                    }
                }
            }
            """.trimIndent(),
        )
        fixturePluginDir.resolve("src/main/java/fixture/SqlDelightClasspathFixturePlugin.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package fixture;

                import app.cash.sqldelight.gradle.SqlDelightExtension;
                import org.gradle.api.GradleException;
                import org.gradle.api.Plugin;
                import org.gradle.api.Project;

                public final class SqlDelightClasspathFixturePlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        project.getLogger().lifecycle("$SQLDELIGHT_FIXTURE_APPLIED_MARKER");
                        try {
                            Object extension = project.getExtensions().findByType(SqlDelightExtension.class);
                            project.getLogger().lifecycle("$SQLDELIGHT_RUNTIME_API_AVAILABLE_MARKER");
                            project.getLogger().lifecycle(
                                extension == null
                                    ? "$SQLDELIGHT_EXTENSION_ABSENT_MARKER"
                                    : "$SQLDELIGHT_EXTENSION_PRESENT_MARKER"
                            );
                        } catch (NoClassDefFoundError exception) {
                            throw new GradleException("$SQLDELIGHT_RUNTIME_API_MISSING_SENTINEL", exception);
                        }
                    }
                }
                """.trimIndent(),
            )
        }

        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("help", "--stacktrace")
    }

    private fun append(projectDir: File, module: String, contents: String) {
        buildFile(projectDir, module).appendText("\n$contents\n")
    }

    private fun removeText(projectDir: File, module: String, contents: String) {
        val file = buildFile(projectDir, module)
        file.writeText(file.readText().replace(contents, ""))
    }

    private fun buildFile(projectDir: File, module: String): File = moduleDir(projectDir, module).resolve("build.gradle.kts")

    private fun architectureFixture(prefix: String): File =
        Files.createTempDirectory(prefix).toFile().also { projectDir ->
            projectDir.resolve("gradle.properties").writeText(
                "org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8\n",
            )
        }

    private fun moduleDir(projectDir: File, module: String): File = projectDir.resolve(module.removePrefix(":").replace(':', '/'))

    private fun runner(projectDir: File, vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                *(if (tasks.isEmpty()) listOf("architectureCheck") else tasks.toList()).plus(
                    listOf("--stacktrace", "--configuration-cache", "--configuration-cache-problems=fail"),
                ).toTypedArray(),
            )

    private fun nowPlayingRunner(projectDir: File, vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                *(if (tasks.isEmpty()) listOf("architectureCheck") else tasks.toList()).plus(
                    listOf(
                        "--rerun-tasks",
                        "--stacktrace",
                        "--configuration-cache",
                        "--configuration-cache-problems=fail",
                    ),
                ).toTypedArray(),
            )

    private fun playlistsRunner(projectDir: File, vararg tasks: String): GradleRunner =
        nowPlayingRunner(projectDir, *tasks)

    private fun searchRunner(projectDir: File, vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(
                *(if (tasks.isEmpty()) listOf("architectureCheck") else tasks.toList()).plus(
                    listOf("--rerun-tasks", "--stacktrace", "--no-configuration-cache"),
                ).toTypedArray(),
            )

    private fun settingsRunner(projectDir: File, vararg tasks: String): GradleRunner =
        searchRunner(projectDir, *tasks)

    private fun qualityAggregationRunner(projectDir: File): GradleRunner =
        GradleRunner.create().withProjectDir(projectDir).withPluginClasspath().withArguments("qualityCheck", "--stacktrace", "--configuration-cache", "--configuration-cache-problems=fail")

    private fun String.resourceRecords(): String =
        lineSequence().single { it.startsWith("TEST_RESOURCE_RECORDS=") }.removePrefix("TEST_RESOURCE_RECORDS=")

    private fun String.resourceInputs(): String =
        lineSequence().single { it.startsWith("TEST_RESOURCE_INPUTS=") }.removePrefix("TEST_RESOURCE_INPUTS=")

    private fun String.composeResourceRecords(): List<String> =
        resourceRecords().removeSurrounding("[", "]").split(", ").filter { "|COMPOSE|" in it }

    private fun String.androidTestConfigurations(): List<String> =
        lineSequence().single { it.startsWith("TEST_ANDROID_TEST_CONFIGURATIONS=") }
            .removePrefix("TEST_ANDROID_TEST_CONFIGURATIONS=")
            .removeSurrounding("[", "]")
            .split(", ")
            .filter(String::isNotBlank)

    private fun String.dependencyEdges(): String =
        lineSequence().single { it.startsWith("TEST_DEPENDENCY_EDGES=") }.removePrefix("TEST_DEPENDENCY_EDGES=")

    private enum class Mutation {
        DependencyCycle, SelfDependency, ProductionKspProcessor, ImplementationProcessor, ForbiddenEdge, PlaylistsApiDependsOnCoreModel, LibraryApiDependsOnCoreDatabase, LibraryApiDependsOnShared, LibraryApiDependsOnImplementation, LibraryImplementationDependsOnShared, SqlDelightRuntimeAndReadme, MissingSqlDelightOwner, TwoSqlDelightOwners, ArbitrarySqlDelightOwner, SpoofedSqlDelightDriver, ExplicitSupportedSqlDelightRoot, ExplicitApiWarningWithStrictCompilerArgs, UnapprovedIosExport,
    }

    private enum class NowPlayingMutation {
        DependsOnShared,
        DependsOnTagLib,
        DependsOnLibraryApi,
        DependsOnLibraryImplementation,
        DependsOnApp,
        DependsOnCoreModel,
        DependsOnPlaylistsImplementation,
        InvalidResourceNamespace,
        IosExport,
    }

    private enum class SearchMutation {
        DependsOnShared,
        DependsOnPlayback,
        DependsOnTagLib,
        DependsOnDatabase,
        DependsOnPlatform,
        DependsOnImplementation,
        DependsOnApp,
        InvalidResourceNamespace,
        WrongAndroidNamespace,
        WrongComposeNamespace,
        WrongPackage,
        MissingKDoc,
        IosExport,
        MissingMovedResource,
        DuplicateMovedResource,
        DuplicateMovedResourceZh,
        ExtraMovedResource,
        CrossOwnerDuplicateResource,
        WrongResourceOwner,
        ForeignFeatureResourceImport,
        ForeignSharedResourceImport,
        SharedCommonMainApiExposure,
        RemoveKspJvmProcessor,
        EmptyPackageRoots,
    }

    private enum class SettingsMutation {
        DependsOnShared,
        DependsOnApp,
        DependsOnDatabase,
        DependsOnPlatform,
        DependsOnPlayback,
        DependsOnTagLib,
        DependsOnLibraryApi,
        DependsOnKoin,
        DependsOnDataStore,
        WrongAndroidNamespace,
        WrongComposeNamespace,
        WrongPackage,
        EmptyPackageRoots,
        MissingKDoc,
        IosExport,
        SharedCommonMainApiExposure,
        MissingMovedResource,
        DuplicateMovedResource,
        DuplicateMovedResourceZh,
        ExtraMovedResource,
        CrossOwnerDuplicateResource,
        WrongResourceOwner,
        LocaleParityDivergence,
        InvalidResourceNamespace,
        ForeignFeatureResourceImport,
        ForeignSharedResourceImport,
        MissingLogo,
    }

    private data class SearchResourceFixture(
        val shared: List<File>,
        val feature: List<File>,
        val sharedSource: File,
        val featureSource: File,
    ) {
        companion object {
            fun fromRepository(root: File): SearchResourceFixture =
                SearchResourceFixture(
                    shared = listOf(
                        root.resolve("shared/src/commonMain/composeResources/values/strings.xml"),
                        root.resolve("shared/src/commonMain/composeResources/values-zh/strings.xml"),
                    ),
                    feature = listOf(
                        root.resolve("feature/search/src/commonMain/composeResources/values/strings.xml"),
                        root.resolve("feature/search/src/commonMain/composeResources/values-zh/strings.xml"),
                    ),
                    sharedSource = root.resolve("shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt"),
                    featureSource = root.resolve("feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt"),
                )
        }
    }

    private data class SettingsResourceFixture(
        val shared: List<File>,
        val feature: List<File>,
        val sharedLogo: File,
        val featureLogo: File,
        val sharedSource: File,
        val featureSource: File,
    )

    private data class QualityAggregationFixture(
        val projectDir: File,
        val markerDirectory: File,
    )

    private enum class SqlDelightFixtureDependency(val configuration: String) {
        CompileOnly("compileOnly"),
        Implementation("implementation"),
    }

    private companion object {
        val searchKspTasks = listOf(
            ":feature:search:kspAndroidMain",
            ":feature:search:kspKotlinJvm",
            ":feature:search:kspKotlinIosArm64",
            ":feature:search:kspKotlinIosSimulatorArm64",
        )
        val settingsKspTasks = listOf(
            ":feature:settings:kspAndroidMain",
            ":feature:settings:kspKotlinJvm",
            ":feature:settings:kspKotlinIosArm64",
            ":feature:settings:kspKotlinIosSimulatorArm64",
        )
        const val SQLDELIGHT_FIXTURE_APPLIED_MARKER = "TEST-SQLDELIGHT-FIXTURE-APPLIED"
        const val SQLDELIGHT_RUNTIME_API_MISSING_SENTINEL = "TEST-SQLDELIGHT-RUNTIME-API-MISSING"
        const val SQLDELIGHT_RUNTIME_API_AVAILABLE_MARKER = "TEST-SQLDELIGHT-RUNTIME-API-AVAILABLE"
        const val SQLDELIGHT_EXTENSION_ABSENT_MARKER = "TEST-SQLDELIGHT-EXTENSION-ABSENT"
        const val SQLDELIGHT_EXTENSION_PRESENT_MARKER = "TEST-SQLDELIGHT-EXTENSION-PRESENT"
        val ARCHITECTURE_RULE: Regex = Regex("ARCH-[A-Z-]+")
        val DIAGNOSTIC_RULE: Regex = Regex("(?m)^> (ARCH-[A-Z-]+)(?= )|^  (ARCH-[A-Z-]+)(?= )")
        val modules = listOf(":androidApp", ":desktopApp", ":shared", ":taglib", ":architecture-processor", ":core:model", ":core:database", ":core:ui", ":feature:library:api", ":feature:library:impl", ":feature:playlists:api", ":feature:playlists:impl")
        val documentedModelSource = """
            package com.eterocell.rhythhaus
            import com.eterocell.rhythhaus.documentedModel as `model-alias`
            /** A documented declaration. */
            @Composable
            public fun documentedModel(): String {
                val sample = \"\"\"import com.eterocell.rhythhaus.library.Fake\npublic class Fake\"\"\"
                // public class AnotherFake
                return sample
            }
            /** A documented property. */
            public val documentedProperty = "model"
        """.trimIndent()
    }
}
