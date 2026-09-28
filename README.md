# Eren System — MODMASE Update Dialog + Admin Panel

Pure native **Java** dual-app system:

| App | Package | Purpose |
|-----|---------|---------|
| **Eren** | `com.eren.dialog` | Update dialog shown inside target mod APKs |
| **Eren Admin** | `com.eren.admin` | Admin panel (Firebase RTDB) to manage dialogs |

## How it works (your exact workflow)

1. Build **Eren** APK via GitHub Actions.
2. Open the APK in **MT Manager** → `classes.dex`.
3. Open `MainActivity.smali` → copy the **hook line** (the `invoke-static` / start Activity call).
4. Delete `MainActivity.smali` (or the whole MainActivity class).
5. Open `Eren.smali` (or `Eren$...`) and search for the **two placeholders**:
   - `PLACEHOLDER_FIREBASE_DATABASE_URL`
   - `PLACEHOLDER_APP_CONNECT_KEY`
6. Replace them with the values shown in **Eren Admin → App detail page**.
7. Also add the **INTERNET** permission line (copyable from Admin) into the target APK’s `AndroidManifest.xml`.
8. In Admin: enable **Dialog Show** for that app → the dialog appears in real time on the modded APK (as long as the user has not updated/reinstalled).

### Dialog behaviour
- **EXIT** → closes the entire host app (`finishAffinity` + `System.exit`).
- **UPDATE** → opens the URL configured in Admin.
- UI structure is **pixel-matched** to the original `Eren.html` (same fonts, colors, radii, feature card, buttons).

### Admin Panel
1. First launch → enter your Firebase **databaseURL** → Connect.
2. Create an **Access Key** (with optional note). Next launches require that key.
3. FAB → Add App (name, short detail, icon URL). Date is automatic.
4. Tap an app → detail page shows:
   - App Connect Key (`MM-XXX-XXX-XXX-ST`)
   - Firebase databaseURL
   - INTERNET permission line (one-tap copy)
   - **Dialog Show Enable / Disable**
   - **Delete**
5. Edit icon → live preview dialog + full editor (image URL / gallery / video, brand name, features list, UPDATE URL, colors with hex + presets, fonts).

## Firebase Realtime Database rules (recommended)

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

(For production lock writes behind auth or your access-key check.)

### Data structure

```
/apps/{appId}
  name, shortDetail, iconUrl, connectKey, databaseURL,
  dialogEnabled (bool), createdAt,
  dialogConfig { imageUrl, brandName, brandHighlight, features[], updateUrl, bgColor, accentColor, ... }

/accessKeys/{key}
  note, createdAt
```

## Build with GitHub Actions

1. Create a new GitHub repository.
2. Push the contents of this folder (the whole `Eren-System` root).
3. Actions → **Build Eren & Eren Admin** → Run workflow.
4. Download the two APKs from the workflow artifacts.

### Local build (optional)

```bash
# Eren
cd Eren && ./gradlew assembleRelease

# Eren Admin
cd ErenAdmin && ./gradlew assembleRelease
```

## Icons

Replace the placeholder round icons:

- `Eren/app/src/main/res/mipmap-*/ic_launcher*.png`
- `ErenAdmin/app/src/main/res/mipmap-*/ic_launcher*.png`

Or just drop your PNG into `assets/` and update the adaptive-icon XML.

## Fonts used (same as HTML)

- Audiowide
- Oswald
- Poppins

TTF files are under `res/font/`. If missing, the system falls back gracefully.

## Placeholders (must replace in smali)

```
PLACEHOLDER_FIREBASE_DATABASE_URL
PLACEHOLDER_APP_CONNECT_KEY
```

These appear as plain strings inside `Eren.java` / `Eren.smali`.

---

Made for your exact MT Manager + Firebase RTDB workflow.
