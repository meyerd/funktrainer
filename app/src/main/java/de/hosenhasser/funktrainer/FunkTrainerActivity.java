/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *  	      2015 Dominik Meyer
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hosenhasser.funktrainer.data.Repository;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class FunkTrainerActivity extends Activity {
	private Repository repository;
    private SimpleCursorAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
          
        repository = new Repository(this);
    }

    public void updateAdapter() {
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	final ListView topicList = (ListView) findViewById(R.id.listView1);
        
        this.adapter = new SimpleCursorAdapter(this,
                R.layout.topic_list_item,
                null,
                new String[]{"name", "status", "next_question"},
                new int[]{R.id.topicListItem, R.id.topicStatusView, R.id.nextQuestionTime});

        adapter.setViewBinder(new ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor,
                                        int columnIndex) {

                if (columnIndex == 4) {
                    final TextView textView = (TextView) view;
                    if (!cursor.isNull(4)) {
                        final long nextQuestion = cursor.getLong(4);
                        final long now = new Date().getTime();
                        if (nextQuestion > now) {

                            if (nextQuestion - now < 64800000L) {
                                textView.setText(getString(R.string.nextLabel) + " " + DateFormat.getTimeInstance().format(new Date(nextQuestion)));
                            } else {
                                textView.setText(getString(R.string.nextLabel) + " " + DateFormat.getDateTimeInstance().format(new Date(nextQuestion)));
                            }
                            return true;
                        }
                    }
                    textView.setText("");
                    return true;
                }

                return false;
            }
        });
        topicList.setAdapter(adapter);
        repository.setTopicsInSimpleCursorAdapter(adapter);
        
        topicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			//@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {

				final Intent intent = new Intent(FunkTrainerActivity.this, AdvancedQuestionAsker.class);
				intent.putExtra(AdvancedQuestionAsker.class.getName() + ".topic", id);
				startActivity(intent);
			}
		});
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	final ListView topicList = (ListView) findViewById(R.id.listView1);
    	
    	final SimpleCursorAdapter adapter = (SimpleCursorAdapter) topicList.getAdapter();
    	final Cursor previousCursor = adapter.getCursor();
    	adapter.changeCursor(null);
    	previousCursor.close();
    	topicList.setAdapter(null);
    }

	/**
	 * Populate options menu.
	 *
	 * @param menu the menu to populate
	 * @return always true
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	/**
	 * Handle option menu selections.
	 *
	 * @param item the Item the user selected
	 */
	@Override
    public boolean onOptionsItemSelected(final MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
			case R.id.mainSearch:
                final Intent intent = new Intent(FunkTrainerActivity.this, QuestionSearch.class);
                // intent.putExtra(AdvacnedQuestionAsker.class.getName() + ".topic", id);
                startActivity(intent);
                return true;
            case R.id.mainLichtblick:
                final Intent intentLichtblick = new Intent(this, LichtblickeViewerActivity.class);
                final int lichtblickPage = 0;
                intentLichtblick.putExtra(LichtblickeViewerActivity.class.getName() + ".lichtblickPage", lichtblickPage);
                startActivity(intentLichtblick);
                return true;

//		case R.id.mainHelp:
//			final StringBuilder uri = new StringBuilder();
//			uri.append("http://funktrainer.hosenhasser.de/app/help?view=TrainerActivity");
//			final Intent intent = new Intent(Intent.ACTION_VIEW);
//			intent.setData(Uri.parse(uri.toString()));
//			startActivity(intent);
//			return true;
//		case R.id.mainInfo:
//			final StringBuilder uri2 = new StringBuilder();
//			uri2.append("http://funktrainer.hosenhasser.de/app/info?view=TrainerActivity");
//			final Intent intent2 = new Intent(Intent.ACTION_VIEW);
//			intent2.setData(Uri.parse(uri2.toString()));
//			startActivity(intent2);
//			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
