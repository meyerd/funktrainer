package de.hosenhasser.funktrainer.exam;

import android.app.Activity;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.QuestionState;
import de.hosenhasser.funktrainer.data.Repository;

public class ExamReportActivity extends Activity {
    private Repository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = new Repository(this);

        setContentView(R.layout.exam_report);

        final QuestionResults results = (QuestionResults) getIntent().getSerializableExtra(getClass().getName() + ".result");
        ListView resultsListView = (ListView) findViewById(R.id.resultList);
        TextView resultsTopText = (TextView) findViewById(R.id.examResultTopText);

        int nCorrect = 0;
        for(QuestionState r : results.getResults()) {
            if(r.isCorrect()) {
                nCorrect++;
            }
        }
        int nRequired = results.getExamSettings().getnRequired();
        boolean passed = nCorrect >= nRequired;

        resultsTopText.setText(getString(R.string.exam_report_result_text) + nCorrect + "/" + nRequired);
        if(passed) {
            resultsTopText.setTextColor(Color.GREEN);
        } else {
            resultsTopText.setTextColor(Color.RED);
        }


        resultsListView.setAdapter(new ListAdapter() {
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
                return results.getResults().size();
            }

            @Override
            public Object getItem(int position) {
                return results.getResults().get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v;
                if (convertView == null) {
                    v = ExamReportActivity.this.getLayoutInflater().inflate(R.layout.exam_report_result_item, null);
                } else {
                    v = convertView;
                }
                TextView reference = (TextView) v.findViewById(R.id.questionReference);
                TextView result = (TextView) v.findViewById(R.id.questionResult);
                QuestionState qs = results.getResults().get(position);
                reference.setText(qs.getQuestion(repository).getReference());
                result.setText(Boolean.toString(qs.isCorrect()));
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
