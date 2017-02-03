package de.hosenhasser.funktrainer.data;

import java.io.Serializable;

public class ExamSettings implements Serializable {
    private int id;
    private int topicId;
    private int nQuestions;
    private int nRequired;
    private long nSecondsAvailable;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getnQuestions() {
        return nQuestions;
    }

    public void setnQuestions(int nQuestions) {
        this.nQuestions = nQuestions;
    }

    public int getnRequired() {
        return nRequired;
    }

    public void setnRequired(int nRequired) {
        this.nRequired = nRequired;
    }

    public long getnSecondsAvailable() {
        return nSecondsAvailable;
    }

    public void setnSecondsAvailable(long nSecondsAvailable) {
        this.nSecondsAvailable = nSecondsAvailable;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }
}
