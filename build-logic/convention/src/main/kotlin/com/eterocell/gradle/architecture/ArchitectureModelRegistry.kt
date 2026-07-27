package com.eterocell.gradle.architecture

import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

public class ArchitectureModelRegistry private constructor(
    private val providers: ProviderFactory,
) {
    private val resourceProviders: MutableList<Provider<List<ResourceRecord>>> = mutableListOf()
    private val sqlDelightRoots: MutableSet<SqlDelightRootRecord> = sortedSetOf()
    private val sqlDelightOwners: MutableSet<String> = sortedSetOf()
    private val kspRegistrations: MutableSet<KspRegistration> = sortedSetOf()
    private val androidTestConfigurations: MutableSet<AndroidTestConfiguration> = sortedSetOf()

    public fun publishResources(records: Iterable<ResourceRecord>) {
        publishResources(providers.provider { records.toList().distinct().sorted() })
    }

    public fun publishResources(records: Provider<out Iterable<ResourceRecord>>) {
        resourceProviders += records.map { it.toList().distinct().sorted() }
    }

    public fun publishSqlDelight(
        owner: String,
        roots: Iterable<SqlDelightRootRecord>,
        isOwner: Boolean,
    ) {
        sqlDelightRoots += roots
        if (isOwner) sqlDelightOwners += owner
    }

    public fun publishKspRegistration(registration: KspRegistration) {
        kspRegistrations += registration
    }

    public fun publishAndroidTestConfiguration(project: Project, configuration: Configuration) {
        androidTestConfigurations += AndroidTestConfiguration(
            module = project.path,
            configuration = configuration.name,
        )
    }

    public fun snapshot(): Snapshot = Snapshot(
        resources = resourceProviders.fold(providers.provider { emptyList() }) { accumulated, next ->
            accumulated.zip(next) { previous, current -> (previous + current).distinct().sorted() }
        },
        sqlDelightRoots = sqlDelightRoots.toList(),
        sqlDelightOwners = sqlDelightOwners.toSet(),
        kspRegistrations = kspRegistrations.toSet(),
        androidTestConfigurations = androidTestConfigurations.toList(),
    )

    public data class ResourceRecord(
        val module: String,
        val sourceSet: String,
        val kind: String,
        val root: File,
        val namespace: String,
    ) : Comparable<ResourceRecord> {
        override fun compareTo(other: ResourceRecord): Int = toString().compareTo(other.toString())
    }

    public data class SqlDelightRootRecord(
        val module: String,
        val database: String,
        val root: File,
        val status: String,
    ) : Comparable<SqlDelightRootRecord> {
        override fun compareTo(other: SqlDelightRootRecord): Int = toString().compareTo(other.toString())
    }

    public data class KspRegistration(
        val module: String,
        val configuration: String,
        val processor: String,
    ) : Comparable<KspRegistration> {
        override fun compareTo(other: KspRegistration): Int = toString().compareTo(other.toString())
    }

    public data class AndroidTestConfiguration(
        val module: String,
        val configuration: String,
    ) : Comparable<AndroidTestConfiguration> {
        override fun compareTo(other: AndroidTestConfiguration): Int = toString().compareTo(other.toString())
    }

    public data class Snapshot(
        val resources: Provider<List<ResourceRecord>>,
        val sqlDelightRoots: List<SqlDelightRootRecord>,
        val sqlDelightOwners: Set<String>,
        val kspRegistrations: Set<KspRegistration>,
        val androidTestConfigurations: List<AndroidTestConfiguration>,
    ) {
        public fun suppressesAndroidSyntheticSelfDependency(
            consumerPath: String,
            configurationName: String,
            providerPath: String,
            directSelfDependencyCount: Int,
        ): Boolean =
            providerPath == consumerPath &&
                directSelfDependencyCount == 1 &&
                androidTestConfigurations.any { identity ->
                    identity.module == consumerPath &&
                        identity.configuration == configurationName
                }
    }

    public companion object {
        private const val KEY: String = "com.eterocell.architectureModelRegistry"

        public fun forRoot(project: Project): ArchitectureModelRegistry {
            val root = project.rootProject
            return root.extensions.extraProperties.let { properties ->
                if (properties.has(KEY)) properties.get(KEY) as ArchitectureModelRegistry
                else ArchitectureModelRegistry(project.providers).also { properties.set(KEY, it) }
            }
        }
    }
}
