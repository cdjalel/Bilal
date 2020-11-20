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
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Keep;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.djalel.android.bilal.PrayerTimesApp;
import com.djalel.android.bilal.R;

import java.util.Locale;

@Keep
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tv = findViewById(R.id.about_textview);
        tv.setMovementMethod(new ScrollingMovementMethod());
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
