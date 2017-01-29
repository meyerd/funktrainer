package de.hosenhasser.funktrainer.exam;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.views.QuestionView;

public class QuestionList extends Activity {
    private static final String TAG = QuestionList.class.getName();

    private int topicId;
    private Repository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO this should be dynamic
        final int numQuestions = 10;

        repository = new Repository(this);

        setContentView(R.layout.exam_question_list);

        if (savedInstanceState != null) {
            topicId = savedInstanceState.getInt(getClass().getName() + ".topic");
        } else {
            Bundle intbundle = getIntent().getExtras();
            if(intbundle != null) {
                topicId = (int) getIntent().getExtras().getLong(getClass().getName() + ".topic");
            }
        }

        final List<Question> questions = new ArrayList<Question>();
        for (int i = 0; i < numQuestions; i++) {
            int selected = repository.selectQuestionByTopicId(topicId).getSelectedQuestion();
            questions.add(repository.getQuestion(selected));
        }

        final QuestionView[] questionViews = new QuestionView[numQuestions];

        final ListView questionListView = (ListView) findViewById(R.id.questionListView);
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
                return ((Question) getItem(position)).getId();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (questionViews[position] == null) {
                    QuestionView v = new QuestionView(QuestionList.this);
                    v.setQuestion((Question)getItem(position));
                    questionViews[position] = v;
                }
                return questionViews[position];
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

        Button evaluateButton = (Button) findViewById(R.id.evaluateButton);
        evaluateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<QuestionResult> result = new ArrayList<QuestionResult>();

                for (int i = 0; i < numQuestions; i++) {
                    QuestionView qv = questionViews[i];
                    if (qv != null) {
                        Log.d(TAG, "question: " + qv.getQuestion().getReference() + ", answer: " + qv.isCorrect());
                        result.add(qv.getResult());
                    } else {
                        result.add(new QuestionResult(questions.get(i), false));
                    }
                }

                Intent i = new Intent(QuestionList.this, ExamReportActivity.class);
                i.putExtra(ExamReportActivity.class.getName() + ".result", result);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });
    }
}
