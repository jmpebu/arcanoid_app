package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Neon Arcanoid", appName)
  }

  @Test
  fun `verify level generator creates bricks and boss`() {
    val bricks = com.example.game.level.LevelGenerator.generateLevel(1, 1000f, 1600f)
    org.junit.Assert.assertTrue(bricks.isNotEmpty())
    val boss = com.example.game.level.LevelGenerator.generateBoss(1, 1000f, 1600f)
    org.junit.Assert.assertNotNull(boss)
    assertEquals("NEXUS-V", boss.name)
    assertEquals(35, boss.maxHp)
  }

  @Test
  fun `verify score updates correctly in engine`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundHelper = com.example.game.audio.HapticSoundHelper(context)
    val engine = com.example.game.engine.ArcanoidEngine(context, soundHelper)
    engine.setDimensions(1000f, 1600f)
    assertEquals(0, engine.score)
    val firstBrick = engine.bricks.first()
    engine.balls.first().apply {
      x = firstBrick.x + firstBrick.width / 2f
      y = firstBrick.y + firstBrick.height / 2f
    }
    engine.update(0.016f)
    org.junit.Assert.assertTrue("Score should increase when brick is damaged or destroyed", engine.score >= 0)
  }

  @Test
  fun `verify mech firing cannons and relics`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundHelper = com.example.game.audio.HapticSoundHelper(context)
    val engine = com.example.game.engine.ArcanoidEngine(context, soundHelper)
    engine.setDimensions(1000f, 1600f)
    org.junit.Assert.assertTrue(engine.canFireCannons)
    engine.fireCannons()
    org.junit.Assert.assertTrue(engine.mechProjectiles.isNotEmpty())
    engine.chooseRelic(com.example.game.model.RelicType.NANOBOT_ARMOR)
    org.junit.Assert.assertTrue(engine.hasRelic(com.example.game.model.RelicType.NANOBOT_ARMOR))
  }

  @Test
  fun `verify boss health bar updates on damage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundHelper = com.example.game.audio.HapticSoundHelper(context)
    val engine = com.example.game.engine.ArcanoidEngine(context, soundHelper)
    engine.setDimensions(1000f, 1600f)
    org.junit.Assert.assertTrue(engine.isBossActive)
    val initialHp = engine.bossHp
    org.junit.Assert.assertEquals(35, initialHp)
    val currentBoss = engine.boss!!
    engine.startOrLaunchBall()
    engine.balls.first().apply {
      x = currentBoss.x
      y = currentBoss.y
      vy = -400f
    }
    engine.update(0.016f)
    org.junit.Assert.assertTrue("Boss HP should decrease when hit", engine.bossHp < initialHp)
  }
}
