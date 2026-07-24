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

package com.djalel.android.bilal.receivers;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.services.AthanService;
import com.djalel.android.bilal.activities.MainActivity;
import com.djalel.android.bilal.helpers.WakeLocker;
import com.djalel.android.bilal.helpers.UserSettings;

import timber.log.Timber;

// Can't use WakefulBroadcastReceiver as it relies on PARTIAL_WAKE_LOCK
// which doesn't stop Athan on power button press
// It's deprecated anyway
public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        int prayer = intent.getIntExtra(AthanService.EXTRA_PRAYER, 2);
        Timber.i("=============== Athan alarm is ON: %d", prayer);

        if (UserSettings.isNotificationEnabled(context)) {
            WakeLocker.acquire(context);
            Intent athanIntent = new Intent(context, AthanService.class);
            athanIntent.setAction(AthanService.ACTION_NOTIFY_ATHAN);
            athanIntent.putExtra(AthanService.EXTRA_PRAYER, prayer);
            athanIntent.putExtra(AthanService.EXTRA_MUEZZIN, UserSettings.getMuezzin(context));

            try {
                if (Build.VERSION.SDK_INT >= 31) {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                        context.startForegroundService(athanIntent);
                    } else {
                        // Permission missing, cannot start foreground service from background.
                        // Fallback to regular startService which may also fail but we'll catch it.
                        context.startService(athanIntent);
                    }
                } else if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(athanIntent);
                } else {
                    context.startService(athanIntent);
                }
            } catch (Exception e) {
                Timber.e(e, "Failed to start AthanService from background");
                WakeLocker.release();
            }
        }
        else {
            Timber.e("Alarm received when set off by user!");
        }

        // Broadcast to MainActivity so it updates its screen if on
        Intent updateIntent = new Intent(MainActivity.UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm alarm.
        PrayerTimesManager.updatePrayerTimes(context, false);
    }
}
