package com.example.game.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors

class HapticSoundHelper(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 40)
    } catch (_: Exception) {
        null
    }

    // Single background thread for audio events to avoid coroutine overhead and native AudioFlinger race conditions
    private val audioExecutor = Executors.newSingleThreadExecutor()

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private var lastSoundTimestamp: Long = 0L
    private var lastHapticTimestamp: Long = 0L

    private fun playToneThrottled(toneType: Int, durationMs: Int, minIntervalMs: Long = 35L) {
        if (!isSoundEnabled || toneGenerator == null) return
        val now = System.currentTimeMillis()
        if (now - lastSoundTimestamp < minIntervalMs) return
        lastSoundTimestamp = now

        audioExecutor.execute {
            try {
                toneGenerator?.startTone(toneType, durationMs)
            } catch (_: Exception) {}
        }
    }

    fun onPaddleHit() {
        val now = System.currentTimeMillis()
        if (isHapticsEnabled && vibrator?.hasVibrator() == true && now - lastHapticTimestamp > 40L) {
            lastHapticTimestamp = now
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        }
        playToneThrottled(ToneGenerator.TONE_PROP_BEEP, 20, 30L)
    }

    fun onBrickHit() {
        val now = System.currentTimeMillis()
        if (isHapticsEnabled && vibrator?.hasVibrator() == true && now - lastHapticTimestamp > 30L) {
            lastHapticTimestamp = now
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
        playToneThrottled(ToneGenerator.TONE_DTMF_8, 25, 30L)
    }

    fun onExplosion() {
        if (isHapticsEnabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(70)
            }
        }
        playToneThrottled(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 100, 60L)
    }

    fun onPowerUpCollected() {
        if (isHapticsEnabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        }
        playToneThrottled(ToneGenerator.TONE_PROP_ACK, 60, 50L)
    }

    fun onBallLost() {
        if (isHapticsEnabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(180)
            }
        }
        playToneThrottled(ToneGenerator.TONE_PROP_NACK, 160, 100L)
    }

    fun onLevelCleared() {
        if (isHapticsEnabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 50, 50, 50, 100)
                val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
        playToneThrottled(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200, 100L)
    }

    fun release() {
        try {
            audioExecutor.shutdown()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
