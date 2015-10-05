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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.QuestionSelection;
import de.hosenhasser.funktrainer.data.Repository;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;

public class QuestionAsker extends Activity {
	private Repository repository;
	private int currentQuestion;
	private int topicId;
	private int correctChoice;
	private int maxProgress;
	private int currentProgress;
	private Random rand = new Random();
	private List<Integer> order;
	private boolean showingCorrectAnswer;
	private Date nextTime;
	private Timer waitTimer;
	private boolean showingStandardView;
	private boolean replaceNNBSP = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		cancelTimer();
		
		repository.close();
		repository = null;
		rand = null;
		order = null;
		nextTime = null;
	}
	
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(getClass().getName() + ".showingCorrectAnswer", showingCorrectAnswer);
		outState.putInt(getClass().getName() + ".currentQuestion", currentQuestion);
		outState.putInt(getClass().getName() + ".maxProgress", maxProgress);
		outState.putInt(getClass().getName() + ".currentProgress", currentProgress);
		outState.putLong(getClass().getName() + ".topic", topicId);
		if (nextTime != null) {
			outState.putLong(getClass().getName() + ".nextTime", nextTime.getTime());
		}
		
		if (order != null) {
			final StringBuilder orderString = new StringBuilder();
			for (int i = 0; i < order.size(); i++) {
				if (i > 0) {
					orderString.append(',');
				}
				orderString.append(order.get(i));
			}
			outState.putString(getClass().getName() + ".order", orderString.toString());
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        repository = new Repository(this);
        
        showStandardView();
        
        if (savedInstanceState != null) {
        	topicId = (int) savedInstanceState.getLong(getClass().getName()+".topic");
        	currentQuestion = savedInstanceState.getInt(getClass().getName()+".currentQuestion");
        	final long nextTimeLong = savedInstanceState.getLong(getClass().getName()+".nextTime");
        	nextTime = nextTimeLong > 0L ? new Date(nextTimeLong) : null;
        	showingCorrectAnswer = savedInstanceState.getBoolean(getClass().getName()+".showingCorrectAnswer");
			maxProgress = savedInstanceState.getInt(getClass().getName() + ".maxProgress");
			currentProgress = savedInstanceState.getInt(getClass().getName() + ".currentProgress");
        	
        	final String orderString = savedInstanceState.getString(getClass().getName()+".order");
        	if (orderString != null) {
        		final String[] orderArray = orderString.split(",");
        		order = new LinkedList<Integer>();
        		for (int i = 0; i < orderArray.length; i++) {
        			order.add(Integer.parseInt(orderArray[i]));
        		}
        	}
        	
        	showQuestion();
        } else {
        	topicId = (int) getIntent().getExtras().getLong(getClass().getName()+".topic");
        	nextQuestion();
        }
        

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
		inflater.inflate(R.menu.askermenu, menu);
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
            case R.id.resetTopic:
				askRestartTopic();
				return true;
            case R.id.statistics:
				final Intent intent = new Intent(this, StatisticsActivity.class);
				intent.putExtra(StatisticsActivity.class.getName() + ".topic", topicId);
				startActivity(intent);
				return true;
			case R.id.help:
				final StringBuilder uri = new StringBuilder();
				uri.append("http://funktrainer.hosenhasser.de/app/help?question=");
				uri.append(currentQuestion);
				uri.append("&topic=");
				uri.append(topicId);
				uri.append("&view=QuestionAsker");
				final Intent intent2 = new Intent(Intent.ACTION_VIEW);
				intent2.setData(Uri.parse(uri.toString()));
				startActivity(intent2);
				return true;
            default:
				return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Restarts the topic after asking for confirmation.
     */
    private void askRestartTopic() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.warningReset);
		builder.setCancelable(true);
		builder.setPositiveButton(R.string.resetOkay, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				restartTopic();
			}
		});
		builder.setNegativeButton(R.string.resetCancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
    }

    private void restartTopic() {
		repository.resetTopic(topicId);
		nextQuestion();
    }

	private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;

		ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar1);
		progress.setMax(5);

		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		final Button contButton = (Button) findViewById(R.id.button1);

		// only enable continue when answer is selected
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                contButton.setEnabled(radioGroup.getCheckedRadioButtonId() != -1);
            }

        });
	
        contButton.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {

                // find what has been selected
                if (showingCorrectAnswer) {
                    showingCorrectAnswer = false;
                    radioGroup.setEnabled(true);
                    nextQuestion();
                    return;
                }

                int selectedButton = radioGroup.getCheckedRadioButtonId();
                if (selectedButton == correctChoice) {
                    Toast.makeText(QuestionAsker.this, getString(R.string.right), Toast.LENGTH_SHORT).show();

                    repository.answeredCorrect(currentQuestion);

                    nextQuestion();

                    return;
                } else if (selectedButton != -1) {
                    repository.answeredIncorrect(currentQuestion);

                    showingCorrectAnswer = true;
                    radioGroup.setEnabled(false);

                    final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
                    correctButton.setBackgroundResource(R.color.correctAnswer);

                    return;
                } else {
                    Toast.makeText(QuestionAsker.this, getString(R.string.noAnswerSelected), Toast.LENGTH_SHORT).show();
                }
            }
        });		
	}
	
	private void nextQuestion() {
		if (!showingStandardView) {
			showStandardView();
		}
		
		if (correctChoice != 0) {
			final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
			correctButton.setBackgroundResource(0);
		}
		
		order = null;
		
		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup.clearCheck();
		final Button contButton = (Button) findViewById(R.id.button1);
		contButton.setEnabled(false);
		
		final QuestionSelection nextQuestion = repository.selectQuestion(topicId);
		
		// any question?
		final int selectedQuestion = nextQuestion.getSelectedQuestion();
		if (selectedQuestion != 0) {
			currentQuestion = selectedQuestion;
			maxProgress = nextQuestion.getMaxProgress();
			currentProgress = nextQuestion.getCurrentProgress();
			nextTime = null;
			showQuestion();
			return;
		}
		
		nextTime = nextQuestion.getNextQuestion();
		if (nextTime != null) {
			showQuestion();
			return;
		}
		
		showingStandardView = false;
		setContentView(R.layout.no_more_questions_finished);

		final Button restartTopicButton = (Button) findViewById(R.id.restartTopic);
		restartTopicButton.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                restartTopic();
                nextQuestion();
            }

        });
		return;
	}
	
	private List<RadioButton> getRadioButtons() {
		final List<RadioButton> radioButtons = new LinkedList<RadioButton>();
		radioButtons.add((RadioButton) findViewById(R.id.radio0));
		radioButtons.add((RadioButton) findViewById(R.id.radio1));
		radioButtons.add((RadioButton) findViewById(R.id.radio2));
		radioButtons.add((RadioButton) findViewById(R.id.radio3));
		return radioButtons;
	}
	
	private void showQuestion() {
		if (nextTime != null) {
			showingStandardView = false;
			setContentView(R.layout.no_more_questions_wait);
			
			final TextView nextTimeText = (TextView) findViewById(R.id.nextTimeText);
			if (nextTime.getTime() - new Date().getTime() < 64800000L) {
				nextTimeText.setText(DateFormat.getTimeInstance().format(nextTime));
			} else {
				nextTimeText.setText(DateFormat.getDateTimeInstance().format(nextTime));
			}
			showNextQuestionAt(nextTime);
			
			final Button resetWaitButton = (Button) findViewById(R.id.resetWait);
			resetWaitButton.setOnClickListener(new View.OnClickListener() {
				//@Override
				public void onClick(View v) {
					cancelTimer();
					repository.continueNow(topicId);
					nextQuestion();
					return;
				}
				
			});
			return;
		}		
		
		final Question question = repository.getQuestion(currentQuestion);

		final TextView levelText = (TextView) findViewById(R.id.levelText);
		levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
				question.getLevel() == 1 ? getString(R.string.secondPass) :
				question.getLevel() == 2 ? getString(R.string.thirdPass) :
				question.getLevel() == 3 ? getString(R.string.fourthPass) :
				question.getLevel() == 4 ? getString(R.string.fifthPass) :
				String.format(getString(R.string.passText), question.getLevel()));

		final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        URLImageParser p = new URLImageParser(textView, this);
        Spanned htmlSpan = Html.fromHtml(safeText(question.getQuestionText()), p, null);
        textView.setText(htmlSpan);
