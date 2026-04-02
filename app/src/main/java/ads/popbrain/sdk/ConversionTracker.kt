package ads.popbrain.sdk

import android.util.Log
import ads.popbrain.sdk.model.AttributionResult

object ConversionTracker {

    fun track(result: AttributionResult) {
        Log.d("PopbrainSDK", "Conversion detected: ${result.referrer}")
        if (!result.isOrganic) {
            Log.d("PopbrainSDK", "Conversion detected: ${result.referrer}")
        } else {
            Log.d("PopbrainSDK", "Organic install")
        }
    }
}