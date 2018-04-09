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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
//import android.support.v4.app.NotificationCompat.WearableExtender;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.helpers.WakeLocker;
import com.djalel.android.bilal.services.AthanAudioService;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.activities.MainActivity;
import com.djalel.android.bilal.activities.StopAthanActivity;
import com.djalel.android.bilal.helpers.UserSettings;

import java.util.GregorianCalendar;

import timber.log.Timber;

import static android.content.Context.VIBRATOR_SERVICE;

public class AlarmReceiver extends BroadcastReceiver
{
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        int prayer = intent.getIntExtra(AthanAudioService.EXTRA_PRAYER, 2);
        Timber.i("=============== Athan alarm is ON: " + prayer);

        if (UserSettings.isVibrateEnabled(context)) {
            // this is independent of notification setVibrate
            Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(new long[]{1000, 1000, 1000}, -1);
            }
        }

        if (UserSettings.isAthanEnabled(context)) {
            WakeLocker.acquire(context);
            Intent audioIntent = new Intent(context, AthanAudioService.class);
            audioIntent.putExtra(AthanAudioService.EXTRA_PRAYER, prayer);
            audioIntent.putExtra(AthanAudioService.EXTRA_MUEZZIN, UserSettings.getMuezzin(context));
            context.startService(audioIntent);
        }

        if (UserSettings.isNotificationEnabled(context)) {
            showNotification(context, prayer);
        }

        // Broadcast to MainActivity so it updates its screen if on
        Intent updateIntent = new Intent(MainActivity.UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm alarm.
        PrayerTimesManager.updatePrayerTimes(context, false);
    }

    private void showNotification(Context context, int index)
    {
        // Use one intent to show MainActivity when notification is touched
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent notifContentIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);

        // Use another cancel/stop intent to stop athan from notification
        // (cancel by swipe and stop by button)
        Intent stopIntent = new Intent(context, StopAthanActivity.class);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifDeleteIntent = PendingIntent.getActivity(context, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        Bitmap largeIconBmp = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_notif_large);
        /*Keep this in case we need it
        Resources res = context.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIconBmp = Bitmap.createScaledBitmap(largeIconBmp, width, height, false);*/


        String contentTitle = String.format(context.getString(R.string.time_for),
                PrayerTimes.getName(context, index, new GregorianCalendar()));
        String contentTxt = String.format(context.getString(R.string.time_in),
                UserSettings.getCityName(context), PrayerTimesManager.formatPrayer(index));
        String actionTxt = context.getString(R.string.stop_athan);

        // Notification channel ID is ignored for Android 7.1.1 (API level 25) and lower.
        String channelId = "bilal_channel_01";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.drawable.ic_stat_notif)
                        .setTicker(contentTitle)
                        .setContentTitle(contentTitle)
                        .setContentText(contentTxt)
                        .setContentIntent(notifContentIntent)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setDeleteIntent(notifDeleteIntent)
                        .setLargeIcon(largeIconBmp)
                        .setShowWhen(false)//.setUsesChronometer(true)
                        .setTimeoutAfter(AthanAudioService.ATHAN_DURATION)
                        .addAction(R.drawable.ic_stop_athan, actionTxt, notifDeleteIntent);
        // don't use notification setSound as system stops athan prematurely!


        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build the notification and issue it
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
