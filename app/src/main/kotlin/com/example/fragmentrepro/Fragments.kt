package com.example.fragmentrepro

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.open_screen_with_pager_button).setOnClickListener {
            requireMainActivity().pushScreen(ScreenFragment.newInstance(hasViewPager = true))
        }
        view.findViewById<View>(R.id.open_screen_without_pager_button).setOnClickListener {
            requireMainActivity().pushScreen(ScreenFragment.newInstance(hasViewPager = false))
        }
    }
}

/**
 * When [hasViewPager] is true, the screen has ViewPager2 + FragmentStateAdapter, and the bug shows up as a crash.
 */
class ScreenFragment : Fragment(R.layout.fragment_screen) {
    private val hasViewPager: Boolean get() = requireArguments().getBoolean(ARG_HAS_VIEW_PAGER)
    val screenNumber: Int get() = requireArguments().getInt(ARG_SCREEN_NUMBER)

    fun createNextScreen(): ScreenFragment = newInstance(hasViewPager, screenNumber + 1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.screen_title).text = "Screen #$screenNumber"
        view.findViewById<View>(R.id.push_next_screen_button).setOnClickListener {
            Log.d(TAG, "Next clicked on Screen #$screenNumber (lifecycle=${lifecycle.currentState})")
            requireMainActivity().pushScreen(createNextScreen())
        }
        view.findViewById<View>(R.id.reproduce_button).setOnClickListener {
            Log.d(TAG, "Reproduce clicked on Screen #$screenNumber")
            requireMainActivity().reproduceWithBackEventsDispatchedManually(createNextScreen())
        }
        if (hasViewPager) {
            val pager = view.findViewById<ViewStub>(R.id.pager_stub).inflate() as ViewPager2
            pager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 3
                override fun createFragment(position: Int): Fragment = PageFragment.newInstance(position)
            }
        } else {
            view.findViewById<View>(R.id.without_pager_label).visibility = View.VISIBLE
        }
    }

    companion object {
        private const val ARG_HAS_VIEW_PAGER = "has_view_pager"
        private const val ARG_SCREEN_NUMBER = "screen_number"

        fun newInstance(hasViewPager: Boolean, screenNumber: Int = 1) = ScreenFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_HAS_VIEW_PAGER, hasViewPager)
                putInt(ARG_SCREEN_NUMBER, screenNumber)
            }
        }
    }
}

class PageFragment : Fragment(R.layout.fragment_label) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.label).text = "Page ${requireArguments().getInt(ARG_POSITION)}"
    }

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int) = PageFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POSITION, position) }
        }
    }
}

private fun Fragment.requireMainActivity(): MainActivity = requireActivity() as MainActivity
private const val TAG = "FragmentRepro"
