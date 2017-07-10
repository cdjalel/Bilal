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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.content.Context.ALARM_SERVICE;

public class AthanManager {

    protected static final String TAG = "AthanManager";

    protected static boolean sLocationIsSet = false;    // must be set by user on 1st UI run
    public static String sCityName = "Souk Ahras";
    public static PTLocation sPTLocation = new PTLocation(36.28639, 7.95111, 1, 0, 697, 1010, 10);
    // http://dateandtime.info/citycoordinates.php?id=2479215

    public static Method sCalculationMethod;

    protected static final GregorianCalendar sLongLongTimeAgo = new GregorianCalendar(0,0,0); 
    protected static GregorianCalendar sLastTime = sLongLongTimeAgo;
    static {
        sCalculationMethod = new Method();
        sCalculationMethod.setMethod(Method.MUSLIM_LEAGUE);
        sCalculationMethod.round = 0;
    }

    public static final String MESSAGE_NOTIFY_ATHAN = "org.linuxac.bilal.NOTIFY";
    public static boolean sAlarmIsEnabled = false;   // 1st enabled by user after location is set
    protected static PendingIntent sAlarmIntent = null;


    public static void enableAthan(Context context)
    {
        enableBootAndTimeChangeReceiver(context);
        scheduleAthanAlarm(context);
    }

    public static void disableAthan(Context context)
    {
        cancelAthanAlarm(context);
        disableBootAndTimeChangeReceiver(context);
    }

    private static void logBootAndTimeChangeReceiver(Context context)
    {
        ComponentName receiver = new ComponentName(context, BootAndTimeChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        Log.d(TAG, "BootAndTimeChangeReceiver EnabledSetting = " +
                pm.getComponentEnabledSetting(receiver));
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

    public static PrayerTimes sPrayerTimes = null;

    private static boolean sameDay(GregorianCalendar a, GregorianCalendar b)
    {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                a.get(Calendar.DATE) == b.get(Calendar.DATE);
    }

    public static void updatePrayerTimes(Context context)
    {
        AthanManager.logBootAndTimeChangeReceiver(context);

        // TODO: use case: 1st run opens settings to let user pick a location (from DB or
        // TODO automatically). Then Athan is enabled by default until user turns it off.
        // TODO Prayer time calc. method is also set automatically (from DB based on location
        // TODO country), unless manually overidden by user, or when DB data is missing, UI must
        // TODO ask user explicitly
        if (!sLocationIsSet) {
            Log.d(TAG, "Location not set!");
//            return;
        }

        // TODO: use location & method from user preferences

        GregorianCalendar nowCal = new GregorianCalendar();
        boolean keepAlarm = false;
        if (sameDay(sLastTime, nowCal)) {
            int oldCurrent = sPrayerTimes.getCurrentIndex();
            sPrayerTimes.updateCurrent(nowCal);
            if (sAlarmIsEnabled && oldCurrent == sPrayerTimes.getCurrentIndex()) {
                keepAlarm = true;
                Log.d(TAG, "Keep old alarm.");
            }
            Log.d(TAG, "Call it a day :)");
        }
        else {
            Log.d(TAG, "Last time: " + DateFormat.getDateTimeInstance().format(sLastTime.getTime()));
            Log.d(TAG, "Now: " + DateFormat.getDateTimeInstance().format(nowCal.getTime()));
            GregorianCalendar[] ptCal = getPrayerTimes(context, nowCal);
            sPrayerTimes = new PrayerTimes(nowCal, ptCal);
            sLastTime = nowCal;
        }

        Log.d(TAG, "Current time: " + sPrayerTimes.format(nowCal));
        Log.d(TAG, "Current prayer: " + getPrayerName(context, sPrayerTimes.getCurrentIndex()));
        Log.d(TAG, "Next prayer: " + getPrayerName(context, sPrayerTimes.getNextIndex()));

        if (sAlarmIsEnabled && !keepAlarm) {
            scheduleAthanAlarm(context);
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
                assert(false);
                break;
        }

        return context.getString(prayerNameResId) ;
    }

    private static void cancelAthanAlarm(Context context)
    {
        if (null != sAlarmIntent) {
            sAlarmIntent.cancel();
            AlarmManager alarmMgr = (AlarmManager)context.getSystemService(ALARM_SERVICE);
            alarmMgr.cancel(sAlarmIntent);
            Log.d(TAG, "Old Athan alarm cancelled.");
        }
        else {
            Log.d(TAG, "No Athan alarm to be cancelled.");
        }
        sAlarmIntent = null;
    }

    protected static void scheduleAthanAlarm(Context context)
    {
        // prepare alarm message to be displayed in a notification when it's triggered.
        String notificationMsg = getPrayerName(context, sPrayerTimes.getNextIndex()) + " " +
                sPrayerTimes.format(sPrayerTimes.getNext());

        // Cancel old alarm and schedule a new one
        cancelAthanAlarm(context);

        Intent intent = new Intent(context, AthanAlarmReceiver.class);
        intent.putExtra(MESSAGE_NOTIFY_ATHAN, notificationMsg);
        sAlarmIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        GregorianCalendar next = sPrayerTimes.getNext();
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), sAlarmIntent);
        Log.d(TAG, "New Athan Alarm set for " + DateFormat.getDateTimeInstance().format(next.getTime()));
    }
}
