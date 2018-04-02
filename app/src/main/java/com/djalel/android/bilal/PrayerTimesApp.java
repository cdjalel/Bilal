package com.djalel.android.bilal;

import android.app.Application;
import timber.log.Timber;
import static timber.log.Timber.DebugTree;

public class PrayerTimesApp extends Application {
    @Override public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }

        Timber.d("PrayerTimesApp");
    }
}
