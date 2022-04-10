package com.hm.activitydemo.hook;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * Created by dumingwei on 2019/2/21
 * Desc:
 */
public class HookHelper {

    public static final String TARGET_INTENT = "TARGET_INTENT";
    public static final String TARGET_INTENT_NAME = "TARGET_INTENT_NAME";

    private static final String TAG = "HookHelper";

    /**
     * 替换IActivityManager
     *
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
        try {
            Object defaultSingleton = null;
            if (Build.VERSION.SDK_INT >= 26) {//8.0以上
                Class<?> activityManageClazz = Class.forName("android.app.ActivityManager");
                //获取ActivityManager中的IActivityManagerSingleton字段
                defaultSingleton = FieldUtil.getField(activityManageClazz, null, "IActivityManagerSingleton");
            } else {
                Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
                defaultSingleton = FieldUtil.getField(activityManagerNativeClazz, null, "gDefault");
            }
            Class<?> singletonClazz = Class.forName("android.util.Singleton");
            Field mInstanceField = FieldUtil.getField(singletonClazz, "mInstance");
            //获取IActivityManager
            Object iActivityManager = mInstanceField.get(defaultSingleton);
            Class<?> iActivityManagerClazz = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iActivityManagerClazz},
                    new IActivityManagerProxy(iActivityManager)
            );
            mInstanceField.set(defaultSingleton, proxy);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "hookAMS: " + e.getMessage());
        }
    }

    public static void hookHandler() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object currentActivityThread = FieldUtil.getField(activityThreadClass, null, "sCurrentActivityThread");
        Field mHField = FieldUtil.getField(activityThreadClass, "mH");
        Handler mH = (Handler) mHField.get(currentActivityThread);
        FieldUtil.setField(Handler.class, mH, "mCallback", new HCallback(mH));

    }

    public static void hookInstrumentation(Context context) throws Exception {
        Class<?> contextImplClazz = Class.forName("android.app.ContextImpl");
        Field mMainThreadField = FieldUtil.getField(contextImplClazz, "mMainThread");
        Object activityThread = mMainThreadField.get(context);
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        Field mInstrumentationField = FieldUtil.getField(activityThreadClazz, "mInstrumentation");
        FieldUtil.setField(activityThreadClazz, activityThread, "mInstrumentation",
                new InstrumentationProxy((Instrumentation) mInstrumentationField.get(activityThread),
                        context.getPackageManager()));
    }

}
