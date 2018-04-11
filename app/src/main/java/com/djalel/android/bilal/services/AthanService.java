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

package com.djalel.android.bilal.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import timber.log.Timber;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.activities.MainActivity;
import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.helpers.WakeLocker;

import java.io.IOException;
import java.util.GregorianCalendar;


public class AthanService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener
{
    public static final String EXTRA_PRAYER = "com.djalel.android.bilal.PRAYER";
    public static final String EXTRA_MUEZZIN = "com.djalel.android.bilal.MUEZZIN";
    public static final int ATHAN_DURATION= 6 * 60 * 1000;          // longest audio is 5' 10''

    public static final String ACTION_NOTIFY_ATHAN = "com.djalel.android.bilal.action.NOTIFY_ATHAN";
    public static final String ACTION_PLAY_ATHAN = "com.djalel.android.bilal.action.PLAY_ATHAN";
    public static final String ACTION_STOP_ATHAN = "com.djalel.android.bilal.action.STOP_ATHAN";

    private int mPrayerIndex;
    private String mAthanFile;
    private MediaPlayer mAudioPlayer = null;
    private boolean mAudioIsOn;
    private boolean isForeground;
    private boolean isStopped = true;
    private BroadcastReceiver mScreenOffReceiver = null;
    private NotificationCompat.Builder mNotificationBuilder;
    private final int mNotificationId = 005;

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Timber.d("onStartCommand action = " + action);

        if (action.equals(ACTION_NOTIFY_ATHAN)) { // sound + notif
            stopAudio(false);        // in case of || starts Alarm + Settings

            mPrayerIndex = intent.getIntExtra(EXTRA_PRAYER, 2);
            mAudioIsOn = UserSettings.isAthanEnabled(this);
            if (mAudioIsOn) {
                mAthanFile = "android.resource://" + getPackageName() + "/" +
                        UserSettings.getMuezzinRes(intent.getStringExtra(EXTRA_MUEZZIN), mPrayerIndex);
                initMediaPlayer();
                registerScreenOffReceiver();
            }

            startForeground(mNotificationId, buildNotification());
            isForeground = true;
            isStopped = false;
        }
        else if (action.equals(ACTION_PLAY_ATHAN)) { // sound only
            stopAudio(false);        // in case of || starts Alarm + Settings

            mPrayerIndex = intent.getIntExtra(EXTRA_PRAYER, 2);
            mAudioIsOn = true;
            mAthanFile = "android.resource://" + getPackageName() + "/" +
                    UserSettings.getMuezzinRes(intent.getStringExtra(EXTRA_MUEZZIN), mPrayerIndex);
            initMediaPlayer();
            isForeground = false;
            isStopped = false;
        }
        else if (action.equals(ACTION_STOP_ATHAN)) {
            if (!isStopped) {
                isStopped = true;
                stopAthan();
                stopSelf(startId);
            }
        }

