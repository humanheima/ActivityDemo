有错误之处欢迎指正批评

源码基于AndroidSDK31版本

理论基础
* 每个Android App都在一个独立空间里, 意味着其运行在一个单独的进程中, 拥有自己的VM, 被系统分配一个唯一的user ID。
* 当我们从桌面点击一个应用图标的启动一个App的时候，是从一个App(Launcher应用)启动另一个App。这中间就涉及了进程间通信。
* Launcher是Android系统启动后，加载的第一个程序，是其他应用程序的入口。也可以简单的理解为我们看到的Android桌面。

* Activity： 就是一个Java对象，它会被创建，同时也会被垃圾回收销毁，只不过它受AMS(ActivityManagerService)管理，所以它才有生命周期。
* ActivityResult：一个活动执行结果返回到原始活动的描述。
* AMS - ActivityManagerService：以下简称AMS，服务端对象，负责系统中所有Activity、Service的生命周期以及BroadcastReceiver和ContentProvider的管理。实现了`IBinder`接口，可以用于进程间的通信。

* ActivityThread ：它主要负责应用程序的主线程（也称为 UI 线程）的运行。管理应用程序的生命周期、处理事件、管理资源和组件以及与系统服务进行通信等。

* ApplicationThread：ApplicationThread是ActivityThread的内部类，继承了IApplicationThread.Stub。是Activity整个框架中客户端(ActivityThread)和服务端(AMS)通信的接口。ApplicationThread 运行在服务端的Binder线程池中。

* Instrumentation：用于监控程序和系统之间的交互操作。

* PackageManagerService：是Android系统中最常用的服务之一。它负责系统中Package的管理，应用程序的安装、卸载、信息查询等。这只是一个很笼统的说法。

我们启动一个和MainActivity不同进程的Activity
```xml
<activity
        android:name="com.example.architecturecomponents.livedata.LiveDataActivity"
        //指定不同进程
        android:process=":second" />
```



Activity的startActivity方法
```
@Override
public void startActivity(Intent intent, Bundle options) {
    if (options != null) {
        startActivityForResult(intent, -1, options);
    } else { 
        //1. 传入小于0的requestCode表示不需要返回结果
        startActivityForResult(intent, -1);
    }
}
```

Activity的startActivityForResult方法精简版，第二个参数requestCode传入小于0的值表示不需要返回结果。

```
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode, 
    @Nullable Bundle options) {
    if(mParent == null) {
        options = transferSpringboardActivityOptions(options);
        //注释1处，调用mInstrumentation.execStartActivity来启动Activity
        Instrumentation.ActivityResult ar =
            mInstrumentation.execStartActivity(
                this, mMainThread.getApplicationThread(), mToken, this,
                intent, requestCode, options);
        if(ar != null) {
            mMainThread.sendActivityResult(
                mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                ar.getResultData());
        }
        // ...
    } else {
       //...
    }
}
```
在上面方法的注释1处，调用mInstrumentation.execStartActivity来启动Activity，mInstrumentation是一个Instrumentation对象。
```
Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode, options);
```
Instrumentation的execStartActivity方法精简版

```
 /**
  * 执行应用程序发出的startActivity调用
  *
  * @param who 一个Context对象，表示是谁要启动Activity.
  * @param contextThread 将要启动Activity的上下文Context所在的主线程，（就是参数who所在的主线程。）
  *
  * @param token 参数who的一个系统内部标志，用来标记是谁正在启动activity，可能为null。
  * @param target 哪个activity正在执行启动activity的操作; 如果这个调用不是由一个activity发起的，target可能为null。
  * @param intent 要被启动的Intent。
  * @param requestCode 请求码，小于0表示不需要返回结果。我们上面传入是-1
  *            
  * @param options 附加选项。
  *
  * @return 默认返回 null.
  *
  */
 public ActivityResult execStartActivity(Context who, IBinder contextThread, 
    IBinder token, Activity target, Intent intent, int requestCode, Bundle options) {
    //将contextThread转成一个IApplicationThread对象
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    //...
    try {
       //...
       //准备离开who所在的进程
       intent.prepareToLeaveProcess(who);
       //注释1处，调用ActivityManagerService的startActivity方法
        int result = ActivityTaskManager.getService().startActivity(whoThread,
                    who.getOpPackageName(), who.getAttributionTag(), intent,
                    intent.resolveTypeIfNeeded(who.getContentResolver()), token,
                    target != null ? target.mEmbeddedID : null, requestCode, 0, null, options);
        //2. 检查执行结果
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    //默认返回null
    return null;
}

```

在注释1处，通过ActivityManager获取ActivityManagerService对象，然后调用ActivityManagerService的startActivity方法。

ActivityManager的getService方法。

```java
public static IActivityManager getService() {
    return IActivityManagerSingleton.get();
}

private static final Singleton<IActivityManager> IActivityManagerSingleton =
        new Singleton<IActivityManager>() {
            @Override
            protected IActivityManager create() {
                final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                final IActivityManager am = IActivityManager.Stub.asInterface(b);
                return am;
            }
        };
```

