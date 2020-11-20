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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.djalel.android.bilal.PrayerTimesApp;
import com.djalel.android.bilal.R;
import com.djalel.android.bilal.adapters.CityListAdapter;
import com.djalel.android.bilal.databases.LocationsDBHelper;
import com.djalel.android.bilal.datamodels.City;
import com.djalel.android.bilal.helpers.UserSettings;

import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class SearchCityActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    private LocationsDBHelper mDBHelper;
    private CityListAdapter mCityListAdapter;
    private ListView mCityListView;
    private String mLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);
        setTitle(getString(R.string.app_name));

        mDBHelper = new LocationsDBHelper(this);
        mDBHelper.openReadable();

        mLanguage = UserSettings.getLanguage(this).toUpperCase(Locale.ENGLISH);

        SearchView searchView = findViewById(R.id.search_city_box);
        searchView.setOnQueryTextListener(this);
        searchView.setIconified(false);
        searchView.setQueryHint(getString(R.string.search_city));      // XML tag has a bug

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        Timber.i("onCreate: intent.action = " + intent.getAction());
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Timber.i("onNewIntent: intent.action = " + intent.getAction());
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // update locale in case user changed it meanwhile
        mLanguage = UserSettings.getLanguage(this).toUpperCase(Locale.ENGLISH);
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        Timber.i("onQueryTextSubmit: " + query);
        if (null != query && !query.isEmpty()) {
            searchCity(query);
        }
        else {
            if (null != mCityListView) {
                mCityListView.setAdapter(null);
            }
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Timber.d("onQueryTextChange: " + newText);
        if (null != newText && !newText.isEmpty()) {
            searchCity(newText);
        }
        else {
            if (null != mCityListView) {
                mCityListView.setAdapter(null);
            }
        }
        return true;
    }

    private void handleIntent(Intent intent)
    {
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            searchCity(query);
        }
        else if (Intent.ACTION_VIEW.equals(action)) {
            if (null != mCityListView) {        // useless when called from onCreate!
                mCityListView.setAdapter(null);
            }
        }
    }

    private void searchCity(String query)
    {
        List<City> cityList = mDBHelper.searchCity(query, mLanguage);

        if (cityList != null){
            mCityListAdapter = new CityListAdapter(this, cityList);
            mCityListView = findViewById(R.id.list_city);
            mCityListView.setAdapter(mCityListAdapter);
            mCityListView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        City newCity = (City) mCityListAdapter.getItem(i);
        // get city name in Locale as search could be in EN
        newCity = mDBHelper.getCity(newCity.getId(), mLanguage);
        Timber.i("Selected city: " + newCity);

        City oldCity = UserSettings.getCity(this);
        if (null == oldCity || oldCity.getId() != newCity.getId()) {
            // save new city
            UserSettings.setCity(this, newCity);

            // adapt preference summary
            Intent resultIntent = new Intent();
            resultIntent.putExtra("name", newCity.getName());
            setResult(Activity.RESULT_OK, resultIntent);
        }
        finish();
    }

    @Override
    public void onDestroy()
    {
        if (null != mDBHelper) {
            mDBHelper.close();
            mDBHelper = null;
        }
        super.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context newBase)
    {
        super.attachBaseContext(updateResources(newBase));
    }

    private static Context updateResources(Context context)
    {
        Locale locale = PrayerTimesApp.getApplication().getLocale();
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }
}
