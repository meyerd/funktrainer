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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import de.hosenhasser.funktrainer.FunkTrainerActivity;
import de.hosenhasser.funktrainer.R;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class Repository extends SQLiteOpenHelper {
	private final Context context;
	private int answerIdSeq = 0;
    private int questionToCategoryIdSeq = 0;
    private int questionToTopicIdSeq = 0;
    private int categoryToTopicIdSeq = 0;
    private int topicIdSeq = 0;
	private SQLiteDatabase database;
	private final String done;

    private final ReentrantLock objlock = new ReentrantLock();

	private ProgressDialog pDialog;
	
	private static final int NUMBER_LEVELS = 5;

    private static final int DATABASE_VERSION = 9;

	public Repository(final Context context) {
		super(context, "topics", null, DATABASE_VERSION);
		done = context.getString(R.string.done);
		this.context = context;
	}

	private String cleanUpTagText(String text) {
		text = text.replace('\n', ' ');
		text = text.replaceAll("[\t ]{1,}", " ");
		return text;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        startLongDatabaseOperation(db, 0, 0, 0);
        // realOnCreate(db);
	}

    public int getFirstTopicIdForQuestionReference(final String questionReference) {
        int topicId = 0;
        final Cursor c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "reference = ?", new String[]{questionReference}, null, null, null, null);
        try {
            c.moveToNext();
            if (!c.isAfterLast()) {
                int questionId = c.getInt(0);
                final Cursor d = getDb().rawQuery("SELECT t._id FROM topic t LEFT JOIN category_to_topic ct ON ct.topic_id = t._id LEFT JOIN question_to_category qt ON qt.category_id = ct.category_id WHERE qt.question_id=? LIMIT 1;", new String[]{Integer.toString(questionId)});
                try {
                    d.moveToNext();
                    if (!d.isAfterLast()) {
                        topicId = d.getInt(0);
                    }
                } finally {
                    d.close();
                }
            }
        } finally {
            c.close();
        }
        return topicId;
    }

    public QuestionSelection selectQuestionByReference(final String questionReference) {
        // TODO: refactor this in order not to replicate the function below
        final QuestionSelection result = new QuestionSelection();
        final List<Integer> possibleQuestions = new LinkedList<Integer>();
        final long now = new Date().getTime();

        int questionCount = 0;
        int openQuestions = 0;
        int maxProgress = 0;
        int currentProgress = 0;
        long soonestNextTime = 0;

        final Cursor c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "reference = ?", new String[]{questionReference}, null, null, null, null);
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                final int qId = c.getInt(0);
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
                        possibleQuestions.add(qId);
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
            result.setSelectedQuestion(possibleQuestions.get(0));
        } else if (soonestNextTime > 0) {
            result.setNextQuestion(new Date(soonestNextTime));
        }

        return result;
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
		
		final Cursor c = getDb().rawQuery("SELECT q._id, q.level, next_time FROM question q LEFT JOIN question_to_category qt ON qt.question_id = q._id LEFT JOIN category_to_topic ct ON ct.category_id = qt.category_id WHERE ct.topic_id=?", new String[]{Integer.toString(topicId)});
        // final Cursor c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
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
		
		final Cursor c = getDb().query("question", new String[]{"_id", "reference", "question", "level", "next_time", "wrong", "correct", "help"}, "_id=?", new String[]{Integer.toString(questionId)}, null, null, null, null);
		try {
			c.moveToNext();
			if (c.isAfterLast()) {
				return null;
			}
			question.setId(c.getInt(0));
			question.setReference(c.getString(1));
            question.setQuestion(c.getString(2));
			question.setLevel(c.getInt(3));
            question.setNextTime(new Date(c.getLong(4)));
            question.setWrong(c.getInt(5));
            question.setCorrect(c.getInt(6));
            question.setHelp(c.getString(7));
		} finally {
			c.close();
		}
		
		// _id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT
		final Cursor answer = getDb().query("answer", new String[]{"_id", "answer, help"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, "order_index");
		try {
			answer.moveToNext();
			while (!answer.isAfterLast()) {
                int answerId = answer.getInt(0);
				question.getAnswers().add(answer.getString(1));
                question.getAnswersHelp().add(answer.getString(2));
				answer.moveToNext();
			}	
		} finally {
			answer.close();
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

        final Cursor c = getDb().rawQuery("SELECT q._id, q.level FROM question q LEFT JOIN question_to_category qt ON qt.question_id = q._id LEFT JOIN category_to_topic ct ON ct.category_id = qt.category_id WHERE ct.topic_id=?", new String[]{Integer.toString(topicId)});
//		final Cursor c = getDb().query("question", new String[]{"_id", "level"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
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

	public String[] getAllQuestionIdentifiers() {
        final HashSet<String> ret = new HashSet<String>();
		final Cursor c = getDb().query("question", new String[]{"_id", "reference"}, null, null, null, null, "reference", null);
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                ret.add(c.getString(1));
                c.moveToNext();
            }
        } finally {
            c.close();
        }

        String[] ret1 = ret.toArray(new String[0]);
        Arrays.sort(ret1);

        return ret1;
	}
	
	public void answeredCorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() + 1;
        final int newWrong = question.getWrong();
        final int newCorrect = question.getCorrect() + 1;
		
		updateAnswered(questionId, newLevel, newWrong, newCorrect);
	}
	
	public void answeredIncorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() <= 0 ? 0 : question.getLevel() - 1;
        final int newWrong = question.getWrong() + 1;
        final int newCorrect = question.getCorrect();
		
		updateAnswered(questionId, newLevel, newWrong, newCorrect);
	}
	
	public void continueNow(final int topicId) {
        SQLiteDatabase db = getDb();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("UPDATE question SET next_time = 0 WHERE _id IN (SELECT question_id FROM question_to_category WHERE category_id IN (SELECT category_id FROM category_to_topic WHERE topic_id=?))", new String[]{Integer.toString(topicId)});
            c.moveToFirst();
            c.close();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
//		final ContentValues updates = new ContentValues();
//		updates.put("next_time", new Date().getTime());
//		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});
	}
	
	public void resetTopic(final int topicId) {
        SQLiteDatabase db = getDb();

        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("UPDATE question SET next_time = 0, level = 0, wrong = 0, correct = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            c.moveToFirst();
            c.close();
            /* db.rawQuery("UPDATE question SET level = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            db.rawQuery("UPDATE question SET wrong = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            db.rawQuery("UPDATE question SET correct = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});*/
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

//		final ContentValues updates = new ContentValues();
//		updates.put("next_time", new Date().getTime());
//		updates.put("level", 0L);
//        updates.put("wrong", 0L);
//        updates.put("correct", 0L);
//		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});
	}
	
	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
        // Lock needed when upgrading simultaneously
        objlock.lock();
        final Cursor c = getTopicsCursor(getDb());
        objlock.unlock();
        adapter.changeCursor(c);
	}
	
	public Cursor getTopicsCursor(final SQLiteDatabase db) {
        Cursor cursor = null;
        // try {
            cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, CASE WHEN MIN(level) >= " + NUMBER_LEVELS + " THEN ? ELSE SUM(CASE WHEN level < " + NUMBER_LEVELS + " THEN 1 ELSE 0 END) END AS status, MIN(CASE WHEN level >= " + NUMBER_LEVELS + " THEN NULL ELSE next_time END) AS next_question FROM topic t LEFT JOIN category_to_topic ct ON ct.topic_id = t._id LEFT JOIN question_to_category qt ON qt.category_id = ct.category_id LEFT JOIN question q ON q._id = qt.question_id GROUP BY t._id, t.order_index, t.name ORDER BY t.order_index", new String[]{done});
//        } catch (SQLiteException e) {
//            cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, NULL AS status, NULL AS next_question FROM topic t;", new String[]{});
//        }
		return cursor;
	}
	
	private void updateAnswered(final int questionId, final int newLevel, final int newWrong, final int newCorrect) {
		final long newNextTime = new Date().getTime() + waitingTimeOnLevel(newLevel);
		
		final ContentValues updates = new ContentValues();
		updates.put("level", newLevel);
		updates.put("next_time", newNextTime);
        updates.put("wrong", newWrong);
        updates.put("correct", newCorrect);
		
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

	private void save(final SQLiteDatabase db, Topic topic) {
		final ContentValues cV = new ContentValues();
		cV.put("_id", topic.getId());
		cV.put("order_index", topic.getIndex());
		cV.put("name", topic.getName());
        cV.put("isprimary", topic.getPrimary() ? 1 : 0);
		db.insert("topic", null, cV);
	}

    private void save(final SQLiteDatabase db, final Category category) {
        final ContentValues cV = new ContentValues();
        cV.put("_id", category.getId());
        cV.put("name", category.getName());
        cV.put("reference", category.getReference());
        cV.put("isprimary", category.getPrimary() ? 1 : 0);
        cV.put("parent", category.getParent());
        db.insert("category", null, cV);
    }
	
	private void save(final SQLiteDatabase db, final Question question) {
		final ContentValues cV = new ContentValues();
		cV.put("_id", question.getId());
        cV.put("reference", question.getReference());
		cV.put("question", question.getQuestion());
        cV.put("next_time", question.getNextTime().getTime());
		cV.put("level", question.getLevel());
        cV.put("wrong", question.getWrong());
        cV.put("correct", question.getCorrect());
        cV.put("help", question.getHelp());
		db.insert("question", null, cV);

        cV.clear();
        cV.put("_id", ++questionToCategoryIdSeq);
        cV.put("question_id", question.getId());
        cV.put("category_id", question.getCategoryId());
        db.insert("question_to_category", null, cV);
			
		int answerIndex = 0;
		for (final String answer : question.getAnswers()) {
			cV.clear();
			cV.put("_id", ++answerIdSeq);
			cV.put("question_id", question.getId());
			cV.put("order_index", answerIndex);
			cV.put("answer", answer);
            cV.put("help", ""); // question.getAnswersHelp().get(answerIndex));
			db.insert("answer", null, cV);
            answerIndex++;
		}
	}
	
	private SQLiteDatabase getDb() {
		if (database == null) {
			database = this.getWritableDatabase();
		}
		return database;
	}

    private void realOnCreate(SQLiteDatabase db) {
        // create databases
        createNewDatabaseScheme9(db);

        int questionIdSeq = 0;
        int categoryIdSeq = 0;

        // fill with data
        try {
            final List<Category> categories = new LinkedList<Category>();
            final List<Question> questions = new LinkedList<Question>();
            final XmlResourceParser xmlResourceParser = context.getResources().getXml(R.xml.funkfragen_nontransform);
            int eventType = xmlResourceParser.getEventType();

            Category currentCategory = null;
            Question currentQuestion = null;
            boolean expectingAnswer = false;
            boolean expectingQuestion = false;

            List<Category> categoryTrace = new LinkedList<Category>();
            int chapterLevel = 0;

            int index = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        final String tagName = xmlResourceParser.getName();
                        if("chapter".equals(tagName)) {
                            String chaptername = xmlResourceParser.getAttributeValue(null, "name");
                            String chapterid = xmlResourceParser.getAttributeValue(null, "id");
                            currentCategory = new Category();
                            currentCategory.setId(++categoryIdSeq);
                            currentCategory.setName(chaptername);
                            currentCategory.setPrimary(chapterLevel <= 0 ? true : false);
                            currentCategory.setParent(chapterLevel <= 0 ? 0 : categoryTrace.get(categoryTrace.size() - 1).getId());
                            currentCategory.setReference(chapterid);
                            categoryTrace.add(currentCategory);

                            chapterLevel += 1;
                        } else if ("question".equals(tagName)) {
                            currentQuestion = new Question();
                            currentQuestion.setId(++questionIdSeq);
                            currentQuestion.setReference(xmlResourceParser.getAttributeValue(null, "id"));
                            currentQuestion.setNextTime(new Date());
                            currentQuestion.setCategoryId(categoryTrace.get(categoryTrace.size() - 1).getId());
                            currentQuestion.setCorrect(0);
                            currentQuestion.setWrong(0);
                            currentQuestion.setLevel(0);
                            currentQuestion.setHelp(" ");
                        } else if ("textquestion".equals(tagName)) {
                            expectingQuestion = true;
                        } else if ("textanswer".equals(tagName)) {
                            expectingAnswer = true;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        if (expectingQuestion) {
                            String qtext = xmlResourceParser.getText();
                            qtext = cleanUpTagText(qtext);
                            currentQuestion.setQuestion(qtext);
                            expectingQuestion = false;
                        }
                        if (expectingAnswer) {
                            String answertext = xmlResourceParser.getText();
                            answertext = cleanUpTagText(answertext);
                            currentQuestion.getAnswers().add(answertext);
                            currentQuestion.getAnswersHelp().add(" ");
                            expectingAnswer = false;
                        }
                    case XmlPullParser.END_TAG:
                        final String endTagName = xmlResourceParser.getName();
                        if("chapter".equals(endTagName)) {
                            categories.add(categoryTrace.get(categoryTrace.size() - 1));
                            chapterLevel -= 1;
                            if(categoryTrace.size() >= 1) {
                                currentCategory = categoryTrace.get(categoryTrace.size() - 1);
                                categoryTrace.remove(categoryTrace.size() - 1);
                            }
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
                for (final Category category : categories) {
                    save(db, category);
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
        createTopics(db);
        createMixtopics(db);
    }

    private void addSubcategoryToTopic(final SQLiteDatabase db, int topicId, int parentCategoryId) {
        final Cursor c = db.query("category", new String[]{"_id", "parent"}, "parent=?", new String[]{Integer.toString(parentCategoryId)}, null, null, "_id", null);
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                int categoryId = c.getInt(0);

                db.beginTransaction();
                try {
                    ContentValues cV = new ContentValues();
                    cV.put("_id", ++categoryToTopicIdSeq);
                    cV.put("category_id", categoryId);
                    cV.put("topic_id", topicId);
                    db.insert("category_to_topic", null, cV);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                addSubcategoryToTopic(db, topicId, categoryId);

                c.moveToNext();
            }
        } finally {
            c.close();
        }
    }

    private void createTopics(final SQLiteDatabase db) {
        // Create all the topics from the primary categories as default
        int index = 0;
        final Cursor c = db.query("category", new String[]{"_id", "name", "reference", "isprimary", "parent"}, "isprimary=?", new String[]{"1"}, null, null, "_id", null);
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                Topic topic = new Topic();
                topic.setId(++topicIdSeq);
                topic.setPrimary(true);
                topic.setIndex(++index);
                topic.setName(c.getString(1));

                db.beginTransaction();
                try {
                    save(db, topic);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                int categoryId = c.getInt(0);
                int topicId = topic.getId();

                db.beginTransaction();
                try {
                    ContentValues cV = new ContentValues();
                    cV.put("_id", ++categoryToTopicIdSeq);
                    cV.put("category_id", categoryId);
                    cV.put("topic_id", topicId);
                    db.insert("category_to_topic", null, cV);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                addSubcategoryToTopic(db, topicId, categoryId);
                c.moveToNext();
            }
        } finally {
            c.close();
        }
    }

    private void createMixtopics(final SQLiteDatabase db) {
        // Create mixes
        // Klasse E
        //  Technische Kenntnisse (Klasse E)
        //  Betriebliche Kenntnisse
        //  Kenntnisse von Vorschriften
        //
        // Klasse A
        //  Technische Kenntnisse (Klasse A)
        //  Betriebliche Kenntnisse
        //  Kenntnisse von Vorschriften
        String[][] mixtopics = new String[][] {
                new String[] {"Klasse E alle (Technik, Betrieb, Vorschriften)", "Technische Kenntnisse (Klasse E)",
                        "Betriebliche Kenntnisse", "Kenntnisse von Vorschriften"}
        };

        // get max topic id
        long maxTopicId = 0;
        long maxTopicOrderIndex = 0;
        db.beginTransaction();
        try {
            Cursor t = db.query("topic", new String[]{"_id, order_index"}, null, null, null, null, null, null);
            try {
                t.moveToNext();
                while (!t.isAfterLast()) {
                    long tId = t.getInt(0);
                    long tIdx = t.getInt(1);
                    if (tId > maxTopicId) {
                        maxTopicId = tId;
                    }
                    if (tIdx > maxTopicOrderIndex) {
                        maxTopicOrderIndex = tIdx;
                    }
                    t.moveToNext();
                }
            } finally {
                t.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        for (final String[] mix : mixtopics) {
            final String mixName = mix[0];
            Topic topic = new Topic();
            topic.setId(++topicIdSeq);
            topic.setPrimary(true);
            // TODO: maybe use long in all the objects for _id and references
            topic.setIndex((int)++maxTopicOrderIndex);
            topic.setName(mixName);

            db.beginTransaction();
            try {
                save(db, topic);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            int topicId = topic.getId();

            for (int i = 1; i < mix.length ; i++) {
                final Cursor c = db.query("category", new String[]{"_id", "name"}, "name=?", new String[]{mix[i]}, null, null, "_id", null);
                try {
                    c.moveToNext();
                    while (!c.isAfterLast()) {
                        int categoryId = c.getInt(0);

                        db.beginTransaction();
                        try {
                            ContentValues cV = new ContentValues();
                            cV.put("_id", ++categoryToTopicIdSeq);
                            cV.put("category_id", categoryId);
                            cV.put("topic_id", topicId);
                            db.insert("category_to_topic", null, cV);
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }

                        addSubcategoryToTopic(db, topicId, categoryId);

                        c.moveToNext();
                    }
                } finally {
                   c.close();
                }
            }
        }
    }

    void createNewDatabaseScheme9(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL, isprimary INT);");
            db.execSQL("CREATE TABLE category (_id INT NOT NULL PRIMARY KEY, name TEXT NOT NULL, reference TEXT NOT NULL, isprimary INT, parent INT REFERENCES category(_id));");
            db.execSQL("CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL, wrong INT, correct INT, help TEXT);");
            db.execSQL("CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), order_index INT, answer TEXT NOT NULL, help TEXT);");
            db.execSQL("CREATE TABLE question_to_category (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), category_id INT NOT NULL REFERENCES category(_id));");
            db.execSQL("CREATE TABLE question_to_topic (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), topic_id INT NOT NULL REFERENCES topic(_id));");
            db.execSQL("CREATE TABLE category_to_topic (_id INT NOT NULL PRIMARY KEY, category_id INT NOT NULL REFERENCES category(_id), topic_id INT NOT NULL REFERENCES topic(_id));");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

	void realUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 6) {
			// Flush db and create again
			db.beginTransaction();
			try {
				if (oldVersion <= 6) {
					db.execSQL("DROP TABLE IF EXISTS image");
					db.execSQL("DROP TABLE IF EXISTS answerImage");
				}
				db.execSQL("DROP TABLE IF EXISTS topic");
				db.execSQL("DROP TABLE IF EXISTS question");
				db.execSQL("DROP TABLE IF EXISTS answer");

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			onCreate(db);
			// return since onCreate already creates newest version.
			return;
		}
		if(oldVersion < 7) {
			// 6 -> 7
			db.beginTransaction();
			try {
				db.execSQL("UPDATE topic SET name = 'Kenntnisse von Vorschriften' WHERE name = 'Prüfungsfragen im Prüfungseil „Kenntnisse von Vorschriften“'");
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
		if(oldVersion < 8) {
			// 7 -> 8
			// add wrong column to questions table
			db.beginTransaction();
			try {
				db.execSQL("ALTER TABLE question ADD COLUMN wrong INT");
				db.execSQL("UPDATE question SET wrong = 0;");
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			// createMixtopicsOld(db);
		}
        if(oldVersion < DATABASE_VERSION) {
            // 8 -> 9

            // rename old scheme tables
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE topic RENAME TO topic_old;");
                db.execSQL("ALTER TABLE question RENAME TO question_old;");
                db.execSQL("ALTER TABLE answer RENAME TO answer_old;");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            // create new db
            realOnCreate(db);

            // put levels of old questions to new database
            db.beginTransaction();
            try {
                Cursor q = db.query("question_old", new String[]{"_id, reference, level"}, null, null, null, null, null, null);
                try {
                    q.moveToNext();
                    while (!q.isAfterLast()) {
                        String reference = q.getString(1);
                        int level = q.getInt(2);
                        ContentValues cV = new ContentValues();
                        cV.put("level", level);
                        db.update("question", cV, "reference=?", new String[]{reference});
                        q.moveToNext();
                    }
                } finally {
                    q.close();
                    db.setTransactionSuccessful();
                }
            } finally {
                db.endTransaction();
            }

            // drop old tables
            db.beginTransaction();
            try {
                db.execSQL("DROP TABLE topic_old;");
                db.execSQL("DROP TABLE question_old;");
                db.execSQL("DROP TABLE answer_old;");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
	}

    private static class LongDatabaseOperationTaskParams {
        public ProgressDialog pDialog;
        public Context context;
        public SQLiteDatabase db;
        public int operation;
        public int oldVersion;
        public int newVersion;
    }

	private class LongDatabaseOperationTask extends AsyncTask<LongDatabaseOperationTaskParams, Integer, Integer> {
        public LongDatabaseOperationTaskParams params;

        protected Integer doInBackground(LongDatabaseOperationTaskParams... params) {
            Log.i("Funktrainer", "Starting long database operation.");
            this.params = params[0];

            switch (this.params.operation) {
                case 0: // create new
                    realOnCreate(this.params.db);
                    break;
                case 1: // upgrade
                    realUpgrade(this.params.db, this.params.oldVersion, this.params.newVersion);
                    break;
                default:
                    throw new RuntimeException("invalid use of startLongDatabaseOperation");
            }

            Log.i("Funktrainer", "Long database operation done.");

            return 0;
        }

        protected void onProgressUpdate(Integer percent) {
            this.params.pDialog.setProgress(percent);
        }

        protected void onPostExecute(Integer res) {
            this.params.pDialog.dismiss();
            objlock.unlock();
            final Intent intent = new Intent(this.params.context, FunkTrainerActivity.class);
            this.params.context.startActivity(intent);
//            FunkTrainerActivity mainactivity = (FunkTrainerActivity)this.params.context;
//            mainactivity.updateAdapter();
        }
    }


	void startLongDatabaseOperation(SQLiteDatabase db, final int operation, int oldVersion,
									int newVersion) {
		this.pDialog = ProgressDialog.show(this.context, this.context.getString(R.string.messagePleaseWait),
				this.context.getString(R.string.messagePreparing), true, false);
		LongDatabaseOperationTaskParams tp = new LongDatabaseOperationTaskParams();
        tp.context = this.context;
        tp.db = db;
        tp.operation = operation;
        tp.oldVersion = oldVersion;
        tp.newVersion = newVersion;
        tp.pDialog = pDialog;

        objlock.lock();

        new LongDatabaseOperationTask().execute(tp);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("Funktrainer", "upgrading database from version " + oldVersion + " to new version "
				+ newVersion);
//        // TODO: this is not shown?
//        Context context = this.context;
//        CharSequence text = "Updating Database ...";
//        int duration = Toast.LENGTH_LONG;
//        Toast toast = Toast.makeText(context, text, duration);
//        toast.show();
//		startLongDatabaseOperation(db, 1, oldVersion, newVersion);
//
//        objlock.lock();
//        Log.i("Funktrainer", "upgrade done.");
//        objlock.unlock();
        realUpgrade(db, oldVersion, newVersion);
	}
}
