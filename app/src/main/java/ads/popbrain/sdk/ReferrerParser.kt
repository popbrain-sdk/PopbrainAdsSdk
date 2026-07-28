package ads.popbrain.sdk

import java.net.URLDecoder

/**
 * Parses the raw Play Store install referrer.
 *
 * Deliberately free of Android framework types so it stays unit-testable on the JVM.
 */
object ReferrerParser {

    private val CLICK_ID_KEYS = listOf("clickId", "utm_clickId", "click_id", "cid")
    private val ADVERTISER_ID_KEYS = listOf("advertiserId", "utm_advertiserId", "advertiser_id", "advId")

    fun parse(referrer: String?): AttributionResult {
        if (referrer.isNullOrEmpty() || referrer.contains("organic", ignoreCase = true)) {
            return AttributionResult(isOrganic = true, referrer = referrer)
        }

        val params = splitParams(referrer)

        val extractedClickId = CLICK_ID_KEYS.firstNotNullOfOrNull { params[it]?.takeIf(String::isNotEmpty) }
        val extractedAdvertiserId =
            ADVERTISER_ID_KEYS.firstNotNullOfOrNull { params[it]?.takeIf(String::isNotEmpty) }

        val hasPopbrainSource =
            params["utm_source"]?.contains("popbrain", ignoreCase = true) == true
        val isNotOrganic =
            hasPopbrainSource || extractedClickId != null || params.containsKey("gclid")

        return AttributionResult(
            isOrganic = !isNotOrganic,
            referrer = referrer,
            campaign = params["utm_campaign"],
            clickId = extractedClickId,
            advertiserId = extractedAdvertiserId,
            params = params
        )
    }

    /**
     * Splits on the structural `&` / `=` first and decodes each key and value separately.
     *
     * Decoding the whole string up front (the previous behaviour) corrupted any value that
     * legitimately contained an encoded `&` or `=` — `utm_campaign=a%26b` became an extra
     * bogus parameter instead of the value `a&b`.
     */
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

    /** Falls back to the raw text if the segment is not valid percent-encoding. */
    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
