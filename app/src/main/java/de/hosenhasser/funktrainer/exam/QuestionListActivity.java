package de.hosenhasser.funktrainer.exam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.ExamSettings;
import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.QuestionState;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.views.QuestionView;

public class QuestionListActivity extends Activity {
    private static final String TAG = QuestionListActivity.class.getName();

    private int topicId;
    private Repository repository;
    private ExamSettings examSettings;
    private ArrayList<QuestionState> questions;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getClass().getName() + ".topic", topicId);
        outState.putParcelableArrayList(getClass().getName() + ".questions", questions);
        outState.putSerializable(getClass().getName() + ".examSettings", examSettings);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = Repository.getInstance();

        setContentView(R.layout.exam_question_list);

        if (savedInstanceState != null) {
            topicId = savedInstanceState.getInt(getClass().getName() + ".topic");
            questions = savedInstanceState.getParcelableArrayList(getClass().getName() + ".questions");
            examSettings = (ExamSettings) savedInstanceState.getSerializable(getClass().getName() + ".examSettings");
        } else {
            Bundle intbundle = getIntent().getExtras();
            if(intbundle != null) {
                topicId = (int) getIntent().getExtras().getLong(getClass().getName() + ".topic");
            }
        }

        if (examSettings == null) {
            examSettings = repository.getExamSettings(topicId);
        }

        if (questions == null ) {
            questions = new ArrayList<>();
            List<Question> qlist = repository.getQuestionsForExam(topicId, examSettings.getnQuestions());
            for(Question q: qlist) {
                questions.add(new QuestionState(q));
            }
        }


        final ListView questionListView = findViewById(R.id.questionListView);
        questionListView.setAdapter(new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return questions.size();
            }

            @Override
            public Object getItem(int position) {
                return questions.get(position);
            }

            @Override
            public long getItemId(int position) {
                return ((QuestionState) getItem(position)).getQuestion(repository).getId();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                QuestionView qv;
                if (convertView == null) {
                    qv = new QuestionView(QuestionListActivity.this);
                } else {
                    qv = (QuestionView) convertView;
                }
                qv.setListPosition(position + 1);
                qv.setQuestionState((QuestionState) getItem(position));
                return qv;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });

        Button evaluateButton = findViewById(R.id.evaluateButton);
        evaluateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < questions.size(); i++) {
                    QuestionState qs = questions.get(i);
                    Log.d(TAG, "question: " + qs.getQuestion(repository).getReference() + ", answer: " + qs.isCorrect());
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean count_statistics = sharedPref.getBoolean("pref_count_exam_answer_statistics", false);
                    if(count_statistics) {
                        if(qs.isCorrect()) {
                            repository.answeredCorrect(qs.getQuestionId());
                        } else {
                            repository.answeredIncorrect(qs.getQuestionId());
                        }
                    }
                }

                QuestionResults result = new QuestionResults(questions, examSettings);

                Intent i = new Intent(QuestionListActivity.this, ExamReportActivity.class);
                i.putExtra(ExamReportActivity.class.getName() + ".result", result);
                startActivity(i);
                finish();
            }
        });

        final TextView countdownText = findViewById(R.id.countdownTimerText);

        new CountDownTimer(examSettings.getnSecondsAvailable() * 1000L, 60 * 1000L) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / (1000L * 60L)) % 60;
                long hours = (millisUntilFinished / (1000L * 60L * 60L)) % 24;
                countdownText.setText(hours + ":" + minutes);
            }
            @SuppressLint("SetTextI18n")
            public void onFinish() {
                countdownText.setText("00:00");
            }
        }.start();
    }
}
