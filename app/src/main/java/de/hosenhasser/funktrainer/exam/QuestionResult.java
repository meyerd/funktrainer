package de.hosenhasser.funktrainer.exam;

import java.io.Serializable;

import de.hosenhasser.funktrainer.data.Question;

public class QuestionResult implements Serializable {
    private String questionReference;
    private boolean result;

    public QuestionResult(Question q, boolean r) {
        this.questionReference = q.getReference();
        this.result = r;
    }

    public String getReference() {
        return questionReference;
    }

    public boolean getResult() {
        return result;
    }
}
