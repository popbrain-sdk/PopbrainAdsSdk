package ads.popbrain.sdk

import ads.popbrain.sdk.model.AttributionResult

object ReferrerParser {

    fun parse(referrer: String?): AttributionResult {

        if (referrer.isNullOrEmpty()) {
            return AttributionResult(isOrganic = true)
        }

        val params = referrer.split("&")
            .mapNotNull {
                val pair = it.split("=")
                if (pair.size == 2) pair[0] to pair[1] else null
            }
            .toMap()

        val isPopbrain = params["utm_source"]?.contains("popbrain", ignoreCase = true) == true

        return AttributionResult(
            isOrganic = !isPopbrain && !params.containsKey("gclid"),
            referrer = referrer,
            campaign = params["utm_campaign"]
        )
    }
}