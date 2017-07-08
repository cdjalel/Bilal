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
import android.graphics.Typeface;
import android.location.Location;

import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements
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
            registerReceiver(mReceiver, new IntentFilter(MainActivity.UPDATE_MESSAGE));
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
//        updatePrayerTimes();
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

    protected void initPrayerViews(GregorianCalendar calendar, Date date) {
        int i, j;

        mTextViewDate.setText(DateFormat.getDateInstance().format(date));

        // change Dhuhur to Jumuaa if needed.
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            mTextViewPrayers[2][0].setText(getString(R.string.jumu3a_ar));
            mTextViewPrayers[2][2].setText(getString(R.string.jumu3a_en));
        }

        for (i = 0; i < Prayer.NB_PRAYERS + 1; i++) {
            for (j = 0; j < 3; j++) {
                mTextViewPrayers[i][j].setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    protected void updatePrayerTimes() {
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
        mTextViewCity.setText(cityName);        // TODO pass to initPrayerViews()

        Method conf = new Method();
        conf.setMethod(Method.MUSLIM_LEAGUE);
        conf.round = 0;

        GregorianCalendar nowCal = new GregorianCalendar();
        Date today = nowCal.getTime();
        Log.d(TAG, "Current time: " + DateFormat.getDateTimeInstance().format(today));

        initPrayerViews(nowCal, today);
        GregorianCalendar[] ptCal = setPrayerTimes(loc, conf, nowCal, today);

        CurrentPrayer current = new CurrentPrayer(nowCal, ptCal).find();
        emphasizeCurrentPrayer(current.getIndex());
        scheduleNextPrayerAthan(current.getNextIndex(), current.getNextCal());
    }

    private void emphasizeCurrentPrayer(int c) {
        for (int j = 0; j < 3; j++) {
            mTextViewPrayers[c][j].setTypeface(null, Typeface.BOLD);
        }
    }

    @NonNull
    private GregorianCalendar[] setPrayerTimes(PTLocation loc, Method conf, GregorianCalendar nowCal, Date today) {
        int i;
        Prayer prayer = new Prayer();
        GregorianCalendar[] ptCal = new GregorianCalendar[Prayer.NB_PRAYERS + 1];

        /* Call the main library function to fill the Prayer times */
        PrayerTime[] pt = prayer.getPrayerTimes(loc, conf, today);
        for (i = 0; i < Prayer.NB_PRAYERS; i++) {
            ptCal[i] = (GregorianCalendar)nowCal.clone();
            ptCal[i].set(Calendar.HOUR_OF_DAY, pt[i].hour);
            ptCal[i].set(Calendar.MINUTE, pt[i].minute);
            ptCal[i].set(Calendar.SECOND, pt[i].second);
            mTextViewPrayers[i][1].setText(
                    String.format("%02d:%02d\n", pt[i].hour, pt[i].minute));
            Log.d(TAG, mTextViewPrayers[i][0].getText().toString() + " "
                    + mTextViewPrayers[i][1].getText().toString() );
        }
        PrayerTime nextPT = prayer.getNextDayFajr(loc, conf, today);
        ptCal[i] = (GregorianCalendar)nowCal.clone();
        ptCal[i].add(Calendar.DATE, 1);
        ptCal[i].set(Calendar.HOUR_OF_DAY, nextPT.hour);
        ptCal[i].set(Calendar.MINUTE, nextPT.minute);
        ptCal[i].set(Calendar.SECOND, nextPT.second);
        mTextViewPrayers[i][1].setText(
                String.format("%02d:%02d\n", nextPT.hour, nextPT.minute));
        Log.d(TAG, mTextViewPrayers[i][0].getText().toString() + " "
                + mTextViewPrayers[i][1].getText().toString() );
        return ptCal;
    }

    private void scheduleNextPrayerAthan(int prayer, GregorianCalendar next)
    {
        // prepare alarm message. TODO: AR only?
        if (prayer == Prayer.NB_PRAYERS) {
            prayer = 0;              // Removes "next" from "next fajr"
        }
        String message = getString(R.string.hana_ar) + " " +
                mTextViewPrayers[prayer][0].getText().toString() + " " +
                mTextViewPrayers[prayer][1].getText().toString();

        // Schedule alarm
        Intent alarmIntent = new Intent(MainActivity.this, AthanBroadcastReceiver.class);
        alarmIntent.putExtra(NOTIFY_MESSAGE, message);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), sender);
        Log.d(TAG, "Alarm scheduled for " +
                DateFormat.getDateTimeInstance().format(next.getTime()));
    }

    private class CurrentPrayer {
        private GregorianCalendar nowCal;
        private GregorianCalendar[] ptCal;
        private int i;
        private int c;
        private GregorianCalendar next;

        public CurrentPrayer(GregorianCalendar nowCal, GregorianCalendar... ptCal) {
            this.nowCal = nowCal;
            this.ptCal = ptCal;
        }

        public int getNextIndex() {
            return i;
        }

        public int getIndex() {
            return c;
        }

        public GregorianCalendar getNextCal() {
            return next;
        }

        public CurrentPrayer find() {
            // Find current and next prayers
            for (i = 0; i < Prayer.NB_PRAYERS; i++) {
                if (nowCal.before(ptCal[i])) {
                    break;
                }
            }

            switch (i) {
                case 0:
                    next = ptCal[c = 0];
                    break;
                case 1:
                    // sunset is skipped. TODO make this a user setting
                    i = 2;
                    // FALLTHROUGH
                case 2:
                    c = 0;
                    next = ptCal[2];
                    break;
                case 3:
                case 4:
                case 5:
                case Prayer.NB_PRAYERS:
                default:
                    c = i-1;
                    next = ptCal[i];
                    break;
            }

            Log.d(TAG, "Current prayer is " + mTextViewPrayers[c][2].getText().toString());
            Log.d(TAG, "Next prayer is " + mTextViewPrayers[i][2].getText().toString());
            return this;
        }
    }
}
