/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *  		  2015 Dominik Meyer
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.data.TopicStats;

public class StatisticsActivity extends Activity {
	private Repository repository;
	private int topicId;

	@Override
	public void onDestroy() {
		super.onDestroy();
		repository = null;
	}
	
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putLong(getClass().getName() + ".topic", topicId);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        repository = Repository.getInstance();
        
        if (savedInstanceState != null) {
        	topicId = (int) savedInstanceState.getLong(getClass().getName()+".topic");
        } else {
        	topicId = getIntent().getExtras().getInt(getClass().getName()+".topic", 0);
        }
        
        setContentView(R.layout.statistics);
        
        final TextView topicName = findViewById(R.id.topicName);
        topicName.setText(repository.getTopic(topicId).getName());
        
        final TopicStats stats = repository.getTopicStat(topicId);
        
        final ProgressBar totalProgress = findViewById(R.id.totalProgress);
        totalProgress.setMax(stats.getMaxProgress());
        totalProgress.setProgress(stats.getCurrentProgress());
        
        final ProgressBar atLevel0 = findViewById(R.id.atLevel0);
        atLevel0.setMax(stats.getQuestionCount());
        atLevel0.setProgress(stats.getQuestionsAtLevel()[0]);
        
        final ProgressBar atLevel1 = findViewById(R.id.atLevel1);
        atLevel1.setMax(stats.getQuestionCount());
        atLevel1.setProgress(stats.getQuestionsAtLevel()[1]);

        final ProgressBar atLevel2 = findViewById(R.id.atLevel2);
        atLevel2.setMax(stats.getQuestionCount());
        atLevel2.setProgress(stats.getQuestionsAtLevel()[2]);

        final ProgressBar atLevel3 = findViewById(R.id.atLevel3);
        atLevel3.setMax(stats.getQuestionCount());
        atLevel3.setProgress(stats.getQuestionsAtLevel()[3]);
        
        final ProgressBar atLevel4 = findViewById(R.id.atLevel4);
        atLevel4.setMax(stats.getQuestionCount());
        atLevel4.setProgress(stats.getQuestionsAtLevel()[4]);

        final ProgressBar atLevel5 = findViewById(R.id.atLevel5);
        atLevel5.setMax(stats.getQuestionCount());
        atLevel5.setProgress(stats.getQuestionsAtLevel()[5]);
	}

	/**
	 * Populate the options menu.
	 * 
	 * @param menu the menu to populate
	 * @return always true
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.statisticsmenu, menu);
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
//		case R.id.statHelp:
//			final StringBuilder uri = new StringBuilder();
//			uri.append("http://funktrainer.hosenhasser.dei/app/help?view=StatisticsActivity");
//			final Intent intent = new Intent(Intent.ACTION_VIEW);
//			intent.setData(Uri.parse(uri.toString()));
//			startActivity(intent);
//			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
