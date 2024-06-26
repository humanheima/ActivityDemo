### Activity


![生命周期图](activity_lifecycle.png)


![隐式 Intent 如何通过系统传递以启动其他 Activity 的图解](intent_filters.png)

SingleTask 启动模式 配合taskAffinity属性也可以在一个新的返回栈里面启动Activity。
```
 <activity
            android:name=".activity.ThirdActivity"
            android:launchMode="singleTask"
            android:taskAffinity="com.hm.task">

        </activity>
```

上面的ThirdActivity会在返回栈(com.hm.task)中被启动。

参考[Android中Activity四种启动模式和taskAffinity属性详解](https://blog.csdn.net/zhangjg_blog/article/details/10923643)

Activity启动流程：

1、应用通过startActivity或是startActivityForResult方法向ActivityManagerService发出启动请求。

2、ActivityManagerService接收到启动请求后会进行必要的初始化以及状态的刷新，然后解析Activity的启动模式，为启动Activity做一系列的准备工作。

3、做完上述准备工作后，会去判断栈顶是否为空，如果不为空即当前有Activity显示在前台，则会先进行栈顶Activity的onPause流程退出。

4、栈顶Activity执行完onPause流程退出后开始启动Activity。如果Activity被启动过则直接执行onRestart->onStart->onResume过程直接启动Activity（热启动过程）。否则执行Activity所在应用的冷启动过程。

5、冷启动过程首先会通过Zygote进程fork出一个新的进程，然后根据传递的”android.app.ActivityThread”字符串，反射出该对象并执行ActivityThread的main方法进行主线程的初始化。

6、Activity所在应用的进程和主线程完成初始化之后开始启动Activity，首先对Activity的ComponentName、ContextImpl、Activity以及Application对象进行了初始化并相互关联，然后设置Activity主题，最后执行onCreate->onStart->onResume方法完成Activity的启动。

7、上述流程都执行完毕后，会去执行栈顶Activity的onStop过程。

原文：https://blog.csdn.net/lj19851227/article/details/82562115

### 关于onNewIntent方法 ，Activity的启动模式无论是 singleTop，singleTask，singleInstance都会调用。
例如要被启动的Activity叫A
1. A的启动模式是singleTop，如果A在栈顶，那么再次启动A那么onNewIntent方法会被调用。
2. A的启动模式是singleTask，或者singleInstance，无论A在不在栈顶，那么再次启动A那么onNewIntent方法会被调用。

### onSaveInstanceState方法和onRestoreInstanceState方法的调用时机
```
com.hm.activitydemo E/SecondActivity: onCreate
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onStart
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onResume
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onPause
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onSaveInstanceState: 
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onStop
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onDestroy
com.hm.activitydemo E/SecondActivity: onCreate
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onStart
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onRestoreInstanceState: 
com.hm.activitydemo E/com.hm.activitydemo.activity.SecondActivity: onResume

```


### 
当Activity A 启动 Activity B的时候，两者的生命周期
```
A ------ >>>> onPause

B ------ >>>> onCreate
B ------ >>>> onStart
B ------ >>>> onResume

A ------ >>>> onStop



```


1. ActivityManagerService是运行在 system_server进程的

* [Activity 的几种启动模式及应用场景](https://blog.csdn.net/lyc088456/article/details/79389727)
* [onSaveInstanceState和onRestoreInstanceState详解](https://www.jianshu.com/p/89e0a7533dbe)


### todo 8.0及以上的hook还有问题，需要解决



### 进程保活相关

查看进程基本信息
```
adb shell ps|grep <package_name>
```
![查看进程信息](查看进程信息.png)


|   |   |
|---|---|
| 值 |解释|
|u0_a64|USER 进程当前用户|
|4001|进程ID|
|1143|进程的父进程ID|
|731164|进程的虚拟内存大小|
|31028|实际驻留”在内存中”的内存大小|
|ffffffff|不知道|
|b765af1b|不知道|
|S|不知道|
| com.hm.activitydemo|包名|


查看当前进程的优先级

我们可以通过cat /proc/进程id/oom_adj可以看到当前进程的adj值。adj值定义在com.android.server.am.ProcessList类中，这个类路径是${android-sdk-path}\sources\android-23\com\android\server\am\ProcessList.java。


```
cat /proc/进程id/oom_adj
```


###  Activity 启动流程 概述

1. 点击桌面图标，Launcher会调用onClick(View v)方法，Launcher 是 Activity 的子类，最终内部会调用 Activity 的 startActivity 方法。
2. 调用Instrumentation 的 execStartActivity 来启动 Activity。用于监控应用程序和系统之间的交互操作。
3. 调用 ActivityManagerService 的 startActivity 方法，在这个过程中，因为我们设置了 FLAG_ACTIVITY_NEW_TASK 的flag，所以会创建一个新的任务栈。
4. 然后因为我们要启动的 Activity 的应用进程还不存在，会调用 ActivityManagerService 的 startProcessLocked方 法，来启动一个新的进程。
    最终是让Zygote进程fork出一个新的进程，并根据传递的 ”android.app.ActivityThread” 字符串，通过反射执行ActivityThread的 `public static void main(String[] args)` 方法对其进行初始化。
5. 在 ActivityThread 的 静态方法中 `public static void main(String[] args)`

```java
public static void main(String[] args) {
    
    //注释1处，准备主线程的Looper
    Looper.prepareMainLooper();

    //注释2处，创建ActivityThread对象
    ActivityThread thread = new ActivityThread();
    //注释3处，内部会发送消息，调用ActivityThread的handleBindApplication方法
    thread.attach(false, startSeq);

    if(sMainThreadHandler == null) {
        sMainThreadHandler = thread.getHandler();
    }

    //注释4处，启动主线程的消息循环
    Looper.loop();

    throw new RuntimeException("Main thread loop unexpectedly exited");
}
```
注释1处，准备主线程的Looper。
注释2处，创建 ActivityThread 对象。在这个过程中，会初始化 ActivityThread 的一些成员变量：

```java
final ApplicationThread mAppThread = new ApplicationThread();
final Looper mLooper = Looper.myLooper();
final H mH = new H();
```

mAppThread 是一个 IApplicationThread.Stub 对象，当 ActivityManagerService 调用 ApplicationThread 的方法，
例如：bindApplication 方法的时候，最终是通过 ActivityThread 的 H 类型的内部类对象发送消息，调用 ActivityThread 的 handleBindApplication 方法，创建了 Application 对象，并调用了 Application 的 attach 方法和 onCreate 方法。

注释3处，调用attach方法初始化Application是调用ActivityManagerService的attachApplication方法，最终是通过ActivityThread的H类型的内部类对象发送消息。当注释4处，启动主线程的消息循环。处理消息的时候，调用ActivityThread的handleBindApplication方法
创建了Application对象，并调用了Application的attach方法和onCreate方法。到此，应用是启动起来了。

7. 应用启动以后，会真正启动Activity，是通过调用 ActivityThread 的scheduleTransaction方法，内部也是通过H发送消息，最终调用ActivityThread的handleLaunchActivity方法
   ActivityThread的performLaunchActivity方法。在performLaunchActivity内部会调用Activity的attach方法。
8. Activity的attach方法内部，会调用attachBaseContext，初始化PhoneWindow对象等。
9. 紧接着会调用Instrumentation的callActivityOnCreate方法方法，内部最终调用Activity的onCreate方法。
10. 然后ActivityThread的handleStartActivity方法，ActivityThread的handleResumeActivity方法，使Activity onResume，并使DecorView可见。

### ActivityThread 和 ApplicationThread 并不是Thread类型的对象。

但是他们都是在主线程中的对象。

* [android进程保活实践](https://www.jianshu.com/p/53c4d8303e19)
* [Android进程保活的一般套路](https://www.jianshu.com/p/1da4541b70ad)
* [Android Hook AMS](https://www.jianshu.com/p/69127e78f210)

