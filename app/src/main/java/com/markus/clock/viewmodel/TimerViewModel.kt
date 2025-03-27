package com.markus.clock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markus.clock.model.ActivityType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

// Data class for minute arc segments
data class MinuteArcSegment(
    val startAngle: Float,
    val sweepAngle: Float
)

// Data class for hour arc segments
data class HourArcSegment(
    val startAngle: Float,
    val sweepAngle: Float
)

class TimerViewModel : ViewModel() {
    
    // UI state
    private val _activityType = MutableStateFlow(ActivityType.PLAY)
    val activityType: StateFlow<ActivityType> = _activityType.asStateFlow()
    
    // Wallpaper selection (default to first wallpaper)
    private val _selectedWallpaperId = MutableStateFlow(1)
    val selectedWallpaperId: StateFlow<Int> = _selectedWallpaperId.asStateFlow()
    
    // Timer end state
    private val _isEndTimeReached = MutableStateFlow(false)
    val isEndTimeReached: StateFlow<Boolean> = _isEndTimeReached.asStateFlow()
    
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
    
    // Start and end time in minutes (from 00:00)
    private val _startTimeMinutes = MutableStateFlow(180) // Default to 3:00 PM
    val startTimeMinutes: StateFlow<Int> = _startTimeMinutes.asStateFlow()
    
    private val _endTimeMinutes = MutableStateFlow(265) // Default to 4:25 PM
    val endTimeMinutes: StateFlow<Int> = _endTimeMinutes.asStateFlow()
    
    // Progress indicator (for the arc)
    private val _progress = MutableStateFlow(1f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
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
        
        // If timer is running, update the progress
        if (_isTimerRunning.value) {
            updateProgress()
        }
    }
    
    fun setActivityType(type: ActivityType) {
        _activityType.value = type
    }
    
    fun setWallpaper(wallpaperId: Int) {
        _selectedWallpaperId.value = wallpaperId
    }
    
    fun setStartTime(hour: Int, minute: Int) {
        _startTimeMinutes.value = hour * 60 + minute
    }
    
    fun setEndTime(hour: Int, minute: Int) {
        _endTimeMinutes.value = hour * 60 + minute
    }
    
    private fun updateProgress() {
        val start = _startTimeMinutes.value
        val end = _endTimeMinutes.value
        val current = _currentHour.value * 60 + _currentMinute.value + (_currentSecond.value / 60f)
        
        // Calculate progress based on current time relative to start and end times
        val totalDuration = calculateDuration(start, end)
        val elapsed = calculateElapsed(start, current)
        
        // Edge cases: before start time or after end time
        when {
            isBeforeStartTime(current, start, end) -> _progress.value = 1f
            isAfterEndTime(current, start, end) -> {
                _progress.value = 0f
                _isEndTimeReached.value = true
            }
            else -> _progress.value = 1f - (elapsed / totalDuration)
        }
    }
    
    private fun isBeforeStartTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // Add debug log
        println("Checking if before start time: current=$currentMinutes, start=$startMinutes, end=$endMinutes")
        
        // Extract hour components for better analysis
        val currentHour = (currentMinutes / 60).toInt()
        val startHour = startMinutes / 60
        val endHour = endMinutes / 60
        
