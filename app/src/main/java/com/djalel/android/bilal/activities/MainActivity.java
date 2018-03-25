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
import android.location.Location;

import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.View;
import android.widget.TextView;
//import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import org.arabeyes.prayertime.*;
import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.helpers.UserSettings;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    public static final String UPDATE_VIEWS = "com.djalel.android.bilal.UPDATE";
    private static final String TAG = "MainActivity";

    private static int BLINK_INTERVAL = 5 * 60 * 1000;
    private static int BLINK_DURATION = 500;
    private static int BLINK_COUNT    = BLINK_INTERVAL/BLINK_DURATION;

    private static final int REQUEST_SEARCH_CITY = 2;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private Boolean mUVReceiverRegistered = false;
    private BroadcastReceiver mUpdateViewsReceiver = null;

    private boolean mIsJumu3a = false;
    private int mImportant = -1;

    private TextView mTextViewCity;
    private TextView mTextViewDate;
    private TextView[][] mTextViewPrayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // MainActivity launch mode is default, so a its task is :
        // - (re)created when started by the notification manager (from AlarmReceiver) as it uses
        //    Intent.FLAG_ACTIVITY_NEW_TASK
        // - cleared (i.e. all activities are finished, especially the Settings one) as it hte latter
        //   starts this one with Intent.FLAG_ACTIVITY_NEW_TASK when Language changes to force UI refresh.

        UserSettings.loadLocale(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
            //Log.e(TAG, "Google Play services unavailable.");
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
    // TODO stop athan button in action bar

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
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            //Log.d(TAG, "Google Play services unavailable.");
            return true;
        } else {
            //Log.e(TAG, "Google Play services is unavailable.");
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
                //Log.d(TAG, "Receiver kicked by action: " + intent.getAction());
                // prayer times have been update by the Alarm Receiver which is the action sender
                updatePrayerViews();
            }
        };
    }

    @Override
    protected void onStart() {
        //Log.d(TAG, "onStart");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void onResume() {
        //Log.d(TAG, "OnResume");
        super.onResume();
        if (UserSettings.isAlarmEnabled(this) && !mUVReceiverRegistered) {
            registerReceiver(mUpdateViewsReceiver, new IntentFilter(UPDATE_VIEWS));
            mUVReceiverRegistered = true;
        }
        PrayerTimesManager.updatePrayerTimes(this, false);
        updatePrayerViews();
    }

    protected void onPause() {
        super.onPause();
        if (mUVReceiverRegistered) {
            unregisterReceiver(mUpdateViewsReceiver);
            mUVReceiverRegistered = false;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            //Log.w(TAG, "onConnected: No location detected");
        }
        else {
            //Log.w(TAG, "onConnected: location detected");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        //Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        //Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private void loadViews() {
        mTextViewCity = (TextView) findViewById(R.id.textViewCity);
        mTextViewDate = (TextView) findViewById(R.id.textViewDate);
        mTextViewPrayers = new TextView[][] {
            {
                    (TextView) findViewById(R.id.textViewFajrName),
                    (TextView) findViewById(R.id.textViewFajrTime)
            },
            {
                    (TextView) findViewById(R.id.textViewSunriseName),
                    (TextView) findViewById(R.id.textViewSunriseTime)
            },
            {
                    (TextView) findViewById(R.id.textViewDhuhrName),
                    (TextView) findViewById(R.id.textViewDhuhrTime)
            },
            {
                    (TextView) findViewById(R.id.textViewAsrName),
                    (TextView) findViewById(R.id.textViewAsrTime)
            },
            {
                    (TextView) findViewById(R.id.textViewMaghribName),
                    (TextView) findViewById(R.id.textViewMaghribTime)
            },
            {
                    (TextView) findViewById(R.id.textViewIshaName),
                    (TextView) findViewById(R.id.textViewIshaTime)
            },
            {
                    (TextView) findViewById(R.id.textViewNextFajrName),
                    (TextView) findViewById(R.id.textViewNextFajrTime)
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
        //Log.d(TAG, "onActivityResult");
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
            return;
        }

        int i, j;

        GregorianCalendar now = new GregorianCalendar();
        mTextViewCity.setText(UserSettings.getCityName(this));

        mTextViewDate.setText(DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                .format(now.getTime()));

        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            mTextViewPrayers[i][1].setText(PrayerTimesManager.formatPrayer(i));
        }

        // change Dhuhr to Jumuaa if needed.
        if (now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            mTextViewPrayers[2][0].setText(getString(R.string.jumu3a));
            mIsJumu3a = true;
        }
        else if (mIsJumu3a) {
            mTextViewPrayers[2][0].setText(getString(R.string.dhuhr));
            mIsJumu3a = false;
        }

        // Reset old important prayer to normal
        if (mImportant != -1) {
            for (j = 0; j < mTextViewPrayers[0].length; j++) {
                mTextViewPrayers[mImportant][j].setTypeface(null, Typeface.NORMAL);
                mTextViewPrayers[mImportant][j].clearAnimation();
            }
        }

        // signal the new important prayer, which is the current if its time is recent, next otherwise
        GregorianCalendar current = PrayerTimesManager.getCurrentPrayer();
        long elapsed = now.getTimeInMillis() - current.getTimeInMillis();
        if (elapsed >= 0 && elapsed <= BLINK_INTERVAL) {
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

            // TODO add touch listener to stop blinking and open map of nearest mosques
        }
        else {
            // Bold Next Prayers
            mImportant = PrayerTimesManager.getNextPrayerIndex();
            for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
                for (j = 0; j < mTextViewPrayers[0].length; j++) {
                    mTextViewPrayers[mImportant][j].setTypeface(null, Typeface.BOLD);
                }
            }
        }
    }
}