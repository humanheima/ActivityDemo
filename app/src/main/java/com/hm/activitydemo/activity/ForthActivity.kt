package com.hm.activitydemo.activity

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.hm.activitydemo.R
import kotlinx.android.synthetic.main.activity_forth.*

/**
 * Crete by dumingwei on 2019/3/18
 * Desc: 用来测试onNewIntent方法
 *
 */
class ForthActivity : AppCompatActivity() {

    private val TAG = "ForthActivity"

    companion object {

        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, ForthActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forth)
        btnOnNewIntent.setOnClickListener {
            MainActivity.launch(this@ForthActivity)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: ")
    }
}
