# Fragment Predictive Back Crash Repro

Tapping a button at the screen edge, where the back gesture also starts, breaks the FragmentManager back stack and can crash the app.

## Environment

androidx.fragment: 1.9.1
androidx.activity: 1.13.0
androidx.viewpager2: 1.1.0
enableOnBackInvokedCallback="true"
Android OS 14+

## Steps to reproduce
- Start(Home)
- Navigate to Screen#1
- Press Down navigate to Screen#2 button
- fire `OnBackPressedCallback.handleOnBackStarted`
  - Occurs when I try to press a button at the edge of the screen.
- Release navigate to Screen#2 button(Quickly)
- call: `FragmentTransaction.commit()`
  - The button is clicked and navigation occurs.
- internal processing: `FragmentManager.execPendingActions()`
- fire: `OnBackPressedCallback.handleOnBackCancelled`
- internal processing: `FragmentManager.cancelBackStackTransition()`

What happens
- FragmentManager Stack is broken.
  - expected
    - Screen#2 is shown.
    - Back: Screen#2 → Screen#1 → Home
  - actual
    - Home is shown.
    - Back: Home → Screen#2 → Home
- Crash occurs when Screen#1 has ViewPager2.

## StackTrace (has ViewPager2)

```
java.lang.IllegalStateException: Fragment no longer exists for key f#0: unique id 0d9bc5b4-fb53-4216-b531-a50eb2127e91
	at androidx.fragment.app.FragmentManager.getFragment(FragmentManager.java:1274)
	at androidx.viewpager2.adapter.FragmentStateAdapter.restoreState(FragmentStateAdapter.java:565)
	at androidx.viewpager2.widget.ViewPager2.restorePendingState(ViewPager2.java:356)
	at androidx.viewpager2.widget.ViewPager2.dispatchRestoreInstanceState(ViewPager2.java:381)
	at android.view.ViewGroup.dispatchRestoreInstanceState(ViewGroup.java:4015)
	at android.view.View.restoreHierarchyState(View.java:23115)
	at androidx.fragment.app.Fragment.restoreViewState(Fragment.java:708)
	at androidx.fragment.app.Fragment.restoreViewState(Fragment.java:3154)
	at androidx.fragment.app.Fragment.performActivityCreated(Fragment.java:3139)
	at androidx.fragment.app.FragmentStateManager.activityCreated(FragmentStateManager.java:638)
	at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:288)
	at androidx.fragment.app.FragmentManager.executeOpsTogether(FragmentManager.java:2193)
	at androidx.fragment.app.FragmentManager.removeRedundantOperationsAndExecute(FragmentManager.java:2094)
	at androidx.fragment.app.FragmentManager.execPendingActions(FragmentManager.java:2031)
	at androidx.fragment.app.FragmentManager.executePendingTransactions(FragmentManager.java:774)
	at androidx.fragment.app.FragmentManager.cancelBackStackTransition(FragmentManager.java:1067)
	at androidx.fragment.app.FragmentManager$1.handleOnBackCancelled(FragmentManager.java:582)
	at androidx.activity.OnBackPressedCallback$OnBackPressedEventHandler.onBackCancelled(OnBackPressedCallback.kt:176)
	at androidx.navigationevent.NavigationEventHandler.doOnBackCancelled$navigationevent(NavigationEventHandler.kt:265)
	at androidx.navigationevent.NavigationEventProcessor.dispatchOnCancelled(NavigationEventProcessor.kt:523)
	at androidx.navigationevent.NavigationEventDispatcher.dispatchOnCancelled$navigationevent(NavigationEventDispatcher.kt:391)
	at androidx.navigationevent.NavigationEventInput.dispatchOnBackCancelled(NavigationEventInput.kt:199)
	at androidx.activity.OnBackPressedDispatcher$OnBackPressedEventInput.backCancelled(OnBackPressedDispatcher.kt:298)
	at androidx.activity.OnBackPressedDispatcher.dispatchOnBackCancelled(OnBackPressedDispatcher.kt:254)
	at com.example.fragmentrepro.PredictiveBackCancelTest.backCancelled(PredictiveBackCancelTest.kt:68)
```

## Workaround

```kotlin
fragmentManager.executePendingTransactions()
if (fragment.lifecycle.currentState == Lifecycle.State.RESUMED) {
    navigate()
}
```
