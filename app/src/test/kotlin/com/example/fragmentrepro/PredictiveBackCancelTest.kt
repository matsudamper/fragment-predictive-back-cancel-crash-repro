package com.example.fragmentrepro

import android.os.Looper
import androidx.activity.BackEventCompat
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PredictiveBackCancelTest {

    // Use a plain FragmentActivity so the test does not depend on the demo MainActivity
    private lateinit var activity: FragmentActivity
    private val fm: FragmentManager get() = activity.supportFragmentManager

    /** Fragment instances that got onCreate again after onDestroy */
    private val revivedFragments = mutableListOf<String>()

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        activity.setContentView(FrameLayout(activity).apply { id = R.id.container })
        val destroyed = mutableSetOf<Fragment>()
        fm.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                    if (f in destroyed) revivedFragments += describe(f)
                }

                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    destroyed += f
                }
            },
            false,
        )
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.container, HomeFragment(), "home")
            .commitNow()
    }

    private fun pushScreen(screen: ScreenFragment) {
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, screen)
            .addToBackStack(null)
            .commit()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun backStarted() = activity.onBackPressedDispatcher.dispatchOnBackStarted(
        BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_RIGHT),
    )

    private fun backCancelled() = activity.onBackPressedDispatcher.dispatchOnBackCancelled()

    private fun describe(f: Fragment) = if (f is ScreenFragment) "Screen #${f.screenNumber}" else f.javaClass.simpleName

    private fun shownFragments() = fm.fragments.map { describe(it) }

    private fun openScreen(hasViewPager: Boolean): ScreenFragment {
        val screen = ScreenFragment.newInstance(hasViewPager)
        pushScreen(screen)
        idle()
        assertEquals(listOf("Screen #1"), shownFragments())
        return screen
    }

    private fun pressBack() {
        activity.onBackPressedDispatcher.onBackPressed()
        idle()
    }

    private fun assertNavigatedToNextScreen() {
        assertEquals("A destroyed Fragment instance was created again", emptyList<String>(), revivedFragments)
        assertEquals(listOf("Screen #2"), shownFragments())
        pressBack()
        assertEquals(listOf("Screen #1"), shownFragments())
        pressBack()
        assertEquals(listOf("HomeFragment"), shownFragments())
    }

    /**
     * For comparison (passes): navigate AFTER FragmentManager has handled the back start.
     */
    @Test
    fun commitAfterBackStartedIsExecuted() {
        val screen = openScreen(hasViewPager = false)

        backStarted()
        idle()
        pushScreen(screen.createNextScreen())
        idle()
        backCancelled()
        idle()

        assertNavigatedToNextScreen()
    }

    /**
     * Bug (fails): back start and navigation commit happen in the same frame, then back is cancelled.
     *
     * Actual result:
     * - Screen #1 gets onDestroy before the cancel
     * - On cancel, the destroyed Screen #1 instance gets onCreate again and is shown together with HomeFragment
     * - Screen #2 is not shown. Pressing back shows Screen #2
     */
    @Test
    fun commitInSameFrameAsBackStarted() {
        val screen = openScreen(hasViewPager = false)

        backStarted()
        pushScreen(screen.createNextScreen())
        idle()
        backCancelled()
        idle()

        assertNavigatedToNextScreen()
    }

    /**
     * Bug (fails): same as above, with a screen that has ViewPager2 + FragmentStateAdapter.
     *
     * The revived Screen #1 restores its old view state. FragmentStateAdapter then looks up a child Fragment
     * that no longer exists, and IllegalStateException (Fragment no longer exists for key f#0) is thrown.
     */
    @Test
    fun commitInSameFrameAsBackStarted_withViewPager2() {
        val screen = openScreen(hasViewPager = true)

        backStarted()
        pushScreen(screen.createNextScreen())
        idle()
        backCancelled()
        idle()

        assertNavigatedToNextScreen()
    }
}
