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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import org.arabeyes.prayertime.*;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
//import android.widget.Toast;

public class BilalActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    public final static String NOTIFY_MESSAGE = "org.linuxac.bilal.NOTIFY";
    public final static String UPDATE_MESSAGE = "org.linuxac.bilal.UPDATE";

    protected static final String TAG = "Bilal";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    protected Boolean mReceiverIsRegistered;
    protected BroadcastReceiver mReceiver;

    protected TextView mTextViewCity;
    protected TextView mTextViewDate;
    protected TextView[][] mTextViewPrayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bilal);

        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play services unavailable.");
            finish();
            return;
        }

        buildGoogleApiClient();     // needed for Location
        initReceiver();
        loadViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bilal, menu);
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
            if (Log.isLoggable(TAG, Log.DEBUG)) { // TODO copy elsewhere
                Log.d(TAG, "Google Play services is available.");
            }
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
        if (!mReceiverIsRegistered) {
            registerReceiver(mReceiver, new IntentFilter(BilalActivity.UPDATE_MESSAGE));
            mReceiverIsRegistered = true;
        }
        updatePrayerTimes();
    }

    protected void onPause() {
        super.onPause();
        if (mReceiverIsRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverIsRegistered = false;
        }
    }

    private void initReceiver() {
        mReceiverIsRegistered = false;
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract data included in the Intent
                Log.d(TAG, "update prayer times");
                updatePrayerTimes();
            }
        };
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
        updatePrayerTimes();
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
        TextView[][] tvp = {
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
        mTextViewPrayers = tvp.clone();
    }

    protected void initPrayerViews(GregorianCalendar nowCal) {
        int i, j;

        // change Dhuhur to Jumuaa if needed.
        if (nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            mTextViewPrayers[2][0].setText(getString(R.string.jumu3a_ar));
            mTextViewPrayers[2][2].setText(getString(R.string.jumu3a_en));
        }

        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            for (j = 0; j < 3; j++) {
                mTextViewPrayers[i][j].setTypeface(null, Typeface.NORMAL);
                mTextViewPrayers[i][j].setTextColor(Color.rgb(0, 0, 0));
            }
        }

        // tomorrow's Fajr is only shown once today's Isha passed
        for (j = 0; j < 3; j++) {
            mTextViewPrayers[Prayer.NB_PRAYERS][j].setVisibility(TextView.INVISIBLE);
        }
    }

    protected void updatePrayerTimes() {
        int i, j;
        String cityName;

        // TODO: use location from user preferences, if not then start with settings
        // activity for user selection from a list with auto detection as an option
        PTLocation loc;
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            loc = new PTLocation(latitude, longitude, 1, 0, 697, 1010, 10);
            cityName = "Lat: " + latitude + " - Long:" + longitude;
        }
        else {
            // http://dateandtime.info/citycoordinates.php?id=2479215
            loc = new PTLocation(36.28639, 7.95111, 1, 0, 697, 1010, 10);
            cityName = "Souk Ahras";
        }
        mTextViewCity.setText(cityName);

        Method conf = new Method();
        conf.setMethod(Method.MUSLIM_LEAGUE);
        conf.round = 0;

        GregorianCalendar nowCal = new GregorianCalendar();
        Log.d(TAG, "Current time: " + DateFormat.getDateTimeInstance().format(nowCal.getTime()));

        Date today = nowCal.getTime();
        mTextViewDate.setText(DateFormat.getDateInstance().format(today));

        initPrayerViews(nowCal);

        /* Call the main function to fill the Prayer times */
        Prayer prayer = new Prayer();
        PrayerTime[] pt = prayer.getPrayerTimes(loc, conf, today);
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            mTextViewPrayers[i][1].setText(
                    String.format(" %3d:%02d\n", pt[i].hour, pt[i].minute));
            Log.d(TAG, mTextViewPrayers[i][0].getText().toString() + " "
                    + mTextViewPrayers[i][1].getText().toString() );
        }

        // Find next prayer and set alarm
        GregorianCalendar next = null;
        GregorianCalendar[] ptCal = new GregorianCalendar[Prayer.NB_PRAYERS];
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            ptCal[i] = (GregorianCalendar)nowCal.clone();
            ptCal[i].set(Calendar.HOUR_OF_DAY, pt[i].hour);
            ptCal[i].set(Calendar.MINUTE, pt[i].minute);
            ptCal[i].set(Calendar.SECOND, pt[i].second);
            Log.d(TAG, mTextViewPrayers[i][0].getText().toString() + " " +
                    DateFormat.getDateTimeInstance().format(ptCal[i].getTime()));
        }

        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            if (ptCal[i].after(nowCal)) {
                if (i == 1) {
                    i++;            // skip sunset which isn't a prayer
                }
                next = ptCal[i];
                break;
            }
        }

        if (next == null) {
            // next prayer is tomorrow's Fajr
            PrayerTime nextPT = prayer.getNextDayFajr(loc, conf, today);
            // then i == Prayer.NB_PRAYERS
            mTextViewPrayers[i][1].setText(
                    String.format(" %3d:%02d\n", nextPT.hour, nextPT.minute));
            for (j = 0; j < 3; j++) {
                mTextViewPrayers[i][j].setVisibility(TextView.VISIBLE);
            }

            next = (GregorianCalendar)nowCal.clone();
            next.add(Calendar.DATE, 1);
            next.set(Calendar.HOUR_OF_DAY, nextPT.hour);
            next.set(Calendar.MINUTE, nextPT.minute);
            next.set(Calendar.SECOND, nextPT.second);
        }

        // highlight next prayer
        Log.d(TAG, "Next prayer is " + mTextViewPrayers[i][2].getText().toString());
        for (j = 0; j < 3; j++) {
            mTextViewPrayers[i][j].setTypeface(null, Typeface.BOLD);
            mTextViewPrayers[i][j].setTextColor(Color.rgb(0, 200, 0));
        }

        // prepare alarm message in AR only
        if (i == Prayer.NB_PRAYERS) {
            i = 0;              // Removes "next" from "next fajr"
        }
        String message = getString(R.string.hana_ar) + " " +
                mTextViewPrayers[i][0].getText().toString() + " " +
                mTextViewPrayers[i][1].getText().toString();

        // Schedule alarm
        Intent alarmIntent = new Intent(BilalActivity.this, BilalAlarm.class);
        alarmIntent.putExtra(NOTIFY_MESSAGE, message);
        PendingIntent sender = PendingIntent.getBroadcast(BilalActivity.this, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), sender);
        Log.d(TAG, "Alarm scheduled for " +
                DateFormat.getDateTimeInstance().format(next.getTime()));
    }
}
