package ads.popbrain.sdk

import android.util.Log

internal object PopbrainLogger {

    private const val TAG = "PopbrainSDK"

    /**
     * Referrer / clickId data is only written to logcat when the integrator opts in via
     * [PopbrainAdsSDK.setLoggingEnabled]. Off by default so attribution data never leaks
     * from a release build.
     */
    @Volatile
    var enabled: Boolean = false

    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    /** Errors carry no attribution data, so they are always logged. */
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
