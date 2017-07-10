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

package org.linuxac.bilal;

import org.arabeyes.prayertime.Prayer;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;


public class PrayerTimes {
    private GregorianCalendar[] all;        // TODO optimize memory, use PrayerTime[] & rework before()
    private GregorianCalendar current;
    private GregorianCalendar next;
    private int c;                          // current prayer index
    private int n;                          // next prayer index

    public PrayerTimes(GregorianCalendar now, GregorianCalendar... all) {
        this.all = all;
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

    public GregorianCalendar get(int i) { assert(i>=0 && i<all.length); return all[i]; }

    public static String format(GregorianCalendar cal)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:m");
        return sdf.format(cal.getTime());
    }

    public String format(int i)
    {
        assert(i>=0 && i<all.length && null != all);
        return format(all[i]);
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
                // sunset is skipped. TODO make skipping a user setting
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
