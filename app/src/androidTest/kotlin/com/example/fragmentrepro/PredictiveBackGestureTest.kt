package com.example.fragmentrepro

import android.os.ParcelFileDescriptor
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * When the bug happens, IllegalStateException is thrown on the main thread and the process crashes.
 * AndroidJUnitRunner reports that exception as the test failure.
 */
@RunWith(AndroidJUnit4::class)
class PredictiveBackGestureTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        enableGestureNavigationAndPredictiveBack()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.findViewById<View>(R.id.open_screen_with_pager_button).performClick() }
        instrumentation.waitForIdleSync()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun flickNextButtonFromRightEdgeDoesNotCrash() {
        repeat(MAX_ATTEMPTS) {
            flickNextButtonFromRightEdge()
            waitForBackCancelAnimationToFinish()
        }
    }

    private fun enableGestureNavigationAndPredictiveBack() {
        shell("cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural")
        shell("settings put global enable_back_animation 1")
        waitForNavigationModeToBeAppliedToSystemUi()
        assertEquals("Gesture Navigation not enabled.", "2", shell("settings get secure navigation_mode"))
    }

    private fun flickNextButtonFromRightEdge() {
        var y = 0
        var screenWidth = 0
        scenario.onActivity { activity ->
            val screen = checkNotNull(activity.supportFragmentManager.findFragmentById(R.id.container))
            val nextButton = screen.requireView().findViewById<View>(R.id.push_next_screen_button)
            val location = IntArray(2).also { nextButton.getLocationOnScreen(it) }
            y = location[1] + nextButton.height / 2
            screenWidth = activity.resources.displayMetrics.widthPixels
        }
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val startX = screenWidth - 2
        val endX = startX - (FLICK_DISTANCE_DP_ON_REPRODUCED_DEVICE * density).toInt()
        shell("input swipe $startX $y $endX $y $FLICK_DURATION_MS_ON_REPRODUCED_DEVICE")
    }

    private fun waitForNavigationModeToBeAppliedToSystemUi() {
        Thread.sleep(2_000)
    }

    private fun waitForBackCancelAnimationToFinish() {
        Thread.sleep(600)
        instrumentation.waitForIdleSync()
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .use { it.readText().trim() }

    private companion object {
        const val MAX_ATTEMPTS = 20

        // The flick that reproduced the bug most often on a real device (ASUS_AI2202, 440dpi): 18px in 15ms
        const val FLICK_DISTANCE_DP_ON_REPRODUCED_DEVICE = 18 / 2.75f
        const val FLICK_DURATION_MS_ON_REPRODUCED_DEVICE = 15
    }
}
