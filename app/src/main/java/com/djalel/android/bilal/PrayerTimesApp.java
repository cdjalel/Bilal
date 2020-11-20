package com.djalel.android.bilal;

import com.djalel.android.bilal.helpers.UserSettings;
import com.readystatesoftware.android.sqliteassethelper.BuildConfig;

import java.util.Locale;

import android.app.Application;
import timber.log.Timber;
import static timber.log.Timber.DebugTree;

public class PrayerTimesApp extends Application {

    // THIS IS A SINGLETON
    static private PrayerTimesApp mContext;
    static public PrayerTimesApp getApplication() { return mContext; }

    private Locale mLocale;
    public void setLocale(Locale locale) { mLocale = locale; }
    public Locale getLocale() { return  mLocale; }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mLocale = UserSettings.getLocale(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }
        Timber.d("PrayerTimesApp");
    }
}
