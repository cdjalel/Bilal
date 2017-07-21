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

import android.app.SearchManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.linuxac.bilal.BuildConfig;
import org.linuxac.bilal.R;
import org.linuxac.bilal.adapters.CityListAdapter;
import org.linuxac.bilal.databases.LocationsDBHelper;
import org.linuxac.bilal.datamodels.City;

import java.util.List;

public class SearchCityActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    private static String TAG = "SearchCityActivity";
    private LocationsDBHelper mDBHelper; // TODO: static?
    private CityListAdapter mCityListAdapter;
    private ListView mCityListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        mDBHelper = new LocationsDBHelper(this);
        mDBHelper.openReadable();

        SearchView searchView = (SearchView) findViewById(R.id.search_city_box);
        searchView.setOnQueryTextListener(this);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        Log.i(TAG, "onCreate: intent.action = " + intent.getAction());
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent: intent.action = " + intent.getAction());
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, "onQueryTextSubmit: " + query);
        if (null != query && !query.isEmpty()) {
            searchCity(query);
        }
        else {
            mCityListView.setAdapter(null);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.i(TAG, "onQueryTextChange: " + query);
        if (null != query && !query.isEmpty()) {
            searchCity(query);
        }
        else {
            mCityListView.setAdapter(null);
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
            mCityListView.setAdapter(null);
        }
    }

    private void searchCity(String query)
    {
        List<City> cityList = mDBHelper.searchCity(query);

        if (cityList != null){
            // TODO: what happens to old adapter &  listview content?
            mCityListAdapter = new CityListAdapter(this , cityList);
            mCityListView = (ListView) findViewById(R.id.list_city);
            mCityListView.setAdapter(mCityListAdapter);
            mCityListView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        City item = (City) mCityListAdapter.getItem(i);
        Log.i(TAG, "onItemClick city: " + item);

        // save new value // TODO check if new is available changeCalculatonMethod()
        UserSettings.setCityID(this, item.getID());

        // adapt preference summary
        Intent resultIntent = new Intent();// TODO getIntent();
        resultIntent.putExtra("name", item.getName());
        //resultIntent.putExtra("id", item.getID());
        setResult(Activity.RESULT_OK, resultIntent);
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
}
