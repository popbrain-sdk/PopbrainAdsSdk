package ads.popbrain.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventBodyTest {

    private val c1 = "65a1b2c3d4e5f60718293a4b"

    private fun body(params: Map<String, Any>?, advertiserId: String? = null) =
        JSONObject(PopbrainAnalyticsManager.buildBody("purchase", params, c1, advertiserId))

    @Test
    fun `required fields are always present`() {
        val json = body(null, "adv_99")
        assertEquals(c1, json.getString("clickId"))
        assertEquals("purchase", json.getString("eventName"))
        assertEquals("adv_99", json.getString("advertiserId"))
    }

    @Test
    fun `reserved keys cannot override the click`() {
        val json = body(mapOf("clickId" to "other", "eventName" to "x", "advertiserId" to "y"))
        assertEquals(c1, json.getString("clickId"))
        assertEquals("purchase", json.getString("eventName"))
        assertFalse(json.has("advertiserId"))
    }

    @Test
    fun `primitive values are sent and objects are dropped`() {
        val json = body(
            mapOf(
                "currency" to "INR",
                "qty" to 2,
                "first" to true,
                "nested" to mapOf("a" to 1),
                "list" to listOf(1, 2),
                "nan" to Double.NaN
            )
        )
        assertEquals("INR", json.getString("currency"))
        assertEquals(2, json.getInt("qty"))
        assertTrue(json.getBoolean("first"))
        assertFalse(json.has("nested"))
        assertFalse(json.has("list"))
        assertFalse(json.has("nan"))
    }

    @Test
    fun `value outside the server range is dropped but the event still goes`() {
        assertFalse(body(mapOf("value" to 0)).has("value"))
        assertFalse(body(mapOf("value" to 100_000_000)).has("value"))
        assertFalse(body(mapOf("value" to "abc")).has("value"))
        assertEquals(499, body(mapOf("value" to 499)).getInt("value"))
        assertEquals("499.50", body(mapOf("value" to "499.50")).getString("value"))
    }

    @Test
    fun `currency must be an upper case three letter code`() {
        assertEquals("INR", body(mapOf("currency" to "INR")).getString("currency"))
        assertFalse(body(mapOf("currency" to "inr")).has("currency"))
        assertFalse(body(mapOf("currency" to "Rupees")).has("currency"))
    }

    @Test
    fun `userId must match the server length rule`() {
        assertFalse(body(mapOf("userId" to "short")).has("userId"))
        assertTrue(body(mapOf("userId" to "user_123456789")).has("userId"))
    }

    @Test
    fun `body never exceeds the server key limit`() {
        val params = (1..40).associate { "p$it" to it }
        assertEquals(PopbrainServerRules.MAX_KEYS, body(params, "adv_99").length())
        assertEquals(PopbrainServerRules.MAX_KEYS, body(params).length())
    }

    @Test
    fun `too long string values and keys are dropped`() {
        val json = body(mapOf("note" to "x".repeat(401), "k".repeat(61) to "v", "ok" to "fine"))
        assertFalse(json.has("note"))
        assertEquals(2 + 1, json.length())
    }
}
