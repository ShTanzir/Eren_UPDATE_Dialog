package com.eren.admin.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static SharedPreferences sp;

    public static void init(Context ctx) {
        sp = ctx.getApplicationContext()
                .getSharedPreferences("eren_admin", Context.MODE_PRIVATE);
    }

    public static void setDatabaseUrl(String url) {
        sp.edit().putString("database_url", url).apply();
    }

    public static String getDatabaseUrl() {
        return sp.getString("database_url", null);
    }

    public static void setAccessKey(String key) {
        sp.edit().putString("access_key", key).apply();
    }

    public static String getAccessKey() {
        return sp.getString("access_key", null);
    }

    public static boolean isLoggedIn() {
        return getDatabaseUrl() != null && getAccessKey() != null;
    }

    public static void clearSession() {
        sp.edit().remove("access_key").apply();
    }

    public static void clearAll() {
        sp.edit().clear().apply();
    }
}
