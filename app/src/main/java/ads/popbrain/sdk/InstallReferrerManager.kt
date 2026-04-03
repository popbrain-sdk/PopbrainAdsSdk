package ads.popbrain.sdk

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import ads.popbrain.sdk.model.AttributionResult
import android.util.Log

object InstallReferrerManager {

    fun fetchReferrer(
        context: Context,
        callback: (AttributionResult) -> Unit
    ) {

        val client = InstallReferrerClient.newBuilder(context).build()

        client.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        val response = client.installReferrer
                        callback(ReferrerParser.parse(response.installReferrer))
                    } else {
                        callback(AttributionResult(isOrganic = true))
                    }
                } catch (e: Exception) {
                    callback(AttributionResult(isOrganic = true))
                } finally {
                    client.endConnection()
                }
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }
}