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

package org.linuxac.bilal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class BilalAlarm extends BroadcastReceiver
{
    public final static String EXTRA_EVENT_ID = "org.linuxac.bilal.EVENT_ID";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String message = intent.getStringExtra(BilalActivity.NOTIFY_MESSAGE);

        Log.d(BilalActivity.TAG, "Alarm is ON: " + message);

        // ask Activity to update prayer times
        Intent updateIntent = new Intent(BilalActivity.UPDATE_MESSAGE);
        context.sendBroadcast(updateIntent);

        // Build intent for notification content
        int notificationId = 0;
        int eventId = 0;
        Intent nextPrayerIntent = new Intent(context, BilalActivity.class);
        nextPrayerIntent.putExtra(EXTRA_EVENT_ID, eventId);
        PendingIntent nextPrayerPendingIntent =
                PendingIntent.getActivity(context, 0, nextPrayerIntent, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(BilalActivity.TAG)
                        .setContentText(message)
                        .setContentIntent(nextPrayerPendingIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
