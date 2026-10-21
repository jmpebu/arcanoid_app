package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.audio.HapticSoundHelper
import com.example.game.engine.ArcanoidEngine
import com.example.game.model.ControlMode
import com.example.game.model.GameStatus
import com.example.game.sensor.AccelerometerController
import com.example.ui.theme.ArcadeBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import kotlinx.coroutines.isActive
import kotlin.math.ceil

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
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                val rawDt = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                val dt = rawDt.coerceIn(0.001f, 0.033f)

                // Steer mech via Accelerometer with direct tilt-to-position mapping
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

            // Floating Active Skill Buttons (Bottom Left: Shield, Bottom Right: Cannons)
            if (gameStatus == GameStatus.PLAYING || gameStatus == GameStatus.READY) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skill 1: Quantum Shield
                    SkillActionButton(
                        icon = Icons.Default.Shield,
                        label = "ESCUDO",
                        color = NeonCyan,
                        cooldownFraction = if (engine.shieldCooldownRemaining > 0f) {
                            engine.shieldCooldownRemaining / engine.shieldCooldownMax
                        } else 0f,
                        cooldownSeconds = engine.shieldCooldownRemaining,
                        isActive = engine.quantumShieldActive,
                        enabled = engine.canActivateShield,
                        testTag = "skill_shield_button",
                        onClick = { engine.activateShield() }
                    )

                    // Skill 2: Plasma Cannons
                    SkillActionButton(
                        icon = Icons.Default.Bolt,
                        label = "CAÑONES",
                        color = NeonYellow,
                        cooldownFraction = if (engine.cannonCooldownRemaining > 0f) {
                            engine.cannonCooldownRemaining / engine.cannonCooldownMax
                        } else 0f,
                        cooldownSeconds = engine.cannonCooldownRemaining,
                        isActive = false,
                        enabled = engine.canFireCannons,
                        testTag = "skill_cannons_button",
                        onClick = { engine.fireCannons() }
                    )
                }
            }

            // Ready to launch prompt overlay
            if (gameStatus == GameStatus.READY) {
                ReadyPromptOverlay(
                    controlMode = controlMode,
                    isSensorAvailable = accelerometer.isSensorAvailable,
                    onLaunch = { engine.startOrLaunchBall() }
                )
            }

            // Roguelike Relic Selection Overlay
            if (gameStatus == GameStatus.RELIC_SELECTION) {
                RelicSelectionOverlay(
                    engine = engine,
                    onRelicChosen = { relic -> engine.chooseRelic(relic) }
                )
            }

            // Game Over overlay
            if (gameStatus == GameStatus.GAME_OVER) {
                GameOverOverlay(
                    score = engine.score,
                    highScore = engine.highScore,
                    level = engine.level,
                    relicCount = engine.activeRelics.size,
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

@Composable
private fun SkillActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    cooldownFraction: Float,
    cooldownSeconds: Float,
    isActive: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val isReady = enabled && cooldownFraction <= 0f
    val buttonAlpha = if (isReady || isActive) 1f else 0.55f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F0B24).copy(alpha = 0.9f))
                .border(
                    width = if (isActive) 2.5.dp else 1.5.dp,
                    color = if (isActive) NeonPurple else if (isReady) color else Color(0xFF33275A),
                    shape = CircleShape
                )
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Circular cooldown progress
            if (cooldownFraction > 0f) {
                CircularProgressIndicator(
                    progress = { cooldownFraction },
                    modifier = Modifier.size(62.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    trackColor = Color.Transparent,
                    strokeWidth = 3.dp
                )
            }

            if (cooldownFraction > 0f) {
                Text(
                    text = "${ceil(cooldownSeconds).toInt()}s",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) NeonPurple else color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = label,
            color = if (isReady) color else Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
