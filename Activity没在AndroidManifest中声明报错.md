
然后我们在 ActivityManagerService.java 中找到了 startActivity 方法，这个方法是一个重要的方法，它是启动Activity的入口。

要绕过 ActivityManagerService 的检查，因为找不到启动的Activity  的 ActivityInfo 和 ResolveInfo 。所以我们用 StubActivity 替换了 TargetActivity 。

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

```java
public ActivityResult execStartActivity(
    Context who, IBinder contextThread, IBinder token, Activity target,
    Intent intent, int requestCode, Bundle options) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    Uri referrer = target != null ? target.onProvideReferrer() : null;
    if(referrer != null) {
        intent.putExtra(Intent.EXTRA_REFERRER, referrer);
    }
    if(mActivityMonitors != null) {
        synchronized(mSync) {
            final int N = mActivityMonitors.size();
            for(int i = 0; i < N; i++) {
                final ActivityMonitor am = mActivityMonitors.get(i);
                ActivityResult result = null;
                if(am.ignoreMatchingSpecificIntents()) {
                    result = am.onStartActivity(intent);
                }
                if(result != null) {
                    am.mHits++;
                    return result;
                } else if(am.match(who, null, intent)) {
                    am.mHits++;
                    if(am.isBlocking()) {
                        return requestCode >= 0 ? am.getResult() : null;
                    }
                    break;
                }
            }
        }
    }
    try {
        intent.migrateExtraStreamToClipData();
        intent.prepareToLeaveProcess(who);
        int result = ActivityManager.getService()
            .startActivity(whoThread, who.getBasePackageName(), intent,
                intent.resolveTypeIfNeeded(who.getContentResolver()),
                token, target != null ? target.mEmbeddedID : null,
                requestCode, 0, null, options);
        //注释1处，这里会检查，如果没在AndroidManifest中声明，会抛出异常
        checkStartActivityResult(result, intent);
    } catch(RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    return null;
}
```

```java
public static void checkStartActivityResult(int res, Object intent) {
    if(!ActivityManager.isStartResultFatalError(res)) {
        return;
    }

    switch(res) {
        case ActivityManager.START_INTENT_NOT_RESOLVED:
        case ActivityManager.START_CLASS_NOT_FOUND:
            //注释1处，没在AndroidManifest中声明，会抛出异常
            if(intent instanceof Intent && ((Intent) intent).getComponent() !=
                null)
                throw new ActivityNotFoundException(
                    "Unable to find explicit activity class " + ((Intent) intent)
                    .getComponent().toShortString() +
                    "; have you declared this activity in your AndroidManifest.xml?"
                );
            throw new ActivityNotFoundException(
                "No Activity found to handle " + intent);
        case ActivityManager.START_PERMISSION_DENIED:
            throw new SecurityException("Not allowed to start activity " +
                intent);
        case ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT:
            throw new AndroidRuntimeException(
                "FORWARD_RESULT_FLAG used while also requesting a result"
            );
        case ActivityManager.START_NOT_ACTIVITY:
            throw new IllegalArgumentException(
                "PendingIntent is not an activity");
        case ActivityManager.START_NOT_VOICE_COMPATIBLE:
            throw new SecurityException(
                "Starting under voice control not allowed for: " +
                intent);
        case ActivityManager.START_VOICE_NOT_ACTIVE_SESSION:
            throw new IllegalStateException(
                "Session calling startVoiceActivity does not match active session"
            );
        case ActivityManager.START_VOICE_HIDDEN_SESSION:
            throw new IllegalStateException(
                "Cannot start voice activity on a hidden session");
        case ActivityManager.START_ASSISTANT_NOT_ACTIVE_SESSION:
            throw new IllegalStateException(
                "Session calling startAssistantActivity does not match active session"
            );
        case ActivityManager.START_ASSISTANT_HIDDEN_SESSION:
            throw new IllegalStateException(
                "Cannot start assistant activity on a hidden session");
        case ActivityManager.START_CANCELED:
            throw new AndroidRuntimeException(
                "Activity could not be started for " + intent);
        default:
            throw new AndroidRuntimeException("Unknown error code " + res +
                " when starting " + intent);
    }
}
```


注释1处，没在AndroidManifest中声明，会抛出异常。

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





