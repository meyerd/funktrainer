package de.hosenhasser.funktrainer;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Bundle;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class FormelsammlungViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener {
    public static final String FORMELSAMMLUNG_FILE = "formelsammlung.pdf";

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
            ed.commit();
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
            // this.pageToShow += 0;
        } else {
            this.pageToShow = 0;
        }

        if (this.pageToShow <= 0) {
            this.pageToShow = mPrefs.getInt("last_page_shown", 0);
        }
        //        if(questionId != 0) {
        //            QuestionSelection questionSel = repository.selectQuestionById(questionId);
        //            int selectedQuestion = questionSel.getSelectedQuestion();
        //            if(selectedQuestion != 0) {
        //                final Question question = repository.getQuestion(selectedQuestion);
        //                pageToShow = question.getLichtblickPage();
        //            }
        //        }

        pdfView = (PDFView) findViewById(R.id.formelsammlung_pdfview);
        pdfView.fromAsset(FORMELSAMMLUNG_FILE)
                //                .pages(0, 1, 2, 3, 4, 5)
                .defaultPage(this.pageToShow)
//                    .showMinimap(false)
                .enableSwipe(true)
                .enableDoubletap(true)
                //                .onDraw
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
