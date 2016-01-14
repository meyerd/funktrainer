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

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;

import de.hosenhasser.funktrainer.data.Repository;

public class QuestionSearch extends Activity {
    // large parts of code inspired from
    // http://www.androidhive.info/2012/09/android-adding-search-functionality-to-listview/
    private ListView lv;
    ArrayAdapter<String> adapter;
    EditText inputSearch;
    ArrayList<HashMap<String, String>> questionList;
    private Repository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_search);

        repository = new Repository(this);
        String[] qs = repository.getAllQuestionIdentifiers();

        lv = (ListView)findViewById(R.id.questionSearchList);
        inputSearch = (EditText)findViewById(R.id.questionSearchInput);

        adapter = new ArrayAdapter<String>(this, R.layout.question_search_list_item,
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
                final Intent intent = new Intent(QuestionSearch.this, QuestionAsker.class);
                intent.putExtra(QuestionAsker.class.getName() + ".questionReference", adapter.getItem(position));
                startActivity(intent);
            }
        });
    }
}
