package de.hosenhasser.funktrainer.data.sync;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.Selection;

import de.hosenhasser.funktrainer.data.Repository;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 */
public class SyncContentProvider extends ContentProvider {
    Repository mRepository;
    Context mContext;
    public static final String AUTHORITY = "de.hosenhasser.funktrainer.provider";

    // URI ID for route: /questions
    public static final int ROUTE_QUESTIONS = 1;
    // URI ID for route: /questions/{ID}
    public static final int ROUTE_QUESTIONS_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "questions", ROUTE_QUESTIONS);
        sUriMatcher.addURI(AUTHORITY, "questions/*", ROUTE_QUESTIONS_ID);
    }

    public static final String CONTENT_TYPE_QUESTIONS =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.funktrainer.questions";
    public static final String CONTENT_ITEM_TYPE_QUESTIONS =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.funktrainer.question";

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mRepository = new Repository(mContext);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_QUESTIONS:
                return CONTENT_TYPE_QUESTIONS;
            case ROUTE_QUESTIONS_ID:
                return CONTENT_ITEM_TYPE_QUESTIONS;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mRepository.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
//        String sql = "SELECT";
//        for(String p: projection) {
//            sql += " " + p + " ";
//        }
//        sql += "FROM question ";
//        Cursor c;
        switch (uriMatch) {
//            case ROUTE_QUESTIONS_ID:
//                String id = uri.getLastPathSegment();
//                sql += "WHERE _id=? AND _id IN (SELECT s.question_id FROM sync s);";
//                c = db.rawQuery(sql, new String[]{id});
//            case ROUTE_QUESTIONS:
//                sql += "WHERE ";
//                sql += selection;
//                sql += " AND _id IN (SELECT s.question_id FROM sync s);";
//                c = db.rawQuery(sql, selectionArgs);
            case ROUTE_QUESTIONS_ID:
                String id = uri.getLastPathSegment();
                builder.where("_id=?", id);
            case ROUTE_QUESTIONS:
                builder.table("question")
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                Context ctx = getContext();
                assert ctx != null;
                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mRepository.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        // filter out anything other than "level", "next_time", "wrong", "correct"
        int vals = 0;
        ContentValues newValues = new ContentValues(values);
        for(String key :
            newValues.keySet()) {
            if(key.equals("level") || key.equals("next_time") || key.equals("wrong") || key.equals("correct")) {
                vals += 1;
            } else {
                newValues.remove(key);
            }
        }
        if (vals <= 0) {
            return 0;
        }
        int count;
        switch (match) {
            case ROUTE_QUESTIONS:
                count = builder.table("question")
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_QUESTIONS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table("question")
                        .where("_id=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }
}
