package com.example.fragmentrepro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.BackEventCompat
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.container, HomeFragment(), "home")
                .commit()
        }
    }

    fun pushScreen(screen: ScreenFragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, screen, "screen")
            .addToBackStack("screen")
            .commit()
    }

    /**
     * Simulates tapping the Next button at the screen edge, where the system back gesture and the click happen at the same time.
     */
    fun reproduceWithBackEventsDispatchedManually(nextScreen: ScreenFragment) {
        onBackPressedDispatcher.dispatchOnBackStarted(
            BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_RIGHT),
        )
        pushScreen(nextScreen)
        dispatchBackCancelledAfterPendingTransactionsAreExecuted()
    }

    private fun dispatchBackCancelledAfterPendingTransactionsAreExecuted() {
        Handler(Looper.getMainLooper()).post {
            onBackPressedDispatcher.dispatchOnBackCancelled()
        }
    }
}
