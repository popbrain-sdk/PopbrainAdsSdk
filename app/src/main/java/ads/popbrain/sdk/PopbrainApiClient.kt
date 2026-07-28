package ads.popbrain.sdk

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Minimal HTTP layer built on the platform's own [HttpURLConnection].
 *
 * The SDK makes exactly two kinds of call, so a full HTTP client would have added ~1.1 MB to
 * every integrator's app for no functional gain. On Android, HttpURLConnection is itself
 * backed by OkHttp inside the platform, so this loses neither connection pooling nor TLS
 * handling — it just stops shipping a second copy.
 */
internal object PopbrainApiClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val MAX_ERROR_BODY_BYTES = 512

    /**
     * Every request and callback runs here, so nothing touches the host app's main thread.
     * Single-threaded on purpose: call volume is tiny, and serialising gives ordered writes
     * to [PopbrainStorage]. Daemon so it never holds the host process open.
     */
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "popbrain-sdk").apply { isDaemon = true }
    }

    fun schedule(delay: Long, unit: TimeUnit, action: () -> Unit) {
        try {
            executor.schedule({
                try {
                    action()
                } catch (e: Exception) {
                    PopbrainLogger.e("Scheduled task failed", e)
                }
            }, delay, unit)
        } catch (e: Exception) {
            PopbrainLogger.e("Could not schedule retry", e)
        }
    }

    fun get(path: String, query: Map<String, String>, label: String, onResult: (ApiResult) -> Unit) {
        submit(label, onResult) { execute(buildUrl(path, query), "GET", null) }
    }

    fun postJson(path: String, json: String, label: String, onResult: (ApiResult) -> Unit) {
        submit(label, onResult) { execute(buildUrl(path, emptyMap()), "POST", json) }
    }

    /** Single funnel, so no call site can forget to classify a failure or log the outcome. */
    private fun submit(label: String, onResult: (ApiResult) -> Unit, work: () -> ApiResult) {
        val task = {
            val result = try {
                work()
            } catch (t: Throwable) {
                ApiResult.fromThrowable(t)
            }
            deliver(label, result, onResult)
        }
        try {
            executor.execute(task)
        } catch (e: Exception) {
            // Executor rejected the task (shutdown). Report rather than swallow.
            deliver(label, ApiResult.fromThrowable(e), onResult)
        }
    }

    private fun deliver(label: String, result: ApiResult, onResult: (ApiResult) -> Unit) {
        when (result) {
            is ApiResult.Success -> PopbrainLogger.d("$label ok (${result.code})")
            is ApiResult.Retryable -> PopbrainLogger.e("$label failed, retryable: ${result.reason}")
            is ApiResult.Permanent -> PopbrainLogger.e("$label failed, permanent: ${result.reason}")
        }
        // A throwing consumer must never kill the SDK's worker thread.
        try {
            onResult(result)
        } catch (e: Exception) {
            PopbrainLogger.e("Error handling $label result", e)
        }
    }

    private fun buildUrl(path: String, query: Map<String, String>): URL {
        val url = StringBuilder(PopbrainEnv.BASE_URL.trimEnd('/'))
            .append('/')
            .append(path.trimStart('/'))

        if (query.isNotEmpty()) {
            url.append('?')
            var first = true
            for ((key, value) in query) {
                if (!first) url.append('&')
                first = false
                // Encoded per key/value, so a referrer value containing '#', '&' or a space
                // cannot corrupt the request.
                url.append(encode(key)).append('=').append(encode(value))
            }
        }
        return URL(url.toString())
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun execute(url: URL, method: String, body: String?): ApiResult {
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }

            if (body != null) {
                val bytes = body.toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }

            // Throws IOException on connectivity/timeout failure, which submit() classifies
            // as retryable.
            val code = connection.responseCode

            return if (code in 200..299) {
                connection.inputStream.drainQuietly()
                ApiResult.Success(code)
            } else {
                ApiResult.fromHttp(code, connection.errorStream?.readBoundedQuietly())
            }
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Draining the body lets the platform release/reuse the socket cleanly. The content is
     * irrelevant on success, so a read failure here changes nothing.
     */
    private fun InputStream.drainQuietly() {
        runCatching {
            use { input ->
                val buffer = ByteArray(4096)
                while (input.read(buffer) != -1) { /* discard */ }
            }
        }
    }

    /** Bounded so a huge error page cannot be pulled into memory or into a log line. */
    private fun InputStream.readBoundedQuietly(): String? = runCatching {
        use { input ->
            val buffer = ByteArray(MAX_ERROR_BODY_BYTES)
            var read = 0
            while (read < MAX_ERROR_BODY_BYTES) {
                val n = input.read(buffer, read, MAX_ERROR_BODY_BYTES - read)
                if (n == -1) break
                read += n
            }
            if (read == 0) null else String(buffer, 0, read, Charsets.UTF_8)
        }
    }.getOrNull()
}
