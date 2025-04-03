package com.markus.clock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markus.clock.model.Activity
import com.markus.clock.ui.theme.ActivityColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.Color

// Data class for minute arc segments
data class MinuteArcSegment(
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,  // Add color to minute arc segment
    val transitionStartTime: Long = 0 // Timestamp when transition started
)

// Data class for hour arc segments
data class HourArcSegment(
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,  // Add color to hour arc segment
    val transitionStartTime: Long = 0 // Timestamp when transition started
)

class TimerViewModel : ViewModel() {
    
    // Activities list - replaces single activity type
    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()
    
    // New activity being created/edited
    private val _currentActivity = MutableStateFlow<Activity?>(null)
    val currentActivity: StateFlow<Activity?> = _currentActivity.asStateFlow()
    
    // Wallpaper selection (default to first wallpaper)
    private val _selectedWallpaperId = MutableStateFlow(1)
    val selectedWallpaperId: StateFlow<Int> = _selectedWallpaperId.asStateFlow()
    
    // Timer end state - true when all activities are completed
    private val _isAllActivitiesCompleted = MutableStateFlow(false)
    val isAllActivitiesCompleted: StateFlow<Boolean> = _isAllActivitiesCompleted.asStateFlow()
    
    // Current time
    private val _currentHour = MutableStateFlow(0)
    val currentHour: StateFlow<Int> = _currentHour.asStateFlow()
    
    private val _currentMinute = MutableStateFlow(0)
    val currentMinute: StateFlow<Int> = _currentMinute.asStateFlow()
    
    private val _currentSecond = MutableStateFlow(0)
    val currentSecond: StateFlow<Int> = _currentSecond.asStateFlow()
    
    // Timer state
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    
    // Current activity completion notification
    private val _recentlyCompletedActivity = MutableStateFlow<Activity?>(null)
    val recentlyCompletedActivity: StateFlow<Activity?> = _recentlyCompletedActivity.asStateFlow()
    
    // Track recently activated activities
    private val _activeTransitions = MutableStateFlow<Map<String, Long>>(emptyMap())
    
    private var timerJob: Job? = null
    private var clockJob: Job? = null
    
    init {
        // Start the real-time clock with a more frequent update interval
        startClock()
    }
    
    private fun startClock() {
        clockJob?.cancel() // Cancel any existing job first
        
        clockJob = viewModelScope.launch {
            while (true) {
                updateCurrentTime()
                delay(100) // Update every 100 ms for smooth second hand movement
            }
        }
    }
    
    private fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        _currentHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        _currentMinute.value = calendar.get(Calendar.MINUTE)
        _currentSecond.value = calendar.get(Calendar.SECOND)
        
