package com.eren.dialog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Update Dialog — UI structure matches Eren.html exactly.
 *
 * AFTER BUILD (MT Manager):
 * Search inside Eren.smali for these two plain-string placeholders and replace them:
 *
 *   PLACEHOLDER_FIREBASE_DATABASE_URL
 *   PLACEHOLDER_APP_CONNECT_KEY
 */
public class Eren extends Activity {

    // =====================================================================
    //  PLACEHOLDERS — replace these two strings in smali after decompile
    // =====================================================================
    public static final String FIREBASE_DATABASE_URL =
            "PLACEHOLDER_FIREBASE_DATABASE_URL";

    public static final String APP_CONNECT_KEY =
            "PLACEHOLDER_APP_CONNECT_KEY";
    // =====================================================================

    private FrameLayout root;
    private LinearLayout dialogCard;
    private ImageView heroImage;
    private TextView smallTitle;
    private TextView brandText;
    private LinearLayout featuresContainer;
    private Button btnExit;
    private Button btnUpdate;

    private String updateUrl = "";
    private ValueEventListener liveListener;
    private DatabaseReference appRef;
    private boolean dialogEnabled = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.parseColor("#FFD0C8"));

        buildUi();
        setContentView(root);

        if (isPlaceholder(FIREBASE_DATABASE_URL) || isPlaceholder(APP_CONNECT_KEY)) {
            showPlaceholderWarning();
            return;
        }

        initFirebaseAndListen();
    }

    private boolean isPlaceholder(String value) {
        return value == null
                || value.isEmpty()
                || value.startsWith("PLACEHOLDER_");
    }

    private void showPlaceholderWarning() {
        Toast.makeText(this,
                "Placeholders not replaced.\nEdit Eren.smali → FIREBASE_DATABASE_URL & APP_CONNECT_KEY",
                Toast.LENGTH_LONG).show();
        // Still show default UI so the dialog is visible for testing
        applyDefaultConfig();
    }

    // ------------------------------------------------------------------
    //  UI — same structure as Eren.html
    // ------------------------------------------------------------------

    private void buildUi() {
        int peach = Color.parseColor("#FFD0C8");
        int cardBg = Color.parseColor("#FFFAFD");
        int textDark = Color.parseColor("#292A2D");
        int brandColor = Color.parseColor("#353539");
        int accentBlue = Color.parseColor("#70A0DF");
        int featureText = Color.parseColor("#37383C");
        int symbolColor = Color.parseColor("#424347");

        root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(peach);
        root.setPadding(dp(16), dp(24), dp(16), dp(24));

        // Outer scroll so small screens never clip
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        scrollLp.gravity = Gravity.CENTER;
        root.addView(scroll, scrollLp);

        LinearLayout centerWrap = new LinearLayout(this);
        centerWrap.setOrientation(LinearLayout.VERTICAL);
        centerWrap.setGravity(Gravity.CENTER);
        scroll.addView(centerWrap, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- Dialog card ----
        dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackground(rounded(cardBg, dp(36)));
        dialogCard.setElevation(dp(12));
        dialogCard.setClipToOutline(true);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                Math.min(dp(680), getResources().getDisplayMetrics().widthPixels - dp(32)),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER_HORIZONTAL;
        centerWrap.addView(dialogCard, cardLp);

        // ---- Hero image (16:9) ----
        FrameLayout hero = new FrameLayout(this);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        heroLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        // aspect 1200/675 ≈ 1.777
        hero.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) ((getResources().getDisplayMetrics().widthPixels - dp(32)) * 675f / 1200f)));
        hero.setBackgroundColor(Color.parseColor("#E8E4E6"));

        heroImage = new ImageView(this);
        heroImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(heroImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Bottom fade
        View fade = new View(this);
        GradientDrawable fadeGd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.parseColor("#DBFFFAFD"), cardBg});
        fade.setBackground(fadeGd);
        FrameLayout.LayoutParams fadeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (hero.getLayoutParams().height * 0.29f));
        fadeLp.gravity = Gravity.BOTTOM;
        hero.addView(fade, fadeLp);

        dialogCard.addView(hero);

        // ---- Content ----
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(25), dp(17), dp(25), dp(23));
        dialogCard.addView(content);

        // Small title "UPDATE"
        smallTitle = new TextView(this);
        smallTitle.setText("UPDATE");
        smallTitle.setTextColor(textDark);
        smallTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        smallTitle.setLetterSpacing(0.04f);
        Typeface audiowide = safeFont(R.font.audiowide);
        if (audiowide != null) smallTitle.setTypeface(audiowide);
        else smallTitle.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        content.addView(smallTitle, lpCenter());

        // Brand MOD + MASE
        brandText = new TextView(this);
        brandText.setText(buildBrandSpan("MOD", "MASE", brandColor, accentBlue));
        brandText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        Typeface oswald = safeFont(R.font.oswald);
        if (oswald != null) brandText.setTypeface(oswald);
        else brandText.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        brandText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brandLp = lpCenter();
        brandLp.topMargin = dp(2);
        content.addView(brandText, brandLp);

        // Features card
        featuresContainer = new LinearLayout(this);
        featuresContainer.setOrientation(LinearLayout.VERTICAL);
        featuresContainer.setBackground(rounded(cardBg, dp(25)));
        featuresContainer.setElevation(dp(4));
        featuresContainer.setPadding(dp(25), dp(20), dp(25), dp(20));
        LinearLayout.LayoutParams featLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        featLp.topMargin = dp(24);
        content.addView(featuresContainer, featLp);

        // Default features (overwritten by Firebase)
        addFeatureRow("New Features Available", symbolColor, featureText);
        addFeatureRow("Previous Bug Fixed", symbolColor, featureText);

        // Buttons row
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setWeightSum(2.05f);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        btnRowLp.topMargin = dp(30);
        content.addView(buttons, btnRowLp);

        btnExit = makeButton("EXIT", Color.WHITE, Color.parseColor("#050505"), false);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(0, dp(63), 1f);
        exitLp.rightMargin = dp(12);
        buttons.addView(btnExit, exitLp);

        btnUpdate = makeButton("UPDATE", accentBlue, Color.parseColor("#050505"), true);
        LinearLayout.LayoutParams updLp = new LinearLayout.LayoutParams(0, dp(63), 1.05f);
        buttons.addView(btnUpdate, updLp);

        // Click listeners
        btnExit.setOnClickListener(v -> exitApp());
        btnUpdate.setOnClickListener(v -> openUpdateUrl());
    }

    private void addFeatureRow(String text, int symbolColor, int textColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView symbol = new TextView(this);
        symbol.setText("❖");
        symbol.setTextColor(symbolColor);
        symbol.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        symbol.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams symLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        symLp.rightMargin = dp(10);
        row.addView(symbol, symLp);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(textColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        Typeface poppins = safeFont(R.font.poppins_bold);
        if (poppins != null) label.setTypeface(poppins);
        else label.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        featuresContainer.addView(row);
    }

    private Button makeButton(String text, int bg, int fg, boolean isUpdate) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(fg);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        Typeface audiowide = safeFont(R.font.audiowide);
        if (audiowide != null) b.setTypeface(audiowide);
        else b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        b.setAllCaps(true);
        b.setStateListAnimator(null);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setCornerRadius(dp(22));
        gd.setStroke(dp(4), Color.parseColor("#080808"));
        b.setBackground(gd);
        return b;
    }

    private CharSequence buildBrandSpan(String left, String right, int leftColor, int rightColor) {
        android.text.SpannableString ss = new android.text.SpannableString(left + right);
        ss.setSpan(new android.text.style.ForegroundColorSpan(leftColor),
                0, left.length(), 0);
        ss.setSpan(new android.text.style.ForegroundColorSpan(rightColor),
                left.length(), left.length() + right.length(), 0);
        return ss;
    }

    private Typeface safeFont(int resId) {
        try {
            return ResourcesCompat.getFont(this, resId);
        } catch (Exception e) {
            return null;
        }
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private LinearLayout.LayoutParams lpCenter() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        return lp;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // ------------------------------------------------------------------
    //  Firebase live config
    // ------------------------------------------------------------------

    private void initFirebaseAndListen() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("com.eren.dialog")
                    .setDatabaseUrl(FIREBASE_DATABASE_URL)
                    .build();

            FirebaseApp app;
            try {
                app = FirebaseApp.getInstance("ErenDialog");
            } catch (IllegalStateException e) {
                app = FirebaseApp.initializeApp(this, options, "ErenDialog");
            }

            FirebaseDatabase db = FirebaseDatabase.getInstance(app);
            // Find the app node by connectKey
            DatabaseReference appsRef = db.getReference("apps");
            appsRef.orderByChild("connectKey").equalTo(APP_CONNECT_KEY)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                Toast.makeText(Eren.this,
                                        "App Connect Key not found in Firebase",
                                        Toast.LENGTH_LONG).show();
                                applyDefaultConfig();
                                return;
                            }
                            for (DataSnapshot child : snapshot.getChildren()) {
                                appRef = child.getRef();
                                attachLiveListener(child);
                                break;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(Eren.this,
                                    "Firebase error: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            applyDefaultConfig();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Firebase init failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            applyDefaultConfig();
        }
    }

    private void attachLiveListener(DataSnapshot initial) {
        applyConfig(initial);
        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                applyConfig(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) { /* ignore */ }
        };
        if (appRef != null) {
            appRef.addValueEventListener(liveListener);
        }
    }

    private void applyConfig(DataSnapshot snap) {
        mainHandler.post(() -> {
            Boolean enabled = snap.child("dialogEnabled").getValue(Boolean.class);
            dialogEnabled = enabled == null || enabled;

            if (!dialogEnabled) {
                // Admin disabled the dialog → close immediately
                finish();
                return;
            }

            DataSnapshot cfg = snap.child("dialogConfig");
            if (!cfg.exists()) {
                applyDefaultConfig();
                return;
            }

            String imageUrl = cfg.child("imageUrl").getValue(String.class);
            String brandName = cfg.child("brandName").getValue(String.class);
            String brandHighlight = cfg.child("brandHighlight").getValue(String.class);
            String upd = cfg.child("updateUrl").getValue(String.class);
            String accent = cfg.child("accentColor").getValue(String.class);
            String bg = cfg.child("bgColor").getValue(String.class);

            if (!TextUtils.isEmpty(upd)) updateUrl = upd;

            // Brand
            if (!TextUtils.isEmpty(brandName)) {
                String left = brandName;
                String right = "";
                if (!TextUtils.isEmpty(brandHighlight) && brandName.contains(brandHighlight)) {
                    int idx = brandName.indexOf(brandHighlight);
                    left = brandName.substring(0, idx);
                    right = brandHighlight;
                } else if (!TextUtils.isEmpty(brandHighlight)) {
                    left = brandName;
                    right = brandHighlight;
                }
                int leftC = Color.parseColor("#353539");
                int rightC = safeColor(accent, Color.parseColor("#70A0DF"));
                brandText.setText(buildBrandSpan(left, right, leftC, rightC));
            }

            // Features
            featuresContainer.removeAllViews();
            DataSnapshot feats = cfg.child("features");
            if (feats.exists()) {
                for (DataSnapshot f : feats.getChildren()) {
                    String t = f.getValue(String.class);
                    if (!TextUtils.isEmpty(t)) {
                        addFeatureRow(t, Color.parseColor("#424347"), Color.parseColor("#37383C"));
                    }
                }
            } else {
                addFeatureRow("New Features Available", Color.parseColor("#424347"), Color.parseColor("#37383C"));
                addFeatureRow("Previous Bug Fixed", Color.parseColor("#424347"), Color.parseColor("#37383C"));
            }

            // Image
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(this)
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(heroImage);
            }

            // Accent button color
            if (!TextUtils.isEmpty(accent)) {
                try {
                    GradientDrawable gd = new GradientDrawable();
                    gd.setColor(Color.parseColor(accent));
                    gd.setCornerRadius(dp(22));
                    gd.setStroke(dp(4), Color.parseColor("#080808"));
                    btnUpdate.setBackground(gd);
                } catch (Exception ignored) {}
            }

            // Background
            if (!TextUtils.isEmpty(bg)) {
                try {
                    root.setBackgroundColor(Color.parseColor(bg));
                } catch (Exception ignored) {}
            }
        });
    }

    private void applyDefaultConfig() {
        updateUrl = "";
        // UI already has defaults from buildUi()
    }

    private int safeColor(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------
    //  Actions
    // ------------------------------------------------------------------

    private void exitApp() {
        try {
            finishAffinity();
        } catch (Exception ignored) {}
        try {
            System.exit(0);
        } catch (Exception ignored) {}
    }

    private void openUpdateUrl() {
        if (TextUtils.isEmpty(updateUrl) || updateUrl.contains("example.com")) {
            Toast.makeText(this, "Update link is not configured.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appRef != null && liveListener != null) {
            appRef.removeEventListener(liveListener);
        }
    }

    @Override
    public void onBackPressed() {
        // Back also exits the whole app (same as EXIT button)
        exitApp();
    }
}
