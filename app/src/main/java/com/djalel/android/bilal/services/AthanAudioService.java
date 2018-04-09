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

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
//import android.os.PowerManager;
import timber.log.Timber;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.helpers.WakeLocker;

import java.io.IOException;


public class AthanAudioService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener
{
    public static final String EXTRA_PRAYER = "com.djalel.android.bilal.PRAYER";
    public static final String EXTRA_MUEZZIN = "com.djalel.android.bilal.MUEZZIN";
    public static final int ATHAN_DURATION= 6 * 60 * 1000;          // longest audio is 5' 10''

    private String mAthanFile;
    private MediaPlayer mAudioPlayer = null;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");

        stopAudio();           // in case played by Alarm & settings in parallel

        int prayerIndex;
        String muezzin;
        if (null != intent) {
            prayerIndex = intent.getIntExtra(EXTRA_PRAYER, 2);
            muezzin = intent.getStringExtra(EXTRA_MUEZZIN);
        }
        else { // fallback
            Timber.e("onStartCommand: intent == null");
            prayerIndex = PrayerTimesManager.getCurrentPrayerIndex();
            muezzin = UserSettings.getMuezzin(this);
        }
        mAthanFile = "android.resource://" + getPackageName() + "/" +
                UserSettings.getMuezzinRes(muezzin, prayerIndex);

        initMediaPlayer();
        return Service.START_NOT_STICKY;        // Don't restart if killed by system for low mem.
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
        return false; // TODO change to true if error is handled by here.
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
        stopAudio();
        WakeLocker.release();
    }

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}
}
