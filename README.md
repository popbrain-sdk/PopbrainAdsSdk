# PopbrainAdsSDK

Android SDK for install attribution using the Google Play Install Referrer API.

The SDK reads the `clickId` that Popbrain attaches to the Play Store referrer, reports the
install back to Popbrain so the click can be counted as a conversion, and attributes any
in-app events you send to that same click.

## Installation

```gradle
implementation("com.github.popbrain-sdk:PopbrainAdsSdk:1.1.1")
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
