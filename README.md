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

### 关于onNewIntent方法 ，Activity的启动模式无论是 singleTop,singleTask,singleInstance都会调用。
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

1. ActivityManagerService是运行在 system_server进程的