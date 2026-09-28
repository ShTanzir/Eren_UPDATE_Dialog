package com.eren.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Self-contained native Java update dialog.
 *
 * IMPORTANT:
 * These two strings are intentionally simple placeholders so they are easy
 * to find in Eren.smali after build/minification is disabled.
 */
public final class Eren {

    /* === EDIT THESE IN SMALI FOR YOUR FINAL MOD === */
    private static final String FIREBASE_DATABASE_URL = "https://YOUR_DATABASE_URL";
    private static final String APP_CONNECT_KEY = "MM-XXX-XXX-XXX-ST";

    private static final long POLL_MS = 2500L;
    private static final String FALLBACK_BG = "#FFD0C8";
    private static final String CARD = "#FFFAFD";
    private static final String TEXT = "#303136";
    private static final String BLUE = "#70A0DF";

    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final java.util.Map<Activity, Dialog> OPEN = new java.util.WeakHashMap<>();
    private static final java.util.Map<Activity, String> VERSION = new java.util.WeakHashMap<>();

    private Eren() {}

    public static void show(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        IO.execute(() -> {
            JSONObject cfg = fetchConfig();
            MAIN.post(() -> {
                if (cfg != null && cfg.optBoolean("enabled", true)) {
                    showConfig(activity, cfg);
                    startPolling(activity, cfg.optString("updatedAt", "0"));
                }
            });
        });
    }

