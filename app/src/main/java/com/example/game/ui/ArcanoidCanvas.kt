package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.game.engine.ArcanoidEngine
import com.example.game.model.Ball
import com.example.game.model.Boss
import com.example.game.model.Brick
import com.example.game.model.BrickType
import com.example.game.model.ControlMode
import com.example.game.model.EnemyProjectile
import com.example.game.model.MechProjectile
import com.example.game.model.PowerUp
import com.example.ui.theme.ArcadeBackground
import com.example.ui.theme.ArcadeGridLine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcanoidCanvas(
    engine: ArcanoidEngine,
    controlMode: ControlMode,
    modifier: Modifier = Modifier
) {
    // Pre-allocated reusable graphics objects to ensure 0 GC pressure
    val brickCorner = remember { CornerRadius(6f, 6f) }
    val brickGlowCorner = remember { CornerRadius(8f, 8f) }
    val borderStroke = remember { Stroke(width = 1.5f) }
    val thickStroke = remember { Stroke(width = 2.5f) }
    val diamondPath = remember { Path() }
    val mechPath = remember { Path() }
    val bossPath = remember { Path() }

    val nativeTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val floatingPaint = remember {
        android.graphics.Paint().apply {
            textSize = 28f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_canvas")
            .pointerInput(controlMode) {
                detectTapGestures {
                    engine.startOrLaunchBall()
                }
            }
            .pointerInput(controlMode) {
                if (controlMode == ControlMode.TOUCH || controlMode == ControlMode.HYBRID) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        engine.movePaddleByDelta(dragAmount.x)
                    }
                }
            }
    ) {
        // Read renderTrigger for Compose draw invalidation without recomposition
        val frameTick = engine.renderTrigger

        val w = size.width
        val h = size.height
        engine.setDimensions(w, h)

        // 1. Arcade Cyberpunk Grid Background
        drawArcadeBackground(w, h)

        // 2. Bottom Laser Shield
        if (engine.bottomShieldActive || engine.quantumShieldActive) {
            drawBottomShield(w, h)
        }

        // 3. Bricks
        for (i in engine.bricks.indices) {
            val brick = engine.bricks[i]
            if (!brick.isDestroyed) {
                drawNeonBrick(brick, brickCorner, brickGlowCorner, borderStroke, diamondPath)
            }
        }

        // 4. Corrupted AI Boss
        val currentBoss = engine.boss
        if (currentBoss != null && !currentBoss.isDefeated) {
            drawCorruptedBoss(currentBoss, bossPath, borderStroke)
        }

        // 5. Mech Plasma Bolts (Player Projectiles)
        for (i in engine.mechProjectiles.indices) {
            val proj = engine.mechProjectiles[i]
            drawMechPlasmaBolt(proj)
        }

        // 6. Boss Hostile Energy Orbs
        for (i in engine.enemyProjectiles.indices) {
            val ep = engine.enemyProjectiles[i]
            drawEnemyEnergyOrb(ep)
        }

        // 7. Falling Power-ups
        for (i in engine.powerUps.indices) {
            drawPowerUp(engine.powerUps[i], borderStroke, nativeTextPaint)
        }

        // 8. Particles
        for (i in engine.particles.indices) {
            val p = engine.particles[i]
            drawCircle(
                color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = p.size * p.life,
                center = Offset(p.x, p.y)
            )
        }

        // 9. Armored Cyberpunk Mech (Replacing plain paddle)
        drawCyberpunkMech(engine, mechPath, borderStroke, thickStroke)

        // 10. Energy Balls & Glowing Trails
        for (i in engine.balls.indices) {
            drawNeonBall(engine.balls[i])
        }

        // 11. Floating Score / Status Texts
        drawFloatingTexts(engine, floatingPaint)
    }
}

