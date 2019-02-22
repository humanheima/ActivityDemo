package com.hm.activitydemo.HookLibrary;

import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/3/24 0024.
 */

public class IActivityManagerProxy implements InvocationHandler {
    private Object mActivityManager;
    private static final String TAG = "IActivityManagerProxy";

    public IActivityManagerProxy(Object activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
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
            String packageName = "com.hm.activitydemo";
            subIntent.setClassName(packageName,packageName+".hook.StubActivity");
            subIntent.putExtra(HookHelper.TARGET_INTENT, intent);
            args[index] = subIntent;
            Log.d(TAG, "hook成功");
        }
        return method.invoke(mActivityManager, args);
    }
}
