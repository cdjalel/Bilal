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

package org.linuxac.bilal.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.linuxac.bilal.AlarmManager;
import org.linuxac.bilal.AthanService;
import org.linuxac.bilal.R;
import org.linuxac.bilal.activities.MainActivity;
import org.linuxac.bilal.activities.StopAthanActivity;
import org.linuxac.bilal.helpers.UserSettings;

public class AlarmReceiver extends BroadcastReceiver
{
    protected static final String TAG = "AlarmReceiver";
    public static final String PRAYER_INDEX = "org.linuxac.bilal";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String alarmTxt = intent.getStringExtra(AlarmManager.ALARM_TXT);
        Log.d(TAG, "Athan alarm is ON: " + alarmTxt);

        int index = Integer.parseInt(alarmTxt.substring(0,1));          // 1st char = prayer index

        if (UserSettings.isAthanEnabled(context)) {
            Intent audioIntent = new Intent(context, AthanService.class);
            audioIntent.putExtra(PRAYER_INDEX, index);
            context.startService(audioIntent);
        }

        if (UserSettings.isNotificationEnabled(context)) {
            String notificationTxt =  alarmTxt.substring(1);
            showNotification(context, notificationTxt);
        }

        if (UserSettings.isVibrateEnabled(context)) {
            // TODO: vibrate
        }

        // Broadcast to MainActivity so it updates its screen if on
        Intent updateIntent = new Intent(MainActivity.UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm alarm.
        AlarmManager.updatePrayerTimes(context, false);
    }

    private void showNotification(Context context, String contentTxt)
    {
        int notificationId = 0;

        // Use one intent to show MainActivity and another intent to stop athan (by notification
        // button or swipe left, TODO add stop from volume down too)
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 0);
        PendingIntent stopAudioIntent = StopAthanActivity.getIntent(notificationId, context);

        String actionTxt = context.getString(R.string.stop_athan);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(context.getString(R.string.time_for))
                        .setContentText(contentTxt)
                        .setContentIntent(activity)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setDeleteIntent(stopAudioIntent)
                        .addAction(R.drawable.ic_clear, actionTxt, stopAudioIntent); //  XML vector

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build the notification and issue it
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
