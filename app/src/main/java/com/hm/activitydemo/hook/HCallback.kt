package com.hm.activitydemo.hook

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log

/**
 * Created by dumingwei on 2019/2/23
 * Desc:
 * 参考链接：http://liuwangshu.cn/framework/applicationprocess/1.html
 */
class HCallback(val mHandler: Handler) : Handler.Callback {

    private val TAG = javaClass.simpleName

    companion object {

        /**
         * 100 就是ActivityThread.H 中的LAUNCH_ACTIVITY
         * public static final int LAUNCH_ACTIVITY = 100;
         *
         * 参考7版本的SDK http://androidxref.com/7.1.2_r36/xref/frameworks/base/core/java/android/app/ActivityThread.java
         */
        val LAUNCH_ACTIVITY = 100
    }


    override fun handleMessage(msg: Message): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            //SDK版本26，也就是Android 8.0以上还没处理
            Log.i(TAG, "handleMessage: 8.0以上还没处理")
        } else {
            Log.i(TAG, "handleMessage: 8.0以下处理 msg = " + msg)
            if (msg.what == LAUNCH_ACTIVITY) {
                val obj = msg.obj
                try {
                    //Note: 得到消息中的Intent（启动SubActivity的Intent）
                    val intent: Intent =
                        FieldUtil.getField(obj.javaClass, obj, "intent") as Intent
                    //Note: 得到此前保存起来的Intent(启动TargetActivity的Intent)
                    val target: Intent =
                        intent.getParcelableExtra<Intent>(HookHelper.TARGET_INTENT)
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
