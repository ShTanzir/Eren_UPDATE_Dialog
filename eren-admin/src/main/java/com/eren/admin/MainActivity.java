package com.eren.admin;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Eren Admin - pure native Java / no XML layouts.
 *
 * Storage model:
 * /adminKeys/<sha256(accessKey)>
 * /apps/<appConnectKey>
 *
 * This build talks to Firebase Realtime Database through its REST API,
 * which means only a database URL is required by the admin UI.
 */
public class MainActivity extends Activity {

    private static final int PICK_MEDIA = 4001;
    private static final long MAX_MEDIA_BYTES = 1500000L;
    private static final String PREF = "eren_admin";
    private static final String BLUE = "#70A0DF";
    private static final String BG = "#F6F7FB";
    private static final String TEXT = "#24262B";
    private static final String MUTED = "#737780";
    private static final String CARD = "#FFFFFF";

    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private String databaseUrl = "";
    private String accessKey = "";
    private String editingMediaUri = "";
    private String editingMediaType = "image";
    private DialogConfig working;
    private FrameLayout previewHost;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        databaseUrl = prefs.getString("db", "");
        accessKey = prefs.getString("key", "");
        getWindow().setStatusBarColor(Color.parseColor("#F6F7FB"));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (!databaseUrl.isEmpty() && !accessKey.isEmpty()) dashboard(); else connectScreen();
    }

    private void connectScreen() {
        LinearLayout root = baseRoot();
        root.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout card = card();
        root.addView(space(1,80));
        TextView logo = title("EREN ADMIN", 32); card.addView(logo);
        card.addView(text("Firebase Realtime Database control panel",15,MUTED));
        card.addView(space(1,20));
        EditText db = field("Firebase databaseURL", databaseUrl);
        card.addView(db);
        Button connect = primary("CONNECT");
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(52));cp.topMargin=dp(12);card.addView(connect,cp);
        TextView note=text("Use the exact RTDB URL from Firebase Console. Example: https://your-project-default-rtdb.firebaseio.com",12,MUTED);
        note.setPadding(0,dp(12),0,0);card.addView(note);
        root.addView(card,wrap(-1,-2));
        connect.setOnClickListener(v -> {
            String u=db.getText().toString().trim();
            if (!validDb(u)){toast("Enter a valid https Firebase database URL");return;}
            setBusy(connect,true);
            io.execute(() -> { String r=Rest.get(u,"/"); main.post(() -> {setBusy(connect,false); if(r!=null){databaseUrl=cleanBase(u);prefs.edit().putString("db",databaseUrl).apply(); loginScreen();}else toast("Connection failed");});});
        });
        setContentView(root);
    }

    private void loginScreen() {
        LinearLayout root=baseRoot();
        LinearLayout card=card();
        card.addView(title("ADMIN ACCESS",28));
        card.addView(text("Enter your admin access key for this database",14,MUTED));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(16),0,0);
        EditText key=field("Access key",""); key.setInputType(0x00000081); key.setTransformationMethod(PasswordTransformationMethod.getInstance());
        row.addView(key,new LinearLayout.LayoutParams(0,dp(52),1));
        Button eye=new Button(this);eye.setText("◉"); eye.setAllCaps(false);row.addView(eye,new LinearLayout.LayoutParams(dp(54),dp(52)));
        eye.setOnClickListener(v->{ if(key.getTransformationMethod()==null)key.setTransformationMethod(PasswordTransformationMethod.getInstance()); else key.setTransformationMethod(null); key.setSelection(key.length());});
        card.addView(row);
        Button enter=primary("LOGIN");card.addView(enter,new LinearLayout.LayoutParams(-1,dp(52)));
        Button create=secondary("CREATE ACCESS KEY");LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(-1,dp(48));c.topMargin=dp(10);card.addView(create,c);
        Button back=secondary("CHANGE DATABASE");LinearLayout.LayoutParams bb=new LinearLayout.LayoutParams(-1,dp(48));bb.topMargin=dp(6);card.addView(back,bb);
        enter.setOnClickListener(v->validateKey(key.getText().toString().trim(),enter));
        create.setOnClickListener(v->createKeyDialog());
        back.setOnClickListener(v->{prefs.edit().remove("db").remove("key").apply();databaseUrl="";accessKey="";connectScreen();});
        root.addView(card,wrap(-1,-2));setContentView(root);
    }

    private void validateKey(String k, Button b) {
        if(k.isEmpty()){toast("Enter the access key");return;} setBusy(b,true);
        String hash=sha256(k);
        io.execute(()->{String r=Rest.get(databaseUrl,"/adminKeys/"+enc(hash)+".json");main.post(()->{setBusy(b,false);if(r!=null&&!"null".equals(r.trim())){accessKey=k;prefs.edit().putString("key",k).apply();dashboard();}else toast("Invalid access key");});});
    }

    private void createKeyDialog() {
        final EditText custom=field("Optional name (for your reference)","Main Admin");
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Create access key").setView(wrapView(custom,16,4,16,4)).setNegativeButton("CANCEL",null).setPositiveButton("GENERATE",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String key=genKey();JSONObject o=new JSONObject();try{o.put("createdAt",System.currentTimeMillis());o.put("label",custom.getText().toString().trim());}catch(Exception ignored){}io.execute(()->{String r=Rest.put(databaseUrl,"/adminKeys/"+enc(sha256(key))+".json",o.toString());main.post(()->{if(r!=null){d.dismiss();accessKey=key;prefs.edit().putString("key",key).apply();showGeneratedKey(key);}else toast("Could not create key. Check database rules.");});});}));d.show();
    }

    private void showGeneratedKey(String key) {
        TextView t=text(key,19,TEXT);t.setTextIsSelectable(true);LinearLayout box=new LinearLayout(this);box.setPadding(dp(22),dp(18),dp(22),dp(18));box.addView(t,new LinearLayout.LayoutParams(-1,-2));new AlertDialog.Builder(this).setTitle("Access key created").setMessage("Save this key. It is shown once here and can be used on another device.").setView(box).setPositiveButton("COPY",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Eren access key",key));toast("Copied");}).setNegativeButton("CLOSE",null).show();
    }

    private void dashboard() {
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.parseColor(BG));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar=toolbar("EREN ADMIN",null);
        Button lock=smallButton("KEY");bar.addView(lock,toolbarLp(70)); lock.setOnClickListener(v->showGeneratedKey(accessKey));
        Button out=smallButton("EXIT");bar.addView(out,toolbarLp(70)); out.setOnClickListener(v->{prefs.edit().remove("key").apply();accessKey="";loginScreen();});
        content.addView(bar);
        ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(16),dp(14),dp(16),dp(100));
        TextView sub=text("Connected: "+databaseUrl,12,MUTED);list.addView(sub); TextView heading=title("Your Apps",22);heading.setPadding(0,dp(12),0,dp(8));list.addView(heading);
        scroll.addView(list);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView fab=buttonLike("+");FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(60),dp(60),Gravity.BOTTOM|Gravity.END);fp.setMargins(0,0,dp(18),dp(18));root.addView(content,new FrameLayout.LayoutParams(-1,-1));root.addView(fab,fp);fab.setElevation(dp(8));fab.setOnClickListener(v->addAppDialog());
        setContentView(root);
        loadApps(list,heading,sub);
    }

    private void loadApps(LinearLayout list,TextView heading,TextView sub){
        io.execute(()->{String r=Rest.get(databaseUrl,"/apps.json");main.post(()->{if (list.getChildCount() > 2) list.removeViews(2, list.getChildCount() - 2);if(r==null){toast("Could not load apps");return;}try{JSONObject all=new JSONObject(r);JSONArray keys=all.names();if(keys==null){TextView e=text("No apps yet. Tap + to add one.",14,MUTED);list.addView(e);return;}for(int i=0;i<keys.length();i++){String k=keys.getString(i);JSONObject o=all.optJSONObject(k);if(o!=null)list.addView(appCard(k,o));}}catch(Exception e){toast("Database data is invalid");}});});
    }

    private View appCard(final String key,final JSONObject o){
        LinearLayout c=card();c.setPadding(dp(14),dp(14),dp(14),dp(14));LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);icon.setBackground(round("#EEF2F7",18));top.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));loadImage(icon,o.optString("iconUrl",""));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(12),0,0,0);TextView n=title(o.optString("name","Untitled"),18);TextView det=text(o.optString("shortDetail",""),13,MUTED);tx.addView(n);tx.addView(det);top.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        c.addView(top);c.addView(space(1,10));TextView keyTv=text(key,12,MUTED);keyTv.setTextIsSelectable(true);c.addView(keyTv);TextView state=text(o.optBoolean("enabled",true)?"●  DIALOG ENABLED":"○  DIALOG DISABLED",12,o.optBoolean("enabled",true)?"#3D8B5D":"#AA5E5E");c.addView(state);c.setOnClickListener(v->appDetails(key,o));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);c.setLayoutParams(lp);return c;
    }

    private void addAppDialog(){
        LinearLayout box=dialogForm();EditText name=field("App name","");EditText detail=field("Short detail","");EditText icon=field("App icon URL","");box.addView(name);box.addView(detail);box.addView(icon);TextView date=text("Date: "+dateNow(),13,MUTED);date.setPadding(0,dp(6),0,0);box.addView(date);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Add app").setView(box).setNegativeButton("CANCEL",null).setPositiveButton("ADD",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()){toast("App name is required");return;}String k=genKey();JSONObject app=new JSONObject();try{app.put("name",name.getText().toString().trim());app.put("shortDetail",detail.getText().toString().trim());app.put("iconUrl",icon.getText().toString().trim());app.put("databaseUrl",databaseUrl);app.put("internetPermission","<uses-permission android:name=\"android.permission.INTERNET\" />");app.put("enabled",true);app.put("createdAt",dateNow());app.put("updatedAt",String.valueOf(System.currentTimeMillis()));app.put("dialog",DialogConfig.defaultConfig().toJson());}catch(Exception ignored){}io.execute(()->{String r=Rest.put(databaseUrl,"/apps/"+enc(k)+".json",app.toString());main.post(()->{if(r!=null){d.dismiss();dashboard();}else toast("Could not add app");});});}));d.show();
    }

    private void appDetails(String key,JSONObject o){
        LinearLayout root=baseRoot();LinearLayout bar=toolbar(o.optString("name","APP"),()->dashboard());Button edit=smallButton("EDIT");bar.addView(edit,toolbarLp(72));edit.setOnClickListener(v->editApp(key,o));root.addView(bar);ScrollView sc=new ScrollView(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(30));
        LinearLayout card=card();ImageView icon=new ImageView(this);icon.setBackground(round("#EEF2F7",22));card.addView(icon,new LinearLayout.LayoutParams(dp(84),dp(84)));loadImage(icon,o.optString("iconUrl",""));card.addView(title(o.optString("name","Untitled"),24));card.addView(text(o.optString("shortDetail",""),14,MUTED));card.addView(space(1,12));card.addView(copyRow("App Connect Key",key));card.addView(copyRow("Firebase databaseURL",databaseUrl));card.addView(copyRow("INTERNET permission","<uses-permission android:name=\"android.permission.INTERNET\" />"));Switch sw=new Switch(this);sw.setText(o.optBoolean("enabled",true)?"Dialog Show: ENABLED":"Dialog Show: DISABLED");sw.setChecked(o.optBoolean("enabled",true));card.addView(sw);sw.setOnCheckedChangeListener((b,checked)->{b.setText(checked?"Dialog Show: ENABLED":"Dialog Show: DISABLED");patchApp(key,"enabled",checked);});Button del=danger("DELETE APP");LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,dp(50));dp.topMargin=dp(12);card.addView(del,dp);del.setOnClickListener(v->confirmDelete(key));c.addView(card);sc.addView(c);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private View copyRow(String label,String value){LinearLayout box=card();TextView l=text(label,12,MUTED);TextView v=text(value,13,TEXT);v.setTextIsSelectable(true);Button cp=secondary("COPY");LinearLayout top=new LinearLayout(this);top.addView(l,new LinearLayout.LayoutParams(0,-2,1));top.addView(cp,new LinearLayout.LayoutParams(dp(78),dp(40)));box.addView(top);box.addView(v);cp.setOnClickListener(x->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(label,value));toast("Copied");});return box;}

    private void confirmDelete(String key){new AlertDialog.Builder(this).setTitle("Delete app?").setMessage("This removes the app and its dialog config from the database.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE",(d,w)->{io.execute(()->{String r=Rest.delete(databaseUrl,"/apps/"+enc(key)+".json");main.post(()->{if(r!=null)dashboard();else toast("Delete failed");});});}).show();}

    private void patchApp(String key,String field,Object value){try{JSONObject o=new JSONObject();o.put(field,value);o.put("updatedAt",String.valueOf(System.currentTimeMillis()));io.execute(()->Rest.patch(databaseUrl,"/apps/"+enc(key)+".json",o.toString()));}catch(Exception ignored){}}

    private void editApp(String key,JSONObject app){
        working=DialogConfig.from(app.optJSONObject("dialog")); editingMediaUri=working.mediaUrl; editingMediaType=working.mediaType;
        ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(14),dp(14),dp(30));
        LinearLayout bar=toolbar("EDIT DIALOG",()->appDetails(key,app));Button save=smallButton("SAVE");bar.addView(save,toolbarLp(70));root.addView(bar);
        previewHost=new FrameLayout(this);previewHost.setBackgroundColor(Color.parseColor(BG));root.addView(previewHost,new LinearLayout.LayoutParams(-1,-2));refreshPreview();
        LinearLayout form=card();
        EditText media=field("Image / video URL",working.mediaUrl);form.addView(media);
        Button pick=secondary("CHOOSE FROM GALLERY");LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(46));pp.topMargin=dp(7);form.addView(pick,pp);
        TextView hint=text("Recommended image: 1200 × 675 px. Portrait video is supported; large local videos should be hosted by URL.",12,MUTED);hint.setPadding(0,dp(7),0,0);form.addView(hint);
        EditText name=field("Dialog name",working.name);form.addView(name);
        EditText features=field("Features (one per line)",working.featuresText());form.addView(features);
        EditText update=field("UPDATE button URL",working.updateUrl);form.addView(update);
        LinearLayout presetRow=new LinearLayout(this);presetRow.setGravity(Gravity.CENTER_VERTICAL);TextView pl=text("Color preset",13,MUTED);presetRow.addView(pl,new LinearLayout.LayoutParams(0,-2,1));String[] presets={"Sky","Rose","Mint","Mono"};for(String p:presets){Button pb=secondary(p);presetRow.addView(pb,new LinearLayout.LayoutParams(dp(62),dp(42)));pb.setOnClickListener(v->{working.applyPreset(((Button)v).getText().toString());refreshPreview();});}form.addView(presetRow);
        EditText bg=field("Background color #HEX",working.backgroundColor);form.addView(bg);EditText accent=field("Accent / UPDATE color #HEX",working.accentColor);form.addView(accent);EditText textColor=field("Text color #HEX",working.textColor);form.addView(textColor);EditText radius=field("Corner radius (12 - 60)",String.valueOf(working.cornerRadius));form.addView(radius);
        sc.addView(root);root.addView(form);setContentView(sc);

        TextWatcher tw=new SimpleTW(){@Override public void afterTextChanged(Editable e){working.mediaUrl=media.getText().toString().trim();working.name=name.getText().toString().trim();working.updateUrl=update.getText().toString().trim();working.setFeatures(features.getText().toString());working.backgroundColor=safeColor(bg.getText().toString(),working.backgroundColor);working.accentColor=safeColor(accent.getText().toString(),working.accentColor);working.textColor=safeColor(textColor.getText().toString(),working.textColor);try{working.cornerRadius=Math.max(12,Math.min(60,Integer.parseInt(radius.getText().toString().trim())));}catch(Exception ignored){}refreshPreview();}};
        media.addTextChangedListener(tw);name.addTextChangedListener(tw);features.addTextChangedListener(tw);update.addTextChangedListener(tw);bg.addTextChangedListener(tw);accent.addTextChangedListener(tw);textColor.addTextChangedListener(tw);radius.addTextChangedListener(tw);
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MEDIA);});
        save.setOnClickListener(v->{working.mediaUrl=media.getText().toString().trim(); working.mediaType=editingMediaType; working.setFeatures(features.getText().toString()); JSONObject patch=new JSONObject();try{patch.put("dialog",working.toJson());patch.put("updatedAt",String.valueOf(System.currentTimeMillis()));}catch(Exception ignored){}io.execute(()->{String r=Rest.patch(databaseUrl,"/apps/"+enc(key)+".json",patch.toString());main.post(()->{if(r!=null){try{app.put("dialog",working.toJson());app.put("updatedAt",String.valueOf(System.currentTimeMillis()));}catch(Exception ignored){}appDetails(key,app);}else toast("Save failed");});});});
    }

    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_MEDIA&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();io.execute(()->{try{String mime=getContentResolver().getType(u);if(mime==null)mime="image/*";InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;long total=0;while((n=in.read(b))>0){total+=n;if(total>MAX_MEDIA_BYTES){in.close();throw new IOException("too large");}out.write(b,0,n);}in.close();editingMediaType=mime.startsWith("video")?"video":"image";editingMediaUri="data:"+mime+";base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);working.mediaType=editingMediaType;working.mediaUrl=editingMediaUri;main.post(this::refreshPreview);}catch(Exception e){main.post(()->toast("Media must be under 1.5 MB. Use a hosted URL for larger files."));}});}}

    private void refreshPreview(){if(previewHost==null||working==null)return;previewHost.post(()->{previewHost.setPadding(dp(8),dp(8),dp(8),dp(8));previewHost.removeAllViews();previewHost.addView(buildPreview(working),new FrameLayout.LayoutParams(-1,-2));});}

    private View buildPreview(DialogConfig d){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackground(round(d.cardColor,dp(d.cornerRadius)));shell.setPadding(0,0,0,dp(12));
        FrameLayout media=new AspectFrame(this,16f/9f);String m=d.mediaUrl;if(d.mediaType.equals("video")&&!m.startsWith("data:")){VideoView vv=new VideoView(this);vv.setVideoURI(Uri.parse(m));vv.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(0,0);vv.start();});media.addView(vv,new FrameLayout.LayoutParams(-1,-1));}else{ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);media.addView(iv,new FrameLayout.LayoutParams(-1,-1));if(m.startsWith("data:image/"))setDataImage(iv,m);else if(!m.isEmpty())loadImage(iv,m);}View fade=new View(this);GradientDrawable gd=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.TRANSPARENT,Color.parseColor(d.cardColor)});fade.setBackground(gd);media.addView(fade,new FrameLayout.LayoutParams(-1,dp(70),Gravity.BOTTOM));shell.addView(media);
        TextView up=text("UPDATE",15,d.textColor);up.setGravity(Gravity.CENTER);up.setLetterSpacing(.08f);shell.addView(up,new LinearLayout.LayoutParams(-1,dp(28)));TextView title=text(d.name,39,d.textColor);title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.create("sans-serif-condensed",Typeface.NORMAL));shell.addView(title,new LinearLayout.LayoutParams(-1,dp(46)));LinearLayout f=linearCard(d.cardColor,18);for(String s:d.features){TextView row=text("❖  "+s,13,d.textColor);f.addView(row,new LinearLayout.LayoutParams(-1,dp(25)));}LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(7);
        shell.addView(f, fp);LinearLayout bs=new LinearLayout(this);bs.setPadding(dp(8),0,dp(8),0);TextView ex=buttonLike("EXIT");ex.setTextSize(14);TextView upb=buttonLike(d.updateText);upb.setTextSize(14);upb.setBackground(outline(d.accentColor,true));bs.addView(ex,new LinearLayout.LayoutParams(0,dp(46),1));bs.addView(space(12,1));bs.addView(upb,new LinearLayout.LayoutParams(0,dp(46),1));shell.addView(bs,padTop(new LinearLayout.LayoutParams(-1,-2),dp(12)));return shell;
    }

    private static abstract class SimpleTW implements TextWatcher{public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){}}

    private static class DialogConfig{
        String mediaUrl="",mediaType="image",name="MODMASE",updateUrl="",updateText="UPDATE",backgroundColor="#FFD0C8",cardColor="#FFFAFD",accentColor="#70A0DF",textColor="#303136";int cornerRadius=36;List<String> features=new ArrayList<>(Arrays.asList("New Features Available","Previous Bug Fixed"));
        String featuresText(){return String.join("\n",features);} void setFeatures(String s){features.clear();for(String x:s.split("\\R")){x=x.trim();if(!x.isEmpty())features.add(x);}if(features.isEmpty())features.add("New Features Available");}
        JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("mediaUrl",mediaUrl);o.put("mediaType",mediaType);o.put("name",name);o.put("topText","UPDATE");o.put("updateUrl",updateUrl);o.put("updateText",updateText);o.put("backgroundColor",backgroundColor);o.put("cardColor",cardColor);o.put("accentColor",accentColor);o.put("textColor",textColor);o.put("featureTextColor",textColor);o.put("cornerRadius",cornerRadius);JSONArray a=new JSONArray();for(String x:features)a.put(x);o.put("features",a);}catch(Exception ignored){}return o;}
        static DialogConfig defaultConfig(){return new DialogConfig();}
        static DialogConfig from(JSONObject o){DialogConfig d=new DialogConfig();if(o==null)return d;d.mediaUrl=o.optString("mediaUrl","");d.mediaType=o.optString("mediaType","image");d.name=o.optString("name","MODMASE");d.updateUrl=o.optString("updateUrl","");d.updateText=o.optString("updateText","UPDATE");d.backgroundColor=safeColor(o.optString("backgroundColor","#FFD0C8"),"#FFD0C8");d.cardColor=safeColor(o.optString("cardColor","#FFFAFD"),"#FFFAFD");d.accentColor=safeColor(o.optString("accentColor","#70A0DF"),"#70A0DF");d.textColor=safeColor(o.optString("textColor","#303136"),"#303136");d.cornerRadius=Math.max(12,Math.min(60,o.optInt("cornerRadius",36)));JSONArray a=o.optJSONArray("features");d.features.clear();if(a!=null)for(int i=0;i<a.length();i++)d.features.add(a.optString(i,""));if(d.features.isEmpty())d.features.add("New Features Available");return d;}
        void applyPreset(String p){if("Sky".equals(p)){backgroundColor="#FFD0C8";accentColor="#70A0DF";cardColor="#FFFAFD";textColor="#303136";}else if("Rose".equals(p)){backgroundColor="#F8D5DE";accentColor="#D97B9C";cardColor="#FFF9FC";textColor="#342D31";}else if("Mint".equals(p)){backgroundColor="#D9F0E8";accentColor="#5FAF93";cardColor="#F8FFFC";textColor="#263631";}else{backgroundColor="#E9E9E9";accentColor="#808080";cardColor="#FAFAFA";textColor="#242424";}}
    }

    /* ---------------- UI helpers ---------------- */
    private LinearLayout baseRoot(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(Color.parseColor(BG));return r;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(round(CARD,dp(22)));c.setElevation(dp(2));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(16),dp(12),dp(16),0);c.setLayoutParams(p);return c;}
    private LinearLayout linearCard(String color,int radius){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(8),dp(14),dp(8));c.setBackground(round(color,dp(radius)));return c;}
    private LinearLayout dialogForm(){LinearLayout l=new LinearLayout(this);l.setPadding(dp(4),0,dp(4),0);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout toolbar(String s,Runnable back){LinearLayout b=new LinearLayout(this);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(14),dp(10),dp(8),dp(8));b.setBackgroundColor(Color.WHITE);if(back!=null){Button x=smallButton("‹");b.addView(x,toolbarLp(48));x.setOnClickListener(v->back.run());}TextView t=title(s,21);b.addView(t,new LinearLayout.LayoutParams(0,dp(54),1));return b;}
    private LinearLayout.LayoutParams toolbarLp(int w){return new LinearLayout.LayoutParams(dp(w),dp(50));}
    private TextView title(String s,float size){TextView v=text(s,size,TEXT);v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return v;}
    private TextView text(String s,float size,String col){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.parseColor(col));v.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private EditText field(String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val);e.setTextSize(15);e.setSingleLine(false);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round("#F1F3F7",dp(16)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(8);e.setLayoutParams(p);return e;}
    private Button primary(String s){Button b=smallButton(s);b.setTextColor(Color.WHITE);b.setBackground(round(BLUE,dp(16)));return b;}
    private Button secondary(String s){Button b=smallButton(s);b.setTextColor(Color.parseColor(TEXT));b.setBackground(outline("#D9DCE3",false));return b;}
    private Button danger(String s){Button b=smallButton(s);b.setTextColor(Color.WHITE);b.setBackground(round("#D95F63",dp(16)));return b;}
    private Button smallButton(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setMinHeight(0);b.setMinWidth(0);return b;}
    private TextView buttonLike(String s){TextView v=text(s,18,TEXT);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.create("monospace",Typeface.NORMAL));v.setBackground(outline("#080808",false));v.setClickable(true);return v;}
    private GradientDrawable outline(String color,boolean fill){GradientDrawable g=new GradientDrawable();g.setColor(fill?Color.parseColor(color):Color.WHITE);g.setCornerRadius(dp(18));g.setStroke(dp(3),Color.parseColor(fill?"#080808":color));return g;}
    private GradientDrawable round(String c,int r){GradientDrawable g=new GradientDrawable();try{g.setColor(Color.parseColor(c));}catch(Exception e){g.setColor(Color.WHITE);}g.setCornerRadius(r);return g;}
    private View space(int w,int h){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(w),dp(h)));return s;}
    private LinearLayout.LayoutParams padTop(LinearLayout.LayoutParams p,int m){p.topMargin=m;return p;}
    private View wrapView(View v,int l,int t,int r,int b){FrameLayout f=new FrameLayout(this);f.setPadding(dp(l),dp(t),dp(r),dp(b));f.addView(v,new FrameLayout.LayoutParams(-1,-2));return f;}
    private LinearLayout.LayoutParams wrap(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private void setBusy(Button b,boolean busy){b.setEnabled(!busy);b.setText(busy?"PLEASE WAIT…":b.getText());}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private boolean validDb(String u){return u.startsWith("https://")&&u.contains("firebaseio.com");}
    private static String cleanBase(String u){while(u.endsWith("/"))u=u.substring(0,u.length()-1);return u;}
    private static String enc(String s){try{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}catch(Exception e){return s;}}
    private static String dateNow(){return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new Date());}
    private static String genKey(){String abc="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";Random r=new Random();return "MM-"+grp(abc,r)+"-"+grp(abc,r)+"-"+grp(abc,r)+"-ST";}private static String grp(String a,Random r){StringBuilder s=new StringBuilder();for(int i=0;i<3;i++)s.append(a.charAt(r.nextInt(a.length())));return s.toString();}
    private static String sha256(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte z:b)x.append(String.format(Locale.US,"%02x",z));return x.toString();}catch(Exception e){return s;}}
    private static String safeColor(String s,String d){try{Color.parseColor(s);return s;}catch(Exception e){return d;}}
    private void loadImage(ImageView iv,String u){if(u==null||u.isEmpty())return;io.execute(()->{try{if(u.startsWith("data:image/")){int i=u.indexOf(',');byte[] b=Base64.decode(u.substring(i+1),Base64.DEFAULT);Bitmap bm=BitmapFactory.decodeByteArray(b,0,b.length);main.post(()->iv.setImageBitmap(bm));return;}HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);InputStream in=c.getInputStream();Bitmap bm=BitmapFactory.decodeStream(in);in.close();main.post(()->iv.setImageBitmap(bm));}catch(Exception ignored){}});}
    private static void setDataImage(ImageView iv,String d){try{int i=d.indexOf(',');byte[] b=Base64.decode(d.substring(i+1),Base64.DEFAULT);iv.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));}catch(Exception ignored){}}
    private static int dp(Context c,int x){return (int)(x*c.getResources().getDisplayMetrics().density+.5f);} private int dp(int x){return dp(this,x);}

    private static final class AspectFrame extends FrameLayout{private final float ratio;AspectFrame(Context c,float r){super(c);ratio=r;}@Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws);int h=(int)(w/ratio);setMeasuredDimension(w,h);for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));}}

    /* ---------------- REST client ---------------- */
    static final class Rest{
        static String get(String base,String path){return req("GET",base,path,null);}static String put(String base,String path,String body){return req("PUT",base,path,body);}static String patch(String base,String path,String body){return req("PATCH",base,path,body);}static String delete(String base,String path){return req("DELETE",base,path,null);}
        static String req(String m,String base,String path,String body){HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(cleanBase(base)+path).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod(m);c.setRequestProperty("Accept","application/json");if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();if(in==null)return code<400?"{}":null;ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();return code>=400?null:out.toString("UTF-8");}catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}}
        static String cleanBase(String s){while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
    }
}
