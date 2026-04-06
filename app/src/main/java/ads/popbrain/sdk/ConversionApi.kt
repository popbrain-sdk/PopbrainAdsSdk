package ads.popbrain.sdk

import android.util.Log
import okhttp3.*
import java.io.IOException

object ConversionApi {
    private val client = OkHttpClient()
    fun reportInstall(referrer: String) {
        val url = "https://server.popbrain.ai/api/v1/analytics/install/add?$referrer"
//        Log.d("PopbrainSDK", "Final Url: $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("PopbrainSDK Error: API Hit Failed - ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("PopbrainSDK Success: Data saved in DB. Code: ${response.code}")
                } else {
                    println("PopbrainSDK Error: Server responded with ${response.code}")
                }
                response.close()
            }
        })
    }
}