package com.hm.activitydemo;

import android.os.Build;
import android.util.Log;

/**
 * Created by p_dmweidu on 2024/8/26
 * Desc: 列出设备信息
 */
public class DeviceUtil {


    public static void printDevice() {

        Log.i("DeviceUtil", "MODEL: " + android.os.Build.MODEL);
        Log.i("DeviceUtil", "RELEASE: " + android.os.Build.VERSION.RELEASE);
        Log.i("DeviceUtil", "SDK_INT: " + android.os.Build.VERSION.SDK_INT);
        Log.i("DeviceUtil", "BRAND: " + android.os.Build.BRAND);
        Log.i("DeviceUtil", "PRODUCT: " + android.os.Build.PRODUCT);
        Log.i("DeviceUtil", "MANUFACTURER: " + android.os.Build.MANUFACTURER);
        Log.i("DeviceUtil", "DISPLAY: " + android.os.Build.DISPLAY);
        Log.i("DeviceUtil", "HOST: " + android.os.Build.HOST);
        Log.i("DeviceUtil", "ID: " + android.os.Build.ID);
        Log.i("DeviceUtil", "DEVICE: " + Build.DEVICE);



    }
}

