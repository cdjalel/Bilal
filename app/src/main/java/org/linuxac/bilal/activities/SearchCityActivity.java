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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.linuxac.bilal.R;
import org.linuxac.bilal.adapters.CityListAdapter;
import org.linuxac.bilal.databases.LocationsDBHelper;
import org.linuxac.bilal.datamodels.City;

import java.util.List;

public class SearchCityActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener {

    private CityListAdapter mCityListAdapter;
    private ListView mCityListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            searchCity(query);
        }
        else if (Intent.ACTION_VIEW.equals(action)) {
            // TODO? reset search box and list content
        }
    }

    private void searchCity(String query)
    {
        LocationsDBHelper dbHelper = new LocationsDBHelper(this);

        List<City> cityList = dbHelper.searchCity(query);

        if (cityList != null){
            // TODO: what happens to old adapter & old listview content?
            mCityListAdapter = new CityListAdapter(this , cityList);
            mCityListView = (ListView) findViewById(R.id.list_city);
            mCityListView.setAdapter(mCityListAdapter);
            mCityListView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        City item = (City) mCityListAdapter.getItem(i);
        // mPreference.setCity(item); // TODO change User Setting
        // TODO adapt setting view preference summary pref_search_city somehow
        // setResult(321); TODO rm
        finish();
    }
}
