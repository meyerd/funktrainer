package de.hosenhasser.funktrainer;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class FormelsammlungViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener {
    public static final String FORMELSAMMLUNG_FILE_V1 = "formelsammlung_v1.pdf";
    public static final String FORMELSAMMLUNG_FILE_V2 = "formelsammlung_v2.pdf";

    private SharedPreferences mPrefs;

    PDFView pdfView;

    int nbPages = 0;
    int pageToShow = 0;

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

        mPrefs = getSharedPreferences("formelsammlung_viewer_shared_preferences", MODE_PRIVATE);

        setContentView(R.layout.activity_formelsammlung_viewer);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle bund = getIntent().getExtras();
        if(bund != null) {
            this.pageToShow = bund.getInt(getClass().getName() + ".lichtblickPage");
        } else {
            this.pageToShow = 0;
        }

        if (this.pageToShow <= 0) {
            this.pageToShow = mPrefs.getInt("last_page_shown", 0);
        }

        pdfView = findViewById(R.id.formelsammlung_pdfview);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String questionsVersion = preferences.getString("pref_questions_version", "2");
        String formelsammlung_file = FORMELSAMMLUNG_FILE_V2;
        if(questionsVersion.equals("1")) {
            formelsammlung_file = FORMELSAMMLUNG_FILE_V1;
        } else if(questionsVersion.equals("2")) {
            formelsammlung_file = FORMELSAMMLUNG_FILE_V2;
        }

        pdfView.fromAsset(formelsammlung_file)
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
