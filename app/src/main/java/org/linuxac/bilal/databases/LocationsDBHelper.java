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
    private static final String DATABASE_NAME = "locations.db";
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

    public List<City> searchCity(String nameEn)
    {
        if (null == mDatabase) {
            Log.w(TAG, "Open database first!");
            return null;
        }

        String query =
                "SELECT locations.id as ID,\n" +
                        "\t   locations.nameEn as en,\n" +
                        "\t   countries.nameEN as c_en,\n" +
                        "\t   countries.regionEN as r_en\n" +
                        "\t   locations.latitude,\n" +
                        "\t   locations.longitude,\n" +
                        "FROM   locations, \n" +
                        "\t   countries\n" +
                        "WHERE  locations.id_contry = countries.id and\n" +
                        "\t   en like '%" + nameEn + "%'";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getFloat(4),
                    cursor.getFloat(5),
                    0           // TODO
            ));
        }
        cursor.close();
        Log.d(TAG, "cityList:\n" + cityList);

        return cityList;
    }

    public List<City> searchCity(double lat, double lng)
    {
        if (null == mDatabase) {
            Log.w(TAG, "Open database first!");
            return null;
        }

        String query =  "SELECT locations.id as ID,\n" +
                        "\t   locations.nameEn as en,\n" +
                        "\t   countries.nameEN as c_en,\n" +
                        "\t   countries.regionEN as r_en\n" +
                        "\t   locations.latitude,\n" +
                        "\t   locations.longitude,\n" +
                        "FROM  locations, \n" +
                        "\t    countries\n" +
                        "WHERE locations.id_contry = countries.id and\n" +
                        "\t    locations.latitude between " + (lat - 0.08) + " and " + (lat + 0.08) + " and\n" +
                        "\t    locations.longitude between " + (lng - 0.08) + " and " + (lng + 0.08);

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<City> cityList = new ArrayList<>();
        while (cursor.moveToNext()) {
            cityList.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getFloat(4),
                    cursor.getFloat(5),
                    0           // TODO
            ));
        }
        cursor.close();
        Log.d(TAG, "cityList:\n" + cityList);

        return cityList;
    }

    public List<String> getAllRegions()
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        Cursor cursor = mDatabase.rawQuery("SELECT regionEN FROM countries", null);

        List<String> regions = new ArrayList<>();
        while (cursor.moveToNext()) {
            regions.add(cursor.getString(0));
        }
        Log.d(TAG, "regions:\n" + regions);

        return regions;
    }

    public List<String> getCountries(String region)
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        Cursor cursor = mDatabase.rawQuery(
                "SELECT nameEN FROM countries WHERE regionEN = ?", new String[] {region});

        List<String> countries = new ArrayList<>();
        while (cursor.moveToNext()) {
            countries.add(cursor.getString(0));
        }
        Log.d(TAG, "regions:\n" + countries);

        return countries;
    }

    public List<City> getCities(String country)
    {
        if (null == mDatabase) {
            Log.d(TAG, "Open DB first!");
            return null;
        }

        String query =
                "SELECT locations.id as ID, " +
                "\t   locations.nameEn as en,\n" +
                "\t   countries.nameEN as c_en,\n" +
                "\t   countries.regionEN as r_en\n" +
                "\t   locations.latitude,\n" +
                "\t   locations.longitude,\n" + // TODO alt
                "FROM  locations, countries " +
                "WHERE locations.id_contry = countries.id and " +
                "      countries.nameEN like '%" + country + "%'";

        Cursor cursor = mDatabase.rawQuery(query, null);

        List<City> cities = new ArrayList<>();
        while (cursor.moveToNext()) {
            cities.add(new City(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getFloat(4),
                    cursor.getFloat(5),
                    0           // TODO
            ));
        }
        Log.d(TAG, "regions:\n" + cities);

        return cities;
    }
}
