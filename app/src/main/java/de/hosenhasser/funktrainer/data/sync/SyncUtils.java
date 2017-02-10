package de.hosenhasser.funktrainer.data.sync;

/*
 * taken from basic sync android example
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SyncUtils {
    private static final long SYNC_FREQUENCY = 60 * 60;  // 1 hour (in seconds)
    private static final String CONTENT_AUTHORITY = SyncContentProvider.AUTHORITY;
    private static final String PREF_SETUP_COMPLETE = "sync_setup_complete";

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     */
    public static Account CreateSyncAccount(Context context) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = SyncAuthenticatorService.GetAccount();
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
//            ContentResolver.addPeriodicSync(
//                    account, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY);
            newAccount = true;
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh();
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }

        return account;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh() {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Account acc = SyncAuthenticatorService.GetAccount();
        if (ContentResolver.isSyncPending(acc, SyncContentProvider.AUTHORITY)  ||
                ContentResolver.isSyncActive(acc, SyncContentProvider.AUTHORITY)) {
            Log.i("Funktrainer", "SyncPending, canceling");
            ContentResolver.cancelSync(acc, SyncContentProvider.AUTHORITY);
        }
        ContentResolver.requestSync(
                acc,      // Sync account
                SyncContentProvider.AUTHORITY, // Content authority
                b);                                      // Extras
    }
}