package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object VibrationHelper {

    private const val TAG = "VibrationHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null
    private var activeWakeLock: PowerManager.WakeLock? = null

    // 3-second strong alarm vibration pattern:
    // 0ms delay -> 850ms vibrate -> 125ms pause -> 850ms vibrate -> 125ms pause -> 1050ms vibrate = 3000ms total
    private val ALARM_PATTERN_TIMINGS = longArrayOf(0L, 850L, 125L, 850L, 125L, 1050L)
    private val ALARM_PATTERN_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255)

    /**
     * Acquires a CPU + screen WakeLock so the phone wakes up from sleep/Doze
     * and stays awake during the entire 3-second vibration and full-screen alert.
     */
    @Synchronized
    fun acquireWakeLock(context: Context, timeoutMs: Long = 5000L) {
        try {
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return
            if (activeWakeLock?.isHeld == true) {
                activeWakeLock?.release()
            }
            @Suppress("DEPRECATION")
            val levelAndFlags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE
            val wl = powerManager.newWakeLock(levelAndFlags, "TimeTableManager:LectureAlarmWakeLock")
            wl.setReferenceCounted(false)
            wl.acquire(timeoutMs)
            activeWakeLock = wl
        } catch (e: Exception) {
            Log.e(TAG, "Screen WakeLock fallback to PARTIAL_WAKE_LOCK: ${e.message}")
            try {
                val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wl = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimeTableManager:CpuWakeLock")
                wl?.setReferenceCounted(false)
                wl?.acquire(timeoutMs)
                activeWakeLock = wl
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to acquire partial WakeLock: ${ex.message}")
            }
        }
    }

    /**
     * Triggers a strong 3-second alarm vibration using USAGE_ALARM attributes so
     * Android 10–16 (API 29–36) and OEM skins (MIUI, OneUI, ColorOS, FunTouchOS)
     * execute the vibration both on lockscreen/background and in foreground.
     */
    @Synchronized
    fun vibrate3Seconds(context: Context, force: Boolean = true) {
        acquireWakeLock(context, 4500L)
        executeVibrationPulse(context)

        // Re-assert vibration 180ms later on the main thread in case NotificationManager.notify()
        // or Activity transition briefly reset the system vibrator service.
        mainHandler.postDelayed({
            executeVibrationPulse(context)
        }, 180L)
    }

    private fun executeVibrationPulse(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator == null || !vibrator.hasVibrator()) {
                Log.w(TAG, "Device does not have a hardware vibrator.")
                return
            }

            val alarmAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = try {
                    if (vibrator.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(ALARM_PATTERN_TIMINGS, ALARM_PATTERN_AMPLITUDES, -1)
                    } else {
                        VibrationEffect.createWaveform(ALARM_PATTERN_TIMINGS, -1)
                    }
                } catch (e: Exception) {
                    VibrationEffect.createOneShot(3000L, VibrationEffect.DEFAULT_AMPLITUDE)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibrationAttributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, vibrationAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(effect, alarmAudioAttributes)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ALARM_PATTERN_TIMINGS, -1, alarmAudioAttributes)
            }
            Log.d(TAG, "3-second alarm vibration started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing vibration: ${e.message}")
        }
    }

    /**
     * Plays the default alarm sound for 3.5 seconds using USAGE_ALARM stream.
     */
    @Synchronized
    fun playAlarmSoundFor3Seconds(context: Context) {
        try {
            stopAlarmSound()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone.play()
            activeRingtone = ringtone
            mainHandler.postDelayed({
                stopAlarmSound()
            }, 3500L)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound: ${e.message}")
        }
    }

    @Synchronized
    fun stopAlarmSound() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            activeRingtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm sound: ${e.message}")
        }
    }

    @Synchronized
    fun cancel(context: Context) {
        mainHandler.removeCallbacksAndMessages(null)
        stopAlarmSound()
        try {
            getVibrator(context)?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration: ${e.message}")
        }
        try {
            if (activeWakeLock?.isHeld == true) {
                activeWakeLock?.release()
            }
            activeWakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: run {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
