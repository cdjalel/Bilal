/*
 *  Copyright © 2017 Djalel Chefrour
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

package org.linuxac.bilal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.arabeyes.prayertime.Method;
import org.arabeyes.prayertime.PTLocation;
import org.arabeyes.prayertime.Prayer;
import org.arabeyes.prayertime.PrayerTime;
import org.linuxac.bilal.helpers.PrayerTimes;
import org.linuxac.bilal.helpers.UserSettings;
import org.linuxac.bilal.receivers.AlarmReceiver;
import org.linuxac.bilal.receivers.BootAndTimeChangeReceiver;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.content.Context.ALARM_SERVICE;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    private static boolean sLocationIsSet = false;    // TODO rm as it's a setting
    public static String sCityName = "Souk Ahras";
    public static PTLocation sPTLocation = new PTLocation(36.28639, 7.95111, 1, 0, 697, 1010, 10);
    // http://dateandtime.info/citycoordinates.php?id=2479215

    private static Method sCalculationMethod;   // TODO rm as it's a setting
    static {
        sCalculationMethod = new Method();
        sCalculationMethod.setMethod(Method.MUSLIM_LEAGUE);
        sCalculationMethod.round = 0;
        sLocationIsSet = true;
    }

    private static final GregorianCalendar sLongLongTimeAgo = new GregorianCalendar(0,0,0);
    private static GregorianCalendar sLastTime = sLongLongTimeAgo;
    public static PrayerTimes sPrayerTimes = null;      // TODO privatize & add getters

    public static final String ALARM_TXT = "org.linuxac.bilal.ALARM_TXT";
    private static PendingIntent sAlarmIntent = null;

    private AlarmScheduler() {}

    public static void enableAlarm(Context context)
    {
        Log.d(TAG, "Enabling Alarm.");
        enableBootAndTimeChangeReceiver(context);
        updatePrayerTimes(context, true);
    }

    public static void disableAlarm(Context context)
    {
        Log.d(TAG, "Disabling Alarm.");
        cancelAlarm(context);
        disableBootAndTimeChangeReceiver(context);
    }
/*

    private static void logBootAndTimeChangeReceiverSetting(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        Log.d(TAG, "BootAndTimeChangeReceiver Setting = " +
                pm.getComponentEnabledSetting(receiver));
    }
*/

    private static void enableBootAndTimeChangeReceiver(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private static void disableBootAndTimeChangeReceiver(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }


    private static boolean sameDay(GregorianCalendar a, GregorianCalendar b)
    {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                a.get(Calendar.DATE) == b.get(Calendar.DATE);
    }


    /*
     * Here be dragons. This is an app entry point. All bets are off!
     * See comment in MainActivity.onCreate()
     */
    public static void updatePrayerTimes(Context context, boolean enableAlarm)
    {
        Log.i(TAG, "updatePrayerTimes(..., " + enableAlarm + ")");

//        logBootAndTimeChangeReceiverSetting(context);

        // TODO UserSettings.getLocation
        // TODO UserSettings.getMethod
        if (!sLocationIsSet) {
            Log.w(TAG, "Location not set! Nothing todo until user starts UI.");
            return;
        }

        // In SettingsActivity, listener calls us before setting is committed to shared prefs.
        boolean setAlarm = enableAlarm || UserSettings.isAlarmEnabled(context);
        GregorianCalendar nowCal = new GregorianCalendar();
        if (sameDay(sLastTime, nowCal)) {
            int oldNext = sPrayerTimes.getNextIndex();
            sPrayerTimes.updateCurrent(nowCal);
            if (setAlarm && !enableAlarm && oldNext == sPrayerTimes.getNextIndex()) {
                setAlarm = false;
                Log.i(TAG, "Keep old alarm.");
            }
            Log.d(TAG, "Call it a day :)");
        }
        else {
            Log.d(TAG, "Last time: " + sPrayerTimes.format(sLastTime));
            GregorianCalendar[] ptCal = getPrayerTimes(context, nowCal);
            sPrayerTimes = new PrayerTimes(nowCal, ptCal);
            sLastTime = nowCal;
        }

        Log.d(TAG, "Current time: " + sPrayerTimes.format(nowCal));
        Log.d(TAG, "Current prayer: " + getPrayerName(context, sPrayerTimes.getCurrentIndex()));
        Log.i(TAG, "Next prayer: " + getPrayerName(context, sPrayerTimes.getNextIndex()));

        if (setAlarm) {
            scheduleAlarm(context);
        }
    }

    @NonNull
    protected static GregorianCalendar[] getPrayerTimes(Context context, GregorianCalendar nowCal)
    {
        int i;
        Prayer prayer = new Prayer();
        Date today = nowCal.getTime();
        GregorianCalendar[] ptCal = new GregorianCalendar[Prayer.NB_PRAYERS + 1];

        /* Call the main library function to fill the Prayer times */
        PrayerTime[] pt = prayer.getPrayerTimes(sPTLocation, sCalculationMethod, today);
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            ptCal[i] = (GregorianCalendar)nowCal.clone();
            ptCal[i].set(Calendar.HOUR_OF_DAY, pt[i].hour);
            ptCal[i].set(Calendar.MINUTE, pt[i].minute);
            ptCal[i].set(Calendar.SECOND, pt[i].second);
            Log.d(TAG, getPrayerName(context, i) + " " + PrayerTimes.format(ptCal[i]));
        }

        PrayerTime nextPT = prayer.getNextDayFajr(sPTLocation, sCalculationMethod, today);
        ptCal[i] = (GregorianCalendar)nowCal.clone();
        ptCal[i].add(Calendar.DATE, 1);
        ptCal[i].set(Calendar.HOUR_OF_DAY, nextPT.hour);
        ptCal[i].set(Calendar.MINUTE, nextPT.minute);
        ptCal[i].set(Calendar.SECOND, nextPT.second);
        Log.d(TAG, context.getString(R.string.nextfajr) + " " + PrayerTimes.format(ptCal[i]));

        return ptCal;
    }

    protected static String getPrayerName(Context context, int prayer)
    {
        int prayerNameResId = 0;
        switch (prayer) {
            case Prayer.NB_PRAYERS:
                // use "fajr" instead of "next fajr"
                // FALLTHROUGH
            case 0:
                prayerNameResId = R.string.fajr_en;
                break;
            case 1:
                prayerNameResId = R.string.shuruk;
                break;
            case 2:
                prayerNameResId = R.string.dhuhur;
                break;
            case 3:
                prayerNameResId = R.string.asr;
                break;
            case 4:
                prayerNameResId = R.string.maghrib;
                break;
            case 5:
                prayerNameResId = R.string.isha;
                break;
            default:
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Invalid prayer index: " + prayer);
                    return "";
                }
                break;
        }

        return context.getString(prayerNameResId) ;
    }

    private static PendingIntent createAlarmIntent(Context context)
    {
        int next = sPrayerTimes.getNextIndex();
        // prepare alarm message to be displayed in a notification when it's triggered.
        String alarmTxt = sPrayerTimes.getNextIndex() + getPrayerName(context, next);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(ALARM_TXT, alarmTxt);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static void cancelAlarm(Context context)
    {
        if (null != sAlarmIntent) {
            sAlarmIntent.cancel();
        }
        else {
            sAlarmIntent = createAlarmIntent(context);
        }
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(sAlarmIntent);
        sAlarmIntent = null;
        Log.i(TAG, "Old alarm cancelled.");
    }

    protected static void scheduleAlarm(Context context)
    {
        sAlarmIntent = createAlarmIntent(context);
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, sPrayerTimes.getNext().getTimeInMillis(), sAlarmIntent);
        Log.i(TAG, "New Alarm set for " + sPrayerTimes.format(sPrayerTimes.getNextIndex()));
    }
}
