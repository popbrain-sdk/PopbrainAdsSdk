package ads.popbrain.sdk

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

object PopbrainAdsSDK {

    private val initialized = AtomicBoolean(false)
    private val executor get() = PopbrainApiClient.executor

    @JvmStatic
    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        try {
            PopbrainStorage.init(appContext)
        } catch (e: Exception) {
            PopbrainLogger.e("SDK storage initialization failed", e)
        }

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
            PopbrainAnalyticsManager.onAttributionResolved()
            ConversionTracker.reportInstallIfNeeded()
            return
        }

        InstallReferrerManager.fetchReferrer(
            context = context,
            onResult = { result -> executor.execute { ConversionTracker.track(result) } },
            onUnavailable = { reason ->
                PopbrainLogger.e("Referrer unavailable ($reason), will retry on next launch")
            }
        )
    }

    @JvmStatic
    @JvmOverloads
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        PopbrainAnalyticsManager.sendEvent(eventName, params)
    }

    @JvmStatic
    fun setAdvertiserId(advertiserId: String) {
        val id = advertiserId.trim()
        if (id.isEmpty()) {
            PopbrainLogger.e("advertiserId is required, ignoring setAdvertiserId call")
            return
        }
        PopbrainStorage.advertiserId = id
    }

    @JvmStatic
    fun getClickId(): String? = PopbrainStorage.clickId

    @JvmStatic
    fun setLoggingEnabled(enabled: Boolean) {
        PopbrainLogger.enabled = enabled
    }
}
