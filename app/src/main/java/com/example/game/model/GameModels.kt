package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
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
    val height: Float = 28f,
    var baseWidth: Float = 170f,
    val color: Color = NeonCyan
)

enum class PowerUpType(val label: String, val icon: String, val color: Color) {
    WIDE_PADDLE("Mecha +", "↔", NeonCyan),
    MULTI_BALL("Multi Bola", "3x", NeonPink),
    SLOW_BALL("Ralentizar", "⏱", NeonGreen),
    LASER_BALL("Super Bola", "⚡", NeonYellow),
    EXTRA_LIFE("+1 Blindaje", "♥", NeonPink),
    BOTTOM_SHIELD("Escudo Neón", "🛡", NeonPurple)
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
    RELIC_SELECTION,
    LEVEL_CLEARED,
    GAME_OVER
}

enum class ControlMode(val displayName: String) {
    ACCELEROMETER("Acelerómetro"),
    TOUCH("Táctil"),
    HYBRID("Híbrido (Ambos)")
}

// --- ROGUELIKE & BOSS ENTITIES ---

data class Boss(
    val id: Int,
    val name: String,
    val subtitle: String,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var maxHp: Int,
    var currentHp: Int,
    var vx: Float = 120f,
    var attackTimer: Float = 0f,
    val attackInterval: Float = 3.0f,
    val color: Color = NeonRed,
    var isDefeated: Boolean = false,
    var eyeGlowPhase: Float = 0f
) {
    val hpFraction: Float
        get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
}

data class MechProjectile(
    val id: Int,
    var x: Float,
    var y: Float,
    val vy: Float = -750f,
    val radius: Float = 5f,
    val damage: Int = 1,
    var isAlive: Boolean = true
)

data class EnemyProjectile(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 260f,
    val radius: Float = 8f,
    val color: Color = NeonRed,
    var isAlive: Boolean = true
)

enum class RelicType(
    val title: String,
    val description: String,
    val icon: String,
    val rarity: String,
    val color: Color
) {
    OVERCHARGED_CANNONS(
        "Cañones Cuádruples",
        "Dispara 4 proyectiles de plasma en lugar de 2",
        "⚡⚡",
        "RARA",
        NeonYellow
    ),
    NEURAL_COOLDOWN(
        "Refrigeración Cuántica",
        "-35% tiempo de recarga en todas las habilidades activas",
        "⏱",
        "RARA",
        NeonCyan
    ),
    NANOBOT_ARMOR(
        "Nanobots Regenerativos",
        "+1 vida máxima y repara 1 vida de inmediato",
        "🛡+",
        "ÉPICA",
        NeonGreen
    ),
    HEAVY_BALL_FUSION(
        "Fusión de Plasma",
        "La bola causa +100% de daño a jefes y rompe ladrillos duros en 1 golpe",
        "☄",
        "ÉPICA",
        NeonPink
    ),
    PADDLE_EXPANSION(
        "Alas de Asalto",
        "Aumenta permanentemente el ancho del mecha un +20%",
        "↔",
        "COMÚN",
        NeonCyan
    ),
    REFLECTIVE_SHIELD(
        "Escudo Reflector",
        "El escudo dura +3s y destruye proyectiles hostiles al contacto",
        "🛡✨",
        "ÉPICA",
        NeonPurple
    )
}

data class Relic(
    val type: RelicType,
    val acquiredAtLevel: Int
)
