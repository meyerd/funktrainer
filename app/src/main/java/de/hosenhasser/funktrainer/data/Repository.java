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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import de.hosenhasser.funktrainer.FunkTrainerActivity;
import de.hosenhasser.funktrainer.R;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class Repository extends SQLiteOpenHelper {
	private final Context context;
	private SQLiteDatabase database;
	private final String done;

    private final ReentrantLock objlock = new ReentrantLock();

	private ProgressDialog pDialog;
	
	private static final int NUMBER_LEVELS = 5;

    private static final int DATABASE_VERSION = 13;

    private static final String DATABASE_SOURCE_SQL_12 = "database_scheme_and_data_12.sql";

	public Repository(final Context context) {
		super(context, "topics", null, DATABASE_VERSION);
		done = context.getString(R.string.done);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
//        startLongDatabaseOperation(db, 0, 0, 0);
         realOnCreate(db);
	}

    public int getFirstTopicIdForQuestionId(final int questionId) {
        return getFirstTopicIdForQuestion(null, questionId);
    }

    public int getFirstTopicIdForQuestionReference(final String questionReference) {
        return getFirstTopicIdForQuestion(questionReference, -1);
    }

    private int getFirstTopicIdForQuestion(final String questionReference, final int questionId) {
        int topicId = 0;
        Cursor c;
        if(questionReference != null) {
            c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "reference = ?", new String[]{questionReference}, null, null, null, null);
        } else {
            c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "_id = ?", new String[]{Integer.toString(questionId)}, null, null, null, null);
        }

        try {
            c.moveToNext();
            if (!c.isAfterLast()) {
                int qId = c.getInt(0);
                final Cursor d = getDb().rawQuery("SELECT t._id FROM topic t LEFT JOIN category_to_topic ct ON ct.topic_id = t._id LEFT JOIN question_to_category qt ON qt.category_id = ct.category_id WHERE qt.question_id=? LIMIT 1;", new String[]{Integer.toString(qId)});
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

    public List<Question> getQuestionsForExam(final int topicId, final int nQuestions) {
        // TODO: remove code duplication with selectQuestion function
        final List<Question> ret = new LinkedList<Question>();
        final List<Integer> possibleQuestions = new LinkedList<Integer>();

        Cursor c;
        c = getDb().rawQuery("SELECT q._id, q.level, q.next_time FROM question q LEFT JOIN question_to_category qt ON qt.question_id = q._id LEFT JOIN category_to_topic ct ON ct.category_id = qt.category_id WHERE ct.topic_id=? ORDER BY q.next_time", new String[]{Integer.toString(topicId)});
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                final int qId = c.getInt(0);
//                final int level = c.getInt(1);
//                final long nextTime = c.getLong(2);
                possibleQuestions.add(qId);
                c.moveToNext();
            }
        } finally {
            c.close();
        }

        // shuffle questions and select nQuestions
        final int questionsInList = possibleQuestions.size();
        final int selectNQuestions = Math.min(questionsInList, nQuestions);

        java.util.Collections.shuffle(possibleQuestions);

        for(int i = 0; i < selectNQuestions; i++) {
            Question q = this.getQuestion(possibleQuestions.remove(0));
            ret.add(q);
        }

        // go through the list of questions and shuffle answers for exam mode
        for(Question q: ret) {
            // TODO: easier solution? this seems very stupid
            List<String> answers = q.getAnswers();
            List<String> answersHelp = q.getAnswersHelp();
            final int nAnswers = answers.size();
            List<String> newAnswers = new LinkedList<String>();
            List<String> newAnswersHelp = new LinkedList<String>();
            List<Integer> shuf = new ArrayList<Integer>(nAnswers);
            for(int i = 0; i < nAnswers; i++) shuf.set(i, i);
            java.util.Collections.shuffle(shuf);
            for(int i = 0; i < nAnswers; i++) {
                int from = shuf.get(i);
                if(from == 0) {
                    q.setCorrectAnswer(i);
                }
                newAnswers.add(answers.get(from));
                newAnswersHelp.add(answersHelp.get(from));
            }
            q.setAnswers(newAnswers);
            q.setAnswersHelp(newAnswersHelp);
        }

        return ret;
     }

    public QuestionSelection selectQuestionByTopicId(final int topicId) {
        return selectQuestion(null, -1, topicId);
    }

    public QuestionSelection selectQuestionById(final int questionId) {
        return selectQuestion(null, questionId, -1);
    }

    public QuestionSelection selectQuestionByReference(final String questionReference) {
        return selectQuestion(questionReference, -1, -1);
    }

    private QuestionSelection selectQuestion(final String questionReference, final int questionId, final int topicId) {
        final QuestionSelection result = new QuestionSelection();
        final List<Integer> possibleQuestions = new LinkedList<Integer>();
        final long now = new Date().getTime();

        int questionCount = 0;
        int openQuestions = 0;
        int maxProgress = 0;
        int currentProgress = 0;
        long soonestNextTime = 0;

        Cursor c;
        if(topicId > -1) {
            c = getDb().rawQuery("SELECT q._id, q.level, q.next_time FROM question q LEFT JOIN question_to_category qt ON qt.question_id = q._id LEFT JOIN category_to_topic ct ON ct.category_id = qt.category_id WHERE ct.topic_id=? ORDER BY q.next_time", new String[]{Integer.toString(topicId)});
        } else if(questionReference != null) {
            c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "reference = ?", new String[]{questionReference}, null, null, null, null);
        } else {
            c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "_id = ?", new String[]{Integer.toString(questionId)}, null, null, null, null);
        }
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

        java.util.Collections.shuffle(possibleQuestions);

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

		final Cursor answer = getDb().query("answer", new String[]{"_id", "answer, help"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, "order_index");
		try {
			answer.moveToNext();
			while (!answer.isAfterLast()) {
//                int answerId = answer.getInt(0);
				question.getAnswers().add(answer.getString(1));
                question.getAnswersHelp().add(answer.getString(2));
				answer.moveToNext();
			}	
		} finally {
			answer.close();
		}
        // for now always the first entry is the correct one
        question.setCorrectAnswer(0);

        final Cursor lichtblick = getDb().query("question_to_lichtblick", new String[]{"_id", "lichtblick"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, null);
        try {
            lichtblick.moveToNext();
            while (!lichtblick.isAfterLast()) {
                int lichtblickPage = lichtblick.getInt(1);
                question.setLichtblickPage(lichtblickPage);
                lichtblick.moveToNext();
            }
        } finally {
            lichtblick.close();
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

	public SearchItem[] getAllQuestionIdentifiers() {
        final HashSet<SearchItem> ret = new HashSet<SearchItem>();
        final Cursor c = getDb().rawQuery("SELECT q._id, q.reference, cat.name, top.name FROM question q LEFT JOIN question_to_category qc ON qc.question_id = q._id LEFT JOIN category cat ON cat._id = qc.category_id LEFT JOIN category_to_topic ct ON ct.category_id = qc.category_id LEFT JOIN topic top ON top._id = ct.topic_id ORDER BY q.reference", null);
        try {
            c.moveToNext();
            while (!c.isAfterLast()) {
                int id = c.getInt(0);
                String reference = c.getString(1);
                String categoryname = c.getString(2);
                String topicname = c.getString(3);
                String label = reference + " (" + topicname + ")";
                SearchItem n = new SearchItem(id, label, reference, categoryname, topicname);
                ret.add(n);
                c.moveToNext();
            }
        } finally {
            c.close();
        }

        SearchItem[] ret1 = ret.toArray(new SearchItem[0]);
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
            Cursor c = db.rawQuery("UPDATE question SET next_time = 1 WHERE _id IN (SELECT question_id FROM question_to_category WHERE category_id IN (SELECT category_id FROM category_to_topic WHERE topic_id=?));", new String[]{Integer.toString(topicId)});
            c.moveToFirst();
            c.close();
            long now = System.currentTimeMillis() / 1000L;
            Cursor c1 = db.rawQuery("UPDATE sync SET modified = ? WHERE question_id IN (SELECT question_id FROM question_to_category WHERE category_id IN (SELECT category_id FROM category_to_topic WHERE topic_id=?));", new String[]{Long.toString(now), Integer.toString(topicId)});
            c1.moveToNext();
            c1.close();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
	}
	
	public void resetTopic(final int topicId) {
        SQLiteDatabase db = getDb();

        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("UPDATE question SET next_time = 1, level = 0, wrong = 0, correct = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            c.moveToFirst();
            c.close();
            long now = System.currentTimeMillis() / 1000L;
            Cursor c1 = db.rawQuery("UPDATE sync SET modified = ? WHERE question_id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Long.toString(now), Integer.toString(topicId)});
            c1.moveToNext();
            c1.close();
            /* db.rawQuery("UPDATE question SET level = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            db.rawQuery("UPDATE question SET wrong = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});
            db.rawQuery("UPDATE question SET correct = 0 WHERE _id IN (SELECT qt.question_id FROM question_to_category qt WHERE qt.category_id IN (SELECT ct.category_id FROM category_to_topic ct WHERE ct.topic_id=?));", new String[]{Integer.toString(topicId)});*/
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
	}
	
	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
        // Lock needed when upgrading simultaneously
        objlock.lock();
        final Cursor c = getTopicsCursor(getDb());
        objlock.unlock();
        adapter.changeCursor(c);
	}
	
	private Cursor getTopicsCursor(final SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, CASE WHEN MIN(level) >= " + NUMBER_LEVELS + " THEN ? ELSE SUM(CASE WHEN level < " + NUMBER_LEVELS + " THEN 1 ELSE 0 END) END AS status, MIN(CASE WHEN level >= " + NUMBER_LEVELS + " THEN NULL ELSE next_time END) AS next_question FROM topic t LEFT JOIN category_to_topic ct ON ct.topic_id = t._id LEFT JOIN question_to_category qt ON qt.category_id = ct.category_id LEFT JOIN question q ON q._id = qt.question_id GROUP BY t._id, t.order_index, t.name ORDER BY t.order_index", new String[]{done});
		return cursor;
	}

	public void setExamTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
        objlock.lock();
        final Cursor c = getExamTopicsCursor(getDb());
        objlock.unlock();
        adapter.changeCursor(c);
    }

	private Cursor getExamTopicsCursor(final SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT _id, name from topic t ORDER BY t.order_index", new String[]{});
        return cursor;
    }

    public ExamSettings getExamSettings(final int topicId) {
        final ExamSettings exs = new ExamSettings();

        final Cursor c = getDb().query("topic_exam_settings", new String[]{"_id", "topic_id", "number_questions", "number_questions_pass", "seconds_available"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null);
        try {
            c.moveToNext();
            if (c.isAfterLast()) {
                return null;
            }
            exs.setId(c.getInt(0));
            exs.setTopicId(c.getInt(1));
            exs.setnQuestions(c.getInt(2));
            exs.setnRequired(c.getInt(3));
            exs.setnSecondsAvailable(c.getInt(4));
        } finally {
            c.close();
        }

        return exs;
    }

	private void updateAnswered(final int questionId, final int newLevel, final int newWrong, final int newCorrect) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean force_pause = sharedPref.getBoolean("pref_force_pause", true);
        long newNextTime = 0;
        if (force_pause) {
            newNextTime = new Date().getTime() + waitingTimeOnLevel(newLevel);
        }
		
		final ContentValues updates = new ContentValues();
		updates.put("level", newLevel);
		updates.put("next_time", newNextTime);
        updates.put("wrong", newWrong);
        updates.put("correct", newCorrect);
		
		getDb().update("question", updates, "_id=?", new String[]{Integer.toString(questionId)});

        // TODO: What happens when the server and the other clients have different local times?
        long now = System.currentTimeMillis() / 1000L;
        final ContentValues updates_sync = new ContentValues();
        updates_sync.put("question_id",  questionId);
        updates_sync.put("modified", now);
        long u = getDb().update("sync", updates_sync, "question_id=?", new String[]{Integer.toString(questionId)});
        if(u == 0) {
            getDb().insert("sync", null, updates_sync);
        }
	}
	
	private long waitingTimeOnLevel(final int level) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context);
        // long waiting_time = sharedPref.getLong("pref_waiting_time_on_level_" + Integer.toString(level), 0L);
        //long waiting_time = sharedPref.getLong("pref_waiting_time_on_level_0", 0L);
        // TODO: whooooot??? get it as string and then parse long again?
        long waiting_time = Long.valueOf(sharedPref.getString("pref_waiting_time_on_level_" + Integer.toString(level), "0"));
//		return level <= 0 ? 15000L :
//			level == 1 ? 60000L :
//			level == 2 ? 30*60000L :
//			level == 3 ? 86400000L :
//			level == 4 ? 3*86400000L :
//			0;
        // Log.d("Funktrainer", "waitingTimeOnLevel: " + Long.toString(waiting_time));
        return waiting_time * 1000;
	}
	
	private SQLiteDatabase getDb() {
		if (database == null) {
			database = this.getWritableDatabase();
		}
		return database;
	}

    public void resetAuxiliarySyncTables(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS sync;");
            db.execSQL("CREATE TABLE sync (_id INT PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id), modified INT);");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void forceSyncUploadOfAllQuestions() {
        SQLiteDatabase db = this.getWritableDatabase();
        long now = System.currentTimeMillis() / 1000L;
        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO sync (question_id, modified) SELECT _id, ? FROM question;", new String[]{Long.toString(now)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void importDatabaseFromSQL(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            InputStream is = context.getResources().getAssets().open(DATABASE_SOURCE_SQL_12);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if(!line.equals("")) {
                    db.execSQL(line);
                }
            }
            db.setTransactionSuccessful();
        } catch(IOException ioex) {
            Log.i("Funktrainer", "Error reading SQL file");
            ioex.printStackTrace();
        } catch(Exception ex) {
            Log.i("Funktrainer", "Exception importing SQL file");
            ex.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    private void realOnCreate(SQLiteDatabase db) {
        importDatabaseFromSQL(db);
        resetAuxiliarySyncTables(db);
    }

	void realUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 12) {
            // any < 12 -> 12
            // use precompiled sql file import
            Log.i("Funktrainer", "DB upgrade 11->12");

            // rename old scheme tables
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE question RENAME TO question_old;");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            importDatabaseFromSQL(db);

            // put levels of old questions to new database
            LinkedList<String> references = new LinkedList<String>();
            LinkedList<Integer> levels = new LinkedList<Integer>();
            Cursor q = db.query("question_old", new String[]{"_id, reference, level"}, null, null, null, null, null, null);
            try {
                q.moveToNext();
                while (!q.isAfterLast()) {
                    String reference = q.getString(1);
                    int level = q.getInt(2);
                    if (level > 0) {
                        references.add(reference);
                        levels.add(level);
                    }
                    q.moveToNext();
                }
            } finally {
                q.close();
            }

            Log.i("Funktrainer", references.size() + " question to be updated");

            db.beginTransaction();
            try {
                ContentValues tmpcV = new ContentValues();
                for (int i = 0; i < references.size(); i++) {
                    tmpcV.clear();
                    tmpcV.put("level", levels.get(i));
                    db.update("question", tmpcV, "reference=?", new String[]{references.get(i)});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // drop old tables
            db.beginTransaction();
            try {
                db.execSQL("DROP TABLE question_old;");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if(oldVersion < 13) {
            // upgrade 12 -> 13
            resetAuxiliarySyncTables(db);
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
        Log.i("Funktrainer", "Database upgrade finished");
	}
}
