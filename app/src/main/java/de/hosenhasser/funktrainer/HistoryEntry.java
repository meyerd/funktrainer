package de.hosenhasser.funktrainer;

import java.util.List;

public class HistoryEntry {
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