ActivityManagerService类精简版
```
public class ActivityManagerService extends IActivityManager.Stub
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
  //...

  private final ActivityStartController mActivityStartController;
  //...

  @Override
   public final int startActivity(IApplicationThread caller, String callingPackage,
            Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
            int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
        return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
                resultWho, requestCode, startFlags, profilerInfo, bOptions,
                UserHandle.getCallingUserId());
    }

  @Override
  public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
            Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
            int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
      return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
              resultWho, requestCode, startFlags, profilerInfo, bOptions, userId,
              true /*validateIncomingUser*/);
  }

  public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
          Intent intent, String resolvedType, IBinder resultTo, String resultWho, 
          int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions,
          int userId, boolean validateIncomingUser) {
     
      return mActivityStartController.obtainStarter(intent, "startActivityAsUser")//注释1处
              .setCaller(caller)
              .setCallingPackage(callingPackage)
              .setResolvedType(resolvedType)
              .setResultTo(resultTo)
              .setResultWho(resultWho)
              .setRequestCode(requestCode)
              .setStartFlags(startFlags)
              .setProfilerInfo(profilerInfo)
              .setActivityOptions(bOptions)
              .setMayWait(userId)//注释2处
              .execute();//注释3处
    }

}
```

在注释1处，返回了一个ActivityStarter对象。ActivityStarter用来配置和执行启动一个Activity。在{ActivityStarter#execute}方法被调用之前ActivityStarter是合法的，在{ActivityStarter#execute}方法被调用之后，ActivityStarter被认为是不合法的并且不应改再被修改或者使用。

在注释2处，调用了ActivityStarter类的setMayWait方法
```
ActivityStarter setMayWait(int userId) {
    //mRequest.mayWait赋值为true
    mRequest.mayWait = true;
    mRequest.userId = userId;

    return this;
}

```
在方法内部将mRequest.mayWait赋值为true，指明我们应该等待启动请求的结果。

在注释3处，调用了ActivityStarter类的execute方法
```
int execute() {
    try {
        if (mRequest.mayWait) {//mRequest.mayWait为true
            //调用startActivityMayWait方法
            return startActivityMayWait(mRequest.caller, mRequest.callingUid,
                    mRequest.callingPackage, mRequest.intent, mRequest.resolvedType,
                    mRequest.voiceSession, mRequest.voiceInteractor, mRequest.resultTo,
                    mRequest.resultWho, mRequest.requestCode, mRequest.startFlags,
                    mRequest.profilerInfo, mRequest.waitResult, mRequest.globalConfig,
                    mRequest.activityOptions, mRequest.ignoreTargetSecurity, mRequest.userId,
                    mRequest.inTask, mRequest.reason,
                    mRequest.allowPendingRemoteAnimationRegistryLookup);
        } 
    //...
    } finally {
        onExecutionComplete();
    }
}

```
ActivityStarter类startActivityMayWait方法精简版
```
private int startActivityMayWait(IApplicationThread caller, int callingUid,
          String callingPackage, Intent intent, String resolvedType,
          IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
          IBinder resultTo, String resultWho, int requestCode, int startFlags,
          ProfilerInfo profilerInfo, WaitResult outResult,
          Configuration globalConfig, SafeActivityOptions options, boolean ignoreTargetSecurity,
          int userId, TaskRecord inTask, String reason,
          boolean allowPendingRemoteAnimationRegistryLookup) {
        //...
        // Save a copy in case ephemeral needs it
        final Intent ephemeralIntent = new Intent(intent);
        // Don't modify the client's object!
        intent = new Intent(intent);
        //...
        ResolveInfo rInfo = mSupervisor.resolveIntent(intent, resolvedType, userId,
            0 /* matchFlags */,
                        computeResolveFilterUid(
                        callingUid, realCallingUid, mRequest.filterCallingUid));
        //...
        // Collect information about the target of the Intent.
       ActivityInfo aInfo = mSupervisor.resolveActivity(intent, rInfo, startFlags, profilerInfo);
      //...
      final ActivityRecord[] outRecord = new ActivityRecord[1];
      //调用startActivity方法
      int res = startActivity(caller, intent, ephemeralIntent, resolvedType, aInfo, rInfo,
                voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid,
                callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options,
                ignoreTargetSecurity, componentSpecified, outRecord, inTask, reason,
                allowPendingRemoteAnimationRegistryLookup);
    //...
    return res;
}

```
```
private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent,
        String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
        IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
        IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
        String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
        SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified,
        ActivityRecord[] outActivity, TaskRecord inTask, String reason,
        boolean allowPendingRemoteAnimationRegistryLookup) {
    //...
    mLastStartActivityResult = startActivity(caller, intent, ephemeralIntent, resolvedType,
            aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode,
            callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags,
            options, ignoreTargetSecurity, componentSpecified, mLastStartActivityRecord,
            inTask, allowPendingRemoteAnimationRegistryLookup);
   //...
   return getExternalResult(mLastStartActivityResult);
}

```
```
private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent,
       String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo,
       IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
       IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid,
       String callingPackage, int realCallingPid, int realCallingUid, int startFlags,
       SafeActivityOptions options,
       boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity,
       TaskRecord inTask, boolean allowPendingRemoteAnimationRegistryLookup) {
    //...
    ActivityRecord r = new ActivityRecord(mService, callerApp, callingPid, callingUid,
                callingPackage, intent, resolvedType, aInfo, mService.getGlobalConfiguration(),
                resultRecord, resultWho, requestCode, componentSpecified, voiceSession != null,
                mSupervisor, checkedOptions, sourceRecord);

    return startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags,
                true /* doResume */, checkedOptions, inTask, outActivity);
}
```
```
private int startActivity(final ActivityRecord r, ActivityRecord sourceRecord,
          IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
          int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
          ActivityRecord[] outActivity) {
   //...
   try {
        //注释1处
        result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor,
                    startFlags, doResume, options, inTask, outActivity);
    } 
    //注释2处
    postStartActivityProcessing(r, result, mTargetStack);

    return result;
}

```
ActivityStarter.startActivityMayWait方法中调用多个startActivity重载方法后会调用到一个比较重要的方法startActivityUnchecked。这个方法里会根据启动标志位和Activity启动模式来决定如何启动一个Activity。

ActivityStarter的startActivityUnchecked方法
```
private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask,
            ActivityRecord[] outActivity) {
    //...
    //初始化一些变量
    setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession,
                voiceInteractor);
    //计算Activity的启动方式
    computeLaunchingTaskFlags();
   
   //...
   //Should this be considered a new task?
   int result = START_SUCCESS;
   if (mStartActivity.resultTo == null && mInTask == null && !mAddingToTask
            && (mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) != 0) {
        //FLAG_ACTIVITY_NEW_TASK
        newTask = true;
        //注释1处，创建新的TaskRecord
        result = setTaskFromReuseOrCreateNewTask(taskToAffiliate, topStack);
    }
    //...
    if (mDoResume) {
         //...

        final ActivityRecord topTaskActivity =
                    mStartActivity.getTask().topRunningActivityLocked();
        if (!mTargetStack.isFocusable()
                || (topTaskActivity != null && topTaskActivity.mTaskOverlay
                && mStartActivity != topTaskActivity)) {
           //...
        else {
              if (mTargetStack.isFocusable() && !mSupervisor.isFocusedStack(mTargetStack)) {
                   mTargetStack.moveToFront("startActivityUnchecked");
               }
               //注释2处
               mSupervisor.resumeFocusedStackTopActivityLocked(mTargetStack, 
               mStartActivity,mOptions);
        }
    }
    return START_SUCCESS;
}

```
在上面方法的注释1处，因为我们设置了FLAG_ACTIVITY_NEW_TASK的flag，所以会创建一个新的任务栈。

在注释2处，调用ActivityStackSupervisor的resumeFocusedStackTopActivityLocked方法。

ActivityStackSupervisor的resumeFocusedStackTopActivityLocked方法精简版

```
boolean resumeFocusedStackTopActivityLocked(ActivityStack targetStack, 
    ActivityRecord target, ActivityOptions targetOptions) {
    //注释1处
    return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);

    final ActivityRecord r = mFocusedStack.topRunningActivityLocked();
    if (r == null || !r.isState(RESUMED)) {
        //注释2处
        mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
    } 
    return false; 
}

```
不能确定上面的的方法到底是执行了注释1处，还是注释2处，暂且认为是执行了注释2处的代码（《Android 进阶解密》书中分析执行的是2处的代码，这里就相信作者一回）

调用了ActivityStack类resumeTopActivityUncheckedLocked方法精简版
```
boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {

       //...
       boolean result = false;
      //注释1处
      result = resumeTopActivityInnerLocked(prev, options);
      //...
      return result;
}
```
注释1处调用了resumeTopActivityInnerLocked方法

```
private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) {
    //...
    //暂停其他Activity
    boolean pausing = mStackSupervisor.pauseBackStacks(userLeaving, next, false);
    if (mResumedActivity != null) {
        pausing |= startPausingLocked(userLeaving, false, next, false);
    }
    //...
    //2. 启动Activity
    mStackSupervisor.startSpecificActivityLocked(next, true, true);
    //...
    return true;

}

```
在ActivityStack.resumeTopActivityInnerLocked方法中会去判断是否有Activity处于resume状态，如果有的话会先让Activity执行pause过程，然后再执行startSpecificActivityLocked方法启动要启动的Activity。因为我们这里是启动根Activity，所以不会有Activity正处在onResume的状态。

直接看ActivityStackSupervisor的startSpecificActivityLocked方法
```
void startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig) {
    //如果要启动的Activity的应用已经在运行了 
    ProcessRecord app = mService.getProcessRecordLocked(r.processName,
            r.info.applicationInfo.uid, true);

    getLaunchTimeTracker().setLaunchTime(r);
    //如果要启动的Activity的应用已经在运行了 
    if (app != null && app.thread != null) {
        //...
        //1. 
        realStartActivityLocked(r, app, andResume, checkConfig);
        return;
    }
    //2. 没有启动则调用ActivityManagerService.startProcessLocked方法创建新的进程处理
    mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
            "activity", r.intent.getComponent(), false, false, true);
}

```
ActivityManagerService.startProcessLocked方法
```
final ProcessRecord startProcessLocked(String processName,
            ApplicationInfo info, boolean knownToBeDead, int intentFlags,
            String hostingType, ComponentName hostingName, boolean allowWhileBooting,
            boolean isolated, boolean keepIfLarge) {
    return startProcessLocked(processName, info, knownToBeDead, intentFlags, hostingType,
            hostingName, allowWhileBooting, isolated, 0 /* isolatedUid */, keepIfLarge,
            null /* ABI override */, null /* entryPoint */, null /* entryPointArgs */,
            null /* crashHandler */);

```
```
final ProcessRecord startProcessLocked(String processName, ApplicationInfo info,
            boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName,
            boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean keepIfLarge,
            String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {

    ProcessRecord app;
    //...
    if (!isolated) {
       //获取进程记录
        app = getProcessRecordLocked(processName, info.uid, keepIfLarge);
    }
    //如果已经存在正在运行的进程或者正在启动的进程，直接返回app。
    if (app != null && app.pid > 0) {
        if ((!knownToBeDead && !app.killed) || app.thread == null) {
            // 我们要启动的应用程序正在运行，或者正在启动，就返回该应用程序
            if (DEBUG_PROCESSES) Slog.v(TAG_PROCESSES, "App already running: " + app);
            // If this is a new package in the process, add the package to the list
            app.addPackage(info.packageName, info.versionCode, mProcessStats);
            checkTime(startTime, "startProcess: done, added package to proc");
            return app;
        }
    }
    //...
    if (app == null) {
        checkTime(startTime, "startProcess: creating new process record");
        //创建新的进程
        app = newProcessRecordLocked(info, processName, isolated, isolatedUid);
        app.crashHandler = crashHandler;
        app.isolatedEntryPoint = entryPoint;
        app.isolatedEntryPointArgs = entryPointArgs;
        checkTime(startTime, "startProcess: done creating new process record");
    }
    //...
    final boolean success = startProcessLocked(app, hostingType, hostingNameStr, abiOverride);
    //...
    return success ? app : null;
}

```
这里要启动我们的进程了。
```
private final boolean startProcessLocked(ProcessRecord app,
            String hostingType, String hostingNameStr, String abiOverride) {
    return startProcessLocked(app, hostingType, hostingNameStr,
                false /* disableHiddenApiChecks */, abiOverride);
}

```
```
private final boolean startProcessLocked(ProcessRecord app, String hostingType,
            String hostingNameStr, boolean disableHiddenApiChecks, String abiOverride) {
  //...
  // Start the process.  It will either succeed and return a result containing
  // the PID of the new process, or else throw a RuntimeException.
  //注释1处，这里注意一下，后面新的进程启动的时候会加载这个类
 final String entryPoint = "android.app.ActivityThread";
 return startProcessLocked(hostingType, hostingNameStr, entryPoint, app, uid, gids,
          runtimeFlags, mountExternal, seInfo, requiredAbi, instructionSet, 
          invokeWith, startTime);
    //...
}

```
上面方法的注释1处，注意一下，后面新的进程启动的时候会加载这个类。

ActivityManagerService的startProcessLocked方法
```
private boolean startProcessLocked(String hostingType, String hostingNameStr, String entryPoint,
            ProcessRecord app, int uid, int[] gids, int runtimeFlags, int mountExternal,
            String seInfo, String requiredAbi, String instructionSet, String invokeWith,
            long startTime) {
    //...
     final ProcessStartResult startResult = startProcess(app.hostingType, entryPoint,
                            app, app.startUid, gids, runtimeFlags, mountExternal, app.seInfo,
                            requiredAbi, instructionSet, invokeWith, app.startTime);
    //...
    return app.pid > 0;
}
```
```
private ProcessStartResult startProcess(String hostingType, String entryPoint,
        ProcessRecord app, int uid, int[] gids, int runtimeFlags, int mountExternal,
        String seInfo, String requiredAbi, String instructionSet, String invokeWith,
        long startTime) {
   //...
  startResult = Process.start(entryPoint,
                      app.processName, uid, uid, gids, runtimeFlags, mountExternal,
                      app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet,
                      app.info.dataDir, invokeWith,
                      new String[] {PROC_START_SEQ_IDENT + app.startSeq});
  return startResult;
}

```
ActivityManagerService.startProcessLocked方法经过多次跳转最终会通过Process.start方法来为应用创建进程。

Process的start方法
```
public static final ProcessStartResult start(final String processClass,
                              final String niceName,
                              int uid, int gid, int[] gids,
                              int runtimeFlags, int mountExternal,
                              int targetSdkVersion,
                              String seInfo,
                              String abi,
                              String instructionSet,
                              String appDataDir,
                              String invokeWith,
                              String[] zygoteArgs) {
    return zygoteProcess.start(processClass, niceName, uid, gid, gids,
                runtimeFlags, mountExternal, targetSdkVersion, seInfo,
                abi, instructionSet, appDataDir, invokeWith, zygoteArgs);
}

```
经过一步步调用，可以发现其最终调用了Zygote并通过socket通信的方式让Zygote进程fork出一个新的进程，并根据传递的”android.app.ActivityThread”字符串，反射出该对象并执行ActivityThread的main方法对其进行初始化。

ActivityThread类的main方法,感觉到这里已经隐隐约约的看到了胜利的曙光
```
public final class ActivityThread extends ClientTransactionHandler {
    //初始化mAppThread对象
    final ApplicationThread mAppThread = new ApplicationThread();

    public static void main(String[] args) {
         //... 
        Process.setArgV0("<pre-initialized>");
        //准备主线程的Looper
        Looper.prepareMainLooper();

        //...
        // 初始化 ActivityThread对象
        ActivityThread thread = new ActivityThread();
        //2. 连接当前进程和Application
        thread.attach(false, startSeq);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
        //
        //主线程的消息队列开始循环
        Looper.loop();
    }
    //...

}

```
ActivityThread的attach方法
```
private void attach(boolean system, long startSeq) {
  //...
  if (!system) {
        //...
        //获取ActivityManagerService对象
        final IActivityManager mgr = ActivityManager.getService();
        try {
            //1. 调用ActivityManagerService的attachApplication方法
            mgr.attachApplication(mAppThread, startSeq);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
           //...
        } 
    //...
}

```
ActivityManagerService的attachApplication方法
```
@Override
public final void attachApplication(IApplicationThread thread, long startSeq) {
    synchronized (this) {
        //...
        attachApplicationLocked(thread, callingPid, callingUid, startSeq);
        //...
    }
}
```
ActivityManagerService的attachApplicationLocked方法
```
private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid, int callingUid, long startSeq) {
    //...
    //注释1处，调用ApplicationThread的bindApplication方法
    thread.bindApplication(processName, appInfo, providers, null, profilerInfo,
                        null, null, null, testMode,
                        mBinderTransactionTrackingEnabled, enableTrackAllocation,
                        isRestrictedBackupMode || !normalMode, app.persistent,
                        new Configuration(getGlobalConfiguration()), app.compat,
                        getCommonServicesLocked(app.isolated),
                        mCoreSettingsObserver.getCoreSettingsLocked(),
                        buildSerial, isAutofillCompatEnabled);
        // 要启动的Activity等待在这个进程中运行
        if (normalMode) {
            try {
                //注释2处，调用ActivityStackSupervisor的attachApplicationLocked方法
                if (mStackSupervisor.attachApplicationLocked(app)) {
                    didSomething = true;
                }
            } 
        }
   //...
  return true;
}

```
在注释1处，调用了ApplicationThread的bindApplication方法

```
public final void bindApplication(String processName, ApplicationInfo appInfo,
                List<ProviderInfo> providers, ComponentName instrumentationName,
                ProfilerInfo profilerInfo, Bundle instrumentationArgs,
                IInstrumentationWatcher instrumentationWatcher,
                IUiAutomationConnection instrumentationUiConnection, int debugMode,
                boolean enableBinderTracking, boolean trackAllocation,
                boolean isRestrictedBackupMode, boolean persistent, Configuration config,
                CompatibilityInfo compatInfo, Map services, Bundle coreSettings,
                String buildSerial, boolean autofillCompatibilityEnabled) {

      //1.调用ActivityThread的sendMessage方法
      sendMessage(H.BIND_APPLICATION, data);
}
```
ActivityThread的sendMessage方法内部是调用mH来发送消息的，mH是一个H类型的对象。H是ActivityThread的内部类，继承了Handler类。我们看一下H的handleMessage方法。
```
case BIND_APPLICATION:
    AppBindData data = (AppBindData)msg.obj;
    //1. 调用handleBindApplication方法
    handleBindApplication(data);
break;

```
ActivityThread的handleBindApplication方法
```
private void handleBindApplication(AppBindData data) {
  //...
  //设置应用的名字
  Process.setArgV0(data.processName);
  //创建Instrumentation对象
  mInstrumentation = new Instrumentation();
  Application app;
  //注释1处
  app = data.info.makeApplication(data.restrictedBackupMode, null);
  mInitialApplication = app;
 //...
 //注释2处启动app
 mInstrumentation.callApplicationOnCreate(app);
 //...
}
```
注释1处，data.info是一个LoadedApk类型的对象，我们看一下LoadedApk的makeApplication方法。
```
 public Application makeApplication(boolean forceDefaultAppClass,
            Instrumentation instrumentation) {
        if (mApplication != null) {
            return mApplication;
        }

        Application app = null;
        //获取appClass
        String appClass = mApplicationInfo.className;
        if (forceDefaultAppClass || (appClass == null)) {
            appClass = "android.app.Application";
        }

        try {
            java.lang.ClassLoader cl = getClassLoader();
            //在这里创建了应用的context
            ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);
            //注释1处，创建Application对象
            app = mActivityThread.mInstrumentation.newApplication(
                    cl, appClass, appContext);
            appContext.setOuterContext(app);
        } 
        mActivityThread.mAllApplications.add(app);
        //把app赋值给mApplication
        mApplication = app;
        //我们在这里传入的instrumentation是null
        if (instrumentation != null) {
            try {
                instrumentation.callApplicationOnCreate(app);
            } 
        }

        //...
        return app;
    }
```
在makeApplication方法中的注释1处，调用了Instrumentation的newApplication方法。
```
public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        //创建了Application对象
        Application app = getFactory(context.getPackageName())
                .instantiateApplication(cl, className);
        //注释1处，调用Application的attach方法。之后我们就可以获取应用的context了。
        app.attach(context);
        return app;
    }
```


在handleBindApplication方法的注释2处调用了Instrumentation的callApplicationOnCreate方法
```
public void callApplicationOnCreate(Application app) {
        //到这里app已经启动了
        app.onCreate();
    }
```

到这里Application的onCreate方法已经被调用了，第一部分已经完毕。

### 二 MainActivity的启动过程

先来一张大概的时序图
![MainActivity的启动流程.png](https://upload-images.jianshu.io/upload_images/3611193-27c60d86241b7904.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


**在上面的分析中，我们知道ActivityManagerService的attachApplicationLocked方法中会调用ActivityStackSupervisor的attachApplicationLocked方法**

ActivityStackSupervisor的attachApplicationLocked方法
```
boolean attachApplicationLocked(ProcessRecord app) throws RemoteException {
    //...
    try {
          if (realStartActivityLocked(activity, app,top == activity, true)) {
                    didSomething = true;
          }
    } 
    //...
    return didSomething;
}
```
ActivityStackSupervisor的realStartActivityLocked方法
```
final boolean realStartActivityLocked(ActivityRecord r, ProcessRecord app,
            boolean andResume, boolean checkConfig) throws RemoteException {
               //...
               // 创建Activity启动的事务Create activity launch transaction.
                final ClientTransaction clientTransaction = ClientTransaction.obtain(app.thread,
                        r.appToken);
                //1. 添加一个LaunchActivityItem类型的callBack
               clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(r.intent),
                        System.identityHashCode(r), r.info,
                        mergedConfiguration.getGlobalConfiguration(),
                        mergedConfiguration.getOverrideConfiguration(), r.compat,
                        r.launchedFromPackage, task.voiceInteractor, app.repProcState, r.icicle,
                        r.persistentState, results, newIntents, mService.isNextTransitionForward(),
                        profilerInfo));

                // 设置Activity想要到达生命周期最终状态，这里我们是要onResume
                final ActivityLifecycleItem lifecycleItem;
           
                lifecycleItem = ResumeActivityItem.obtain(mService.isNextTransitionForward());
                 //设置生命周期状态请求为ResumeActivityItem
                clientTransaction.setLifecycleStateRequest(lifecycleItem);

                // Schedule transaction.
                mService.getLifecycleManager().scheduleTransaction(clientTransaction);
    //...
    return true;
}

```
在ActivityStackSupervisor.realStartActivityLocked方法中为ClientTransaction对象添加LaunchActivityItem的callback，然后设置当前的生命周期状态请求为ResumeActivityItem，最后调用ClientLifecycleManager.scheduleTransaction方法执行。

ClientLifecycleManager的scheduleTransaction方法
```
void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
    //...
    transaction.schedule();
    //...
}

```
ClientTransaction的schedule方法
```
public void schedule() throws RemoteException {
    //这里的mClient就是一个ApplicationThread对象
    mClient.scheduleTransaction(this);
}

```

内部调用了ActivityThread的scheduleTransaction方法。这个方法是继承自ClientTransactionHandler类

```
void scheduleTransaction(ClientTransaction transaction) {
    transaction.preExecute(this);
    //发送消息
    sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction);
}

```
发送消息最终是由ActivityThread中的mH对象发送的，mH的类型是H，H是ActivityThread的一个内部类，H继承了Handler类。

H 的handleMessage方法
```
case EXECUTE_TRANSACTION:
     final ClientTransaction transaction = (ClientTransaction) msg.obj;
      mTransactionExecutor.execute(transaction);
break;

```
mTransactionExecutor的类型是TransactionExecutor

TransactionExecutor的execute方法

```
public void execute(ClientTransaction transaction) {
    final IBinder token = transaction.getActivityToken();
    //注释1处，先执行callback
    executeCallbacks(transaction);
    //注释2处，执行生命周期状态请求
    executeLifecycleState(transaction);
    mPendingActions.clear();
}

```
1. 先执行callback
```
public void executeCallbacks(ClientTransaction transaction) {
    //...
    //调用ClientTransactionItem的execute方法，还记得我们传入的是LaunchActivityItem。
    item.execute(mTransactionHandler, token, mPendingActions);
    //...

}

```
LaunchActivityItem的execute方法
```
@Override
public void execute(ClientTransactionHandler client, IBinder token,
            PendingTransactionActions pendingActions) {
  //调用ClientTransactionHandler的handleLaunchActivity
  client.handleLaunchActivity(r, pendingActions, null /* customIntent */);
      
}

```
ClientTransactionHandler 是一个抽象类，ActivityThread继承了ClientTransactionHandler 并重写了handleLaunchActivity方法

ActivityThread的handleLaunchActivity方法
```
@Override
public Activity handleLaunchActivity(ActivityClientRecord r,
          PendingTransactionActions pendingActions, Intent customIntent) {
     
    //1. 调用performLaunchActivity方法
    final Activity a = performLaunchActivity(r, customIntent);

   //...

  return a;

}

```
ActivityThread的performLaunchActivity方法精简版，这个方法内部会执行Activity的attach方法和onCreate方法
```
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
         //...
        //构建Activity的上下文
         ContextImpl appContext = createBaseContextForActivity(r);
        //...
        //创建Activity实例
        Activity activity = null;
        try {
            java.lang.ClassLoader cl = appContext.getClassLoader();
            activity = mInstrumentation.newActivity(
                    cl, component.getClassName(), r.intent);
            //...
        } catch (Exception e) {
            //...
        }

        try {
             //1.获取Application对象
            Application app = r.packageInfo.makeApplication(false, mInstrumentation);
           //...
            if (activity != null) {
               //...
              appContext.setOuterContext(activity);
              //2. activity调用attach方法
                activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor, window, r.configCallback);
               //...
              //3. 调用Activity的onCreate方法
              mInstrumentation.callActivityOnCreate(activity, r.state);
             //...
             // 这时候Activity的状态已经是ON_CREATE
             r.setState(ON_CREATE);
            // 4. 将ActivityRecord对象保存在ActivityThread的mActivities中
            mActivities.put(r.token, r);

        } catch (SuperNotCalledException e) {
            throw e;

        } catch (Exception e) {
            //...
        }
        
        return activity;
    }
```

注释2处，调用Activity的attach()方法
```
final void attach(Context context, ActivityThread aThread,
        Instrumentation instr, IBinder token, int ident,
        Application application, Intent intent, ActivityInfo info,
        CharSequence title, Activity parent, String id,
        NonConfigurationInstances lastNonConfigurationInstances,
        Configuration config, String referrer, IVoiceInteractor voiceInteractor,
        Window window, ActivityConfigCallback activityConfigCallback) {
    //添加context
    attachBaseContext(context);
    //初始化PhoneWindow对象
    mWindow = new PhoneWindow(this, window, activityConfigCallback);
    //...
    //给window对象设置WindowManager
    mWindow.setWindowManager(
            (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
            mToken, mComponent.flattenToString(),
            (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
   //给mWindowManager赋值
    mWindowManager = mWindow.getWindowManager();
    mCurrentConfig = config;
    //...
}
```
该方法初始化了context，mWindow和mWindowManager等重要变量。

3. Instrumentation的callActivityOnCreate方法
```
/**
 * 执行调用Activity的onCreate方法
 * 
 * @param activity The activity being created.
 * @param icicle The previously frozen state (or null) to pass through to onCreate().
 */
public void callActivityOnCreate(Activity activity, Bundle icicle) {
    prePerformCreate(activity);
    //这个方法名看起来看起来有戏
    activity.performCreate(icicle);
    postPerformCreate(activity);
}

```
Activity的performCreate方法
```
final void performCreate(Bundle icicle, PersistableBundle persistentState) {
    mCanEnterPictureInPicture = true;
    restoreHasCurrentPermissionRequest(icicle);
    if (persistentState != null) {
        onCreate(icicle, persistentState);
    } else {
        //亲人啊，终于找到你了
        onCreate(icicle);
    }
    mActivityTransitionState.readState(icicle);

    mVisibleFromClient = !mWindow.getWindowStyle().getBoolean(
            com.android.internal.R.styleable.Window_windowNoDisplay, false);
    mFragments.dispatchActivityCreated();
    mActivityTransitionState.setEnterActivityOptions(this, getActivityOptions());
}

```
现在Activity状态已经走到生命周期方法onCreate了，那么是怎么从onCreate到onResume的呢？

让我们回到ActivityStackSupervisor.realStartActivityLocked方法中，我们设置了onResume生命周期状态请求生命周期状态，最后调用ClientLifecycleManager.scheduleTransaction方法执行。
```
lifecycleItem = ResumeActivityItem.obtain(mService.isNextTransitionForward());
//设置OnResume生命周期状态请求
clientTransaction.setLifecycleStateRequest(lifecycleItem);

 // Schedule transaction.
 mService.getLifecycleManager().scheduleTransaction(clientTransaction);

```
所以在TransactionExecutor的execute方法中，我们执行完了callback，然后继续执行生命周期状态请求
```
public void execute(ClientTransaction transaction) {
    final IBinder token = transaction.getActivityToken();
    //1. 先执行callback
    executeCallbacks(transaction);
    //2. 执行生命周期状态请求
    executeLifecycleState(transaction);
    mPendingActions.clear();  
}

```
TransactionExecutor的executeLifecycleState方法
```
private void executeLifecycleState(ClientTransaction transaction) {
    //生命周期是onResume，lifecycleItem 是ResumeActivityItem对象
    final ActivityLifecycleItem lifecycleItem = transaction.getLifecycleStateRequest();
    //...
    //1. 我们当前状态是onCreate，目标状态是onResume
   cycleToPath(r, lifecycleItem.getTargetState(), true /* excludeLastState */);
    // 2. ResumeActivityItem调用
    lifecycleItem.execute(mTransactionHandler, token, mPendingActions);
   //...
}

```
注释1处，调用cycleToPath方法，注意我们传入的第3个参数为true。
```
private void cycleToPath(ActivityClientRecord r, int finish, boolean excludeLastState) {
   //这时候Activity已经onCreate了
   final int start = r.getLifecycleState();
    //注释1处，获取所有的状态
    final IntArray path = mHelper.getLifecyclePath(start, finish, excludeLastState);
    //注释2处
    performLifecycleSequence(r, path);
}

```
在注释1处，获取所有的生命周期状态,我们传入的start是onCreate,finish是onResume,但是我们传入excludeLastState为true。mLifecycleSequence只会添加1个状态即onStarat,而不会添加onResume。

```
public IntArray getLifecyclePath(int start, int finish, boolean excludeLastState) {
    mLifecycleSequence.clear();
    if (finish >= start) {
        // just go there
        for (int i = start + 1; i <= finish; i++) {
            mLifecycleSequence.add(i);
        }
    }
   // 移除最后一个状态，以防我们想用一些特定的参数执行它。
   if (excludeLastState && mLifecycleSequence.size() != 0) {
        mLifecycleSequence.remove(mLifecycleSequence.size() - 1);
    }
   return mLifecycleSequence;
}

```
ActivityLifecycleItem中对应生命周期的整数
```
public abstract class ActivityLifecycleItem extends ClientTransactionItem {

    public static final int UNDEFINED = -1;
    public static final int PRE_ON_CREATE = 0;
    public static final int ON_CREATE = 1;
    public static final int ON_START = 2;
    public static final int ON_RESUME = 3;
    public static final int ON_PAUSE = 4;
    public static final int ON_STOP = 5;
    public static final int ON_DESTROY = 6;
    public static final int ON_RESTART = 7;

}
```
然后回到cycleToPath方法的注释2处

```
    private void performLifecycleSequence(ActivityClientRecord r, IntArray path) {
        final int size = path.size();
        //循环遍历所有的状态
        for (int i = 0, state; i < size; i++) {
            state = path.get(i);
            switch (state) {
                case ON_CREATE:
                    mTransactionHandler.handleLaunchActivity(r, mPendingActions,
                            null /* customIntent */);
                    break;
                case ON_START:
                    mTransactionHandler.handleStartActivity(r, mPendingActions);
                    break;
                case ON_RESUME:
                    mTransactionHandler.handleResumeActivity(r.token, false /* finalStateRequest */,
                            r.isForward, "LIFECYCLER_RESUME_ACTIVITY");
                    break;
                case ON_PAUSE:
                    mTransactionHandler.handlePauseActivity(r.token, false /* finished */,
                            false /* userLeaving */, 0 /* configChanges */, mPendingActions,
                            "LIFECYCLER_PAUSE_ACTIVITY");
                    break;
                case ON_STOP:
                    mTransactionHandler.handleStopActivity(r.token, false /* show */,
                            0 /* configChanges */, mPendingActions, false /* finalStateRequest */,
                            "LIFECYCLER_STOP_ACTIVITY");
                    break;
                case ON_DESTROY:
                    mTransactionHandler.handleDestroyActivity(r.token, false /* finishing */,
                            0 /* configChanges */, false /* getNonConfigInstance */,
                            "performLifecycleSequence. cycling to:" + path.get(size - 1));
                    break;
                case ON_RESTART:
                    mTransactionHandler.performRestartActivity(r.token, false /* start */);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected lifecycle state: " + state);
            }
        }
    }

```
这个方法最终由ActivityThread执行handleStartActivity方法，最终Activity会调用onStart方法。

ActivityThread的handleStartActivity方法
```
@Override
public void handleStartActivity(ActivityClientRecord r,
            PendingTransactionActions pendingActions) {
    final Activity activity = r.activity;
    // Start
    activity.performStart("handleStartActivity");
    r.setState(ON_START);
}

```

我们还要回到TransactionExecutor的executeLifecycleState方法的注释2处
```
private void executeLifecycleState(ClientTransaction transaction) {
    //生命周期是onResume，lifecycleItem 是ResumeActivityItem对象
    final ActivityLifecycleItem lifecycleItem = transaction.getLifecycleStateRequest();
    //...
    //注释1处， 我们当前状态是onCreate，目标状态是onResume
    cycleToPath(r, lifecycleItem.getTargetState(), true /* excludeLastState */);
    // 注释2处，ResumeActivityItem调用
    lifecycleItem.execute(mTransactionHandler, token, mPendingActions);
   //...
}

```
ResumeActivityItem的execute方法
```
@Override
public void execute(ClientTransactionHandler client, IBinder token,
        PendingTransactionActions pendingActions) {
    //ActivityThread的handleResumeActivity方法
    client.handleResumeActivity(token, true /* finalStateRequest */, mIsForward,
            "RESUME_ACTIVITY");
}
```

ActivityThread的handleResumeActivity方法
```
@Override
public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
            String reason) {
      
    //注释1处，
    final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
    

    final Activity a = r.activity;
    // 当启动Activity传入的requestCode大于0，mStartedActivity为true。
    //我们传入的requestCode是-1,willBeVisible是true
    boolean willBeVisible = !a.mStartedActivity;
    if (r.window == null && !a.mFinished && willBeVisible) {
        r.window = r.activity.getWindow();
        View decor = r.window.getDecorView();
        //先让DecorView不可见
        decor.setVisibility(View.INVISIBLE);
        ViewManager wm = a.getWindowManager();
        WindowManager.LayoutParams l = r.window.getAttributes();
        a.mDecor = decor;       
        //...
        if (a.mVisibleFromClient) {//默认是true
            if (!a.mWindowAdded) {
                a.mWindowAdded = true;
                //ViewManager添加DecorView,这里的ViewManager其实是ViewRootImpl
                wm.addView(decor, l);
            } 
        }
    } 
    if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null 
        && !r.hideForNow) {
            
       //...
       r.activity.mVisibleFromServer = true;
       mNumVisibleActivities++;
       if (r.activity.mVisibleFromClient) {
            //注释2处，调用activity的makeVisible方法
            r.activity.makeVisible();
        }
    }
     //...   
    
}
```

ActivityThread的handleResumeActivity方法注释1处，调用了performResumeActivity方法

```
 public ActivityClientRecord performResumeActivity(IBinder token, boolean finalStateRequest,
            String reason) {
  //...
  r.activity.performResume(r.startsNotResumed, reason);
  //...
}
```
Activity的performResume方法
```
final void performResume(boolean followedByPause, String reason) {
    //...
   mInstrumentation.callActivityOnResume(this);
   //...
}
```
Instrumentation的callActivityOnResume方法
```
public void callActivityOnResume(Activity activity) {
        activity.mResumed = true;
        //终于onResume了。
        activity.onResume();
        //...
    }
```
然后在注释2处，调用了Activity的makeVisible()方法，我们看一下。
```
void makeVisible() {
    if (!mWindowAdded) {
        ViewManager wm = getWindowManager();
        wm.addView(mDecor, getWindow().getAttributes());
        mWindowAdded = true;
    }
   //这时候DecorView才可见
    mDecor.setVisibility(View.VISIBLE);
}

```

结尾：就先这样吧。总结起来就是一句话：自己选择看源码，就是跪着也要看完。但是也是连蒙带猜。。。还要继续再研究。

参考链接：
1. [Android Activity的启动过程过程分析](http://www.jcodecraeer.com/a/anzhuokaifa/2018/0130/9274.html)
2. [一篇让你记住Activity启动流程！](https://mp.weixin.qq.com/s/8eh7JyAYkO1lldfxYQ0JuA)
3. [浅析Android Activity的启动过程](https://blog.csdn.net/yyh352091626/article/details/51086117 )
4. [（Android 9.0）Activity启动流程源码分析](https://blog.csdn.net/lj19851227/article/details/82562115)
5. [Android 组件系列 -- Activity 启动流程(9.0)](https://blog.csdn.net/cjh_android/article/details/82533321)
6. [[译]Android Application启动流程分析](https://www.jianshu.com/p/a5532ecc8377)
