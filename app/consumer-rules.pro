# Rules shipped to any app that embeds this SDK.
# Referenced from build.gradle.kts via consumerProguardFiles.

# Public entry points, called reflectively by integrators / from the manifest.
-keep public class ads.popbrain.sdk.PopbrainAdsSDK { public *; }
-keep public class ads.popbrain.sdk.PopbrainAnalyticsManager { public *; }
-keep public class ads.popbrain.sdk.ReferrerParser { public *; }
-keep public class ads.popbrain.sdk.AttributionResult { *; }

# Instantiated by the framework from the merged manifest.
-keep class ads.popbrain.sdk.SdkInitProvider
-keep class ads.popbrain.sdk.InstallReferrerReceiver

# Play Install Referrer AIDL surface.
-keep class com.android.installreferrer.** { *; }
