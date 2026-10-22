package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.Lecture
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val EXTRA_LECTURE_ID = "extra_lecture_id"
    const val EXTRA_SUBJECT = "extra_subject"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_DAY = "extra_day"
    const val EXTRA_ROOM = "extra_room"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun scheduleLecture(context: Context, lecture: Lecture) {
        if (!lecture.enabled) {
            cancelLecture(context, lecture.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = calculateNextTriggerTime(lecture)

        val intent = Intent(context, LectureAlarmReceiver::class.java).apply {
            putExtra(EXTRA_LECTURE_ID, lecture.id)
            putExtra(EXTRA_SUBJECT, lecture.subject)
            putExtra(EXTRA_TIME, lecture.formattedTime)
            putExtra(EXTRA_DAY, lecture.day)
            putExtra(EXTRA_ROOM, lecture.roomName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lecture.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val showIntent = PendingIntent.getActivity(
                context,
                lecture.id.toInt(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled exact AlarmClock for '${lecture.subject}' at $triggerTime")
        } catch (e: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "Scheduled fallback exact alarm for '${lecture.subject}' at $triggerTime")
            } catch (ex: Exception) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                Log.e(TAG, "Used inexact idle alarm fallback: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun scheduleSnooze(context: Context, lectureId: Long, subject: String, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, LectureAlarmReceiver::class.java).apply {
            putExtra(EXTRA_LECTURE_ID, lectureId)
            putExtra(EXTRA_SUBJECT, "$subject (Snoozed ${minutes}m)")
            putExtra(EXTRA_TIME, "Snooze Alert")
            putExtra(EXTRA_DAY, "Now")
            putExtra(EXTRA_ROOM, "")
        }

        val snoozeRequestCode = (lectureId + 99999).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val showIntent = PendingIntent.getActivity(
                context,
                snoozeRequestCode,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showIntent), pendingIntent)
        } catch (e: Exception) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    fun cancelLecture(context: Context, lectureId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, LectureAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lectureId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun calculateNextTriggerTime(lecture: Lecture): Long {
        val hour = lecture.hour.coerceIn(0, 23)
        val minute = lecture.minute.coerceIn(0, 59)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Add 2-second buffer to avoid re-triggering within the exact same minute
        val now = System.currentTimeMillis() + 2000L

        when (lecture.repeatType) {
            "Daily" -> {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            "Weekdays" -> {
                while (calendar.timeInMillis <= now ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                ) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            "Weekends" -> {
                while (calendar.timeInMillis <= now ||
                    (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                     calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                ) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            "Once" -> {
                if (lecture.oneTimeDateMillis > now) {
                    return lecture.oneTimeDateMillis
                }
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            "Custom" -> {
                val allowedDays = parseCustomDays(lecture.customDays.ifBlank { lecture.day })
                if (allowedDays.isEmpty()) {
                    if (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return calendar.timeInMillis
                }
                var safetyCounter = 0
                while ((calendar.timeInMillis <= now || !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) && safetyCounter < 14) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    safetyCounter++
                }
                return calendar.timeInMillis
            }

            else -> {
                val allowedDays = parseCustomDays(lecture.day)
                if (allowedDays.isNotEmpty()) {
                    var safetyCounter = 0
                    while ((calendar.timeInMillis <= now || !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) && safetyCounter < 14) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        safetyCounter++
                    }
                    return calendar.timeInMillis
                }
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        }
    }

    private fun parseCustomDays(daysString: String): Set<Int> {
        val result = mutableSetOf<Int>()
        val tokens = daysString.split(",").map { it.trim().lowercase(Locale.US) }
        for (token in tokens) {
            when {
                token.startsWith("sun") -> result.add(Calendar.SUNDAY)
                token.startsWith("mon") -> result.add(Calendar.MONDAY)
                token.startsWith("tue") -> result.add(Calendar.TUESDAY)
                token.startsWith("wed") -> result.add(Calendar.WEDNESDAY)
                token.startsWith("thu") -> result.add(Calendar.THURSDAY)
                token.startsWith("fri") -> result.add(Calendar.FRIDAY)
                token.startsWith("sat") -> result.add(Calendar.SATURDAY)
            }
        }
        return result
    }
}
