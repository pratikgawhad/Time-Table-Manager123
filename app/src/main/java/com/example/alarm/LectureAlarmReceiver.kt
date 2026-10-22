package com.example.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.TimeTableApplication
import com.example.data.preferences.SettingsPreferences
import com.example.ui.FullScreenAlertActivity
import com.example.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LectureAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Immediately wake up the CPU and screen for at least 5 seconds
        VibrationHelper.acquireWakeLock(context, 5500L)

        val prefs = SettingsPreferences(context)
        if (!prefs.areAlarmsGloballyEnabled) {
            Log.d(TAG, "Alarm triggered but global alarms are disabled in settings.")
            return
        }

        val lectureId = intent.getLongExtra(AlarmScheduler.EXTRA_LECTURE_ID, -1L)
        val subject = intent.getStringExtra(AlarmScheduler.EXTRA_SUBJECT) ?: "Upcoming Lecture"
        val time = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: ""
        val day = intent.getStringExtra(AlarmScheduler.EXTRA_DAY) ?: ""
        val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""

        Log.d(TAG, "Alarm received for subject: $subject, lectureId: $lectureId")

        // 2. Prepare intent for FullScreenAlertActivity
        val alertIntent = Intent(context, FullScreenAlertActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_LECTURE_ID, lectureId)
            putExtra(AlarmScheduler.EXTRA_SUBJECT, subject)
            putExtra(AlarmScheduler.EXTRA_TIME, time)
            putExtra(AlarmScheduler.EXTRA_DAY, day)
            putExtra(AlarmScheduler.EXTRA_ROOM, room)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (lectureId.coerceAtLeast(1L) * 10).toInt(),
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build high-priority alarm notification with full-screen wake intent
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentSubtitle = if (room.isNotBlank()) "$time • Room $room ($day)" else "$time • $day"

        val notificationBuilder = NotificationCompat.Builder(context, TimeTableApplication.CHANNEL_ID_LECTURE_ALARMS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Lecture Starting Now: $subject")
            .setContentText(contentSubtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        if (prefs.isSoundEnabled) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            notificationBuilder.setSound(alarmSound)
        } else {
            notificationBuilder.setSilent(true)
        }

        if (prefs.isVibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0L, 850L, 125L, 850L, 125L, 1050L))
        }

        val notificationId = (lectureId.toInt()).takeIf { it > 0 } ?: 1001
        notificationManager.notify(notificationId, notificationBuilder.build())

        // 4. Trigger 3-second hardware vibration & optional alarm ringtone AFTER notify()
        // so NotificationManager does not override or cancel the active vibration.
        if (prefs.isVibrationEnabled) {
            VibrationHelper.vibrate3Seconds(context, force = true)
        }
        if (prefs.isSoundEnabled) {
            VibrationHelper.playAlarmSoundFor3Seconds(context)
        }

        // 5. Launch FullScreenAlertActivity directly when permitted
        try {
            context.startActivity(alertIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Direct startActivity from receiver blocked (fullScreenIntent active): ${e.message}")
        }

        // 6. Re-arm next recurrence in background using goAsync() so OS doesn't kill the receiver
        if (lectureId > 0L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? TimeTableApplication
                    val repo = app?.repository
                    val lecture = repo?.getLectureById(lectureId)
                    if (lecture != null && lecture.enabled) {
                        if (lecture.repeatType.equals("Once", ignoreCase = true)) {
                            // One-time lecture has fired; mark disabled so it won't loop
                            repo.updateLecture(lecture.copy(enabled = false))
                        } else {
                            AlarmScheduler.scheduleLecture(context, lecture)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling next occurrence: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "LectureAlarmReceiver"
    }
}
