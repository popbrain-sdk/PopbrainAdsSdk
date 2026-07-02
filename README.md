# PopbrainAdsSDK

Android SDK for install attribution using Google Play Install Referrer API.

A lightweight Android SDK designed for efficient install attribution and referral tracking.

## Installation

🚀 Quick Start
To get started with the SDK, initialize it in your Application class or main activity:

// Initialize the SDK
PopbrainAnalyticsManager.initAnalytics("your_click_id", "your_advertiser_id")

📊 Usage
Send events easily from anywhere in your app:

val eventData = HashMap<String, Any>()
eventData.put("event_key", "event_value")
PopbrainAnalyticsManager.sendEvent("event_name", eventData)

🛡️ Permissions
Ensure you have the necessary internet permissions in your AndroidManifest.xml:

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

Add dependency:

```gradle
implementation("com.github.popbrain-sdk:PopbrainAdsSdk:1.1.2")
