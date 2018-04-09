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

import android.app.Notification;
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
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import timber.log.Timber;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.activities.MainActivity;
import com.djalel.android.bilal.activities.StopAthanActivity;
import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.helpers.WakeLocker;

import java.io.IOException;


public class AthanService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener
{
    public static final String EXTRA_MUEZZIN = "com.djalel.android.bilal.MUEZZIN";

    public static final String ACTION_NOTIFY_ATHAN = "com.djalel.android.bilal.action.NOTIFY_ATHAN";
    public static final String ACTION_PLAY_ATHAN = "com.djalel.android.bilal.action.PLAY_ATHAN";
    public static final String ACTION_STOP_ATHAN = "com.djalel.android.bilal.action.STOP_ATHAN";

    private int mPrayerIndex;
    private String mAthanFile;
    private MediaPlayer mAudioPlayer = null;
    private boolean mAudioIsOn;
    private boolean isForeground;
    private boolean isStopped;

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Timber.d("onStartCommand action = " + action);

        isStopped = false;
        if (action.equals(ACTION_NOTIFY_ATHAN)) { // sound + notif
            stopAudio();        // in case of || starts Alarm + Settings

            mPrayerIndex = PrayerTimesManager.getCurrentPrayerIndex();
            mAudioIsOn = UserSettings.isAthanEnabled(this);
            if (mAudioIsOn) {
                mAthanFile = "android.resource://" + getPackageName() + "/" +
                        UserSettings.getMuezzinRes(UserSettings.getMuezzin(this), mPrayerIndex);
                initMediaPlayer();
            }

            int notificationId = 005;
            startForeground(notificationId, buildNotification());
            isForeground = true;
        }
        else if (action.equals(ACTION_PLAY_ATHAN)) { // sound only
            stopAudio();        // in case of || starts Alarm + Settings

            mPrayerIndex = 2;
            mAudioIsOn = true;
            mAthanFile = "android.resource://" + getPackageName() + "/" +
                    UserSettings.getMuezzinRes(intent.getStringExtra(EXTRA_MUEZZIN), mPrayerIndex);
            initMediaPlayer();
            isForeground = false;
        }
        else if (action.equals(ACTION_STOP_ATHAN)) {
            isStopped = true;
            stopAthan();
            stopSelf(startId);
        }

        return Service.START_NOT_STICKY;    // Don't restart if killed
    }

    private void stopAthan() {
        Timber.d("stopAthan");
        if (mAudioIsOn) {
            stopAudio();
            mAudioIsOn = false;
        }
        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }
        WakeLocker.release();
    }

    public static void stopAthanAction(Context context) {
        Intent stopIntent = new Intent(context, AthanService.class);
        context.stopService(stopIntent); // should call onDestroy but with no guarantee
        //stopIntent.setAction(AthanService.ACTION_STOP_ATHAN);
        //context.startService(stopIntent);
    }

    private Notification buildNotification() {
        // Use one intent to show MainActivity when notification is touched
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

        Bitmap largeIconBmp = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.ic_notif_large);
        /*Keep this in case we need it
        Resources res = this.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIconBmp = Bitmap.createScaledBitmap(largeIconBmp, width, height, false);*/


        String contentTitle = String.format(this.getString(R.string.time_for),
                PrayerTimes.getName(this, mPrayerIndex));
        String contentTxt = String.format(this.getString(R.string.time_in),
                UserSettings.getCityName(this), PrayerTimesManager.formatPrayer(mPrayerIndex));
        String stopActionTxt = this.getString(R.string.stop_athan);

        // Notification channel ID is ignored for Android 7.1.1 (API level 25) and lower.
        String channelId = "bilal_channel_01";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.drawable.ic_notif)
                        .setContentTitle(contentTitle)
                        .setContentText(contentTxt)
                        .setContentIntent(notifContentIntent)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
//                        .setAutoCancel(true)
//                        .setLargeIcon(largeIconBmp)
                        //.setOngoing(true);
                        .setShowWhen(false);//.setUsesChronometer(true);
                        // TODO: add a timeout (till Iqama) with android O,  .setTimeoutAfter(20 * 60 * 1000)

        if (mAudioIsOn) {
            // Use another cancel/stop intent to stop athan from notification
            // (cancel by swipe and stop by button)
            Intent stopIntent = new Intent(this, StopAthanActivity.class);
            stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notifDeleteIntent = PendingIntent.getActivity(this, 0,
                    stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Timber.d("setDeleteIntent");

            notificationBuilder
                    .setDeleteIntent(notifDeleteIntent);
//                    .addAction(R.drawable.ic_stop_athan, stopActionTxt, notifDeleteIntent);
        }

        if (UserSettings.isVibrateEnabled(this)) {
            notificationBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000, 1000, 1000});
        }

        return notificationBuilder.build();
    }

    private void initMediaPlayer() {
        if (null == mAudioPlayer) {
            try {
                mAudioPlayer = new MediaPlayer();
                mAudioPlayer.setDataSource(this, Uri.parse(mAthanFile));
                mAudioPlayer.setOnPreparedListener(this);
                mAudioPlayer.setOnErrorListener(this);
                mAudioPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                mAudioPlayer.prepareAsync(); // prepare async to not block main thread
                mAudioPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mAudioPlayer.setVolume(1.0f, 1.0f);
                Timber.d("Audio player prepared asynchronously!");
            } catch (IOException e) {
                e.printStackTrace();
                Timber.e(e.getMessage(), e);
            }
        }
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        mAudioPlayer.start();
        Timber.i("Audio started playing!");
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

	private void stopAudio() {
		if (mAudioPlayer != null) {
            Timber.d("Stopping Athan");
            if (mAudioPlayer.isPlaying()) { mAudioPlayer.stop(); }
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }

	public void onPause() {
        if (mAudioPlayer.isPlaying()) { mAudioPlayer.stop(); }
	}

	public void onDestroy() {
        Timber.d("onDestroy");
        if (!isStopped) {               // in case android call this if it forces a kill
            isStopped = true;
            stopAthan();
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}
}
