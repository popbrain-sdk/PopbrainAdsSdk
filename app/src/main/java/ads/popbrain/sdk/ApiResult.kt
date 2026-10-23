package ads.popbrain.sdk

import java.io.IOException

internal sealed class ApiResult {

    data class Success(val code: Int) : ApiResult()

    data class Retryable(val reason: String, val cause: Throwable? = null) : ApiResult()

    data class Permanent(val reason: String, val cause: Throwable? = null) : ApiResult()

    companion object {

        fun fromHttp(code: Int, errorBody: String?): ApiResult = when {
            code in 200..299 -> Success(code)
            code == 408 || code == 429 || code >= 500 ->
                Retryable("HTTP $code${errorBody.suffix()}")
            else -> Permanent("HTTP $code${errorBody.suffix()}")
        }

        fun fromThrowable(t: Throwable): ApiResult = when (t) {
            is IOException -> Retryable("network failure: ${t.javaClass.simpleName}", t)
            else -> Permanent("unexpected failure: ${t.javaClass.simpleName}", t)
        }

        private fun String?.suffix(): String =
            if (isNullOrBlank()) "" else " - ${take(200)}"
    }
}
