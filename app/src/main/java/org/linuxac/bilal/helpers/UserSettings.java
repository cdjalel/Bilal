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
import android.preference.PreferenceManager;
import android.util.Log;

import org.arabeyes.prayertime.Method;
import org.arabeyes.prayertime.Prayer;
import org.linuxac.bilal.R;

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

    public static int getCityID(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getInt("locations_search_city", -1);
    }


    public static void setCityID(Context context, int id) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("locations_search_city", id);
        editor.apply();
    }

    public static int getCalculationMethod(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String method = sharedPref.getString("locations_method", String.valueOf(Method.V2_MWL));
        Log.w(TAG, "getCalculationMethod: " + method);
        return Integer.parseInt(method);
    }

    public static boolean getCalculationRound(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("locations_rounding", true);
    }

    public static String getLocale(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("general_language", "en"); // TODO ar_DZ
    }

}
