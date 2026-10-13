package com.eterocell.rhythhaus.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val AutomaticResumePolicyPreferenceKey =
    stringPreferencesKey("automatic_resume_policy")

public interface AutomaticResumePreferenceStore {
    public val selectedPolicy: StateFlow<AutomaticResumePolicy>

    public suspend fun readPolicy(): AutomaticResumePolicy

    public suspend fun setPolicy(policy: AutomaticResumePolicy)
}

internal class DataStoreAutomaticResumePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : AutomaticResumePreferenceStore {
    private val gate = Mutex()
    private val mutablePolicy =
        MutableStateFlow(AutomaticResumePolicy.FollowSystemRecommendation)

    override val selectedPolicy: StateFlow<AutomaticResumePolicy> =
        mutablePolicy.asStateFlow()

    override suspend fun readPolicy(): AutomaticResumePolicy = gate.withLock {
        val policy =
            try {
                when (dataStore.data
                    .first()
                    .asMap()
                    .entries
                    .firstOrNull {
                        it.key.name == AutomaticResumePolicyPreferenceKey.name
                    }
                    ?.value as? String) {
                    "never" -> AutomaticResumePolicy.Never
                    "follow-system" ->
                        AutomaticResumePolicy.FollowSystemRecommendation
                    else -> AutomaticResumePolicy.FollowSystemRecommendation
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                AutomaticResumePolicy.FollowSystemRecommendation
            }
        mutablePolicy.value = policy
        policy
    }

    override suspend fun setPolicy(policy: AutomaticResumePolicy) {
        gate.withLock {
            dataStore.edit { preferences ->
                preferences[AutomaticResumePolicyPreferenceKey] =
                    when (policy) {
                        AutomaticResumePolicy.FollowSystemRecommendation ->
                            "follow-system"
                        AutomaticResumePolicy.Never -> "never"
                    }
            }
            mutablePolicy.value = policy
        }
    }
}

public expect fun createAutomaticResumePreferenceStore():
    AutomaticResumePreferenceStore
