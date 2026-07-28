package ads.popbrain.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferrerParserTest {

    @Test
    fun `null referrer is organic`() {
        assertTrue(ReferrerParser.parse(null).isOrganic)
    }

    @Test
    fun `empty referrer is organic`() {
        assertTrue(ReferrerParser.parse("").isOrganic)
    }

    @Test
    fun `play organic placeholder is organic`() {
        assertTrue(ReferrerParser.parse("utm_source=google-play&utm_medium=organic").isOrganic)
    }

    @Test
    fun `referrer without any attribution signal is organic`() {
        val result = ReferrerParser.parse("utm_source=newsletter&utm_medium=email")
        assertTrue(result.isOrganic)
        assertNull(result.clickId)
    }

    @Test
    fun `clickId marks the install as paid`() {
        val result = ReferrerParser.parse("utm_source=popbrain&clickId=abc123&utm_campaign=summer")
        assertFalse(result.isOrganic)
        assertEquals("abc123", result.clickId)
        assertEquals("summer", result.campaign)
    }

    @Test
    fun `popbrain source alone marks the install as paid`() {
        val result = ReferrerParser.parse("utm_source=Popbrain_Network&utm_campaign=diwali")
        assertFalse(result.isOrganic)
        assertEquals("diwali", result.campaign)
    }

    @Test
    fun `gclid marks the install as paid`() {
        assertFalse(ReferrerParser.parse("gclid=xyz").isOrganic)
    }

    @Test
    fun `empty clickId is not treated as attribution`() {
        val result = ReferrerParser.parse("utm_source=news&clickId=")
        assertTrue(result.isOrganic)
        assertNull(result.clickId)
    }

    @Test
    fun `alternative clickId keys are recognised`() {
        assertEquals("k1", ReferrerParser.parse("utm_clickId=k1").clickId)
        assertEquals("k2", ReferrerParser.parse("click_id=k2").clickId)
    }

    @Test
    fun `advertiserId is extracted from the referrer`() {
        val result = ReferrerParser.parse("clickId=c1&advertiserId=adv_99")
        assertEquals("adv_99", result.advertiserId)
    }

    @Test
    fun `encoded ampersand inside a value does not create a bogus parameter`() {
        // Decoding the whole string before splitting used to turn this into two parameters.
        val result = ReferrerParser.parse("clickId=c1&utm_campaign=spring%26summer")
        assertEquals("spring&summer", result.campaign)
        assertEquals("c1", result.clickId)
        assertEquals(2, result.params.size)
    }

    @Test
    fun `encoded equals inside a value is preserved`() {
        val result = ReferrerParser.parse("clickId=a%3Db%3Dc")
        assertEquals("a=b=c", result.clickId)
    }

    @Test
    fun `unencoded equals inside a value is preserved`() {
        val result = ReferrerParser.parse("clickId=a=b=c")
        assertEquals("a=b=c", result.clickId)
    }

    @Test
    fun `malformed segments are skipped`() {
        val result = ReferrerParser.parse("clickId=c1&&novalue&=orphan&utm_campaign=x")
        assertEquals("c1", result.clickId)
        assertEquals("x", result.campaign)
        assertEquals(2, result.params.size)
    }

    @Test
    fun `raw referrer is preserved on the result`() {
        val raw = "utm_source=popbrain&clickId=abc123"
        assertEquals(raw, ReferrerParser.parse(raw).referrer)
    }
}
