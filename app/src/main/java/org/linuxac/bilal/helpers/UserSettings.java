/*
 *  Copyright © 2015 Djalel Chefrour
 *
 *  This file is part of Bilal.
 *
 *  Bilal is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Bilal is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Bilal.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.linuxac.bilal.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.arabeyes.prayertime.Method;
import org.arabeyes.prayertime.Prayer;
import org.linuxac.bilal.BuildConfig;
import org.linuxac.bilal.R;
import org.linuxac.bilal.databases.LocationsDBHelper;
import org.linuxac.bilal.datamodels.City;

import java.util.Locale;

public class UserSettings {
    private static String TAG = "UserSettings";

//    public enum Muezzin {
//        ABDULBASET, ALIMULLA, ALQATAMI, ALQAZAZ, ASEREHY, JOSHAR, RIAD
//    }

    public static boolean isAlarmEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean res = sharedPref.getBoolean("notifications_prayer_time", false);
        Log.d(TAG, "notifications_prayer_time = " + res);
        return res;
    }

    public static boolean isAthanEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean res = sharedPref.getBoolean("notifications_athan", false);
        Log.d(TAG, "notifications_athan = " + res);
        return res;
    }

    public static int getMuezzin(Context context, int i) {
        int resId;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String muezzin = sharedPref.getString("notifications_muezzin", "ABDELBASET");
        Log.d(TAG, "notifications_muezzin = " + muezzin);

        if (i == 0 || i == Prayer.NB_PRAYERS) {
            switch (muezzin) {
                case "ABDELBASET":
                    resId = R.raw.abdulbaset_fajr;
                    break;
                case "ALIMULLA":
                    resId = R.raw.alimulla_fajr;
                    break;
                case "ALQATAMI":
                    resId = R.raw.alqatami_fajr;
                    break;
                case "ASEREHY":
                    resId = R.raw.aserehy_fajr;
                    break;
                case "JOSHAR":
                    resId = R.raw.joshar_fajr;
                    break;
                case "KEFAH":
                    resId = R.raw.kefah_fajr;
                    break;
                case "RIAD":
                    resId = R.raw.riad_fajr;
                    break;
                default:
                    resId = R.raw.abdulbaset_fajr;
                    break;
            }
        }
        else {
            switch (muezzin) {
                case "ABDELBASET":
                    resId = R.raw.abdulbaset;
                    break;
                case "ALIMULLA":
                    resId = R.raw.alimulla;
                    break;
                case "ALQATAMI":
                    resId = R.raw.alqatami;
                    break;
                case "ASEREHY":
                    resId = R.raw.aserehy;
                    break;
                case "JOSHAR":
                    resId = R.raw.joshar;
                    break;
                case "KEFAH":
                    resId = R.raw.kefah;
                    break;
                case "RIAD":
                    resId = R.raw.riad;
                    break;
                default:
                    resId = R.raw.abdulbaset;
                    break;
            }
        }

        return resId;
    }

    public static boolean isNotificationEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean res = sharedPref.getBoolean("notifications_screen", false);
        Log.d(TAG, "notifications_screeh = " + res);
        return res;
    }

    public static boolean isVibrateEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean res = sharedPref.getBoolean("notifications_vibrate", false);
        Log.d(TAG, "notifications_vibrate = " + res);
        return res;
    }

    private int getPrefSyncFrequency(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String str = sharedPref.getString("sync_frequency", "180");
        return Integer.parseInt(str);
    }

    public static City getCity(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String cityStream = sharedPref.getString("locations_search_city", "");
        if (!cityStream.isEmpty()) {
            return City.deserialize(cityStream);
        }
        return null;
    }

    public static void setCity(Context context, City city) {
        // update Locale as it depends on city country
        setLocale(context, null, city.getCountryCode());

        // serialize & save new city in shared pref.
        String cityStream = city.serialize();
        if (null != cityStream) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("locations_search_city", cityStream);
            editor.apply();
        }
    }

    private static City updateCity(Context context, String language) {
        City city = getCity(context);
        if (null != city) {
            city = (new LocationsDBHelper(context)).getCity(city.getId(), language.toUpperCase());
            if (null != city) {
                setCity(context, city);
                return city;
            }
        }
        return null;
    }

    public static String getCityName(Context context) {
        City city = getCity(context);
        return null != city ? city.toShortString() : context.getString(R.string.pref_undefined_city);
    }

    public static int getCalculationMethod(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String method = sharedPref.getString("locations_method", String.valueOf(Method.V2_MWL));
        Log.w(TAG, "getCalculationMethod: " + method);
        return Integer.parseInt(method);
    }

    public static boolean isMathhabHanafi(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("locations_mathhab_hanafi", false);
    }

    public static int getRounding(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("locations_rounding", true) ? 1 : 0;
    }

    public static String getLanguage(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("general_language", "ar"); // TODO flag icons
    }

    public static Locale getLocale(Context context) {
        String lang = getLanguage(context);
        City city = getCity(context);
        return null != city ? new Locale(lang, city.getCountryCode()) : new Locale(lang);
    }

    private static void setLocale(Context context, Locale locale) {
        // Update sys. config
        Resources res = context.getApplicationContext().getResources();
        Configuration config = res.getConfiguration();
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
        }
        else {
            config.locale = locale;
        }
        res.updateConfiguration(config, res.getDisplayMetrics());
        Locale.setDefault(locale);

        // TODO set inputmethod language for SearchCity
    }

    public static void setLocale(Context context, String newLang, String newCC) {
        // saving in shared prefs. is handled by the framework after the caller

        if (null == newLang) {      // there is a new CC
            newLang = getLanguage(context);
        }
        else {                      // there is a new language
            // update saved city as it depends on it
            City city = updateCity(context, newLang);
            if (null != city) {
                newCC = city.getCountryCode();
            }
        }

        setLocale(context, null != newCC ? new Locale(newLang, newCC) : new Locale(newLang));
    }

    public static void loadLocale(Context context) {
        setLocale(context, getLocale(context));
    }
}