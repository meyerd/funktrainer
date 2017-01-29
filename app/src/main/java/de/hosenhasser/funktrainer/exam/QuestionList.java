package de.hosenhasser.funktrainer.exam;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.List;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.views.QuestionView;

public class QuestionList extends Activity {

    private int topicId;
    private Repository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        for (int i = 0; i < 10; i++) {
            int selected = repository.selectQuestionByTopicId(topicId).getSelectedQuestion();
            questions.add(repository.getQuestion(selected));
        }

        ListView questionListView = (ListView) findViewById(R.id.questionListView);
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
                QuestionView v = new QuestionView(QuestionList.this);
                v.setQuestion((Question)getItem(position));
                return v;
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
    }
}
