package com.example.game.level

import androidx.compose.ui.graphics.Color
import com.example.game.model.Boss
import com.example.game.model.Brick
import com.example.game.model.BrickType
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import kotlin.random.Random

object LevelGenerator {

    fun generateBoss(levelNumber: Int, playAreaWidth: Float, playAreaHeight: Float): Boss {
        val bossY = playAreaHeight * 0.08f
        return when (levelNumber) {
            1 -> Boss(
                id = 1,
                name = "NEXUS-V",
                subtitle = "Núcleo Corrupto del Firewall",
                x = playAreaWidth / 2f,
                y = bossY,
                width = (playAreaWidth * 0.32f).coerceIn(110f, 200f),
                height = 48f,
                maxHp = 35,
                currentHp = 35,
                vx = 110f,
                attackInterval = 2.8f,
                color = NeonRed
            )
            2 -> Boss(
                id = 2,
                name = "OVERLORD-KRNL",
                subtitle = "Centinela Cuántico Autónomo",
                x = playAreaWidth / 2f,
                y = bossY,
                width = (playAreaWidth * 0.38f).coerceIn(130f, 230f),
                height = 54f,
                maxHp = 65,
                currentHp = 65,
                vx = 135f,
                attackInterval = 2.2f,
                color = NeonPurple
            )
            3 -> Boss(
                id = 3,
                name = "ZERO-NEURAL",
                subtitle = "Singularidad Cibernética Rogue",
                x = playAreaWidth / 2f,
                y = bossY,
                width = (playAreaWidth * 0.44f).coerceIn(150f, 260f),
                height = 60f,
                maxHp = 100,
                currentHp = 100,
                vx = 160f,
                attackInterval = 1.7f,
                color = NeonOrange
            )
            else -> Boss(
                id = levelNumber,
                name = "LEVIATHAN-${levelNumber * 7}",
                subtitle = "Megared Hostil Sector $levelNumber",
                x = playAreaWidth / 2f,
                y = bossY,
                width = (playAreaWidth * 0.42f).coerceIn(140f, 250f),
                height = 56f,
                maxHp = 70 + (levelNumber * 25),
                currentHp = 70 + (levelNumber * 25),
                vx = 130f + (levelNumber * 8f),
                attackInterval = (2.2f - (levelNumber * 0.1f)).coerceAtLeast(1.2f),
                color = NeonRed
            )
        }
    }

    fun generateLevel(levelNumber: Int, playAreaWidth: Float, playAreaHeight: Float): List<Brick> {
        val bricks = mutableListOf<Brick>()
        // Leave room at the top for the boss
        val topMargin = playAreaHeight * 0.17f
        val sideMargin = playAreaWidth * 0.04f
        val usableWidth = playAreaWidth - (sideMargin * 2f)

        when (levelNumber) {
            1 -> {
                // Firewall Grid - 4 rows, 6 columns
                val rows = 4
                val cols = 6
                val spacing = 6f
                val brickWidth = (usableWidth - (spacing * (cols - 1))) / cols
                val brickHeight = 26f

                val rowColors = listOf(NeonPink, NeonOrange, NeonYellow, NeonCyan)

                var id = 0
                for (r in 0 until rows) {
                    val color = rowColors[r]
                    for (c in 0 until cols) {
                        val bx = sideMargin + c * (brickWidth + spacing)
                        val by = topMargin + r * (brickHeight + spacing)
                        val isPowerup = (r == 1 && (c == 2 || c == 3)) || (r == 3 && c == 4)
                        val type = if (isPowerup) BrickType.POWERUP else BrickType.NORMAL

                        bricks.add(
                            Brick(
                                id = id++,
                                x = bx,
                                y = by,
                                width = brickWidth,
                                height = brickHeight,
                                hitsRemaining = 1,
                                maxHits = 1,
                                baseColor = color,
                                type = type,
                                points = (4 - r) * 10
                            )
                        )
                    }
                }
            }

            2 -> {
                // Cyber Fortified Mesh - 5 rows, 7 columns
                val rows = 5
                val cols = 7
                val spacing = 6f
                val brickWidth = (usableWidth - (spacing * (cols - 1))) / cols
                val brickHeight = 24f

                var id = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (r == 0 && (c == 0 || c == 6)) continue

                        val bx = sideMargin + c * (brickWidth + spacing)
                        val by = topMargin + r * (brickHeight + spacing)

                        val isExplosive = (r == 2 && (c == 0 || c == 6))
                        val isTough = r in 1..2 && c in 2..4

                        val (type, maxHits, color, points) = when {
                            isExplosive -> Quad(BrickType.EXPLOSIVE, 1, NeonOrange, 30)
                            isTough -> Quad(BrickType.TOUGH, 2, NeonPurple, 25)
                            (r + c) % 3 == 0 -> Quad(BrickType.POWERUP, 1, NeonYellow, 20)
                            else -> Quad(BrickType.NORMAL, 1, NeonCyan, 15)
                        }

                        bricks.add(
                            Brick(
                                id = id++,
                                x = bx,
                                y = by,
                                width = brickWidth,
                                height = brickHeight,
                                hitsRemaining = maxHits,
                                maxHits = maxHits,
                                baseColor = color,
                                type = type,
                                points = points
                            )
                        )
                    }
                }
            }

