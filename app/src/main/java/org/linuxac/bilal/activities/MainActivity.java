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

package org.linuxac.bilal.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.location.Location;

import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
//import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import org.arabeyes.prayertime.*;
import org.linuxac.bilal.AthanManager;
import org.linuxac.bilal.R;
import org.linuxac.bilal.helpers.PrayerTimes;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    public static final String MESSAGE_UPDATE_VIEWS = "org.linuxac.bilal.UPDATE";

    protected static final String TAG = "MainActivity";

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    protected Boolean mReceiverRegistered = false;
    protected BroadcastReceiver mUpdateViewsReceiver = null;

    protected boolean mIsJumu3a = false;

    protected TextView mTextViewCity;
    protected TextView mTextViewDate;
    protected TextView[][] mTextViewPrayers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play services unavailable.");
            finish();
            return;
        }
        buildGoogleApiClient();     // needed for Location

        // TODO loads prefs

        initReceiver();
        loadViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
            Log.d(TAG, "Google Play services unavailable.");
            return true;
        } else {
            Log.e(TAG, "Google Play services is unavailable.");
            return false;
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void initReceiver() {
        mReceiverRegistered = false;
        mUpdateViewsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Receiver kicked by action: " + intent.getAction());
                // prayer times have been update by the Alarm Receiver which is the action sender
                updatePrayerViews();
            }
        };
    }

//    private void deleteReceiver() {
//        if (mReceiverRegistered) {
//            assert(null != mUpdateViewsReceiver);
//            unregisterReceiver(mUpdateViewsReceiver);
//            mReceiverRegistered = false;
//        }
//        mUpdateViewsReceiver = null;
//    }

    @Override
    protected void onStart() {
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
        super.onResume();
        Log.d(TAG, "onResume: rcvr reg = " + mReceiverRegistered);

        if (AthanManager.isAlarmEnabled() && !mReceiverRegistered) {
            registerReceiver(mUpdateViewsReceiver, new IntentFilter(MESSAGE_UPDATE_VIEWS));
            mReceiverRegistered = true;
        }
        AthanManager.updatePrayerTimes(this);
        updatePrayerViews();
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: rcvr reg = " + mReceiverRegistered);

        if (mReceiverRegistered) {
            assert(null != mUpdateViewsReceiver);
            unregisterReceiver(mUpdateViewsReceiver);
            mReceiverRegistered = false;
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
            Log.w(TAG, "onConnected: No location detected");
        }
        else {
            Log.w(TAG, "onConnected: location detected");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    protected void loadViews() {
        mTextViewCity = (TextView) findViewById(R.id.textViewCity);
        mTextViewDate = (TextView) findViewById(R.id.textViewDate);
        mTextViewPrayers = new TextView[][] {
            {
                    (TextView) findViewById(R.id.textViewFajrAR),
                    (TextView) findViewById(R.id.textViewFajr),
                    (TextView) findViewById(R.id.textViewFajrEN)
            },
            {
                    (TextView) findViewById(R.id.textViewSunriseAR),
                    (TextView) findViewById(R.id.textViewSunrise),
                    (TextView) findViewById(R.id.textViewSunriseEN)
            },
            {
                    (TextView) findViewById(R.id.textViewDhuhurAR),
                    (TextView) findViewById(R.id.textViewDhuhur),
                    (TextView) findViewById(R.id.textViewDhuhurEN)
            },
            {
                    (TextView) findViewById(R.id.textViewAsrAR),
                    (TextView) findViewById(R.id.textViewAsr),
                    (TextView) findViewById(R.id.textViewAsrEN)
            },
            {
                    (TextView) findViewById(R.id.textViewMaghribAR),
                    (TextView) findViewById(R.id.textViewMaghrib),
                    (TextView) findViewById(R.id.textViewMaghribEN)
            },
            {
                    (TextView) findViewById(R.id.textViewIshaAR),
                    (TextView) findViewById(R.id.textViewIsha),
                    (TextView) findViewById(R.id.textViewIshaEN)
            },
            {
                    (TextView) findViewById(R.id.textViewNextFajrAR),
                    (TextView) findViewById(R.id.textViewNextFajr),
                    (TextView) findViewById(R.id.textViewNextFajrEN)
            }
        };
    }

    protected void setPrayerTimesViews(String cityName, PrayerTimes prayerTimes) {
        int i, j;

        mTextViewCity.setText(cityName);

        if (null == prayerTimes) {
            mTextViewDate.setText(getString(R.string.not_available));
            return;
        }

        GregorianCalendar today = prayerTimes.getCurrent();
        mTextViewDate.setText(DateFormat.getDateInstance().format(today.getTime()));

        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            mTextViewPrayers[i][1].setText(prayerTimes.format(i));
        }

        // change Dhuhur to Jumuaa if needed.
        if (today.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            mTextViewPrayers[2][0].setText(getString(R.string.jumu3a_ar));
            mTextViewPrayers[2][2].setText(getString(R.string.jumu3a_en));
            mIsJumu3a = true;
        }
        else if (mIsJumu3a) {
            mTextViewPrayers[2][0].setText(getString(R.string.dhuhur_ar));
            mTextViewPrayers[2][2].setText(getString(R.string.dhuhur_en));
            mIsJumu3a = false;
        }

        // emphasize Current Prayer
        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            for (j = 0; j < 3; j++) {
                mTextViewPrayers[i][j].setTypeface(null,
                        i == prayerTimes.getCurrentIndex() ? Typeface.BOLD_ITALIC : Typeface.NORMAL);
            }
        }
    }

    private void updatePrayerViews() {
        setPrayerTimesViews(AthanManager.sCityName, AthanManager.sPrayerTimes);
    }
}
