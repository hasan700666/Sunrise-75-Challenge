package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class AppScreen {
    SPLASH,
    MAIN,
    RESULT
}

data class Question(
    val id: Int,
    val text: String,
    val isChecked: Boolean = false
)

data class UiState(
    val currentScreen: AppScreen = AppScreen.SPLASH,
    val streakCount: Int = 0,
    val todayCompleted: Boolean = false,
    val todaySuccessful: Boolean = false,
    val activeChallengeDate: LocalDate = LocalDate.now(),
    val todaySunriseTime: LocalTime = LocalTime.of(5, 30),
    val currentSunriseSchedule: Map<LocalDate, LocalTime> = emptyMap(),
    val questions: List<Question> = emptyList(),
    val isImageUploading: Boolean = false,
    val generatedBitmap: Bitmap? = null,
    val savedImageUriString: String? = null,
    val cityName: String = "Dhaka",
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val isSyncingSunrise: Boolean = false,
    val toastMessage: String? = null
)

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs = ChallengePrefs(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    companion object {
        const val TAG = "ChallengeViewModel"
    }

    init {
        // Initialize default questions based on the elite Sunrise 75 Challenge
        val initialQuestions = listOf(
            Question(1, "Did you wake up before sunrise today?"),
            Question(2, "Did you stay fully hydrated (3+ Liters of water)?"),
            Question(3, "Did you read 10 pages of a book of choice?"),
            Question(4, "Did you complete 45 minutes of physical workout?"),
            Question(5, "Did you maintain a clean diet and avoid alcohol?")
        )

        _uiState.value = UiState(
            streakCount = prefs.streak,
            questions = initialQuestions,
            cityName = prefs.cityName,
            latitude = prefs.latitude.toDouble(),
            longitude = prefs.longitude.toDouble()
        )

        // Run splash timeout and prepare sunrise schedule in parallel
        viewModelScope.launch {
            // Splash screens are 1 second long as per requirements
            delay(1000)
            
            // Check sunrise offline schedule and initialize
            initializeSunriseAndSchedule()
            
            // Navigate to main activity dashboard
            _uiState.value = _uiState.value.copy(currentScreen = AppScreen.MAIN)
        }
    }

    /**
     * Initializes sunrise schedules in local storage or calculates offline immediately.
     * Attempts the non-blocking background fetch if connected to the internet.
     */
    private suspend fun initializeSunriseAndSchedule() {
        var schedule = prefs.getSunriseSchedule()
        val today = LocalDate.now()
        
        // If schedule has expired or is empty, generate locally first
        if (schedule.isEmpty() || !schedule.containsKey(today)) {
            Log.d(TAG, "No schedule on file for today, generating 100% offline dataset")
            schedule = SunriseManager.generateOfflineSchedule(today, prefs.latitude.toDouble(), prefs.longitude.toDouble())
            prefs.saveSunriseSchedule(schedule)
        }

        val clockTime = LocalDateTime.now()
        val finalSchedule = schedule
        val activeDate = calculateActiveChallengeDate(clockTime, finalSchedule)
        val activeSunrise = finalSchedule[activeDate] ?: LocalTime.of(5, 30)

        _uiState.value = _uiState.value.copy(
            activeChallengeDate = activeDate,
            todaySunriseTime = activeSunrise,
            currentSunriseSchedule = finalSchedule,
            streakCount = prefs.streak,
            todayCompleted = prefs.isDayCompleted(activeDate),
            todaySuccessful = prefs.wasDaySuccessful(activeDate)
        )

        // Non-blocking trigger: Fetch fresh data if internet is available, to update schedule with true accuracy!
        viewModelScope.launch {
            refreshSunriseScheduleOnline()
        }
    }

    /**
     * Compares timezone clock with the true sunrise schedule to determine active challenge day.
     */
    fun calculateActiveChallengeDate(currentTime: LocalDateTime, schedule: Map<LocalDate, LocalTime>): LocalDate {
        val today = currentTime.toLocalDate()
        val sunriseForToday = schedule[today] ?: LocalTime.of(5, 30)
        val sunriseDateTimeForToday = LocalDateTime.of(today, sunriseForToday)

        return if (currentTime.isAfter(sunriseDateTimeForToday) || currentTime.isEqual(sunriseDateTimeForToday)) {
            today
        } else {
            today.minusDays(1)
        }
    }

    /**
     * Attempts to refresh sunrise online from the global api.
     */
    fun refreshSunriseScheduleOnline() {
        if (_uiState.value.isSyncingSunrise) return
        _uiState.value = _uiState.value.copy(isSyncingSunrise = true)

        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val apiSchedule = SunriseManager.fetchSunriseSchedule(
                    startDate = today,
                    latitude = _uiState.value.latitude,
                    longitude = _uiState.value.longitude
                )

                if (apiSchedule.isNotEmpty()) {
                    prefs.saveSunriseSchedule(apiSchedule)
                    
                    val clockTime = LocalDateTime.now()
                    val activeDate = calculateActiveChallengeDate(clockTime, apiSchedule)
                    val activeSunrise = apiSchedule[activeDate] ?: LocalTime.of(5, 30)

                    _uiState.value = _uiState.value.copy(
                        currentSunriseSchedule = apiSchedule,
                        activeChallengeDate = activeDate,
                        todaySunriseTime = activeSunrise,
                        todayCompleted = prefs.isDayCompleted(activeDate),
                        todaySuccessful = prefs.wasDaySuccessful(activeDate),
                        isSyncingSunrise = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSyncingSunrise = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing online schedule", e)
                _uiState.value = _uiState.value.copy(isSyncingSunrise = false)
            }
        }
    }

    /**
     * Updates location configuration, triggering fresh sunrise times.
     */
    fun updateLocation(city: String, lat: Double, lng: Double) {
        prefs.cityName = city
        prefs.latitude = lat.toFloat()
        prefs.longitude = lng.toFloat()

        _uiState.value = _uiState.value.copy(
            cityName = city,
            latitude = lat,
            longitude = lng
        )

        viewModelScope.launch {
            // Regenerate offline immediately
            val today = LocalDate.now()
            val offlineSchedule = SunriseManager.generateOfflineSchedule(today, lat, lng)
            prefs.saveSunriseSchedule(offlineSchedule)
            
            val clock = LocalDateTime.now()
            val activeDate = calculateActiveChallengeDate(clock, offlineSchedule)
            val activeSunrise = offlineSchedule[activeDate] ?: LocalTime.of(5, 30)

            _uiState.value = _uiState.value.copy(
                currentSunriseSchedule = offlineSchedule,
                activeChallengeDate = activeDate,
                todaySunriseTime = activeSunrise,
                todayCompleted = prefs.isDayCompleted(activeDate),
                todaySuccessful = prefs.wasDaySuccessful(activeDate)
            )

            // Attempt online sync
            refreshSunriseScheduleOnline()
        }
    }

    /**
     * Triggers whenever a question's checkbox state is updated.
     */
    fun toggleQuestion(questionId: Int, checked: Boolean) {
        val updatedQuestions = _uiState.value.questions.map {
            if (it.id == questionId) it.copy(isChecked = checked) else it
        }
        _uiState.value = _uiState.value.copy(questions = updatedQuestions)
    }

    /**
     * Handles answering "NO" to any daily challenge question.
     * Instantly resets active challenge streak count to 0, saves failures, and refreshes UI.
     */
    fun onAnswerNo() {
        prefs.streak = 0
        prefs.markFailed(_uiState.value.activeChallengeDate)

        _uiState.value = _uiState.value.copy(
            streakCount = 0,
            todayCompleted = true,
            todaySuccessful = false,
            toastMessage = "Streak Reset to 0! Stay disciplined, rise tomorrow."
        )
    }

    /**
     * Processes selected image, handles vertical divider sections and overlays labels.
     * Automatically increments the completed streak count by 1.
     */
    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(isImageUploading = true)

        viewModelScope.launch {
            try {
                val processedBitmap = withContext(Dispatchers.IO) {
                    // Safe, high-performance bitmap loader for low-end Android devices
                    val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Use standard software canvas for low-end device compatibility
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }

                    // Memory safety scaling - Limit height to max 1080px to protect low-RAM hardware!
                    val maxWidth = 1000f
                    val maxHeight = 1000f
                    val scale = (maxWidth / originalBitmap.width).coerceAtMost(maxHeight / originalBitmap.height)
                    val scaledBitmap = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    // Increment the daily streak count on high quality completions
                    val newStreak = prefs.streak + 1
                    
                    // Division + Streak Overlay Painter
                    ImageProcessor.processStreakImage(scaledBitmap, newStreak)
                }

                // Apply persistence changes safely
                val finalStreak = prefs.streak + 1
                prefs.streak = finalStreak
                prefs.markCompleted(_uiState.value.activeChallengeDate)

                _uiState.value = _uiState.value.copy(
                    streakCount = finalStreak,
                    todayCompleted = true,
                    todaySuccessful = true,
                    generatedBitmap = processedBitmap,
                    currentScreen = AppScreen.RESULT,
                    isImageUploading = false,
                    toastMessage = "Day Complete! Challenge Saved."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading/processing chosen image", e)
                _uiState.value = _uiState.value.copy(
                    isImageUploading = false,
                    toastMessage = "Error loading image. Try another."
                )
            }
        }
    }

    /**
     * Download processed completed bitmap to public system Picture Gallery.
     */
    fun downloadGeneratedImage() {
        val bitmap = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch {
            val fileUri = withContext(Dispatchers.IO) {
                ImageProcessor.saveImageToGallery(context, bitmap)
            }
            if (fileUri != null) {
                _uiState.value = _uiState.value.copy(
                    savedImageUriString = fileUri.toString(),
                    toastMessage = "Saved to Gallery /Pictures/Challenge!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Failed to download image. Try again."
                )
            }
        }
    }

    /**
     * Resets the showing message state.
     */
    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    /**
     * Navigates back to dashboard/main menu.
     */
    fun navigateToMain() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.MAIN)
    }

    /**
     * Allows user to view computed image again.
     */
    fun navigateToResult() {
        if (_uiState.value.generatedBitmap != null) {
            _uiState.value = _uiState.value.copy(currentScreen = AppScreen.RESULT)
        }
    }
    
    /**
     * Developer override for testing - resets all challenge data.
     */
    fun resetAllDataForTesting() {
        prefs.streak = 0
        prefs.completedDates = emptySet()
        prefs.failedDates = emptySet()
        
        // Reset local questions list
        val refreshedQuestions = _uiState.value.questions.map { it.copy(isChecked = false) }
        
        _uiState.value = _uiState.value.copy(
            streakCount = 0,
            todayCompleted = false,
            todaySuccessful = false,
            questions = refreshedQuestions,
            generatedBitmap = null,
            savedImageUriString = null,
            toastMessage = "All Data Reset Successfully"
        )
    }
}
