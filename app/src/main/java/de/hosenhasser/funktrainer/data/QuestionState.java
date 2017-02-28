package de.hosenhasser.funktrainer.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestionState implements Serializable, Parcelable {
    private static final String TAG = QuestionState.class.getName();

    private QuestionState(Parcel in) {
        questionId = in.readInt();
        in.readList(order, QuestionState.class.getClassLoader());
        answer = in.readInt();
        listeners = new ArrayList<>();
    }

    public static final Creator<QuestionState> CREATOR = new Creator<QuestionState>() {
        @Override
        public QuestionState createFromParcel(Parcel in) {
            return new QuestionState(in);
        }

        @Override
        public QuestionState[] newArray(int size) {
            return new QuestionState[size];
        }
    };

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(questionId);
        parcel.writeList(order);
        parcel.writeInt(answer);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        listeners = new ArrayList<>();
    }

    public int getAnswer() {
        return answer;
    }

    public static interface QuestionStateListener {
        void onAnswerSelected(int answer);
    }

    private transient Question question;
    private int questionId;
    private List<Integer> order;
    private int answer = -1;
    private transient List<QuestionStateListener> listeners;

    private static final Random rand = new Random();

    public QuestionState(Question q) {
        this(q.getId());
        this.question = q;
    }

    public QuestionState(int questionId) {
        this.questionId = questionId;
        listeners = new ArrayList<>();

        this.order = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            order.add(rand.nextInt(order.size() + 1), i);
        }

        Log.d(TAG, "generated new order: " + order);
    }

    public int getQuestionId() {
        return questionId;
    }

    public Question getQuestion(Repository repository) {
        if (question == null) {
            question = repository.getQuestion(questionId);
        }
        return question;
    }

    public List<Integer> getOrder() {
        return order;
    }

    public boolean hasAnswer() {
        return answer >= 0;
    }

    public boolean isCorrect() {
        return getCorrectAnswer() == answer;
    }

    public void setAnswer(int answer) {
        Log.d(TAG, "selected answer: " + answer);
        this.answer = answer;
        for (QuestionStateListener l : listeners) l.onAnswerSelected(answer);
    }

    public int getCorrectAnswer() {
        return order.get(0);
    }

    public void addQuestionStateListener(QuestionStateListener l) {
        listeners.add(l);
    }

    public void removeQuestionStateListener(QuestionStateListener l) {
        listeners.remove(l);
    }
}
