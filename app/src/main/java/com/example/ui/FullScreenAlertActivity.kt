package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TimeTableApplication
import com.example.alarm.AlarmScheduler
import com.example.data.preferences.SettingsPreferences
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay

class FullScreenAlertActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var lectureId: Long = 0L
    private var subjectName: String = "Class Time"
    private var hasTriggeredFocusVibration = false

    private val closeRunnable = Runnable {
        if (!isFinishing) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLockscreenWake()
        super.onCreate(savedInstanceState)

        lectureId = intent.getLongExtra(AlarmScheduler.EXTRA_LECTURE_ID, 0L)
        subjectName = intent.getStringExtra(AlarmScheduler.EXTRA_SUBJECT) ?: "Class Time"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""
        val day = intent.getStringExtra(AlarmScheduler.EXTRA_DAY) ?: ""
        val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""

        val prefs = (applicationContext as? TimeTableApplication)?.preferences
            ?: SettingsPreferences(this)

        if (prefs.isVibrationEnabled) {
            VibrationHelper.vibrate3Seconds(this, force = true)
        }
        if (prefs.isSoundEnabled) {
            VibrationHelper.playAlarmSoundFor3Seconds(this)
        }

        // Keep the full-screen white alert visible for 15 seconds (while vibrating for 3 seconds)
        // so the user has time to read the lecture details or tap Snooze / Dismiss.
        handler.postDelayed(closeRunnable, 15000L)

        setContent {
            ModernPulseWhiteAlertWithSnooze(
                subject = subjectName,
                time = time,
                day = day,
                room = room,
                onDismiss = { dismissNow() },
                onSnooze = { minutes -> snoozeNow(minutes) }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasTriggeredFocusVibration) {
            hasTriggeredFocusVibration = true
            val prefs = (applicationContext as? TimeTableApplication)?.preferences
                ?: SettingsPreferences(this)
            if (prefs.isVibrationEnabled) {
                VibrationHelper.vibrate3Seconds(this, force = true)
            }
        }
    }

    private fun setupLockscreenWake() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun snoozeNow(minutes: Int) {
        handler.removeCallbacks(closeRunnable)
        VibrationHelper.cancel(this)
        AlarmScheduler.scheduleSnooze(this, lectureId, subjectName, minutes)
        Toast.makeText(this, "Alarm snoozed for $minutes minutes", Toast.LENGTH_SHORT).show()
        if (!isFinishing) finish()
    }

    private fun dismissNow() {
        handler.removeCallbacks(closeRunnable)
        VibrationHelper.cancel(this)
        if (!isFinishing) finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(closeRunnable)
        super.onDestroy()
    }
}

@Composable
fun ModernPulseWhiteAlertWithSnooze(
    subject: String,
    time: String,
    day: String,
    room: String,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(15) }
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // SOLID PURE WHITE BACKGROUND (#FFFFFF)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .testTag("solid_white_alert_overlay")
            .padding(24.dp)
    ) {
        // TOP MINIMAL LABEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LECTURE ALERT • VIBRATING (3s)",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp
            )
        }

        // CENTER: HUGE SUBJECT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .scale(pulseScale)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = subject,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 62.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_subject_title")
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (time.isNotBlank()) {
                Text(
                    text = time,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
            }

            if (day.isNotBlank() || room.isNotBlank()) {
                val subtitle = listOfNotNull(
                    day.takeIf { it.isNotBlank() },
                    room.takeIf { it.isNotBlank() }?.let { "Room $it" }
                ).joinToString(" • ")

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            }
        }

        // BOTTOM: SNOOZE 5m, SNOOZE 10m, and DISMISS BUTTONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Auto-closing in ${secondsRemaining}s • Tap snooze or dismiss",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Snooze 5 Min
                OutlinedButton(
                    onClick = { onSnooze(5) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snooze 5m", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Snooze 10 Min
                OutlinedButton(
                    onClick = { onSnooze(10) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snooze 10m", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("dismiss_alert_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
