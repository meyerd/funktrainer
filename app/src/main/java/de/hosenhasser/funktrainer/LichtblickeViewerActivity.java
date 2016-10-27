package de.hosenhasser.funktrainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LichtblickeViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener{

    public static final String LICHTBLICKE_A_FILE = "Lichtblick-A.pdf";

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
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("last_page_shown", pdfView.getCurrentPage() + 1);
        ed.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lichtblicke_viewer);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);

        this.pageToShow = getIntent().getExtras().getInt(getClass().getName() + ".lichtblickPage");
        this.pageToShow += 1;

        if(this.pageToShow <= 1) {
            this.pageToShow = mPrefs.getInt("last_page_shown", 1);
        }
//        if(questionId != 0) {
//            QuestionSelection questionSel = repository.selectQuestionById(questionId);
//            int selectedQuestion = questionSel.getSelectedQuestion();
//            if(selectedQuestion != 0) {
//                final Question question = repository.getQuestion(selectedQuestion);
//                pageToShow = question.getLichtblickPage();
//            }
//        }

        pdfView = (PDFView)findViewById(R.id.pdfview);
        pdfView.fromAsset(LICHTBLICKE_A_FILE)
//                .pages(0, 1, 2, 3, 4, 5)
                .defaultPage(this.pageToShow)
                .showMinimap(false)
                .enableSwipe(true)
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