//		textView.setText(safeText(question.getQuestionText()));
		
		final List<RadioButton> radioButtons = getRadioButtons();
		
		if (order == null) {
			order = new LinkedList<Integer>();

			for (int i = 0; i < 4; i++) {
				order.add(rand.nextInt(order.size() + 1), i);
			}
		}
		correctChoice = radioButtons.get(order.get(0)).getId();
		for (int i = 0; i < 4; i++) {
            RadioButton rb = radioButtons.get(order.get(i));
            URLImageParser p_rb = new URLImageParser(rb, this);
            Spanned htmlSpan_rb = Html.fromHtml(safeText(question.getAnswers().get(i)), p_rb, null);
            rb.setText(htmlSpan_rb);
//            rb.setText(Html.fromHtml(safeText(question.getAnswers().get(i))));
//            if(question.getAnswerImages().get(i).size() > 0) {
//                String iname = question.getAnswerImages().get(i).get(0);
//                String stripped_iname = iname.substring(0, iname.length() - 4);
//                int imageResourceId = -1;
//                Context context = rb.getContext();
//                imageResourceId = context.getResources().getIdentifier(stripped_iname, "drawable", context.getPackageName());
//                Drawable img = context.getResources().getDrawable(imageResourceId);
//                rb.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
//                //rb.setBackgroundResource(imageResourceId);
//            } else {
//				rb.setCompoundDrawables(null, null, null, null);
//			}
		}

		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setMax(maxProgress);
		progressBar.setProgress(currentProgress);
		
        // remove previous question image if any
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            final View childAtIndex = linearLayout.getChildAt(i);
            if (childAtIndex instanceof ImageView) {
                linearLayout.removeViewAt(i);
                break;
            }
        }

