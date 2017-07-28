package org.linuxac.bilal.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import org.linuxac.bilal.helpers.UserSettings;

import java.util.Locale;

public class DeviceLocaleChangeReceiver extends BroadcastReceiver {

    protected static final String TAG = "LocaleChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, action);
        String prefLanguage = UserSettings.getPrefLanguage(context);
        if (UserSettings.languageUsesDeviceSettings(context, prefLanguage)) {
            UserSettings.setLocale(context, prefLanguage, null);
        }
    }
}
