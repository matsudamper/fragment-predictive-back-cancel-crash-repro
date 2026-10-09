# Fragment Predictive Back Crash Repro

Tapping a button at the screen edge, where the back gesture also starts, breaks the FragmentManager back stack and can crash the app.

<a href="https://github.com/matsudamper/fragment-predictive-back-cancel-crash-repro/raw/refs/heads/main/docs/crash.mp4">Video</a>

## Environment

- androidx.fragment: 1.9.1
- androidx.activity: 1.13.0
- androidx.viewpager2: 1.1.0
- enableOnBackInvokedCallback="true"
- Android OS 14+

## Steps to reproduce
- Start(Home)
- Navigate to Screen#1
- Press Down navigate to Screen#2 button
- fire `OnBackPressedCallback.handleOnBackStarted`
  - Occurs when quickly flicking inward from the screen edge on the button.
- Release navigate to Screen#2 button(Quickly)
- call: `FragmentTransaction.commit()`
  - The button is clicked and navigation occurs.
- internal processing: `FragmentManager.execPendingActions()`
  - Executes the transition enqueued by `handleOnBackStarted` and the commit together.
- fire: `OnBackPressedCallback.handleOnBackCancelled`
- internal processing: `FragmentManager.cancelBackStackTransition()`

What happens
- FragmentManager back stack is broken.
  - expected
    - Screen#2 is shown.
    - Back: Screen#2 → Screen#1 → Home
  - actual
    - Home is shown.
    - Back: Home → Screen#2 → Home
- Crash occurs when Screen#1 has ViewPager2.

## StackTrace (has ViewPager2)

```
java.lang.IllegalStateException: Fragment no longer exists for key f#0: unique id ff76dcc5-d7af-4861-bba7-a17c0d712b07
	at androidx.fragment.app.FragmentManager.getFragment(FragmentManager.java:1274)
	at androidx.viewpager2.adapter.FragmentStateAdapter.restoreState(FragmentStateAdapter.java:565)
	at androidx.viewpager2.widget.ViewPager2.restorePendingState(ViewPager2.java:356)
	at androidx.viewpager2.widget.ViewPager2.dispatchRestoreInstanceState(ViewPager2.java:381)
	at android.view.ViewGroup.dispatchRestoreInstanceState(ViewGroup.java:4034)
	at android.view.View.restoreHierarchyState(View.java:22232)
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
	at androidx.navigationevent.OnBackInvokedInput$createOnBackAnimationCallback$1.onBackCancelled(OnBackInvokedInput.android.kt:109)
	at android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper.lambda$onBackCancelled$3(WindowOnBackInvokedDispatcher.java:353)
	at android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper.$r8$lambda$MvEvoNsuMagpR44wZwrfwOccMBA(Unknown Source:0)
	at android.window.WindowOnBackInvokedDispatcher$OnBackInvokedCallbackWrapper$$ExternalSyntheticLambda3.run(Unknown Source:2)
	at android.window.BackProgressAnimator$2.onAnimationEnd(BackProgressAnimator.java:144)
	at com.android.internal.dynamicanimation.animation.DynamicAnimation.endAnimationInternal(DynamicAnimation.java:720)
	at com.android.internal.dynamicanimation.animation.DynamicAnimation.doAnimationFrame(DynamicAnimation.java:690)
	at android.animation.AnimationHandler.doAnimationFrame(AnimationHandler.java:328)
	at android.animation.AnimationHandler.-$$Nest$mdoAnimationFrame(Unknown Source:0)
	at android.animation.AnimationHandler$1.doFrame(AnimationHandler.java:86)
	at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1337)
	at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1348)
	at android.view.Choreographer.doCallbacks(Choreographer.java:952)
	at android.view.Choreographer.doFrame(Choreographer.java:878)
	at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1322)
	at android.os.Handler.handleCallback(Handler.java:958)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loopOnce(Looper.java:205)
	at android.os.Looper.loop(Looper.java:294)
	at android.app.ActivityThread.main(ActivityThread.java:8177)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)
```