    private static void startPolling(final Activity activity, final String knownVersion) {
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                if (activity == null || activity.isFinishing()) return;
                IO.execute(() -> {
                    JSONObject cfg = fetchConfig();
                    MAIN.post(() -> {
                        if (cfg == null) { MAIN.postDelayed(this, POLL_MS); return; }
                        boolean enabled = cfg.optBoolean("enabled", true);
                        String v = cfg.optString("updatedAt", "0");
                        if (!enabled) {
                            dismiss(activity);
                        } else if (!v.equals(VERSION.get(activity))) {
                            showConfig(activity, cfg);
                        }
                        MAIN.postDelayed(this, POLL_MS);
                    });
                });
            }
        }, POLL_MS);
    }

    private static JSONObject fetchConfig() {
        try {
            if (FIREBASE_DATABASE_URL.contains("YOUR_DATABASE_URL") || APP_CONNECT_KEY.contains("XXX")) return null;
            String path = "/apps/" + URLEncoder.encode(APP_CONNECT_KEY, "UTF-8").replace("+", "%20") + ".json";
            String out = request("GET", url(FIREBASE_DATABASE_URL, path), null);
            if (out == null || "null".equals(out.trim())) return null;
            return new JSONObject(out);
        } catch (Exception e) {
            return null;
        }
    }

    private static String url(String base, String path) {
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + path;
    }

    private static String request(String method, String target, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(target).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c.getOutputStream().write(bytes);
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        if (code >= 400) return null;
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static void showConfig(final Activity a, final JSONObject cfg) {
        dismiss(a);
        VERSION.put(a, cfg.optString("updatedAt", String.valueOf(System.currentTimeMillis())));

        final JSONObject d = cfg.optJSONObject("dialog");
        if (d == null) return;

        final Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(buildView(a, dialog, d));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams p = w.getAttributes();
            p.dimAmount = 0.55f;
            w.setAttributes(p);
        }
        dialog.setOnShowListener(x -> sizeWindow(dialog, a));
        dialog.show();
        sizeWindow(dialog, a);
        OPEN.put(a, dialog);
    }

    private static void sizeWindow(Dialog dialog, Activity a) {
        Window w = dialog.getWindow();
        if (w == null) return;
        int sw = a.getResources().getDisplayMetrics().widthPixels;
        int max = dp(a, 680);
        int width = Math.min(max, (int)(sw * 0.92f));
        w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private static View buildView(final Activity a, final Dialog dialog, final JSONObject d) {
        String bg = color(d, "backgroundColor", FALLBACK_BG);
        String card = color(d, "cardColor", CARD);
        String accent = color(d, "accentColor", BLUE);
        String txt = color(d, "textColor", TEXT);
        String featureText = color(d, "featureTextColor", txt);
        int radius = clamp(d.optInt("cornerRadius", 36), 12, 60);

        LinearLayout shell = new LinearLayout(a);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(0,0,0,0);
        shell.setBackground(round(card, dp(a, radius)));

        FrameLayout media = new AspectFrame(a, 16f/9f);
        media.setBackgroundColor(Color.parseColor("#E8E4E6"));
        String mediaUrl = d.optString("mediaUrl", "");
        String mediaType = d.optString("mediaType", "image");
        if (mediaUrl.startsWith("data:image/")) {
            ImageView iv = new ImageView(a);
            iv.setScaleType(scale(d.optString("imageFit", "CROP")));
            media.addView(iv, new FrameLayout.LayoutParams(-1,-1));
            setDataImage(iv, mediaUrl);
        } else if ("video".equalsIgnoreCase(mediaType) && !mediaUrl.isEmpty()) {
            VideoView vv = new VideoView(a);
            vv.setVideoURI(Uri.parse(mediaUrl));
            vv.setOnPreparedListener(mp -> { mp.setLooping(true); mp.setVolume(0f,0f); vv.start(); });
            media.addView(vv, new FrameLayout.LayoutParams(-1,-1));
        } else {
            ImageView iv = new ImageView(a);
            iv.setScaleType(scale(d.optString("imageFit", "CROP")));
            media.addView(iv, new FrameLayout.LayoutParams(-1,-1));
            if (!mediaUrl.isEmpty()) loadImage(iv, mediaUrl);
        }
        GradientDrawable fade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.parseColor("#EEFFFAFD"), Color.parseColor(card)});
        View overlay = new View(a);
        overlay.setBackground(fade);
        FrameLayout.LayoutParams fo = new FrameLayout.LayoutParams(-1, dp(a, 78), Gravity.BOTTOM);
        media.addView(overlay, fo);
        shell.addView(media, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(a, 20), dp(a, 10), dp(a, 20), dp(a, 20));

        TextView update = tv(a, d.optString("topText", "UPDATE"), 18, txt, "monospace", false);
        update.setGravity(Gravity.CENTER);
        update.setLetterSpacing(.08f);
        content.addView(update, new LinearLayout.LayoutParams(-1, dp(a, 32)));

        TextView title = tv(a, d.optString("name", "MODMASE"), 56, txt, "sans-serif-condensed", false);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL));
        String rawName = d.optString("name", "MODMASE");
        String[] parts = rawName.split(" ",2);
        if (parts.length==2) {
            android.text.SpannableString s = new android.text.SpannableString(rawName);
            int cut = parts[0].length()+1;
            s.setSpan(new android.text.style.ForegroundColorSpan(accent), cut, rawName.length(), 0);
            title.setText(s);
        }
        content.addView(title, new LinearLayout.LayoutParams(-1, dp(a, 68)));

        LinearLayout featureCard = new LinearLayout(a);
        featureCard.setOrientation(LinearLayout.VERTICAL);
        featureCard.setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 16));
        featureCard.setBackground(round(card, dp(a, 25)));
        featureCard.setElevation(dp(a, 2));
        JSONArray fs = d.optJSONArray("features");
        if (fs == null || fs.length() == 0) {
            fs = new JSONArray();
            fs.put("New Features Available");
            fs.put("Previous Bug Fixed");
        }
        for (int i=0;i<fs.length();i++) {
            String f = fs.optString(i, "");
            TextView row = tv(a, "❖  " + f, 16, featureText, "sans-serif", true);
            row.setPadding(0, dp(a, 2), 0, dp(a, 2));
            featureCard.addView(row, new LinearLayout.LayoutParams(-1, dp(a, 30)));
        }
        LinearLayout.LayoutParams fcp = new LinearLayout.LayoutParams(-1, -2);
        fcp.topMargin = dp(a, 12);
        content.addView(featureCard, fcp);

        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        int gap = dp(a, 10);
        TextView exit = button(a, "EXIT", "#FFFFFF", txt);
        TextView updateBtn = button(a, d.optString("updateText","UPDATE"), accent, "#050505");
        LinearLayout.LayoutParams bp1 = new LinearLayout.LayoutParams(0, dp(a, 58), 1f);
        bp1.rightMargin = gap/2;
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, dp(a, 58), 1.05f);
        bp2.leftMargin = gap/2;
        buttons.addView(exit, bp1);
        buttons.addView(updateBtn, bp2);
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(-1, -2);
        bcp.topMargin = dp(a, 18);
        content.addView(buttons, bcp);

        exit.setOnClickListener(v -> {
            dismiss(a);
            try { a.finishAffinity(); } catch (Exception ignored) {}
        });
        updateBtn.setOnClickListener(v -> {
            String u = d.optString("updateUrl", "");
            if (u.isEmpty()) { Toast.makeText(a, "Update link is not set", Toast.LENGTH_SHORT).show(); return; }
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
            catch (Exception e) { Toast.makeText(a, "Invalid update URL", Toast.LENGTH_SHORT).show(); }
        });

        shell.addView(content, new LinearLayout.LayoutParams(-1,-2));
        return shell;
    }

    private static void setDataImage(ImageView iv, String data) {
        try {
            int i = data.indexOf(',');
            byte[] b = Base64.decode(data.substring(i+1), Base64.DEFAULT);
            iv.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));
        } catch (Exception ignored) {}
    }

    private static void loadImage(ImageView iv, String url) {
        IO.execute(() -> {
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(10000); c.connect();
                InputStream in=c.getInputStream();
                Bitmap b=BitmapFactory.decodeStream(in); in.close();
                MAIN.post(() -> { if (b!=null) iv.setImageBitmap(b); });
            } catch (Exception ignored) {}
        });
    }

    private static ImageView.ScaleType scale(String fit) {
        return "FIT".equalsIgnoreCase(fit) ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP;
    }

    private static TextView tv(Context c, String s, float size, String color, String font, boolean bold) {
        TextView v=new TextView(c);
        v.setText(s); v.setTextSize(TypedValue.COMPLEX_UNIT_SP,size); v.setTextColor(Color.parseColor(color));
        v.setGravity(Gravity.CENTER_VERTICAL); v.setTypeface(android.graphics.Typeface.create(font,bold?1:0));
        return v;
    }

    private static TextView button(Context c, String s, String bg, String fg) {
        TextView v=tv(c,s,20,fg,"monospace",false); v.setGravity(Gravity.CENTER);
        GradientDrawable g=round(bg,dp(c,22)); g.setStroke(dp(c,3),Color.BLACK); v.setBackground(g); v.setClickable(true);
        return v;
    }

    private static GradientDrawable round(String c,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(c)); g.setCornerRadius(r); return g; }
    private static int dp(Context c,int x){ return (int)(x*c.getResources().getDisplayMetrics().density+.5f); }
    private static int clamp(int n,int lo,int hi){ return Math.max(lo,Math.min(hi,n)); }
    private static String color(JSONObject o,String k,String d){ String s=o.optString(k,d); try{Color.parseColor(s);return s;}catch(Exception e){return d;} }

    private static void dismiss(Activity a){
        Dialog d=OPEN.remove(a); if(d!=null && d.isShowing()) try{d.dismiss();}catch(Exception ignored){}
    }

    private static final class AspectFrame extends FrameLayout {
        private final float ratio;
        AspectFrame(Context c,float ratio){super(c);this.ratio=ratio;}
        @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws);int h=(int)(w/ratio);setMeasuredDimension(w,h);int childH=MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY);int childW=MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY);for(int i=0;i<getChildCount();i++)getChildAt(i).measure(childW,MeasureSpec.makeMeasureSpec(getChildAt(i).getLayoutParams().height>0?getChildAt(i).getLayoutParams().height:h,MeasureSpec.EXACTLY));}
    }
}
