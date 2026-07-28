package ads.popbrain.sdk

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One shared OkHttp client for the whole SDK — a library should not ship two connection pools
 * and two dispatcher thread pools into the host app.
 */
internal object PopbrainHttp {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
