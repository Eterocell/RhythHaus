package com.eterocell.rhythhaus.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

internal const val CurrentOnboardingSchemaVersion: Int = 1
private val CompletedSchemaVersionKey =
    intPreferencesKey("completed_schema_version")

public enum class OnboardingEligibility {
    Loading,
    Required,
    Completed
}

public interface OnboardingPreferenceStore {
    public val eligibility: Flow<OnboardingEligibility>

    public suspend fun markCurrentVersionCompleted()
}

internal class DataStoreOnboardingPreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : OnboardingPreferenceStore {
    private val completionConfirmed = MutableStateFlow(false)

    override val eligibility: Flow<OnboardingEligibility> =
        dataStore.data
            .catch { failure ->
                if (failure is CancellationException) throw failure
                emit(androidx.datastore.preferences.core.emptyPreferences())
            }
            .combine(completionConfirmed) { preferences, completed ->
                if (completed ||
                    (preferences[CompletedSchemaVersionKey] ?: 0) >=
                        CurrentOnboardingSchemaVersion) {
                    OnboardingEligibility.Completed
                } else {
                    OnboardingEligibility.Required
                }
            }
            .onStart { emit(OnboardingEligibility.Loading) }

    override suspend fun markCurrentVersionCompleted() {
        dataStore.edit { preferences ->
            val existing = preferences[CompletedSchemaVersionKey] ?: 0
            if (existing < CurrentOnboardingSchemaVersion) {
                preferences[CompletedSchemaVersionKey] =
                    CurrentOnboardingSchemaVersion
            }
        }
        completionConfirmed.value = true
    }
}

public expect fun createOnboardingPreferenceStore(): OnboardingPreferenceStore
