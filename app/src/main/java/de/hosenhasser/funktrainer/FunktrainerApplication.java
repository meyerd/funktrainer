package de.hosenhasser.funktrainer;

import android.app.Application;
import android.content.Context;

public class FunktrainerApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        FunktrainerApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return FunktrainerApplication.context;
    }
}