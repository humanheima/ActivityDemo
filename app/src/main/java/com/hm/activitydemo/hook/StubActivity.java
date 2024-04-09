package com.hm.activitydemo.hook;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import com.hm.activitydemo.R;

/**
 * Created by dumingwei on 2019/2/21
 * Desc:
 */

public class StubActivity extends AppCompatActivity {

    private static final String TAG = "StubActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");
        setContentView(R.layout.activity_stub);
    }
}
