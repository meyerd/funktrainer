/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *            2015 Dominik Meyer
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
import java.util.LinkedList;
import java.util.List;

public class Question {
	private int id;
//	private int topicId;
	private int categoryId;
	private String reference;
	private String question;
	private List<String> answers = new LinkedList<String>();
	private List<String> answersHelp = new LinkedList<String>();
	private int level;
	private Date nextTime;
	private int wrong;
	private int correct;
	private String help;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
//	public int getTopicId() {
//		return topicId;
//	}
//	public void setTopicId(int topicId) {
//		this.topicId = topicId;
//	}
	public int getCategoryId() { return categoryId; }
	public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getQuestion() {
		return question;
	}
	public void setQuestion(String question) {
		this.question = question;
	}
	public List<String> getAnswers() {
		return answers;
	}
	public void setAnswers(List<String> answers) {
		this.answers = answers;
	}
	public List<String> getAnswersHelp() {
		return answersHelp;
	}
	public void setAnswersHelp(List<String> answersHelp) {
		this.answersHelp = answersHelp;
	}
	public int getLevel() {
		return level;
	}
	public Date getNextTime() { return nextTime; }
	public void setNextTime(Date nextTime) {
		this.nextTime = nextTime;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public int getWrong() { return wrong; }
	public void setWrong(int wrong) {
		this.wrong = wrong;
	}
	public int getCorrect() { return correct; }
	public void setCorrect(int correct) { this.correct = correct; }
	public String getHelp() { return help; }
	public void setHelp(String help) { this.help = help; }
}
