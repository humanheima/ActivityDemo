package com.hm.activitydemo.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;

import com.hm.activitydemo.R;
import com.hm.activitydemo.base.BaseActivity;

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
        Log.e(TAG, "onCreate");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG, "onConfigurationChanged");
    }

    public void startSecondActivity(View view) {
        SecondActivity.launch(this);
    }
}
