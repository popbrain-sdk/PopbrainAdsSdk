package ads.popbrain.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionApiTest {

    private val c1 = "65a1b2c3d4e5f60718293a4b"

    @Test
    fun `clickId is always sent first`() {
        val query = ConversionApi.buildQuery(mapOf("utm_source" to "popbrain"), c1)
        assertEquals(listOf("clickId", "utm_source"), query.keys.toList())
        assertEquals(c1, query["clickId"])
    }

    @Test
    fun `clickId aliases are not sent so the server rename cannot override clickId`() {
        val params = linkedMapOf(
            "click_id" to "65a1b2c3d4e5f60718293a4c",
            "Click-Id" to "x",
            "clickid" to "y",
            "utm_clickId" to c1,
            "utm_source" to "popbrain"
        )
        val query = ConversionApi.buildQuery(params, c1)
        assertEquals(mapOf("clickId" to c1, "utm_source" to "popbrain"), query)
    }

    @Test
    fun `query never exceeds the server key limit`() {
        val params = (1..40).associate { "p$it" to "v$it" }
        val query = ConversionApi.buildQuery(params, c1)
        assertEquals(PopbrainServerRules.MAX_KEYS, query.size)
        assertEquals(c1, query["clickId"])
    }

    @Test
    fun `too long keys and values are dropped instead of failing the install`() {
        val params = linkedMapOf(
            "k".repeat(61) to "v",
            "long" to "v".repeat(401),
            "ok" to "v".repeat(400)
        )
        val query = ConversionApi.buildQuery(params, c1)
        assertFalse(query.containsKey("long"))
        assertFalse(query.containsKey("k".repeat(61)))
        assertTrue(query.containsKey("ok"))
    }
}
