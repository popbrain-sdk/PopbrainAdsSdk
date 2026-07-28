# PopbrainAdsSDK

Android SDK for install attribution using the Google Play Install Referrer API.

The SDK reads the `clickId` that Popbrain attaches to the Play Store referrer, reports the
install back to Popbrain so the click can be counted as a conversion, and attributes any
in-app events you send to that same click.

## Installation

```gradle
implementation("com.github.popbrain-sdk:PopbrainAdsSdk:1.2.0")
```

## 🚀 Quick Start

**Nothing to call.** The SDK self-initialises on app start via a `ContentProvider`, reads the
install referrer, extracts the `clickId` and reports the install automatically.

The install is reported **once per device** — it is only marked as sent after the server
confirms it, so an install that happens offline is retried on the next launch.

## 📊 In-app events

Send events from anywhere in your app:

```kotlin
PopbrainAdsSDK.trackEvent(
    "purchase",
    mapOf("value" to 499, "currency" to "INR")
)
```

`eventName` is **mandatory** — a blank or whitespace-only name is rejected and the event is
dropped rather than sent.

Events fired before the referrer has been read are queued in memory and sent as soon as the
`clickId` is known, so an event at app start is not lost.

`clickId`, `advertiserId` and `eventName` are reserved — passing them in `params` is ignored
so an event can never be re-attributed to a different click.

## ⚙️ Optional configuration

```kotlin
// Only if your advertiser id does not arrive in the referrer
PopbrainAdsSDK.setAdvertiserId("your_advertiser_id")

// The clickId this install was attributed to (null when organic)
val clickId = PopbrainAdsSDK.getClickId()

// Referrer/clickId logging — keep off in release builds
PopbrainAdsSDK.setLoggingEnabled(BuildConfig.DEBUG)
```

If you already know the ids out of band, the explicit entry point still works:

```kotlin
PopbrainAnalyticsManager.initAnalytics("your_click_id", "your_advertiser_id")
PopbrainAnalyticsManager.sendEvent("event_name", mapOf("event_key" to "event_value"))
```

## 🛡️ Permissions

The SDK declares everything it needs and the manifest merger adds it to your app — you do not
need to add these yourself:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE" />
```

## 🔧 For maintainers — switching environment

The backend is selected by a single constant in `PopbrainEnv.kt`. It is baked into the
published AAR, so set it to match the release target before pushing:

| Target | `BASE_URL` | Remote |
|---|---|---|
| Test | `https://devserver.popbrain.ai/` | `PopbrainTestSdk` |
| Production | `https://server.popbrain.ai/` | `origin` |

### Endpoints

| Purpose | Method | Path |
|---|---|---|
| Install conversion | `GET` | `api/v1/analytics/install/add?clickId={clickId}` |
| In-app event | `POST` | `api/v1/pixel/s2s` |

Both calls forward **every field the SDK has**, and the server decides what to keep.

The install call sends `clickId` plus the rest of the parsed referrer, each key and value
percent-encoded:

```
api/v1/analytics/install/add?clickId=abc123&utm_source=popbrain&utm_campaign=summer
```

The event call sends `clickId`, `eventName`, `advertiserId` when known, and any params the
integrator attached:

```json
{
  "clickId": "abc123",
  "advertiserId": "adv_99",
  "eventName": "purchase",
  "value": 499,
  "currency": "INR"
}
```

`clickId`, `advertiserId` and `eventName` are reserved — a custom param using one of those
keys is ignored so an event cannot be re-attributed to a different click.

## 📦 Footprint

| Dependency | Size | Why |
|---|---|---|
| `kotlin-stdlib` | 1.7 MB | Already present in any Kotlin app |
| `annotations` | 17 KB | Transitive of kotlin-stdlib |
| `installreferrer` | 8 KB | Play referrer — the only real dependency |

**Total 1.69 MB across 3 artifacts**, plus a 42 KB AAR. Discounting `kotlin-stdlib`, which
every Kotlin app already ships, the SDK adds roughly **65 KB** to an integrator's app.

Deliberately absent:

- **No `appcompat` / `material` / `androidx.core`** — this is a headless SDK with no UI. They
  were pulling in ~9 MB of transitive dependencies (lifecycle, coroutines, fragment,
  recyclerview, constraintlayout) that no code path touched.
- **No OkHttp / Retrofit / Gson** — the SDK makes two API calls. It uses the platform's
  `HttpURLConnection` (itself backed by OkHttp inside Android, so nothing is lost) and builds
  JSON with the framework's `org.json`.
- **No resources** — the template `themes.xml` / `colors.xml` / `strings.xml` were merging a
  `Theme.PopbrainAdsSdk` and an `app_name` string into every consumer app.

## Error handling

API failures are classified so retries only happen when they can help:

| Outcome | Examples | Behaviour |
|---|---|---|
| Success | 2xx | Install marked reported |
| Retryable | offline, timeout, 5xx, 408, 429 | Install retried next launch (max 10); events retried twice with backoff |
| Permanent | 4xx | Retry loop stopped, logged as an error |

## Attribution flow

```
Popbrain ad click → clickId → Play Store referrer
                                     ↓
                    SdkInitProvider (auto-init on app start)
                                     ↓
              InstallReferrerManager → ReferrerParser (organic vs paid)
                                     ↓
                    ConversionTracker → /analytics/install/add
                                     ↓
              PopbrainAnalyticsManager → /pixel/s2s  (in-app events)
```
