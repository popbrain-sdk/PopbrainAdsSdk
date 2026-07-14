package ads.popbrain.sdk

import ads.popbrain.sdk.AttributionResult
import android.util.Log

object ReferrerParser {

    fun parse(referrer: String?): AttributionResult {
//        Log.d("PopbrainSDK", "Raw Referrer: $referrer")
        if (referrer.isNullOrEmpty() || referrer.contains("organic", ignoreCase = true)) {
            return AttributionResult(isOrganic = true)
        }

        val params = referrer.split("&").mapNotNull {
            val pair = it.split("=")
            if (pair.size == 2) pair[0] to pair[1] else null
        }.toMap()

        val extractedClickId = params["clickId"] ?: params["utm_clickId"]

        val hasPopbrainSource = params["utm_source"]?.contains("popbrain", ignoreCase = true) == true
        val isNotOrganic = hasPopbrainSource || !extractedClickId.isNullOrEmpty() || params.containsKey("gclid")

        return AttributionResult(
            isOrganic = !isNotOrganic,
            referrer = referrer,
            campaign = params["utm_campaign"],
            clickId = extractedClickId
        )
    }
}