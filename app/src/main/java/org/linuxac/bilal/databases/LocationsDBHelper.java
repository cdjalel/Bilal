/*
 *  Copyright © 2017 Djalel Chefrour
 *  Copyright © 2017 Mouadh Bekhouche
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
 *  Superclass SQLiteAssetHelper is:
 *  Copyright © 2011 readyState Software Ltd
 *  Copyright © 2007 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.linuxac.bilal.databases;

import org.linuxac.bilal.datamodels.City;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class LocationsDBHelper extends SQLiteAssetHelper {
    private static final String TAG = "LocationsDBHelper";
    private static final String DATABASE_NAME = "locations.db3";
    private static final int DATABASE_VERSION = 1;
    private static final int DATABASE_CLOSED = -1;
    // Add tables and keys names

    private SQLiteDatabase mDatabase;
    private int mOpenMode = -1;

    public LocationsDBHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mOpenMode = DATABASE_CLOSED;
    }

    public void openReadable()
    {
        switch (mOpenMode) {
            case SQLiteDatabase.OPEN_READONLY:
                Log.d(TAG, "DB already open readonly!");
                break;

            case SQLiteDatabase.OPEN_READWRITE:
                mDatabase.close();
                // FALLTHROUGH

            case DATABASE_CLOSED:
            default:
                mOpenMode = SQLiteDatabase.OPEN_READONLY;
                mDatabase = this.getReadableDatabase();
                break;
        }
    }

    public void openWritable()
    {
        switch (mOpenMode) {
            case SQLiteDatabase.OPEN_READWRITE:
                Log.d(TAG, "DB already open readwrite!");
                break;

            case SQLiteDatabase.OPEN_READONLY:
                mDatabase.close();
                // FALLTHROUGH

            case DATABASE_CLOSED:
            default:
                mOpenMode = SQLiteDatabase.OPEN_READWRITE;
                mDatabase = this.getWritableDatabase();
                break;
        }
    }

    public void close()
    {
        if (null == mDatabase) {
            Log.d(TAG, "DB already closed!");
            return;
        }
        mOpenMode = DATABASE_CLOSED;
        mDatabase.close();
        mDatabase = null;
    }

    // called only when other settings affecting DB (e.g., language) changes
    public City getCity(int id, String language)
    {
        if (-1 >= id) {
            Log.e(TAG, "Bad city id: " + id);
            return null;
        }

        String query =
                "SELECT " +
                    "cities.cityId as id, " +
                    "cities.name"+language + ", " +
                    "countries.name"+language + ", " +
                    "timezones.name"+language + ", " +
                    "timezones.nameEN, " +          // used for PT calculations only
                    "cities.latitude, " +
                    "cities.longitude, " +
                    "cities.altitude " +
                "FROM cities " +
                "INNER JOIN countries ON cities.countryCode = countries.countryCode " +
                "INNER JOIN timezones ON timezones.tzId = cities.tzId " +
                "WHERE id = ?";
        Log.d(TAG, query);

        openReadable();
        Cursor cursor = mDatabase.rawQuery(query, new String[] {String.valueOf(id)});

        City city = null;
        if (cursor.moveToNext()) {
            city = new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getFloat(5),
                    cursor.getFloat(6),
                    cursor.getInt(7)
                );
        }
        cursor.close();
        close();
        Log.d(TAG, "city:\n" + city);

        return city;
    }

    // Remaining methods are called only for city search
    public List<City> searchCity(String city, String language)
    {
        if (null == mDatabase) {
            Log.w(TAG, "Open database first!");
            return null;
        }

        String query =
                "SELECT " +
                    "cities.cityId, " +
                    "cities.name"+language + " as name, " +
                    "countries.name"+language + ", " +
                    "timezones.name"+language + ", " +
                    "timezones.nameEN, " +          // used for PT calculations only
                    "cities.latitude, " +
                    "cities.longitude, " +
                    "cities.altitude " +
                "FROM cities " +
                "INNER JOIN countries ON cities.countryCode = countries.countryCode " +
                "INNER JOIN timezones ON timezones.tzId = cities.tzId " +
                "WHERE name LIKE ?";

        Log.d(TAG, query);
        Cursor cursor = mDatabase.rawQuery(query, new String[] {"%"+city+"%"});

        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getFloat(5),
                    cursor.getFloat(6),
                    cursor.getInt(7)
            ));
        }
        cursor.close();
        Log.d(TAG, "cityList:\n" + cityList);

        return cityList;
    }

    public List<City> searchCity(double lat, double lng, String language)
    {
        if (null == mDatabase) {
            Log.w(TAG, "Open database first!");
            return null;
        }

        String query =
                "SELECT " +
                    "cities.cityId, " +
                    "cities.name"+language + ", " +
                    "countries.name"+language + ", " +
                    "timezones.name"+language + ", " +
                    "timezones.nameEN, " +          // used for PT calculations only
                    "cities.latitude as lat, " +
                    "cities.longitude, " +
                    "cities.altitude " +
                "FROM cities " +
                "INNER JOIN countries ON cities.countryCode = countries.countryCode " +
                "INNER JOIN timezones ON timezones.tzId = cities.tzId " +
                "WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ?";
        Log.d(TAG, query);

        Cursor cursor = mDatabase.rawQuery(query, new String[] {
            String.valueOf(lat - 0.08), String.valueOf(lat + 0.08),
            String.valueOf(lng - 0.08), String.valueOf(lng + 0.08)});

        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getFloat(5),
                    cursor.getFloat(6),
                    cursor.getInt(7)
            ));
        }
        cursor.close();
        Log.d(TAG, "cityList:\n" + cityList);

        return cityList;
    }

    public List<String> getAllTimezones(String language)
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        String query = "SELECT name"+language + " FROM timezones";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<String> timezones = new ArrayList<>();
        while (cursor.moveToNext()) {
            timezones.add(cursor.getString(0));
        }
        cursor.close();
        Log.d(TAG, "timezones:\n" + timezones);

        return timezones;
    }

    public List<String> getAllCountries(String language)
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        String query = "SELECT name"+language + " FROM countries";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<String> countries = new ArrayList<>();
        while (cursor.moveToNext()) {
            countries.add(cursor.getString(0));
        }
        cursor.close();
        Log.d(TAG, "countries:\n" + countries);

        return countries;
    }

    public List<City> getCities(String country, String language)
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        String query =
                "SELECT " +
                    "cities.cityId, " +
                    "cities.name"+language + ", " +
                    "countries.name"+language + " as name, " +
                    "timezones.name"+language + ", " +
                    "timezones.nameEN, " +          // used for PT calculations only
                    "cities.latitude, " +
                    "cities.longitude, " +
                    "cities.altitude " +
                "FROM cities" +
                "INNER JOIN countries ON cities.countryCode = countries.countryCode " +
                "INNER JOIN timezones ON timezones.tzId = cities.tzId " +
                "WHERE name LIKE ?";

        Cursor cursor = mDatabase.rawQuery(query, new String[] {"%"+country+"%"});

        List<City> cities = new ArrayList<>();
        while (cursor.moveToNext()) {
            cities.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getFloat(5),
                    cursor.getFloat(6),
                    cursor.getInt(7)
            ));
        }
        cursor.close();
        Log.d(TAG, "cities:\n" + cities);

        return cities;
    }
}
