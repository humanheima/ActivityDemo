package com.hm.activitydemo.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;

import com.hm.activitydemo.R;
import com.hm.activitydemo.base.BaseActivity;
import com.hm.activitydemo.hook.InstrumentationProxy;
import com.hm.activitydemo.hook.TargetActivity;

import java.lang.reflect.Field;

public class MainActivity extends BaseActivity {

    public static void launch(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected int bindLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        TAG = getClass().getName();
        //replaceActivityInstrumentation(this);
    }

    private void replaceActivityInstrumentation(Activity activity) {
        try {
            Field field = Activity.class.getDeclaredField("mInstrumentation");
            Log.e(TAG, "replaceActivityInstrumentation: field=" + field);
            field.setAccessible(true);
            Instrumentation instrumentation = (Instrumentation) field.get(activity);
            Instrumentation instrumentationProxy = new InstrumentationProxy(instrumentation);
            field.set(activity, instrumentationProxy);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchFieldException: " + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "IllegalAccessException: " + e.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG, "onConfigurationChanged");
    }

    public void startSecondActivity(View view) {
        //SecondActivity.launch(this);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setComponent(new ComponentName(this, SecondActivity.class));
        intent.setComponent(new ComponentName(this, "com.hm.activitydemo.activity.SecondActivity"));
        startActivity(intent);
    }

    public void hookAMS(View view) {
        Intent intent = new Intent(MainActivity.this, TargetActivity.class);
        startActivity(intent);
    }
}
