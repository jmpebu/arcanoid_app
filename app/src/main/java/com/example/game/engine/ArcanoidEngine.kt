package com.example.game.engine

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.game.audio.HapticSoundHelper
import com.example.game.level.LevelGenerator
import com.example.game.model.Ball
import com.example.game.model.Boss
import com.example.game.model.Brick
import com.example.game.model.BrickType
import com.example.game.model.EnemyProjectile
import com.example.game.model.FloatingText
import com.example.game.model.GameStatus
import com.example.game.model.MechProjectile
import com.example.game.model.Paddle
import com.example.game.model.Particle
import com.example.game.model.PowerUp
import com.example.game.model.PowerUpType
import com.example.game.model.Relic
import com.example.game.model.RelicType
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
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

    var renderTrigger by mutableLongStateOf(0L)

    var width: Float = 1000f
    var height: Float = 1600f

    val paddle: Paddle = Paddle(x = 500f, y = 1450f, width = 180f, baseWidth = 180f)
    val balls = mutableListOf<Ball>()
    val bricks = mutableListOf<Brick>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    // Roguelike & Combat Projectiles
    val mechProjectiles = mutableListOf<MechProjectile>()
    val enemyProjectiles = mutableListOf<EnemyProjectile>()
    var boss by mutableStateOf<Boss?>(null)
    var bossHp by mutableIntStateOf(35)
        private set
    var bossMaxHp by mutableIntStateOf(35)
        private set
    var bossName by mutableStateOf("NEXUS-V")
        private set
    var isBossActive by mutableStateOf(true)
        private set

    // Relics acquired during the run
    val activeRelics = mutableStateListOf<Relic>()
    var relicChoices by mutableStateOf<List<RelicType>>(emptyList())

    // Score & Run Stats
    var score by mutableIntStateOf(0)
        private set
    var highScore by mutableIntStateOf(prefs.getInt("high_score", 0))
        private set
    var lives by mutableIntStateOf(3)
        private set
    var maxLives by mutableIntStateOf(3)
        private set
    var level by mutableIntStateOf(1)
        private set
    var combo by mutableIntStateOf(0)
        private set

    // Shield States
    var bottomShieldActive by mutableStateOf(false)
        private set
    var quantumShieldActive by mutableStateOf(false)
        private set

    // Active Skill Cooldowns (Exposed to UI for circular meters)
    var cannonCooldownMax = 5.0f
    var cannonCooldownRemaining by mutableFloatStateOf(0f)
    val canFireCannons: Boolean
        get() = cannonCooldownRemaining <= 0f && (_gameStatus.value == GameStatus.PLAYING || _gameStatus.value == GameStatus.READY)

    var shieldCooldownMax = 18.0f
    var shieldCooldownRemaining by mutableFloatStateOf(0f)
    var shieldActiveTimer by mutableFloatStateOf(0f)
    val canActivateShield: Boolean
        get() = shieldCooldownRemaining <= 0f && !quantumShieldActive && (_gameStatus.value == GameStatus.PLAYING || _gameStatus.value == GameStatus.READY)

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

        paddle.y = height - 130f
        updatePaddleBaseWidth()

        if (isFirstInit || _gameStatus.value == GameStatus.READY) {
            paddle.x = width / 2f
            resetBallToPaddle()
            if (bricks.isEmpty()) {
                loadLevel(level)
            }
        }
    }

    private fun updatePaddleBaseWidth() {
        var base = (width * 0.28f).coerceIn(130f, 230f)
        if (hasRelic(RelicType.PADDLE_EXPANSION)) {
            base *= 1.2f
        }
        paddle.baseWidth = base
        paddle.width = if (widePaddleTimer > 0f) paddle.baseWidth * 1.4f else paddle.baseWidth
    }

    fun hasRelic(type: RelicType): Boolean {
        return activeRelics.any { it.type == type }
    }

    fun startOrLaunchBall() {
        if (_gameStatus.value == GameStatus.READY) {
            _gameStatus.value = GameStatus.PLAYING
            if (balls.isEmpty()) {
                resetBallToPaddle()
            }
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
        maxLives = 3
        level = 1
        combo = 0
        activeRelics.clear()
        relicChoices = emptyList()
        widePaddleTimer = 0f
        laserBallTimer = 0f
        slowBallTimer = 0f
        bottomShieldActive = false
        quantumShieldActive = false
        cannonCooldownRemaining = 0f
        shieldCooldownRemaining = 0f
        shieldActiveTimer = 0f
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
        mechProjectiles.clear()
        enemyProjectiles.clear()
        updatePaddleBaseWidth()
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
        quantumShieldActive = false
        powerUps.clear()
        particles.clear()
        floatingTexts.clear()
        mechProjectiles.clear()
        enemyProjectiles.clear()
        updatePaddleBaseWidth()
        paddle.x = width / 2f
        loadLevel(level)
        resetBallToPaddle()
        _gameStatus.value = GameStatus.READY
    }

    fun triggerRelicSelection() {
        // Pick 3 random distinct relics not already picked
        val available = RelicType.values().filter { !hasRelic(it) }
        val pool = if (available.isNotEmpty()) available else RelicType.values().toList()
        relicChoices = pool.shuffled().take(3)
        _gameStatus.value = GameStatus.RELIC_SELECTION
    }

    fun chooseRelic(type: RelicType) {
        activeRelics.add(Relic(type, level))
        soundHelper.onPowerUpCollected()

        // Apply immediate effects
        when (type) {
            RelicType.NANOBOT_ARMOR -> {
                maxLives++
                lives = (lives + 1).coerceAtMost(maxLives)
            }
            RelicType.PADDLE_EXPANSION -> {
                updatePaddleBaseWidth()
            }
            else -> {}
        }

        relicChoices = emptyList()
        nextLevel()
    }

    // --- ACTIVE SKILL: PLASMA CANNONS ---
    fun fireCannons() {
        if (!canFireCannons) return
        soundHelper.onCannonFire()

        val cooldownReduction = if (hasRelic(RelicType.NEURAL_COOLDOWN)) 0.65f else 1.0f
        cannonCooldownRemaining = cannonCooldownMax * cooldownReduction

        val halfW = paddle.width / 2f
        val wingOffset = halfW * 0.72f
        val cannonY = paddle.y - paddle.height / 2f - 4f

        if (hasRelic(RelicType.OVERCHARGED_CANNONS)) {
            // 4 plasma bolts
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x - wingOffset, cannonY))
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x - wingOffset * 0.45f, cannonY))
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x + wingOffset * 0.45f, cannonY))
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x + wingOffset, cannonY))
        } else {
            // Dual plasma bolts
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x - wingOffset, cannonY))
            mechProjectiles.add(MechProjectile(nextEntityId++, paddle.x + wingOffset, cannonY))
        }

        // Emit muzzle flash particles
        spawnExplosionParticles(paddle.x - wingOffset, cannonY, NeonYellow, 4)
        spawnExplosionParticles(paddle.x + wingOffset, cannonY, NeonYellow, 4)
    }

    // --- ACTIVE SKILL: QUANTUM SHIELD ---
    fun activateShield() {
        if (!canActivateShield) return
        soundHelper.onPowerUpCollected()

        val duration = if (hasRelic(RelicType.REFLECTIVE_SHIELD)) 8.0f else 5.0f
        shieldActiveTimer = duration
        quantumShieldActive = true
        bottomShieldActive = true

        val cooldownReduction = if (hasRelic(RelicType.NEURAL_COOLDOWN)) 0.65f else 1.0f
        shieldCooldownRemaining = shieldCooldownMax * cooldownReduction

        spawnExplosionParticles(paddle.x, paddle.y, NeonCyan, 16)
        addFloatingText("¡ESCUDO ACTIVO!", paddle.x, paddle.y - 30f, NeonCyan)
    }

    fun smoothMovePaddleTowards(targetX: Float, dt: Float) {
        val halfW = paddle.width / 2f
        val clampedTarget = targetX.coerceIn(halfW, width - halfW)

        val blend = 1f - exp(-22f * dt)
        paddle.x += (clampedTarget - paddle.x) * blend
        paddle.x = paddle.x.coerceIn(halfW, width - halfW)

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
        val newBoss = LevelGenerator.generateBoss(lvl, width, height)
        boss = newBoss
        bossHp = newBoss.maxHp
        bossMaxHp = newBoss.maxHp
        bossName = newBoss.name
        isBossActive = true
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

        // Update Timers for active powerups and skills
        if (cannonCooldownRemaining > 0f) {
            cannonCooldownRemaining = (cannonCooldownRemaining - dt).coerceAtLeast(0f)
        }
        if (shieldCooldownRemaining > 0f) {
            shieldCooldownRemaining = (shieldCooldownRemaining - dt).coerceAtLeast(0f)
        }
        if (shieldActiveTimer > 0f) {
            shieldActiveTimer -= dt
            if (shieldActiveTimer <= 0f) {
                quantumShieldActive = false
            }
        }

        if (widePaddleTimer > 0f) {
            widePaddleTimer -= dt
            if (widePaddleTimer <= 0f) {
                updatePaddleBaseWidth()
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

        // Update Boss AI Behavior & Projectiles
        updateBoss(dt)

        // Update Mech Plasma Projectiles
        updateMechProjectiles(dt)

        // Update Enemy Hostile Projectiles
        updateEnemyProjectiles(dt)

        // Update Balls
        val currentSpeedMultiplier = if (slowBallTimer > 0f) 0.65f else 1.0f
        val ballsIterator = balls.iterator()
        val ballsToRemove = mutableListOf<Ball>()

        while (ballsIterator.hasNext()) {
            val ball = ballsIterator.next()

            ball.trail.add(0, Pair(ball.x, ball.y))
            if (ball.trail.size > 6) {
                ball.trail.removeAt(ball.trail.size - 1)
            }

            ball.x += ball.vx * currentSpeedMultiplier * dt
            ball.y += ball.vy * currentSpeedMultiplier * dt

            // Wall Collisions
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

            // Top Ceiling Collision
            if (ball.y - ball.radius <= 0f) {
                ball.y = ball.radius
                ball.vy = abs(ball.vy)
                spawnWallParticles(ball.x, ball.y, NeonPink)
                soundHelper.onPaddleHit()
            }

            // Bottom Floor Collision
            if (ball.y + ball.radius >= height) {
                if (bottomShieldActive || quantumShieldActive) {
                    if (!quantumShieldActive) bottomShieldActive = false
                    ball.y = height - ball.radius - 8f
                    ball.vy = -abs(ball.vy)
                    soundHelper.onExplosion()
                    spawnExplosionParticles(width / 2f, height - 10f, NeonPurple, 15)
                    addFloatingText("¡ESCUDO REPELENTE!", width / 2f, height - 60f, NeonPurple)
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
                ball.y = paddleTop - ball.radius - 1f

                val hitFactor = ((ball.x - paddle.x) / halfPaddleW).coerceIn(-1f, 1f)
                val maxBounceAngle = 60f * (PI.toFloat() / 180f)
                val bounceAngle = hitFactor * maxBounceAngle

                val currentSpeed = sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
                val baseSpeed = (height * 0.44f).coerceIn(380f, 850f)
                val newSpeed = (currentSpeed * 1.01f).coerceIn(baseSpeed, baseSpeed * 1.5f)

                ball.vx = newSpeed * sin(bounceAngle)
                ball.vy = -newSpeed * cos(bounceAngle)

                combo = 0
                spawnWallParticles(ball.x, ball.y, NeonCyan)
                soundHelper.onPaddleHit()
            }

            // Collision with Boss
            checkBossBallCollision(ball)

            // Collision with Bricks
            checkBrickCollisions(ball)
        }

        balls.removeAll(ballsToRemove)

        // Ball loss check
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

            if (pu.y - pu.size > height) {
                powerUpIterator.remove()
            }
        }

        updateParticles(dt)
        updateFloatingTexts(dt)

        // Check if level/boss cleared
        val currentBoss = boss
        if (currentBoss != null && currentBoss.isDefeated) {
            val clearBonus = level * 1000
            score += clearBonus
            checkAndSaveHighScore()
            soundHelper.onLevelCleared()
            addFloatingText("¡JEFE ERRADICADO! +$clearBonus", width / 2f, height / 2f, NeonGold)
            triggerRelicSelection()
        }
    }

    private fun updateBoss(dt: Float) {
        val b = boss ?: return
        if (b.isDefeated) return

        // Side to side movement
        b.x += b.vx * dt
        val halfW = b.width / 2f
        val minX = halfW + width * 0.04f
        val maxX = width - halfW - width * 0.04f

        if (b.x <= minX) {
            b.x = minX
            b.vx = abs(b.vx)
        } else if (b.x >= maxX) {
            b.x = maxX
            b.vx = -abs(b.vx)
        }

        // Attack firing
        b.attackTimer += dt
        b.eyeGlowPhase += dt * 3.5f

        if (b.attackTimer >= b.attackInterval) {
            b.attackTimer = 0f
            // Boss attacks!
            spawnBossProjectiles(b)
        }
    }

    private fun spawnBossProjectiles(b: Boss) {
        val count = (1 + (level % 3)).coerceIn(1, 3)
        when (count) {
            1 -> {
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x,
                        y = b.y + b.height / 2f + 6f,
                        vx = 0f,
                        vy = 260f
                    )
                )
            }
            2 -> {
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x - b.width * 0.3f,
                        y = b.y + b.height / 2f + 6f,
                        vx = -40f,
                        vy = 260f
                    )
                )
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x + b.width * 0.3f,
                        y = b.y + b.height / 2f + 6f,
                        vx = 40f,
                        vy = 260f
                    )
                )
            }
            else -> {
                // 3 spread orbs
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x,
                        y = b.y + b.height / 2f + 6f,
                        vx = 0f,
                        vy = 270f
                    )
                )
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x - b.width * 0.35f,
                        y = b.y + b.height / 2f + 6f,
                        vx = -70f,
                        vy = 250f
                    )
                )
                enemyProjectiles.add(
                    EnemyProjectile(
                        id = nextEntityId++,
                        x = b.x + b.width * 0.35f,
                        y = b.y + b.height / 2f + 6f,
                        vx = 70f,
                        vy = 250f
                    )
                )
            }
        }
    }

    private fun updateMechProjectiles(dt: Float) {
        val iterator = mechProjectiles.iterator()
        val currentBoss = boss

        while (iterator.hasNext()) {
            val proj = iterator.next()
            proj.y += proj.vy * dt

            // Off-screen
            if (proj.y < -20f) {
                iterator.remove()
                continue
            }

            // Hit Boss
            if (currentBoss != null && !currentBoss.isDefeated) {
                val halfBW = currentBoss.width / 2f
                val halfBH = currentBoss.height / 2f
                if (proj.x >= currentBoss.x - halfBW &&
                    proj.x <= currentBoss.x + halfBW &&
                    proj.y >= currentBoss.y - halfBH &&
                    proj.y <= currentBoss.y + halfBH
                ) {
                    damageBoss(proj.damage)
                    spawnExplosionParticles(proj.x, proj.y, NeonYellow, 5)
                    iterator.remove()
                    continue
                }
            }

            // Hit Bricks
            var hitBrick = false
            for (brick in bricks) {
                if (brick.isDestroyed) continue
                if (proj.x >= brick.x && proj.x <= brick.x + brick.width &&
                    proj.y >= brick.y && proj.y <= brick.y + brick.height
                ) {
                    damageBrick(brick, proj.x, proj.y)
                    spawnExplosionParticles(proj.x, proj.y, NeonCyan, 4)
                    hitBrick = true
                    break
                }
            }
            if (hitBrick) {
                iterator.remove()
            }
        }
    }

    private fun updateEnemyProjectiles(dt: Float) {
        val iterator = enemyProjectiles.iterator()
        val paddleTop = paddle.y - paddle.height / 2f
        val paddleBottom = paddle.y + paddle.height / 2f
        val halfPaddleW = paddle.width / 2f

        while (iterator.hasNext()) {
            val ep = iterator.next()
            ep.x += ep.vx * dt
            ep.y += ep.vy * dt

            // Off-screen
            if (ep.y > height + 20f) {
                iterator.remove()
                continue
            }

            // Hit Shield
            if (quantumShieldActive) {
                val shieldRadius = paddle.width * 0.65f
                val dx = ep.x - paddle.x
                val dy = ep.y - paddle.y
                if (dx * dx + dy * dy <= shieldRadius * shieldRadius) {
                    soundHelper.onBrickHit()
                    spawnExplosionParticles(ep.x, ep.y, NeonCyan, 8)
                    iterator.remove()
                    continue
                }
            }

            // Hit Player Mech
            if (ep.y + ep.radius >= paddleTop &&
                ep.y - ep.radius <= paddleBottom &&
                ep.x >= paddle.x - halfPaddleW &&
                ep.x <= paddle.x + halfPaddleW
            ) {
                lives--
                soundHelper.onBallLost()
                spawnExplosionParticles(ep.x, ep.y, NeonRed, 14)
                addFloatingText("¡IMPACTO!", paddle.x, paddle.y - 40f, NeonRed)
                iterator.remove()
                if (lives <= 0) {
                    _gameStatus.value = GameStatus.GAME_OVER
                    checkAndSaveHighScore()
                }
            }
        }
    }

    private fun checkBossBallCollision(ball: Ball) {
        val b = boss ?: return
        if (b.isDefeated) return

        val halfBW = b.width / 2f
        val halfBH = b.height / 2f
        val closestX = ball.x.coerceIn(b.x - halfBW, b.x + halfBW)
        val closestY = ball.y.coerceIn(b.y - halfBH, b.y + halfBH)

        val distX = ball.x - closestX
        val distY = ball.y - closestY

        if (distX * distX + distY * distY <= ball.radius * ball.radius) {
            // Reflect ball downwards
            ball.vy = abs(ball.vy)
            ball.y = b.y + halfBH + ball.radius + 1f

            val damage = if (hasRelic(RelicType.HEAVY_BALL_FUSION)) 6 else 3
            damageBoss(damage)
            spawnExplosionParticles(ball.x, ball.y, b.color, 12)
        }
    }

    private fun damageBoss(dmg: Int) {
        val b = boss ?: return
        if (b.isDefeated) return

        val newHp = (bossHp - dmg).coerceAtLeast(0)
        bossHp = newHp
        b.currentHp = newHp
        soundHelper.onBossDamaged()

        val pointsEarned = dmg * 50
        score += pointsEarned
        checkAndSaveHighScore()
        addFloatingText("-$dmg", b.x + (rng.nextFloat() * 40f - 20f), b.y, NeonGold)

        if (bossHp <= 0) {
            b.isDefeated = true
            isBossActive = false
            soundHelper.onBossDefeated()
            spawnExplosionParticles(b.x, b.y, NeonRed, 35)
            spawnExplosionParticles(b.x, b.y, NeonYellow, 25)
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
        val dmg = if (hasRelic(RelicType.HEAVY_BALL_FUSION)) 2 else 1
        brick.hitsRemaining -= dmg
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

            if (brick.type == BrickType.EXPLOSIVE) {
                soundHelper.onExplosion()
                triggerExplosionAround(brick)
            }

            if (brick.type == BrickType.POWERUP || rng.nextFloat() < 0.18f) {
                spawnPowerUp(brick.x + brick.width / 2f, brick.y + brick.height / 2f)
            }
        } else {
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
                paddle.width = paddle.baseWidth * 1.4f
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
                if (lives < maxLives + 2) lives++
            }
            PowerUpType.BOTTOM_SHIELD -> {
                bottomShieldActive = true
            }
        }
    }

    private fun spawnExplosionParticles(x: Float, y: Float, color: Color, count: Int) {
        if (particles.size > 50) return
        val clampedCount = count.coerceAtMost(16)
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
        if (particles.size > 50) return
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
        if (floatingTexts.size > 5) floatingTexts.removeAt(0)
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
