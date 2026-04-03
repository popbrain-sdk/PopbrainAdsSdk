package ads.popbrain.sdk

import ads.popbrain.sdk.model.AttributionResult
import android.util.Log

object ReferrerParser {

    fun parse(referrer: String?): AttributionResult {
        Log.d("PopbrainSDK", "Raw Referrer: $referrer")
        if (referrer.isNullOrEmpty() || referrer.contains("organic", ignoreCase = true)) {
            return AttributionResult(isOrganic = true)
        }

        val params = referrer.split("&").mapNotNull {
            val pair = it.split("=")
            if (pair.size == 2) pair[0] to pair[1] else null
        }.toMap()

        val hasPopbrainSource =
            params["utm_source"]?.contains("popbrain", ignoreCase = true) == true
        val hasClickId = params.containsKey("clickId") || params.containsKey("click_Id")
        val hasGclid = params.containsKey("gclid")

        val isNotOrganic = hasPopbrainSource || hasClickId || hasGclid

        return AttributionResult(
            isOrganic = !isNotOrganic,
            referrer = referrer,
            campaign = params["utm_campaign"]
        )
    }
}