package com.markus.clock.service

import android.app.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.markus.clock.R
import com.markus.clock.model.Activity
import com.markus.clock.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class TimerService : Service() {
    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var activities: List<Activity> = emptyList()
    private var isTimerRunning: Boolean = false
    private var currentHour: Int = 0
    private var currentMinute: Int = 0
    private var currentSecond: Int = 0
    private var recentlyCompletedActivity: Activity? = null
    private var isAllActivitiesCompleted: Boolean = false

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startClock()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText("Your activities are being tracked in the background.")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startClock() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                updateCurrentTime()
                if (isTimerRunning) {
                    updateActivitiesProgress()
                    updateNotification()
                }
                delay(100)
            }
        }
    }

    private fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)
        currentSecond = calendar.get(Calendar.SECOND)
    }

    private fun updateActivitiesProgress() {
        val currentMinutes = currentHour * 60 + currentMinute + (currentSecond / 60f)
        var allCompleted = true
        var anyNewlyCompleted = false

        val updatedActivities = activities.map { activity ->
            val isCompleted = isAfterEndTime(currentMinutes, activity.startTimeMinutes, activity.endTimeMinutes)
            if (isCompleted && !activity.isCompleted) {
                anyNewlyCompleted = true
                recentlyCompletedActivity = activity
            }
            if (!isCompleted) {
                allCompleted = false
            }
            activity.copy(isCompleted = isCompleted)
        }

        if (anyNewlyCompleted) {
            activities = updatedActivities
        }
        isAllActivitiesCompleted = allCompleted && activities.isNotEmpty()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun isAfterEndTime(currentMinutes: Float, startMinutes: Int, endMinutes: Int): Boolean {
        return if (startMinutes > endMinutes) {
            currentMinutes < endMinutes || currentMinutes >= startMinutes
        } else {
            currentMinutes > endMinutes
        }
    }

    fun setActivities(newActivities: List<Activity>) {
        activities = newActivities
    }

    fun startTimer() {
        isTimerRunning = true
        activities = activities.map { it.copy(isCompleted = false) }
        isAllActivitiesCompleted = false
        recentlyCompletedActivity = null
    }

    fun stopTimer() {
        isTimerRunning = false
        isAllActivitiesCompleted = false
        recentlyCompletedActivity = null
    }

    fun getActivities(): List<Activity> = activities
    fun isTimerRunning(): Boolean = isTimerRunning
    fun getCurrentHour(): Int = currentHour
    fun getCurrentMinute(): Int = currentMinute
    fun getCurrentSecond(): Int = currentSecond
    fun getRecentlyCompletedActivity(): Activity? = recentlyCompletedActivity
    fun isAllActivitiesCompleted(): Boolean = isAllActivitiesCompleted

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }
} 