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
import com.example.game.model.Brick
import com.example.game.model.BrickType
import com.example.game.model.ControlMode
import com.example.game.model.PowerUp
import com.example.ui.theme.ArcadeBackground
import com.example.ui.theme.ArcadeGridLine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

@Composable
fun ArcanoidCanvas(
    engine: ArcanoidEngine,
    controlMode: ControlMode,
    modifier: Modifier = Modifier
) {
    // Pre-allocated reusable graphics objects to ensure 0 GC pressure during 60/120 FPS rendering
    val brickCorner = remember { CornerRadius(6f, 6f) }
    val brickGlowCorner = remember { CornerRadius(8f, 8f) }
    val borderStroke = remember { Stroke(width = 1.5f) }
    val diamondPath = remember { Path() }

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
            textSize = 30f
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
        // CRITICAL FOR SMOOTH 60/120 FPS:
        // Reading engine.renderTrigger INSIDE the DrawScope registers this draw layer with Compose.
        // Whenever engine.renderTrigger updates, ONLY this draw layer invalidates without triggering recomposition!
        val frameTick = engine.renderTrigger

        val w = size.width
        val h = size.height
        engine.setDimensions(w, h)

        // 1. Draw Grid Background
        drawArcadeBackground(w, h)

        // 2. Draw Bottom Shield Laser
        if (engine.bottomShieldActive) {
            drawBottomShield(w, h)
        }

        // 3. Draw Bricks (optimized, 0 allocations)
        for (i in engine.bricks.indices) {
            val brick = engine.bricks[i]
            if (!brick.isDestroyed) {
                drawNeonBrick(brick, brickCorner, brickGlowCorner, borderStroke, diamondPath)
            }
        }

        // 4. Draw Falling Power-ups
        for (i in engine.powerUps.indices) {
            drawPowerUp(engine.powerUps[i], borderStroke, nativeTextPaint)
        }

        // 5. Draw Particles
        for (i in engine.particles.indices) {
            val p = engine.particles[i]
            drawCircle(
                color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = p.size * p.life,
                center = Offset(p.x, p.y)
            )
        }

        // 6. Draw Paddle
        drawNeonPaddle(engine)

        // 7. Draw Balls and Trails
        for (i in engine.balls.indices) {
            drawNeonBall(engine.balls[i])
        }

        // 8. Draw Floating Texts
        drawFloatingTexts(engine, floatingPaint)
    }
}

private fun DrawScope.drawArcadeBackground(w: Float, h: Float) {
    drawRect(color = ArcadeBackground, size = Size(w, h))

    // Vertical grid lines
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

    // Horizontal grid lines
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
    // Laser glow rect
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

private fun DrawScope.drawNeonBrick(
    brick: Brick,
    corner: CornerRadius,
    glowCorner: CornerRadius,
    borderStroke: Stroke,
    diamondPath: Path
) {
    val glowAlpha = brick.currentGlowAlpha

    // Outer glow
    drawRoundRect(
        color = brick.baseColor.copy(alpha = 0.22f * glowAlpha),
        topLeft = Offset(brick.x - 2f, brick.y - 2f),
        size = Size(brick.width + 4f, brick.height + 4f),
        cornerRadius = glowCorner
    )

    // Brick Base Body
    drawRoundRect(
        color = brick.baseColor,
        topLeft = Offset(brick.x, brick.y),
        size = Size(brick.width, brick.height),
        cornerRadius = corner
    )

    // Top surface shine
    drawLine(
        color = Color.White.copy(alpha = 0.6f),
        start = Offset(brick.x + 4f, brick.y + 3f),
        end = Offset(brick.x + brick.width - 4f, brick.y + 3f),
        strokeWidth = 1.5f
    )

    // Bright border
    drawRoundRect(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(brick.x, brick.y),
        size = Size(brick.width, brick.height),
        cornerRadius = corner,
        style = borderStroke
    )

    // Special indicators
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

    // Outer Glow
    drawRoundRect(
        color = pu.type.color.copy(alpha = 0.3f),
        topLeft = Offset(topLeft.x - 3f, topLeft.y - 3f),
        size = Size(width + 6f, height + 6f),
        cornerRadius = CornerRadius((height + 6f) / 2f, (height + 6f) / 2f)
    )

    // Capsule Body
    drawRoundRect(
        color = pu.type.color,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = pillCorner
    )

    // Border
    drawRoundRect(
        color = Color.White,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = pillCorner,
        style = borderStroke
    )

    // Draw Icon text using pre-allocated paint
    drawContext.canvas.nativeCanvas.apply {
        drawText(pu.type.icon, pu.x, pu.y + 8f, textPaint)
    }
}

private fun DrawScope.drawNeonPaddle(engine: ArcanoidEngine) {
    val paddle = engine.paddle
    val halfW = paddle.width / 2f
    val halfH = paddle.height / 2f
    val topLeft = Offset(paddle.x - halfW, paddle.y - halfH)
    val corner = CornerRadius(halfH, halfH)

    // Outer Aura Glow
    drawRoundRect(
        color = NeonCyan.copy(alpha = 0.25f),
        topLeft = Offset(topLeft.x - 4f, topLeft.y - 4f),
        size = Size(paddle.width + 8f, paddle.height + 8f),
        cornerRadius = CornerRadius(halfH + 4f, halfH + 4f)
    )

    // Paddle Body
    drawRoundRect(
        color = NeonCyan,
        topLeft = topLeft,
        size = Size(paddle.width, paddle.height),
        cornerRadius = corner
    )

    // Inner Neon Core
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(paddle.x - halfW + 4f, paddle.y - halfH + 3f),
        size = Size(paddle.width - 8f, paddle.height - 6f),
        cornerRadius = CornerRadius(halfH - 3f, halfH - 3f)
    )

    // Center directional pip
    drawCircle(
        color = NeonPurple,
        radius = 3.5f,
        center = Offset(paddle.x, paddle.y)
    )
}

private fun DrawScope.drawNeonBall(ball: Ball) {
    // 1. Comet trail
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

    // 2. Halo Glow
    val haloColor = if (ball.isPiercing) NeonYellow else NeonCyan
    drawCircle(
        color = haloColor.copy(alpha = 0.25f),
        radius = ball.radius * 1.5f,
        center = Offset(ball.x, ball.y)
    )

    // 3. Ball Core
    drawCircle(
        color = haloColor,
        radius = ball.radius,
        center = Offset(ball.x, ball.y)
    )

    // Center shine
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
