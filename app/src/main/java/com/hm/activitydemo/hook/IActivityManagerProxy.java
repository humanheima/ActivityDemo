package com.hm.activitydemo.hook;

import android.content.Intent;
import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by dumingwei on 2019/2/21
 * Desc:
 */

public class IActivityManagerProxy implements InvocationHandler {

    private Object mActivityManager;
    private final String TAG = "IActivityManagerProxy";

    public IActivityManagerProxy(Object activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.i(TAG, "invoke: methodName " + method.getName());
        if ("startActivity".equals(method.getName())) {
            Log.i(TAG, "invoke: startActivity");
            //找到intent
            Intent intent = null;
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            intent = (Intent) args[index];
            Intent subIntent = new Intent();
            //包名别写错了
            subIntent.setClassName("com.hm.activitydemo", "com.hm.activitydemo.hook.StubActivity");
            subIntent.putExtra(HookHelper.TARGET_INTENT, intent);
            args[index] = subIntent;
            Log.e(TAG, "invoke: hook成功");
        }
        return method.invoke(mActivityManager, args);
    }
}
