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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import de.hosenhasser.funktrainer.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class Repository extends SQLiteOpenHelper {
	private final Context context;
	private int answerIdSeq;
	private int imageIdSeq;
    private int answerImageSeq;
	private SQLiteDatabase database;
	private final String done;
	
	private static final int NUMBER_LEVELS = 5;

	public Repository(final Context context) {
		super(context, "topics", null, 4);
		done = context.getString(R.string.done);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create databases
		db.beginTransaction();
		try {
			db.execSQL("CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL)");
			db.execSQL("CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(_id) ON DELETE CASCADE, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL)");
			db.execSQL("CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT NOT NULL)");
			db.execSQL("CREATE TABLE image (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id) ON DELETE CASCADE, order_index INT NOT NULL, image TEXT NOT NULL)");
            db.execSQL("CREATE TABLE answerImage (_id INT NOT NULL PRIMARY KEY, answer_id INT NOT NULL REFERENCES answer(_id) ON DELETE CASCADE, order_index INT NOT NULL, image TEXT NOT NULL)");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
				
		// fill with data
		try {
			final List<Topic> topics = new LinkedList<Topic>();
			final List<Question> questions = new LinkedList<Question>();
			final XmlResourceParser xmlResourceParser = context.getResources().getXml(R.xml.funkfragen);
			int eventType = xmlResourceParser.getEventType();

			Topic currentTopic = null;
			Question currentQuestion = null;
			boolean expectingAnswer = false;
			boolean expectingQuestion = false;

            List topicPrefixes = new ArrayList<String>();
            int topiclevel = 0;

			int index = 0;
            int idcounter = 0;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					final String tagName = xmlResourceParser.getName();
                    if("chapter".equals(tagName)) {
                        String chaptername = xmlResourceParser.getAttributeValue(null, "name");
                        if(topiclevel <= 0) {
                            topicPrefixes.clear();
                            topicPrefixes.add(chaptername);
                            currentTopic = new Topic();
                            currentTopic.setId(idcounter++);
                            currentTopic.setIndex(index++);
                            currentTopic.setName(chaptername);
                        } else {
                            topicPrefixes.add(chaptername);
                        }
//                        currentTopic = new Topic();
//                        currentTopic.setId(idcounter++);
//                        currentTopic.setIndex(index++);
//                        String tprefix = TextUtils.join(" - ", topicPrefixes);
//                        currentTopic.setName(tprefix);
                        topiclevel += 1;
                    } else if ("question".equals(tagName)) {
                        currentQuestion = new Question();
                        currentQuestion.setId(idcounter++);
                        currentQuestion.setReference(xmlResourceParser.getAttributeValue(null, "id"));
                        currentQuestion.setNextTime(new Date());
                        currentQuestion.setTopicId(currentTopic.getId());
					} else if ("textquestion".equals(tagName)) {
						expectingQuestion = true;
					} else if ("textanswer".equals(tagName)) {
						expectingAnswer = true;
					}
					break;
				case XmlPullParser.TEXT:
					if (expectingQuestion) {
                        String qtext = xmlResourceParser.getText();
                        qtext = qtext.replace('\n', ' ');
                        int imgi = 1;
                        List<String> images = new ArrayList<String>();
                        while(imgi > 0) {
                            // String beginstr = "&lt;img src='";
                            String beginstr = "<img src='";
                            int pos = qtext.indexOf(beginstr);
                            if(pos >= 0) {
                                int endpos = qtext.indexOf("'>");
                                if (endpos > pos) {
                                    String img = qtext.substring(pos + beginstr.length(), endpos);
                                    images.add(img);
                                    //Log.d("Funktrainer", "found image" + img);
                                } else {
                                    endpos = pos + beginstr.length();
                                }
                                if(endpos + 2 < qtext.length() - 1) {
                                    qtext = qtext.substring(0, pos) + qtext.substring(endpos + 2, qtext.length() - 1);
                                } else {
                                    qtext = qtext.substring(0, pos);
                                }
                            }
                            imgi = pos;
                        }
						currentQuestion.setQuestionText(qtext);
                        currentQuestion.setImages(images);
						expectingQuestion = false;
					}
					if (expectingAnswer) {
                        String answertext = xmlResourceParser.getText();
                        answertext = answertext.replace('\n', ' ');
                        int imgi = 1;
                        List<String> images = new ArrayList<String>();
                        while(imgi > 0) {
                            // String beginstr = "&lt;img src='";
                            String beginstr = "<img src='";
                            int pos = answertext.indexOf(beginstr);
                            if(pos >= 0) {
                                int endpos = answertext.indexOf("'>");
                                if (endpos > 0) {
                                    String img = answertext.substring(pos + beginstr.length(), endpos);
                                    images.add(img);
                                    //Log.d("Funktrainer", "found image" + img);
                                } else {
                                    endpos = pos + beginstr.length();
                                }
                                if(endpos + 2 < answertext.length() - 1) {
                                    answertext = answertext.substring(0, pos) + answertext.substring(endpos + 2, answertext.length() - 1);
                                } else {
                                    answertext = answertext.substring(0, pos);
                                }
                            }
                            imgi = pos;
                        }

						currentQuestion.getAnswers().add(answertext);
                        currentQuestion.getAnswerImages().add(images);
						expectingAnswer = false;
					}
				case XmlPullParser.END_TAG:
					final String endTagName = xmlResourceParser.getName();
                    if("chapter".equals(endTagName)) {
//                        if(topiclevel >= 3) {
//                            topics.add(currentTopic);
//                        }
                        if(topiclevel <= 1) {
                            topics.add(currentTopic);
                            currentTopic = null;
                        }
                        topiclevel -= 1;
                        if(topicPrefixes.size() >= 1) {
                            topicPrefixes.remove(topicPrefixes.size() - 1);
                        }
//                        currentTopic = null;
                    } else if ("question".equals(endTagName)) {
						questions.add(currentQuestion);
						currentQuestion = null;
                    }
					break;
				}
                xmlResourceParser.next();
				eventType = xmlResourceParser.getEventType();
			}
			xmlResourceParser.close();
			
			db.beginTransaction();
			try {
				for (final Topic topic : topics) {
					save(db, topic);
				}
				for (final Question question : questions) {
					save(db, question);
				}
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}
	
	public QuestionSelection selectQuestion(final int topicId) {
		final QuestionSelection result = new QuestionSelection();
		final List<Integer> possibleQuestions = new LinkedList<Integer>();
		final long now = new Date().getTime();
		
		int questionCount = 0;
		int openQuestions = 0;
		int maxProgress = 0;
		int currentProgress = 0;
		long soonestNextTime = 0;
		
		final Cursor c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
		try {
			c.moveToNext();
			while (!c.isAfterLast()) {
				final int questionId = c.getInt(0);
				final int level = c.getInt(1);
				final long nextTime = c.getLong(2);
				
				questionCount++;
				maxProgress += NUMBER_LEVELS;
				currentProgress += level;
				if (level < NUMBER_LEVELS) {
					openQuestions++;
					
					if (nextTime > now) {
						if (soonestNextTime == 0 || soonestNextTime > nextTime) {
							soonestNextTime = nextTime;
						}
					} else {
						possibleQuestions.add(questionId);
					}	
				}
				
				c.moveToNext();
			}
			
		} finally {
			c.close();
		}
		
		result.setTotalQuestions(questionCount);
		result.setMaxProgress(maxProgress);
		result.setCurrentProgress(currentProgress);
		result.setOpenQuestions(openQuestions);
		result.setFinished(possibleQuestions.isEmpty() && soonestNextTime == 0);
		if (!possibleQuestions.isEmpty()) {
			Random rand = new Random();
			result.setSelectedQuestion(possibleQuestions.get(rand.nextInt(possibleQuestions.size())));
		} else if (soonestNextTime > 0) {
			result.setNextQuestion(new Date(soonestNextTime));
		}
		
		return result;
	}
	
	public Question getQuestion(final int questionId) {
		final Question question = new Question();
		
		final Cursor c = getDb().query("question", new String[]{"_id", "topic_id", "reference", "question", "level", "next_time"}, "_id=?", new String[]{Integer.toString(questionId)}, null, null, null, null);
		try {
			c.moveToNext();
			if (c.isAfterLast()) {
				return null;
			}
			question.setId(c.getInt(0));
			question.setTopicId(c.getInt(1));
			question.setReference(c.getString(2));
			question.setQuestionText(c.getString(3));
			question.setLevel(c.getInt(4));
			question.setNextTime(new Date(c.getLong(5)));
		} finally {
			c.close();
		}
		
		// _id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT
		final Cursor answer = getDb().query("answer", new String[]{"_id", "answer"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, "order_index");
		try {
			answer.moveToNext();
			while (!answer.isAfterLast()) {
                int answerId = answer.getInt(0);
				question.getAnswers().add(answer.getString(1));
                final Cursor answerImage = getDb().query("answerImage", new String[]{"image"}, "answer_id=?", new String[]{Integer.toString(answerId)}, null, null, "order_index");
                List<String> imagesForAnswer = new LinkedList<String>();
                try {
                    answerImage.moveToNext();
                    while (!answerImage.isAfterLast()) {
                        imagesForAnswer.add(answerImage.getString(0));
                        answerImage.moveToNext();
                    }
                } finally {
                    answerImage.close();
                }
                question.getAnswerImages().add(imagesForAnswer);
				answer.moveToNext();
			}	
		} finally {
			answer.close();
		}
		final Cursor image = getDb().query("image", new String[]{"image"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, "order_index");
		try {
			image.moveToNext();
			while (!image.isAfterLast()) {
				question.getImages().add(image.getString(0));
				image.moveToNext();
			}
		} finally {
			image.close();
		}
		
		return question;
	}

    public Topic getTopic(final int topicId) {
		final Topic topic = new Topic();
            
		final Cursor c = getDb().query("topic", new String[]{"_id", "order_index", "name"}, "_id=?", new String[]{Integer.toString(topicId)}, null, null, null);
		try {
			c.moveToNext();
			if (c.isAfterLast()) {
				return null;
			}
			topic.setId(c.getInt(0));
			topic.setIndex(c.getInt(1));
			topic.setName(c.getString(2));
		} finally {
			c.close();
		}

		return topic;
    }

    public TopicStats getTopicStat(final int topicId) {
		final TopicStats stats = new TopicStats();
		stats.setLevels(NUMBER_LEVELS);
		stats.setQuestionsAtLevel(new int[NUMBER_LEVELS+1]);

		int currentProgress = 0;
		int maxProgress = 0;
		int questionCount = 0;

		final Cursor c = getDb().query("question", new String[]{"_id", "level"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
		try {
			c.moveToNext();
			while (!c.isAfterLast()) {
				questionCount++;
				currentProgress += c.getInt(1);
				maxProgress += NUMBER_LEVELS;
				stats.getQuestionsAtLevel()[c.getInt(1)]++;
				c.moveToNext();
			}
		} finally {
			c.close();
		}

		stats.setCurrentProgress(currentProgress);
		stats.setMaxProgress(maxProgress);
		stats.setQuestionCount(questionCount);

		return stats;
    }
	
	public void answeredCorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() + 1;
		
		updateAnswered(questionId, newLevel);	
	}
	
	public void answeredIncorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() <= 0 ? 0 : question.getLevel() - 1;
		
		updateAnswered(questionId, newLevel);		
	}
	
	public void continueNow(final int topicId) {
		final ContentValues updates = new ContentValues();
		updates.put("next_time", new Date().getTime());
		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});
	}
	
	public void resetTopic(final int topicId) {
		final ContentValues updates = new ContentValues();
		updates.put("next_time", new Date().getTime());
		updates.put("level", 0L);
		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});		
	}
	
	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
		final Cursor c = getTopicsCursor(getDb());
		adapter.changeCursor(c);
	}
	
	public Cursor getTopicsCursor(final SQLiteDatabase db) {
		final Cursor cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, CASE WHEN MIN(level) >= " + NUMBER_LEVELS + " THEN ? ELSE SUM(CASE WHEN level < " + NUMBER_LEVELS +" THEN 1 ELSE 0 END) END AS status, MIN(CASE WHEN level >= " + NUMBER_LEVELS + " THEN NULL ELSE next_time END) AS next_question FROM topic t LEFT JOIN question q ON q.topic_id = t._id GROUP BY t._id, t.order_index, t.name ORDER BY t.order_index", new String[]{done});
		return cursor;
	}
	
	private void updateAnswered(final int questionId, final int newLevel) {
		final long newNextTime = new Date().getTime() + waitingTimeOnLevel(newLevel);
		
		final ContentValues updates = new ContentValues();
		updates.put("level", newLevel);
		updates.put("next_time", newNextTime);
		
		getDb().update("question", updates, "_id=?", new String[]{Integer.toString(questionId)});
	}
	
	private long waitingTimeOnLevel(final int level) {
		return level <= 0 ? 15000L :
			level == 1 ? 60000L :
			level == 2 ? 30*60000L :
			level == 3 ? 86400000L :
			level == 4 ? 3*86400000L :
			0;
	}

	private void save(final SQLiteDatabase db, Topic currentTopic) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put("_id", currentTopic.getId());
		contentValues.put("order_index", currentTopic.getIndex());
		contentValues.put("name", currentTopic.getName());
		db.insert("topic", null, contentValues);
	}
	
	private void save(final SQLiteDatabase db, final Question question) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put("_id", question.getId());
		contentValues.put("topic_id", question.getTopicId());
		contentValues.put("reference", question.getReference());
		contentValues.put("question", question.getQuestionText());
		contentValues.put("level", 0);
		contentValues.put("next_time", question.getNextTime().getTime());
		db.insert("question", null, contentValues);
			
		int answerIndex = 0;
		for (final String answer : question.getAnswers()) {
			contentValues.clear();
			contentValues.put("_id", ++answerIdSeq);
			contentValues.put("question_id", question.getId());
			contentValues.put("order_index", answerIndex);
			contentValues.put("answer", answer);
			db.insert("answer", null, contentValues);
            int answerImageIndex = 0;
            for (final String answerImage : question.getAnswerImages().get(answerIndex)) {
                contentValues.clear();
                contentValues.put("_id", ++answerImageSeq);
                contentValues.put("answer_id", answerIdSeq);
                contentValues.put("order_index", answerImageIndex++);
                contentValues.put("image", answerImage);
                db.insert("answerImage", null, contentValues);
            }
            answerIndex++;
		}

		int imageIndex = 0;
		for(final String image : question.getImages()) {
			contentValues.clear();
			contentValues.put("_id", ++imageIdSeq);
			contentValues.put("question_id", question.getId());
			contentValues.put("order_index", imageIndex++);
			contentValues.put("image", image);
			db.insert("image", null, contentValues);
		}
	}
	
	private SQLiteDatabase getDb() {
		if (database == null) {
			database = this.getWritableDatabase();
		}
		return database;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		if (oldVersion <= 2) {
//			final ContentValues updates = new ContentValues();
//			updates.put("question", "Welches Funkzeugnis ist mindestens erforderlich, um mit einer Seefunkstelle auf einem Sportfahrzeug am Weltweiten Seenot- und Sicherheitsfunksystem (GMDSS) im Seegebiet A3 teilnehmen zu können?");
//			db.update("question", updates, "_id=?", new String[]{"4408"});
//		}
//		if (oldVersion <= 3) {
//			final ContentValues updates = new ContentValues();
//			updates.put("answer", "Sportboote ohne Antriebsmaschine oder solche mit einer größten nicht überschreitbaren Nutzleistung von 11,03 Kilowatt (15 PS) oder weniger.");
//			db.update("answer", updates, "question_id=? AND order_index=?", new String[]{"9664", "0"});
//			updates.clear();
//			updates.put("answer", "Sportboote mit Antriebsmaschine mit einer größeren Nutzleistung als 11,03 Kilowatt (15 PS).");
//			db.update("answer", updates, "question_id=? AND order_index=?", new String[]{"9664", "2"});
//		}
	}
}
