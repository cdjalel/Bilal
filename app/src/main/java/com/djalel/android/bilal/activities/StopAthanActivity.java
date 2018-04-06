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

package com.djalel.android.bilal.activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.receivers.AlarmReceiver;
import com.djalel.android.bilal.services.AthanAudioService;

import timber.log.Timber;

public class StopAthanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.i("onCreate");

        // stop athan audio
        if (UserSettings.isAthanEnabled(this)) {
            Intent stopIntent = new Intent(this, AthanAudioService.class);
            stopService(stopIntent);
        }

        // cancel notification
        if (UserSettings.isNotificationEnabled(this)) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                Timber.i("cancel notification");
                manager.cancel(AlarmReceiver.NOTIFICATION_ID);
            }
        }

        finish(); // since finish() is called in onCreate(), onDestroy() will be called immediately
    }
}
