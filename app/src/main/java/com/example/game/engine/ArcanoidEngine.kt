package com.example.game.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.game.audio.HapticSoundHelper
import com.example.game.level.LevelGenerator
import com.example.game.model.Ball
import com.example.game.model.Brick
import com.example.game.model.BrickType
import com.example.game.model.FloatingText
import com.example.game.model.GameStatus
import com.example.game.model.Paddle
import com.example.game.model.Particle
import com.example.game.model.PowerUp
import com.example.game.model.PowerUpType
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ArcanoidEngine(
    context: Context,
    val soundHelper: HapticSoundHelper
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arcanoid_prefs", Context.MODE_PRIVATE)

    // Render trigger read by Canvas to redraw each frame without recomposition
    var renderTrigger by mutableLongStateOf(0L)

    var width: Float = 1000f
    var height: Float = 1600f

    val paddle: Paddle = Paddle(x = 500f, y = 1450f, width = 180f, baseWidth = 180f)
    val balls = mutableListOf<Ball>()
    val bricks = mutableListOf<Brick>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    var score: Int = 0
        private set
    var highScore: Int = prefs.getInt("high_score", 0)
        private set
    var lives: Int = 3
        private set
    var level: Int = 1
        private set
    var combo: Int = 0
        private set

    var bottomShieldActive: Boolean = false
        private set

    private var widePaddleTimer: Float = 0f
    private var laserBallTimer: Float = 0f
    private var slowBallTimer: Float = 0f

    private val _gameStatus = MutableStateFlow(GameStatus.READY)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private var nextEntityId = 1
    private val rng = Random.Default

    fun setDimensions(newWidth: Float, newHeight: Float) {
        if (newWidth <= 0f || newHeight <= 0f) return
        val isFirstInit = (width == 1000f && height == 1600f)
        width = newWidth
        height = newHeight

        paddle.y = height - 120f
        paddle.baseWidth = (width * 0.28f).coerceIn(120f, 220f)
        paddle.width = if (widePaddleTimer > 0f) paddle.baseWidth * 1.5f else paddle.baseWidth

        if (isFirstInit || _gameStatus.value == GameStatus.READY) {
            paddle.x = width / 2f
            resetBallToPaddle()
            if (bricks.isEmpty()) {
                loadLevel(level)
            }
        }
    }

    fun startOrLaunchBall() {
        if (_gameStatus.value == GameStatus.READY) {
            _gameStatus.value = GameStatus.PLAYING
            if (balls.isEmpty()) {
                resetBallToPaddle()
            }
            // Launch ball upwards with initial angle
            val ball = balls.firstOrNull() ?: return
            val launchAngle = (-55f - rng.nextFloat() * 70f) * (PI.toFloat() / 180f)
            val baseSpeed = height * 0.42f
            ball.vx = baseSpeed * cos(launchAngle)
            ball.vy = baseSpeed * sin(launchAngle)
            soundHelper.onPaddleHit()
        } else if (_gameStatus.value == GameStatus.PAUSED) {
            _gameStatus.value = GameStatus.PLAYING
        }
    }

    fun pauseGame() {
        if (_gameStatus.value == GameStatus.PLAYING) {
            _gameStatus.value = GameStatus.PAUSED
        }
    }

    fun resumeGame() {
        if (_gameStatus.value == GameStatus.PAUSED) {
            _gameStatus.value = GameStatus.PLAYING
        }
    }

    fun restartGame() {
        score = 0
        lives = 3
        level = 1
        combo = 0
        widePaddleTimer = 0f
        laserBallTimer = 0f
        slowBallTimer = 0f
        bottomShieldActive = false
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
        paddle.width = paddle.baseWidth
        paddle.x = width / 2f
        loadLevel(1)
        resetBallToPaddle()
        _gameStatus.value = GameStatus.READY
    }

    fun nextLevel() {
        level++
        combo = 0
        widePaddleTimer = 0f
        laserBallTimer = 0f
        slowBallTimer = 0f
        bottomShieldActive = false
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
        paddle.width = paddle.baseWidth
        paddle.x = width / 2f
        loadLevel(level)
        resetBallToPaddle()
        _gameStatus.value = GameStatus.READY
    }

    /**
     * Smoothly moves the paddle towards a target X position using exponential damping.
     * Guarantees zero jitter, zero stuttering, and instant natural responsiveness.
     */
    fun smoothMovePaddleTowards(targetX: Float, dt: Float) {
        val halfW = paddle.width / 2f
        val clampedTarget = targetX.coerceIn(halfW, width - halfW)

        // Exponential smoothing factor: 22.0 gives immediate, buttery-smooth tracking without overshoot
        val blend = 1f - exp(-22f * dt)
        paddle.x += (clampedTarget - paddle.x) * blend
        paddle.x = paddle.x.coerceIn(halfW, width - halfW)

        // Keep docked ball attached to paddle center
        if (_gameStatus.value == GameStatus.READY && balls.isNotEmpty()) {
            balls[0].x = paddle.x
        }
    }

    fun movePaddleByDelta(deltaX: Float) {
        val halfW = paddle.width / 2f
        paddle.x = (paddle.x + deltaX).coerceIn(halfW, width - halfW)

        if (_gameStatus.value == GameStatus.READY && balls.isNotEmpty()) {
            balls[0].x = paddle.x
        }
    }

    private fun loadLevel(lvl: Int) {
        bricks.clear()
        bricks.addAll(LevelGenerator.generateLevel(lvl, width, height))
    }

    private fun resetBallToPaddle() {
        balls.clear()
        val ballRadius = (width * 0.024f).coerceIn(12f, 18f)
        val newBall = Ball(
            id = nextEntityId++,
            x = paddle.x,
            y = paddle.y - paddle.height / 2f - ballRadius - 2f,
            vx = 0f,
            vy = 0f,
            radius = ballRadius
        )
        balls.add(newBall)
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceIn(0.001f, 0.033f)

        // Update Timers for active powerups
        if (widePaddleTimer > 0f) {
            widePaddleTimer -= dt
            if (widePaddleTimer <= 0f) {
                paddle.width = paddle.baseWidth
            }
        }
        if (laserBallTimer > 0f) {
            laserBallTimer -= dt
            if (laserBallTimer <= 0f) {
                balls.forEach { it.isPiercing = false }
            }
        }
        if (slowBallTimer > 0f) {
            slowBallTimer -= dt
        }

        if (_gameStatus.value != GameStatus.PLAYING) {
            updateParticles(dt)
            updateFloatingTexts(dt)
            return
        }

        // Update Balls
        val currentSpeedMultiplier = if (slowBallTimer > 0f) 0.65f else 1.0f
        val ballsIterator = balls.iterator()
        val ballsToRemove = mutableListOf<Ball>()

        while (ballsIterator.hasNext()) {
            val ball = ballsIterator.next()

            // Update trail (limit trail size to 6 points for high performance)
            ball.trail.add(0, Pair(ball.x, ball.y))
            if (ball.trail.size > 6) {
                ball.trail.removeAt(ball.trail.size - 1)
            }

            // Move ball
            ball.x += ball.vx * currentSpeedMultiplier * dt
            ball.y += ball.vy * currentSpeedMultiplier * dt

            // Left & Right Wall Collisions (with position separation)
            if (ball.x - ball.radius <= 0f) {
                ball.x = ball.radius
                ball.vx = abs(ball.vx)
                spawnWallParticles(ball.x, ball.y, NeonCyan)
                soundHelper.onPaddleHit()
            } else if (ball.x + ball.radius >= width) {
                ball.x = width - ball.radius
                ball.vx = -abs(ball.vx)
                spawnWallParticles(ball.x, ball.y, NeonCyan)
                soundHelper.onPaddleHit()
            }

            // Top Ceiling Collision (with position separation)
            if (ball.y - ball.radius <= 0f) {
                ball.y = ball.radius
                ball.vy = abs(ball.vy)
                spawnWallParticles(ball.x, ball.y, NeonPink)
                soundHelper.onPaddleHit()
            }

            // Bottom Shield / Floor Collision
            if (ball.y + ball.radius >= height) {
                if (bottomShieldActive) {
                    bottomShieldActive = false
                    ball.y = height - ball.radius - 8f
                    ball.vy = -abs(ball.vy)
                    soundHelper.onExplosion()
                    spawnExplosionParticles(width / 2f, height - 10f, NeonPurple, 15)
                    addFloatingText("¡ESCUDO!", width / 2f, height - 60f, NeonPurple)
                } else {
                    ballsToRemove.add(ball)
                }
            }

            // Paddle Collision
            val paddleTop = paddle.y - paddle.height / 2f
            val paddleBottom = paddle.y + paddle.height / 2f
            val halfPaddleW = paddle.width / 2f

            if (ball.vy > 0f &&
                ball.y + ball.radius >= paddleTop &&
                ball.y - ball.radius <= paddleBottom &&
                ball.x >= paddle.x - halfPaddleW - ball.radius &&
                ball.x <= paddle.x + halfPaddleW + ball.radius
            ) {
                // Push ball strictly above paddle to prevent multi-frame sticking
                ball.y = paddleTop - ball.radius - 1f

                // Angle based on hit location
                val hitFactor = ((ball.x - paddle.x) / halfPaddleW).coerceIn(-1f, 1f)
                val maxBounceAngle = 60f * (PI.toFloat() / 180f)
                val bounceAngle = hitFactor * maxBounceAngle

                val currentSpeed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
                val baseSpeed = (height * 0.44f).coerceIn(380f, 850f)
                val newSpeed = (currentSpeed * 1.01f).coerceIn(baseSpeed, baseSpeed * 1.5f)

                ball.vx = newSpeed * sin(bounceAngle)
                ball.vy = -newSpeed * cos(bounceAngle)

                combo = 0 // reset combo on paddle hit
                spawnWallParticles(ball.x, ball.y, NeonCyan)
                soundHelper.onPaddleHit()
            }

            // Brick Collisions
            checkBrickCollisions(ball)
        }

        // Remove fallen balls
        balls.removeAll(ballsToRemove)

        // If no balls remaining, lose a life
        if (balls.isEmpty()) {
            lives--
            soundHelper.onBallLost()
            combo = 0
            if (lives <= 0) {
                _gameStatus.value = GameStatus.GAME_OVER
                checkAndSaveHighScore()
            } else {
                _gameStatus.value = GameStatus.READY
                resetBallToPaddle()
            }
        }

        // Update PowerUps
        val powerUpIterator = powerUps.iterator()
        val paddleTop = paddle.y - paddle.height / 2f
        val paddleBottom = paddle.y + paddle.height / 2f
        val halfPaddleW = paddle.width / 2f

        while (powerUpIterator.hasNext()) {
            val pu = powerUpIterator.next()
            pu.y += pu.vy * dt

            // Paddle collect power-up
            if (pu.y + pu.size / 2f >= paddleTop &&
                pu.y - pu.size / 2f <= paddleBottom &&
                pu.x >= paddle.x - halfPaddleW &&
                pu.x <= paddle.x + halfPaddleW
            ) {
                applyPowerUp(pu.type)
                soundHelper.onPowerUpCollected()
                spawnExplosionParticles(pu.x, pu.y, pu.type.color, 10)
                addFloatingText(pu.type.label, pu.x, pu.y - 20f, pu.type.color)
                powerUpIterator.remove()
                continue
            }

            // Fall off screen
            if (pu.y - pu.size > height) {
                powerUpIterator.remove()
            }
        }

        // Update particles & floating texts
        updateParticles(dt)
        updateFloatingTexts(dt)

        // Check if level cleared
        val remainingBreakable = bricks.count { !it.isDestroyed }
        if (remainingBreakable == 0 && bricks.isNotEmpty()) {
            val clearBonus = level * 500
            score += clearBonus
            checkAndSaveHighScore()
            soundHelper.onLevelCleared()
            addFloatingText("¡NIVEL LIMPIO! +$clearBonus", width / 2f, height / 2f, NeonGold)
            _gameStatus.value = GameStatus.LEVEL_CLEARED
        }
    }

    private fun checkBrickCollisions(ball: Ball) {
        for (brick in bricks) {
            if (brick.isDestroyed) continue

            val closestX = ball.x.coerceIn(brick.x, brick.x + brick.width)
            val closestY = ball.y.coerceIn(brick.y, brick.y + brick.height)

            val distX = ball.x - closestX
            val distY = ball.y - closestY
            val distSq = distX * distX + distY * distY

            if (distSq <= ball.radius * ball.radius) {
                // Collision happened with clean separation
                if (!ball.isPiercing) {
                    val overlapX = (ball.radius + brick.width / 2f) - abs(ball.x - (brick.x + brick.width / 2f))
                    val overlapY = (ball.radius + brick.height / 2f) - abs(ball.y - (brick.y + brick.height / 2f))

                    if (overlapX < overlapY) {
                        ball.vx = if (ball.x < brick.x + brick.width / 2f) -abs(ball.vx) else abs(ball.vx)
                        ball.x += if (ball.vx > 0) overlapX else -overlapX
                    } else {
                        ball.vy = if (ball.y < brick.y + brick.height / 2f) -abs(ball.vy) else abs(ball.vy)
                        ball.y += if (ball.vy > 0) overlapY else -overlapY
                    }
                }

                damageBrick(brick, ball.x, ball.y)
                if (!ball.isPiercing) break
            }
        }
    }

    private fun damageBrick(brick: Brick, hitX: Float, hitY: Float) {
        brick.hitsRemaining--
        soundHelper.onBrickHit()

        if (brick.hitsRemaining <= 0) {
            brick.isDestroyed = true
            combo++
            val multiplier = 1f + (combo - 1) * 0.25f
            val earnedPoints = (brick.points * multiplier).toInt()
            score += earnedPoints
            checkAndSaveHighScore()

            val text = if (combo > 1) "+$earnedPoints (x$combo!)" else "+$earnedPoints"
            addFloatingText(text, brick.x + brick.width / 2f, brick.y, brick.baseColor)
            spawnExplosionParticles(brick.x + brick.width / 2f, brick.y + brick.height / 2f, brick.baseColor, 10)

            // Trigger explosive brick chained damage
            if (brick.type == BrickType.EXPLOSIVE) {
                soundHelper.onExplosion()
                triggerExplosionAround(brick)
            }

            // Power-up chance
            if (brick.type == BrickType.POWERUP || rng.nextFloat() < 0.18f) {
                spawnPowerUp(brick.x + brick.width / 2f, brick.y + brick.height / 2f)
            }
        } else {
            // Hit tough brick
            spawnWallParticles(hitX, hitY, brick.baseColor)
        }
    }

    private fun triggerExplosionAround(originBrick: Brick) {
        val blastRadius = originBrick.width * 1.8f
        val cx = originBrick.x + originBrick.width / 2f
        val cy = originBrick.y + originBrick.height / 2f

        spawnExplosionParticles(cx, cy, NeonOrange, 18)

        for (b in bricks) {
            if (b.isDestroyed || b.id == originBrick.id) continue
            val bcx = b.x + b.width / 2f
            val bcy = b.y + b.height / 2f
            val dist = sqrt((cx - bcx) * (cx - bcx) + (cy - bcy) * (cy - bcy))
            if (dist <= blastRadius) {
                damageBrick(b, bcx, bcy)
            }
        }
    }

    private fun spawnPowerUp(x: Float, y: Float) {
        val types = PowerUpType.values()
        val chosen = types[rng.nextInt(types.size)]
        powerUps.add(
            PowerUp(
                id = nextEntityId++,
                type = chosen,
                x = x,
                y = y,
                vy = (height * 0.16f).coerceIn(150f, 250f)
            )
        )
    }

    private fun applyPowerUp(type: PowerUpType) {
        when (type) {
            PowerUpType.WIDE_PADDLE -> {
                widePaddleTimer = 15f
                paddle.width = paddle.baseWidth * 1.5f
            }
            PowerUpType.MULTI_BALL -> {
                if (balls.isNotEmpty()) {
                    val baseBall = balls.first()
                    for (i in 0..1) {
                        val angleOffset = if (i == 0) 0.35f else -0.35f
                        val speed = sqrt(baseBall.vx * baseBall.vx + baseBall.vy * baseBall.vy)
                        val angle = (if (baseBall.vy < 0) -PI / 2f else PI / 2f).toFloat() + angleOffset
                        balls.add(
                            Ball(
                                id = nextEntityId++,
                                x = baseBall.x,
                                y = baseBall.y,
                                vx = speed * cos(angle),
                                vy = speed * sin(angle),
                                radius = baseBall.radius,
                                isPiercing = laserBallTimer > 0f
                            )
                        )
                    }
                }
            }
            PowerUpType.SLOW_BALL -> {
                slowBallTimer = 12f
            }
            PowerUpType.LASER_BALL -> {
                laserBallTimer = 9f
                balls.forEach { it.isPiercing = true }
            }
            PowerUpType.EXTRA_LIFE -> {
                if (lives < 5) lives++
            }
            PowerUpType.BOTTOM_SHIELD -> {
                bottomShieldActive = true
            }
        }
    }

    private fun spawnExplosionParticles(x: Float, y: Float, color: Color, count: Int) {
        // Enforce maximum particle pool to avoid GC
        if (particles.size > 40) return
        val clampedCount = count.coerceAtMost(12)
        for (i in 0 until clampedCount) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val speed = rng.nextFloat() * 200f + 50f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = speed * cos(angle),
                    vy = speed * sin(angle),
                    color = color,
                    size = rng.nextFloat() * 6f + 3f,
                    life = 1f,
                    decay = rng.nextFloat() * 0.05f + 0.03f
                )
            )
        }
    }

    private fun spawnWallParticles(x: Float, y: Float, color: Color) {
        if (particles.size > 40) return
        for (i in 0 until 4) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val speed = rng.nextFloat() * 100f + 25f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = speed * cos(angle),
                    vy = speed * sin(angle),
                    color = color,
                    size = rng.nextFloat() * 4f + 2f,
                    life = 0.7f,
                    decay = 0.06f
                )
            )
        }
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        if (floatingTexts.size > 4) floatingTexts.removeAt(0)
        floatingTexts.add(
            FloatingText(
                id = nextEntityId++,
                text = text,
                x = x.coerceIn(60f, width - 60f),
                y = y,
                color = color
            )
        )
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= p.decay
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val ft = iterator.next()
            ft.y += ft.vy * dt
            ft.alpha -= 0.025f
            if (ft.alpha <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun checkAndSaveHighScore() {
        if (score > highScore) {
            highScore = score
            prefs.edit().putInt("high_score", highScore).apply()
        }
    }
}
