package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

enum class BrickType {
    NORMAL,
    TOUGH,       // Takes 2 hits
    EXPLOSIVE,   // Explodes adjacent bricks
    ARMORED,     // Takes 3 hits
    POWERUP      // Guaranteed power-up drop
}

data class Brick(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    var hitsRemaining: Int,
    val maxHits: Int = 1,
    val baseColor: Color,
    val type: BrickType = BrickType.NORMAL,
    val points: Int = 10,
    var isDestroyed: Boolean = false
) {
    val currentGlowAlpha: Float
        get() = (hitsRemaining.toFloat() / maxHits.toFloat()).coerceIn(0.3f, 1f)
}

data class Ball(
    val id: Int = 0,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float = 16f,
    var isPiercing: Boolean = false,
    val trail: MutableList<Pair<Float, Float>> = mutableListOf()
)

data class Paddle(
    var x: Float,
    var y: Float,
    var width: Float,
    val height: Float = 24f,
    var baseWidth: Float = 160f,
    val color: Color = NeonCyan
)

enum class PowerUpType(val label: String, val icon: String, val color: Color) {
    WIDE_PADDLE("Paleta +", "↔", NeonCyan),
    MULTI_BALL("Multi Bola", "3x", NeonPink),
    SLOW_BALL("Ralentizar", "⏱", NeonGreen),
    LASER_BALL("Super Bola", "⚡", NeonYellow),
    EXTRA_LIFE("+1 Vida", "♥", NeonPink),
    BOTTOM_SHIELD("Escudo", "🛡", NeonPurple)
}

data class PowerUp(
    val id: Int,
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    val vy: Float = 180f,
    val size: Float = 32f,
    var collected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var size: Float,
    var life: Float = 1f,
    val decay: Float = 0.03f
)

data class FloatingText(
    val id: Int,
    val text: String,
    var x: Float,
    var y: Float,
    var vy: Float = -60f,
    var alpha: Float = 1f,
    val color: Color = NeonGold
)

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    LEVEL_CLEARED,
    GAME_OVER
}

enum class ControlMode(val displayName: String) {
    ACCELEROMETER("Acelerómetro"),
    TOUCH("Táctil"),
    HYBRID("Híbrido (Ambos)")
}
