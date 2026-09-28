package com.eren.admin;

import android.app.Application;

import com.eren.admin.data.Prefs;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class ErenAdminApp extends Application {

    private static ErenAdminApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Prefs.init(this);

        String dbUrl = Prefs.getDatabaseUrl();
        if (dbUrl != null && !dbUrl.isEmpty()) {
            try {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("com.eren.admin")
                        .setDatabaseUrl(dbUrl)
                        .build();
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this, options);
                }
            } catch (Exception ignored) {}
        }
    }

    public static ErenAdminApp get() {
        return instance;
    }
}
