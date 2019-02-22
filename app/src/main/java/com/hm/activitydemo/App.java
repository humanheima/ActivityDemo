package com.hm.activitydemo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.hm.activitydemo.hook.HookHelper;


/**
 * Created by dumingwei on 2019/2/22
 * Desc:
 */
public class App extends Application {

    private final String TAG = "App";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            HookHelper.hookAMS();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "attachBaseContext: " + e.getMessage());
        }
    }
}
