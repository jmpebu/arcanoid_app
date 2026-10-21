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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shield
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
import com.example.game.model.RelicType
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
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import kotlin.math.roundToInt

@Composable
fun RelicSelectionOverlay(
    engine: ArcanoidEngine,
    onRelicChosen: (RelicType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArcadeBackground.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(ArcadeSurface)
                .border(2.dp, NeonPurple, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "⚡ MEJORA CIBERNÉTICA ⚡",
                color = NeonCyan,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Elige 1 reliquia para tu mecha de asalto:",
                color = ArcadeTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                engine.relicChoices.forEach { relic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRelicChosen(relic) }
                            .testTag("relic_card_${relic.name}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, relic.color)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(relic.color.copy(alpha = 0.18f))
                                    .border(1.dp, relic.color, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = relic.icon,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = relic.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = relic.rarity,
                                        color = relic.color,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = relic.description,
                                    color = ArcadeTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
                .padding(12.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ArcadeSurface),
            border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AJUSTES DEL MECHA",
                        color = NeonCyan,
                        fontSize = 16.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Control Mode Selector
                Text(
                    text = "MODO DE PILOTAJE",
                    color = ArcadeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ControlMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else ArcadeSurfaceVariant)
                                .clickable { onModeChange(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) ArcadeBackground else ArcadeTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Accelerometer Section
                if (currentMode != ControlMode.TOUCH && accelerometerController.isSensorAvailable) {
                    Text(
                        text = "CALIBRACIÓN ACELERÓMETRO",
                        color = NeonYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inclinación: ${(tiltDegrees).roundToInt()}°",
                            color = ArcadeTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = { accelerometerController.calibrateNeutral() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("calibrate_center_button")
                        ) {
                            Text("Fijar Centro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sensibilidad: ${(sensitivity * 10).roundToInt() / 10f}x",
                        color = ArcadeTextSecondary,
                        fontSize = 11.sp
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invertir dirección de giro",
                            color = ArcadeTextPrimary,
                            fontSize = 12.sp
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

                    Spacer(modifier = Modifier.height(8.dp))
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sonidos Cyberpunk", color = ArcadeTextPrimary, fontSize = 12.sp)
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vibración Háptica", color = ArcadeTextPrimary, fontSize = 12.sp)
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

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "GUARDAR Y CONTINUAR",
                        color = ArcadeBackground,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
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
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ArcadeSurface.copy(alpha = 0.94f))
                .border(2.dp, NeonCyan, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Acelerómetro",
                tint = NeonCyan,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PILOTO LISTO",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            val instructionText = if (controlMode != ControlMode.TOUCH && isSensorAvailable) {
                "Inclina tu móvil a los lados para mover el mecha.\nUsa los botones inferiores para disparar cañones y activar el escudo."
            } else {
                "Arrastra el dedo para mover el mecha.\nUsa los botones inferiores para disparar cañones y activar el escudo."
            }

            Text(
                text = instructionText,
                color = ArcadeTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                    text = "TOCA PARA INICIAR ASALTO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    highScore: Int,
    level: Int,
    relicCount: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNewRecord = score >= highScore && score > 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArcadeBackground.copy(alpha = 0.90f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ArcadeSurface)
                .border(2.5.dp, NeonRed, RoundedCornerShape(22.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "MECHA DESTRUIDO",
                color = NeonRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isNewRecord) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGold.copy(alpha = 0.2f))
                        .border(1.dp, NeonGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "★ ¡NUEVO RÉCORD DE ASALTO! ★",
                        color = NeonGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = "Sector Alcanzado: $level",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Reliquias Obtenidas: $relicCount",
                color = NeonPurple,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Puntos Finales: $score",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "Mejor Récord: $highScore",
                color = ArcadeTextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restart_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REINICIAR ASALTO ROGUELIKE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
