package ads.popbrain.sdk

internal object ConversionApi {

    /** `{base}/api/v1/analytics/install/add?clickId={clickId}&<rest of the referrer>` */
    private const val INSTALL_PATH = "api/v1/analytics/install/add"

    /**
     * Forwards the whole parsed referrer, not just the clickId — the server receives every
     * field the Play referrer carried (utm_source, utm_campaign, gclid, …) and decides what
     * to keep. Each key and value is percent-encoded by [PopbrainApiClient].
     */
    fun reportInstall(
        params: Map<String, String>,
        clickId: String?,
        onResult: (ApiResult) -> Unit
    ) {
        val query = LinkedHashMap(params)
        if (clickId != null && !query.containsKey("clickId")) {
            query["clickId"] = clickId
        }
        PopbrainApiClient.get(INSTALL_PATH, query, "Install report", onResult)
    }
}
