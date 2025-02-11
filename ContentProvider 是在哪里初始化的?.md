# 调用路径

在 ActivityThread handleBindApplication 方法中。在创建了 Application 之后， 和 调用 Application 的 onCreate 方法之前。

```
onCreate:java.lang.Throwable
at com.hm.activitydemo.TempContentProvider.onCreate(TempContentProvider.kt:17)
at android.content.ContentProvider.attachInfo(ContentProvider.java:1917)
at android.content.ContentProvider.attachInfo(ContentProvider.java:1892)
at android.app.ActivityThread.installProvider(ActivityThread.java:6239)
at android.app.ActivityThread.installContentProviders(ActivityThread.java:5805)
at android.app.ActivityThread.handleBindApplication(ActivityThread.java:5722)
at android.app.ActivityThread.-wrap1(Unknown Source:0)
at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1656)
at android.os.Handler.dispatchMessage(Handler.java:106)
at android.os.Looper.loop(Looper.java:164)
at android.app.ActivityThread.main(ActivityThread.java:6494)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:438)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:807)
                                                                   
```
