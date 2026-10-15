package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.ArcanoidEngine
import com.example.game.model.ControlMode
import com.example.game.model.GameStatus
import com.example.game.sensor.AccelerometerController
import com.example.ui.theme.ArcadeBorder
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.ArcadeTextSecondary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonYellow
import kotlin.math.roundToInt

@Composable
fun ArcanoidHud(
    engine: ArcanoidEngine,
    accelerometerController: AccelerometerController,
    controlMode: ControlMode,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tiltFactor by accelerometerController.currentTiltFactor.collectAsState()
    val tiltDegrees by accelerometerController.tiltDegrees.collectAsState()
    val gameStatus by engine.gameStatus.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Row: Level, Scores, Lives & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArcadeSurface)
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "NVL ${engine.level}",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Score Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${engine.score}".padStart(5, '0'),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RÉCORD: ${engine.highScore}",
                    color = NeonGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Lives indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 1..5) {
                    val active = i <= engine.lives
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Vida $i",
                        tint = if (active) NeonPink else Color(0x33FF007F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Action Buttons (Pause & Settings)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (gameStatus == GameStatus.PLAYING) {
                            engine.pauseGame()
                        } else if (gameStatus == GameStatus.PAUSED) {
                            engine.resumeGame()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (gameStatus == GameStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pausar o reanudar",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Row: Accelerometer Tilt Indicator & Combo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spirit Level Bubble Meter for Accelerometer
            if (controlMode != ControlMode.TOUCH && accelerometerController.isSensorAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ArcadeSurface.copy(alpha = 0.85f))
                        .border(1.dp, ArcadeBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Giro: ${(tiltDegrees).roundToInt()}°",
                        color = ArcadeTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    // Horizontal tube meter
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF0F0B24))
                            .border(0.8.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Center crosshair line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(10.dp)
                                .background(Color.White.copy(alpha = 0.4f))
                        )

                        // Glowing bubble moving left/right (-24dp to +24dp offset)
                        val bubbleOffset = (tiltFactor * 22f).coerceIn(-24f, 24f).dp
                        Box(
                            modifier = Modifier
                                .offset(x = bubbleOffset)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White, NeonCyan)
                                    )
                                )
                        )
                    }

                    // Quick Calibrate button
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF231A45))
                            .clickable { accelerometerController.calibrateNeutral() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("quick_calibrate_button")
                    ) {
                        Text(
                            text = "Calibrar",
                            color = NeonYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Control: ${controlMode.displayName}",
                    color = ArcadeTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Combo notification tag
            AnimatedVisibility(
                visible = engine.combo > 1,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonPink, NeonOrange)
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "COMBO x${engine.combo}!",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
