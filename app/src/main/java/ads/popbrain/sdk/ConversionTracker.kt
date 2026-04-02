package ads.popbrain.sdk

import android.util.Log
import ads.popbrain.sdk.model.AttributionResult

object ConversionTracker {

    fun track(result: AttributionResult) {
        if (!result.isOrganic) {
            Log.d("PopbrainSDK", "Conversion detected: ${result.referrer}")
        } else {
            Log.d("PopbrainSDK", "Organic install")
        }
    }
}