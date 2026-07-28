package ads.popbrain.sdk

import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal object ConversionApi {

    private const val INSTALL_URL = "https://server.popbrain.ai/api/v1/analytics/install/add"

    /**
     * @param onSuccess invoked only on a 2xx. The caller uses this to mark the install as
     *        reported, so a failed send is retried on the next launch rather than lost.
     */
    fun reportInstall(params: Map<String, String>, clickId: String?, onSuccess: () -> Unit) {
        val base = INSTALL_URL.toHttpUrlOrNull()
        if (base == null) {
            PopbrainLogger.e("Install endpoint is not a valid URL")
            return
        }

        // Built through HttpUrl instead of string-concatenating the raw referrer, so referrer
        // values containing '#', spaces or reserved characters cannot corrupt the request.
        val urlBuilder = base.newBuilder()
        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        if (clickId != null && !params.containsKey("clickId")) {
            urlBuilder.addQueryParameter("clickId", clickId)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        PopbrainHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                PopbrainLogger.e("Install report failed, will retry on next launch", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        PopbrainLogger.d("Install reported. Code: ${it.code}")
                        onSuccess()
                    } else {
                        PopbrainLogger.e("Install report rejected with ${it.code}")
                    }
                }
            }
        })
    }
}
