package com.example.tripexpensetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeCityDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_city")

/**
 * Manages the active city session state with persistence across app restarts.
 * Only one city can be "visited" at a time.
 */
@Singleton
class ActiveCityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACTIVE_TRIP_ID = stringPreferencesKey("active_trip_id")
        private val ACTIVE_CITY_ID = stringPreferencesKey("active_city_id")
        private val ACTIVE_CITY_NAME = stringPreferencesKey("active_city_name")
    }
    
    /**
     * Get the currently active city ID for a trip
     */
    val activeCityId: Flow<String?> = context.activeCityDataStore.data.map { preferences: Preferences ->
        preferences[ACTIVE_CITY_ID]
    }
    
    val activeTripId: Flow<String?> = context.activeCityDataStore.data.map { preferences: Preferences ->
        preferences[ACTIVE_TRIP_ID]
    }
    
    val activeCityName: Flow<String?> = context.activeCityDataStore.data.map { preferences: Preferences ->
        preferences[ACTIVE_CITY_NAME]
    }
    
    /**
     * Start a visit to a city. Only one city can be active at a time.
     */
    suspend fun startCityVisit(tripId: String, cityId: String, cityName: String) {
        context.activeCityDataStore.edit { preferences: MutablePreferences ->
            preferences[ACTIVE_TRIP_ID] = tripId
            preferences[ACTIVE_CITY_ID] = cityId
            preferences[ACTIVE_CITY_NAME] = cityName
        }
    }
    
    /**
     * End the current city visit
     */
    suspend fun endCityVisit() {
        context.activeCityDataStore.edit { preferences: MutablePreferences ->
            preferences.remove(ACTIVE_TRIP_ID)
            preferences.remove(ACTIVE_CITY_ID)
            preferences.remove(ACTIVE_CITY_NAME)
        }
    }
    
    /**
     * Check if a specific city is currently active
     */
    fun isCityActive(cityId: String): Flow<Boolean> {
        return activeCityId.map { it == cityId }
    }
}
