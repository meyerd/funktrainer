package de.hosenhasser.funktrainer;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.hosenhasser.funktrainer.data.LichtblickType;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LichtblickeViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener{
    private SharedPreferences mPrefs;

    public static final String LICHTBLICKE_A_FILE = "Lichtblick-A.pdf";
    public static final String LICHTBLICKE_E_FILE = "Lichtblick-E.pdf";

    PDFView pdfView;

    int nbPages = 0;
    int pageToShow = 0;
    LichtblickType lichtblickType = LichtblickType.A;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        if(pdfView != null) {
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putInt("last_page_shown", pdfView.getCurrentPage());
            ed.apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);

        setContentView(R.layout.activity_lichtblicke_viewer);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.pageToShow = getIntent().getExtras().getInt(getClass().getName() + ".lichtblickPage", 0);
        this.lichtblickType = LichtblickType.values()[getIntent().getExtras().getInt(getClass().getName() + ".lichtblickType", 0)];

        if (this.pageToShow <= 0) {
            this.pageToShow = mPrefs.getInt("last_page_shown", 0);
        }

        pdfView = findViewById(R.id.pdfview);
        String fileToLoad = LICHTBLICKE_A_FILE;
        if(this.lichtblickType.equals(LichtblickType.E)) {
            fileToLoad = LICHTBLICKE_E_FILE;
        }
        pdfView.fromAsset(fileToLoad)
                .defaultPage(this.pageToShow)
                .enableSwipe(true)
                .enableDoubletap(true)
                .onLoad(this)
                .onPageChange(this)
                .load();
    }

    public void loadComplete(int nbPages) {
        this.nbPages = nbPages;
        this.pageToShow = max(min(nbPages, this.pageToShow), 0);
    }

    public void onPageChanged(int page, int pageCount) {

    }
}
