package de.hosenhasser.funktrainer.data.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import de.hosenhasser.funktrainer.R;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final ContentResolver mContentResolver;

    private static final String TAG = "Funktrainer";

    private static final String[] PROJECTION_QUESTION = new String[]{
            "_id", "level", "next_time", "wrong", "correct"
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_LEVEL = 1;
    public static final int COLUMN_NEXT_TIME = 2;
    public static final int COLUMN_WRONG = 3;
    public static final int COLUMN_CORRECT = 4;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParalleSyncs) {
        super(context, autoInitialize, allowParalleSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");
        try {
            final URL location = new URL("http://sync");
            InputStream stream = null;

            try {
                Log.i(TAG, "Streaming data from network: " + location);
                Toast.makeText(getContext(), getContext().getString(R.string.pref_sync_now_toast_message),
                                Toast.LENGTH_LONG).show();
                //s tream = downloadUrl(location);
                // updateLocalFeedData(stream, syncResult);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
                Toast.makeText(getContext(), getContext().getString(R.string.pref_sync_done_toast_message),
                        Toast.LENGTH_LONG).show();
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (MalformedURLException e) {
            Log.wtf(TAG, "Feed URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
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
        Log.i(TAG, "Network synchronization complete");
    }
}
