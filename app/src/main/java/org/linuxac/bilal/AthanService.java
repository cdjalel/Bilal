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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.linuxac.bilal.helpers.UserSettings;
import org.linuxac.bilal.receivers.AlarmReceiver;

import java.io.IOException;


public class AthanService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener  {
    protected static final String TAG = "AthanAudioService";
    public static final String EXTRA_EVENT_ID = "org.linuxac.bilal.EVENT_ID";

    private MediaPlayer mAudioPlayer = null;
    private int mPrayer;

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != AlarmScheduler.sPrayerTimes) {
            mPrayer = AlarmScheduler.sPrayerTimes.getNextIndex();
        }
        else if (null != intent) {
            Log.w(TAG, "onStartCommand: AlarmScheduler.sPrayerTimes == null");
            mPrayer = intent.getIntExtra(AlarmReceiver.PRAYER_INDEX, 0);
        }
        else {
            Log.w(TAG, "onStartCommand: AlarmScheduler.sPrayerTimes == null");
            Log.w(TAG, "onStartCommand: intent == null");
            mPrayer = 2;        // 80% chance correct :)
        }
        initMediaPlayer();
        return Service.START_NOT_STICKY;
    }

    private void initMediaPlayer() {
        if (null == mAudioPlayer) {
            mAudioPlayer = new MediaPlayer();
            Context context = this; //getApplicationContext();      // TODO: why not this?
            String path = "android.resource://" + getPackageName() + "/" +
                    UserSettings.getMuezzin(context, mPrayer);

            try {
                mAudioPlayer.setDataSource(context, Uri.parse(path));
                mAudioPlayer.setOnPreparedListener(this);
                mAudioPlayer.setOnErrorListener(this);
                mAudioPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
                mAudioPlayer.prepareAsync(); // prepare async to not block main thread
                Log.d(TAG, "Audio player started asynchronously!");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        mAudioPlayer.start();
        Log.i(TAG, "Audio started playing!");
        if(!mAudioPlayer.isPlaying()) {
	        Log.w(TAG, "Problem in playing audio");
	    }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        Log.e(TAG, "what=" + what + " extra=" + extra);
        return false; // TODO change to true if error is handled by here.
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mAudioPlayer == null) initMediaPlayer();
                else if (!mAudioPlayer.isPlaying()) mAudioPlayer.start();
                mAudioPlayer.setVolume(1.0f, 1.0f);
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

	public void onStop() {

		if (mAudioPlayer != null) {
            if (mAudioPlayer.isPlaying()) mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }
	
	public void onPause() {
        if (mAudioPlayer.isPlaying()) mAudioPlayer.stop();
	}
	
	public void onDestroy() {
        onStop();
    }

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}
}
