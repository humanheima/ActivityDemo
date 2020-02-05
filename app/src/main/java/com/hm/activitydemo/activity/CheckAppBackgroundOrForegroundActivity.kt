package com.hm.activitydemo.activity

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.hm.activitydemo.R


/**
 * Crete by dumingwei on 2020-02-05
 * Desc: 判断App处于前台还是后台
 *
 */
class CheckAppBackgroundOrForegroundActivity : AppCompatActivity() {

    private val TAG = "CheckAppBackgroundOrFor"

    companion object {

        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, CheckAppBackgroundOrForegroundActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_app_background_or_foreground)
    }

    fun onClick(v: View) {
        when (v.id) {

            R.id.btnRunningTask -> {
                getForegroundActivity()
            }
            R.id.btnRunningProcess -> {

                val mAppForeground = isAppForeground(this)

                Log.d(TAG, "onClick: mAppForeground ? $mAppForeground")

                val foregroundApp = getForegroundApp(this)
                Log.d(TAG, "onClick: 前台的是那个应用 $foregroundApp")
            }
            R.id.btnActivityLifecycleCallbacks -> {
                //在App中注册生命周期监听registerActivityLifecycleCallbacks

            }
            R.id.btnUsageStatsManager -> {


            }
            R.id.btnAccessibilityService -> {
                //暂时不去了解

            }
            R.id.btnParseProcess -> {

            }

        }

    }

    /**
     * 当一个App处于前台时，会处于RunningTask这个栈的栈顶，所以可以取出RunningTask栈顶的任务进程，
     * 与需要判断的App的包名进行比较，来达到目的。
     * getRunningTask方法在5.0以上已经被废弃，只会返回自己和系统的一些不敏感的task，不再返回其他应用的task，
     * 用此方法来判断自身App是否处于后台仍然有效，但是无法判断其他应用是否位于前台，因为不能再获取信息。
     */
    private fun getForegroundActivity(): String? {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTaskList: List<ActivityManager.RunningTaskInfo> = activityManager.getRunningTasks(1)
        if (runningTaskList.isNullOrEmpty()) {
            Log.d(TAG, "running task is null, ams is abnormal!!!")
            return null
        }
        for (taskInfo in runningTaskList) {
            Log.d(TAG, "getForegroundActivity: ${taskInfo.topActivity.packageName}")
        }

        val mRunningTask = runningTaskList[0]

        return mRunningTask.topActivity.packageName

    }


    /**
     * 判断当前app是不是在前台
     */
    fun isAppForeground(context: Context): Boolean {

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val runningProcessList: List<ActivityManager.RunningAppProcessInfo> = activityManager.runningAppProcesses


        if (runningProcessList.isNullOrEmpty()) {
            Log.d(TAG, "runningAppProcessInfoList is null!")
            return false
        }
        for (processInfo in runningProcessList) {
            if (processInfo.processName == packageName
                    && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                Log.d(TAG, "isAppForeground: ${processInfo.processName}")
                return true
            }
        }
        return false
    }

    /**
     * 判断在前台的是哪个应用
     */
    fun getForegroundApp(context: Context): String? {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = am.runningAppProcesses ?: return null

        for (ra in runningAppProcesses) {
            if (ra.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return ra.processName
            }
        }
        return null
    }


}
