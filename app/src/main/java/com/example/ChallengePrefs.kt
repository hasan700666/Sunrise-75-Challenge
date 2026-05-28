package com.example

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

class ChallengePrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sunrise_75_challenge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STREAK = "streak"
        private const val KEY_COMPLETED_DATES = "completed_dates"
        private const val KEY_FAILED_DATES = "failed_dates"
        private const val KEY_SUNRISE_SCHEDULE = "sunrise_schedule"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_CITY_NAME = "city_name"
    }

    // Streak System
    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    // Completed Dates (Set of Strings "YYYY-MM-DD")
    var completedDates: Set<String>
        get() = prefs.getStringSet(KEY_COMPLETED_DATES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_COMPLETED_DATES, value).apply()

    // Failed Dates (Set of Strings "YYYY-MM-DD")
    var failedDates: Set<String>
        get() = prefs.getStringSet(KEY_FAILED_DATES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAILED_DATES, value).apply()

    // Save location configuration for more personalized sunrise
    var latitude: Float
        get() = prefs.getFloat(KEY_LATITUDE, 23.8103f) // Default Dhaka
        set(value) = prefs.edit().putFloat(KEY_LATITUDE, value).apply()

    var longitude: Float
        get() = prefs.getFloat(KEY_LONGITUDE, 90.4125f)
        set(value) = prefs.edit().putFloat(KEY_LONGITUDE, value).apply()

    var cityName: String
        get() = prefs.getString(KEY_CITY_NAME, "Dhaka") ?: "Dhaka"
        set(value) = prefs.edit().putString(KEY_CITY_NAME, value).apply()

    // Sunrise schedule stored as a JSON String: { "YYYY-MM-DD": "HH:MM", ... }
    fun getSunriseSchedule(): Map<LocalDate, LocalTime> {
        val jsonString = prefs.getString(KEY_SUNRISE_SCHEDULE, null) ?: return emptyMap()
        val schedule = mutableMapOf<LocalDate, LocalTime>()
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val date = LocalDate.parse(key)
                val time = LocalTime.parse(jsonObject.getString(key))
                schedule[date] = time
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return schedule
    }

    fun saveSunriseSchedule(schedule: Map<LocalDate, LocalTime>) {
        try {
            val jsonObject = JSONObject()
            for ((date, time) in schedule) {
                jsonObject.put(date.toString(), time.toString())
            }
            prefs.edit().putString(KEY_SUNRISE_SCHEDULE, jsonObject.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper functions
    fun markCompleted(date: LocalDate) {
        val currentSet = completedDates.toMutableSet()
        currentSet.add(date.toString())
        completedDates = currentSet
        
        // Remove from failed if it was there
        val currentFailed = failedDates.toMutableSet()
        if (currentFailed.remove(date.toString())) {
            failedDates = currentFailed
        }
    }

    fun markFailed(date: LocalDate) {
        val currentSet = failedDates.toMutableSet()
        currentSet.add(date.toString())
        failedDates = currentSet
        
        // Remove from completed if it was there
        val currentCompleted = completedDates.toMutableSet()
        if (currentCompleted.remove(date.toString())) {
            completedDates = currentCompleted
        }
    }

    fun isDayCompleted(date: LocalDate): Boolean {
        return completedDates.contains(date.toString()) || failedDates.contains(date.toString())
    }

    fun wasDaySuccessful(date: LocalDate): Boolean {
        return completedDates.contains(date.toString())
    }
}
