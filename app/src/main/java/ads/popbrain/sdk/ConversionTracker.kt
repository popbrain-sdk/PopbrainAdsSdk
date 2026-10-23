package ads.popbrain.sdk

internal object ConversionTracker {

    private const val MAX_INSTALL_ATTEMPTS = 10

    fun track(result: AttributionResult) {
        PopbrainStorage.referrer = result.referrer
        PopbrainStorage.referrerProcessed = true

        result.clickId?.let { PopbrainStorage.clickId = it }
        result.advertiserId?.let { PopbrainStorage.advertiserId = it }

        PopbrainAnalyticsManager.onAttributionResolved()

        if (result.isOrganic) {
            PopbrainLogger.d("Organic install, nothing to report")
            return
        }

        reportInstallIfNeeded()
    }

    fun reportInstallIfNeeded() {
        if (PopbrainStorage.installReported) {
            PopbrainLogger.d("Install already reported, skipping")
            return
        }

        val attempts = PopbrainStorage.installAttempts
        if (attempts >= MAX_INSTALL_ATTEMPTS) {
            PopbrainLogger.e("Install report abandoned after $attempts attempts")
            return
        }

        val stored = ReferrerParser.parse(PopbrainStorage.referrer)
        val clickId = stored.clickId ?: return

        PopbrainStorage.installAttempts = attempts + 1
        PopbrainLogger.d("Reporting install (attempt ${attempts + 1}) for clickId=$clickId")

        ConversionApi.reportInstall(stored.params, clickId) { result ->
            when (result) {
                is ApiResult.Success -> {
                    PopbrainStorage.installReported = true
                }
                is ApiResult.Permanent -> {
                    PopbrainStorage.installAttempts = MAX_INSTALL_ATTEMPTS
                    PopbrainLogger.e("Install rejected permanently: ${result.reason}")
                }
                is ApiResult.Retryable -> {
                    PopbrainLogger.d("Install will retry next launch: ${result.reason}")
                }
            }
        }
    }
}
