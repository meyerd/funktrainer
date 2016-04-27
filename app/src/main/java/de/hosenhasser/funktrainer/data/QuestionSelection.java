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

package de.hosenhasser.funktrainer.data;

import java.util.Date;

public class QuestionSelection {
	private int selectedQuestion;
	private boolean finished;
	private Date nextQuestion;
	private int openQuestions;
	private int totalQuestions;
	private int maxProgress;
	private int currentProgress;
	private int topicId;

	public int getSelectedQuestion() {
		return selectedQuestion;
	}
	public void setSelectedQuestion(final int selectedQuestion) {
		this.selectedQuestion = selectedQuestion;
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(final boolean finished) {
		this.finished = finished;
	}
	public Date getNextQuestion() {
		return nextQuestion;
	}
	public void setNextQuestion(final Date nextQuestion) {
		this.nextQuestion = nextQuestion;
	}
	public int getOpenQuestions() {
		return openQuestions;
	}
	public void setOpenQuestions(final int openQuestions) {
		this.openQuestions = openQuestions;
	}
	public int getTotalQuestions() {
		return totalQuestions;
	}
	public void setTotalQuestions(final int totalQuestions) {
		this.totalQuestions = totalQuestions;
	}
	public int getMaxProgress() {
		return maxProgress;
	}
	public void setMaxProgress(final int maxProgress) {
		this.maxProgress = maxProgress;
	}
	public int getCurrentProgress() {
		return currentProgress;
	}
	public void setCurrentProgress(final int currentProgress) {
		this.currentProgress = currentProgress;
	}
	public int getTopicId() { return topicId; }
	public void setTopicId(int topicId) { this.topicId = topicId; }
}
