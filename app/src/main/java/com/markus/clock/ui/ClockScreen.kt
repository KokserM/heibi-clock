package com.markus.clock.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.markus.clock.model.ActivityType
import com.markus.clock.model.Wallpapers
import com.markus.clock.ui.theme.PlayPrimary
import com.markus.clock.ui.theme.PlaySecondary
import com.markus.clock.ui.theme.SleepPrimary
import com.markus.clock.ui.theme.SleepSecondary
import com.markus.clock.viewmodel.TimerViewModel
import com.markus.clock.viewmodel.MinuteArcSegment
import com.markus.clock.viewmodel.HourArcSegment
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun ClockScreen(timerViewModel: TimerViewModel) {
    // Get the current state from the ViewModel
    val activityType by timerViewModel.activityType.collectAsState()
    val isTimerRunning by timerViewModel.isTimerRunning.collectAsState()
    val progress by timerViewModel.progress.collectAsState()
    val isEndTimeReached by timerViewModel.isEndTimeReached.collectAsState()
    
    // Get current time
    val hour by timerViewModel.currentHour.collectAsState()
    val minute by timerViewModel.currentMinute.collectAsState()
    val second by timerViewModel.currentSecond.collectAsState()
    
    // Get timer time ranges
    val startTimeMinutes by timerViewModel.startTimeMinutes.collectAsState()
    val endTimeMinutes by timerViewModel.endTimeMinutes.collectAsState()
    
    // Calculate start and end times for display
    val startHour = startTimeMinutes / 60
    val startMinute = startTimeMinutes % 60
    val endHour = endTimeMinutes / 60
    val endMinute = endTimeMinutes % 60
    
    // Clock hands
    val secondHandRotation = remember { Animatable(initialValue = 0f) }
    val previousSecond = remember { mutableStateOf(0) }
    var totalRotation = remember { mutableStateOf(0f) }
    
    // Zoom animation for end time reached
    val zoomScale = remember { Animatable(initialValue = 1f) }
    
    // Start zoom animation when end time is reached
    LaunchedEffect(isEndTimeReached) {
        if (isEndTimeReached) {
            // Run a repeating animation that zooms in and out
            repeat(3) {
                // Zoom in - more subtle (1.2 instead of 1.5)
                zoomScale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = tween(
                        durationMillis = 800, // Slower animation (800ms instead of 500ms)
                        easing = LinearEasing
                    )
                )
                
                // Zoom out - more subtle (0.9 instead of 0.8)
                zoomScale.animateTo(
                    targetValue = 0.9f,
                    animationSpec = tween(
                        durationMillis = 800, // Slower animation (800ms instead of 500ms)
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
    
    // Get animated progress for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )
    
    // Get colors based on activity type
    val (primaryColor, secondaryColor) = when (activityType) {
        ActivityType.PLAY -> Pair(PlayPrimary, PlaySecondary)
        ActivityType.SLEEP -> Pair(SleepPrimary, SleepSecondary)
    }
    
    // Get selected wallpaper
    val wallpaperId by timerViewModel.selectedWallpaperId.collectAsState()
    val wallpaper = Wallpapers.getWallpaperById(wallpaperId)
    
    // Track time selection dialogs
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showWallpaperSelector by remember { mutableStateOf(false) }
    
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
                ) {
                    // Apply the zoom scale animation
                    scale(zoomScale.value) {
                        // Add dark circular overlay to improve contrast with bright backgrounds
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.70f),  // Increased opacity to 70%
                            radius = size.minDimension / 2 + 40f  // Made the background larger
                        )
                        
                        // Draw clock face
                        drawClockFace()
                        
                        // Draw hour numbers
                        drawHourNumbers()
                        
                        // Draw timer arc if timer is running OR when times are set (for preview)
                        if (!isEndTimeReached && (isTimerRunning || (startTimeMinutes > 0 && endTimeMinutes > 0))) {
                            // Hour arc properties
                            val hourArcSize = Size(size.width * 0.95f, size.height * 0.95f)
                            val hourArcOffset = Offset(
                                (size.width - hourArcSize.width) / 2f,
                                (size.height - hourArcSize.height) / 2f
                            )
                            
                            // Get the hour arc segment
                            val hourArcSegment = if (isTimerRunning) {
                                // Convert from TimerViewModel.HourArcSegment to our local HourArcSegment
                                timerViewModel.getHourArcSegment()?.let { 
                                    println("Using hour arc from ViewModel: startAngle=${it.startAngle}, sweepAngle=${it.sweepAngle}")
                                    HourArcSegment(it.startAngle, it.sweepAngle) 
                                }
                            } else if (startTimeMinutes > 0 && endTimeMinutes > 0) {
                                // For preview when timer is not running - Use the ViewModel function directly
                                timerViewModel.getInitialHourArcSegment()?.let {
                                    println("Using preview hour arc: startAngle=${it.startAngle}, sweepAngle=${it.sweepAngle}")
                                    HourArcSegment(it.startAngle, it.sweepAngle)
                                } ?: run {
                                    // Fallback to manual calculation if ViewModel returns null
                                    val startHourPosition = startHour % 12 + (startMinute / 60f)
                                    val endHourPosition = endHour % 12 + (endMinute / 60f)
                                    
                                    val startAngle = (startHourPosition * 30f) - 90f
                                    val endAngle = (endHourPosition * 30f) - 90f
                                    
                                    // Calculate sweep angle, handling midnight crossing
                                    val sweepAngle = if (endHourPosition < startHourPosition) {
                                        (360f - startAngle) + endAngle // Crosses midnight
                                    } else {
                                        endAngle - startAngle
                                    }
                                    
                                    println("Manually calculated hour arc: startAngle=$startAngle, sweepAngle=$sweepAngle")
                                    
                                    // Safety check - ensure we have a visible arc
                                    if (sweepAngle <= 0.5f) {
                                        HourArcSegment(startAngle, 30f) // Show at least 30 degrees
                                    } else {
                                        HourArcSegment(startAngle, sweepAngle)
                                    }
                                }
                            } else {
                                null
                            }
                            
                            // Draw hour arc if there's a segment to show
                            hourArcSegment?.let { segment ->
                                // Debug logging
                                println("Drawing hour arc: startAngle=${segment.startAngle}, sweepAngle=${segment.sweepAngle}, endAngle=${segment.startAngle + segment.sweepAngle}")

                                // Draw filled pie segment for hour arc with solid color
                                val strokeWidth = 48f
                                val clockRadius = size.minDimension / 2  // 140.dp (~420px)
                                val hourArcSize = Size(
                                    (clockRadius - strokeWidth / 2) * 2,  // Diameter adjusted for stroke
                                    (clockRadius - strokeWidth / 2) * 2
                                )
                                val hourArcOffset = Offset(
                                    clockRadius - hourArcSize.width / 2,  // Center horizontally
                                    clockRadius - hourArcSize.height / 2  // Center vertically
                                )

                                drawArc(
                                    color = Color(0xFFE3D10E), // Solid yellow color (#e3d10e)
                                    startAngle = segment.startAngle,
                                    sweepAngle = segment.sweepAngle,
                                    useCenter = false,
                                    size = hourArcSize,
                                    topLeft = hourArcOffset,
                                    alpha = 0.9f,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                            
                            // Minute arc properties
                            val minuteArcSize = Size(size.width * 0.8f, size.height * 0.8f)
                            val minuteArcOffset = Offset(
                                (size.width - minuteArcSize.width) / 2f,
                                (size.height - minuteArcSize.height) / 2f
                            )
                            
                            // Get minute arc segments (handles hour boundary crossing)
                            val minuteArcSegments = if (isTimerRunning) {
                                // Convert from TimerViewModel.MinuteArcSegment to our local MinuteArcSegment
                                println("Getting minute segments for running timer")
                                timerViewModel.getMinuteArcSegments().map { 
                                    MinuteArcSegment(it.startAngle, it.sweepAngle) 
                                }
                            } else if (startTimeMinutes > 0 && endTimeMinutes > 0) {
                                // Show preview arcs when timer is not running but times are set
                                // Use the getInitialMinuteArcSegments from ViewModel instead of creating our own
                                println("Getting initial minute segments for preview (timer not running)")
                                timerViewModel.getInitialMinuteArcSegments().map {
                                    MinuteArcSegment(it.startAngle, it.sweepAngle)
                                }
                            } else {
                                println("No conditions met for minute segments, returning empty list")
                                emptyList()
                            }
                            
                            // Log segments for debugging
                            if (minuteArcSegments.isNotEmpty()) {
                                println("Total minute segments: ${minuteArcSegments.size}")
                                minuteArcSegments.forEachIndexed { index, segment ->
                                    println("Minute segment $index: startAngle=${segment.startAngle}, sweepAngle=${segment.sweepAngle}, endAngle=${segment.startAngle + segment.sweepAngle}")
                                }
                            } else {
                                println("WARNING: No minute segments to draw!")
                            }
                            
                            // Draw each minute arc segment
                            minuteArcSegments.forEachIndexed { index, segment ->
                                // Skip segments with zero or tiny sweep angles
                                if (segment.sweepAngle < 0.05f) {
                                    println("Skipping too small segment $index: sweepAngle=${segment.sweepAngle}")
                                    return@forEachIndexed
                                }
                                
                                println("Drawing segment $index: startAngle=${segment.startAngle}, sweepAngle=${segment.sweepAngle}")
                                
                                // Draw with solid color instead of gradient
                                drawArc(
                                    color = Color(0xFFA7E810), // Solid green color (#a7e810) for minute wedge
                                    startAngle = segment.startAngle,
                                    sweepAngle = segment.sweepAngle,
                                    useCenter = true,
                                    size = minuteArcSize,
                                    topLeft = minuteArcOffset,
                                    alpha = 0.8f // Slight transparency for better appearance
                                )
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
                            drawSecondHand(primaryColor)
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
                            color = primaryColor,
                            radius = 6f,
                            center = center
                        )
                        
                        // If end time is reached, draw "TIME'S UP!" text
                        if (isEndTimeReached) {
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
                                center.y + textPaint.textSize / 3, // Adjust for vertical centering
                                textPaint
                            )
                        }
                    }
                }
            }
            
            // Controls
            if (!isTimerRunning) {
                ControlsSection(
                    activityType = activityType,
                    startTime = timerViewModel.formatStartTime(),
                    endTime = timerViewModel.formatEndTime(),
                    primaryColor = primaryColor,
                    onActivityTypeSelected = { timerViewModel.setActivityType(it) },
                    onStartTimeClicked = { showStartTimePicker = true },
                    onEndTimeClicked = { showEndTimePicker = true },
                    onStartClicked = { timerViewModel.startTimer() }
                )
            } else {
                if (isEndTimeReached) {
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
    
    // Time picker dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                timerViewModel.setStartTime(hour, minute)
                showStartTimePicker = false
            }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                timerViewModel.setEndTime(hour, minute)
                showEndTimePicker = false
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
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsSection(
    activityType: ActivityType,
    startTime: String,
    endTime: String,
    primaryColor: Color,
    onActivityTypeSelected: (ActivityType) -> Unit,
    onStartTimeClicked: () -> Unit,
    onEndTimeClicked: () -> Unit,
    onStartClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Activity selector
        var expanded by remember { mutableStateOf(false) }
        val activityLabel = when (activityType) {
            ActivityType.PLAY -> stringResource(R.string.play)
            ActivityType.SLEEP -> stringResource(R.string.sleep)
        }
        
        Text(
            text = stringResource(R.string.select_activity),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Text(
                    text = activityLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.play)) },
                    onClick = {
                        onActivityTypeSelected(ActivityType.PLAY)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep)) },
                    onClick = {
                        onActivityTypeSelected(ActivityType.SLEEP)
                        expanded = false
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time range selector
        Text(
            text = "Set Time Range",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "From",
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                TextButton(
                    onClick = onStartTimeClicked,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Text(
                        text = startTime,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            
            Text(
                text = "→",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // End time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "To",
                    color = Color.White,
                    fontSize = 14.sp
                )
                
                TextButton(
                    onClick = onEndTimeClicked,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Text(
                        text = endTime,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Start button
        Button(
            onClick = onStartClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Green color for start button
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.start),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
} 