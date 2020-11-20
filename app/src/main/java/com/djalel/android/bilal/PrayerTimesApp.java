package com.djalel.android.bilal;

import com.djalel.android.bilal.helpers.UserSettings;
import com.readystatesoftware.android.sqliteassethelper.BuildConfig;

import java.util.Locale;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import timber.log.Timber;
import static timber.log.Timber.DebugTree;

public class PrayerTimesApp extends Application {

    // THIS IS A SINGLETON
    private static PrayerTimesApp mContext;
    public static PrayerTimesApp getApplication() { return mContext; }

    private static Locale mLocale;
    public static void setLocale(Locale locale) { mLocale = locale; }

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

    public static Context updateLocale(Context context)
    {
        Configuration configuration = context.getResources().getConfiguration();

        if (mLocale != null) {
            Locale.setDefault(mLocale);
            configuration.setLocale(mLocale);
        }

        return context.createConfigurationContext(configuration);
    }
}
