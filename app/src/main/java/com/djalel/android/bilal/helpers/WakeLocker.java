package com.djalel.android.bilal.helpers;

import android.content.Context;
import android.os.PowerManager;

public class WakeLocker {
    private static PowerManager.WakeLock wakeLock = null;

    public static void acquire(Context context) {
        if (wakeLock != null) {
            wakeLock.release();
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
            PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeLocker");
        wakeLock.acquire();
    }

    public static void release() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
