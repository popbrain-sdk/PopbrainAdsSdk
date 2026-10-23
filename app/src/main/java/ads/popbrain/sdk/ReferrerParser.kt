package ads.popbrain.sdk

import java.net.URLDecoder

object ReferrerParser {

    private val CLICK_ID_KEYS = listOf("clickId", "utm_clickId", "click_id", "cid")
    private val ADVERTISER_ID_KEYS = listOf("advertiserId", "utm_advertiserId", "advertiser_id", "advId")

    fun parse(referrer: String?): AttributionResult {
        if (referrer.isNullOrEmpty()) {
            return AttributionResult(isOrganic = true, referrer = referrer)
        }

        val params = splitParams(referrer)

        val extractedClickId = CLICK_ID_KEYS.firstNotNullOfOrNull {
            params[it]?.trim()?.takeIf(PopbrainServerRules::isValidClickId)
        }
        val extractedAdvertiserId =
            ADVERTISER_ID_KEYS.firstNotNullOfOrNull { params[it]?.takeIf(String::isNotEmpty) }

        return AttributionResult(
            isOrganic = extractedClickId == null,
            referrer = referrer,
            campaign = params["utm_campaign"],
            clickId = extractedClickId,
            advertiserId = extractedAdvertiserId,
            params = params
        )
    }

    private fun splitParams(referrer: String): Map<String, String> {
        val params = LinkedHashMap<String, String>()
        for (pair in referrer.split("&")) {
            if (pair.isEmpty()) continue
            val separator = pair.indexOf('=')
            if (separator <= 0) continue
            val key = decode(pair.substring(0, separator))
            val value = decode(pair.substring(separator + 1))
            if (key.isNotEmpty()) params[key] = value
        }
        return params
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
