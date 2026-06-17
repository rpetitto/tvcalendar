# Google TV Calendar App

## Project overview

A beautiful, always-on Google Calendar display app for Google TV / Android TV.
Built with Kotlin + Jetpack Compose for TV. Personal use — sideloaded via ADB,
no Play Store submission required. The UI is ambient and display-only (no remote
navigation needed). Calendar data stays fresh via background polling.

The implemented package is `com.rpetitto.tvcalendar`.

---

## Tech stack

| Layer | Library / Tool |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose for TV (`androidx.tv:tv-material`) |
| Auth | Google OAuth 2.0 Device Flow (limited-input device) |
| Calendar data | Google Calendar REST API v3 |
| HTTP client | Retrofit 2 + OkHttp 3 |
| Local cache | Room database |
| Background refresh | WorkManager |
| Secrets storage | Android EncryptedSharedPreferences |
| Build | Gradle Kotlin DSL + version catalog |
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 35 |

---

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/rpetitto/tvcalendar/
│   ├── MainActivity.kt               # Single activity, Compose entry point
│   ├── MainViewModel.kt              # App-level state (AuthState + CalendarUiState)
│   ├── auth/
│   │   ├── OAuthApi.kt               # Retrofit interface for the device-flow endpoints
│   │   ├── DeviceFlowAuth.kt         # OAuth Device Flow logic
│   │   ├── TokenStore.kt             # EncryptedSharedPreferences wrapper
│   │   └── TokenRefresher.kt         # Auto-refresh access tokens
│   ├── data/
│   │   ├── CalendarRepository.kt     # Single source of truth
│   │   ├── remote/
│   │   │   ├── CalendarApiService.kt # Retrofit interface
│   │   │   ├── CalendarModels.kt     # API response data classes
│   │   │   └── RetrofitModule.kt     # Retrofit/OkHttp builder
│   │   └── local/
│   │       ├── CalendarDatabase.kt   # Room DB
│   │       ├── EventDao.kt           # DAO
│   │       └── EventEntity.kt        # Room entity
│   ├── ui/
│   │   ├── theme/                    # Color.kt, Type.kt, Theme.kt
│   │   ├── screens/                  # PairingScreen, CalendarScreen, LoadingScreen
│   │   └── components/               # WeekView, AgendaView, EventCard, ClockWidget,
│   │                                 #   DateHeader, QrCode, EventFormatting
│   └── worker/
│       ├── CalendarSyncWorker.kt     # WorkManager periodic sync
│       ├── SyncScheduler.kt          # Schedules periodic + one-off syncs
│       └── BootReceiver.kt           # Re-schedules sync after reboot
└── res/
    ├── drawable/banner.png           # 320x180 TV launcher banner (placeholder)
    └── values/                       # strings.xml, colors.xml, themes.xml
```

---

## Build phases (as implemented)

1. **Scaffold** — Gradle (version catalog in `gradle/libs.versions.toml`),
   TV-configured manifest (leanback feature, banner, landscape, LEANBACK_LAUNCHER),
   Compose `MainActivity`.
2. **Device Flow OAuth** — `DeviceFlowAuth` requests a device code and polls the
   token endpoint as a `Flow<TokenResult>`; `TokenStore` persists tokens encrypted;
   `TokenRefresher` silently refreshes; `PairingScreen` shows the code + QR.
3. **Calendar data layer** — Retrofit `CalendarApiService`, Gson models, Room
   cache, and a `CalendarRepository` that syncs all calendars 14 days out, maps to
   `EventEntity`, and exposes per-day / per-week `Flow`s plus a midnight tick.
4. **Compose UI** — dark ambient theme, live `ClockWidget`, 7-day `WeekView`
   sidebar, scrolling `AgendaView` of `EventCard`s. No focusable elements.
5. **Always-on reliability** — `FLAG_KEEP_SCREEN_ON`, per-minute time-tick day
   rollover, 15-minute WorkManager sync, boot receiver, resume-after-30-min sync.
6. **Sideload** — see `README.md`.

---

## Key API call

```
GET https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events
  ?timeMin={now ISO8601}
  &timeMax={now + 14 days ISO8601}
  &singleEvents=true
  &orderBy=startTime
  &maxResults=250
Authorization: Bearer {access_token}
```

OAuth endpoints (host `https://oauth2.googleapis.com/`):
- Device code: `POST /device/code`
- Token poll / refresh: `POST /token`

---

## Configuration

`local.properties` (never committed — see `local.properties.example`):

```properties
GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_client_secret
```

These are exposed via `BuildConfig.GOOGLE_CLIENT_ID` / `BuildConfig.GOOGLE_CLIENT_SECRET`.

### Google Cloud Console checklist
- [ ] Enable **Google Calendar API**
- [ ] OAuth consent screen → External → add your account as a test user
- [ ] Credentials → OAuth Client ID → **TV and Limited Input devices**
- [ ] Scope `https://www.googleapis.com/auth/calendar.readonly`
- [ ] App stays in "Testing" mode — no verification needed for personal use

---

## Coding conventions

- All async work uses Kotlin coroutines + Flow, no RxJava.
- ViewModels expose `StateFlow`, not `LiveData`.
- Repository is the single source of truth — the UI never calls the API directly.
- No hardcoded user-facing strings in UI — use `strings.xml`.
- All date/time handling uses `java.time` with desugaring enabled for API 21.
- Error states are explicit sealed classes, not nullable returns.
- No force-unwrap (`!!`).

---

## What you must do outside this repo

- Create the Google Cloud project and OAuth credentials (browser).
- Put the credentials in `local.properties`.
- Run the ADB sideload commands against your TV.
- Approve the OAuth consent on your phone during first launch.
