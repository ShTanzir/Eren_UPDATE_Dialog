package com.eren.admin.data;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Context;

public final class FirebaseHelper {

    public static void ensureInitialized(Context ctx, String databaseUrl) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("com.eren.admin")
                        .setDatabaseUrl(databaseUrl)
                        .build();
                FirebaseApp.initializeApp(ctx, options);
            }
        } catch (Exception ignored) {}
    }

    public static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseReference apps() {
        return root().child("apps");
    }

    public static DatabaseReference accessKeys() {
        return root().child("accessKeys");
    }
}
