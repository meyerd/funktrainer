/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2015 Dominik Meyer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.hosenhasser.funktrainer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.data.SearchItem;

public class QuestionSearch extends Activity {
    // large parts of code inspired from
    // http://www.androidhive.info/2012/09/android-adding-search-functionality-to-listview/

    class SearchItemArrayAdapter extends ArrayAdapter<SearchItem> implements Filterable {
        private int resource;
        private int textViewResourceId;
        private SearchItem[] searchItems;
        private SearchItem[] originalSearchItems;

        public SearchItemArrayAdapter(Context context, int resource, int textViewResourceId, SearchItem[] searchItems) {
            super(context, 0, searchItems);
            this.resource = resource;
            this.textViewResourceId = textViewResourceId;
            this.searchItems = searchItems;
        }

        @Override
        public int getCount() {
            return searchItems.length;
        }

        @Override
        public SearchItem getItem(int position) {
            // TODO: why is position 1 if there is only one item left?
//            if(position >= searchItems.length)
//                return searchItems[searchItems.length - 1];
            return searchItems[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SearchItem n = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(this.resource, parent, false);
            }
            TextView tvListItem = convertView.findViewById(this.textViewResourceId);
            if(n != null) {
                tvListItem.setText(n.label);
            } else {
                tvListItem.setText("");
            }
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                // taken from
                // https://stackoverflow.com/questions/19122848/custom-getfilter-in-custom-arrayadapter-in-android
                // and
                // http://www.survivingwithandroid.com/2012/10/android-listview-custom-filter-and.html

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    Log.d("Funktrainer", "Search: " + constraint);
                    if(originalSearchItems == null) {
                        originalSearchItems = searchItems;
                    }
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        results.values = originalSearchItems;
                        results.count = originalSearchItems.length;
                    } else {
                        List<SearchItem> n = new ArrayList<SearchItem>();
                        for (SearchItem s : originalSearchItems) {
                            if (s.label.toUpperCase().startsWith(constraint.toString().toUpperCase())) {
                                n.add(s);
                            }
                        }
                        results.values = n.toArray(new SearchItem[0]);
                        results.count = n.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results.count == 0) {
                        notifyDataSetInvalidated();
                    } else {
                        Log.d("Funktrainer", "Search: publish results " + Integer.toString(results.count));
                        searchItems = (SearchItem[]) results.values;
                        notifyDataSetChanged();
                    }
                }
            };
        }
    }

    private ListView lv;
    SearchItemArrayAdapter adapter;
    EditText inputSearch;
    ArrayList<HashMap<String, String>> questionList;
    private Repository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_search);

        repository = Repository.getInstance();
        SearchItem[] qs = repository.getAllQuestionIdentifiers();

        lv = findViewById(R.id.questionSearchList);
        inputSearch = findViewById(R.id.questionSearchInput);

        adapter = new SearchItemArrayAdapter(this, R.layout.question_search_list_item,
                R.id.search_list_item, qs);
        lv.setAdapter(adapter);

        inputSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                QuestionSearch.this.adapter.getFilter().filter(charSequence);
            }

            public void afterTextChanged(Editable editable) {

            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Intent intent = new Intent(QuestionSearch.this, AdvancedQuestionAsker.class);
                SearchItem si = adapter.getItem(position);
                if(si != null) {
                    intent.putExtra(AdvancedQuestionAsker.class.getName() + ".questionId", si.id);
                } else {
                    intent.putExtra(AdvancedQuestionAsker.class.getName() + ".questionId", 0);
                }
                startActivity(intent);
            }
        });
    }
}
