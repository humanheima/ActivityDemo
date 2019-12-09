package com.hm.activitydemo.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
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

    private var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d(TAG, "handleMessage: ${msg.what}")
        }
    }
    var run = true

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
        btnSendInMain.setOnClickListener {
            for (i in 0 until 1000000) {
                Log.d(TAG, "send message: ")
                handler.sendEmptyMessage(i)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        run = false
        handler.removeCallbacksAndMessages(null)
    }

}
