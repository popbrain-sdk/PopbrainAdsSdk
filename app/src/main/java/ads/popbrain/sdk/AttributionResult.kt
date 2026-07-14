package ads.popbrain.sdk

data class AttributionResult(
    val isOrganic: Boolean,
    val referrer: String? = null,
    val campaign: String? = null,
    val clickId: String? = null
)