        // Special handling for time ranges that cross midnight
        if (startMinutes > endMinutes) {
            // If the range crosses midnight (e.g., 23:00 to 01:00)
            // We need to check if we're before the start time but after midnight
            
            // If current time is less than end time, it means we're in the morning part
            // of a range that crosses midnight
            if (currentMinutes < endMinutes) {
                // We're already in the next day, so we're not before the start time
                println("Current time is in next day morning, not before start time")
                return false
            }
            
            // If current time is less than start time, it means we're before the start time
            if (currentMinutes < startMinutes) {
                println("Current time is before start time (crosses midnight case)")
                return true
            }
            
            // Otherwise we're between start time and midnight, so we're not before start time
            println("Current time is between start time and midnight, not before start time")
            return false
        } 
        else {
            // Normal case, without crossing midnight
            val result = currentMinutes < startMinutes
            println("Normal case: current time ${if (result) "is" else "is not"} before start time")
            return result
        }
    }
    
    private fun isAfterEndTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // If range is within the same day (or crosses midnight)
        if (startMinutes <= endMinutes || startMinutes > endMinutes) {
            // If current time is in the evening (after 6PM) and end time is in the morning (before 6AM)
            // then we're dealing with tomorrow's morning
            val currentHour = (currentMinutes / 60).toInt()
            val endHour = endMinutes / 60
            
            if (currentHour >= 18 && endHour < 6) {
                // Current time is evening, end time is tomorrow morning
                return false // We're not after tomorrow's end time
            }
            
            // Normal case within the same day
            return if (startMinutes <= endMinutes) {
                currentMinutes > endMinutes
            } else {
                // Crosses midnight
                currentMinutes > endMinutes && currentMinutes < startMinutes
            }
        }
        return false
    }
    
    private fun calculateDuration(startMinutes: Int, endMinutes: Int): Float {
        // Handle time wrapping around midnight
        return if (endMinutes >= startMinutes) {
            (endMinutes - startMinutes).toFloat()
        } else {
            ((24 * 60) - startMinutes + endMinutes).toFloat()
        }
    }
    
    private fun calculateElapsed(startMinutes: Int, currentMinutes: Float): Float {
        // Handle time wrapping around midnight
        return if (currentMinutes >= startMinutes) {
            currentMinutes - startMinutes
        } else {
            ((24 * 60) - startMinutes + currentMinutes)
        }
    }
    
    fun startTimer() {
        if (_isTimerRunning.value) {
            stopTimer()
        }
        
        _isTimerRunning.value = true
        updateProgress()
    }
    
    fun stopTimer() {
        _isTimerRunning.value = false
        _isEndTimeReached.value = false
    }
    
    fun formatStartTime(): String {
        return formatTimeMinutes(_startTimeMinutes.value)
    }
    
    fun formatEndTime(): String {
        return formatTimeMinutes(_endTimeMinutes.value)
    }
    
    private fun formatTimeMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
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
        return (minute * 6f) + (second * 0.1f) // 6 degrees per minute, 0.1 degrees per second
    }
    
    fun getSecondAngle(): Float {
        return _currentSecond.value * 6f // 6 degrees per second
    }
    
    // Functions for the arcs
    fun getStartAngle(): Float {
        // If we're not running, return current angle
        if (!_isTimerRunning.value) {
            return getCurrentAngle()
        }
        
        // Return the current hour hand position
        return getCurrentAngle()
    }
    
    fun getEndAngle(): Float {
        val endHour = _endTimeMinutes.value / 60 % 12
        val endMinute = _endTimeMinutes.value % 60
        return (endHour * 30f) + (endMinute * 0.5f)
    }
    
    // Calculate the current time position in degrees (for hour arc)
    private fun calculateCurrentAngle(): Float {
        val currentHour = _currentHour.value % 12
        val currentMinute = _currentMinute.value
        val currentSecond = _currentSecond.value
        
        return (currentHour * 30f) + (currentMinute * 0.5f) + (currentSecond * 0.5f / 60f)
    }
    
    // Make this public so the UI can access it
    fun getCurrentAngle(): Float = calculateCurrentAngle()
    
    // Calculate the current minute position in degrees
    private fun calculateCurrentAngleMinutes(): Float {
        return (_currentMinute.value * 6f) + (_currentSecond.value * 0.1f)
    }
    
    // Make this public so the UI can access it
    fun getCurrentAngleMinutes(): Float = calculateCurrentAngleMinutes()
    
    // Check if current time is within the time range, properly handling midnight crossing
    private fun isCurrentTimeInRange(): Boolean {
        val currentMinutes = _currentHour.value * 60 + _currentMinute.value + (_currentSecond.value / 60f)
        val startMinutes = _startTimeMinutes.value
        val endMinutes = _endTimeMinutes.value
        
        // If the range doesn't cross midnight
        if (startMinutes <= endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes
        } 
        // If the range crosses midnight
        else {
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
    
    // Special case for midnight hour (12AM/hour 0)
    private fun normalizeHour(hour: Int): Int {
        return if (hour == 0) 12 else hour
    }
    
    // Special case for times within the same hour as current time
    private fun isWithinCurrentHourRange(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        val currentHour = (currentMinutes / 60).toInt()
        val startHour = startMinutes / 60
        val endHour = endMinutes / 60
        
        return currentHour == startHour && currentHour == endHour &&
               currentMinutes >= startMinutes && currentMinutes <= endMinutes
    }
    
    // Get the sweep angle for the hour arc - making sure it diminishes as time passes
    fun getSweepAngle(): Float {
        // If timer isn't running, return 0
        if (!_isTimerRunning.value) {
            return 0f
        }
        
        // Get current time components
        val currentHour = _currentHour.value
        val currentMinute = _currentMinute.value
        val currentSecond = _currentSecond.value
        
        // Start and end time components
        val startHour = _startTimeMinutes.value / 60
        val startMinute = _startTimeMinutes.value % 60
        val endHour = _endTimeMinutes.value / 60
        val endMinute = _endTimeMinutes.value % 60
        
        // Current and target times in minutes from day start
        val currentTimeMinutes = currentHour * 60 + currentMinute + (currentSecond / 60f)
        val startTimeMinutes = _startTimeMinutes.value
        val endTimeMinutes = _endTimeMinutes.value
        
        // If timer is for future time (current time is before start time)
        // Calculate full sweep angle from start to end
        if (isBeforeStartTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            // Hour positions including minutes
            val startHourPosition = startHour % 12 + (startMinute / 60f)
            val endHourPosition = endHour % 12 + (endMinute / 60f)
            
            // Calculate sweep angle, handling midnight crossing
            return if (endHourPosition < startHourPosition) {
                // Crosses midnight - full circle plus the portion from 0 to end
                (360f - (startHourPosition * 30f)) + (endHourPosition * 30f)
            } else {
                // Normal case - difference between angles
                (endHourPosition - startHourPosition) * 30f
            }
        }
        
        // If timer has ended
        if (isAfterEndTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            return 0f // Timer finished - no arc
        }
        
        // Calculate end angle in degrees (for the hour arc)
        val endAngle = getEndAngle()
        
        // Get current position of hour hand in degrees
        val currentAngle = getCurrentAngle()
        
        // Normalize angles to handle cases around midnight (0/360 degrees)
        val normalizedEndAngle = if (endAngle < currentAngle) endAngle + 360f else endAngle
        
        // Calculate the sweep angle
        val sweepAngle = normalizedEndAngle - currentAngle
        
        // Safety check - if sweep angle is too large (over 300 degrees), 
        // there might be an error in calculation - cap it at 300 degrees
        return if (sweepAngle > 300f) {
            300f
        } else {
            sweepAngle
        }
    }
    
    // Function to check if a time is in the future
    private fun isFutureTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        // Check if both start and end times are in the future
        return currentMinutes < startMinutes
    }
    
    // Functions for the minute arc
    fun getStartAngleMinutes(): Float {
        return (_startTimeMinutes.value % 60) * 6f
    }
    
    fun getEndAngleMinutes(): Float {
        return (_endTimeMinutes.value % 60) * 6f
    }
    
    // Get the sweep angle for the minute arc
    fun getSweepAngleMinutes(): Float {
        // This function is now deprecated in favor of the segment-based approach
        // Return a default value that won't affect our UI
        return 0f
    }
    
    fun getInitialMinuteArcSegments(): List<MinuteArcSegment> {
        // Modified condition: Only check if values are set, not whether timer is running
        // This allows us to use this function for future time preview even when timer is running
        if (_startTimeMinutes.value <= 0 || _endTimeMinutes.value <= 0) {
            println("Initial minute arc segments: values not set, returning empty list")
            return emptyList()
        }
        
        println("Creating initial minute arc segments for preview")
        val segments = mutableListOf<MinuteArcSegment>()
        
        // Start and end time components
        val startHour = _startTimeMinutes.value / 60
        val startMinute = _startTimeMinutes.value % 60
        val endHour = _endTimeMinutes.value / 60
        val endMinute = _endTimeMinutes.value % 60
        
        println("Processing time range: $startHour:$startMinute to $endHour:$endMinute")
        
        // Case 1: Same hour range (e.g., 10:05 to 10:45)
        if (startHour == endHour) {
            // Start minute position
            val startMinuteAngle = (startMinute * 6f) - 90f
            // End minute position
            val endMinuteAngle = (endMinute * 6f) - 90f
            
            // Create a single continuous wedge from start to end
            val sweepAngle = if (endMinute > startMinute) {
                endMinuteAngle - startMinuteAngle
            } else {
                // Handle case where end is before start (crossing midnight)
                (360f - startMinuteAngle) + endMinuteAngle
            }
            
            segments.add(MinuteArcSegment(
                startAngle = startMinuteAngle,
                sweepAngle = sweepAngle
            ))
            
            println("Created single continuous wedge for same hour: startAngle=$startMinuteAngle, sweepAngle=$sweepAngle")
            return segments
        }
        
        // Case 2: Cross-hour boundary (e.g., 8:40 to 9:15)
        println("Handling multi-hour boundary: $startHour:$startMinute to $endHour:$endMinute")
        
        // First segment: From start minute to end of first hour
        val startMinuteAngle = (startMinute * 6f) - 90f
        val endOfFirstHourAngle = (60 * 6f) - 90f  // End of the first hour is at 60 minutes
        
        // Calculate the sweep angle for the first hour segment
        val firstHourSweepAngle = endOfFirstHourAngle - startMinuteAngle
        
        // Add first hour segment
        segments.add(MinuteArcSegment(
            startAngle = startMinuteAngle,
            sweepAngle = firstHourSweepAngle
        ))
        println("Created first hour segment: startAngle=$startMinuteAngle, sweepAngle=$firstHourSweepAngle")
        
        // Calculate the hour difference - this is critical for multiple hour spans
        val hourDifference = if (endHour >= startHour) {
            endHour - startHour
        } else {
            // Handle crossing midnight
            (24 - startHour) + endHour
        }
        
        println("Hour difference: $hourDifference")
        
        // For intermediate hours (if any), add full circles
        if (hourDifference > 1) {
            for (i in 1 until hourDifference) {
                // Current intermediate hour in the sequence
                val intermediateHour = (startHour + i) % 24
                
                segments.add(MinuteArcSegment(
                    startAngle = -90f,  // Start at 12 o'clock
                    sweepAngle = 360f   // Full circle
                ))
                println("Created intermediate hour segment for hour $intermediateHour: full circle")
            }
        }
        
        // Always add the end hour segment, this is crucial for cross-hour boundary cases
        // If endMinute is 0, we still want to show a small segment to indicate the end point
        val endHourSweepAngle = if (endMinute == 0) {
            0.1f  // Tiny arc to show the end point when it's exactly on the hour
        } else {
            (endMinute * 6f)  // Normal sweep from 12 o'clock to endMinute
        }
        
        segments.add(MinuteArcSegment(
            startAngle = -90f,  // Start at 12 o'clock (top of the hour)
            sweepAngle = endHourSweepAngle
        ))
        
        println("Created end hour segment: startAngle=-90f, sweepAngle=$endHourSweepAngle, endMinute=$endMinute")
        println("Total initial minute segments created: ${segments.size}")
        return segments
    }
    
    // Get the hour arc for the current time
    fun getHourArcSegment(): HourArcSegment? {
        // If timer isn't running, return null
        if (!_isTimerRunning.value) {
            return null
        }
        
        // Get current time components
        val currentHour = _currentHour.value
        val currentMinute = _currentMinute.value
        val currentSecond = _currentSecond.value
        
        // Current and target times in minutes from day start
        val currentTimeMinutes = currentHour * 60 + currentMinute + (currentSecond / 60f)
        val startTimeMinutes = _startTimeMinutes.value
        val endTimeMinutes = _endTimeMinutes.value
        
        // If timer is for future time (current time is before start time)
        // Show preview arc instead
        if (isBeforeStartTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            println("Future time detected while timer running - showing preview hour arc")
            
            // Use the same logic as getInitialHourArcSegment
            // Calculate start and end positions
            val startHour = startTimeMinutes / 60
            val startMinute = startTimeMinutes % 60
            val endHour = endTimeMinutes / 60
            val endMinute = endTimeMinutes % 60
            
            // Hour positions including minutes
            val startHourPosition = startHour % 12 + (startMinute / 60f)
            val endHourPosition = endHour % 12 + (endMinute / 60f)
            
            // Calculate angles
            val startAngle = (startHourPosition * 30f) - 90f
            val endAngle = (endHourPosition * 30f) - 90f
            
            // Calculate sweep angle, handling midnight crossing
            val sweepAngle = if (endHourPosition < startHourPosition) {
                (360f - startAngle) + endAngle // Crosses midnight
            } else {
                endAngle - startAngle
            }
            
            return HourArcSegment(startAngle, sweepAngle)
        }
        
        // If timer has ended
        if (isAfterEndTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            return null
        }
        
        // Normal case - timer is running and we're within the time range
        
        // Get current hour angle (accounting for position within the hour)
        val hourPosition = currentHour % 12 + (currentMinute / 60f) + (currentSecond / 3600f)
        val currentAngle = (hourPosition * 30f) - 90f
        
        // Get end hour angle
        val endHour = endTimeMinutes / 60
        val endMinute = endTimeMinutes % 60
        val endHourPosition = endHour % 12 + (endMinute / 60f)
        val endAngle = (endHourPosition * 30f) - 90f
        
        // Normalize angles to ensure correct arc calculation
        val normalizedEndAngle = if (endAngle < currentAngle) {
            endAngle + 360f
        } else {
            endAngle
        }
        
        // Calculate sweep angle from current to end
        val sweepAngle = normalizedEndAngle - currentAngle
        
        // Safety check - cap maximum sweep angle
        val finalSweepAngle = if (sweepAngle > 300f) 300f else sweepAngle
        
        return HourArcSegment(currentAngle, finalSweepAngle)
    }
    
    // Get the initial hour arc preview (before timer starts)
    fun getInitialHourArcSegment(): HourArcSegment? {
        // If timer is running or the values aren't set, return null
        if (_isTimerRunning.value || _startTimeMinutes.value <= 0 || _endTimeMinutes.value <= 0) {
            return null
        }
        
        // Calculate start and end positions
        val startHour = _startTimeMinutes.value / 60
        val startMinute = _startTimeMinutes.value % 60
        val endHour = _endTimeMinutes.value / 60
        val endMinute = _endTimeMinutes.value % 60
        
        // Hour positions including minutes
        val startHourPosition = startHour % 12 + (startMinute / 60f)
        val endHourPosition = endHour % 12 + (endMinute / 60f)
        
        // Calculate angles
        val startAngle = (startHourPosition * 30f) - 90f
        val endAngle = (endHourPosition * 30f) - 90f
        
        // Calculate sweep angle, handling midnight crossing
        val sweepAngle = if (endHourPosition < startHourPosition) {
            (360f - startAngle) + endAngle // Crosses midnight
        } else {
            endAngle - startAngle
        }
        
        return HourArcSegment(startAngle, sweepAngle)
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        clockJob?.cancel()
    }
    
    private fun isInEndHourOfRange(): Boolean {
        val currentHour = _currentHour.value
        val endHour = _endTimeMinutes.value / 60
        return currentHour == endHour
    }
    
    // Get the minute arc components for drawing
    fun getMinuteArcSegments(): List<MinuteArcSegment> {
        // If timer isn't running, return empty list
        if (!_isTimerRunning.value) {
            println("Timer not running, returning empty minute segments list")
            return emptyList()
        }
        
        // Current time components
        val currentHour = _currentHour.value
        val currentMinute = _currentMinute.value
        val currentSecond = _currentSecond.value
        
        // Start and end time components
        val startHour = _startTimeMinutes.value / 60
        val startMinute = _startTimeMinutes.value % 60
        val endHour = _endTimeMinutes.value / 60
        val endMinute = _endTimeMinutes.value % 60
        
        // Current time in minutes from day start
        val currentTimeMinutes = currentHour * 60 + currentMinute + (currentSecond / 60f)
        val startTimeMinutes = _startTimeMinutes.value
        val endTimeMinutes = _endTimeMinutes.value
        
        println("Current time: $currentHour:$currentMinute:$currentSecond")
        println("Time range: $startHour:$startMinute to $endHour:$endMinute")
        
        // Initialize segments list
        val segments = mutableListOf<MinuteArcSegment>()
        
        // If timer is for future time (current time is before start time)
        // Show preview arcs instead
        if (isBeforeStartTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            println("Future time detected in getMinuteArcSegments - showing preview arcs")
            return getInitialMinuteArcSegments()
        }
        
        // If timer has ended
        if (isAfterEndTime(currentTimeMinutes, startTimeMinutes, endTimeMinutes)) {
            println("Timer has ended, returning empty list")
            return emptyList()
        }
        
        // Calculate the hour difference - this is critical for multiple hour spans
        val hourDifference = if (endHour >= startHour) {
            endHour - startHour
        } else {
            // Handle crossing midnight
            (24 - startHour) + endHour
        }
        
        println("Hour difference: $hourDifference")
        
        // Normal case - timer is running and we're within the time range
        
        // Case 1: Same hour range (e.g., 10:05 to 10:45)
        if (startHour == endHour) {
            if (currentHour == startHour && currentMinute >= startMinute && currentMinute < endMinute) {
                // Current minute position (with seconds precision)
                val currentMinuteAngle = (currentMinute * 6f) + (currentSecond * 0.1f) - 90f
                // End minute position
                val endMinuteAngle = (endMinute * 6f) - 90f
                
                // Create one continuous wedge from current minute to end minute
                segments.add(MinuteArcSegment(
                    startAngle = currentMinuteAngle,
                    sweepAngle = endMinuteAngle - currentMinuteAngle
                ))
                println("Created single continuous wedge for same hour: ${currentMinute} to ${endMinute}")
            }
            return segments
        }
        
        // Case 2: Cross-hour boundary (e.g., 9:30 to 10:45)
        
        // If we're in the start hour
        if (currentHour == startHour && currentMinute >= startMinute) {
            // We're in the start hour and past the start minute
            val currentMinuteAngle = (currentMinute * 6f) + (currentSecond * 0.1f) - 90f
            val endOfHourAngle = (60 * 6f) - 90f
            
            // Create one continuous wedge from current minute to end of hour
            segments.add(MinuteArcSegment(
                startAngle = currentMinuteAngle,
                sweepAngle = endOfHourAngle - currentMinuteAngle
            ))
            println("Created first segment for start hour: ${currentMinute} to 60")
            
            // Always add future hours' segments if we're in the start hour
            // This is critical for cases like 12:00 to 13:25
            if (hourDifference >= 1) {
                // Add the end hour segment to show the remaining wedge
                if (endMinute > 0) {
                    segments.add(MinuteArcSegment(
                        startAngle = -90f,  // Start at 12 o'clock (top of the hour)
                        sweepAngle = (endMinute * 6f)  // From 12 o'clock to end minute
                    ))
                    println("Added future end hour segment (hour ${endHour}): 0 to ${endMinute}")
                }
            }
        }
        // If we're in an intermediate hour (between start and end)
        else if (currentHour > startHour && currentHour < endHour) {
            // Show the remaining arc in the current hour
            val currentMinuteAngle = (currentMinute * 6f) + (currentSecond * 0.1f) - 90f
            val endOfHourAngle = (60 * 6f) - 90f
            
            // Create one continuous wedge from current minute to end of hour
            segments.add(MinuteArcSegment(
                startAngle = currentMinuteAngle,
                sweepAngle = endOfHourAngle - currentMinuteAngle
            ))
            println("Created segment for intermediate hour ${currentHour}: ${currentMinute} to 60")
            
            // Add any remaining future hours
            if (endHour > currentHour) {
                // Add the end hour segment
                if (endMinute > 0) {
                    segments.add(MinuteArcSegment(
                        startAngle = -90f,  // Start at 12 o'clock (top of the hour)
                        sweepAngle = (endMinute * 6f)  // From 12 o'clock to end minute
                    ))
                    println("Added future end hour segment from intermediate hour: 0 to ${endMinute}")
                }
            }
        }
        // If we're in the end hour with end minute > 0
        else if (currentHour == endHour && endMinute > 0) {
            // Only create a segment if we're before the end minute
            if (currentMinute < endMinute) {
                // We're in the end hour but before the end minute
                // Create one continuous wedge from current minute to end minute
                val currentMinuteAngle = (currentMinute * 6f) + (currentSecond * 0.1f) - 90f
                val endMinuteAngle = (endMinute * 6f) - 90f
                
                segments.add(MinuteArcSegment(
                    startAngle = currentMinuteAngle,
                    sweepAngle = endMinuteAngle - currentMinuteAngle
                ))
                println("Created continuous wedge for end hour ${endHour}: ${currentMinute} to ${endMinute}")
            }
        }
        
        // Special case: We're in the hour before the end hour and need to display the next hour segment
        // This is critical for showing the wedge from 13:00 to 13:25 when current time is in hour 12
        if (currentHour == endHour - 1 && currentMinute >= startMinute) {
            // Always add the end hour segment to show the remaining wedge in the next hour
            if (endMinute > 0) {
                // Check if we already added this segment in the code above
                val alreadyAdded = segments.any { segment ->
                    segment.startAngle == -90f && segment.sweepAngle == (endMinute * 6f)
                }
                
                if (!alreadyAdded) {
                    segments.add(MinuteArcSegment(
                        startAngle = -90f,  // Start at 12 o'clock (top of the hour)
                        sweepAngle = (endMinute * 6f)  // From 12 o'clock to end minute
                    ))
                    println("Added next hour segment (hour ${endHour}): 0 to ${endMinute}")
                }
            } else {
                // If end minute is 0, show a tiny wedge at exactly 12 o'clock
                segments.add(MinuteArcSegment(
                    startAngle = -90f,  // 12 o'clock position
                    sweepAngle = 0.1f   // Tiny angle just to show something
                ))
                println("Added tiny arc for exact end time at hour ${endHour} (endMinute = 0)")
            }
        }
        
        println("Created total of ${segments.size} segments")
        return segments
    }
} 