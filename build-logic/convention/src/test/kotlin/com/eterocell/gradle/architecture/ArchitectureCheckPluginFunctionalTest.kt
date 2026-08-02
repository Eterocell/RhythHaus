package com.eterocell.gradle.architecture

import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val result = runner(projectDir, task).buildAndFail()
        assertExactDiagnostics(result.output, expectedDiagnostics)
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

    private fun assertKspCompilationFailure(source: String, expectedDiagnostic: String) {
        val projectDir = kspFixture(source)
        val result = runner(projectDir, "compileKotlinJvm").buildAndFail()
        assertTrue(result.output.contains(expectedDiagnostic), result.output)
    }

    private fun assertKspTaskRan(output: String) {
        assertTrue(output.contains(":core:model:kspKotlinJvm"), output)
        assertTrue(!output.contains(":core:model:kspKotlinJvm SKIPPED"), output)
        assertTrue(!output.contains(":core:model:kspKotlinJvm NO-SOURCE"), output)
    }

    private fun kspDiagnostics(output: String): List<String> =
        output
            .lineSequence()
            .mapNotNull { line ->
                Regex("(ARCH-(?:PACKAGE|KDOC) .*)").find(line)?.groupValues?.get(1)
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

    private data class QualityAggregationFixture(
        val projectDir: File,
        val markerDirectory: File,
    )

    private enum class SqlDelightFixtureDependency(val configuration: String) {
        CompileOnly("compileOnly"),
        Implementation("implementation"),
    }

    private companion object {
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
