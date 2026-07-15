package ads.popbrain.sdk

import ads.popbrain.sdk.AttributionResult
import android.util.Log
import java.net.URLDecoder

object ReferrerParser {

    fun parse(referrer: String?): AttributionResult {
        if (referrer.isNullOrEmpty() || referrer.contains("organic", ignoreCase = true)) {
            return AttributionResult(isOrganic = true)
        }

        val decodedReferrer = try {
            URLDecoder.decode(referrer, "UTF-8")
        } catch (e: Exception) {
            referrer
        }

        val params = decodedReferrer.split("&").mapNotNull {
            val pair = it.split("=")
            if (pair.size >= 2) {
                pair[0] to pair.drop(1).joinToString("=")
            } else null
        }.toMap()

        val extractedClickId = params["clickId"] ?: params["utm_clickId"]

        val hasPopbrainSource =
            params["utm_source"]?.contains("popbrain", ignoreCase = true) == true
        val isNotOrganic =
            hasPopbrainSource || !extractedClickId.isNullOrEmpty() || params.containsKey("gclid")
        println("PopbrainSDK: Referrer : $referrer")
        return AttributionResult(
            isOrganic = !isNotOrganic,
            referrer = referrer,
            campaign = params["utm_campaign"],
            clickId = extractedClickId
        )
    }
}