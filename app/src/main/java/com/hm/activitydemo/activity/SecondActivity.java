package com.hm.activitydemo.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.hm.activitydemo.R;
import com.hm.activitydemo.base.BaseActivity;

public class SecondActivity extends BaseActivity {

    private TextView textView;

    public static void launch(Context context) {
        Intent starter = new Intent(context, SecondActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "onSaveInstanceState: ");
        outState.putString("text", "hello text");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.e(TAG, "onRestoreInstanceState: ");
        textView.setText(savedInstanceState.getString("text"));
    }

    @Override
    protected int bindLayout() {
        return R.layout.activity_second;
    }

    @Override
    protected void initData() {
        TAG = getClass().getName();
        textView = findViewById(R.id.textView);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.e(TAG, "ORIENTATION_LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.e(TAG, "ORIENTATION_PORTRAIT");
        }
    }

    public void startThirdActivity(View view) {
        Intent starter = new Intent("com.hm.action.d");
        starter.setType("text/plain");
        if (starter.resolveActivity(getPackageManager()) != null) {
            startActivity(starter);
        }
    }
}
