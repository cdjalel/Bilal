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

package com.djalel.android.bilal.helpers;

import android.content.Context;

import org.arabeyes.prayertime.Prayer;
import com.djalel.android.bilal.BuildConfig;
import bilal.linuxac.bilal.R;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;


public class PrayerTimes {
    private static final String TAG = "PrayerTimes";
    private GregorianCalendar[] all;
    private GregorianCalendar current;
    private GregorianCalendar next;
    private int c;                          // current prayer index
    private int n;                          // next prayer index
    private boolean rounded;

    public PrayerTimes(GregorianCalendar now, GregorianCalendar[] all, boolean rounded) {
        this.all = all;
        this.rounded = rounded;
        findCurrent(now);
    }

    public void updateCurrent(GregorianCalendar now)
    {
        findCurrent(now);
    }

    public int getCurrentIndex() {
        return c;
    }

    public int getNextIndex() {
        return n;
    }

    public GregorianCalendar getCurrent() {
        return current;
    }

    public GregorianCalendar getNext() {
        return next;
    }

    public String getCurrentName(Context context) {return getName(context, c); }

    public String getNextName(Context context) {return getName(context, n); }

    public static String getName(Context context, int prayer)
    {
        int prayerNameResId = 0;
        switch (prayer) {
            case Prayer.NB_PRAYERS:
                // use "fajr" instead of "next fajr"
                // FALLTHROUGH
            case 0:
                prayerNameResId = R.string.fajr;
                break;
            case 1:
                prayerNameResId = R.string.shuruk;
                break;
            case 2:
                prayerNameResId = R.string.dhuhr;
                break;
            case 3:
                prayerNameResId = R.string.asr;
                break;
            case 4:
                prayerNameResId = R.string.maghrib;
                break;
            case 5:
                prayerNameResId = R.string.isha;
                break;
            default:
                if (BuildConfig.DEBUG) {
                    //Log.e(TAG, "Invalid prayer index: " + prayer);
                    return "";
                }
                break;
        }

        return context.getString(prayerNameResId) ;
    }

    public static String format(GregorianCalendar cal, int round)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(
            round != 0 ? "HH:mm" : "HH:mm:ss", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    public String format(GregorianCalendar cal)
    {
        return format(cal, rounded? 1:0);
    }

    public String format(int i)
    {
        if (BuildConfig.DEBUG  && (i < 0 || i >= all.length || null == all)) {
            //Log.w(TAG, "index out of range or prayers array is null");
            return null;
        }
        return format(all[i], rounded? 1:0);
    }

    private void findCurrent(GregorianCalendar now)
    {
        // Find current and next prayers
        for (n = 0; n < Prayer.NB_PRAYERS; n++) {
            if (now.before(all[n])) {
                break;
            }
        }

        switch (n) {
            case 0:
                c = 0;
                break;
            case 1:
                // sunset is skipped.
                n = 2;
                // FALLTHROUGH
            case 2:
                c = 0;
                break;
            case 3:
            case 4:
            case 5:
            case Prayer.NB_PRAYERS:
            default:
                c = n - 1;
                break;
        }
        current = all[c];
        next = all[n];
    }
}
