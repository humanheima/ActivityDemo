package com.hm.activitydemo.activity;

import android.content.Context;
import android.view.View;

import com.hm.activitydemo.R;
import com.hm.activitydemo.base.BaseActivity;

public class ThirdActivity extends BaseActivity {

    @Override
    protected int bindLayout() {
        TAG = getClass().getName();
        return R.layout.activity_third;
    }

    @Override
    protected void initData() {

    }

    public void startMainActivity(View view) {
        MainActivity.launch(this);
    }
}
