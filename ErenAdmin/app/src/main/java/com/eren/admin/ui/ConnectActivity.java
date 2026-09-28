package com.eren.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.data.Prefs;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ConnectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already fully logged in → Home
        if (Prefs.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // Database URL already saved → go to Login (access key)
        if (Prefs.getDatabaseUrl() != null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_connect);

        EditText etUrl = findViewById(R.id.etDatabaseUrl);
        Button btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url) || !url.startsWith("https://")) {
                Toast.makeText(this, "Enter a valid Firebase databaseURL (https://...)", Toast.LENGTH_SHORT).show();
                return;
            }
            // Normalize: must end without trailing slash ideally
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

            FirebaseHelper.ensureInitialized(this, url);
            Prefs.setDatabaseUrl(url);

            // Quick connectivity check
            final String finalUrl = url;
            FirebaseHelper.root().child(".info/connected")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Toast.makeText(ConnectActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                            // First time → create first access key screen
                            startActivity(new Intent(ConnectActivity.this, CreateKeyActivity.class)
                                    .putExtra("first_time", true));
                            finish();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(ConnectActivity.this,
                                    "Cannot reach Firebase: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            Prefs.clearAll();
                        }
                    });
        });
    }
}
