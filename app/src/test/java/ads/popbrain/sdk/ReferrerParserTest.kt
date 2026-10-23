package ads.popbrain.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferrerParserTest {

    private val c1 = "65a1b2c3d4e5f60718293a4b"
    private val c2 = "65A1B2C3D4E5F60718293A4C"

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
        val result = ReferrerParser.parse("utm_source=popbrain&clickId=$c1&utm_campaign=summer")
        assertFalse(result.isOrganic)
        assertEquals(c1, result.clickId)
        assertEquals("summer", result.campaign)
    }

    @Test
    fun `the word organic elsewhere does not hide a popbrain click`() {
        val result = ReferrerParser.parse("utm_campaign=organic_boost&utm_medium=organic&clickId=$c1")
        assertFalse(result.isOrganic)
        assertEquals(c1, result.clickId)
    }

    @Test
    fun `popbrain source without a clickId is organic`() {
        val result = ReferrerParser.parse("utm_source=Popbrain_Network&utm_campaign=diwali")
        assertTrue(result.isOrganic)
        assertEquals("diwali", result.campaign)
    }

    @Test
    fun `gclid without a popbrain clickId is organic`() {
        assertTrue(ReferrerParser.parse("gclid=xyz").isOrganic)
    }

    @Test
    fun `clickId that is not a popbrain click id is ignored`() {
        val result = ReferrerParser.parse("clickId=abc123&cid=12345")
        assertTrue(result.isOrganic)
        assertNull(result.clickId)
    }

    @Test
    fun `empty clickId is not treated as attribution`() {
        val result = ReferrerParser.parse("utm_source=news&clickId=")
        assertTrue(result.isOrganic)
        assertNull(result.clickId)
    }

    @Test
    fun `alternative clickId keys are recognised`() {
        assertEquals(c1, ReferrerParser.parse("utm_clickId=$c1").clickId)
        assertEquals(c2, ReferrerParser.parse("click_id=$c2").clickId)
    }

    @Test
    fun `first valid clickId key wins over an invalid one`() {
        assertEquals(c1, ReferrerParser.parse("clickId=bad&utm_clickId=$c1").clickId)
    }

    @Test
    fun `advertiserId is extracted from the referrer`() {
        val result = ReferrerParser.parse("clickId=$c1&advertiserId=adv_99")
        assertEquals("adv_99", result.advertiserId)
    }

    @Test
    fun `encoded ampersand inside a value does not create a bogus parameter`() {
        val result = ReferrerParser.parse("clickId=$c1&utm_campaign=spring%26summer")
        assertEquals("spring&summer", result.campaign)
        assertEquals(c1, result.clickId)
        assertEquals(2, result.params.size)
    }

    @Test
    fun `encoded equals inside a value is preserved`() {
        val result = ReferrerParser.parse("clickId=$c1&utm_content=a%3Db%3Dc")
        assertEquals("a=b=c", result.params["utm_content"])
    }

    @Test
    fun `unencoded equals inside a value is preserved`() {
        val result = ReferrerParser.parse("clickId=$c1&utm_content=a=b=c")
        assertEquals("a=b=c", result.params["utm_content"])
    }

    @Test
    fun `malformed segments are skipped`() {
        val result = ReferrerParser.parse("clickId=$c1&&novalue&=orphan&utm_campaign=x")
        assertEquals(c1, result.clickId)
        assertEquals("x", result.campaign)
        assertEquals(2, result.params.size)
    }

    @Test
    fun `raw referrer is preserved on the result`() {
        val raw = "utm_source=popbrain&clickId=$c1"
        assertEquals(raw, ReferrerParser.parse(raw).referrer)
    }

    @Test
    fun `referrer built by the popbrain server is attributed`() {
        val result = ReferrerParser.parse("utm_source=popbrain&utm_clickId=$c1&clickId=$c1")
        assertFalse(result.isOrganic)
        assertEquals(c1, result.clickId)
    }
}
