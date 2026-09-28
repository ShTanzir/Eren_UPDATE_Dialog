# Eren Workspace — Native Java Update Dialog + Admin Panel

This repository contains two Android applications built with **pure native Java UI code (no XML layouts)**:

- `eren-dialog/` → the injectable update-dialog APK.
- `eren-admin/` → the Firebase Realtime Database admin panel.

## Important architecture note

The apps use the **Firebase Realtime Database REST API** rather than bundling the Firebase Android SDK. Firebase documents that a Realtime Database URL can be used as a REST endpoint by appending `.json`, and that GET/PUT/PATCH/DELETE are supported. This is why the dialog APK can work with the two values you requested: **databaseURL + App Connect Key**.

The dialog polls its app node every ~2.5 seconds. Firebase REST also supports server-sent events if you later want to replace the polling loop with a true streaming client.

## Data model

```text
/adminKeys/<sha256(access-key)>
  createdAt: 1712345678901
  label: "Main Admin"

/apps/<MM-XXX-XXX-XXX-ST>
  name: "MODMASE"
  shortDetail: "..."
  iconUrl: "https://.../icon.png"
  databaseUrl: "https://...firebaseio.com"
  internetPermission: "<uses-permission android:name=... />"
  enabled: true
  createdAt: "28 Sep 2026"
  updatedAt: "172..."
  dialog:
    mediaUrl: "https://.../image.png"
    mediaType: "image"
    name: "MODMASE"
    topText: "UPDATE"
    features: ["New Features Available", "Previous Bug Fixed"]
    updateText: "UPDATE"
    updateUrl: "https://..."
    backgroundColor: "#FFD0C8"
    cardColor: "#FFFAFD"
    accentColor: "#70A0DF"
    textColor: "#303136"
    cornerRadius: 36
```

## Eren dialog workflow

1. Build `eren-dialog`.
2. Open `classes.dex` in MT Manager.
3. `MainActivity.smali` contains this hook:

```smali
invoke-static {p0}, Lcom/eren/dialog/Eren;->show(Landroid/app/Activity;)V
```

4. Copy that hook into the Activity you want to trigger the dialog from.
5. The `Eren.smali` class contains these two easy-to-find strings:

```text
https://YOUR_DATABASE_URL
MM-XXX-XXX-XXX-ST
```

6. Replace them with the app's Firebase `databaseURL` and generated App Connect Key.

7. Enable that app's dialog in Eren Admin.

The dialog APK is built with minification off so the class/method names remain readable.

## Eren Admin workflow

1. Launch **Eren Admin**.
2. Enter your Firebase RTDB URL and press `CONNECT`.
3. Create an admin access key. Save it.
4. Use that key to log in later.
5. Add an app. The admin panel automatically generates a unique App Connect Key.
6. Open the app page to copy the key, database URL and INTERNET permission line.
7. Open **EDIT** to configure the dialog.
8. The editor has a live preview, media URL/local picker, name, feature lines, update URL, color presets, custom HEX colors and corner radius.
9. Save. The dialog APK picks up the new `updatedAt` value on its next polling cycle.
10. `Dialog Show` can disable the dialog without rebuilding the target APK.

## Media

The editor supports image/video URLs. It can also put a small image/video from Android's document picker into the database as a Base64 data URL. The current client intentionally caps local embedded media at about **1.5 MB**; larger video should be hosted by URL instead. This keeps RTDB payloads from becoming enormous.

## Firebase security

The requested key-only admin model is a **client-side access-key design**, not a secure server-side authentication system. The APK contains the database URL, and REST calls are governed by your Firebase Realtime Database Security Rules. Do not treat the access key as a substitute for Firebase Authentication or server-side authorization. For a production/public deployment, lock your database rules down and put privileged writes behind authenticated identities or a trusted backend.

For a private modding workflow, this structure is simple and works without `google-services.json`.

## Build

This project is configured for Android Gradle Plugin 8.13.2 / Gradle 8.13 / JDK 17, with `compileSdk 36`. The Android documentation lists Gradle 8.13 as the required version for AGP 8.13 and JDK 17 as the required/default toolchain.

GitHub Actions builds both APKs.

### Local

Install JDK 17 + Android SDK 36 + Gradle 8.13, then run:

```bash
gradle :eren-dialog:assembleRelease :eren-admin:assembleRelease
```

### GitHub Actions

Push the folder to a GitHub repository. The workflow in `.github/workflows/build.yml` creates:

- `eren-dialog-release`
- `eren-admin-release`
- `eren-dialog-debug`
- `eren-admin-debug`

## Combined repository

Use `Eren-Workspace.zip` when you want one repository/one GitHub Action to build both apps.

## Firebase REST references

- Firebase REST setup: https://firebase.google.com/docs/database/rest/start
- Firebase REST read/write reference: https://firebase.google.com/docs/reference/rest/database
