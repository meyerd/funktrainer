package de.hosenhasser.funktrainer.views;

import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.URLImageParser;
import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.data.QuestionState;
import de.hosenhasser.funktrainer.data.Repository;

public class QuestionView extends LinearLayout {
    private boolean replaceNNBSP = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private RadioGroup radioGroup;
    private TextView questionTextView;
    private QuestionState questionState;
    private int listPosition;
    private Repository repository;

    public QuestionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuestionView(Context context) {
        super(context);
        init();
    }

    public QuestionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        listPosition = -1;
        inflate(getContext(), R.layout.question, this);
        this.radioGroup = findViewById(R.id.radioGroup1);
        questionTextView = findViewById(R.id.textViewQuestion);
        repository = new Repository(getContext());

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                questionState.setAnswer(getPositionOfButton(i));
            }
        });
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    public void setListPosition(final int listPosition) {
        this.listPosition = listPosition;
    }

    public int getPositionOfButton(int id) {
        for (int i = 0; i < 4; i++) {
            if (radioGroup.getChildAt(i).getId() == id) return i;
        }
        return -1;
    }


    public void setRadioGroupEnabled(boolean enabled) {
        radioGroup.setEnabled(enabled);
        for (RadioButton b : getRadioButtons()) b.setEnabled(enabled);
    }

    public void setQuestionState(QuestionState qs) {
        this.questionState = qs;
        Question q = qs.getQuestion(repository);

        setQuestionText(q.getQuestion());

        final List<RadioButton> radioButtons = getRadioButtons();

        for (int i = 0; i < 4; i++) {
            RadioButton rb = radioButtons.get(qs.getOrder().get(i));
            URLImageParser p_rb = new URLImageParser(rb, getContext());
            Spanned htmlSpan_rb = Html.fromHtml(safeText(q.getAnswers().get(i)), p_rb, null);
            rb.setText(htmlSpan_rb);
        }

        for (RadioButton b: radioButtons) {
            // reset default color
            b.setBackgroundResource(0);
        }

        if (qs.hasAnswer()) {
            radioGroup.check(getRadioButtons().get(qs.getAnswer()).getId());
        } else {
            radioGroup.clearCheck();
        }
    }

    public QuestionState getQuestionState() {
        return questionState;
    }

    public void showCorrectAnswer() {
        final RadioButton correctButton = getRadioButtons().get(questionState.getCorrectAnswer());
        correctButton.setBackgroundResource(R.color.correctAnswer);
    }

    private void setQuestionText(String text) {
        URLImageParser p = new URLImageParser(questionTextView, getContext());
        if(this.listPosition > 0) {
            text = this.listPosition + ") " + text;
        }
        Spanned htmlSpan = Html.fromHtml(safeText(text), p, null);
        questionTextView.setText(htmlSpan);
    }

    private String safeText(final String source) {
        return replaceNNBSP && source != null ? source.replace('\u202f', '\u00a0') : source;
    }

    private List<RadioButton> getRadioButtons() {
        final List<RadioButton> radioButtons = new LinkedList<>();
        radioButtons.add((RadioButton) findViewById(R.id.radio0));
        radioButtons.add((RadioButton) findViewById(R.id.radio1));
        radioButtons.add((RadioButton) findViewById(R.id.radio2));
        radioButtons.add((RadioButton) findViewById(R.id.radio3));
        return radioButtons;
    }
}
