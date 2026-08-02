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
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
//import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

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

    public static final String CHANNEL_ID = "bilal_channel_01";

    private int mPrayerIndex;
    private String mAthanFile;
    private MediaPlayer mAudioPlayer = null;
    private boolean mAudioIsOn;
    private boolean isForeground;
    private boolean isStopped = true;
    private BroadcastReceiver mScreenOffReceiver = null;
    private NotificationCompat.Builder mNotificationBuilder;
    private final int mNotificationId = 5;

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Timber.w("onStartCommand with null intent");
            return Service.START_NOT_STICKY;
        }
        String action = intent.getAction();
        Timber.d("onStartCommand action = %s", action);

        if (action != null) {
            switch (action) {
                case ACTION_NOTIFY_ATHAN:  // sound + notif
                    stopAudio(false);        // in case of || starts Alarm + Settings


                    mPrayerIndex = intent.getIntExtra(EXTRA_PRAYER, 2);
                    mAudioIsOn = UserSettings.isAthanEnabled(this);
                    if (mAudioIsOn) {
                        mAthanFile = "android.resource://" + getPackageName() + "/" +
                                UserSettings.getMuezzinRes(intent.getStringExtra(EXTRA_MUEZZIN), mPrayerIndex);
                        initMediaPlayer();
                        registerScreenOffReceiver();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(mNotificationId, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                    }
                    else {
                        startForeground(mNotificationId, buildNotification());
                    }
                    isForeground = true;
                    isStopped = false;
                    break;
                case ACTION_PLAY_ATHAN:  // sound only from settings activity when user selects muezzin
                    stopAudio(false);        // in case of || starts Alarm + Settings

                    mPrayerIndex = intent.getIntExtra(EXTRA_PRAYER, 2);
                    mAudioIsOn = true;
                    mAthanFile = "android.resource://" + getPackageName() + "/" +
                            UserSettings.getMuezzinRes(intent.getStringExtra(EXTRA_MUEZZIN), mPrayerIndex);
                    initMediaPlayer();
                    isForeground = false;
                    isStopped = false;
                    break;
                case ACTION_STOP_ATHAN:
                    if (!isStopped) {
                        isStopped = true;
                        stopAthan();
                        stopSelf(startId);
                    }
                    break;
            }
        }
        return Service.START_NOT_STICKY;    // Don't restart if killed
    }

    private Notification buildNotification() {
        // Use one intent to show MainActivity when notification is touched
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        // Use another intent to stop Athan/close notification via button
        // (swipe left/right doesn't work on service notification)
        Intent stopIntent = new Intent(this, StopAthanActivity.class); // TODO inner class
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifDeleteIntent = PendingIntent.getActivity(this, 0,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
                UserSettings.getCityName(this), PrayerTimesManager.formatPrayerTime(mPrayerIndex));

        // Notification channel ID is ignored for Android 7.1.1 (API level 25) and lower.
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
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
                // WakeLock is handled globally by WakeLocker class, 
                // so mAudioPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK) is not needed here.

                AudioAttributes audioAttributes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build();
                    mAudioPlayer.setAudioAttributes(audioAttributes);
                } else {
                    mAudioPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                }
                mAudioPlayer.setVolume(1.0f, 1.0f);

                requestAudioFocus(audioAttributes);

                mAudioPlayer.prepareAsync(); // prepare async to not block main thread
                Timber.d("Audio player prepared asynchronously");
            } catch (Exception e) {
                Timber.e(e, "Failed to initialize media player");
            }
        }
    }

    private void requestAudioFocus(AudioAttributes playbackAttributes) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use the attributes created in initMediaPlayer if available
            if (playbackAttributes == null) {
                playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
            }

            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();

            result = am.requestAudioFocus(focusRequest);
        } else {
            result = am.requestAudioFocus(this, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.d("Audio focus granted");
        } else {
            Timber.w("Audio focus not granted: %d", result);
        }
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        if (mAudioPlayer == null) {
            Timber.w("onPrepared called but mAudioPlayer is null");
            return;
        }
        mAudioPlayer.start();
        mAudioPlayer.setOnCompletionListener(this);
        Timber.i("Audio started playing. Duration = %s", mAudioPlayer.getDuration());
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

            mNotificationBuilder.clearActions();
            if (UserSettings.isVibrateEnabled(this)) {
                mNotificationBuilder.setVibrate(null);
            }

            Intent stopIntent = new Intent(this, StopAthanActivity.class);
            stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notifDeleteIntent = PendingIntent.getActivity(this, 0,
                    stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
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
                if (mAudioPlayer != null) {
                    if (mAudioPlayer.isPlaying()) mAudioPlayer.stop();
                    mAudioPlayer.release();
                    mAudioPlayer = null;
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mAudioPlayer != null && mAudioPlayer.isPlaying()) mAudioPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mAudioPlayer != null && mAudioPlayer.isPlaying()) mAudioPlayer.setVolume(0.1f, 0.1f);
                break;
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
            }
            else {
                stopForeground(true);
            }
            isForeground = false;
        }
    }

    public static void stopAthanAction(Context context) {
        Intent stopIntent = new Intent(context, AthanService.class);
        // Using stopService instead of startService with ACTION_STOP_ATHAN
        // to avoid BackgroundServiceStartNotAllowedException on Android 12+
        context.stopService(stopIntent);
    }

    public void onDestroy() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(mNotificationId);
        }

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
                    //stopAthanAction(context);
                    // we do not stop athan anymore as new wakelock does not allow lock with screen dimming
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
