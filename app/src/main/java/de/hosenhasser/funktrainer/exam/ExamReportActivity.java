package de.hosenhasser.funktrainer.exam;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.hosenhasser.funktrainer.R;

public class ExamReportActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.exam_report);

        final QuestionResults results = (QuestionResults) getIntent().getSerializableExtra(getClass().getName() + ".result");
        ListView resultsListView = (ListView) findViewById(R.id.resultList);
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
                QuestionResultEntry r = results.getResults().get(position);
                reference.setText(r.getReference());
                result.setText(Boolean.toString(r.getResult()));
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
