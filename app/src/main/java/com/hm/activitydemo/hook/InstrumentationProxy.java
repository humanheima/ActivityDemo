package com.hm.activitydemo.hook;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.RuntimeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InstrumentationProxy extends Instrumentation {


    private final String TAG = "InstrumentationProxy";


    Instrumentation instrumentation;

    public InstrumentationProxy(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }


    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        Log.e(TAG, "execStartActivity: hook successful");
        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class
            );
            return (ActivityResult) execStartActivity.invoke(instrumentation, who, contextThread, token, target,
                    intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}


    /* fun execStartActivity(who: Context, contextThread: IBinder, token: IBinder, target: Activity,
                          intent: Intent, requestCode: Int, options: Bundle?): ActivityResult {

        Log.e(TAG, "execStartActivity: hook success:$who")
        try {
            val execStartActivity = Instrumentation::class.java.getMethod("execStartActivity",
                    Context::class.java,
                    IBinder::class.java,
                    IBinder::class.java,
                    Activity::class.java,
                    Intent::class.java,
                    Int::class.java,
                    Bundle::class.java
            )

            return execStartActivity.invoke(mInstrumentation, who, contextThread, token, target, intent, requestCode, options) as ActivityResult

        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }
*/

