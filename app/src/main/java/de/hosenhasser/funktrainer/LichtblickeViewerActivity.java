package de.hosenhasser.funktrainer;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import de.hosenhasser.funktrainer.data.LichtblickType;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LichtblickeViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener{
    private SharedPreferences mPrefs;

    public static final String LICHTBLICKE_A_FILE = "Lichtblick-A_v1.pdf";
    public static final String LICHTBLICKE_E_FILE = "Lichtblick-E_v1.pdf";

    PDFView pdfView;

    int nbPages = 0;
    int pageToShow = 0;
    LichtblickType lichtblickType = LichtblickType.A_v1;

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
        this.lichtblickType = LichtblickType.values()[getIntent().getExtras().getInt(getClass().getName() + ".lichtblickType", LichtblickType.NONE.ordinal())];

        if (this.pageToShow <= 0) {
            this.pageToShow = mPrefs.getInt("last_page_shown", 0);
        }

        pdfView = findViewById(R.id.pdfview);
        String fileToLoad = LICHTBLICKE_A_FILE;
        if(this.lichtblickType.equals(LichtblickType.A_v1) || this.lichtblickType.equals(LichtblickType.NONE)) {
            fileToLoad = LICHTBLICKE_A_FILE;
        } else if(this.lichtblickType.equals(LichtblickType.E_v1)) {
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
