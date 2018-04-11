package com.djalel.android.bilal.helpers;

import android.content.Context;
import android.os.PowerManager;

import com.djalel.android.bilal.services.AthanService;

import timber.log.Timber;

public class WakeLocker {
    private static PowerManager.WakeLock sWakeLock = null;

    public static void acquire(Context context) {
        if (sWakeLock != null) {
            if (sWakeLock.isHeld()) {
                Timber.w("sWakeLock isHeld");
                return;
            }
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (null != pm) {
            sWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, "WakeLocker");
            Timber.i("sWakeLock.acquire");
            sWakeLock.acquire(AthanService.ATHAN_DURATION
                    + 500 // give some room for notif timeout first
                    );
        }

    }

    public static void release() {
        if (sWakeLock != null) {
            if (sWakeLock.isHeld()) {
                Timber.i("sWakeLock.release");
                sWakeLock.release();
            }
            else {
                Timber.i("sWakeLock released by its own timeout");
            }
            sWakeLock = null;
        }
    }
}
