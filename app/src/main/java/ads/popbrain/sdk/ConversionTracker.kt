package ads.popbrain.sdk

internal object ConversionTracker {

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

        val stored = ReferrerParser.parse(PopbrainStorage.referrer)
        if (stored.isOrganic) return

        PopbrainLogger.d("Reporting install for clickId=${stored.clickId}")
        ConversionApi.reportInstall(stored.params, stored.clickId) {
            PopbrainStorage.installReported = true
        }
    }
}
