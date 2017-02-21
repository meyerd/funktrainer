package de.hosenhasser.funktrainer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

public class HistoryEntry implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
//        private String questionText;
//        private String helpText;
//        private String referenceText;
//        private List<String> answersText;
//        private List<String> answersHelpText;
//        private List<Integer> order;
//        private int correctAnswer;
//        private int answerGiven;
        pc.writeInt(flags);
        pc.writeString(questionText);
        pc.writeString(helpText);
        pc.writeString(referenceText);
        pc.writeStringList(answersText);
        pc.writeStringList(answersHelpText);
        int[] ordertmp = new int[order.size()];
        for (int i = 0; i < order.size(); i++) {
            ordertmp[i] = order.get(i);
        }
        pc.writeInt(order.size());
        pc.writeIntArray(ordertmp);
        pc.writeInt(correctAnswer);
        pc.writeInt(answerGiven);
    }

    public static final Parcelable.Creator<HistoryEntry> CREATOR = new Parcelable.Creator<HistoryEntry>() {
        public HistoryEntry createFromParcel(Parcel pc) {
            return new HistoryEntry(pc);
        }
        public HistoryEntry[] newArray(int size) {
            return new HistoryEntry[size];
        }
    };

    public HistoryEntry() {

    }

    public HistoryEntry(Parcel pc) {
        int flags = pc.readInt();
        questionText = pc.readString();
        helpText = pc.readString();
        referenceText = pc.readString();
        pc.readStringList(answersText);
        pc.readStringList(answersHelpText);
        final int orderlen = pc.readInt();
        int[] ordertmp = new int[orderlen];
        pc.readIntArray(ordertmp);
        order = new LinkedList<Integer>();
        for (int i = 0; i < ordertmp.length; i++) {
            order.add(ordertmp[i]);
        }
        correctAnswer = pc.readInt();
        answerGiven = pc.readInt();
    }
}
