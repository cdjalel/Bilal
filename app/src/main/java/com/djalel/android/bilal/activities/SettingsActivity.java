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

package com.djalel.android.bilal.activities;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.R;

import java.util.List;

import org.arabeyes.prayertime.Method;
import com.djalel.android.bilal.services.AthanService;

import timber.log.Timber;

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
    private static final int REQUEST_SEARCH_CITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(getString(R.string.app_name));
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

    private static int setListPrefSummary(Preference pref, String value) {
        ListPreference listPref = (ListPreference) pref;
        int index = listPref.findIndexOfValue(value);
        listPref.setSummary(index >= 0 ? listPref.getEntries()[index] : null);
        return index;
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

            // Set language summary to current user setting
            Preference pref = findPreference("general_language");
            String language = UserSettings.getPrefLanguage(pref.getContext());
            setListPrefSummary(pref, language);

            // bind it to change listener
            pref.setOnPreferenceChangeListener(sGeneralPrefsListener);


            // Set numerals summary to current user setting
            pref = findPreference("general_numerals");
            setListPrefSummary(pref, UserSettings.getNumerals(pref.getContext()));

            // bind it to change listener
            pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
            // Numerals pref. available only when language is arabic.
            if (UserSettings.languageIsArabic(getActivity(), language)) {
                pref.setEnabled(true);
            } else {
                pref.setEnabled(false);
            }
        }

        private /*static*/ Preference.OnPreferenceChangeListener sGeneralPrefsListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                String key = preference.getKey();
                Context context = preference.getContext();

                switch (key) {
                    case "general_language":
                        // Set the summary to reflect the new value.
                        setListPrefSummary(preference, stringValue);

                        // Change locale?
                        if (!stringValue.equals(UserSettings.getPrefLanguage(context))) {
                            Timber.d("New language: " + stringValue);
                            UserSettings.setLocale(context, stringValue, null);

                            // numerals pref. only ON for arabic.
                            Preference numeralsPref = findPreference("general_numerals");
                            if (UserSettings.languageIsArabic(getActivity(), stringValue)) {
                                numeralsPref.setEnabled(true);
                            }
                            else {
                                numeralsPref.setEnabled(false);
                            }

                            refreshUI(context);
                        }
                    break;

                    case  "general_numerals":
                        // Set the summary to reflect the new value.
                        setListPrefSummary(preference, stringValue);

                        // Change locale?
                        if (!stringValue.equals(UserSettings.getNumerals(context))) {
                            Timber.d("New numerals: " + stringValue);
                            UserSettings.setLocale(context, null, stringValue);
                            refreshUI(context);
                        }
                    break;

                default:
                    // For other preferences, set the summary to the value's
                    // simple string representation.
                    //preference.setSummary(stringValue);
                    break;
                }

                return true;
            }

            private void refreshUI(Context context) {
                // refresh UI (framework part) with new Locale
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocationsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_locations);

            // Set city summary to current user setting
            Preference pref = findPreference("locations_search_city");
            Context context = pref.getContext();
            pref.setSummary(UserSettings.getCityName(context));

            // bind on click listener to start SearchCityActivity
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Timber.d("onPrefClick");
                        startActivityForResult(preference.getIntent(), REQUEST_SEARCH_CITY);
                        return true;
                    }
                   });


            // Set method summary to current user setting
            pref = findPreference("locations_method");
            int method = UserSettings.getCalculationMethod(context);
            setListPrefSummary(pref, String.valueOf(method));
            // Bind to onchange listener
            pref.setOnPreferenceChangeListener(sMethodChangeListener);


            // Bind to change listener
            pref = findPreference("locations_rounding");
            pref.setOnPreferenceChangeListener(sMethodChangeListener);


            // Bind mathhab pref to its change listener
            pref = findPreference("locations_mathhab_hanafi");
            pref.setOnPreferenceChangeListener(sMethodChangeListener);
            // Mathhab hanafi pref. only for Karachi method.
            if (method == Method.V2_KARACHI) {
                pref.setEnabled(true);
            } else {
                pref.setEnabled(false);
            }
        }

        private /*static*/ Preference.OnPreferenceChangeListener sMethodChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                Context context = preference.getContext();

                switch (preference.getKey()) {
                    case "locations_method":
                        // Set the summary to reflect the new value.
                        int index = setListPrefSummary(preference, stringValue);

                        // Trigger new calc if value change
                        index += Method.V2_MWL;
                        int oldMethodIdx = UserSettings.getCalculationMethod(context);
                        if (oldMethodIdx != index) {
                            Timber.d("New calc method: " + index);

                            // Mathhab hanafi pref. only for Karachi method.
                            Preference mathhabPref = findPreference("locations_mathhab_hanafi");
                            if (index == Method.V2_KARACHI) {
                                mathhabPref.setEnabled(true);
                            } else {
                                mathhabPref.setEnabled(false);
                            }

                            PrayerTimesManager.handleLocationChange(context, index, -1, -1);
                        }
                        break;
                    case "locations_rounding":
                        // Trigger new calc if value change
                        int oldRound = UserSettings.getRounding(context);
                        int newRound = stringValue.equals("true") ? 1 : 0;
                        if (oldRound != newRound) {
                            PrayerTimesManager.handleLocationChange(context, -1, newRound, -1);
                        }
                        break;
                    case "locations_mathhab_hanafi":
                        // Trigger new calc if value change
                        boolean oldMathhab = UserSettings.isMathhabHanafi(context);
                        boolean newMathhab = stringValue.equals("true");
                        if (oldMathhab != newMathhab) {
                            PrayerTimesManager.handleLocationChange(context, -1, -1, newMathhab ? 2 : 1);
                        }
                        break;
                    default:
                        // For other preferences, set the summary to the value's
                        // simple string representation.
                        //preference.setSummary(stringValue);
                        break;
                }
                return true;
            }
        };

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Timber.d("onActivityResult");
            if (requestCode == REQUEST_SEARCH_CITY) {
                if(resultCode == Activity.RESULT_OK){
                    Preference pref = findPreference("locations_search_city");
                    pref.setSummary(data.getStringExtra("name"));

                    // Main UI will be refreshed automatically by it's OnResume

                    PrayerTimesManager.handleLocationChange(getActivity(), -1, -1, -1);
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

            // Bind prayer time pref to its change listener
            Preference pref = findPreference("notifications_prayer_time");
            pref.setOnPreferenceChangeListener(sNotifPrayerTimeListener);

            // Bind muezzin to its change listener
            pref = findPreference("notifications_muezzin");
            pref.setOnPreferenceChangeListener(sMuezzinChangeListener);

            // Set summary to current value
            setListPrefSummary(pref, UserSettings.getMuezzin(pref.getContext()));
        }

        private static Preference.OnPreferenceChangeListener sNotifPrayerTimeListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Timber.d("sNotifPrayerTimeListener: " + newValue.toString());
                if (newValue.toString().equals("true")) {
                    PrayerTimesManager.enableAlarm(preference.getContext());
                }
                else {
                    PrayerTimesManager.disableAlarm(preference.getContext());
                }
                return true;
            }
        };

        private static Preference.OnPreferenceChangeListener sMuezzinChangeListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String stringValue = newValue.toString();
                final Context context = preference.getContext();
                final ListPreference listPref = (ListPreference) preference;
                int index = listPref.findIndexOfValue(stringValue);
                final String name = index >= 0 ? listPref.getEntries()[index].toString() : "";

                Timber.d("sMuezzinChangeListener: " + name);

                // start Athan Audio
                Intent audioIntent = new Intent(context, AthanService.class);
                audioIntent.putExtra(AthanService.EXTRA_PRAYER, 2);
                audioIntent.putExtra(AthanService.EXTRA_MUEZZIN, stringValue);
                context.startService(audioIntent);

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.select_muezzin, name))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                listPref.setValue(stringValue);
                                listPref.setSummary(name);
                                UserSettings.setMuezzin(context, stringValue);
                                stopAthan(context);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                stopAthan(context);
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                stopAthan(context);
                            }
                        })
                        .create()
                        .show();

                return false;
            }

            private void stopAthan(Context context) {
                // stop athan audio
                Intent stopIntent = new Intent(context, AthanService.class);
                context.stopService(stopIntent);
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
