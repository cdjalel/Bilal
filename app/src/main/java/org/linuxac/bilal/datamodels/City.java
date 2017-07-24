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
 */

package org.linuxac.bilal.datamodels;

import java.io.Serializable;

public class City implements Serializable {

    private int id;
    private String name;
    private String country;
    private String timezone;
    private float latitude;
    private float longitude;
    private float altitude;

    public City(int i, String n, String c, String tz, float lat, float lon, float alt) {
        id = i;
        name = n;
        country = c;
        timezone = tz;
        latitude = lat;
        longitude = lon;
        altitude = alt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getCountry() {
        return country;
    }

    public String getTimezone() {
        return timezone;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getAltitude() {
        return altitude;
    }

    @Override
    public String toString() {
        return toLongString();
    }

    public String toShortString() {
        return name + " (" + country + ")";
    }

    public String toLongString() {
        return name + " (" + timezone + ")";
    }
}
