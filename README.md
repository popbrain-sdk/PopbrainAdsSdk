# PopbrainAdsSDK

Android SDK for install attribution using the Google Play Install Referrer API.

The SDK reads the `clickId` that Popbrain attaches to the Play Store referrer, reports the
install back to Popbrain so the click can be counted as a conversion, and attributes any
in-app events you send to that same click.

## Installation

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then the dependency:

```gradle
implementation("com.github.popbrain-sdk:PopbrainAdsSdk:1.2.2")
```

Requirements: `minSdk` 21 or higher, and the app must be installed **from Google Play** — an
APK sideload or another store never carries a Play install referrer.

## 🔗 Play Store URL for your Popbrain campaign

The SDK can only attribute an install if the Popbrain `clickId` reaches the app. Google Play
passes **only the `referrer` parameter** of the store link to the app, so the campaign's target
URL must contain a `referrer` parameter. Popbrain puts the `clickId` into it on every click.

✅ **Use this target URL** (replace the package name):

```
https://play.google.com/store/apps/details?id=com.your.app&referrer=utm_source%3Dpopbrain
```

After a click, Popbrain turns it into
`...&referrer=utm_source%3Dpopbrain%26utm_clickId%3D<clickId>%26clickId%3D<clickId>` and the app
receives `utm_source=popbrain&utm_clickId=<clickId>&clickId=<clickId>`.

| Target URL | What the app receives | Install counted |
|---|---|---|
| `https://play.google.com/store/apps/details?id=com.your.app&referrer=utm_source%3Dpopbrain` | `utm_source=popbrain&utm_clickId=…&clickId=…` | ✅ Yes (recommended) |
| `https://play.google.com/store/apps/details?id=com.your.app&referrer=clickId%3D{CLICK_ID}` | `clickId=…&utm_clickId=…` | ✅ Yes |
| `https://play.google.com/store/apps/details?id=com.your.app&referrer={CLICK_ID}` | the bare `clickId` | ✅ Yes (from 1.2.2) |
| `https://play.google.com/store/apps/details?id=com.your.app` | nothing from Popbrain — Play reports `utm_source=google-play&utm_medium=organic` | ❌ **No**, every install looks organic |
| `market://details?id=…`, a short link or your own redirect page | depends — the `referrer` is usually lost | ❌ Not supported |

Rules for the `referrer` value:

- Inside `referrer`, `=` must be written as `%3D` and `&` as `%26`, otherwise Play cuts the
  value at the first `&` and the rest becomes a separate (ignored) parameter.
- You may add your own keys, e.g. `referrer=utm_source%3Dpopbrain%26utm_campaign%3Dsummer` —
  they are forwarded with the install. Keep it short: the install call carries at most 20
  fields, keys up to 60 and values up to 400 characters.
- Do not put your own `clickId` / `click_id` / `utm_clickId` in the URL — Popbrain sets them.

**How to test before going live:** upload the build to a Play Console internal testing track,
open the target URL from a Popbrain test click on a device that does not have the app, install
from Play, then launch the app with `PopbrainAdsSDK.setLoggingEnabled(true)` and look for
`Reporting install (attempt 1) for clickId=…` in Logcat (tag `PopbrainSDK`). The Play referrer
stays available for 90 days after install.

## 🚀 Quick Start

**Nothing to call.** The SDK self-initialises on app start via a `ContentProvider`, reads the
install referrer, extracts the `clickId` and reports the install automatically.

The install is reported **once per device** — it is only marked as sent after the server
confirms it, so an install that happens offline is retried on the next launch.

An install counts as a Popbrain install only when the referrer carries a valid Popbrain
`clickId` (a 24 character id, sent by Popbrain as both `clickId` and `utm_clickId`). Anything
else — Play's `utm_medium=organic`, a Google Ads `gclid`, other campaigns — is organic and
nothing is reported, because the server can only count an install against a real click.

## 📊 In-app events

Send events from anywhere in your app:

```kotlin
PopbrainAdsSDK.trackEvent(
    "purchase",
    mapOf("value" to 499, "currency" to "INR")
)
```

`eventName` is **mandatory** and must be **2–40 letters, numbers or underscores**
(`purchase`, `add_to_cart`). Any other name (`add to cart`, `add-to-cart`) is rejected by the
server, so the SDK drops it up front and logs an error.

Events fired before the referrer has been read are queued in memory and sent as soon as the
`clickId` is known, so an event at app start is not lost.

`clickId`, `advertiserId` and `eventName` are reserved — passing them in `params` is ignored
so an event can never be re-attributed to a different click.

`params` follow the server's rules. A param that breaks them is dropped with an error log, and
the event itself is still sent:

| Rule | Limit |
|---|---|
| Value type | `String`, `Number` or `Boolean` — maps, lists and `null` are dropped |
| Key length | max 60 characters |
| String value | max 400 characters |
| Number of params | max 17 (18 when no `advertiserId`) — the request carries 20 fields in total |
| `value` | number between `0.01` and `99999999` |
| `currency` | upper case 3 letter ISO code such as `INR`, `USD` — an unknown code is still rejected by the server |
| `userId` | 12–40 characters |

## ⚙️ Optional configuration

```kotlin
// Only if your advertiser id does not arrive in the referrer
PopbrainAdsSDK.setAdvertiserId("your_advertiser_id")

// The clickId this install was attributed to (null when organic)
val clickId = PopbrainAdsSDK.getClickId()

// Referrer/clickId logging — keep off in release builds
PopbrainAdsSDK.setLoggingEnabled(BuildConfig.DEBUG)
```

If you already know the ids out of band, the explicit entry point still works. The `clickId`
must be the 24 character Popbrain click id, any other value is ignored:

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

`v1`, `v2` and `v3` of both endpoints run the same server code, so the SDK stays on `v1`.

Both calls forward the fields the SDK has, trimmed to what the server accepts — a request the
server would reject with `400` is never sent, because that would lose the install or event
for good. Both endpoints require a 24 character `clickId` and accept at most 20 fields, keys
up to 60 characters and string values up to 400 characters.

The install call sends `clickId` first, then the rest of the parsed referrer, each key and
value percent-encoded. Other click id keys (`click_id`, `utm_clickId`, …) are left out so the
server's `click_id` → `clickId` rename can never replace the real one:

```
api/v1/analytics/install/add?clickId=65a1b2c3d4e5f60718293a4b&utm_source=popbrain&utm_campaign=summer
```

The event call sends `clickId`, `eventName`, `advertiserId` when known, and any params the
integrator attached:

```json
{
  "clickId": "65a1b2c3d4e5f60718293a4b",
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
| Permanent | 4xx (e.g. `404` click or event not found, `422` conversion window expired) | Retry loop stopped, logged as an error |

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
