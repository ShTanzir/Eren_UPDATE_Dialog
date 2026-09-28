package com.eren.admin.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.models.AppModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dialog editor with live preview that mirrors the Eren.html / Eren.java structure.
 */
public class EditDialogActivity extends AppCompatActivity {

    private String appId;
    private AppModel.DialogConfig cfg = new AppModel.DialogConfig();

    // Preview views
    private FrameLayout previewRoot;
    private ImageView previewHero;
    private TextView previewBrand;
    private LinearLayout previewFeatures;
    private Button previewUpdateBtn;

    // Editors
    private EditText etImageUrl, etBrandName, etBrandHighlight, etUpdateUrl;
    private EditText etAccent, etBg;
    private EditText etFeature1, etFeature2, etFeature3;
    private LinearLayout featureEditorBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dialog);

        appId = getIntent().getStringExtra("app_id");
        if (appId == null) {
            finish();
            return;
        }

        previewRoot = findViewById(R.id.previewRoot);
        previewHero = findViewById(R.id.previewHero);
        previewBrand = findViewById(R.id.previewBrand);
        previewFeatures = findViewById(R.id.previewFeatures);
        previewUpdateBtn = findViewById(R.id.previewUpdateBtn);

        etImageUrl = findViewById(R.id.etImageUrl);
        etBrandName = findViewById(R.id.etBrandName);
        etBrandHighlight = findViewById(R.id.etBrandHighlight);
        etUpdateUrl = findViewById(R.id.etUpdateUrl);
        etAccent = findViewById(R.id.etAccent);
        etBg = findViewById(R.id.etBg);
        etFeature1 = findViewById(R.id.etFeature1);
        etFeature2 = findViewById(R.id.etFeature2);
        etFeature3 = findViewById(R.id.etFeature3);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        // Color presets
        setupPreset(R.id.presetBlue, "#70A0DF");
        setupPreset(R.id.presetGreen, "#4CAF50");
        setupPreset(R.id.presetPurple, "#9C27B0");
        setupPreset(R.id.presetOrange, "#FF9800");
        setupPreset(R.id.presetRed, "#E53935");
        setupPreset(R.id.presetTeal, "#009688");

        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                refreshPreviewFromEditors();
            }
        };
        etImageUrl.addTextChangedListener(watcher);
        etBrandName.addTextChangedListener(watcher);
        etBrandHighlight.addTextChangedListener(watcher);
        etAccent.addTextChangedListener(watcher);
        etBg.addTextChangedListener(watcher);
        etFeature1.addTextChangedListener(watcher);
        etFeature2.addTextChangedListener(watcher);
        etFeature3.addTextChangedListener(watcher);

        loadConfig();
    }

    private void setupPreset(int id, String color) {
        View v = findViewById(id);
        if (v == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(8));
        v.setBackground(gd);
        v.setOnClickListener(x -> {
            etAccent.setText(color);
            refreshPreviewFromEditors();
        });
    }

    private void loadConfig() {
        FirebaseHelper.apps().child(appId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object val = snapshot.getValue();
                        if (!(val instanceof Map)) return;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) val;
                        AppModel app = AppModel.fromSnapshot(appId, map);
                        cfg = app.dialogConfig;

                        etImageUrl.setText(cfg.imageUrl);
                        etBrandName.setText(cfg.brandName);
                        etBrandHighlight.setText(cfg.brandHighlight);
                        etUpdateUrl.setText(cfg.updateUrl);
                        etAccent.setText(cfg.accentColor);
                        etBg.setText(cfg.bgColor);

                        List<String> f = cfg.features;
                        if (f.size() > 0) etFeature1.setText(f.get(0));
                        if (f.size() > 1) etFeature2.setText(f.get(1));
                        if (f.size() > 2) etFeature3.setText(f.get(2));

                        refreshPreviewFromEditors();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void refreshPreviewFromEditors() {
        String brand = etBrandName.getText().toString().trim();
        String highlight = etBrandHighlight.getText().toString().trim();
        String accent = etAccent.getText().toString().trim();
        String bg = etBg.getText().toString().trim();
        String img = etImageUrl.getText().toString().trim();

        if (TextUtils.isEmpty(brand)) brand = "MODMASE";
        if (TextUtils.isEmpty(highlight)) highlight = "MASE";

        int leftC = Color.parseColor("#353539");
        int rightC = safeColor(accent, 0xFF70A0DF);
        previewBrand.setText(buildBrand(brand, highlight, leftC, rightC));

        previewFeatures.removeAllViews();
        addPreviewFeature(etFeature1.getText().toString().trim());
        addPreviewFeature(etFeature2.getText().toString().trim());
        addPreviewFeature(etFeature3.getText().toString().trim());

        if (!TextUtils.isEmpty(img)) {
            Glide.with(this).load(img).centerCrop().into(previewHero);
        }

        try {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(safeColor(accent, 0xFF70A0DF));
            gd.setCornerRadius(dp(18));
            gd.setStroke(dp(3), Color.BLACK);
            previewUpdateBtn.setBackground(gd);
        } catch (Exception ignored) {}

        try {
            if (!TextUtils.isEmpty(bg)) {
                previewRoot.setBackgroundColor(Color.parseColor(bg));
            }
        } catch (Exception ignored) {}
    }

    private void addPreviewFeature(String text) {
        if (TextUtils.isEmpty(text)) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView sym = new TextView(this);
        sym.setText("❖");
        sym.setTextColor(Color.parseColor("#424347"));
        sym.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.addView(sym);

        TextView label = new TextView(this);
        label.setText("  " + text);
        label.setTextColor(Color.parseColor("#37383C"));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(label);

        previewFeatures.addView(row);
    }

    private CharSequence buildBrand(String full, String highlight, int leftC, int rightC) {
        if (full.contains(highlight) && !highlight.isEmpty()) {
            int idx = full.indexOf(highlight);
            String left = full.substring(0, idx);
            String right = highlight;
            android.text.SpannableString ss = new android.text.SpannableString(left + right);
            ss.setSpan(new android.text.style.ForegroundColorSpan(leftC), 0, left.length(), 0);
            ss.setSpan(new android.text.style.ForegroundColorSpan(rightC), left.length(), left.length() + right.length(), 0);
            return ss;
        }
        android.text.SpannableString ss = new android.text.SpannableString(full);
        ss.setSpan(new android.text.style.ForegroundColorSpan(leftC), 0, full.length(), 0);
        return ss;
    }

    private void save() {
        cfg.imageUrl = etImageUrl.getText().toString().trim();
        cfg.brandName = etBrandName.getText().toString().trim();
        cfg.brandHighlight = etBrandHighlight.getText().toString().trim();
        cfg.updateUrl = etUpdateUrl.getText().toString().trim();
        cfg.accentColor = etAccent.getText().toString().trim();
        cfg.bgColor = etBg.getText().toString().trim();

        List<String> feats = new ArrayList<>();
        String f1 = etFeature1.getText().toString().trim();
        String f2 = etFeature2.getText().toString().trim();
        String f3 = etFeature3.getText().toString().trim();
        if (!f1.isEmpty()) feats.add(f1);
        if (!f2.isEmpty()) feats.add(f2);
        if (!f3.isEmpty()) feats.add(f3);
        if (feats.isEmpty()) {
            feats.add("New Features Available");
            feats.add("Previous Bug Fixed");
        }
        cfg.features = feats;

        FirebaseHelper.apps().child(appId).child("dialogConfig").setValue(cfg.toMap())
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Saved — live dialog will update now", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int safeColor(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