        return Service.START_NOT_STICKY;    // Don't restart if killed
    }

    private Notification buildNotification() {
        // Use one intent to show MainActivity when notification is touched
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

        // Use another intent to stop Athan/close notification via button
        // (swipe left/right doesn't work on service notification)
        Intent stopIntent = new Intent(this, StopAthanActivity.class); // TODO inner class
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifDeleteIntent = PendingIntent.getActivity(this, 0,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIconBmp = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.ic_notif_large);
        /*Keep this in case we need it
        Resources res = this.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIconBmp = Bitmap.createScaledBitmap(largeIconBmp, width, height, false);*/

        String contentTitle = String.format(this.getString(R.string.time_for),
                PrayerTimes.getName(this, mPrayerIndex, new GregorianCalendar()));
        String contentTxt = String.format(this.getString(R.string.time_in),
                UserSettings.getCityName(this), PrayerTimesManager.formatPrayer(mPrayerIndex));

        // Notification channel ID is ignored for Android 7.1.1 (API level 25) and lower.
        String channelId = "bilal_channel_01";
        mNotificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_stat_notif)
                .setLargeIcon(largeIconBmp)
                .setTicker(contentTitle)
                .setContentTitle(contentTitle)
                .setContentText(contentTxt)
                .setContentIntent(notifContentIntent)
                .setDeleteIntent(notifDeleteIntent) // swipe doesn't work for serv. notifs!
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setLargeIcon(largeIconBmp)
                .setShowWhen(false) //.setUsesChronometer(true)
                //.setSound starts Athan which is stopped by system prematurely!
                ;
        if (mAudioIsOn) {
            mNotificationBuilder.addAction(R.drawable.ic_stop_athan,
                    this.getString(R.string.stop_athan), notifDeleteIntent);
        }
        else {
            mNotificationBuilder.addAction(R.drawable.ic_close_notification,
                    this.getString(R.string.close_notification), notifDeleteIntent);
            WakeLocker.release();
        }

        if (UserSettings.isVibrateEnabled(this)) {
            mNotificationBuilder.setVibrate(new long[]{1000, 1000, 1000});
        }

        return mNotificationBuilder.build();
    }

    private void initMediaPlayer() {
        if (null == mAudioPlayer) {
            try {
                mAudioPlayer = new MediaPlayer();
                mAudioPlayer.setDataSource(this, Uri.parse(mAthanFile));
                mAudioPlayer.setOnPreparedListener(this);
                mAudioPlayer.setOnErrorListener(this);
//                mAudioPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                mAudioPlayer.prepareAsync(); // prepare async to not block main thread
                mAudioPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mAudioPlayer.setVolume(1.0f, 1.0f);
                Timber.d("Audio player prepared asynchronously");
            } catch (IOException e) {
                e.printStackTrace();
                Timber.e(e.getMessage(), e);
            }
        }
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        mAudioPlayer.start();
        mAudioPlayer.setOnCompletionListener(this);
        Timber.i("Audio started playing. Duration = " + mAudioPlayer.getDuration());
        if(!mAudioPlayer.isPlaying()) {
	        Timber.w("Problem in playing audio");
	    }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        Timber.e("what=" + what + " extra=" + extra);
        return false; // TODO change to true if error is handled here.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Timber.d("onCompletion");
        if (mNotificationBuilder != null && isForeground) {
            stopAudio(true);
            mAudioIsOn = false;

            mNotificationBuilder.mActions.clear();
            if (UserSettings.isVibrateEnabled(this)) {
                mNotificationBuilder.setVibrate(null);
            }

            Intent stopIntent = new Intent(this, StopAthanActivity.class);
            stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notifDeleteIntent = PendingIntent.getActivity(this, 0,
                    stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.addAction(R.drawable.ic_close_notification,
                    this.getString(R.string.close_notification), notifDeleteIntent);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(mNotificationId, mNotificationBuilder.build());
        }
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mAudioPlayer == null) initMediaPlayer();
                else if (!mAudioPlayer.isPlaying()) mAudioPlayer.start();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mAudioPlayer.isPlaying()) mAudioPlayer.stop();
                mAudioPlayer.release();
                mAudioPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mAudioPlayer.isPlaying()) mAudioPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mAudioPlayer.isPlaying()) mAudioPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public void onPause() {
        if (mAudioPlayer.isPlaying()) { mAudioPlayer.stop(); }
    }

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}


    private void stopAudio(boolean unlock) {
        if (mAudioPlayer != null) {
            Timber.d("Stopping Audio");
            if (mAudioPlayer.isPlaying()) { mAudioPlayer.stop(); }
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        if (mScreenOffReceiver != null) {
            Timber.d("Unregister Screen OFF Receiver");
            unregisterReceiver(mScreenOffReceiver);
            mScreenOffReceiver = null;
        }
        if (unlock) {
            WakeLocker.release();
        }
    }

    private void stopAthan() {
        Timber.d("stopAthan");
        if (mAudioIsOn) {
            stopAudio(true);
            mAudioIsOn = false;
        }
        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }
    }

    public static void stopAthanAction(Context context) {
        Intent stopIntent = new Intent(context, AthanService.class);
        stopIntent.setAction(AthanService.ACTION_STOP_ATHAN);
        context.startService(stopIntent);
    }

    public void onDestroy() {
        Timber.d("onDestroy");
        if (!isStopped) {               // in case android call this when it forces a kill
            isStopped = true;
            stopAthan();
        }
    }

    private void registerScreenOffReceiver() {
        if (mScreenOffReceiver == null) {
            mScreenOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Timber.i("Screen OFF, stopping Athan");
                    stopAthanAction(context);
                }
            };
            Timber.d("Register Screen OFF Receiver");
            registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }
        else {
            Timber.e("Screen OFF Receiver already registered!");
        }
    }

    public static class StopAthanActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Timber.d("onCreate");
            super.onCreate(savedInstanceState);

            stopAthanAction(this);
            finish(); // since finish() is called in onCreate(), onDestroy() will be called immediately
        }
    }
}
