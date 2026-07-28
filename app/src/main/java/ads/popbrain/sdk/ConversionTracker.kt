package ads.popbrain.sdk

internal object ConversionTracker {

    /**
     * Give up after this many failed launches. Without a bound, an install the server keeps
     * rejecting would hit the API on every app start for the lifetime of the app.
     */
    private const val MAX_INSTALL_ATTEMPTS = 10

    /**
     * Persists the attribution, wires the clickId into analytics, and reports the install
     * exactly once.
     */
    fun track(result: AttributionResult) {
        PopbrainStorage.referrer = result.referrer
        PopbrainStorage.referrerProcessed = true

        result.clickId?.let { PopbrainStorage.clickId = it }
        result.advertiserId?.let { PopbrainStorage.advertiserId = it }

        // This is the link that was missing: the clickId parsed from the referrer now becomes
        // the identity every in-app event is attributed to.
        PopbrainAnalyticsManager.onAttributionResolved()

        if (result.isOrganic) {
            PopbrainLogger.d("Organic install, nothing to report")
            return
        }

        reportInstallIfNeeded()
    }

    /**
     * Retries a previously parsed-but-unsent install using the persisted referrer. Called on
     * launch when [PopbrainStorage.referrerProcessed] is set but the send never succeeded.
     */
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
        if (stored.isOrganic) return

        val clickId = (stored.clickId ?: PopbrainStorage.clickId)?.takeIf(String::isNotEmpty)

        PopbrainStorage.installAttempts = attempts + 1
        PopbrainLogger.d("Reporting install (attempt ${attempts + 1}) for clickId=$clickId")

        ConversionApi.reportInstall(stored.params, clickId) { result ->
            when (result) {
                is ApiResult.Success -> {
                    PopbrainStorage.installReported = true
                }
                is ApiResult.Permanent -> {
                    // Retrying sends the identical bad request, so stop rather than burn a
                    // network call on every launch. Logged loudly — this needs a fix.
                    PopbrainStorage.installAttempts = MAX_INSTALL_ATTEMPTS
                    PopbrainLogger.e("Install rejected permanently: ${result.reason}")
                }
                is ApiResult.Retryable -> {
                    // Attempt counter already incremented; next launch tries again.
                    PopbrainLogger.d("Install will retry next launch: ${result.reason}")
                }
            }
        }
    }
}
