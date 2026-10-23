package ads.popbrain.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopbrainServerRulesTest {

    @Test
    fun `click id must be a 24 character hex object id`() {
        assertTrue(PopbrainServerRules.isValidClickId("65a1b2c3d4e5f60718293a4b"))
        assertFalse(PopbrainServerRules.isValidClickId(null))
        assertFalse(PopbrainServerRules.isValidClickId(""))
        assertFalse(PopbrainServerRules.isValidClickId("abc123"))
        assertFalse(PopbrainServerRules.isValidClickId("65a1b2c3d4e5f60718293a4z"))
        assertFalse(PopbrainServerRules.isValidClickId("65a1b2c3d4e5f60718293a4b0"))
    }

    @Test
    fun `event name follows the server pattern`() {
        assertTrue(PopbrainServerRules.isValidEventName("purchase"))
        assertTrue(PopbrainServerRules.isValidEventName("add_to_cart_2"))
        assertTrue(PopbrainServerRules.isValidEventName("a".repeat(40)))
        assertFalse(PopbrainServerRules.isValidEventName("a"))
        assertFalse(PopbrainServerRules.isValidEventName("a".repeat(41)))
        assertFalse(PopbrainServerRules.isValidEventName("add to cart"))
        assertFalse(PopbrainServerRules.isValidEventName("add-to-cart"))
    }

    @Test
    fun `click id alias keys match the server rename`() {
        listOf("clickId", "click_id", "click-id", "click id", "CLICKID").forEach {
            assertTrue(it, PopbrainServerRules.isClickIdAliasKey(it))
        }
        assertFalse(PopbrainServerRules.isClickIdAliasKey("utm_clickId"))
        assertFalse(PopbrainServerRules.isClickIdAliasKey("cid"))
    }
}
