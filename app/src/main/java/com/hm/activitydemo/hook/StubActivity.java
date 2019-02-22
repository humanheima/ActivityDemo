package com.hm.activitydemo.hook;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hm.activitydemo.R;

/**
 * Created by dumingwei on 2019/2/21
 * Desc:
 */

public class StubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stub);
    }
}
