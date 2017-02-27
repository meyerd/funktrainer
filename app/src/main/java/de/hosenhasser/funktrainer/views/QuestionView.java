package de.hosenhasser.funktrainer.views;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import java.util.Random;

import de.hosenhasser.funktrainer.HistoryEntry;
import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.URLImageParser;
import de.hosenhasser.funktrainer.data.Question;
import de.hosenhasser.funktrainer.exam.QuestionResultEntry;

public class QuestionView extends LinearLayout {
    private boolean replaceNNBSP = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private RadioGroup radioGroup;
    private TextView questionTextView;
    private List<Integer> order;
    private Random rand = new Random();
    private int correctChoice;
    private Question question;
    private int listPosition;

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
        this.radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        questionTextView = (TextView) findViewById(R.id.textViewQuestion);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        QuestionViewSavedState ss = (QuestionViewSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).restoreHierarchyState(ss.childrenStates);
        }
        this.order = ss.order;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        QuestionViewSavedState ss = new QuestionViewSavedState(superState);
        ss.order = this.order;
        for (int i =0; i < getChildCount(); i++) {
            getChildAt(i).saveHierarchyState(ss.childrenStates);
        }
        return ss;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    static class QuestionViewSavedState extends BaseSavedState {
        List<Integer> order;
        SparseArray childrenStates = new SparseArray<>();

        QuestionViewSavedState(Parcelable superState) {
            super(superState);
        }

        private QuestionViewSavedState(Parcel in, ClassLoader classLoader) {
            super(in);
            childrenStates = in.readSparseArray(classLoader);
            final String orderString = in.readString();
            if (orderString != null && !orderString.equals("")) {
                final String[] orderArray = orderString.split(",");
                order = new LinkedList<Integer>();
                for (String s : orderArray) {
                    order.add(Integer.parseInt(s));
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeSparseArray(childrenStates);
            if (order != null) {
                final StringBuilder orderString = new StringBuilder();
                for (int i = 0; i < order.size(); i++) {
                    if (i > 0) {
                        orderString.append(',');
                    }
                    orderString.append(order.get(i));
                }
                out.writeString(orderString.toString());
            } else {
                out.writeString("");
            }
        }

        public static final ClassLoaderCreator<QuestionViewSavedState> CREATOR
                = new ClassLoaderCreator<QuestionViewSavedState>() {
            @Override
            public QuestionViewSavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new QuestionViewSavedState(in, loader);
            }

            @Override
            public QuestionViewSavedState createFromParcel(Parcel in) {
                return createFromParcel(null);
            }

            public QuestionViewSavedState[] newArray(int size) {
                return new QuestionViewSavedState[size];
            }
        };
    }

    public void setListPosition(final int listPosition) {
        this.listPosition = listPosition;
    }

    public void setOnRadioCheckedListener(RadioGroup.OnCheckedChangeListener l) {
        radioGroup.setOnCheckedChangeListener(l);
    }

    public int getCheckedRadioButtonId() {
        return radioGroup.getCheckedRadioButtonId();
    }

    public int getPositionOfButton(int id) {
        RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup1);
        for (int i = 0; i < 4; i++) {
            if (group.getChildAt(i).getId() == id) return i;
        }
        return -1;
    }

    public void setRadioGroupEnabled(boolean enabled) {
        radioGroup.setEnabled(enabled);
    }

    public void setQuestion(Question q) {
        question = q;

        setQuestionText(q.getQuestion());

        final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        final List<RadioButton> radioButtons = getRadioButtons();

        if(order == null) {
            order = new LinkedList<Integer>();

            for (int i = 0; i < 4; i++) {
                order.add(rand.nextInt(order.size() + 1), i);
            }
        }
        correctChoice = radioButtons.get(order.get(0)).getId();
        for (int i = 0; i < 4; i++) {
            RadioButton rb = radioButtons.get(order.get(i));
            URLImageParser p_rb = new URLImageParser(rb, getContext());
            Spanned htmlSpan_rb = Html.fromHtml(safeText(q.getAnswers().get(i)), p_rb, null);
            rb.setText(htmlSpan_rb);
        }

        for (RadioButton b: radioButtons) {
            // reset default color
            b.setBackgroundResource(0);
        }

        radioGroup.clearCheck();
    }

    public void setHistoryEntry(HistoryEntry e) {
        setQuestionText(e.getQuestionText());

        final List<RadioButton> localRadioButtons = getRadioButtons();

        final List<Integer> historder = e.getOrder();

        for (int i = 0; i < 4; i++) {
            RadioButton rb = localRadioButtons.get(historder.get(i));
            rb.setSelected(false);
            rb.setChecked(false);
            rb.setBackgroundResource(R.color.defaultBackground);
            rb.setClickable(false);
//            rb.setWidth(rblayoutwidth - 6);
            URLImageParser p_rb = new URLImageParser(rb, getContext());
            Spanned htmlSpan_rb = Html.fromHtml(safeText(e.getAnswersText().get(i)), p_rb, null);
            rb.setText(htmlSpan_rb);
        }

        final RadioButton correctButton = localRadioButtons.get(historder.get(e.getCorrectAnswer()));
        correctButton.setBackgroundResource(R.color.correctAnswer);
        final RadioButton chosenButton = localRadioButtons.get(e.getAnswerGiven());
        chosenButton.setChecked(true);

        setRadioGroupEnabled(false);
    }

    public int getCorrectChoice() {
        return correctChoice;
    }

    public List<Integer> getOrder() {
        return order;
    }

    public Question getQuestion() {
        return question;
    }

    public boolean isCorrect() {
        return getCheckedRadioButtonId() == getCorrectChoice();
    }

    public QuestionResultEntry getResult() {
        return new QuestionResultEntry(question, isCorrect());
    }

    public void showCorrectAnswer() {
        final RadioButton correctButton = (RadioButton) findViewById(getCorrectChoice());
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
        final List<RadioButton> radioButtons = new LinkedList<RadioButton>();
        radioButtons.add((RadioButton) findViewById(R.id.radio0));
        radioButtons.add((RadioButton) findViewById(R.id.radio1));
        radioButtons.add((RadioButton) findViewById(R.id.radio2));
        radioButtons.add((RadioButton) findViewById(R.id.radio3));
        return radioButtons;
    }
}
