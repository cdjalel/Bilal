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
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.linuxac.bilal.AthanAudioService;
import org.linuxac.bilal.AthanManager;
import org.linuxac.bilal.activities.MainActivity;
import org.linuxac.bilal.R;
import org.linuxac.bilal.activities.StopAthanActivity;

public class AthanAlarmReceiver extends BroadcastReceiver
{
    protected static final String TAG = "AthanAlarmReceiver";
    public final static String EXTRA_EVENT_ID = "org.linuxac.bilal.EVENT_ID";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Play athan audio
        Intent audioIntent = new Intent(context, AthanAudioService.class);
        context.startService(audioIntent);


        String athanMessage = context.getString(R.string.time_for) + " " +
                intent.getStringExtra(AthanManager.MESSAGE_NOTIFY_ATHAN);
        Log.d(TAG, "Athan alarm is ON: " + athanMessage);

        // TODO: make it configurable
        notifyAthan(context, athanMessage);

        // Broadcast to MainActivity so it updates its current prayer if on screen
        Intent updateIntent = new Intent(MainActivity.MESSAGE_UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm athan alarm.
        AthanManager.updatePrayerTimes(context);
    }

    private void notifyAthan(Context context, String athanMessage)
    {
        // Build intent to start MainActivity when notification is touched
        int notificationId = 0;
        int eventId = 0;

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 0);

        // Use another intent to stop athan from notification (button or swipe left)
        // TODO add stop from volume down
        PendingIntent stopAudioIntent = StopAthanActivity.getIntent(notificationId, context);

        String actionMsg = context.getString(R.string.stop_athan);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(TAG) // TODO check on UI
                        .setContentText(athanMessage)
                        .setContentIntent(activity)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setDeleteIntent(stopAudioIntent)
                        .addAction(R.drawable.ic_clear, actionMsg, stopAudioIntent);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build the notification and issue it
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