        // If timer is running, update the activities progress
        if (_isTimerRunning.value) {
            updateActivitiesProgress()
        }
    }
    
    fun setWallpaper(wallpaperId: Int) {
        _selectedWallpaperId.value = wallpaperId
    }
    
    // Clear the recently completed activity notification
    fun clearCompletionNotification() {
        _recentlyCompletedActivity.value = null
    }
    
    // Create a new activity and add it to the list
    fun createActivity(name: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        val startTimeMinutes = startHour * 60 + startMinute
        val endTimeMinutes = endHour * 60 + endMinute
        
        // Get a color pair for the activity (cycling through available colors)
        val colorIndex = _activities.value.size % ActivityColors.size
        val (hourColor, minuteColor) = ActivityColors[colorIndex]
        
        val newActivity = Activity(
            id = UUID.randomUUID().toString(),
            name = name,
            hourColor = hourColor,
            minuteColor = minuteColor,
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes
        )
        
        // Add to list of activities
        val updatedList = _activities.value.toMutableList()
        updatedList.add(newActivity)
        
        // Sort activities by start time
        updatedList.sortBy { it.startTimeMinutes }
        
        _activities.value = updatedList
        _currentActivity.value = null // Clear current activity
    }
    
    // Update an existing activity
    fun updateActivity(activity: Activity) {
        val updatedList = _activities.value.toMutableList()
        val index = updatedList.indexOfFirst { it.id == activity.id }
        
        if (index != -1) {
            updatedList[index] = activity
            
            // Sort activities by start time
            updatedList.sortBy { it.startTimeMinutes }
            
            _activities.value = updatedList
        }
        
        _currentActivity.value = null // Clear current activity
    }
    
    // Delete an activity
    fun deleteActivity(activityId: String) {
        val updatedList = _activities.value.toMutableList()
        updatedList.removeAll { it.id == activityId }
        _activities.value = updatedList
    }
    
    // Set activity being edited
    fun setCurrentActivity(activity: Activity?) {
        _currentActivity.value = activity
    }
    
    // Create a new activity for editing
    fun createNewActivity() {
        // Get start time based on previous activity or current time
        val lastActivity = _activities.value.maxByOrNull { it.endTimeMinutes }
        
        val startTimeMinutes = if (lastActivity != null) {
            // Use the end time of the last activity
            lastActivity.endTimeMinutes
        } else {
            // Default to current hour if no previous activities
            _currentHour.value * 60
        }
        
        // Set end time to one hour after start time
        val endTimeMinutes = startTimeMinutes + 60
        
        _currentActivity.value = Activity(
            id = UUID.randomUUID().toString(),
            name = "",
            hourColor = Color.Gray, // Placeholder, will be set on save
            minuteColor = Color.Gray, // Placeholder, will be set on save
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes
        )
    }
    
    // Check if current activity is a new one (not yet saved)
    fun isNewActivity(): Boolean {
        val currentActivity = _currentActivity.value ?: return false
        // Check if this activity exists in our activities list
        return _activities.value.none { it.id == currentActivity.id }
    }
    
    private fun updateActivitiesProgress() {
        val currentMinutes = _currentHour.value * 60 + _currentMinute.value + (_currentSecond.value / 60f)
        var allCompleted = true
        var anyNewlyCompleted = false
        
        // Check each activity
        val updatedActivities = _activities.value.map { activity ->
            // Check if activity is in progress or completed
            val isCompleted = isAfterEndTime(currentMinutes, activity.startTimeMinutes, activity.endTimeMinutes)
            
            // If an activity just completed, notify
            if (isCompleted && !activity.isCompleted) {
                anyNewlyCompleted = true
                _recentlyCompletedActivity.value = activity
            }
            
            // Check if all activities are completed
            if (!isCompleted) {
                allCompleted = false
            }
            
            // Return updated activity
            activity.copy(isCompleted = isCompleted)
        }
        
        // Update activities with completion status
        if (anyNewlyCompleted) {
            _activities.value = updatedActivities
        }
        
        // Update all activities completed flag
        _isAllActivitiesCompleted.value = allCompleted && _activities.value.isNotEmpty()
    }
    
    // Start the timer with the current set of activities
    fun startTimer() {
        if (_isTimerRunning.value) {
            stopTimer()
        }
        
        if (_activities.value.isEmpty()) {
            return // Don't start if no activities
        }
        
        // Reset completion status
        val resetActivities = _activities.value.map { it.copy(isCompleted = false) }
        _activities.value = resetActivities
        
        _isTimerRunning.value = true
        _isAllActivitiesCompleted.value = false
        _recentlyCompletedActivity.value = null
    }
    
    fun stopTimer() {
        _isTimerRunning.value = false
        _isAllActivitiesCompleted.value = false
        _recentlyCompletedActivity.value = null
    }
    
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
    
    // Return hour and minute angles for the clock hands
    fun getHourAngle(): Float {
        val hour = _currentHour.value % 12
        val minute = _currentMinute.value
        return (hour * 30f) + (minute * 0.5f) // 30 degrees per hour, 0.5 degrees per minute
    }
    
    fun getMinuteAngle(): Float {
        val minute = _currentMinute.value
        val second = _currentSecond.value
        // For each minute, the hand should move 6 degrees (360/60)
        // For each second, the hand should move 0.1 degrees (6/60)
        // Correcting the calculation to ensure the minute hand is positioned correctly
        return minute * 6f + second * 0.1f
    }
    
    fun getSecondAngle(): Float {
        return _currentSecond.value * 6f // 6 degrees per second
    }
    
    // Calculate the current time position in degrees (for hour arc)
    private fun calculateCurrentAngle(): Float {
        val currentHour = _currentHour.value % 12
        val currentMinute = _currentMinute.value
        val currentSecond = _currentSecond.value
        
        // For hour hand: 30 degrees per hour (360/12), 0.5 degrees per minute (30/60)
        return (currentHour * 30f) + (currentMinute * 0.5f) + (currentSecond * 0.5f / 60f)
    }
    
    // Make this public so the UI can access it
    fun getCurrentAngle(): Float = calculateCurrentAngle()
    
    // Calculate the current minute position in degrees
    private fun calculateCurrentAngleMinutes(): Float {
        // Ensure this matches the minute angle calculation in getMinuteAngle()
        val minute = _currentMinute.value
        val second = _currentSecond.value
        return minute * 6f + second * 0.1f
    }
    
    // Make this public so the UI can access it
    fun getCurrentAngleMinutes(): Float = calculateCurrentAngleMinutes()
    
    // Get hour arc segments for all activities
    fun getHourArcSegments(): List<HourArcSegment> {
        if (!_isTimerRunning.value && _activities.value.isEmpty()) {
            return emptyList()
        }
        
        val currentTimeMinutes = _currentHour.value * 60 + _currentMinute.value + (_currentSecond.value / 60f)
        val currentTime = System.currentTimeMillis()
        val activeTransitions = _activeTransitions.value.toMutableMap()
        var transitionsUpdated = false
        
        return _activities.value.filterNot { it.isCompleted }.map { activity ->
            val startHour = activity.startTimeMinutes / 60 % 12
            val startMinute = activity.startTimeMinutes % 60
            val endHour = activity.endTimeMinutes / 60 % 12
            val endMinute = activity.endTimeMinutes % 60
            
            val startAngle = (startHour * 30f) + (startMinute * 0.5f) - 90f
            var sweepAngle = 0f
            
            // Calculate hour positions including minutes
            val startHourPosition = startHour + (startMinute / 60f)
            val endHourPosition = endHour + (endMinute / 60f)
            
            // Calculate sweep angle, handling midnight crossing
            sweepAngle = if (activity.endTimeMinutes < activity.startTimeMinutes || 
                            (endHourPosition < startHourPosition && activity.endTimeMinutes > activity.startTimeMinutes)) {
                // Crosses midnight - full circle plus the portion from 0 to end
                (360f - (startHourPosition * 30f)) + (endHourPosition * 30f)
            } else {
                // Normal case - difference between angles
                (endHourPosition - startHourPosition) * 30f
            }
            
            // Check if activity is active now
            val isActive = isCurrentTimeInRange(currentTimeMinutes, activity.startTimeMinutes, activity.endTimeMinutes)
            
            // Check if this is a newly activated activity and record transition start time
            if (isActive && !activeTransitions.containsKey(activity.id)) {
                activeTransitions[activity.id] = currentTime
                transitionsUpdated = true
            } else if (!isActive && activeTransitions.containsKey(activity.id)) {
                // Remove from active transitions if no longer active
                activeTransitions.remove(activity.id)
                transitionsUpdated = true
            }
            
            // Calculate opacity based on transition state
            val transitionStartTime = activeTransitions[activity.id] ?: 0
            val elapsedTime = if (transitionStartTime > 0) currentTime - transitionStartTime else 1000L
            val transitionDuration = 1000L // 1 second transition
            
            if (isAfterEndTime(currentTimeMinutes, activity.startTimeMinutes, activity.endTimeMinutes)) {
                // Activity completed - no arc
                HourArcSegment(startAngle, 0f, activity.hourColor)
            } else if (isActive) {
                // Activity in progress - calculate remaining arc
                val currentAngle = getCurrentAngle() - 90f // Adjust for drawing
                var remainingSweepAngle = 0f
                
                if (endHourPosition < startHourPosition) {
                    // Handle crossing midnight
                    val currentHourPosition = (_currentHour.value % 12) + (_currentMinute.value / 60f)
                    
                    remainingSweepAngle = if (currentHourPosition < endHourPosition) {
                        // We're in the morning part after midnight
                        endHourPosition * 30f - (currentHourPosition * 30f)
                    } else {
                        // We're in the evening part before midnight
                        (360f - (currentHourPosition * 30f)) + (endHourPosition * 30f)
                    }
                } else {
                    // Normal case
                    remainingSweepAngle = (endHourPosition * 30f) - ((_currentHour.value % 12) + (_currentMinute.value / 60f)) * 30f
                }
                
                // Calculate opacity - transition from 0.4 to 1.0 over 1 second
                val opacity = if (elapsedTime < transitionDuration) {
                    0.4f + (0.6f * (elapsedTime.toFloat() / transitionDuration.toFloat()))
                } else {
                    1.0f
                }
                
                HourArcSegment(
                    currentAngle, 
                    remainingSweepAngle, 
                    activity.hourColor.copy(alpha = opacity),
                    transitionStartTime
                )
            } else {
                // Future activity - reduced opacity
                HourArcSegment(startAngle, sweepAngle, activity.hourColor.copy(alpha = 0.4f))
            }
        }.also {
            // Update active transitions if needed
            if (transitionsUpdated) {
                _activeTransitions.value = activeTransitions
            }
        }
    }
    
    // Get minute arc segments for all activities
    fun getMinuteArcSegments(): List<MinuteArcSegment> {
        if (!_isTimerRunning.value && _activities.value.isEmpty()) {
            return emptyList()
        }
        
        val currentTimeMinutes = _currentHour.value * 60 + _currentMinute.value + (_currentSecond.value / 60f)
        val currentTime = System.currentTimeMillis()
        val activeTransitions = _activeTransitions.value
        
        return _activities.value.filterNot { it.isCompleted }.map { activity ->
            val startMinuteAngle = ((activity.startTimeMinutes % 60) * 6f) - 90f
            val endMinuteAngle = ((activity.endTimeMinutes % 60) * 6f) - 90f
            
            // Calculate sweep angle, handling hour crossings
            val sweepAngle = if (activity.endTimeMinutes / 60 != activity.startTimeMinutes / 60) {
                // Crosses an hour boundary
                val hoursDifference = (activity.endTimeMinutes / 60) - (activity.startTimeMinutes / 60)
                if (hoursDifference == 1 || (hoursDifference < 0 && hoursDifference > -23)) {
                    // Crossing just one hour boundary
                    val minutesToHourEnd = 60 - (activity.startTimeMinutes % 60)
                    val minutesFromHourStart = activity.endTimeMinutes % 60
                    (minutesToHourEnd + minutesFromHourStart) * 6f
                } else if (endMinuteAngle < startMinuteAngle) {
                    (360f - startMinuteAngle) + endMinuteAngle
                } else {
                    endMinuteAngle - startMinuteAngle
                }
            } else {
                // Same hour
                if (endMinuteAngle < startMinuteAngle) {
                    (360f - startMinuteAngle) + endMinuteAngle
                } else {
                    endMinuteAngle - startMinuteAngle
                }
            }
            
            // Check if activity is active now
            val isActive = isCurrentTimeInRange(currentTimeMinutes, activity.startTimeMinutes, activity.endTimeMinutes)
            
            // Calculate opacity based on transition state
            val transitionStartTime = activeTransitions[activity.id] ?: 0
            val elapsedTime = if (transitionStartTime > 0) currentTime - transitionStartTime else 1000L
            val transitionDuration = 1000L // 1 second transition
            
            if (isAfterEndTime(currentTimeMinutes, activity.startTimeMinutes, activity.endTimeMinutes)) {
                // Activity completed - no arc
                MinuteArcSegment(startMinuteAngle, 0f, activity.minuteColor)
            } else if (isActive) {
                // Activity in progress - calculate remaining arc
                val minute = _currentMinute.value
                val second = _currentSecond.value
                val currentMinuteAngle = (minute * 6f + second * 0.1f) - 90f
                var remainingSweepAngle = 0f
                
                // Check if we're crossing an hour boundary
                if (activity.endTimeMinutes / 60 != _currentHour.value) {
                    // Different hour boundary
                    val minutesUntilHourEnd = 60 - _currentMinute.value - (_currentSecond.value / 60f)
                    val remainingMinutesInNextHour = activity.endTimeMinutes % 60
                    
                    // Calculate total remaining sweep angle
                    remainingSweepAngle = (minutesUntilHourEnd + remainingMinutesInNextHour) * 6f
                } else {
                    // Same hour - calculate the exact angle to the end minute
                    val endMinutes = activity.endTimeMinutes % 60
                    val currentMinutesWithSeconds = _currentMinute.value + (_currentSecond.value / 60f)
                    
                    // Ensure we stop exactly at the end minute
                    remainingSweepAngle = (endMinutes - currentMinutesWithSeconds) * 6f
                }
                
                // Ensure the sweep angle is positive and doesn't exceed 360 degrees
                if (remainingSweepAngle < 0) {
                    remainingSweepAngle += 360f
                }
                
                // Calculate opacity - transition from 0.4 to 1.0 over 1 second
                val opacity = if (elapsedTime < transitionDuration) {
                    0.4f + (0.6f * (elapsedTime.toFloat() / transitionDuration.toFloat()))
                } else {
                    1.0f
                }
                
                MinuteArcSegment(
                    currentMinuteAngle, 
                    remainingSweepAngle, 
                    activity.minuteColor.copy(alpha = opacity),
                    transitionStartTime
                )
            } else {
                // Future activity - reduced opacity
                MinuteArcSegment(startMinuteAngle, sweepAngle, activity.minuteColor.copy(alpha = 0.4f))
            }
        }
    }
    
    // Check if current time is before the start time
    private fun isBeforeStartTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // Handle midnight crossing
        if (startMinutes > endMinutes) {
            // If current time is less than end time, it means we're in the morning part
            // of a range that crosses midnight
            if (currentMinutes < endMinutes) {
                return false
            }
            
            // If current time is less than start time, it means we're before the start time
            if (currentMinutes < startMinutes) {
                return true
            }
            
            // Otherwise we're between start time and midnight, so we're not before start time
            return false
        } 
        else {
            // Normal case, without crossing midnight
            return currentMinutes < startMinutes
        }
    }
    
    // Check if current time is after the end time
    private fun isAfterEndTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // Handle midnight crossing
        if (startMinutes > endMinutes) {
            // If current time is between midnight and end time, we're not after end time
            if (currentMinutes < endMinutes) {
                return false
            }
            
            // If current time is between start time and midnight, we're not after end time
            if (currentMinutes >= startMinutes) {
                return false
            }
            
            // Otherwise, we're after end time
            return true
        } else {
            // Normal case, without crossing midnight
            return currentMinutes > endMinutes
        }
    }
    
    // Check if current time is within a time range
    private fun isCurrentTimeInRange(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // If the range doesn't cross midnight
        if (startMinutes <= endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes
        } 
        // If the range crosses midnight
        else {
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
    
    // Set activity name while editing
    fun setActivityName(name: String) {
        _currentActivity.value = _currentActivity.value?.copy(name = name)
    }
} 