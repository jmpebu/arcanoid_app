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
  fun `verify level generator creates bricks`() {
    val bricks = com.example.game.level.LevelGenerator.generateLevel(1, 1000f, 1600f)
    org.junit.Assert.assertTrue(bricks.isNotEmpty())
    org.junit.Assert.assertEquals(35, bricks.size)
  }
}
