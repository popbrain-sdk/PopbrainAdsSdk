package ads.popbrain.sdk

import android.util.Log
import ads.popbrain.sdk.model.AttributionResult

object ConversionTracker {

    fun track(result: AttributionResult) {
        if (!result.isOrganic) {
            val clickId = extractClickId(result.referrer!!)
            if (clickId.isNotEmpty()) {
                ConversionApi.reportInstall(clickId, result.referrer)
            }
        } else {
            Log.d("PopbrainSDK", "Organic install")
        }
    }

    fun extractClickId(referrer: String): String {
        try {
            val params = referrer.split("&")
            for (param in params) {
                val pair = param.split("=")
                if (pair.size == 2) {
                    val key = pair[0].trim()
                    val value = pair[1].trim()
                    if (key.equals("clickId", ignoreCase = true)) {
                        return value
                    }
                }
            }
        } catch (e: Exception) {
            println("PopbrainSDK: Parsing Error: ${e.message}")
        }
        return ""
    }
}