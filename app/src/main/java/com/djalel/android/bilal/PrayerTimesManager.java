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
import org.linuxac.bilal.datamodels.City;
import org.linuxac.bilal.receivers.AlarmReceiver;
import org.linuxac.bilal.receivers.BootAndTimeChangeReceiver;
import org.linuxac.bilal.services.AthanService;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;

public class PrayerTimesManager {
    private static final String TAG = "PrayerTimesManager";


    private static final GregorianCalendar sLongLongTimeAgo = new GregorianCalendar(0,0,0);
    private static GregorianCalendar sLastTime = sLongLongTimeAgo;
    private static PrayerTimes sPrayerTimes = null;
    private static Method sMethod = null;
    private static PendingIntent sAlarmIntent = null;
    private static boolean sNewCalc = false;

    private PrayerTimesManager() {}

    public static boolean prayerTimesNotAvailable()
    {
        return null == sPrayerTimes;
    }

    public static GregorianCalendar getCurrentPrayer()
    {
        if (BuildConfig.DEBUG && null == sPrayerTimes) {
            //Log.w(TAG, "sPrayerTimes == null");
            return null;
        }
        return sPrayerTimes.getCurrent();
    }

    public static int getCurrentPrayerIndex()
    {
        if (BuildConfig.DEBUG && null == sPrayerTimes) {
            //Log.w(TAG, "sPrayerTimes == null");
            return -1;
        }
        return sPrayerTimes.getCurrentIndex();
    }

    public static GregorianCalendar getNextPrayer()
    {
        if (BuildConfig.DEBUG && null == sPrayerTimes) {
            //Log.w(TAG, "sPrayerTimes == null");
            return null;
        }
        return sPrayerTimes.getNext();
    }

    public static int getNextPrayerIndex()
    {
        if (BuildConfig.DEBUG && null == sPrayerTimes) {
            //Log.w(TAG, "sPrayerTimes == null");
            return 2;           // fallback to dhuhr
        }
        return sPrayerTimes.getNextIndex();
    }

    public static String formatPrayer(int i)
    {
        if (BuildConfig.DEBUG && null == sPrayerTimes) {
            //Log.w(TAG, "sPrayerTimes == null");
            return "";
        }
        return sPrayerTimes.format(i);
    }

    public static void enableAlarm(Context context)
    {
        //Log.d(TAG, "Enabling Alarm.");
        enableBootAndTimeChangeReceiver(context);
        updatePrayerTimes(context, true);
    }

    public static void disableAlarm(Context context)
    {
        //Log.d(TAG, "Disabling Alarm.");
        cancelAlarm(context);
        disableBootAndTimeChangeReceiver(context);
    }

    public static void handleLocationChange(Context context, int m, int r, int h)
    {
        if (!(-1 == m && -1 == r && -1 == h)) {
            sMethod = new Method();
            sMethod.setMethod(m > -1 ? m : (m = UserSettings.getCalculationMethod(context)));
            sMethod.round = r > -1 ? r : UserSettings.getRounding(context);
            if (m == Method.V2_KARACHI) {
                sMethod.mathhab = h > -1 ? h : (UserSettings.isMathhabHanafi(context) ? 2 : 1);
            }
        }
        sNewCalc = true;
        updatePrayerTimes(context, false);
    }

    public static void handleBootComplete(Context context)
    {
        updatePrayerTimes(context, false);
    }

    public static void handleTimeChange(Context context)
    {
        sNewCalc = true;
        cancelAlarm(context);     // avoid unwanted alarm trigger from AlarmService
        updatePrayerTimes(context, false);
    }

    private static void logBootAndTimeChangeReceiverSetting(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        //Log.d(TAG, "BootAndTimeChangeReceiver Setting = " +
        //        pm.getComponentEnabledSetting(receiver));
    }

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
        //Log.i(TAG, "--- updatePrayerTimes(..., " + enableAlarm + ")");

        if (BuildConfig.DEBUG) {
            logBootAndTimeChangeReceiverSetting(context);
        }

        City city = UserSettings.getCity(context);
        if (null == city) {
            //Log.w(TAG, "Location not set! Nothing todo until user chooses a city.");
            return;
        }

