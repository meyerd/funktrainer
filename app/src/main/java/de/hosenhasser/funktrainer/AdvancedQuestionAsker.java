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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.ArrayMap;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.QuestionSelection;
import de.hosenhasser.funktrainer.data.Repository;

class HistoryEntry {
    private String questionText;
    private String helpText;
    private String referenceText;
    private List<String> answersText;
    private List<String> answersHelpText;
    private List<Integer> order;
    private int correctAnswer;
    private int answerGiven;

    public String getQuestionText() {return questionText;}
    public void setQuestionText(String questionText) {this.questionText = questionText;}
    public String getHelpText() {return helpText; }
    public void setHelpText(String helpText) {this.helpText = helpText;}
    public String getReferenceText() {return referenceText;}
    public void setReferenceText(String referenceText) {this.referenceText=referenceText;}
    public List<String> getAnswersText() {return answersText;}
    public void setAnswersText(List<String> answersText) {this.answersText = answersText;}
    public List<String> getAnswersHelpText() {return answersHelpText;}
    public List<Integer> getOrder() {return order;}
    public void setOrder(List<Integer> order) {this.order=order;}
    public void setAnswersHelpText(List<String> answersHelpText) {this.answersHelpText = answersHelpText;}
    public int getCorrectAnswer() {return correctAnswer;}
    public void setCorrectAnswer(int correctAnswer) {this.correctAnswer=correctAnswer;}
    public int getAnswerGiven() {return answerGiven;}
    public void setAnswerGiven(int answerGiven) {this.answerGiven=answerGiven;}
}

public class AdvancedQuestionAsker extends Activity {
    private Repository repository;
    private int currentQuestion;
    private int currentQuestionId;
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

    private HashMap<Integer, Integer> radioButtonIdToPositionMap = new HashMap<Integer, Integer>();

    private SharedPreferences mPrefs;

    private ViewFlipper viewFlipper;
    private GestureDetector gestureDetector;

    private int historyPosition = 0;
    private LinkedList<HistoryEntry> history = new LinkedList<HistoryEntry>();

    private static final int MAX_HISTORY_LENGTH = 30;

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

    class CustomGestureDetector extends GestureDetector.SimpleOnGestureListener {
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
        final HistoryEntry histentry = history.get(history.size() - historyPosition);
        TextView oldQuestionTextNumber = (TextView)findViewById(R.id.oldQuestionHeatTextNumber);
        oldQuestionTextNumber.setText(Integer.toString(-historyPosition));

        final TextView referenceText = (TextView) findViewById(R.id.referenceTextold);
        referenceText.setText(histentry.getReferenceText());

        final TextView textView = (TextView) findViewById(R.id.textViewFrageold);
        URLImageParser p = new URLImageParser(textView, this);
        Spanned htmlSpan = Html.fromHtml(safeText(histentry.getQuestionText()), p, null);
        textView.setText(htmlSpan);

        final List<RadioButton> localRadioButtons = getRadioButtonsOld();

        final List<Integer> historder = histentry.getOrder();


        for (int i = 0; i < 4; i++) {
            RadioButton rb = localRadioButtons.get(historder.get(i));
            rb.setSelected(false);
            rb.setChecked(false);
            rb.setBackgroundResource(R.color.defaultBackground);
            rb.setClickable(false);
//            rb.setWidth(rblayoutwidth - 6);
            URLImageParser p_rb = new URLImageParser(rb, this);
            Spanned htmlSpan_rb = Html.fromHtml(safeText(histentry.getAnswersText().get(i)), p_rb, null);
            rb.setText(htmlSpan_rb);
        }
        LinearLayout rblayout = (LinearLayout) findViewById(R.id.linearLayout1old);
        rblayout.measure(0, 0);
        int rblayoutwidth = rblayout.getWidth();

        final RadioGroup rgroup = (RadioGroup)findViewById(R.id.radioGroup1old);
        rgroup.setEnabled(false);

        final RadioButton correctButton = localRadioButtons.get(historder.get(histentry.getCorrectAnswer()));
        correctButton.setBackgroundResource(R.color.correctAnswer);
        final RadioButton chosenButton = localRadioButtons.get(histentry.getAnswerGiven());
        chosenButton.setChecked(true);
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

        repository = new Repository(this);
        mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);

        showStandardView();

