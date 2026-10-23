package ads.popbrain.sdk

internal object ConversionApi {

    private const val INSTALL_PATH = "api/v1/analytics/install/add"

    fun reportInstall(
        params: Map<String, String>,
        clickId: String,
        onResult: (ApiResult) -> Unit
    ) {
        PopbrainApiClient.get(INSTALL_PATH, buildQuery(params, clickId), "Install report", onResult)
    }

    internal fun buildQuery(params: Map<String, String>, clickId: String): Map<String, String> {
        val query = linkedMapOf("clickId" to clickId)

        for ((key, value) in params) {
            when {
                PopbrainServerRules.isClickIdAliasKey(key) || value.trim() == clickId -> continue
                !PopbrainServerRules.isValidKey(key) ->
                    PopbrainLogger.e("Ignoring referrer param with invalid key '${key.take(20)}'")
                !PopbrainServerRules.isValidString(value) ->
                    PopbrainLogger.e("Ignoring referrer param '$key', value too long")
                query.size >= PopbrainServerRules.MAX_KEYS -> {
                    PopbrainLogger.e("Referrer has too many params, extra params not sent")
                    break
                }
                else -> query[key] = value
            }
        }
        return query
    }
}