        GregorianCalendar nowCal = new GregorianCalendar();
        // next test includes location & time change see handlers above
        if (null != sPrayerTimes && !sNewCalc && sameDay(sLastTime, nowCal)) {
            sPrayerTimes.updateCurrent(nowCal);
            //Log.d(TAG, "Call it a day :)");
        }
        else {
            calcPrayerTimes(context, nowCal, city);
        }

        //Log.d(TAG, "Current time: " + sPrayerTimes.format(nowCal));
        //Log.d(TAG, "Current prayer: " + sPrayerTimes.getCurrentName(context));
        //Log.i(TAG, "Next prayer: " + sPrayerTimes.getNextName(context));

        // In SettingsActivity, listener calls us before setting is committed to shared prefs.
        if (enableAlarm || UserSettings.isAlarmEnabled(context)) {
            scheduleAlarm(context);
        }
    }

    @NonNull
    private static void calcPrayerTimes(Context context, GregorianCalendar nowCal, City city)
    {
        int i;
        Prayer prayer = new Prayer();
        Date today = nowCal.getTime();
        GregorianCalendar[] ptCal = new GregorianCalendar[Prayer.NB_PRAYERS + 1];

        TimeZone tz = TimeZone.getTimeZone(city.getTimezoneEN());

        double gmtDiffHrs = tz.getOffset(nowCal.getTimeInMillis()) / (1000 * 3600);   // TODO nowCal TZ?
        int dst = tz.inDaylightTime(nowCal.getTime()) ? 1 : 0;
        //Log.w(TAG, "TZ: gmtDiff = " + gmtDiffHrs + ", DST = " + dst);

        PTLocation location = new PTLocation(city.getLatitude(), city.getLongitude(),
            gmtDiffHrs, dst, city.getAltitude(), 1010 /* pressure */, 10 /* temperature */);

        if (null == sMethod) {
            int m = UserSettings.getCalculationMethod(context);
            sMethod = new Method();
            sMethod.setMethod(m);
            sMethod.round = UserSettings.getRounding(context);
            if (m == Method.V2_KARACHI && UserSettings.isMathhabHanafi(context)) {
                sMethod.mathhab = 2;
            }
        }

        //Log.d(TAG, "Last time: " + PrayerTimes.format(sLastTime, sMethod.round));
        sLastTime = nowCal;

        /* Call the main library function to fill the Prayer times */
        PrayerTime[] pt = prayer.getPrayerTimes(location, sMethod, today);
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            ptCal[i] = (GregorianCalendar)nowCal.clone();
            ptCal[i].set(Calendar.HOUR_OF_DAY, pt[i].hour);
            ptCal[i].set(Calendar.MINUTE, pt[i].minute);
            ptCal[i].set(Calendar.SECOND, pt[i].second);
            //Log.d(TAG, PrayerTimes.getName(context, i) + " " + PrayerTimes.format(ptCal[i], sMethod.round));
        }

        PrayerTime nextPT = prayer.getNextDayFajr(location, sMethod, today);
        ptCal[i] = (GregorianCalendar)nowCal.clone();
        ptCal[i].add(Calendar.DATE, 1);
        ptCal[i].set(Calendar.HOUR_OF_DAY, nextPT.hour);
        ptCal[i].set(Calendar.MINUTE, nextPT.minute);
        ptCal[i].set(Calendar.SECOND, nextPT.second);
        //Log.d(TAG, context.getString(R.string.nextfajr) + " " + PrayerTimes.format(ptCal[i], sMethod.round));

        sPrayerTimes = new PrayerTimes(nowCal, ptCal, sMethod.round == 1);
        sMethod = null;
        sNewCalc = false;
    }

    private static PendingIntent createAlarmIntent(Context context)
    {
        int idx = null == sPrayerTimes? 2 : sPrayerTimes.getNextIndex();
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AthanService.EXTRA_PRAYER, idx);
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
        //Log.i(TAG, "Old alarm cancelled.");
    }

    private static void scheduleAlarm(Context context)
    {
        sAlarmIntent = createAlarmIntent(context);
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, sPrayerTimes.getNext().getTimeInMillis(), sAlarmIntent);
        //Log.i(TAG, "New Alarm set for " + sPrayerTimes.format(sPrayerTimes.getNextIndex()));
    }
}
