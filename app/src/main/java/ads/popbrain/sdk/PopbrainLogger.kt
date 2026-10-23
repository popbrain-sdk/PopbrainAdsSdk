package ads.popbrain.sdk

import android.util.Log

internal object PopbrainLogger {

    private const val TAG = "PopbrainSDK"

    @Volatile
    var enabled: Boolean = false

    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
