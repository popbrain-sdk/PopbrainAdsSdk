package ads.popbrain.sdk

import java.io.IOException

/**
 * Outcome of a Popbrain API call, split by whether retrying can ever help.
 *
 * This distinction is what keeps attribution correct: a [Retryable] failure must leave the
 * install unmarked so the next launch tries again, while a [Permanent] failure must stop the
 * retry loop instead of hitting the server on every app start forever.
 */
internal sealed class ApiResult {

    data class Success(val code: Int) : ApiResult()

    /** No connectivity, timeout, 5xx, 408 or 429 — the same request may succeed later. */
    data class Retryable(val reason: String, val cause: Throwable? = null) : ApiResult()

    /** 4xx (other than 408/429) or a malformed request — retrying sends the same bad call. */
    data class Permanent(val reason: String, val cause: Throwable? = null) : ApiResult()

    companion object {

        fun fromHttp(code: Int, errorBody: String?): ApiResult = when {
            code in 200..299 -> Success(code)
            code == 408 || code == 429 || code >= 500 ->
                Retryable("HTTP $code${errorBody.suffix()}")
            else -> Permanent("HTTP $code${errorBody.suffix()}")
        }

        fun fromThrowable(t: Throwable): ApiResult = when (t) {
            // Retrofit surfaces connectivity problems, timeouts and DNS failures as IOException.
            is IOException -> Retryable("network failure: ${t.javaClass.simpleName}", t)
            // Anything else is a bug in the request we built — retrying cannot fix it.
            else -> Permanent("unexpected failure: ${t.javaClass.simpleName}", t)
        }

        private fun String?.suffix(): String =
            if (isNullOrBlank()) "" else " - ${take(200)}"
    }
}
