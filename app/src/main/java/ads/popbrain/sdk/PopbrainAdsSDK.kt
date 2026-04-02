package ads.popbrain.sdk

import android.content.Context
import android.util.Log

object PopbrainAdsSDK {
    fun initialize(context: Context) {
        InstallReferrerManager.fetchReferrer(context) { result ->
            Log.d("PopbrainSDK", "Conversion detected: ${result.}")
            ConversionTracker.track(result)
        }
    }
}