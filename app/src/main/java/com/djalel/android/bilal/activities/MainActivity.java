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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;

import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.View;
import android.widget.TextView;

import com.djalel.android.bilal.helpers.PrayerTimes;
import com.djalel.android.bilal.services.AthanService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
//import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import org.arabeyes.prayertime.*;
import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.helpers.UserSettings;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    public static final String UPDATE_VIEWS = "com.djalel.android.bilal.UPDATE";

    private static final int BLINK_DURATION = 500;
    private static final int BLINK_COUNT    = AthanService.ATHAN_DURATION/BLINK_DURATION;

    private static final int REQUEST_SEARCH_CITY = 2;

    private GoogleApiClient mGoogleApiClient;
//    private Location mLastLocation;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        // MainActivity launch mode is default, so its task is :
        // - (re)created when started by the notification manager (from AlarmReceiver) as it uses
        //    Intent.FLAG_ACTIVITY_NEW_TASK
        // - cleared (i.e. all activities are finished, especially the Settings one) as the latter
        //   starts this Main one with Intent.FLAG_ACTIVITY_NEW_TASK when Language changes to force UI refresh.

        UserSettings.loadLocale(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        if (!isGooglePlayServicesAvailable()) {
            Timber.e("Google Play services unavailable.");
            finish();
            return;
        }
        buildGoogleApiClient();     // needed for Location

        // The other two entry points of the app, which run when MainActivity usually does not, are:
        //      - the alarm receiver called at prayer time
        //      - the boot complete and time change receiver
        // Both receivers will call AthanAlarm.updatePrayerTimes to schedule next alarm if
        // the Athan notification is enabled. As the latter needs to check this setting we need to
        // load its default a priori. This is done only here, as it is a mandatory execution path
        // before enabling both receivers by enabling Athan notification in the Settings activity.

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_locations, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        //PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);

        // TODO force SearchCityActivity on 1st run with an intermediate explanatory dialogue

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

    /**
     * Checks if Google Play services is available.
     * @return true if it is.
     */
    private boolean isGooglePlayServicesAvailable() {
//        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            Timber.d("Google Play services available.");
            return true;
        } else {
            Timber.e("Google Play services is unavailable.");
            return false;
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void initReceiver() {
        mUVReceiverRegistered = false;
        mUpdateViewsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("Receiver kicked by action: " + intent.getAction());
                // prayer times have been update by the Alarm Receiver or TimeChange Receiver
                updatePrayerViews();
            }
        };
    }

    @Override
    protected void onStart() {
        Timber.d("onStart");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Timber.d("OnStop");
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void onResume() {
        Timber.d("OnResume");
        super.onResume();
        if (UserSettings.isNotificationEnabled(this) && !mUVReceiverRegistered) {
            registerReceiver(mUpdateViewsReceiver, new IntentFilter(UPDATE_VIEWS));
            mUVReceiverRegistered = true;
        }
        PrayerTimesManager.updatePrayerTimes(this, false);
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

    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        // TODO checkPermission()
//        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        if (mLastLocation == null) {
//            //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
//            Timber.w("onConnected: No location detected");
//        }
//        else {
//            Timber.w("onConnected: location detected");
//        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Timber.i("Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Timber.i("Connection suspended");
        mGoogleApiClient.connect();
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


        View.OnClickListener cityListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SearchCityActivity.class);
                startActivityForResult(intent, REQUEST_SEARCH_CITY);
            }
        };
        mTextViewCity.setOnClickListener(cityListener);
        mTextViewDate.setOnClickListener(cityListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("onActivityResult");
        if (requestCode == REQUEST_SEARCH_CITY) {
            if(resultCode == Activity.RESULT_OK){
                PrayerTimesManager.handleLocationChange(this, -1, -1, -1);
                //updatePrayerViews(); // This is called by OnResume anyway.
                // It will also retrieve new city name from settings, so no need to read it
                // from the intent here
            }
        }
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
                View.OnClickListener currentPrayerListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Timber.d("currentPrayerListener");
                        for (int j = 0; j < mTextViewPrayers[0].length; j++) {
                            mTextViewPrayers[mImportant][j].clearAnimation();
                            mTextViewPrayers[mImportant][j].setOnClickListener(null);
                        }
                        mTextViewPrayers[mImportant][1].setVisibility(View.INVISIBLE);
                        AthanService.stopAthanAction(v.getContext());
                    }
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

        mCountHandler = new Handler();
        mUpdateCount = new Runnable() {
            @Override
            public void run() {
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
            }
        };
        mUpdateCount.run();
    }
}
