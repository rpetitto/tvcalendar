# TV Calendar

A beautiful, always-on Google Calendar display app for Google TV / Android TV.
Built with Kotlin + Jetpack Compose. Ambient, display-only (no remote navigation),
kept fresh by background polling. Personal use — sideloaded via ADB.

## Architecture

| Layer | Implementation |
|---|---|
| UI | Jetpack Compose (`ui/screens`, `ui/components`, `ui/theme`) |
| Auth | OAuth 2.0 Device Flow (`auth/`) |
| Data | Calendar REST API v3 + Room cache, single-source-of-truth repository (`data/`) |
| Background | WorkManager periodic sync + boot receiver (`worker/`) |
| State | `MainViewModel` exposing `StateFlow`s |

```
app/src/main/java/com/rpetitto/tvcalendar/
├── MainActivity.kt          # Compose entry point, window flags, time-tick receiver
├── MainViewModel.kt         # AuthState + CalendarUiState
├── auth/                    # DeviceFlowAuth, OAuthApi, TokenStore, TokenRefresher
├── data/                    # CalendarRepository + remote/ + local/
├── ui/                      # theme/, screens/, components/
└── worker/                  # CalendarSyncWorker, SyncScheduler, BootReceiver
```

## Setup

1. **Google Cloud Console**
   - Enable the **Google Calendar API**.
   - OAuth consent screen → External → add your Google account as a test user.
   - Create an OAuth Client ID of type **TV and Limited Input devices**.
   - Scope: `https://www.googleapis.com/auth/calendar.readonly`.

2. **Credentials** — copy `local.properties.example` to `local.properties` and fill in:
   ```properties
   GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your_client_secret
   ```
   `local.properties` is git-ignored and must never be committed.

3. **Build**
   ```bash
   ./gradlew assembleDebug
   # → app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Sideload to your TV**
   ```bash
   adb connect 192.168.x.x:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.rpetitto.tvcalendar/.MainActivity
   ```

## First launch

The app shows a pairing screen with a user code and a QR of the verification URL.
On your phone, visit the URL, enter the code, and approve the read-only Calendar
scope. The TV then switches to the ambient calendar display and keeps itself in
sync every 15 minutes.

## Notes

- `minSdk` is 21 per spec. `EncryptedSharedPreferences` (token storage) requires
  API 23+ at runtime, so use an Android TV device on API 23 or newer.
- `java.time` is enabled on older APIs via core library desugaring.
