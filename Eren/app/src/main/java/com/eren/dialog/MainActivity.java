package com.eren.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * HOOK ENTRY POINT
 *
 * After APK is built, open classes.dex in MT Manager:
 * 1. Open MainActivity.smali
 * 2. Copy the invoke-static / startActivity line that launches Eren
 * 3. Delete MainActivity.smali (or the whole class)
 * 4. Paste the hook into the target mod's entry Activity
 *
 * This class exists only so the compiled smali contains a clean,
 * easy-to-copy launch call.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== HOOK LINE (copy this from smali) =====
        Intent intent = new Intent(this, Eren.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        // ===== END HOOK LINE =====

        finish();
    }
}