            3 -> {
                // Singularity Shield - 5 rows, 7 columns
                val rows = 5
                val cols = 7
                val spacing = 6f
                val brickWidth = (usableWidth - (spacing * (cols - 1))) / cols
                val brickHeight = 24f

                val midRow = rows / 2
                val midCol = cols / 2

                var id = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val distFromCenter = kotlin.math.abs(r - midRow) + kotlin.math.abs(c - midCol)
                        if (distFromCenter > 3) continue

                        val bx = sideMargin + c * (brickWidth + spacing)
                        val by = topMargin + r * (brickHeight + spacing)

                        val isCore = distFromCenter == 0
                        val isExplosive = distFromCenter == 2 && (r == midRow || c == midCol)
                        val isPowerup = distFromCenter == 3

                        val (type, maxHits, color, points) = when {
                            isCore -> Quad(BrickType.ARMORED, 3, NeonGold, 50)
                            isExplosive -> Quad(BrickType.EXPLOSIVE, 1, NeonOrange, 35)
                            isPowerup -> Quad(BrickType.POWERUP, 1, NeonGreen, 25)
                            distFromCenter == 1 -> Quad(BrickType.TOUGH, 2, NeonPurple, 30)
                            else -> Quad(BrickType.NORMAL, 1, NeonPink, 20)
                        }

                        bricks.add(
                            Brick(
                                id = id++,
                                x = bx,
                                y = by,
                                width = brickWidth,
                                height = brickHeight,
                                hitsRemaining = maxHits,
                                maxHits = maxHits,
                                baseColor = color,
                                type = type,
                                points = points
                            )
                        )
                    }
                }
            }

            else -> {
                // Procedural Higher Levels
                val rows = (4 + (levelNumber % 3)).coerceAtMost(6)
                val cols = 7
                val spacing = 6f
                val brickWidth = (usableWidth - (spacing * (cols - 1))) / cols
                val brickHeight = 24f

                val rng = Random(levelNumber * 42L)
                val palette = listOf(NeonPink, NeonCyan, NeonPurple, NeonGreen, NeonOrange, NeonYellow)

                var id = 0
                for (r in 0 until rows) {
                    val rowColor = palette[(r + levelNumber) % palette.size]
                    for (c in 0 until cols) {
                        val bx = sideMargin + c * (brickWidth + spacing)
                        val by = topMargin + r * (brickHeight + spacing)

                        val roll = rng.nextFloat()
                        val (type, maxHits, color, points) = when {
                            roll < 0.12f -> Quad(BrickType.EXPLOSIVE, 1, NeonOrange, 40)
                            roll < 0.25f -> Quad(BrickType.POWERUP, 1, NeonYellow, 25)
                            roll < 0.50f -> Quad(BrickType.TOUGH, 2, NeonPurple, 30)
                            roll < 0.65f && levelNumber >= 4 -> Quad(BrickType.ARMORED, 3, NeonGold, 50)
                            else -> Quad(BrickType.NORMAL, 1, rowColor, 15)
                        }

                        bricks.add(
                            Brick(
                                id = id++,
                                x = bx,
                                y = by,
                                width = brickWidth,
                                height = brickHeight,
                                hitsRemaining = maxHits,
                                maxHits = maxHits,
                                baseColor = color,
                                type = type,
                                points = points
                            )
                        )
                    }
                }
            }
        }

        return bricks
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
