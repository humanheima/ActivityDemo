package com.hm.activitydemo.hook

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import java.lang.Exception

/**
 * Created by dumingwei on 2019/2/23
 * Desc:
 */
class HCallback(val mHandler: Handler) : Handler.Callback {

    private val TAG = javaClass.simpleName

    companion object {

        /**
         * 100 就是ActivityThread.H 中的LAUNCH_ACTIVITY
         * public static final int LAUNCH_ACTIVITY = 100;

         */
        val LAUNCH_ACTIVITY = 100
    }


    override fun handleMessage(msg: Message): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {

        } else {
            if (msg.what == LAUNCH_ACTIVITY) {
                val obj = msg.obj
                try {
                    //得到消息中的Intent（启动SubActivity的Intent）
                    val intent: Intent = FieldUtil.getField(obj.javaClass, obj, "intent") as Intent
                    //得到此前保存起来的Intent(启动TargetActivity的Intent)
                    val target: Intent = intent.getParcelableExtra<Intent>(HookHelper.TARGET_INTENT)
                    intent.component = target.component

                } catch (e: Exception) {
                    Log.d(TAG, "handleMessage: ${e.message}")
                }

            }
            mHandler.handleMessage(msg)
        }

        return true
    }
}
