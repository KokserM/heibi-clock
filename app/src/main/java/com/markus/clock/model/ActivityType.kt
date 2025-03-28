package com.markus.clock.model

import androidx.compose.ui.graphics.Color

// Activity data class to replace the enum
data class Activity(
    val id: String,
    val name: String,
    val hourColor: Color,
    val minuteColor: Color,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val isCompleted: Boolean = false
) 