//        final List<ImageView> questionImages = getQuestionImage(question.getImages());
//		int idx = 3;
//        for(ImageView iv : questionImages) {
//            linearLayout.addView(iv, idx++);
//        }

	}
	
    private List<ImageView> getQuestionImage(List<String> inames) {
        List<ImageView> images = new ArrayList<ImageView>();
        for(String iname : inames) {
            String stripped_iname = iname.substring(0, iname.length() - 4);
            int imageResourceId = -1;

            final ImageView image = new ImageView(this);
            Context context = image.getContext();
            imageResourceId = context.getResources().getIdentifier(stripped_iname, "drawable", context.getPackageName());
            //imageResourceId = R.drawable.tb111f;
            // image.setBackgroundColor(Color.WHITE);
            image.setImageResource(imageResourceId);
            images.add(image);
        }
        return images;
    }
	
	private void showNextQuestionAt(final Date when) {
		scheduleTask(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					//@Override
					public void run() {
						nextQuestion();
					}
				});
			}
			
		}, when);
	}
	
	private synchronized void scheduleTask(final TimerTask task, final Date when) {
		cancelTimer();
		waitTimer = new Timer("waitNextQuestion", true);
		waitTimer.schedule(task, when);
	}
	
	private synchronized void cancelTimer() {
		if (waitTimer != null) {
			waitTimer.cancel();
			waitTimer = null;
		}
	}

	private String safeText(final String source) {
		return replaceNNBSP && source != null ? source.replace('\u202f', '\u00a0') : source;
	}
}
