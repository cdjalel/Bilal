/*
 *  Copyright © 2017 Djalel Chefrour
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


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import org.linuxac.bilal.AlarmScheduler;
import org.linuxac.bilal.helpers.UserSettings;
import org.linuxac.bilal.R;

import java.util.List;

import org.arabeyes.prayertime.Method;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "SettingsActivity";

    private static final int REQUEST_SEARCH_CITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || LocationsPreferenceFragment.class.getName().equals(fragmentName)
                //|| DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("general_language"));
            // TODO: Need to trigger UI locale reconfig in a separate listener?
        }
    }

    private static Preference.OnPreferenceChangeListener sMethodChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            String key = preference.getKey();
            Context ctxt = preference.getContext();
            Method method = new Method();

            if (key.equals("locations_method")) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ?
                            listPreference.getEntries()[index] : null);

                // Trigger new cacl if value change
                int oldMethodIdx = UserSettings.getCalculationMethod(preference.getContext());
                if (oldMethodIdx != ++index) {
                    Log.d(TAG, "New calc method: " + index);
                    method.setMethod(index);
                    method.round = UserSettings.getCalculationRound(ctxt)? 1 : 0;
                    AlarmScheduler.handleLocationChange(ctxt, method);
                }
            }
            else if (key.equals("locations_rounding")) {
                // Trigger new cacl if value change
                boolean oldRound  = UserSettings.getCalculationRound(preference.getContext());
                boolean newRound = stringValue.equals("true");
                if (oldRound != newRound) {
                    method.setMethod(UserSettings.getCalculationMethod(ctxt));
                    method.round = newRound ? 1 : 0;
                    AlarmScheduler.handleLocationChange(ctxt, method);
                }
            }
            else {
                // For other preferences, set the summary to the value's
                // simple string representation.
                //preference.setSummary(stringValue);
            }

            return true;
        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocationsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_locations);

            // Set summary to current user setting
            Preference pref = findPreference("locations_search_city");
            Context ctxt = pref.getContext();
            pref.setSummary(AlarmScheduler.getCityName(ctxt));

            // bind listener to start SearchCity activity
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Log.d(TAG, "onPrefClick");
                        startActivityForResult(preference.getIntent(), REQUEST_SEARCH_CITY);
                        return true;
                    }
                   });

            // Set method summary to current user setting
            int methodIdx = UserSettings.getCalculationMethod(ctxt);
            ListPreference listPref = (ListPreference) findPreference("locations_method");
            listPref.setSummary(methodIdx > 0 ? listPref.getEntries()[methodIdx - 1] : null);

            listPref.setOnPreferenceChangeListener(sMethodChangeListener);

            pref = findPreference("locations_rounding");
            pref.setOnPreferenceChangeListener(sMethodChangeListener);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.d(TAG, "onActivityResult");
            if (requestCode == REQUEST_SEARCH_CITY) {
                if(resultCode == Activity.RESULT_OK){
                    Preference pref = findPreference("locations_search_city");
                    pref.setSummary(data.getStringExtra("name"));

                    AlarmScheduler.handleLocationChange(getActivity(), null);
                }
                //else if (resultCode == Activity.RESULT_CANCELED) {
                //}
            }
        }
    }


    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notifications);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_muezzin"));

            Preference pref = findPreference("notifications_prayer_time");
            pref.setOnPreferenceChangeListener(sPrefPrayerTimeListener);
        }

        private static Preference.OnPreferenceChangeListener sPrefPrayerTimeListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, "sPrefPrayerTimeListener: " + newValue.toString());
                if (newValue.toString().equals("true")) {
                    AlarmScheduler.enableAlarm(preference.getContext());
                }
                else {
                    AlarmScheduler.disableAlarm(preference.getContext());
                }
                return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
            }
        };
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
/*
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

    }
*/
}