        viewFlipper = (ViewFlipper)findViewById(R.id.questionAskerViewFlipper);
        viewFlipper.setInAnimation(this, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(this, android.R.anim.fade_out);
        CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        gestureDetector = new GestureDetector(this, customGestureDetector);

        final Button backToQuestionButton = (Button)findViewById(R.id.backToQuestionButton);
        backToQuestionButton.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                historyPosition = 0;
                viewFlipper.showNext();
            }
        });

        // build radio button id to position map
        radioButtonIdToPositionMap.put((findViewById(R.id.radio0)).getId(), 0);
        radioButtonIdToPositionMap.put((findViewById(R.id.radio1)).getId(), 1);
        radioButtonIdToPositionMap.put((findViewById(R.id.radio2)).getId(), 2);
        radioButtonIdToPositionMap.put((findViewById(R.id.radio3)).getId(), 3);


        if (savedInstanceState != null) {
            topicId = (int) savedInstanceState.getLong(getClass().getName() + ".topic");
            currentQuestion = savedInstanceState.getInt(getClass().getName() + ".currentQuestion");
            final long nextTimeLong = savedInstanceState.getLong(getClass().getName() + ".nextTime");
            nextTime = nextTimeLong > 0L ? new Date(nextTimeLong) : null;
            showingCorrectAnswer = savedInstanceState.getBoolean(getClass().getName() + ".showingCorrectAnswer");
            maxProgress = savedInstanceState.getInt(getClass().getName() + ".maxProgress");
            currentProgress = savedInstanceState.getInt(getClass().getName() + ".currentProgress");

            final String orderString = savedInstanceState.getString(getClass().getName() + ".order");
            if (orderString != null) {
                final String[] orderArray = orderString.split(",");
                order = new LinkedList<Integer>();
                for (String s : orderArray) {
                    order.add(Integer.parseInt(s));
                }
            }

            showQuestion();
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
                currentQuestionId = lastQuestionShown;
                nextQuestion(lastQuestionShown);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("last_question_shown", currentQuestionId);
        ed.commit();
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
            case R.id.showLichtblick:
                final Intent intentLichtblick = new Intent(this, LichtblickeViewerActivity.class);
                final Question question = repository.getQuestion(currentQuestion);
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
        boolean handled = super.dispatchTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event);
        return handled;
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

                final Question question = repository.getQuestion(currentQuestion);
                HistoryEntry histentry = new HistoryEntry();
                histentry.setReferenceText(question.getReference());
                histentry.setQuestionText(question.getQuestion());
                histentry.setHelpText(question.getHelp());
                histentry.setAnswersText(question.getAnswers());
                histentry.setAnswersHelpText(question.getAnswersHelp());
                histentry.setCorrectAnswer(0);
                LinkedList<Integer> historder = new LinkedList<Integer>();
                for(int i = 0; i < order.size(); i++) {
                    historder.add(order.get(i));
                }
                histentry.setOrder(historder);

                int selectedButton = radioGroup.getCheckedRadioButtonId();

                histentry.setAnswerGiven(radioButtonIdToPositionMap.get(selectedButton));
                history.add(histentry);

                if(history.size() > MAX_HISTORY_LENGTH) {
                    history.remove();
                }

                if (selectedButton == correctChoice) {
                    Toast.makeText(AdvancedQuestionAsker.this, getString(R.string.right), Toast.LENGTH_SHORT).show();

                    repository.answeredCorrect(currentQuestion);

                    nextQuestion();

                    // return;
                } else if (selectedButton != -1) {
                    repository.answeredIncorrect(currentQuestion);

                    showingCorrectAnswer = true;
                    radioGroup.setEnabled(false);

                    final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
                    correctButton.setBackgroundResource(R.color.correctAnswer);

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

    private void nextQuestion(final String questionReference) {
        this.nextQuestion(questionReference, -1);
    }

    private void nextQuestion(final String questionReference, final int questionId) {
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

        QuestionSelection nextQuestion;
        if (questionReference != null) {
            nextQuestion = repository.selectQuestionByReference(questionReference);
            topicId = repository.getFirstTopicIdForQuestionReference(questionReference);
        } else if(questionId != -1) {
            nextQuestion = repository.selectQuestionById(questionId);
            topicId = repository.getFirstTopicIdForQuestionId(questionId);
        } else {
            nextQuestion = repository.selectQuestionByTopicId(topicId);
        }

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
        // return;
    }

    private List<RadioButton> getRadioButtons() {
        final List<RadioButton> radioButtons = new LinkedList<RadioButton>();
        radioButtons.add((RadioButton) findViewById(R.id.radio0));
        radioButtons.add((RadioButton) findViewById(R.id.radio1));
        radioButtons.add((RadioButton) findViewById(R.id.radio2));
        radioButtons.add((RadioButton) findViewById(R.id.radio3));
        return radioButtons;
    }

    private List<RadioButton> getRadioButtonsOld() {
        final List<RadioButton> radioButtons = new LinkedList<RadioButton>();
        radioButtons.add((RadioButton) findViewById(R.id.radio0old));
        radioButtons.add((RadioButton) findViewById(R.id.radio1old));
        radioButtons.add((RadioButton) findViewById(R.id.radio2old));
        radioButtons.add((RadioButton) findViewById(R.id.radio3old));
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
                    // return;
                }

            });
            return;
        }

        final Question question = repository.getQuestion(currentQuestion);
        currentQuestionId = question.getId();

        final TextView levelText = (TextView) findViewById(R.id.levelText);
        levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
                question.getLevel() == 1 ? getString(R.string.secondPass) :
                        question.getLevel() == 2 ? getString(R.string.thirdPass) :
                                question.getLevel() == 3 ? getString(R.string.fourthPass) :
                                        question.getLevel() == 4 ? getString(R.string.fifthPass) :
                                                String.format(getString(R.string.passText), question.getLevel()));

        final TextView referenceText = (TextView) findViewById(R.id.referenceText);
        referenceText.setText(question.getReference());

        final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        URLImageParser p = new URLImageParser(textView, this);
        Spanned htmlSpan = Html.fromHtml(safeText(question.getQuestion()), p, null);
        textView.setText(htmlSpan);

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
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setMax(maxProgress);
        progressBar.setProgress(currentProgress);

//        // remove previous question image if any
//        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
//        for (int i = 0; i < linearLayout.getChildCount(); i++) {
//            final View childAtIndex = linearLayout.getChildAt(i);
//            if (childAtIndex instanceof ImageView) {
//                linearLayout.removeViewAt(i);
//                break;
//            }
//        }
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
