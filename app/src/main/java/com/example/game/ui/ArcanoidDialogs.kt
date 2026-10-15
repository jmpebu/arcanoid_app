package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.ArcanoidEngine
import com.example.game.model.ControlMode
import com.example.game.model.GameStatus
import com.example.game.sensor.AccelerometerController
import com.example.ui.theme.ArcadeBackground
import com.example.ui.theme.ArcadeBorder
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.ArcadeSurfaceVariant
import com.example.ui.theme.ArcadeTextPrimary
import com.example.ui.theme.ArcadeTextSecondary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    accelerometerController: AccelerometerController,
    currentMode: ControlMode,
    onModeChange: (ControlMode) -> Unit,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val tiltFactor by accelerometerController.currentTiltFactor.collectAsState()
    val tiltDegrees by accelerometerController.tiltDegrees.collectAsState()
    var sensitivity by remember { mutableStateOf(accelerometerController.sensitivity) }
    var invertAxis by remember { mutableStateOf(accelerometerController.invertAxis) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArcadeSurface),
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AJUSTES DEL JUEGO",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = ArcadeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Mode Selector
                Text(
                    text = "MODO DE CONTROL",
                    color = ArcadeTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan else ArcadeSurfaceVariant)
                                .clickable { onModeChange(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) ArcadeBackground else ArcadeTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Accelerometer Section (if enabled)
                if (currentMode != ControlMode.TOUCH && accelerometerController.isSensorAvailable) {
                    Text(
                        text = "CALIBRACIÓN ACELERÓMETRO",
                        color = NeonYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Angle Display and Calibrate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inclinación: ${(tiltDegrees).roundToInt()}°",
                            color = ArcadeTextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = { accelerometerController.calibrateNeutral() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("calibrate_center_button")
                        ) {
                            Text("Fijar Centro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sensitivity Slider
                    Text(
                        text = "Sensibilidad: ${(sensitivity * 10).roundToInt() / 10f}x",
                        color = ArcadeTextSecondary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            sensitivity = it
                            accelerometerController.sensitivity = it
                        },
                        valueRange = 0.8f..2.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ArcadeBorder
                        )
                    )

                    // Invert Axis toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invertir dirección de giro",
                            color = ArcadeTextPrimary,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = invertAxis,
                            onCheckedChange = {
                                invertAxis = it
                                accelerometerController.invertAxis = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonPurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Audio & Vibration Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sonido",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sonidos Arcade", color = ArcadeTextPrimary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibración",
                            tint = NeonPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vibración Háptica", color = ArcadeTextPrimary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonPink,
                            checkedTrackColor = NeonPurple
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "GUARDAR Y CONTINUAR",
                        color = ArcadeBackground,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ReadyPromptOverlay(
    controlMode: ControlMode,
    isSensorAvailable: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onLaunch() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ArcadeSurface.copy(alpha = 0.92f))
                .border(2.dp, NeonCyan, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Acelerómetro",
                tint = NeonCyan,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¡PREPARADO!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            val instructionText = if (controlMode != ControlMode.TOUCH && isSensorAvailable) {
                "Inclina tu móvil a los lados para mover la paleta.\n¡Toca la pantalla para lanzar la bola!"
            } else {
                "Arrastra el dedo para mover la paleta.\n¡Toca la pantalla para lanzar la bola!"
            }

            Text(
                text = instructionText,
                color = ArcadeTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan, NeonPink)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "TOCA PARA JUGAR",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun LevelClearedOverlay(
    level: Int,
    score: Int,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArcadeBackground.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ArcadeSurface)
                .border(2.5.dp, NeonGreen, RoundedCornerShape(22.dp))
                .padding(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Victoria",
                tint = NeonGold,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¡NIVEL $level SUPERADO!",
                color = NeonGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PUNTUACIÓN: $score",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onNextLevel,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("next_level_button")
            ) {
                Text(
                    text = "SIGUIENTE NIVEL",
                    color = ArcadeBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    highScore: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNewRecord = score >= highScore && score > 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArcadeBackground.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ArcadeSurface)
                .border(2.5.dp, NeonPink, RoundedCornerShape(22.dp))
                .padding(28.dp)
        ) {
            Text(
                text = "FIN DE LA PARTIDA",
                color = NeonPink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isNewRecord) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGold.copy(alpha = 0.2f))
                        .border(1.dp, NeonGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "★ ¡NUEVO RÉCORD! ★",
                        color = NeonGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Puntos Finales: $score",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Mejor Marca: $highScore",
                color = ArcadeTextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restart_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JUGAR DE NUEVO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
