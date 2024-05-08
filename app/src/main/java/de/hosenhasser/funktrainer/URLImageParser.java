/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2015 Dominik Meyer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * inspired from the solution on:
 * https://stackoverflow.com/questions/7424512/android-html-imagegetter-as-asynctask
 */

package de.hosenhasser.funktrainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;

public class URLImageParser implements Html.ImageGetter {
    Context c;
    View container;
    int screenwidth;
    int screenheight;

    String name_prefix;

    public URLImageParser(View t, Context c) {
        this.c = c;
        this.container = t;
        DisplayMetrics metrics = this.c.getResources().getDisplayMetrics();
        int screenwidth = metrics.widthPixels;
        int screenheight = metrics.heightPixels;
        this.screenwidth = screenwidth;
        this.screenheight = screenheight;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        final String questionsVersion = sharedPref.getString("pref_questions_version", "2");

        name_prefix = "v2_";
        if(questionsVersion.equals("1")) {
            name_prefix = "v1_";
        } else if(questionsVersion.equals("2")) {
            name_prefix = "v2_";
        }
    }

    public Drawable getDrawable(String source) {
        /*
          parse url images in the form of <img="asdf.png"/>
          first strip the part 'img="'
          then construct the resource locator depending on the selected questions version
         */
        // TODO: fix this logic somehow otherwise always looking up preferences and then determine
        //       the path might be sub-optima, unclear?

        String stripped_iname = source.substring(0, source.length() - 4);
        int imageResourceId = -1;
        imageResourceId = this.c.getResources().getIdentifier(this.name_prefix + stripped_iname,
                "drawable", this.c.getPackageName());
        Drawable img = this.c.getResources().getDrawable(imageResourceId);
        int containerwidth = this.container.getMeasuredWidth();
        int orgwidth = img.getIntrinsicWidth();
//        int width = Math.min(orgwidth * 2, this.screenwidth - (int) (this.screenwidth + 0.1));
        int width = Math.min((int)(orgwidth * 1.2), containerwidth);
        if (width <= 0) {
            width = Math.min((int)(orgwidth * 1.2), this.screenwidth - 10);
        }
        float ratio = (float)width / (float)orgwidth;
        img.setBounds(0, 0, width, (int)(img.getIntrinsicHeight() * ratio));

        return img;
    }
}