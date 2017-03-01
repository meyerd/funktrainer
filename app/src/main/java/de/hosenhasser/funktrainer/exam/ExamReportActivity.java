package de.hosenhasser.funktrainer.exam;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.QuestionState;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.views.QuestionView;

public class ExamReportActivity extends Activity {
    private Repository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = new Repository(this);

        setContentView(R.layout.exam_report);

        final QuestionResults results = (QuestionResults) getIntent().getSerializableExtra(getClass().getName() + ".result");
        ExpandableListView resultsListView = (ExpandableListView) findViewById(R.id.resultList);
        TextView resultsTopText = (TextView) findViewById(R.id.examResultTopText);
        TextView resultsRecommendation = (TextView) findViewById(R.id.examResultRecommendation);

        int nCorrect = 0;
        for(QuestionState r : results.getResults()) {
            if(r.isCorrect()) {
                nCorrect++;
            }
        }
        int nQuestions = results.getExamSettings().getnQuestions();
        int nRequired = results.getExamSettings().getnRequired();
        boolean passed = nCorrect >= nRequired;

        resultsTopText.setText(String.format(getString(R.string.exam_report_result_text), nCorrect, nQuestions));
        if (passed) {
            resultsTopText.setTextColor(Color.GREEN);
            resultsRecommendation.setText(getString(R.string.exam_result_recommendation_passed));
        } else {
            resultsTopText.setTextColor(Color.RED);
            resultsRecommendation.setText(getString(R.string.exam_result_recommendation_failed));
        }

        resultsListView.setAdapter(new ExpandableListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getGroupCount() {
                return results.getResults().size();
            }

            @Override
            public int getChildrenCount(int i) {
                return 1;
            }

            @Override
            public Object getGroup(int i) {
                return null;
            }

            @Override
            public Object getChild(int i, int i1) {
                return null;
            }

            @Override
            public long getGroupId(int i) {
                return 0;
            }

            @Override
            public long getChildId(int i, int i1) {
                return 0;
            }


            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
                View v;
                if (view == null) {
                    v = ExamReportActivity.this.getLayoutInflater().inflate(R.layout.exam_report_result_item, viewGroup, false);
                } else {
                    v = view;
                }
                TextView reference = (TextView) v.findViewById(R.id.questionReference);
                TextView result = (TextView) v.findViewById(R.id.questionResult);
                QuestionState qs = results.getResults().get(i);
                reference.setText(qs.getQuestion(repository).getReference());
                if (qs.hasAnswer()) {
                    result.setText(getResources().getString(qs.isCorrect() ? R.string.correct : R.string.wrong));
                    result.setTextColor(qs.isCorrect() ? Color.GREEN : Color.RED);
                } else {
                    // did a lot of googling on how to get the default color back. this is about as simple as i could find.
                    // even though it's not really what i was looking for, it should get the job done.
                    ColorStateList color = new TextView(ExamReportActivity.this).getTextColors();

                    result.setText(getResources().getString(R.string.not_answered));
                    result.setTextColor(color);
                }
                return v;
            }

            @Override
            public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
                QuestionView v;
                if (view == null) {
                    v = new QuestionView(ExamReportActivity.this);
                } else {
                    v = (QuestionView) view;
                }
                v.setQuestionState(results.getResults().get(i));
                v.setRadioGroupEnabled(false);
                v.showCorrectAnswer();
                return v;
            }

            @Override
            public boolean isChildSelectable(int i, int i1) {
                return true;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public void onGroupExpanded(int i) {

            }

            @Override
            public void onGroupCollapsed(int i) {

            }

            @Override
            public long getCombinedChildId(long l, long l1) {
                return 0;
            }

            @Override
            public long getCombinedGroupId(long l) {
                return 0;
            }
        });

    }
}
