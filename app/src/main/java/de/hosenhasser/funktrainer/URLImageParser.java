package de.hosenhasser.funktrainer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View;

public class URLImageParser implements Html.ImageGetter {
    Context c;
    View container;
    int screenwidth;
    int screenheight;

    /***
     * Construct the URLImageParser which will execute AsyncTask and refresh the container
     * @param t
     * @param c
     */
    public URLImageParser(View t, Context c) {
        this.c = c;
        this.container = t;
        DisplayMetrics metrics = this.c.getResources().getDisplayMetrics();
        int screenwidth = metrics.widthPixels;
        int screenheight = metrics.heightPixels;
        this.screenwidth = screenwidth;
        this.screenheight = screenheight;
    }

    public Drawable getDrawable(String source) {
        String stripped_iname = source.substring(0, source.length() - 4);
        int imageResourceId = -1;
        imageResourceId = this.c.getResources().getIdentifier(stripped_iname, "drawable", this.c.getPackageName());
        Drawable img = this.c.getResources().getDrawable(imageResourceId);
        int containerwidth = this.container.getMeasuredWidth();
        int orgwidth = img.getIntrinsicWidth();
//        int width = Math.min(orgwidth * 2, this.screenwidth - (int) (this.screenwidth + 0.1));
        int width = Math.min((int)(orgwidth * 1.2), containerwidth);
        float ratio = (float)width / (float)orgwidth;
        img.setBounds(0, 0, width, (int)(img.getIntrinsicHeight() * ratio));

        return img;
    }
}