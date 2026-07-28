package ads.popbrain.sdk

internal object ConversionApi {

    private const val INSTALL_PATH = "api/v1/analytics/install/add"

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
