package com.hm.activitydemo.hook;

import android.os.Build;
import android.os.Handler;

import com.hm.activitydemo.HookLibrary.FieldUtil;
import com.hm.activitydemo.HookLibrary.HCallback;
import com.hm.activitydemo.HookLibrary.IActivityManagerProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * Created by dumingwei on 2019/2/21
 * Desc:
 */
public class HookHelper {

    public static final String TARGET_INTENT = "TARGET_INTENT";

    /**
     * 替换IActivityManager
     *
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
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
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerClazz}, new IActivityManagerProxy(iActivityManager));
        mInstanceField.set(defaultSingleton, proxy);
    }

    /*public static void hookAMS() throws Exception {
        Object defaultSingleton=null;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> activityManagerClazz = Class.forName("android.app.ActivityManager");
            defaultSingleton=  com.hm.activitydemo.HookLibrary.FieldUtil.getField(activityManagerClazz,null,"IActivityManagerSingleton");
        } else {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            defaultSingleton=  com.hm.activitydemo.HookLibrary.FieldUtil.getField(activityManagerNativeClass,null,"gDefault");
        }
        Class<?> singletonClazz = Class.forName("android.util.Singleton");
        Field mInstanceField= com.hm.activitydemo.HookLibrary.FieldUtil.getField(singletonClazz,"mInstance");
        Object iActivityManager = mInstanceField.get(defaultSingleton);
        Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { iActivityManagerClass }, new IActivityManagerProxy(iActivityManager));
        mInstanceField.set(defaultSingleton, proxy);
    }*/

}
