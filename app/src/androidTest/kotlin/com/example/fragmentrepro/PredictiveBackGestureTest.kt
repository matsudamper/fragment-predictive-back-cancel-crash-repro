package com.example.fragmentrepro

import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
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
    private var screenRecording: ParcelFileDescriptor? = null
    private var previousExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        enableGestureNavigationAndPredictiveBack()
        startScreenRecording()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.findViewById<View>(R.id.open_screen_with_pager_button).performClick() }
        instrumentation.waitForIdleSync()
        pushScreensSoThatCompletedBackGesturesDoNotReachHome()
    }

    @After
    fun tearDown() {
        stopScreenRecording()
        Thread.setDefaultUncaughtExceptionHandler(previousExceptionHandler)
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
        shell("setprop log.tag.FragmentManager VERBOSE")
        waitForNavigationModeToBeAppliedToSystemUi()
        assertEquals("Gesture Navigation not enabled.", "2", shell("settings get secure navigation_mode"))
    }

    private fun pushScreensSoThatCompletedBackGesturesDoNotReachHome() {
        repeat(MAX_ATTEMPTS) {
            scenario.onActivity { findNextButton(it).performClick() }
            instrumentation.waitForIdleSync()
        }
    }

    private fun flickNextButtonFromRightEdge() {
        var y = 0
        var screenWidth = 0
        scenario.onActivity { activity ->
            val nextButton = findNextButton(activity)
            val location = IntArray(2).also { nextButton.getLocationOnScreen(it) }
            y = location[1] + nextButton.height / 2
            screenWidth = activity.resources.displayMetrics.widthPixels
        }
        val flick = if (Build.VERSION.SDK_INT >= 35) FLICK_ON_ANDROID_15_AND_LATER else FLICK_ON_ANDROID_14
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val startX = screenWidth - 2
        val endX = startX - (flick.distanceDp * density).toInt()
        shell("input swipe $startX $y $endX $y ${flick.durationMs}")
    }


    private fun startScreenRecording() {
        // Files in this directory are copied to the host after the test run
        val dir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir") ?: "/sdcard/Download"
        val file = "$dir/flick-api${Build.VERSION.SDK_INT}.mp4"
        Log.d(TAG, "Recording the screen to $file")
        screenRecording = instrumentation.uiAutomation.executeShellCommand("screenrecord --size 352x784 --bit-rate 300000 $file")
        // The bug kills this process. Stop the recording before that, or the mp4 cannot be played.
        previousExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            stopScreenRecording()
            previousExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun stopScreenRecording() {
        val recording = screenRecording ?: return
        screenRecording = null
        shell("pkill -INT screenrecord")
        // Reading to the end waits until screenrecord has finished writing the file
        ParcelFileDescriptor.AutoCloseInputStream(recording).use { it.readBytes() }
    }

    private fun findNextButton(activity: MainActivity): View {
        val screen = checkNotNull(activity.supportFragmentManager.findFragmentById(R.id.container))
        return screen.requireView().findViewById(R.id.push_next_screen_button)
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

    private class Flick(val distanceDp: Float, val durationMs: Int)

    private companion object {
        const val TAG = "FragmentRepro"
        const val MAX_ATTEMPTS = 50

        // Reproduced most often on a real device (ASUS_AI2202, Android 14, 440dpi): 18px in 15ms
        val FLICK_ON_ANDROID_14 = Flick(distanceDp = 18 / 2.75f, durationMs = 15)

        // From Android 15, the system often does not send onBackStarted for the short flick above.
        // Reproduced most often on the Pixel 6 emulator (Android 17, 420dpi): 50px in 15ms. Also reproduces on Android 15 and 16
        val FLICK_ON_ANDROID_15_AND_LATER = Flick(distanceDp = 50 / 2.625f, durationMs = 15)
    }
}
