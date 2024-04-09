### 仅在Android 8.0上测试通过，大于8.0的版本会有问题
App 中开始hook。

```java
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    try {
        Log.i(TAG, "attachBaseContext: ");
        /**
         * 注释1处，这两行代码需要一起调用
         */
        HookHelper.hookAMS();
        HookHelper.hookHandler();

        //HookHelper.hookInstrumentation(base);

    } catch(Exception e) {
        e.printStackTrace();
        Log.e(TAG, "attachBaseContext: " + e.getMessage());
    }
}
```

AndroidManifest.xml 中 占位的Activity

```xml
<activity android:name=".hook.StubActivity" />
```

MainActivity 测试

```java
/**
 * 这个Activity是没有在AndroidManifest.xml中注册的，通过hook的方式启动。替换占位的StubActivity
 *
 * @param view
 */
public void hookActivity(View view) {
    Intent intent = new Intent(MainActivity.this, TargetActivity.class);
    startActivity(intent);
}
```

### 疑问

能hook第一个启动的Activity吗？。测试下来发现应用启动MainActivity的时候，没有调用 startActivity() 方法，没法hook。

