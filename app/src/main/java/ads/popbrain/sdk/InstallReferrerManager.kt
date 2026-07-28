package ads.popbrain.sdk

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.util.concurrent.atomic.AtomicBoolean

internal object InstallReferrerManager {

    /**
     * @param onResult parsed referrer. Fires at most once.
     * @param onUnavailable the referrer could not be read *this time* (service unavailable,
     *        Play Store missing, connection dropped). Distinct from an organic install — the
     *        caller must retry later instead of permanently classifying the user as organic.
     */
    fun fetchReferrer(
        context: Context,
        onResult: (AttributionResult) -> Unit,
        onUnavailable: (reason: String) -> Unit
    ) {
        val client = try {
            InstallReferrerClient.newBuilder(context.applicationContext).build()
        } catch (e: Exception) {
            onUnavailable("client build failed: ${e.message}")
            return
        }

        // The Play service can invoke the listener more than once (setup finished, then a
        // disconnect). Everything downstream must run exactly once.
        val delivered = AtomicBoolean(false)

        try {
            client.startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (!delivered.compareAndSet(false, true)) return

                    // Resolve first, deliver afterwards. Invoking the callback inside the try
                    // meant any downstream failure fell into catch and delivered a *second*
                    // callback claiming the install was organic.
                    var result: AttributionResult? = null
                    var failure: String? = null

                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            result = ReferrerParser.parse(client.installReferrer.installReferrer)
                        } else {
                            failure = "responseCode=$responseCode"
                        }
                    } catch (e: Exception) {
                        failure = "referrer read failed: ${e.message}"
                    } finally {
                        try {
                            client.endConnection()
                        } catch (e: Exception) {
                            PopbrainLogger.e("Failed to close referrer client", e)
                        }
                    }

                    if (result != null) onResult(result) else onUnavailable(failure ?: "unknown")
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Without this the callback would never fire when the service drops before
                    // setup completes, and the install would be silently lost.
                    if (delivered.compareAndSet(false, true)) {
                        onUnavailable("service disconnected before setup finished")
                    }
                }
            })
        } catch (e: Exception) {
            if (delivered.compareAndSet(false, true)) {
                onUnavailable("startConnection failed: ${e.message}")
            }
        }
    }
}
