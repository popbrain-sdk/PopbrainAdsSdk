package ads.popbrain.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * The retry decision drives whether an install is re-sent on the next launch, so the
 * classification is worth pinning down.
 */
class ApiResultTest {

    @Test
    fun `2xx is success`() {
        assertTrue(ApiResult.fromHttp(200, null) is ApiResult.Success)
        assertTrue(ApiResult.fromHttp(201, null) is ApiResult.Success)
        assertTrue(ApiResult.fromHttp(299, null) is ApiResult.Success)
    }

    @Test
    fun `success carries the status code`() {
        assertEquals(204, (ApiResult.fromHttp(204, null) as ApiResult.Success).code)
    }

    @Test
    fun `server errors are retryable`() {
        assertTrue(ApiResult.fromHttp(500, null) is ApiResult.Retryable)
        assertTrue(ApiResult.fromHttp(502, null) is ApiResult.Retryable)
        assertTrue(ApiResult.fromHttp(503, null) is ApiResult.Retryable)
    }

    @Test
    fun `timeout and rate limit are retryable`() {
        assertTrue(ApiResult.fromHttp(408, null) is ApiResult.Retryable)
        assertTrue(ApiResult.fromHttp(429, null) is ApiResult.Retryable)
    }

    @Test
    fun `client errors are permanent`() {
        assertTrue(ApiResult.fromHttp(400, null) is ApiResult.Permanent)
        assertTrue(ApiResult.fromHttp(401, null) is ApiResult.Permanent)
        assertTrue(ApiResult.fromHttp(404, null) is ApiResult.Permanent)
        assertTrue(ApiResult.fromHttp(422, null) is ApiResult.Permanent)
    }

    @Test
    fun `io failures are retryable`() {
        assertTrue(ApiResult.fromThrowable(IOException("offline")) is ApiResult.Retryable)
        assertTrue(ApiResult.fromThrowable(UnknownHostException("dns")) is ApiResult.Retryable)
        assertTrue(ApiResult.fromThrowable(SocketTimeoutException("slow")) is ApiResult.Retryable)
    }

    @Test
    fun `non-io failures are permanent`() {
        assertTrue(ApiResult.fromThrowable(IllegalArgumentException("bad url")) is ApiResult.Permanent)
        assertTrue(ApiResult.fromThrowable(IllegalStateException("boom")) is ApiResult.Permanent)
    }

    @Test
    fun `error body is included in the reason`() {
        val result = ApiResult.fromHttp(400, "clickId missing") as ApiResult.Permanent
        assertTrue(result.reason.contains("HTTP 400"))
        assertTrue(result.reason.contains("clickId missing"))
    }

    @Test
    fun `blank error body adds no suffix`() {
        assertEquals("HTTP 400", (ApiResult.fromHttp(400, "  ") as ApiResult.Permanent).reason)
        assertEquals("HTTP 500", (ApiResult.fromHttp(500, null) as ApiResult.Retryable).reason)
    }

    @Test
    fun `long error body is truncated`() {
        val result = ApiResult.fromHttp(400, "x".repeat(500)) as ApiResult.Permanent
        assertTrue("reason was ${result.reason.length} chars", result.reason.length < 250)
    }
}