private fun DrawScope.drawArcadeBackground(w: Float, h: Float) {
    drawRect(color = ArcadeBackground, size = Size(w, h))

    val verticalSpacing = w / 8f
    for (i in 1 until 8) {
        val x = i * verticalSpacing
        drawLine(
            color = ArcadeGridLine,
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
    }

    val horizontalSpacing = h / 16f
    for (j in 1 until 16) {
        val y = j * horizontalSpacing
        drawLine(
            color = ArcadeGridLine,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawBottomShield(w: Float, h: Float) {
    val laserY = h - 6f
    drawRect(
        color = NeonPurple.copy(alpha = 0.35f),
        topLeft = Offset(0f, laserY - 10f),
        size = Size(w, 20f)
    )
    drawLine(
        color = NeonCyan,
        start = Offset(0f, laserY),
        end = Offset(w, laserY),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White,
        start = Offset(0f, laserY),
        end = Offset(w, laserY),
        strokeWidth = 1.2f
    )
}

/**
 * Renders the Corrupted AI Boss with menacing robotic hull, cyclops eye and energy arcs
 */
private fun DrawScope.drawCorruptedBoss(
    boss: Boss,
    path: Path,
    stroke: Stroke
) {
    val cx = boss.x
    val cy = boss.y
    val halfW = boss.width / 2f
    val halfH = boss.height / 2f

    // Outer ominous glow
    drawRoundRect(
        color = boss.color.copy(alpha = 0.22f),
        topLeft = Offset(cx - halfW - 6f, cy - halfH - 6f),
        size = Size(boss.width + 12f, boss.height + 12f),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Boss Main Chassis Path
    path.reset()
    path.moveTo(cx - halfW, cy - halfH * 0.4f)
    path.lineTo(cx - halfW * 0.7f, cy - halfH)
    path.lineTo(cx + halfW * 0.7f, cy - halfH)
    path.lineTo(cx + halfW, cy - halfH * 0.4f)
    path.lineTo(cx + halfW * 0.85f, cy + halfH)
    path.lineTo(cx - halfW * 0.85f, cy + halfH)
    path.close()

    // Chassis body
    drawPath(path, color = Color(0xFF1A0A26))
    drawPath(path, color = boss.color, style = stroke)

    // Side Cannon Pods
    drawRect(
        color = boss.color,
        topLeft = Offset(cx - halfW - 2f, cy - 6f),
        size = Size(8f, 16f)
    )
    drawRect(
        color = boss.color,
        topLeft = Offset(cx + halfW - 6f, cy - 6f),
        size = Size(8f, 16f)
    )

    // Cyclops Central Eye
    val eyeRadius = 9f
    drawCircle(
        color = Color(0xFF0F051D),
        radius = eyeRadius + 3f,
        center = Offset(cx, cy)
    )
    val eyePulse = (sin(boss.eyeGlowPhase) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
    drawCircle(
        color = boss.color.copy(alpha = eyePulse),
        radius = eyeRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = Offset(cx, cy)
    )
}

/**
 * Renders the Epic Cyberpunk Combat Mech (replacing the plain bar)
 */
private fun DrawScope.drawCyberpunkMech(
    engine: ArcanoidEngine,
    mechPath: Path,
    borderStroke: Stroke,
    thickStroke: Stroke
) {
    val paddle = engine.paddle
    val cx = paddle.x
    val cy = paddle.y
    val halfW = paddle.width / 2f
    val halfH = paddle.height / 2f

    // 1. Quantum Shield Dome when active
    if (engine.quantumShieldActive) {
        val shieldRadius = paddle.width * 0.62f
        drawCircle(
            color = NeonCyan.copy(alpha = 0.22f),
            radius = shieldRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = NeonCyan,
            radius = shieldRadius,
            center = Offset(cx, cy),
            style = thickStroke
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = shieldRadius * 0.95f,
            center = Offset(cx, cy),
            style = borderStroke
        )
    }

    // 2. Thruster Flame Particles underneath
    val thrusterY = cy + halfH
    val wingOffset = halfW * 0.65f
    // Left thruster flame
    drawCircle(
        color = NeonOrange,
        radius = 5f,
        center = Offset(cx - wingOffset, thrusterY + 4f)
    )
    drawCircle(
        color = NeonYellow,
        radius = 2.5f,
        center = Offset(cx - wingOffset, thrusterY + 2f)
    )
    // Right thruster flame
    drawCircle(
        color = NeonOrange,
        radius = 5f,
        center = Offset(cx + wingOffset, thrusterY + 4f)
    )
    drawCircle(
        color = NeonYellow,
        radius = 2.5f,
        center = Offset(cx + wingOffset, thrusterY + 2f)
    )

    // 3. Armored Swept Wings Geometry
    mechPath.reset()
    // Front center nose
    mechPath.moveTo(cx, cy - halfH * 1.15f)
    // Upper right wing edge
    mechPath.lineTo(cx + halfW * 0.6f, cy - halfH * 0.6f)
    // Right cannon mount tip
    mechPath.lineTo(cx + halfW, cy - halfH * 0.2f)
    // Right wing wingtip
    mechPath.lineTo(cx + halfW * 0.9f, cy + halfH)
    // Bottom inner recess
    mechPath.lineTo(cx + halfW * 0.35f, cy + halfH * 0.7f)
    mechPath.lineTo(cx, cy + halfH * 0.9f)
    mechPath.lineTo(cx - halfW * 0.35f, cy + halfH * 0.7f)
    // Left wing wingtip
    mechPath.lineTo(cx - halfW * 0.9f, cy + halfH)
    // Left cannon mount tip
    mechPath.lineTo(cx - halfW, cy - halfH * 0.2f)
    // Upper left wing edge
    mechPath.lineTo(cx - halfW * 0.6f, cy - halfH * 0.6f)
    mechPath.close()

    // Outer Aura Glow
    drawRoundRect(
        color = NeonCyan.copy(alpha = 0.25f),
        topLeft = Offset(cx - halfW - 4f, cy - halfH - 4f),
        size = Size(paddle.width + 8f, paddle.height + 8f),
        cornerRadius = CornerRadius(halfH, halfH)
    )

    // Dark Titanium Chassis
    drawPath(mechPath, color = Color(0xFF0F172A))
    // Armor Border
    drawPath(mechPath, color = NeonCyan, style = borderStroke)

    // 4. Dual Plasma Cannon Barrels
    val cannonW = 6f
    val cannonH = 14f
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(cx - wingOffset - cannonW / 2f, cy - halfH - 4f),
        size = Size(cannonW, cannonH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = NeonYellow,
        topLeft = Offset(cx - wingOffset - cannonW / 2f + 1f, cy - halfH - 4f),
        size = Size(cannonW - 2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(cx + wingOffset - cannonW / 2f, cy - halfH - 4f),
        size = Size(cannonW, cannonH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = NeonYellow,
        topLeft = Offset(cx + wingOffset - cannonW / 2f + 1f, cy - halfH - 4f),
        size = Size(cannonW - 2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    // 5. Central Fusion Core
    drawCircle(
        color = Color(0xFF1E1035),
        radius = 7f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = NeonCyan,
        radius = 5f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White,
        radius = 2.5f,
        center = Offset(cx, cy)
    )
}

/**
 * Player Plasma Cannon Bolt
 */
private fun DrawScope.drawMechPlasmaBolt(proj: MechProjectile) {
    val boltLength = 16f
    // Glow
    drawLine(
        color = NeonYellow.copy(alpha = 0.35f),
        start = Offset(proj.x, proj.y),
        end = Offset(proj.x, proj.y + boltLength + 4f),
        strokeWidth = 6f
    )
    // Core beam
    drawLine(
        color = Color.White,
        start = Offset(proj.x, proj.y),
        end = Offset(proj.x, proj.y + boltLength),
        strokeWidth = 2.5f
    )
}

/**
 * Boss Hostile Energy Orb
 */
private fun DrawScope.drawEnemyEnergyOrb(ep: EnemyProjectile) {
    // Outer red glow
    drawCircle(
        color = ep.color.copy(alpha = 0.3f),
        radius = ep.radius * 1.7f,
        center = Offset(ep.x, ep.y)
    )
    // Energy orb body
    drawCircle(
        color = ep.color,
        radius = ep.radius,
        center = Offset(ep.x, ep.y)
    )
    // White-hot core
    drawCircle(
        color = Color.White,
        radius = ep.radius * 0.45f,
        center = Offset(ep.x, ep.y)
    )
}

private fun DrawScope.drawNeonBrick(
    brick: Brick,
    corner: CornerRadius,
    glowCorner: CornerRadius,
    borderStroke: Stroke,
    diamondPath: Path
) {
    val glowAlpha = brick.currentGlowAlpha

    drawRoundRect(
        color = brick.baseColor.copy(alpha = 0.22f * glowAlpha),
        topLeft = Offset(brick.x - 2f, brick.y - 2f),
        size = Size(brick.width + 4f, brick.height + 4f),
        cornerRadius = glowCorner
    )

    drawRoundRect(
        color = brick.baseColor,
        topLeft = Offset(brick.x, brick.y),
        size = Size(brick.width, brick.height),
        cornerRadius = corner
    )

    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(brick.x + 4f, brick.y + 3f),
        end = Offset(brick.x + brick.width - 4f, brick.y + 3f),
        strokeWidth = 1.5f
    )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(brick.x, brick.y),
        size = Size(brick.width, brick.height),
        cornerRadius = corner,
        style = borderStroke
    )

    when (brick.type) {
        BrickType.EXPLOSIVE -> {
            val cx = brick.x + brick.width / 2f
            val cy = brick.y + brick.height / 2f
            diamondPath.reset()
            diamondPath.moveTo(cx, cy - 5f)
            diamondPath.lineTo(cx + 5f, cy)
            diamondPath.lineTo(cx, cy + 5f)
            diamondPath.lineTo(cx - 5f, cy)
            diamondPath.close()
            drawPath(diamondPath, color = Color.White)
        }
        BrickType.ARMORED, BrickType.TOUGH -> {
            if (brick.hitsRemaining > 1) {
                val cy = brick.y + brick.height / 2f
                drawCircle(Color.White, radius = 2.5f, center = Offset(brick.x + 8f, cy))
                drawCircle(Color.White, radius = 2.5f, center = Offset(brick.x + brick.width - 8f, cy))
            }
        }
        BrickType.POWERUP -> {
            val cx = brick.x + brick.width / 2f
            val cy = brick.y + brick.height / 2f
            drawCircle(Color.White, radius = 3.5f, center = Offset(cx, cy))
        }
        BrickType.NORMAL -> {}
    }
}

private fun DrawScope.drawPowerUp(
    pu: PowerUp,
    borderStroke: Stroke,
    textPaint: android.graphics.Paint
) {
    val width = pu.size * 2.2f
    val height = pu.size
    val topLeft = Offset(pu.x - width / 2f, pu.y - height / 2f)
    val pillCorner = CornerRadius(height / 2f, height / 2f)

    drawRoundRect(
        color = pu.type.color.copy(alpha = 0.3f),
        topLeft = Offset(topLeft.x - 3f, topLeft.y - 3f),
        size = Size(width + 6f, height + 6f),
        cornerRadius = CornerRadius((height + 6f) / 2f, (height + 6f) / 2f)
    )

    drawRoundRect(
        color = pu.type.color,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = pillCorner
    )

    drawRoundRect(
        color = Color.White,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = pillCorner,
        style = borderStroke
    )

    drawContext.canvas.nativeCanvas.apply {
        drawText(pu.type.icon, pu.x, pu.y + 8f, textPaint)
    }
}

private fun DrawScope.drawNeonBall(ball: Ball) {
    val trailSize = ball.trail.size
    val trailColor = if (ball.isPiercing) NeonYellow else NeonPink
    for (i in 0 until trailSize) {
        val pt = ball.trail[i]
        val progress = 1f - (i.toFloat() / trailSize.toFloat())
        drawCircle(
            color = trailColor.copy(alpha = 0.3f * progress),
            radius = ball.radius * (0.35f + 0.55f * progress),
            center = Offset(pt.first, pt.second)
        )
    }

    val haloColor = if (ball.isPiercing) NeonYellow else NeonCyan
    drawCircle(
        color = haloColor.copy(alpha = 0.25f),
        radius = ball.radius * 1.5f,
        center = Offset(ball.x, ball.y)
    )

    drawCircle(
        color = haloColor,
        radius = ball.radius,
        center = Offset(ball.x, ball.y)
    )

    drawCircle(
        color = Color.White,
        radius = ball.radius * 0.45f,
        center = Offset(ball.x - ball.radius * 0.25f, ball.y - ball.radius * 0.25f)
    )
}

private fun DrawScope.drawFloatingTexts(
    engine: ArcanoidEngine,
    paint: android.graphics.Paint
) {
    drawContext.canvas.nativeCanvas.apply {
        for (i in engine.floatingTexts.indices) {
            val ft = engine.floatingTexts[i]
            paint.color = android.graphics.Color.argb(
                (ft.alpha.coerceIn(0f, 1f) * 255).toInt(),
                (ft.color.red * 255).toInt(),
                (ft.color.green * 255).toInt(),
                (ft.color.blue * 255).toInt()
            )
            drawText(ft.text, ft.x, ft.y, paint)
        }
    }
}
