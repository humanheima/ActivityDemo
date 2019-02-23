package com.hm.activitydemo.hook;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.RuntimeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class InstrumentationProxy extends Instrumentation {


    private final String TAG = "InstrumentationProxy";


    private Instrumentation instrumentation;
    private PackageManager mPackageManager;


    public InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager) {
        this.instrumentation = instrumentation;
        this.mPackageManager = packageManager;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String intentName = intent.getStringExtra(HookHelper.TARGET_INTENT_NAME);
        if (!TextUtils.isEmpty(intentName)) {
            return super.newActivity(cl, intentName, intent);
        }
        return super.newActivity(cl, className, intent);
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);

        //如果Activity没有在manifest文件中注册
        if (infos == null || infos.size() == 0) {
            intent.putExtra(HookHelper.TARGET_INTENT_NAME, intent.getComponent().getClassName());
            intent.setClassName(who, "com.hm.activitydemo.hook.StubActivity");
        }

        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class
            );
            Log.e(TAG, "execStartActivity: hook successful");
            return (ActivityResult) execStartActivity.invoke(instrumentation, who, contextThread, token, target,
                    intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}

