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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
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
    val boss = engine.boss

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Top Row: Level / Sector, Score, Lives & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level / Sector Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArcadeSurface)
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SECTOR ${engine.level}",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Score & High Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${engine.score}".padStart(6, '0'),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RÉCORD: ${engine.highScore}",
                    color = NeonGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Mech Health / Shields
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 1..engine.maxLives.coerceAtMost(6)) {
                    val active = i <= engine.lives
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Escudo $i",
                        tint = if (active) NeonCyan else Color(0x3300F0FF),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Pause & Settings
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
                        .size(36.dp)
                        .testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (gameStatus == GameStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pausar o reanudar",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Boss Health Bar (When boss is active)
        if (engine.isBossActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B0B24))
                    .border(1.dp, NeonRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠ IA CORRUPTA: ${engine.bossName}",
                        color = NeonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${engine.bossHp}/${engine.bossMaxHp} HP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { (engine.bossHp.toFloat() / engine.bossMaxHp.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonRed,
                    trackColor = Color(0xFF3B1225)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Secondary Row: Accelerometer Sensor Meter, Relics Count & Combo Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accelerometer spirit level
            if (controlMode != ControlMode.TOUCH && accelerometerController.isSensorAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ArcadeSurface.copy(alpha = 0.85f))
                        .border(1.dp, ArcadeBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${(tiltDegrees).roundToInt()}°",
                        color = ArcadeTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 5.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F0B24))
                            .border(0.6.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(8.dp)
                                .background(Color.White.copy(alpha = 0.4f))
                        )

                        val bubbleOffset = (tiltFactor * 18f).coerceIn(-18f, 18f).dp
                        Box(
                            modifier = Modifier
                                .offset(x = bubbleOffset)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White, NeonCyan)
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF231A45))
                            .clickable { accelerometerController.calibrateNeutral() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("quick_calibrate_button")
                    ) {
                        Text(
                            text = "Calibrar",
                            color = NeonYellow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Relics Count Chip
            if (engine.activeRelics.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArcadeSurface)
                        .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Token,
                        contentDescription = "Reliquias",
                        tint = NeonPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${engine.activeRelics.size} Reliquias",
                        color = NeonPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Combo Notification
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
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "COMBO x${engine.combo}!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
