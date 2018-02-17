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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.QuestionSelection;
import de.hosenhasser.funktrainer.data.QuestionState;
import de.hosenhasser.funktrainer.data.Repository;
import de.hosenhasser.funktrainer.views.QuestionView;

public class AdvancedQuestionAsker extends Activity {
    private Repository repository;
    private QuestionState currentQuestionState;
    private int topicId;
    private int maxProgress;
    private int currentProgress;
    private boolean showingCorrectAnswer;
    private Date nextTime;
    private Timer waitTimer;
    private boolean showingStandardView;

    private SharedPreferences mPrefs;
    private boolean mUpdateNextAnswered;

    private ViewFlipper viewFlipper;
    private GestureDetector gestureDetector;

    private int historyPosition = 0;
    private ArrayList<QuestionState> history = new ArrayList<>();

    private static final int MAX_HISTORY_LENGTH = 30;

    private ColorStateList oldTextColor;

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelTimer();

        repository = null;
        nextTime = null;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(getClass().getName() + ".showingCorrectAnswer", showingCorrectAnswer);
        outState.putParcelable(getClass().getName() + ".currentQuestionState", currentQuestionState);
        outState.putInt(getClass().getName() + ".maxProgress", maxProgress);
        outState.putInt(getClass().getName() + ".currentProgress", currentProgress);
        outState.putLong(getClass().getName() + ".topic", topicId);
        if (nextTime != null) {
            outState.putLong(getClass().getName() + ".nextTime", nextTime.getTime());
        }
        outState.putInt(getClass().getName() + ".historyPosition", historyPosition);
        outState.putParcelableArrayList(getClass().getName() + ".history", history);
    }

    private class CustomGestureDetector extends GestureDetector.SimpleOnGestureListener {
        /*
         * inspired by http://codetheory.in/android-viewflipper-and-viewswitcher/
         *         and https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
         */
        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();


            if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0) {
                    // Swipe left (next)
                    flipRight();
                } else {
                    // Swipe right (previous)
                    flipLeft();
                }
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }
    }

    private void updateHistoryView() {
        final QuestionState histentry = history.get(history.size() - historyPosition);
        TextView oldQuestionTextNumber = findViewById(R.id.oldQuestionHeatTextNumber);
        oldQuestionTextNumber.setText(Integer.toString(-historyPosition));

        final TextView referenceText = findViewById(R.id.referenceTextold);
        referenceText.setText(histentry.getQuestion(repository).getReference());
        if(histentry.getQuestion(repository).getOutdated()) {
            referenceText.setTextColor(Color.RED);
        } else {
            referenceText.setTextColor(this.oldTextColor);
        }

        final QuestionView questionView = findViewById(R.id.questionViewOld);
        questionView.setQuestionState(histentry);
        questionView.showCorrectAnswer();
        questionView.setRadioGroupEnabled(false);
    }

    private void flipRight() {
        //Log.i("Funktrainer", "flip right");
        if (!showingStandardView) {
            return;
        }
        if(history.size() > historyPosition) {
            int historyPositionOld = historyPosition;
            historyPosition = Math.min(historyPosition + 1, history.size() + 1);
            if(historyPositionOld <= 0 && historyPosition > 0) {
                viewFlipper.showPrevious();
            }
            if(historyPosition > 0) {
                updateHistoryView();
            }
        }
    }

    private void flipLeft() {
        //Log.i("Funktrainer", "flip left");
        if (!showingStandardView) {
            return;
        }
        if (historyPosition > 0) {
            int historyPositionOld = historyPosition;
            historyPosition = Math.max(historyPosition - 1, 0);
            if(historyPositionOld > 0 && historyPosition <= 0) {
                viewFlipper.showNext();
            }
            if(historyPosition > 0) {
                updateHistoryView();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUpdateNextAnswered = true;

        repository = Repository.getInstance();
        mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keep_screen_on = sharedPref.getBoolean("pref_keep_screen_on", false);
        if (keep_screen_on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        showStandardView();

        viewFlipper = findViewById(R.id.questionAskerViewFlipper);
        viewFlipper.setInAnimation(this, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(this, android.R.anim.fade_out);
        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        gestureDetector = new GestureDetector(this, customGestureDetector);

        final Button backToQuestionButton = findViewById(R.id.backToQuestionButton);
        backToQuestionButton.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                historyPosition = 0;
                viewFlipper.showNext();
            }
        });

        final TextView referenceText = findViewById(R.id.referenceText);
        this.oldTextColor = referenceText.getTextColors();

        if (savedInstanceState != null) {
            topicId = (int) savedInstanceState.getLong(getClass().getName() + ".topic");
            currentQuestionState = savedInstanceState.getParcelable(getClass().getName() + ".currentQuestionState");
            final long nextTimeLong = savedInstanceState.getLong(getClass().getName() + ".nextTime");
            nextTime = nextTimeLong > 0L ? new Date(nextTimeLong) : null;
            showingCorrectAnswer = savedInstanceState.getBoolean(getClass().getName() + ".showingCorrectAnswer");
            maxProgress = savedInstanceState.getInt(getClass().getName() + ".maxProgress");
            currentProgress = savedInstanceState.getInt(getClass().getName() + ".currentProgress");

            historyPosition = savedInstanceState.getInt(getClass().getName() + ".historyPosition");
            history = savedInstanceState.getParcelableArrayList(getClass().getName() + ".history");

            showQuestion();

            if (historyPosition > 0) {
                viewFlipper.showPrevious();
                updateHistoryView();
            }
        } else {
            Bundle intbundle = getIntent().getExtras();
            if(intbundle != null) {
                topicId = (int) getIntent().getExtras().getLong(getClass().getName() + ".topic");
                if (topicId != 0) {
                    nextQuestion();
                } else {
                    int questionId = getIntent().getExtras().getInt(getClass().getName() + ".questionId");
                    // String questionReference = getIntent().getExtras().getString(getClass().getName() + ".questionReference");
                    nextQuestion(questionId);
                }
            } else {
                int lastQuestionShown = mPrefs.getInt("last_question_shown", 1);
                nextQuestion(lastQuestionShown);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        if (currentQuestionState != null) {
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putInt("last_question_shown", currentQuestionState.getQuestionId());
            ed.apply();
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
            case R.id.showFormelsammlung:
                final Intent intentFormelsammlung = new Intent(this, FormelsammlungViewerActivity.class);
                startActivity(intentFormelsammlung);
                return true;
            case R.id.showLichtblick:
                final Intent intentLichtblick = new Intent(this, LichtblickeViewerActivity.class);
                final Question question = currentQuestionState.getQuestion(repository);
                final int lichtblickPage = question.getLichtblickPage();
                intentLichtblick.putExtra(LichtblickeViewerActivity.class.getName() + ".lichtblickPage", lichtblickPage);
                startActivity(intentLichtblick);
                return true;
//            case R.id.reportError:
//                final StringBuilder uri = new StringBuilder();
//			    uri.append("http://funktrainer.hosenhasser.de/app/reportError?view=QuestionAsker&Reference=" + Integer.toString(this.currentQuestion));
//			    final Intent intent1 = new Intent(Intent.ACTION_VIEW);
//		    	intent1.setData(Uri.parse(uri.toString()));
//	    		startActivity(intent1);
//    			return true;
//			case R.id.help:
//				final StringBuilder uri = new StringBuilder();
//				uri.append("http://funktrainer.hosenhasser.de/app/help?question=");
//				uri.append(currentQuestion);
//				uri.append("&topic=");
//				uri.append(topicId);
//				uri.append("&view=QuestionAsker");
//				final Intent intent2 = new Intent(Intent.ACTION_VIEW);
//				intent2.setData(Uri.parse(uri.toString()));
//				startActivity(intent2);
//				return true;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        return gestureDetector.onTouchEvent(event);
    }

    private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;

        ProgressBar progress = findViewById(R.id.progressBar1);
        progress.setMax(5);

        final QuestionView questionView = findViewById(R.id.questionView);
        final Button contButton = findViewById(R.id.button1);
        final Button skipButton = findViewById(R.id.skipButton);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean show_skip = sharedPref.getBoolean("pref_show_skip_button", false);
        if (show_skip) {
            skipButton.setVisibility(View.VISIBLE);
            skipButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    questionView.setRadioGroupEnabled(true);
                    nextQuestion();
                }
            });
        } else {
            skipButton.setVisibility(View.INVISIBLE);
        }

        contButton.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                // find what has been selected
                if (showingCorrectAnswer) {
                    showingCorrectAnswer = false;
                    questionView.setRadioGroupEnabled(true);
                    nextQuestion();
                    return;
                }

                history.add(questionView.getQuestionState());

                if(history.size() > MAX_HISTORY_LENGTH) {
                    history.remove(0);
                }

                QuestionState state = questionView.getQuestionState();
                int currentQuestionId = state.getQuestion(repository).getId();

                if (state.isCorrect()) {
                    Toast.makeText(AdvancedQuestionAsker.this, getString(R.string.right), Toast.LENGTH_SHORT).show();

                    if(mUpdateNextAnswered) {
                        repository.answeredCorrect(currentQuestionId);
                    }
                    mUpdateNextAnswered = true;

                    nextQuestion();

                    // return;
                } else if (state.hasAnswer()) {
                    if(mUpdateNextAnswered) {
                        repository.answeredIncorrect(currentQuestionId);
                    }
                    mUpdateNextAnswered = true;

                    showingCorrectAnswer = true;
                    questionView.setEnabled(false);
                    questionView.showCorrectAnswer();

                    // return;
                } else {
                    Toast.makeText(AdvancedQuestionAsker.this, getString(R.string.noAnswerSelected), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void nextQuestion() {
        this.nextQuestion(null, -1);
    }

    private void nextQuestion(final int questionId) {
        this.nextQuestion(null, questionId);
    }

    private void nextQuestion(final String questionReference, final int questionId) {
        if (!showingStandardView) {
            showStandardView();
        }

        final ScrollView scrollView = findViewById(R.id.questionAskerScrollView);
        scrollView.fullScroll(View.FOCUS_UP);

        final Button contButton = findViewById(R.id.button1);
        contButton.setEnabled(false);

        QuestionSelection nextQuestion;
        if (questionReference != null) {
            nextQuestion = repository.selectQuestionByReference(questionReference);
            topicId = repository.getFirstTopicIdForQuestionReference(questionReference);
            this.mUpdateNextAnswered = false;
        } else if(questionId != -1) {
            nextQuestion = repository.selectQuestionById(questionId);
            topicId = repository.getFirstTopicIdForQuestionId(questionId);
        } else {
            nextQuestion = repository.selectQuestionByTopicId(topicId);
        }

        // any question?
        final int selectedQuestion = nextQuestion.getSelectedQuestion();
        if (selectedQuestion != 0) {
            currentQuestionState = new QuestionState(selectedQuestion);
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

        final Button restartTopicButton = findViewById(R.id.restartTopic);
        restartTopicButton.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                restartTopic();
                nextQuestion();
            }

        });
        // return;
    }

    private void showQuestion() {
        if (nextTime != null) {
            showingStandardView = false;
            setContentView(R.layout.no_more_questions_wait);

            final TextView nextTimeText = findViewById(R.id.nextTimeText);
            if (nextTime.getTime() - new Date().getTime() < 64800000L) {
                nextTimeText.setText(DateFormat.getTimeInstance().format(nextTime));
            } else {
                nextTimeText.setText(DateFormat.getDateTimeInstance().format(nextTime));
            }
            showNextQuestionAt(nextTime);

            final Button resetWaitButton = findViewById(R.id.resetWait);
            resetWaitButton.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    cancelTimer();
                    repository.continueNow(topicId);
                    nextQuestion();
                    // return;
                }

            });
            return;
        }

        final Question question = currentQuestionState.getQuestion(repository);

        final TextView levelText = findViewById(R.id.levelText);
        levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
                question.getLevel() == 1 ? getString(R.string.secondPass) :
                        question.getLevel() == 2 ? getString(R.string.thirdPass) :
                                question.getLevel() == 3 ? getString(R.string.fourthPass) :
                                        question.getLevel() == 4 ? getString(R.string.fifthPass) :
                                                String.format(getString(R.string.passText), question.getLevel()));

        final TextView referenceText = findViewById(R.id.referenceText);
        referenceText.setText(question.getReference());
        if(question.getOutdated()) {
            referenceText.setTextColor(Color.RED);
        } else {
            referenceText.setTextColor(this.oldTextColor);
        }

        final QuestionView questionView = findViewById(R.id.questionView);
        questionView.setQuestionState(currentQuestionState);
        final Button contButton = findViewById(R.id.button1);
        questionView.getQuestionState().addQuestionStateListener(new QuestionState.QuestionStateListener() {
            @Override
            public void onAnswerSelected(int answer) {
                contButton.setEnabled(answer >= 0);
            }
        });

        contButton.setEnabled(currentQuestionState.hasAnswer());


        final ProgressBar progressBar = findViewById(R.id.progressBar1);
        progressBar.setMax(maxProgress);
        progressBar.setProgress(currentProgress);
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
}
