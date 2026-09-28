package com.eren.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.data.Prefs;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Prefs.isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        TextView tvUrl = findViewById(R.id.tvDatabaseUrl);
        EditText etKey = findViewById(R.id.etAccessKey);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnChangeUrl = findViewById(R.id.btnChangeUrl);

        String url = Prefs.getDatabaseUrl();
        tvUrl.setText(url != null ? url : "—");
        FirebaseHelper.ensureInitialized(this, url);

        btnLogin.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            if (TextUtils.isEmpty(key)) {
                Toast.makeText(this, "Enter Access Key", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseHelper.accessKeys().child(key)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Prefs.setAccessKey(key);
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Invalid Access Key", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(LoginActivity.this,
                                    error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnChangeUrl.setOnClickListener(v -> {
            Prefs.clearAll();
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
        });
    }
}
