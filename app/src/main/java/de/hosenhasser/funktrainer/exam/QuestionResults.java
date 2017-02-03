package de.hosenhasser.funktrainer.exam;

import java.io.Serializable;
import java.util.ArrayList;
import de.hosenhasser.funktrainer.data.ExamSettings;

public class QuestionResults implements Serializable {
    private ArrayList<QuestionResultEntry> results;
    private ExamSettings examSettings;

    public QuestionResults(final ArrayList<QuestionResultEntry> rl, final ExamSettings e) {
        this.results = rl;
        this.examSettings = e;
    }

    public ArrayList<QuestionResultEntry> getResults() {
        return results;
    }

    public ExamSettings getExamSettings() {
        return examSettings;
    }
}