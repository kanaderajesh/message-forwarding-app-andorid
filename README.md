# SMS Forwarder — Android App

A lightweight Android background service app that monitors incoming SMS messages and automatically forwards them to a configured phone number **only when the message body contains one or more keywords from a user-defined list**.

---

## Features

- Runs as a **foreground service** — survives screen-off and low-memory situations
- **Keyword filtering** — only forwards messages that contain at least one keyword (case-insensitive, partial match)
- **Persistent configuration** — forward-to number and keyword list survive app restarts
- **Start / Stop control** from the UI
- Real-time status indicator (Active / Stopped)
- Works on Android 8.0+ (API 26+)

---

## App Architecture

```
app/src/main/
├── java/com/smsforwarder/
│   ├── MainActivity.kt          # UI: configure number, keywords, start/stop
│   ├── SmsReceiver.kt           # BroadcastReceiver: intercepts incoming SMS
│   ├── SmsForwardingService.kt  # ForegroundService: keeps app alive
│   ├── KeywordsAdapter.kt       # RecyclerView adapter for keyword list
│   └── PrefsManager.kt          # SharedPreferences helper
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   └── item_keyword.xml
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

### How it works

```
Incoming SMS
     │
     ▼
 SmsReceiver (manifest-registered, always active)
     │
     ├─ forwarding disabled?  ──► ignore
     ├─ no forward number?    ──► ignore
     ├─ no keywords?          ──► ignore
     │
     └─ body contains keyword?
           YES ──► SmsManager.sendMultipartTextMessage(forwardTo)
           NO  ──► ignore
```

---

## Development Setup

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| **JDK** | 17 (recommended) or 11 | [Adoptium](https://adoptium.net/) |
| **Android Studio** | Hedgehog (2023.1.1) or newer | [developer.android.com/studio](https://developer.android.com/studio) |
| **Android SDK** | API 34 (Android 14) | Via Android Studio SDK Manager |
| **Gradle** | 8.0 (bundled via wrapper) | Included — no manual install needed |

> **Note:** You do **not** need to install Gradle manually. The `gradlew` wrapper script downloads the correct version automatically on first build.

---

### 1. Clone the repository

```bash
git clone https://github.com/kanaderajesh/message-forwarding-app-andorid.git
cd message-forwarding-app-andorid
git checkout claude/android-sms-forwarder-ZYYHu
```

### 2. Open in Android Studio

1. Launch **Android Studio**
2. Click **File → Open** (or "Open" on the welcome screen)
3. Navigate to the cloned folder and click **OK**
4. Wait for Gradle sync to finish (bottom status bar shows progress)
5. If prompted, accept any SDK license agreements

### 3. Install required SDK components

Android Studio will prompt you automatically, but you can also check manually:

**Tools → SDK Manager → SDK Platforms tab**
- ✅ Android 14 (API 34)

**Tools → SDK Manager → SDK Tools tab**
- ✅ Android SDK Build-Tools 34
- ✅ Android Emulator (optional, for testing)

---

## Building the APK

### Option A — Android Studio GUI (recommended)

1. Open the project in Android Studio
2. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**
3. Wait for the build to complete
4. Click **locate** in the notification that appears, or find the APK at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option B — Command Line (Linux / macOS)

```bash
# Make the wrapper executable (first time only)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Option C — Command Line (Windows)

```cmd
gradlew.bat assembleDebug
```

Output: `app\build\outputs\apk\debug\app-debug.apk`

---

## Building a Release APK (for deployment)

A release APK must be **signed** before it can be installed on devices or uploaded to the Play Store.

### Step 1 — Generate a keystore (one-time setup)

```bash
keytool -genkey -v \
  -keystore smsforwarder-release.jks \
  -alias smsforwarder \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted for a password and some identity details. **Keep this `.jks` file safe — you cannot update the app without it.**

### Step 2 — Configure signing in `app/build.gradle`

Add a `signingConfigs` block inside the `android {}` block:

```groovy
android {
    ...
    signingConfigs {
        release {
            storeFile     file("../smsforwarder-release.jks")
            storePassword "YOUR_STORE_PASSWORD"
            keyAlias      "smsforwarder"
            keyPassword   "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            minifyEnabled false
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

> **Security tip:** Instead of hard-coding passwords, store them in `local.properties` (which is git-ignored) and read them with `project.properties["..."]`.

### Step 3 — Build the signed release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Alternative — Sign via Android Studio

1. **Build → Generate Signed Bundle / APK**
2. Choose **APK**
3. Create or select a keystore
4. Choose **release** build variant
5. Click **Finish**

---

## Installing the APK on a Device

### Via ADB (USB)

```bash
# Enable USB Debugging on the device:
# Settings → About Phone → tap Build Number 7 times
# Settings → Developer Options → USB Debugging → ON

adb install app/build/outputs/apk/debug/app-debug.apk
```

### Direct transfer

1. Copy the `.apk` file to the device (USB, email, cloud storage)
2. On the device: **Settings → Security → Install unknown apps** → allow your file manager
3. Open the APK file and tap **Install**

---

## Permissions

| Permission | Why it's needed |
|---|---|
| `RECEIVE_SMS` | Intercept incoming SMS messages |
| `SEND_SMS` | Forward the matched message |
| `READ_SMS` | Read message content for keyword matching |
| `FOREGROUND_SERVICE` | Run the monitoring service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Required on Android 14+ for the data-sync foreground type |
| `POST_NOTIFICATIONS` | Show the persistent service notification (Android 13+) |

All runtime permissions (`RECEIVE_SMS`, `SEND_SMS`, `READ_SMS`, `POST_NOTIFICATIONS`) are requested from the user when **Start Forwarding** is tapped.

---

## Usage

1. **Install** the APK and open the app.
2. Enter the **phone number** to forward messages to and tap **Save Number**.
3. Type a keyword in the **New Keyword** field and tap **Add**. Repeat for as many keywords as needed.
   - Keywords are matched **case-insensitively** (e.g., `OTP` matches `Your OTP is 1234`).
   - Tap the **trash icon** next to a keyword to remove it.
4. Tap **Start Forwarding**. Grant the requested permissions.
5. A persistent notification confirms the service is active.
6. Tap **Stop Forwarding** to disable forwarding at any time.

---

## Important Notes

- The **SMS Receiver** is registered in the manifest, meaning it activates even if the app is not open. Forwarding only occurs when the service has been started (i.e., the user has explicitly enabled it via the UI).
- On some OEM ROMs (Xiaomi MIUI, Huawei EMUI, Samsung One UI), you may need to whitelist the app in **Battery Optimization** settings to prevent the system from killing the service.
  - **Settings → Apps → SMS Forwarder → Battery → Unrestricted**
- The app targets **Android 14 (API 34)** and has a minimum SDK of **Android 8.0 (API 26)**.

---

## Project Dependencies

| Library | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.12.0 | Kotlin Android extensions |
| `androidx.appcompat:appcompat` | 1.6.1 | Backwards-compatible UI components |
| `com.google.android.material:material` | 1.10.0 | Material Design components |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Flexible layout engine |
| `androidx.recyclerview:recyclerview` | 1.3.2 | Keywords list UI |

---

## License

MIT
