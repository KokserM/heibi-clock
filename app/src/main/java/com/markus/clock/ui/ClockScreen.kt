package com.markus.clock.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.markus.clock.R
import com.markus.clock.model.Activity
import com.markus.clock.model.Wallpapers
import com.markus.clock.ui.theme.PlayPrimary
import com.markus.clock.ui.theme.PlaySecondary
import com.markus.clock.ui.theme.SleepPrimary
import com.markus.clock.ui.theme.SleepSecondary
import com.markus.clock.viewmodel.TimerViewModel
import com.markus.clock.viewmodel.MinuteArcSegment
import com.markus.clock.viewmodel.HourArcSegment
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun ClockScreen(timerViewModel: TimerViewModel) {
    // Get the current state from the ViewModel
    val activities by timerViewModel.activities.collectAsState()
    val currentActivity by timerViewModel.currentActivity.collectAsState()
    val isTimerRunning by timerViewModel.isTimerRunning.collectAsState()
    val isAllActivitiesCompleted by timerViewModel.isAllActivitiesCompleted.collectAsState()
    val recentlyCompletedActivity by timerViewModel.recentlyCompletedActivity.collectAsState()
    
    // Get current time
    val hour by timerViewModel.currentHour.collectAsState()
    val minute by timerViewModel.currentMinute.collectAsState()
    val second by timerViewModel.currentSecond.collectAsState()
    
    // Clock hands
    val secondHandRotation = remember { Animatable(initialValue = 0f) }
    val previousSecond = remember { mutableStateOf(0) }
    var totalRotation = remember { mutableStateOf(0f) }
    
    // Zoom animation for end time reached
    val zoomScale = remember { Animatable(initialValue = 1f) }
    
    // Activity notification
    val showCompletionNotification = remember { mutableStateOf(false) }
    val completedActivityName = remember { mutableStateOf("") }
    
    // Start zoom animation when all activities are completed
    LaunchedEffect(isAllActivitiesCompleted) {
        if (isAllActivitiesCompleted) {
            // Run a repeating animation that zooms in and out
            repeat(3) {
                // Zoom in
                zoomScale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = LinearEasing
                    )
                )
                
                // Zoom out
                zoomScale.animateTo(
                    targetValue = 0.9f,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = LinearEasing
                    )
                )
            }
            
            // Return to normal size
            zoomScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearEasing
                )
            )
        } else {
            zoomScale.snapTo(1f)
        }
    }
    
    // Show notification when activity is completed
    LaunchedEffect(recentlyCompletedActivity) {
        if (recentlyCompletedActivity != null) {
            completedActivityName.value = recentlyCompletedActivity?.name ?: ""
            showCompletionNotification.value = true
            
            // Automatically hide notification after 3 seconds
            delay(3000)
            showCompletionNotification.value = false
            timerViewModel.clearCompletionNotification()
        }
    }
    
    // Update second hand animation when second changes
    LaunchedEffect(second) {
        // Handle 59->0 transition
        if (previousSecond.value == 59 && second == 0) {
            // Add a full rotation (360 degrees)
            totalRotation.value += 360f
        }
        
        // Calculate target angle based on seconds plus accumulated rotations
        val targetAngle = second * 6f + totalRotation.value
        
        // Animate to the target angle
        secondHandRotation.animateTo(
            targetValue = targetAngle,
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearEasing
            )
        )
        
        previousSecond.value = second
    }
    
    // Get angles for the clock hands
    val hourAngle = timerViewModel.getHourAngle()
    val minuteAngle = timerViewModel.getMinuteAngle()
    
    val secondAngle by animateFloatAsState(
        targetValue = secondHandRotation.value,
        animationSpec = tween(durationMillis = 100),
        label = "secondAngle"
    )
    
    // Get colors based on activity type
    val primaryColor = Color(0xFF4CAF50) // Default to green color
    val secondaryColor = Color(0xFFF44336) // Red color for stop button
    
    // Get selected wallpaper
    val wallpaperId by timerViewModel.selectedWallpaperId.collectAsState()
    val wallpaper = Wallpapers.getWallpaperById(wallpaperId)
    
    // Track dialogs state
    var showWallpaperSelector by remember { mutableStateOf(false) }
    var showActivityEditor by remember { mutableStateOf(false) }
    
    // Full screen container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        Image(
            painter = painterResource(id = wallpaper.resourceId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        
        // Activity completion notification
        AnimatedVisibility(
            visible = showCompletionNotification.value,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Activity completed: ${completedActivityName.value}",
                    color = Color.White
                )
            }
        }
        
        // Content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title with current time
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                if (!isTimerRunning) {
                    TextButton(
                        onClick = { showWallpaperSelector = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = stringResource(R.string.theme),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Analog Clock display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Analog Clock with hands and timer arc
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .offset(y = if (isTimerRunning) (-40).dp else 0.dp) // Only offset when timer is running
                ) {
                    // Apply the zoom scale animation
                    scale(zoomScale.value) {
                        // Add dark circular overlay to improve contrast with bright backgrounds
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.70f),
                            radius = size.minDimension / 2 + 40f
                        )
                        
                        // Draw clock face
                        drawClockFace()
                        
                        // Draw hour numbers
                        drawHourNumbers()
                        
                        // Draw arcs for activities if timer is running or activities exist
                        if (isTimerRunning || activities.isNotEmpty()) {
                            // Get hour arc segments
                            val hourSegments = timerViewModel.getHourArcSegments()
                            
                            // Draw hour arcs
                            if (hourSegments.isNotEmpty()) {
                                val clockRadius = size.minDimension / 2
                                val strokeWidth = 48f
                                
                                // Draw segments in reverse order so earlier activities appear on top
                                hourSegments.reversed().forEach { segment ->
                                    val hourArcSize = Size(
                                        (clockRadius - strokeWidth / 2) * 2,
                                        (clockRadius - strokeWidth / 2) * 2
                                    )
                                    val hourArcOffset = Offset(
                                        clockRadius - hourArcSize.width / 2,
                                        clockRadius - hourArcSize.height / 2
                                    )
                                    
                                    drawArc(
                                        color = segment.color,
                                        startAngle = segment.startAngle,
                                        sweepAngle = segment.sweepAngle,
                                        useCenter = false,
                                        size = hourArcSize,
                                        topLeft = hourArcOffset,
                                        alpha = 0.9f,
                                        style = Stroke(width = strokeWidth)
                                    )
                                }
                            }
                            
                            // Get minute arc segments
                            val minuteSegments = timerViewModel.getMinuteArcSegments()
                            
                            // Draw minute arcs
                            if (minuteSegments.isNotEmpty()) {
                                val minuteArcSize = Size(size.width * 0.8f, size.height * 0.8f)
                                val minuteArcOffset = Offset(
                                    (size.width - minuteArcSize.width) / 2f,
                                    (size.height - minuteArcSize.height) / 2f
                                )
                                
                                // Draw segments in reverse order so earlier activities appear on top
                                minuteSegments.reversed().forEach { segment ->
                                    drawArc(
                                        color = segment.color,
                                        startAngle = segment.startAngle,
                                        sweepAngle = segment.sweepAngle,
                                        useCenter = true,
                                        size = minuteArcSize,
                                        topLeft = minuteArcOffset,
                                        alpha = 0.8f
                                    )
                                }
                            }
                        }
                        
                        // Draw hour marks
                        drawHourMarks()
                        
                        // Draw hour hand
                        rotate(hourAngle) {
                            drawHourHand()
                        }
                        
                        // Draw minute hand
                        rotate(minuteAngle) {
                            drawMinuteHand()
                        }
                        
                        // Draw second hand
                        rotate(secondAngle) {
                            drawSecondHand(Color(0xFFFF1744))
                        }
                        
                        // Draw center pin
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = 10f,
                            center = Offset(center.x + 1f, center.y + 1f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 9f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFF1744),
                            radius = 6f,
                            center = center
                        )
                        
                        // If all activities completed, draw "TIME'S UP!" text
                        if (isAllActivitiesCompleted) {
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = size.minDimension / 6
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            
                            // Draw text in center of clock
                            drawContext.canvas.nativeCanvas.drawText(
                                "TIME'S UP!",
                                center.x,
                                center.y + textPaint.textSize / 3,
                                textPaint
                            )
                        }
                    }
                }
                
                // Current and upcoming activities info
                if (isTimerRunning && activities.isNotEmpty()) {
                    val currentTimeMinutes = hour * 60 + minute
                    val sortedActivities = activities.sortedBy { it.startTimeMinutes }
                    
                    // Find current activity (the one that's currently active)
                    val currentActivity = sortedActivities.firstOrNull { activity ->
                        currentTimeMinutes >= activity.startTimeMinutes && 
                        currentTimeMinutes < activity.endTimeMinutes
                    }
                    
                    // Find next activity (the one that starts after current activity ends)
                    val nextActivity = if (currentActivity != null) {
                        sortedActivities.firstOrNull { activity ->
                            activity.startTimeMinutes >= currentActivity.endTimeMinutes
                        }
                    } else {
                        // If no current activity, find the next upcoming activity
                        sortedActivities.firstOrNull { activity ->
                            activity.startTimeMinutes > currentTimeMinutes
                        }
                    }
                    
                    if (currentActivity != null || nextActivity != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Current activity
                            currentActivity?.let { activity ->
                                ActivityInfoCard(
                                    activity = activity,
                                    isCurrent = true,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // Next activity
                            nextActivity?.let { activity ->
                                ActivityInfoCard(
                                    activity = activity,
                                    isCurrent = false
                                )
                            }
                        }
                    }
                }
            }
            
            // Controls
            if (!isTimerRunning) {
                ActivitiesControlSection(
                    activities = activities,
                    onAddActivity = { timerViewModel.createNewActivity(); showActivityEditor = true },
                    onEditActivity = { timerViewModel.setCurrentActivity(it); showActivityEditor = true },
                    onDeleteActivity = { timerViewModel.deleteActivity(it) },
                    onStartClicked = { timerViewModel.startTimer() }
                )
            } else {
                if (isAllActivitiesCompleted) {
                    // Show "Time's Up!" button with simpler text
                    Button(
                        onClick = { timerViewModel.stopTimer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green color
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "TIME'S UP!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    // Regular stop button
                    Button(
                        onClick = { timerViewModel.stopTimer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336) // Red color for stop button
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = stringResource(R.string.stop))
                    }
                }
            }
        }
    }
    
    // Show ActivityEditor Dialog
    if (showActivityEditor && currentActivity != null) {
        ActivityEditorDialog(
            activity = currentActivity!!,
            onDismiss = { 
                timerViewModel.setCurrentActivity(null)
                showActivityEditor = false 
            },
            onSave = { name, startHour, startMinute, endHour, endMinute ->
                if (timerViewModel.isNewActivity()) {
                    // Create new activity
                    timerViewModel.createActivity(name, startHour, startMinute, endHour, endMinute)
                } else {
                    // Update existing activity
                    val updatedActivity = currentActivity!!.copy(
                        name = name,
                        startTimeMinutes = startHour * 60 + startMinute,
                        endTimeMinutes = endHour * 60 + endMinute
                    )
                    timerViewModel.updateActivity(updatedActivity)
                }
                showActivityEditor = false
            }
        )
    }
    
    if (showWallpaperSelector) {
        WallpaperSelectorDialog(
            onDismiss = { showWallpaperSelector = false },
            onSelect = { wallpaperId ->
                timerViewModel.setWallpaper(wallpaperId)
                showWallpaperSelector = false
            },
            selectedWallpaperId = wallpaperId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = timePickerState)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        onConfirm(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    }
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun WallpaperSelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    selectedWallpaperId: Int
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_wallpaper),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                Wallpapers.getWallpapers().forEach { wallpaper ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedWallpaperId == wallpaper.id) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else 
                                    Color.Transparent
                            )
                            .padding(8.dp)
                            .clickable { onSelect(wallpaper.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = wallpaper.resourceId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = wallpaper.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesControlSection(
    activities: List<Activity>,
    onAddActivity: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onStartClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Activities list with fixed height
        if (activities.isNotEmpty()) {
            Text(
                text = stringResource(R.string.scheduled_activities),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Fixed height container for activities list
            Box(
                modifier = Modifier
                    .height(200.dp) // Fixed height for the activities list
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activities) { activity ->
                        ActivityItem(
                            activity = activity,
                            onEdit = { onEditActivity(activity) },
                            onDelete = { onDeleteActivity(activity.id) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Add Activity button
        FloatingActionButton(
            onClick = onAddActivity,
            modifier = Modifier.padding(bottom = 16.dp),
            containerColor = Color(0xFF4CAF50)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Activity"
            )
        }
        
        // Start button
        Button(
            onClick = onStartClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green color for start button
            ),
            modifier = Modifier
                .fillMaxWidth(),
            enabled = activities.isNotEmpty()
        ) {
            Text(
                text = stringResource(R.string.start),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ActivityItem(
    activity: Activity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33FFFFFF)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Color indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color dots for hour and minute arcs
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(activity.hourColor, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(activity.minuteColor, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Activity name and time range
                Column {
                    Text(
                        text = activity.name.ifEmpty { stringResource(R.string.unnamed_activity) },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "${formatTimeForDisplay(activity.startTimeMinutes)} - ${formatTimeForDisplay(activity.endTimeMinutes)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Action buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Helper function to format time
private fun formatTimeForDisplay(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityEditorDialog(
    activity: Activity,
    onDismiss: () -> Unit,
    onSave: (name: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) {
    var name by remember { mutableStateOf(activity.name) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(activity.startTimeMinutes / 60) }
    var startMinute by remember { mutableStateOf(activity.startTimeMinutes % 60) }
    var endHour by remember { mutableStateOf(activity.endTimeMinutes / 60) }
    var endMinute by remember { mutableStateOf(activity.endTimeMinutes % 60) }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (activity.name.isEmpty()) stringResource(R.string.add_activity) else stringResource(R.string.edit_activity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.activity_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Time range row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.from),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    TextButton(
                        onClick = { showStartTimePicker = true }
                    ) {
                        Text(
                            text = String.format("%02d:%02d", startHour, startMinute),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Text(
                    text = "→",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                
                // End time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.to),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    TextButton(
                        onClick = { showEndTimePicker = true }
                    ) {
                        Text(
                            text = String.format("%02d:%02d", endHour, endMinute),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                
                TextButton(
                    onClick = {
                        onSave(
                            name,
                            startHour,
                            startMinute,
                            endHour,
                            endMinute
                        )
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
    
    // Start time picker dialog
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startHour = hour
                startMinute = minute
                showStartTimePicker = false
            }
        )
    }
    
    // End time picker dialog
    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endHour = hour
                endMinute = minute
                showEndTimePicker = false
            }
        )
    }
}

private fun DrawScope.drawClockFace() {
    // Draw outer circle
    drawCircle(
        color = Color.White,
        radius = size.minDimension / 2,
        style = Stroke(width = 8f)
    )
    
    // Draw inner circle (clock face)
    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        radius = size.minDimension / 2 - 4f
    )
}

private fun DrawScope.drawHourNumbers() {
    val radius = size.minDimension / 2 - 40f // Position numbers inside the clock edge
    val centerX = center.x
    val centerY = center.y
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f // Adjust size as needed
        textAlign = android.graphics.Paint.Align.CENTER // Center the text on the point
    }

    for (i in 1..12) {
        val angle = Math.toRadians(((i * 30) - 90).toDouble()) // -90 to start at 12 o'clock
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()

        // Draw the hour number as text
        drawContext.canvas.nativeCanvas.drawText(
            i.toString(),
            x,
            y + textPaint.textSize / 3, // Adjust vertical alignment
            textPaint
        )

    }
}

private fun DrawScope.drawHourMarks() {
    val radius = size.minDimension / 2
    val centerX = center.x
    val centerY = center.y
    
    // Draw hour marks
    for (i in 0 until 12) {
        val angle = Math.toRadians((i * 30).toDouble())
        val startX = centerX + (radius - 15) * cos(angle).toFloat()
        val startY = centerY + (radius - 15) * sin(angle).toFloat()
        val endX = centerX + radius * cos(angle).toFloat()
        val endY = centerY + radius * sin(angle).toFloat()
        
        // Draw a glow effect for major hour marks (12, 3, 6, 9)
        if (i % 3 == 0) {
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        
        drawLine(
            color = Color.White,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (i % 3 == 0) 5f else 3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawHourHand() {
    val length = size.minDimension * 0.3f
    
    // Draw shadow for depth effect
    drawLine(
        color = Color.Black.copy(alpha = 0.5f),
        start = Offset(center.x + 3f, center.y + 3f),
        end = Offset(center.x + 3f, center.y - length + 3f),
        strokeWidth = 14f,
        cap = StrokeCap.Round
    )
    
    // Draw hour hand with more vibrant gradient
    val hourHandGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFC107), Color(0xFFFF6F00), Color(0xFFE65100)),
        start = center,
        end = Offset(center.x, center.y - length)
    )
    
    drawLine(
        brush = hourHandGradient,
        start = center,
        end = Offset(center.x, center.y - length),
        strokeWidth = 14f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMinuteHand() {
    val length = size.minDimension * 0.4f
    
    // Draw shadow for depth effect
    drawLine(
        color = Color.Black.copy(alpha = 0.5f),
        start = Offset(center.x + 3f, center.y + 3f),
        end = Offset(center.x + 3f, center.y - length + 3f),
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
    
    // Draw minute hand with more vibrant gradient
    val minuteHandGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF64B5F6), Color(0xFF2196F3), Color(0xFF0D47A1)),
        start = center,
        end = Offset(center.x, center.y - length)
    )
    
    drawLine(
        brush = minuteHandGradient,
        start = center,
        end = Offset(center.x, center.y - length),
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSecondHand(accentColor: Color) {
    val length = size.minDimension * 0.45f
    
    // Draw shadow for depth effect
    drawLine(
        color = Color.Black.copy(alpha = 0.3f),
        start = Offset(center.x + 1f, center.y + 1f),
        end = Offset(center.x + 1f, center.y - length + 1f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    
    // Draw second hand with bright color
    drawLine(
        color = Color(0xFFFF1744),  // Bright red
        start = center,
        end = Offset(center.x, center.y - length),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    
    // Add a small circle at the base of the second hand
    drawCircle(
        color = Color(0xFFFF1744),
        radius = 6f,
        center = center
    )
}

@Composable
private fun ActivityInfoCard(
    activity: Activity,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33FFFFFF)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Activity info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isCurrent) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Activity details
                Column {
                    Text(
                        text = if (isCurrent) stringResource(R.string.current_activity) else stringResource(R.string.next_activity),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = activity.name.ifEmpty { stringResource(R.string.unnamed_activity) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "${formatTimeForDisplay(activity.startTimeMinutes)} - ${formatTimeForDisplay(activity.endTimeMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Color indicators
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(activity.hourColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(activity.minuteColor, RoundedCornerShape(4.dp))
                )
            }
        }
    }
} 