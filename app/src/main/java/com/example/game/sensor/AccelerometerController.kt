package com.example.game.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class AccelerometerController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isSensorAvailable: Boolean = accelerometer != null

    // Low-pass filtered tilt value (negative = left, positive = right)
    @Volatile
    private var filteredTiltX = 0f
    private var neutralOffset = 0f

    // Settings
    var sensitivity: Float = 1.5f
    var invertAxis: Boolean = false

    // Throttled UI StateFlows for HUD (updated at max ~15 Hz to prevent Compose recomposition thrashing)
    private var lastHudUpdateTimestamp: Long = 0L
    private val _currentTiltFactor = MutableStateFlow(0f)
    val currentTiltFactor: StateFlow<Float> = _currentTiltFactor.asStateFlow()

    private val _tiltDegrees = MutableStateFlow(0f)
    val tiltDegrees: StateFlow<Float> = _tiltDegrees.asStateFlow()

    private var isListening = false

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    fun calibrateNeutral() {
        neutralOffset += filteredTiltX
        filteredTiltX = 0f
    }

    fun resetCalibration() {
        neutralOffset = 0f
    }

    /**
     * Directly maps the current phone tilt angle to an absolute paddle target X on the screen.
     * This provides ultra-smooth, responsive, and natural physical control without velocity runaway or drift.
     */
    fun computeTargetPaddleX(screenWidth: Float, paddleWidth: Float): Float {
        val direction = if (invertAxis) -1f else 1f
        val effectiveTilt = filteredTiltX * direction

        // Standard phone tilt comfort range is ~20 to 25 degrees (approx 3.5 m/s^2 along X)
        // Adjust by sensitivity (higher sensitivity = smaller tilt needed)
        val maxTiltGravity = 3.6f / sensitivity.coerceIn(0.6f, 3.0f)
        val normalized = (effectiveTilt / maxTiltGravity).coerceIn(-1f, 1f)

        val halfW = paddleWidth / 2f
        val minX = halfW
        val maxX = screenWidth - halfW
        val centerX = screenWidth / 2f
        val travelSpan = centerX - minX

        // Map normalized -1f..+1f to minX..maxX
        return centerX + (normalized * travelSpan)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // In portrait mode:
        // Tilting right makes event.values[0] negative, tilting left makes it positive
        val rawX = -event.values[0]

        // Smooth low-pass filter: 0.22 alpha eliminates hand jitter while responding instantly (no lag)
        val alpha = 0.22f
        val centeredRaw = rawX - neutralOffset
        filteredTiltX = (alpha * centeredRaw) + ((1f - alpha) * filteredTiltX)

        // Throttle StateFlow updates so Compose HUD only recomposes at ~15 Hz
        val now = System.currentTimeMillis()
        if (now - lastHudUpdateTimestamp >= 66L) {
            lastHudUpdateTimestamp = now
            val deg = (filteredTiltX / 9.8f * 90f).coerceIn(-60f, 60f)
            _tiltDegrees.value = deg
            val normalized = (filteredTiltX / 4.0f).coerceIn(-1f, 1f)
            _currentTiltFactor.value = normalized
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
