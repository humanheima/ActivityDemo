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