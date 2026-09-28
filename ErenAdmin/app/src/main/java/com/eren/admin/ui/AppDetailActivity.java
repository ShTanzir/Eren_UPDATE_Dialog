package com.eren.admin.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.models.AppModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class AppDetailActivity extends AppCompatActivity {

    private String appId;
    private AppModel app;
    private ValueEventListener listener;

    private ImageView ivIcon;
    private TextView tvName, tvDetail, tvConnectKey, tvDbUrl, tvInternetPerm;
    private Switch switchDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        appId = getIntent().getStringExtra("app_id");
        if (appId == null) {
            finish();
            return;
        }

        ivIcon = findViewById(R.id.ivIcon);
        tvName = findViewById(R.id.tvName);
        tvDetail = findViewById(R.id.tvDetail);
        tvConnectKey = findViewById(R.id.tvConnectKey);
        tvDbUrl = findViewById(R.id.tvDbUrl);
        tvInternetPerm = findViewById(R.id.tvInternetPerm);
        switchDialog = findViewById(R.id.switchDialog);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnCopyKey = findViewById(R.id.btnCopyKey);
        Button btnCopyUrl = findViewById(R.id.btnCopyUrl);
        Button btnCopyPerm = findViewById(R.id.btnCopyPerm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // INTERNET permission line (exactly what user copies into target APK)
        String permLine = "<uses-permission android:name=\"android.permission.INTERNET\" />";
        tvInternetPerm.setText(permLine);

        btnCopyKey.setOnClickListener(v -> copy(tvConnectKey.getText().toString(), "Connect Key"));
        btnCopyUrl.setOnClickListener(v -> copy(tvDbUrl.getText().toString(), "Database URL"));
        btnCopyPerm.setOnClickListener(v -> copy(permLine, "INTERNET permission"));

        switchDialog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (app == null) return;
            // only write when user actually toggles
            if (buttonView.isPressed()) {
                FirebaseHelper.apps().child(appId).child("dialogEnabled").setValue(isChecked)
                        .addOnSuccessListener(a ->
                                Toast.makeText(this,
                                        isChecked ? "Dialog Enabled" : "Dialog Disabled",
                                        Toast.LENGTH_SHORT).show());
            }
        });

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditDialogActivity.class);
            i.putExtra("app_id", appId);
            startActivity(i);
        });

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete App")
                .setMessage("This will permanently remove the app and its dialog config.")
                .setPositiveButton("Delete", (d, w) ->
                        FirebaseHelper.apps().child(appId).removeValue()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                    finish();
                                }))
                .setNegativeButton("Cancel", null)
                .show());

        listen();
    }

    private void listen() {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    finish();
                    return;
                }
                Object val = snapshot.getValue();
                if (!(val instanceof Map)) return;
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) val;
                app = AppModel.fromSnapshot(appId, map);

                tvName.setText(app.name);
                tvDetail.setText(app.shortDetail);
                tvConnectKey.setText(app.connectKey);
                tvDbUrl.setText(app.databaseURL);
                switchDialog.setChecked(app.dialogEnabled);

                if (app.iconUrl != null && !app.iconUrl.isEmpty()) {
                    Glide.with(AppDetailActivity.this).load(app.iconUrl).circleCrop().into(ivIcon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseHelper.apps().child(appId).addValueEventListener(listener);
    }

    private void copy(String text, String label) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, label + " copied", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            FirebaseHelper.apps().child(appId).removeEventListener(listener);
        }
    }
}
