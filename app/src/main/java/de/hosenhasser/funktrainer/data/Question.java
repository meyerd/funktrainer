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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Question implements Parcelable {
	private int id = 0;
	private int topicId = 0;
	private int categoryId = 0;
	private String reference = "";
	private String question = "";
	private List<String> answers = new LinkedList<String>();
	private List<String> answersHelp = new LinkedList<String>();
	private int level = 0;
	private Date nextTime = new Date();
	private int wrong = 0;
	private int correct = 0;
	private String help = "";
	private int lichtblickPage = 0;
	private LichtblickType lichtblickType = LichtblickType.NONE;
	private int correctAnswer = 0;
	private boolean outdated = false;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getTopicId() {
		return topicId;
	}
	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}
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
	public int getLichtblickPage() { return lichtblickPage; }
	public void setLichtblickPage(int lichtblickPage) { this.lichtblickPage = lichtblickPage; }
	public LichtblickType getLichtblickType() { return lichtblickType; }
	public void setLichtblickType(LichtblickType lichtblickType) { this.lichtblickType = lichtblickType; }
	public void setCorrectAnswer(int correctAnswer) { this.correctAnswer = correctAnswer; }
	public int getCorrectAnswer() {return this.correctAnswer; }
	public void setOutdated(boolean outdated) { this.outdated = outdated; }
	public boolean getOutdated() {return this.outdated; }

	@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeInt(id);
        pc.writeInt(topicId);
        pc.writeInt(categoryId);
        pc.writeString(reference);
        pc.writeString(question);
        pc.writeStringList(answers);
        pc.writeStringList(answersHelp);
        pc.writeInt(level);
        pc.writeLong(nextTime.getTime());
        pc.writeInt(wrong);
        pc.writeInt(correct);
        pc.writeString(help);
        pc.writeInt(lichtblickPage);
        pc.writeInt(lichtblickType.ordinal());
        pc.writeInt(correctAnswer);
        pc.writeInt(outdated ? 1 : 0);
    }

    public static final Parcelable.Creator<Question> CREATOR = new Parcelable.Creator<Question>() {
        public Question createFromParcel(Parcel pc) {
            return new Question(pc);
        }
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };

    public Question() {

    }

    public Question(Parcel pc) {
        id = pc.readInt();
        topicId = pc.readInt();
        categoryId = pc.readInt();
        reference = pc.readString();
        question = pc.readString();
        answers = pc.createStringArrayList();
        answersHelp = pc.createStringArrayList();
        level = pc.readInt();
        nextTime = new Date();
        nextTime.setTime(pc.readLong());
        wrong = pc.readInt();
        correct = pc.readInt();
        help = pc.readString();
        lichtblickPage = pc.readInt();
        lichtblickType = LichtblickType.values()[pc.readInt()];
        correctAnswer = pc.readInt();
        outdated = (pc.readInt() != 0);
    }
}
