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
import com.djalel.android.bilal.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class PrayerTimes {
    private final GregorianCalendar[] all;
    private GregorianCalendar current;
    private GregorianCalendar next;
    private int c;                          // current prayer index
    private int n;                          // next prayer index
    private final boolean rounded;

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

    public String getCurrentName(Context context) {return getName(context, c, current); }

    public String getNextName(Context context) {return getName(context, n, next); }

    public static String getName(Context context, int prayer, GregorianCalendar cal)
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
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    prayerNameResId = R.string.jumu3a;
                }
                else {
                    prayerNameResId = R.string.dhuhr;
                }
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
                break;
        }

        return context.getString(prayerNameResId) ;
    }

    public static String formatPrayerTime(GregorianCalendar cal, int round)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(
            round != 0 ? "HH:mm" : "HH:mm:ss", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    public String formatPrayerTime(GregorianCalendar cal)
    {
        return formatPrayerTime(cal, rounded? 1:0);
    }

    public String formatPrayerTime(int i)
    {
        if (i < 0 || null == all || i >= all.length) {
            Timber.e("index out of range or prayers array is null");
            return null;
        }
        return formatPrayerTime(all[i], rounded? 1:0);
    }

    public String formatTimeToNextPrayer(Context context, GregorianCalendar from)
    {
        return formatTimeInterval(context, from, next);
    }

    public String formatTimeFromCurrentPrayer(Context context, GregorianCalendar to)
    {
        return formatTimeInterval(context, current, to);
    }

    private String formatTimeInterval(Context context, GregorianCalendar from, GregorianCalendar to)
    {
        // assert to.compareTo(from) > 1
        long millis = to.getTimeInMillis() - from.getTimeInMillis();
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        Timber.d(formatPrayerTime(to) + "-" + formatPrayerTime(from) + "=" +
                hours + ":" + minutes + (rounded? "" : ":" + seconds));

        return hours != 0 ?
                rounded ?
                        String.format(Locale.getDefault(), context.getString(R.string.time_interval_rounded), hours, minutes):
                        String.format(Locale.getDefault(), context.getString(R.string.time_interval), hours, minutes, seconds):
                rounded ?
                    String.format(Locale.getDefault(), context.getString(R.string.time_interval_0h_rounded), minutes):
                    String.format(Locale.getDefault(), context.getString(R.string.time_interval_0h), minutes, seconds);
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
