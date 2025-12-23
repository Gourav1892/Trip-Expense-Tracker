package com.example.tripexpensetracker.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_ONBOARDING_COMPLETED = "is_onboarding_completed"
    }

    fun saveOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_IS_ONBOARDING_COMPLETED, true).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_IS_ONBOARDING_COMPLETED, false)
    }
}
