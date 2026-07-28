package ads.popbrain.sdk

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

object PopbrainAdsSDK {

    private val initialized = AtomicBoolean(false)
    private val executor get() = PopbrainApiClient.executor

    /**
     * Invoked automatically by [SdkInitProvider] on app start — integrators do not need to
     * call this. Safe to call more than once; the work runs only on the first call.
     */
    @JvmStatic
    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        // Kept off the main thread: this touches SharedPreferences, and onCreate of a
        // ContentProvider runs before the host app's first frame.
        executor.execute {
            try {
                PopbrainStorage.init(appContext)
                resolveAttribution(appContext)
            } catch (e: Exception) {
                PopbrainLogger.e("SDK initialization failed", e)
            }
        }
    }

    private fun resolveAttribution(context: Context) {
        if (PopbrainStorage.referrerProcessed) {
            // Referrer already read on a previous launch. Do not bind to the Play service
            // again — that is what caused a fresh install conversion on every app start.
            PopbrainAnalyticsManager.onAttributionResolved()
            ConversionTracker.reportInstallIfNeeded()
            return
        }

        InstallReferrerManager.fetchReferrer(
            context = context,
            onResult = { result -> executor.execute { ConversionTracker.track(result) } },
            onUnavailable = { reason ->
                // Deliberately not persisted as organic. A transient Play Store failure must
                // not permanently misclassify a paid install; the next launch retries.
                PopbrainLogger.e("Referrer unavailable ($reason), will retry on next launch")
            }
        )
    }

    /**
     * Reports an in-app event (purchase, signup, level_complete…). Events fired before the
     * referrer has been read are queued and sent as soon as the clickId is known.
     */
    @JvmStatic
    @JvmOverloads
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        PopbrainAnalyticsManager.sendEvent(eventName, params)
    }

    /** Optional — only needed when the advertiser id does not arrive in the referrer. */
    @JvmStatic
    fun setAdvertiserId(advertiserId: String) {
        val id = advertiserId.trim()
        if (id.isEmpty()) {
            PopbrainLogger.e("advertiserId is required, ignoring setAdvertiserId call")
            return
        }
        PopbrainStorage.advertiserId = id
    }

    /** The clickId this install was attributed to, or null for an organic install. */
    @JvmStatic
    fun getClickId(): String? = PopbrainStorage.clickId

    /** Enables logcat output of referrer/clickId data. Leave off in release builds. */
    @JvmStatic
    fun setLoggingEnabled(enabled: Boolean) {
        PopbrainLogger.enabled = enabled
    }
}
