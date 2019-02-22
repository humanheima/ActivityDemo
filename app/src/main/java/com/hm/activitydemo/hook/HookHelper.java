package com.hm.activitydemo.hook;

import android.os.Build;

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

}
