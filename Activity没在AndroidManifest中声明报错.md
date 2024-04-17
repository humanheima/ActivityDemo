```java
 Caused by: android.content.ActivityNotFoundException: Unable to find explicit activity class
{com.hm.activitydemo/com.hm.activitydemo.activity.SecondActivity}; have you declared this activity in
        your AndroidManifest.xml?
        at android.app.Instrumentation.checkStartActivityResult(Instrumentation.java:2005)
        at android.app.Instrumentation.execStartActivity(Instrumentation.java:1673)
        at android.app.Activity.startActivityForResult(Activity.java:4586)
        at android.support.v4.app.FragmentActivity.startActivityForResult(FragmentActivity.java:767)
        at android.app.Activity.startActivityForResult(Activity.java:4544)
        at android.support.v4.app.FragmentActivity.startActivityForResult(FragmentActivity.java:754)
        at android.app.Activity.startActivity(Activity.java:4905)
        at android.app.Activity.startActivity(Activity.java:4873)

```

看来占位的Activity，StubActivity，看来是绕过Instrumentation的检查。

然后我们在 ActivityManagerService.java 中找到了 startActivity 方法，这个方法是一个重要的方法，它是启动Activity的入口。

我们要代理ActivityManagerService 的 startActivity 方法。这里当我们发现调用 startActivity 方法时，用占位的 StubActivity 替换了真正的 Activity 。
然后在 ActivityThread 真正的 handleLaunchActivity 方法中，取出被启动的Activity的真正Intent，进行启动。

参考项目下 [Android7版本的 ActivityThread.java](Android7%E7%89%88%E6%9C%AC%E7%9A%84%20ActivityThread.java)

```java

private void handleLaunchActivity(ActivityClientRecord r, Intent customIntent,
    String reason) {
    // If we are getting ready to gc after going to the background, well
    // we are back active so skip it.
    unscheduleGcIdler();
    mSomeActivitiesChanged = true;

    if(r.profilerInfo != null) {
        mProfiler.setProfiler(r.profilerInfo);
        mProfiler.startProfiling();
    }

    // Make sure we are running with the most recent config.
    handleConfigurationChanged(null, null);

    if(localLOGV) Slog.v(
        TAG, "Handling launch of " + r);

    // Initialize before creating the activity
    WindowManagerGlobal.initialize();
    //注释1处，启动Activity
    Activity a = performLaunchActivity(r, customIntent);

    if(a != null) {
        r.createdConfig = new Configuration(mConfiguration);
        reportSizeConfigurations(r);
        Bundle oldState = r.state;
        handleResumeActivity(r.token, false, r.isForward, !r.activity.mFinished &&
            !r.startsNotResumed, r.lastProcessedSeq, reason);

        if(!r.activity.mFinished && r.startsNotResumed) {
            // The activity manager actually wants this one to start out paused, because it
            // needs to be visible but isn't in the foreground. We accomplish this by going
            // through the normal startup (because activities expect to go through onResume()
            // the first time they run, before their window is displayed), and then pausing it.
            // However, in this case we do -not- need to do the full pause cycle (of freezing
            // and such) because the activity manager assumes it can just retain the current
            // state it has.
            performPauseActivityIfNeeded(r, reason);

            // We need to keep around the original state, in case we need to be created again.
            // But we only do this for pre-Honeycomb apps, which always save their state when
            // pausing, so we can not have them save their state when restarting from a paused
            // state. For HC and later, we want to (and can) let the state be saved as the
            // normal part of stopping the activity.
            if(r.isPreHoneycomb()) {
                r.state = oldState;
            }
        }
    } else {
        // If there was an error, for any reason, tell the activity manager to stop us.
        try {
            ActivityManagerNative.getDefault()
                .finishActivity(r.token, Activity.RESULT_CANCELED, null,
                    Activity.DONT_FINISH_TASK_WITH_ACTIVITY);
        } catch(RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
}

```





