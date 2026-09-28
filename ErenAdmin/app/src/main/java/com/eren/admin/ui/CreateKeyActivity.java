package com.eren.admin.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.data.Prefs;
import com.eren.admin.util.KeyGen;

import java.util.HashMap;
import java.util.Map;

public class CreateKeyActivity extends AppCompatActivity {

    private String generatedKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_key);

        boolean firstTime = getIntent().getBooleanExtra("first_time", false);

        TextView tvKey = findViewById(R.id.tvGeneratedKey);
        EditText etNote = findViewById(R.id.etNote);
        Button btnCopy = findViewById(R.id.btnCopyKey);
        Button btnSave = findViewById(R.id.btnSaveKey);
        Button btnSkip = findViewById(R.id.btnSkip);

        generatedKey = KeyGen.generateAccessKey();
        tvKey.setText(generatedKey);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("access_key", generatedKey));
            Toast.makeText(this, "Key copied", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            String note = etNote.getText().toString().trim();
            Map<String, Object> data = new HashMap<>();
            data.put("note", note.isEmpty() ? "Admin" : note);
            data.put("createdAt", System.currentTimeMillis());

            FirebaseHelper.accessKeys().child(generatedKey).setValue(data)
                    .addOnSuccessListener(a -> {
                        Prefs.setAccessKey(generatedKey);
                        Toast.makeText(this, "Access key saved", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        if (firstTime) {
            btnSkip.setVisibility(android.view.View.GONE);
        } else {
            btnSkip.setOnClickListener(v -> finish());
        }
    }
}
