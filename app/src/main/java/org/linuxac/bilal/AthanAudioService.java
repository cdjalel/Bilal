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
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.linuxac.bilal.activities.MainActivity;

import java.io.IOException;


public class AthanAudioService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener  {
    protected static final String TAG = "AthanAudioService";
    MediaPlayer audioPlayer = null;

	public void onCreate(){
	    super.onCreate();
	    Log.d(TAG, "onCreate()");
	}

    private void initMediaPlayer() {
        if (null == audioPlayer) {
            String path = "android.resource://" + getPackageName() + "/" + R.raw.athan;
            audioPlayer = new MediaPlayer();
            try {
                audioPlayer.setDataSource(getApplicationContext(), Uri.parse(path));
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage(), e);
            }
            audioPlayer.setOnPreparedListener(this);
            audioPlayer.setOnErrorListener(this);
            audioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            audioPlayer.prepareAsync(); // prepare async to not block main thread
            Log.d(TAG, "Audio player started asynchronously!");
        }
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
        initMediaPlayer();
        return Service.START_STICKY;
	}

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        audioPlayer.start();
        Log.d(TAG, "Audio started playing!");
        if(!audioPlayer.isPlaying()) {
	        Log.d(TAG, "Problem in playing audio");
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
                if (audioPlayer == null) initMediaPlayer();
                else if (!audioPlayer.isPlaying()) audioPlayer.start();
                audioPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (audioPlayer.isPlaying()) audioPlayer.stop();
                audioPlayer.release();
                audioPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (audioPlayer.isPlaying()) audioPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (audioPlayer.isPlaying()) audioPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

	public void onStop() {
		if (audioPlayer != null) {
            if (audioPlayer.isPlaying()) audioPlayer.stop();
            audioPlayer.release();
            audioPlayer = null;
        }
	}
	
	public void onPause() {
        if (audioPlayer.isPlaying()) audioPlayer.stop();
	}
	
	public void onDestroy() {
        onStop();
    }

	@Override
	public IBinder onBind(Intent intent) {
	    return null;
	}
}
