package ads.popbrain.sdk

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.util.concurrent.atomic.AtomicBoolean

internal object InstallReferrerManager {

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

        val delivered = AtomicBoolean(false)

        try {
            client.startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (!delivered.compareAndSet(false, true)) return

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
