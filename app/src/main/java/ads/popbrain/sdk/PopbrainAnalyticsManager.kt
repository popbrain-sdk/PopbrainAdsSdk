package ads.popbrain.sdk

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

object PopbrainAnalyticsManager {
    private var clickId: String? = null
    private var advertiserId: String? = null
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun initAnalytics(clickId: String, advertiserId: String) {
        this.clickId = clickId
        this.advertiserId = advertiserId
    }

    fun sendEvent(eventName: String, extraParams: HashMap<String, Any>? = null) {
        if (clickId == null || advertiserId == null) {
            println("PopbrainSDK Error: Analytics not initialized with clickId/advertiserId")
            return
        }
        val json = JSONObject().apply {
            put("clickId", clickId)
            put("advertiserId", advertiserId)
            put("eventName", eventName)

            extraParams?.forEach { (key, value) ->
                put(key, value)
            }
        }

        val request = Request.Builder()
            .url("https://devserver.popbrain.ai/api/v1/pixel/s2s")
            .post(json.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                println("PopbrainSDK Error: S2S event failed - ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                println("PopbrainSDK: Event $eventName sent. Code: ${response.code}")
                response.close()
            }
        })
    }
}