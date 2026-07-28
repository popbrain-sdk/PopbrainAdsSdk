package ads.popbrain.sdk

data class AttributionResult(
    val isOrganic: Boolean,
    val referrer: String? = null,
    val campaign: String? = null,
    val clickId: String? = null,
    val advertiserId: String? = null,
    /** Decoded referrer key/value pairs, forwarded as-is to the install endpoint. */
    val params: Map<String, String> = emptyMap()
)
