package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.game.audio.HapticSoundHelper
import com.example.game.engine.ArcanoidEngine
import com.example.game.model.ControlMode
import com.example.game.model.GameStatus
import com.example.game.sensor.AccelerometerController
import com.example.ui.theme.ArcadeBackground
import kotlinx.coroutines.isActive

@Composable
fun ArcanoidScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val soundHelper = remember { HapticSoundHelper(context) }
    val engine = remember { ArcanoidEngine(context, soundHelper) }
    val accelerometer = remember { AccelerometerController(context) }

    var controlMode by remember {
        mutableStateOf(
            if (accelerometer.isSensorAvailable) ControlMode.ACCELEROMETER else ControlMode.TOUCH
        )
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val gameStatus by engine.gameStatus.collectAsState()
    val currentControlMode by rememberUpdatedState(controlMode)

    // Manage Accelerometer Sensor Lifecycle
    DisposableEffect(controlMode) {
        if (controlMode != ControlMode.TOUCH) {
            accelerometer.start()
        }
        onDispose {
            accelerometer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundHelper.release()
            accelerometer.stop()
        }
    }

    // Handle back button: pause if playing, or dismiss settings
    BackHandler(enabled = gameStatus == GameStatus.PLAYING || showSettingsDialog) {
        if (showSettingsDialog) {
            showSettingsDialog = false
        } else if (gameStatus == GameStatus.PLAYING) {
            engine.pauseGame()
        }
    }

    // High performance continuous VSYNC game loop
    // Tied to Unit so it NEVER tears down, restarts, or skips frames when status or mode changes!
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                val rawDt = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                // Clamp dt to 33ms (30fps threshold) to prevent massive physical jumps on app switch
                val dt = rawDt.coerceIn(0.001f, 0.033f)

                // Steer paddle via Accelerometer with direct tilt-to-position mapping
                if (currentControlMode != ControlMode.TOUCH && accelerometer.isSensorAvailable) {
                    val targetX = accelerometer.computeTargetPaddleX(engine.width, engine.paddle.width)
                    engine.smoothMovePaddleTowards(targetX, dt)
                }

                // Advance game physics
                engine.update(dt)

                // Trigger Canvas draw layer invalidation without recomposing Compose nodes
                engine.renderTrigger = frameTime
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArcadeBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ArcadeBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top HUD
                ArcanoidHud(
                    engine = engine,
                    accelerometerController = accelerometer,
                    controlMode = controlMode,
                    onOpenSettings = {
                        engine.pauseGame()
                        showSettingsDialog = true
                    }
                )

                // Main Game Canvas
                ArcanoidCanvas(
                    engine = engine,
                    controlMode = controlMode,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ready to launch overlay
            if (gameStatus == GameStatus.READY) {
                ReadyPromptOverlay(
                    controlMode = controlMode,
                    isSensorAvailable = accelerometer.isSensorAvailable,
                    onLaunch = { engine.startOrLaunchBall() }
                )
            }

            // Level Cleared overlay
            if (gameStatus == GameStatus.LEVEL_CLEARED) {
                LevelClearedOverlay(
                    level = engine.level,
                    score = engine.score,
                    onNextLevel = { engine.nextLevel() }
                )
            }

            // Game Over overlay
            if (gameStatus == GameStatus.GAME_OVER) {
                GameOverOverlay(
                    score = engine.score,
                    highScore = engine.highScore,
                    onRestart = { engine.restartGame() }
                )
            }

            // Settings & Calibration Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    accelerometerController = accelerometer,
                    currentMode = controlMode,
                    onModeChange = { newMode -> controlMode = newMode },
                    soundEnabled = soundEnabled,
                    onSoundToggle = {
                        soundEnabled = it
                        soundHelper.isSoundEnabled = it
                    },
                    hapticsEnabled = hapticsEnabled,
                    onHapticsToggle = {
                        hapticsEnabled = it
                        soundHelper.isHapticsEnabled = it
                    },
                    onDismiss = {
                        showSettingsDialog = false
                        if (gameStatus == GameStatus.PAUSED) {
                            engine.resumeGame()
                        }
                    }
                )
            }
        }
    }
}
