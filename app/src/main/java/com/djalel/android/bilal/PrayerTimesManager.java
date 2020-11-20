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

package com.djalel.android.bilal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import timber.log.Timber;

import org.arabeyes.prayertime.Method;
import org.arabeyes.prayertime.PTLocation;
import org.arabeyes.prayertime.Prayer;
import org.arabeyes.prayertime.PrayerTime;

import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.datamodels.City;
import com.djalel.android.bilal.receivers.AlarmReceiver;
import com.djalel.android.bilal.receivers.BootAndTimeChangeReceiver;
import com.djalel.android.bilal.services.AthanService;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;

public class PrayerTimesManager {
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
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return null;
        }
        return sPrayerTimes.getCurrent();
    }

    public static int getCurrentPrayerIndex()
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return 2;
        }
        return sPrayerTimes.getCurrentIndex();
    }

    public static int getNextPrayerIndex()
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return 2;           // fallback to dhuhr
        }
        return sPrayerTimes.getNextIndex();
    }

    public static String  getNextName(Context context)
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return null;
        }
        return sPrayerTimes.getNextName(context);
    }

    public static String formatPrayerTime(int i)
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return "";
        }
        return sPrayerTimes.formatPrayerTime(i);
    }

    public static String formatTimeToNextPrayer(Context context, GregorianCalendar from)
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return "";
        }
        return context.getString(R.string.time_to_next) + " " +
                sPrayerTimes.formatTimeToNextPrayer(context, from);
    }

    public static String formatTimeFromCurrentPrayer(Context context, GregorianCalendar to)
    {
        if (null == sPrayerTimes) {
            Timber.e("sPrayerTimes == null");
            return "";
        }
        return String.format(context.getString(R.string.time_up),
                sPrayerTimes.getCurrentName(context)) +
                sPrayerTimes.formatTimeFromCurrentPrayer(context, to);
    }

    public static void enableAlarm(Context context)
    {
        Timber.d("Enabling Alarm.");
        enableBootAndTimeChangeReceiver(context);
        updatePrayerTimes(context, true);
    }

    public static void disableAlarm(Context context)
    {
        Timber.d("Disabling Alarm.");
        cancelAlarm(context);
        disableBootAndTimeChangeReceiver(context);
    }

    public static void handleSettingsChange(Context context, int calcMethod, int round, int mathhab)
    {
        if (!(-1 == calcMethod && -1 == round && -1 == mathhab)) {
            sMethod = new Method();
            sMethod.setMethod(calcMethod > -1 ? calcMethod : (calcMethod = UserSettings.getCalculationMethod(context)));
            sMethod.round = round > -1 ? round : UserSettings.getRounding(context);
            if (calcMethod == Method.V2_KARACHI) {
                sMethod.mathhab = mathhab > -1 ? mathhab : (UserSettings.isMathhabHanafi(context) ? 2 : 1);
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

        // stop running Athan if any
        AthanService.stopAthanAction(context);

        cancelAlarm(context);     // avoid unwanted alarm trigger from AlarmService
        updatePrayerTimes(context, false);
    }

    private static void logBootAndTimeChangeReceiverSetting(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        String state;
        switch (pm.getComponentEnabledSetting(receiver)){
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                state = "ENABLED";
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                state = "DISABLED";
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
                state = "DISABLED_UNTIL_USED";
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                state = "DISABLED_USER";
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
            default:
                state = "DEFAULT (Enabled in Manifest)";
                break;
        }
        Timber.d("BootAndTimeChangeReceiver Setting = " + state);
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
        Timber.i("--- updatePrayerTimes(..., " + enableAlarm + ")");
        logBootAndTimeChangeReceiverSetting(context);

        City city = UserSettings.getCity(context);
        if (null == city) {
            Timber.w("Location not set! Nothing todo until user chooses a city.");
            return;
        }

        GregorianCalendar nowCal = new GregorianCalendar();
        // next test includes location & time change see handlers above
        if (null != sPrayerTimes && !sNewCalc && sameDay(sLastTime, nowCal)) {
            sPrayerTimes.updateCurrent(nowCal);
            Timber.d("Call it a day :)");
        }
        else {
            calcPrayerTimes(context, nowCal, city);
        }

        Timber.d("Current time: " + sPrayerTimes.formatPrayerTime(nowCal));
        Timber.d("Current prayer: " + sPrayerTimes.getCurrentName(context));
        Timber.i("Next prayer: " + sPrayerTimes.getNextName(context));

        Timber.d("UserSettings.isNotificationEnabled = " + UserSettings.isNotificationEnabled(context));

        // SettingsActivity listener calls before setting is committed to shared prefs, so it uses
        // enableAlarm boolean.
        if (enableAlarm || UserSettings.isNotificationEnabled(context)) {
            scheduleAlarm(context);
        }
    }

    private static void calcPrayerTimes(Context context, GregorianCalendar nowCal, City city)
    {
        int i;
        Prayer prayer = new Prayer();
        Date today = nowCal.getTime();
        GregorianCalendar[] ptCal = new GregorianCalendar[Prayer.NB_PRAYERS + 1];

        TimeZone tz = TimeZone.getTimeZone(city.getTimezoneEN());

        double gmtDiffHrs = tz.getOffset(nowCal.getTimeInMillis()) / (1000 * 3600);   // TODO nowCal TZ?
        int dst = tz.inDaylightTime(nowCal.getTime()) ? 1 : 0;
        Timber.w("TZ: gmtDiff = " + gmtDiffHrs + ", DST = " + dst);

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

        Timber.d("Last time: " + PrayerTimes.formatPrayerTime(sLastTime, sMethod.round));
        sLastTime = nowCal;

        /* Call the main library function to fill the Prayer times */
        PrayerTime[] pt = prayer.getPrayerTimes(location, sMethod, today);
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            ptCal[i] = (GregorianCalendar)nowCal.clone();
            ptCal[i].set(Calendar.HOUR_OF_DAY, pt[i].hour);
            ptCal[i].set(Calendar.MINUTE, pt[i].minute);
            ptCal[i].set(Calendar.SECOND, pt[i].second);
            Timber.d(PrayerTimes.getName(context, i, nowCal) + " " +
                    PrayerTimes.formatPrayerTime(ptCal[i], sMethod.round));
        }

        PrayerTime nextPT = prayer.getNextDayFajr(location, sMethod, today);
        ptCal[i] = (GregorianCalendar)nowCal.clone();
        ptCal[i].add(Calendar.DATE, 1);
        ptCal[i].set(Calendar.HOUR_OF_DAY, nextPT.hour);
        ptCal[i].set(Calendar.MINUTE, nextPT.minute);
        ptCal[i].set(Calendar.SECOND, nextPT.second);
        Timber.d(context.getString(R.string.nextfajr) + " " + PrayerTimes.formatPrayerTime(ptCal[i], sMethod.round));

        sPrayerTimes = new PrayerTimes(nowCal, ptCal, sMethod.round == 1);
        sMethod = null;
        sNewCalc = false;
    }

    private static PendingIntent createAlarmIntent(Context context)
    {
        int index = null == sPrayerTimes? 2 : sPrayerTimes.getNextIndex();
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AthanService.EXTRA_PRAYER, index);
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
        if (null != alarmMgr) {
            alarmMgr.cancel(sAlarmIntent);
        }
        sAlarmIntent = null;
        Timber.i("Old alarm cancelled.");
    }

    private static void scheduleAlarm(Context context)
    {
        sAlarmIntent = createAlarmIntent(context);
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        if (null != alarmMgr) {
            if (Build.VERSION.SDK_INT >= 23) {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sPrayerTimes.getNext().getTimeInMillis(), sAlarmIntent);
            }
            else if (Build.VERSION.SDK_INT >= 19) {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, sPrayerTimes.getNext().getTimeInMillis(), sAlarmIntent);
            }
            else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, sPrayerTimes.getNext().getTimeInMillis(), sAlarmIntent);
            }
            Timber.i("New Alarm set for " + sPrayerTimes.formatPrayerTime(sPrayerTimes.getNextIndex()));
        }
    }
}
