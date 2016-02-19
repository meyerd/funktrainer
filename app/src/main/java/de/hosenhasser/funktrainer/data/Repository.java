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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import de.hosenhasser.funktrainer.FunkTrainerActivity;
import de.hosenhasser.funktrainer.R;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class Repository extends SQLiteOpenHelper {
	private final Context context;
	private int answerIdSeq;
	private int imageIdSeq;
    private int answerImageSeq;
	private SQLiteDatabase database;
	private final String done;

	private ProgressDialog pDialog;
	
	private static final int NUMBER_LEVELS = 5;

    private static final int DATABASE_VERSION = 8;

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
		
		final Cursor c = getDb().query("question", new String[]{"_id", "topic_id", "reference", "question", "level", "next_time", "wrong"}, "_id=?", new String[]{Integer.toString(questionId)}, null, null, null, null);
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
            question.setWrong(c.getInt(6));
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
		
		updateAnswered(questionId, newLevel, newWrong);
	}
	
	public void answeredIncorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() <= 0 ? 0 : question.getLevel() - 1;
        final int newWrong = question.getWrong() + 1;
		
		updateAnswered(questionId, newLevel, newWrong);
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
        updates.put("wrong", 0L);
		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});		
	}
	
	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
		final Cursor c = getTopicsCursor(getDb());
		adapter.changeCursor(c);
	}
	
	public Cursor getTopicsCursor(final SQLiteDatabase db) {
		final Cursor cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, CASE WHEN MIN(level) >= " + NUMBER_LEVELS + " THEN ? ELSE SUM(CASE WHEN level < " + NUMBER_LEVELS + " THEN 1 ELSE 0 END) END AS status, MIN(CASE WHEN level >= " + NUMBER_LEVELS + " THEN NULL ELSE next_time END) AS next_question FROM topic t LEFT JOIN question q ON q.topic_id = t._id GROUP BY t._id, t.order_index, t.name ORDER BY t.order_index", new String[]{done});
		return cursor;
	}
	
	private void updateAnswered(final int questionId, final int newLevel, final int newWrong) {
		final long newNextTime = new Date().getTime() + waitingTimeOnLevel(newLevel);
		
		final ContentValues updates = new ContentValues();
		updates.put("level", newLevel);
		updates.put("next_time", newNextTime);
        updates.put("wrong", newWrong);
		
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
        contentValues.put("wrong", 0);
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
            answerIndex++;
		}
	}
	
	private SQLiteDatabase getDb() {
		if (database == null) {
			database = this.getWritableDatabase();
		}
		return database;
	}

    private void realOnCreate(LongDatabaseOperationTask task, SQLiteDatabase db) {
        // create databases
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL)");
            db.execSQL("CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(_id) ON DELETE CASCADE, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL, wrong INT NOT NULL)");
            db.execSQL("CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT NOT NULL)");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // fill with data
        try {
            final List<Topic> topics = new LinkedList<Topic>();
            final List<Question> questions = new LinkedList<Question>();
            final XmlResourceParser xmlResourceParser = context.getResources().getXml(R.xml.funkfragen_nontransform);
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
                            qtext = cleanUpTagText(qtext);
                            currentQuestion.setQuestionText(qtext);
                            expectingQuestion = false;
                        }
                        if (expectingAnswer) {
                            String answertext = xmlResourceParser.getText();
                            answertext = cleanUpTagText(answertext);
                            currentQuestion.getAnswers().add(answertext);
                            expectingAnswer = false;
                        }
                    case XmlPullParser.END_TAG:
                        final String endTagName = xmlResourceParser.getName();
                        if("chapter".equals(endTagName)) {
                            if(topiclevel <= 1) {
                                topics.add(currentTopic);
                                currentTopic = null;
                            }
                            topiclevel -= 1;
                            if(topicPrefixes.size() >= 1) {
                                topicPrefixes.remove(topicPrefixes.size() - 1);
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
        createMixtopics(task, db);
    }


    private void createMixtopics(LongDatabaseOperationTask task, final SQLiteDatabase db) {
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
                        "Betriebliche Kenntnisse", "Kenntnisse von Vorschriften"},
                new String[] {"Klasse A alle (Technik, Betrieb, Vorschriften)", "Technische Kenntnisse (Klasse A)",
                        "Betriebliche Kenntnisse", "Kenntnisse von Vorschriften"}
        };
        // get max topic id
        long maxTopicId = 0;
        long maxTopicOrderIndex = 0;
        db.beginTransaction();
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
        long maxQuestionId = 0;
        Cursor qt = db.query("question", new String[]{"_id"}, null, null, null, null, null, null);
        try {
            qt.moveToNext();
            while (!qt.isAfterLast()) {
                long qId = qt.getInt(0);
                if (qId > maxQuestionId) {
                    maxQuestionId = qId;
                }
                qt.moveToNext();
            }
        } finally {
            qt.close();
        }
        long maxAnswerId = 0;
        Cursor at = db.query("answer", new String[]{"_id"}, null, null, null, null, null, null);
        try {
            at.moveToNext();
            while (!at.isAfterLast()) {
                long aId = at.getInt(0);
                if (aId > maxAnswerId) {
                    maxAnswerId = aId;
                }
                at.moveToNext();
            }
        } finally {
            at.close();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        // create mixtopics
        long newAnswerRunningIndex = 1;
        for (final String[] mix : mixtopics) {
            final String mixName = mix[0];
            db.beginTransaction();
            try {
                ContentValues mixTopicVals = new ContentValues();
                long mixId = ++maxTopicId;
                mixTopicVals.put("_id", mixId);
                mixTopicVals.put("order_index", ++maxTopicOrderIndex);
                mixTopicVals.put("name", mixName);
                db.insert("topic", null, mixTopicVals);
                for (int i = 1; i < mix.length ; i++) {
                    Cursor q = db.rawQuery("SELECT _id, topic_id, reference, question, level, next_time, wrong FROM question WHERE topic_id = (SELECT _id FROM topic WHERE name = ?)", new String[]{mix[i]});
                    try {
                        q.moveToNext();
                        while (!q.isAfterLast()) {
                            long qId = q.getInt(0);
                            String qReference = q.getString(2);
                            String qQuestion = q.getString(3);
                            long newQuestionId = ++maxQuestionId;
                            ContentValues newQuestionVals = new ContentValues();
                            newQuestionVals.put("_id", newQuestionId);
                            newQuestionVals.put("topic_id", mixId);
                            newQuestionVals.put("reference", qReference);
                            newQuestionVals.put("question", qQuestion);
                            newQuestionVals.put("next_time", (new Date()).getTime());
                            newQuestionVals.put("level", 0L);
                            newQuestionVals.put("wrong", 0L);
                            db.insert("question", null, newQuestionVals);

                            Cursor a = db.query("answer", new String[]{"_id", "question_id", "order_index", "answer"}, "question_id = ?", new String[]{Long.toString(qId)}, null, null, null, null);
                            try {
                                a.moveToNext();
                                while (!a.isAfterLast()) {
                                    String aText = a.getString(3);
                                    ContentValues newAnswerVals = new ContentValues();
                                    long newAnswerId = ++maxAnswerId;
                                    newAnswerVals.put("_id", newAnswerId);
                                    newAnswerVals.put("question_id", newQuestionId);
                                    newAnswerVals.put("order_index", newAnswerRunningIndex++);
                                    newAnswerVals.put("answer", aText);
                                    db.insert("answer", null, newAnswerVals);

                                    a.moveToNext();
                                }
                            } finally {
                                a.close();
                            }
                            q.moveToNext();
                        }
                    } finally {
                        q.close();
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }


	void realUpgrade(LongDatabaseOperationTask task, SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion <= 6) {
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
		if(oldVersion <= 7) {
			// 6 -> 7
			db.beginTransaction();
			try {
				db.execSQL("UPDATE topic SET name = 'Kenntnisse von Vorschriften' WHERE name = 'Prüfungsfragen im Prüfungseil „Kenntnisse von Vorschriften“'");
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
		if(oldVersion <= DATABASE_VERSION) {
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
			createMixtopics(task, db);
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
                    realOnCreate(this, this.params.db);
                    break;
                case 1: // upgrade
                    realUpgrade(this, this.params.db, this.params.oldVersion, this.params.newVersion);
                    break;
                default:
                    throw new RuntimeException("invalid use of startLongDatabaseOperation");
            }

            Log.i("Funktrainer", "Long database operation done.");

            return 0;
        }

        protected void onProgressUpdate(Integer percent) {
            this.params.pDialog.setProgress(percent);
            FunkTrainerActivity mainactivity = (FunkTrainerActivity)this.params.context;
            mainactivity.updateAdapter();
        }

        protected void onPostExecute(Integer res) {
            this.params.pDialog.dismiss();

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
		startLongDatabaseOperation(db, 1, oldVersion, newVersion);
	}
}
