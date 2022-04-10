package com.hm.activitydemo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.hm.activitydemo.hook.HookHelper;


/**
 * Created by dumingwei on 2019/2/22
 * Desc:
 */
public class App extends Application {

    private final String TAG = "App";

    private int mActivityCount = 0;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            Log.i(TAG, "attachBaseContext: ");
            /**
             * 这两行代码需要一起调用
             */
            HookHelper.hookAMS();
            HookHelper.hookHandler();

            //HookHelper.hookInstrumentation(base);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "attachBaseContext: " + e.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d(TAG, "onActivityStarted: ");
                mActivityCount++;

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(TAG, "onActivityStopped: ");
                mActivityCount--;
                if (mActivityCount == 0) {
                    Log.d(TAG, "onActivityStopped: 应用进入后台");
                }

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }
}
