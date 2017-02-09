package de.hosenhasser.funktrainer.data.sync;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
public class SyncAuthenticatorService extends Service {
    private static final String ACCOUNT_TYPE = "de.hosenhasser.funktrainer.sync";
    private static final String ACCOUNT_NAME = "funktrainer_sync";

    public static Account GetAccount() {
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACCOUNT_TYPE);
    }

    // Instance field that stores the authenticator object
    private SyncAuthenticator mAuthenticator;
    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SyncAuthenticator(this);
    }
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
