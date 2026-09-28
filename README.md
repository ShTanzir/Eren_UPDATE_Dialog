# Eren System — MODMASE Update Dialog + Admin Panel (FIXED)

Pure native **Java** dual-app system. GitHub Actions build is fixed (no removed `tools` SDK package).

| App | Package | Purpose |
|-----|---------|---------|
| **Eren** | `com.eren.dialog` | Update dialog shown inside target mod APKs |
| **Eren Admin** | `com.eren.admin` | Admin panel (Firebase RTDB) to manage dialogs |

## Build with GitHub Actions

1. Create a new GitHub repository.
2. Upload / push the **entire** contents of this folder (Eren, ErenAdmin, `.github`).
3. Actions → **Build Eren & Eren Admin** → Run workflow (or push to main).
4. Download APKs from workflow artifacts:
   - `Eren-Dialog-APK` (debug)
   - `Eren-Admin-APK` (debug)

### What was fixed in this version
- `android-actions/setup-android` no longer installs the removed `tools` package
- Explicit packages: `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`
- Gradle 8.7 + AGP 8.5.2
- Builds `assembleDebug` (no release signing issues on CI)
- Removed missing custom font resource references (system fonts used)

## MT Manager workflow (Dialog APK)

1. Build **Eren** APK.
2. Open APK in MT Manager → `classes.dex`.
3. Open `MainActivity.smali` → copy the hook line that starts `Eren`.
4. Delete `MainActivity.smali`.
5. Open `Eren.smali` and replace these two placeholders:
   - `PLACEHOLDER_FIREBASE_DATABASE_URL`
   - `PLACEHOLDER_APP_CONNECT_KEY`
6. Add INTERNET permission (copy from Admin app detail page) into target APK `AndroidManifest.xml`.
7. In Admin: enable **Dialog Show** for that app.

### Dialog behaviour
- **EXIT** → closes the host app
- **UPDATE** → opens the URL from Admin
- Real-time config sync from Firebase

## Admin Panel

1. Enter Firebase **databaseURL** → Connect  
2. Create **Access Key** → next launches require that key  
3. FAB → Add App  
4. App detail: Connect Key, databaseURL, INTERNET line (copy), Enable/Disable, Delete, Edit  

## Firebase rules (test)

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

## Data structure

```
/apps/{appId}
  name, shortDetail, iconUrl, connectKey, databaseURL,
  dialogEnabled, createdAt, dialogConfig { ... }

/accessKeys/{key}
  note, createdAt
```
