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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.res.Configuration;
import android.graphics.Typeface;

import android.os.Build;
import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;

import android.os.Handler;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.View;
import android.widget.TextView;

import com.djalel.android.bilal.PrayerTimesApp;
import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.services.AthanService;

import org.arabeyes.prayertime.*;
import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.helpers.UserSettings;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    public static final String UPDATE_VIEWS = "com.djalel.android.bilal.UPDATE";

    private static final int BLINK_DURATION = 500;
    private static final int BLINK_COUNT    = AthanService.ATHAN_DURATION/BLINK_DURATION;

    private Boolean mUVReceiverRegistered = false;
    private BroadcastReceiver mUpdateViewsReceiver = null;

    private int mImportant = -1;

    private TextView mTextViewCity;
    private TextView mTextViewDate;
    private TextView mTextViewToNext;
    private TextView[][] mTextViewPrayers;

    // The time (in ms) interval to update the countdown mTextViewToNex.
    private static final int COUNT_INTERVAL_SECOND = 1000;
    private static final int COUNT_INTERVAL_MINUTE = 60 * 1000;
    private Handler mCountHandler;
    private Runnable mUpdateCount;

    ActivityResultLauncher<Intent> mSearchCityActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        // MainActivity launch mode is default, so its task is :
        // - (re)created when started by the notification manager (from AlarmReceiver) as it uses
        //    Intent.FLAG_ACTIVITY_NEW_TASK
        // - cleared (i.e. all activities are finished, especially the Settings one) as the latter
        //   starts this Main one with Intent.FLAG_ACTIVITY_NEW_TASK when Language changes to force UI refresh.

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        // The other two entry points of the app, which run when MainActivity usually does not, are:
        //      - the alarm receiver called at prayer time
        //      - the boot complete and time change receiver
        // Both receivers will call AthanAlarm.updatePrayerTimes to schedule next alarm if
        // the Athan notification is enabled. As the latter needs to check this setting we need to
        // load its default a priori. This is done only here, as it is a mandatory execution path
        // before enabling both receivers by enabling Athan notification in the Settings activity.

        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.location_preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.notifications_preferences, false);
        //PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);

        // TODO force SearchCityActivity on 1st run with an intermediate explanatory dialogue

        mSearchCityActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Timber.d("onActivityResult");
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        // Intent data = result.getData();
                        PrayerTimesManager.handleSettingsChange(this, -1, -1, -1);
                        //updatePrayerViews() is called by OnResume anyway.
                        // It will also retrieve new city name from settings, so no need to read it
                        // here from the intent
                    }
                });
        
        createNotificationChannel();

        initReceiver();
        loadViews();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // TODO 'find mosque in map' button in action bar

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initReceiver() {
        mUVReceiverRegistered = false;
        mUpdateViewsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("Receiver kicked by action: %s", intent.getAction());
                // prayer times have been update by the Alarm Receiver or TimeChange Receiver
                updatePrayerViews();
            }
        };
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    protected void onResume() {
        Timber.d("OnResume");
        super.onResume();
        if (UserSettings.isNotificationEnabled(this) && !mUVReceiverRegistered) {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(mUpdateViewsReceiver, new IntentFilter(UPDATE_VIEWS), RECEIVER_EXPORTED);
            }
            else {
                registerReceiver(mUpdateViewsReceiver, new IntentFilter(UPDATE_VIEWS));
            }
            mUVReceiverRegistered = true;
        }
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        if (null != alarmMgr && Build.VERSION.SDK_INT >= 31 && alarmMgr.canScheduleExactAlarms()) {
            PrayerTimesManager.updatePrayerTimes(this, true);
        }
        else {
            PrayerTimesManager.updatePrayerTimes(this, false);
        }
        updatePrayerViews();
    }

    protected void onPause() {
        Timber.d("OnPause");
        super.onPause();
        if (mUVReceiverRegistered) {
            unregisterReceiver(mUpdateViewsReceiver);
            mUVReceiverRegistered = false;
        }
        stopCount();
    }

    private void loadViews() {
        mTextViewCity = findViewById(R.id.textViewCity);
        mTextViewDate = findViewById(R.id.textViewDate);
        mTextViewToNext = findViewById(R.id.textViewToNext);
        mTextViewPrayers = new TextView[][] {
            {
                    findViewById(R.id.textViewFajrName),
                    findViewById(R.id.btnStopFajr),
                    findViewById(R.id.textViewFajrTime)
            },
            {
                    findViewById(R.id.textViewSunriseName),
                    findViewById(R.id.btnStopSunrise),
                    findViewById(R.id.textViewSunriseTime)
            },
            {
                    findViewById(R.id.textViewDhuhrName),
                    findViewById(R.id.btnStopDhuhr),
                    findViewById(R.id.textViewDhuhrTime)
            },
            {
                    findViewById(R.id.textViewAsrName),
                    findViewById(R.id.btnStopAsr),
                    findViewById(R.id.textViewAsrTime)
            },
            {
                    findViewById(R.id.textViewMaghribName),
                    findViewById(R.id.btnStopMaghrib),
                    findViewById(R.id.textViewMaghribTime)
            },
            {
                    findViewById(R.id.textViewIshaName),
                    findViewById(R.id.btnStopIsha),
                    findViewById(R.id.textViewIshaTime)
            },
            {
                    findViewById(R.id.textViewNextFajrName),
                    findViewById(R.id.btnStopNextFajr),
                    findViewById(R.id.textViewNextFajrTime)
            }
        };

        View.OnClickListener cityListener = v -> {
            Intent intent = new Intent(v.getContext(), SearchCityActivity.class);
            //startActivityForResult(intent, REQUEST_SEARCH_CITY);
            mSearchCityActivityResultLauncher.launch(intent);
        };
        mTextViewCity.setOnClickListener(cityListener);
        mTextViewDate.setOnClickListener(cityListener);
    }

    private void updatePrayerViews()
    {
        if (PrayerTimesManager.prayerTimesNotAvailable()) {
            Timber.w("prayerTimesNotAvailable");
            return;
        }

        int i, j;

        GregorianCalendar now = new GregorianCalendar();
        mTextViewCity.setText(UserSettings.getCityName(this));

        mTextViewDate.setText(DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                .format(now.getTime()));

        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            mTextViewPrayers[i][1].setVisibility(View.INVISIBLE);
            mTextViewPrayers[i][2].setText(PrayerTimesManager.formatPrayerTime(i));
        }

        // change Dhuhr to Jumuaa if needed.
        mTextViewPrayers[2][0].setText(PrayerTimes.getName(this,2,now));

        // Reset old important prayer to normal
        if (mImportant != -1) {
            for (j = 0; j < mTextViewPrayers[0].length; j++) {
                mTextViewPrayers[mImportant][j].setTypeface(null, Typeface.NORMAL);
                mTextViewPrayers[mImportant][j].clearAnimation();
                mTextViewPrayers[mImportant][j].setOnClickListener(null);
            }
        }

        // signal the new important prayer, which is the current if its time is recent, next otherwise
        GregorianCalendar current = PrayerTimesManager.getCurrentPrayer();
        if (current == null) {
            return;
        }
        long elapsed = now.getTimeInMillis() - current.getTimeInMillis();
        if (elapsed >= 0 && elapsed <= AthanService.ATHAN_DURATION) {
            mTextViewToNext.setText(PrayerTimesManager.formatTimeFromCurrentPrayer(MainActivity.this, now));
            startCount(false);

            // blink Current Prayers
            mImportant = PrayerTimesManager.getCurrentPrayerIndex();
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(BLINK_DURATION);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(BLINK_COUNT);
            for (j = 0; j < mTextViewPrayers[0].length; j++) {
                mTextViewPrayers[mImportant][j].setTypeface(null, Typeface.BOLD );
                mTextViewPrayers[mImportant][j].startAnimation(anim);
            }

            // add stop button if Athan is ON
            if (UserSettings.isAthanEnabled(this)) {
                View.OnClickListener currentPrayerListener = v -> {
                    Timber.d("currentPrayerListener");
                    for (int j1 = 0; j1 < mTextViewPrayers[0].length; j1++) {
                        mTextViewPrayers[mImportant][j1].clearAnimation();
                        mTextViewPrayers[mImportant][j1].setOnClickListener(null);
                    }
                    mTextViewPrayers[mImportant][1].setVisibility(View.INVISIBLE);
                    AthanService.stopAthanAction(v.getContext());
                };
                mTextViewPrayers[mImportant][1].setVisibility(View.VISIBLE);
                for (j = 0; j < mTextViewPrayers[0].length; j++) {
                    mTextViewPrayers[mImportant][j].setOnClickListener(currentPrayerListener);
                }
            }
        }
        else {
            mTextViewToNext.setText(PrayerTimesManager.formatTimeToNextPrayer(this, now));
            startCount(true);

            // Bold Next Prayers
            mImportant = PrayerTimesManager.getNextPrayerIndex();
            for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
                for (j = 0; j < mTextViewPrayers[0].length; j++) {
                    mTextViewPrayers[mImportant][j].setTypeface(null, Typeface.BOLD);
                }
            }
        }
    }

    private void stopCount() {
        if (mCountHandler != null) {
            mCountHandler.removeCallbacks(mUpdateCount);
            mCountHandler = null;
        }

        if (mUpdateCount != null) {
            mUpdateCount = null;
        }
    }

    private void startCount(final boolean down) {
        stopCount();

        mCountHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        mUpdateCount = () -> {
            try {
                GregorianCalendar now = new GregorianCalendar();
                mTextViewToNext.setText(down ?
                        PrayerTimesManager.formatTimeToNextPrayer(MainActivity.this, now) :
                        PrayerTimesManager.formatTimeFromCurrentPrayer(MainActivity.this, now));
            } finally {
                mCountHandler.postDelayed(mUpdateCount,
                        UserSettings.getRounding(MainActivity.this) == 1 ?
                                COUNT_INTERVAL_MINUTE : COUNT_INTERVAL_SECOND);
            }
        };
        mUpdateCount.run();
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(PrayerTimesApp.updateLocale(newBase));
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(AthanService.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
