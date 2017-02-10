package de.hosenhasser.funktrainer.data.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Iterator;

import de.hosenhasser.funktrainer.R;
import de.hosenhasser.funktrainer.data.Repository;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final ContentResolver mContentResolver;

    private static final String TAG = "Funktrainer";

    private static final String[] PROJECTION_QUESTION = new String[]{
            "_id", "level", "next_time", "wrong", "correct", "modified"
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_LEVEL = 1;
    public static final int COLUMN_NEXT_TIME = 2;
    public static final int COLUMN_WRONG = 3;
    public static final int COLUMN_CORRECT = 4;
    public static final int COLUMN_MODIFIED = 5;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    // Taken from: https://stackoverflow.com/questions/7166129/how-can-i-calculate-the-sha-256-hash-of-a-string-in-android
    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String sync_key = sharedPref.getString("pref_sync_key", "");
        String sync_secret = sharedPref.getString("pref_sync_secret", "");

        if (sync_key.equals("") || sync_secret.equals("")) {
            Log.i(TAG, "Synchronization key or secret not set");
            return;
        }

        Log.i(TAG, "Beginning network synchronization");
//        Toast.makeText(getContext(), getContext().getString(R.string.pref_sync_now_toast_message),
//                                Toast.LENGTH_LONG).show();
        boolean waserror = false;
        try {
            final String host = "http://q2k.mooo.com:9876";
            final String path = "/funktrainer/api/v1.0/questions";
            final URL location = new URL(host + path);

            // build json request
            JSONObject json_request = new JSONObject();
            final ContentResolver contentResolver = getContext().getContentResolver();
//                Uri uri = Uri.parse("content://" + SyncContentProvider.AUTHORITY).buildUpon().appendPath("questions").build();
//                Cursor c = contentResolver.query(uri, PROJECTION_QUESTION, null, null, null);
            Repository repo = new Repository(getContext());
            SQLiteDatabase db = repo.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT q._id, q.level, q.next_time, q.wrong, q.correct, s.modified FROM question q, sync s WHERE q._id == s.question_id AND q._id IN (SELECT ss.question_id FROM sync ss);", new String[]{});
            while (c.moveToNext()) {
                int qid = c.getInt(0);
                int qlevel = c.getInt(1);
                int qnext_time = c.getInt(2);
                int qwrong = c.getInt(3);
                int qcorrect = c.getInt(4);
                int qmodified = c.getInt(5);
                JSONObject qobj = new JSONObject();
                qobj.put("l", qlevel);
                qobj.put("n", qnext_time);
                qobj.put("w", qwrong);
                qobj.put("c", qcorrect);
                qobj.put("m", qmodified);
                json_request.put(Integer.toString(qid), qobj);
            }
            c.close();
            // calculate MAC
            String req = json_request.toString();
            MessageDigest mac = MessageDigest.getInstance("SHA-256");
            mac.update(sync_key.getBytes());
            mac.update(sync_secret.getBytes());
            mac.update(path.getBytes());
            mac.update(req.getBytes());
            String macs = bytesToHexString(mac.digest());
            // do request
            InputStream in = null;

            try {
                Log.i(TAG, "Streaming data from network: " + location);
                StringBuilder result = new StringBuilder();
                HttpURLConnection httpCon = null;
                try {
                    httpCon = (HttpURLConnection) location.openConnection();
                    httpCon.setRequestProperty("Content-Type", "application/json");
                    httpCon.setRequestProperty("X-Key", sync_key);
                    httpCon.setRequestProperty("X-Mac", macs);
                    httpCon.setConnectTimeout(60 * 1000);
                    httpCon.setReadTimeout(60 * 1000);
                    httpCon.setDoOutput(true);
                    httpCon.setRequestMethod("PUT");
                    OutputStream out = new BufferedOutputStream(httpCon.getOutputStream());
                    out.write(req.getBytes());
                    out.close();

                    in = new BufferedInputStream(httpCon.getInputStream());

                    int status = httpCon.getResponseCode();
                    switch (status) {
                        case 200:
                        case 201:
                            BufferedReader br = new BufferedReader(new InputStreamReader(in));
                            String line;
                            while ((line = br.readLine()) != null) {
                                result.append(line);
                            }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error when communicating with server", e);
                    syncResult.stats.numIoExceptions++;
                    waserror = true;
                } finally {
                    if (httpCon != null) {
                        try {
                            httpCon.disconnect();
                        } catch (Exception e) {
                            Log.e(TAG, "Error disconnecting http connection", e);
                            syncResult.stats.numIoExceptions++;
                            waserror = true;
                        }
                    }
                }
                // parse request answer and update db
                try {
                    SQLiteDatabase udb = repo.getWritableDatabase();

                    JSONObject json_result = new JSONObject(result.toString());
                    for (Iterator<String> iter = json_result.keys(); iter.hasNext(); ) {
                        String key = iter.next();
                        try {
                            JSONObject qobj = json_result.getJSONObject(key);
                            try {
                                int qid = Integer.parseInt(key);
                                int qlevel = qobj.getInt("l");
                                int qwrong = qobj.getInt("w");
                                int qcorrect = qobj.getInt("c");
                                long qnext_time = qobj.getLong("n");
//                                long qmodified = qobj.getLong("m");
                                final ContentValues qupdates = new ContentValues();
                                qupdates.put("level", qlevel);
                                qupdates.put("wrong", qwrong);
                                qupdates.put("correct", qcorrect);
                                qupdates.put("next_time", qnext_time);
                                long u = udb.update("question", qupdates, "_id=?", new String[]{Integer.toString(qid)});
                                if(u <= 0) {
                                    Log.e(TAG, "Tried to update a question whose id does not exist: " + Integer.toString(qid));
                                    waserror = true;
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Could not parse question object", e);
                                syncResult.stats.numParseExceptions++;
                                waserror = true;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Could not get question object", e);
                            syncResult.stats.numParseExceptions++;
                            waserror = true;
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json reply from server", e);
                    syncResult.stats.numParseExceptions++;
                    waserror = true;
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "No SHA-256 algorithm message digest available", e);
            waserror = true;
        } catch (JSONException e) {
            Log.e(TAG, "Building json object failed", e);
            syncResult.stats.numParseExceptions++;
            waserror = true;
        } catch (MalformedURLException e) {
            Log.wtf(TAG, "Sync URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            waserror = true;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            waserror = true;
//        } catch (ParseException e) {
//            Log.e(TAG, "Error parsing feed: " + e.toString());
//            syncResult.stats.numParseExceptions++;
//            return;
//        } catch (RemoteException e) {
//            Log.e(TAG, "Error updating database: " + e.toString());
//            syncResult.databaseError = true;
//            return;
//        } catch (OperationApplicationException e) {
//            Log.e(TAG, "Error updating database: " + e.toString());
//            syncResult.databaseError = true;
//            return;
        }
        if (!waserror) {
//            Toast.makeText(getContext(), getContext().getString(R.string.pref_sync_done_toast_message),
//                    Toast.LENGTH_LONG).show();
            Repository repo = new Repository(getContext());
            repo.resetAuxiliarySyncTables(repo.getWritableDatabase());
        }
//        } else {
//            Toast.makeText(getContext(), getContext().getString(R.string.pref_sync_error_toast_message),
//                    Toast.LENGTH_LONG).show();
//        }
        Log.i(TAG, "Network synchronization complete");
    }
}
