package de.hosenhasser.funktrainer.exam;

import java.io.Serializable;
import java.util.List;

import de.hosenhasser.funktrainer.data.ExamSettings;
import de.hosenhasser.funktrainer.data.QuestionState;

public class QuestionResults implements Serializable {
    private List<QuestionState> results;
    private ExamSettings examSettings;

    public QuestionResults(final List<QuestionState> rl, final ExamSettings e) {
        this.results = rl;
        this.examSettings = e;
    }

    public List<QuestionState> getResults() {
        return results;
    }

    public ExamSettings getExamSettings() {
        return examSettings;
    }
}