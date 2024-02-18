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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;

import com.djalel.android.bilal.PrayerTimesApp;
import com.djalel.android.bilal.PrayerTimesManager;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.helpers.PermissionsDialog;
import com.djalel.android.bilal.helpers.UserSettings;
import com.djalel.android.bilal.helpers.WakeLocker;
import com.djalel.android.bilal.services.AthanService;

import org.arabeyes.prayertime.Method;

import java.util.Objects;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    private static PermissionsDialog mPermissionsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> {
                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        setTitle(R.string.title_activity_settings);
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPermissionsDialog = new PermissionsDialog(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle back arrow click
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Navigate back to main activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                Objects.requireNonNull(pref.getFragment()));
        fragment.setArguments(args);
        //fragment.setTargetFragment(caller, 0);
        //getSupportFragmentManager().setFragmentResultListener("request Key", fragment.getViewLifecycleOwner(), (requestKey, result) -> { });
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    /*
    private static int setListPrefSummary(android.preference.Preference pref, String value) {
        android.preference.ListPreference listPref = (android.preference.ListPreference) pref;
        int index = listPref.findIndexOfValue(value);
        listPref.setSummary(index >= 0 ? listPref.getEntries()[index] : null);
        return index;
    }
    */

    public static class GeneralFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);
            String language;
            Preference pref = findPreference("general_language");
            if (pref != null) {
                language = UserSettings.getPrefLanguage(pref.getContext());
                // Set language summary to current user setting
                //setListPrefSummary(pref, language);

                // bind it to change listener
                pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
            }
            else {
                Timber.e("No such preference general_language!");
                language = "Arabic";
                //return;
            }

            // Set numerals summary to current user setting
            pref = findPreference("general_numerals");
            if (pref != null) {
                //setListPrefSummary(pref, UserSettings.getNumerals(pref.getContext()));
                // bind it to change listener
                pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
                // Numerals pref. available only when language is arabic.
                pref.setEnabled(UserSettings.languageIsArabic(getActivity(), language));
            }
            else {
                Timber.e("No such preference general_numerals!");
                //return;
            }

            // Bind to change listener
            pref = findPreference("general_rounding");
            if (pref != null) {
                pref.setOnPreferenceChangeListener(sGeneralPrefsListener);
            }
            else {
                Timber.e("No such preference general_rounding!");
            }
        }

        private final /*static*/ Preference.OnPreferenceChangeListener sGeneralPrefsListener =
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        String stringValue = value.toString();
                        String key = preference.getKey();
                        Context context = preference.getContext();

                        switch (key) {
                            case "general_language":
                                // Set the summary to reflect the new value.
                                //setListPrefSummary(preference, stringValue);

                                // Change locale?
                                if (!stringValue.equals(UserSettings.getPrefLanguage(context))) {
                                    Timber.d("New language: %s", stringValue);
                                    UserSettings.setLocale(context, stringValue, null);

                                    // numerals pref. only ON for arabic.
                                    Preference numeralsPref = findPreference("general_numerals");
                                    if (numeralsPref == null) return true;
                                    numeralsPref.setEnabled(UserSettings.languageIsArabic(getActivity(), stringValue));
                                    refreshUI(context);
                                }
                                break;

                            case  "general_numerals":
                                // Set the summary to reflect the new value.
                                //setListPrefSummary(preference, stringValue);

                                // Change locale?
                                if (!stringValue.equals(UserSettings.getNumerals(context))) {
                                    Timber.d("New numerals: %s", stringValue);
                                    UserSettings.setLocale(context, null, stringValue);
                                    refreshUI(context);
                                }
                                break;

                            case "general_rounding":
                                // Trigger new calc if value change
                                int oldRound = UserSettings.getRounding(context);
                                int newRound = stringValue.equals("true") ? 1 : 0;
                                if (oldRound != newRound) {
                                    PrayerTimesManager.handleSettingsChange(context, -1, newRound, -1);
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

    public static class LocationFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.location_preferences, rootKey);

            Preference pref = findPreference("locations_search_city");
            if (pref != null) {
                // Set city summary to current user setting
                //pref.setSummary(UserSettings.getCityName(pref.getContext()));

                // bind on click listener to start SearchCityActivity
                pref.setOnPreferenceClickListener(preference -> {
                    Timber.d("onPreferenceClick");
                    //startActivityForResult(preference.getIntent(), REQUEST_SEARCH_CITY);
                    Intent intent = new Intent(getContext(), SearchCityActivity.class);
                    preference.setIntent(intent);
                    return true;
                });
            }
            else {
                Timber.e("No such preference locations_search_city!");
            }

            // Set method summary to current user setting
            pref = findPreference("locations_method");
            int method;
            if (pref != null) {
                method = UserSettings.getCalculationMethod(pref.getContext());
                //setListPrefSummary(pref, String.valueOf(method));
                // Bind to onchange listener
                pref.setOnPreferenceChangeListener(sMethodChangeListener);
            }
            else {
                Timber.e("No such preference locations_method!");
                method = 10;
            }

            pref = findPreference("locations_mathhab_hanafi");
            if (pref != null) {
                // Bind mathhab pref to its change listener
                pref.setOnPreferenceChangeListener(sMethodChangeListener);
                // Mathhab hanafi pref. only for Karachi method.
                pref.setEnabled(method == Method.V2_KARACHI);
            }
            else {
                Timber.e("No such preference locations_mathhab_hanafi!");
            }
        }

        private final /*static*/ Preference.OnPreferenceChangeListener sMethodChangeListener =
                (preference, value) -> {
                    String stringValue = value.toString();
                    Context context = preference.getContext();

                    switch (preference.getKey()) {
                        case "locations_method":
                            // Set the summary to reflect the new value.
                            //int index = setListPrefSummary(preference, stringValue);
                            //index += Method.V2_MWL;

                            // Trigger new calc if value change
                            int index = Integer.parseInt(stringValue);
                            int oldMethodIdx = UserSettings.getCalculationMethod(context);
                            if (oldMethodIdx != index) {
                                Timber.d("New calc method: %s", index);

                                // Mathhab hanafi pref. only for Karachi method.
                                Preference mathhabPref = findPreference("locations_mathhab_hanafi");
                                if (mathhabPref != null) { mathhabPref.setEnabled(index == Method.V2_KARACHI); }

                                PrayerTimesManager.handleSettingsChange(context, index, -1, -1);
                            }
                            break;
                        case "locations_mathhab_hanafi":
                            // Trigger new calc if value change
                            boolean oldMathhab = UserSettings.isMathhabHanafi(context);
                            boolean newMathhab = stringValue.equals("true");
                            if (oldMathhab != newMathhab) {
                                PrayerTimesManager.handleSettingsChange(context, -1, -1, newMathhab ? 2 : 1);
                            }
                            break;
                        default:
                            // For other preferences, set the summary to the value's
                            // simple string representation.
                            //preference.setSummary(stringValue);
                            break;
                    }
                    return true;
                };
    }

    public static class NotificationsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Inflate preference XML for SDK version 23 (M) or higher
                setPreferencesFromResource(R.xml.notifications_preferences_v23, rootKey);
            } else {
                // Inflate preference XML for SDK version below 21
                setPreferencesFromResource(R.xml.notifications_preferences_legacy, rootKey);
            }

            // Bind prayer time pref to its change listener
            Preference pref = findPreference("notifications_prayer_time");
            if (pref != null) {
                pref.setOnPreferenceChangeListener(sNotifPrayerTimeListener);
            }
            else {
                Timber.e("No such preference notifications_prayer_time!");
            }

            // Bind muezzin to its change listener
            pref = findPreference("notifications_muezzin");
            if (pref != null) {
                pref.setOnPreferenceChangeListener(sMuezzinChangeListener);
            }
            else {
                Timber.e("No such preference notifications_muezzin!");
                // Set summary to current value
                //setListPrefSummary(pref, UserSettings.getMuezzin(pref.getContext()));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pref = findPreference("notifications_permissions");
                if (pref != null) {
                    pref.setOnPreferenceClickListener(preference -> {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        if (null!= activity) {
                            mPermissionsDialog.showPermissionsDialog();
                        }
                        return true;
                    });
                }
                else {
                    Timber.e("No such preference notifications_permissions!");
                }
            }
        }

        private static final Preference.OnPreferenceChangeListener sNotifPrayerTimeListener =
                (preference, newValue) -> {
                    Timber.d("sNotifPrayerTimeListener: %s", newValue.toString());
                    if (newValue.toString().equals("true")) {
                        PrayerTimesManager.enableAlarm(preference.getContext());
                    }
                    else {
                        PrayerTimesManager.disableAlarm(preference.getContext());
                    }
                    return true;
                };
        private static final Preference.OnPreferenceChangeListener sMuezzinChangeListener =
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String stringValue = newValue.toString();
                        final Context context = preference.getContext();
                        final ListPreference listPref = (ListPreference) preference;
                        int index = listPref.findIndexOfValue(stringValue);
                        final String name = index >= 0 ? listPref.getEntries()[index].toString() : "";

                        Timber.d("sMuezzinChangeListener: %s", name);
                        playAthan(context, stringValue);

                        // Use the Builder class for convenient dialog construction
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(context.getString(R.string.select_muezzin, name))
                                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                    listPref.setValue(stringValue);
                                    UserSettings.setMuezzin(context, stringValue);
                                    stopAthan(context);
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, id) -> stopAthan(context))
                                .setOnDismissListener(dialog -> stopAthan(context))
                                .create()
                                .show();

                        return false;
                    }

                    private void playAthan(Context context, String stringValue) {
                        // start Athan Audio
                        WakeLocker.acquire(context);
                        Intent playIntent = new Intent(context, AthanService.class);
                        playIntent.setAction(AthanService.ACTION_PLAY_ATHAN);
                        playIntent.putExtra(AthanService.EXTRA_PRAYER, 2);
                        playIntent.putExtra(AthanService.EXTRA_MUEZZIN, stringValue);
                        context.startService(playIntent);
                    }

                    private void stopAthan(Context context) {
                        AthanService.stopAthanAction(context);
                    }
                };
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(PrayerTimesApp.updateLocale(newBase));
    }
}
