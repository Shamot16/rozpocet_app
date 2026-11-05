package com.example

import com.example.data.remote.EkasaReceiptService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EkasaReceiptServiceTest {

    @Test
    fun testExtractReceiptIdStartingWithO() {
        val raw = "O-8E7DB82F01B347589571D37E84B12C94"
        val extracted = EkasaReceiptService.extractReceiptId(raw)
        assertEquals("O-8E7DB82F01B347589571D37E84B12C94", extracted)
    }

    @Test
    fun testExtractReceiptIdStartingWithP() {
        val raw = "P-1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D"
        val extracted = EkasaReceiptService.extractReceiptId(raw)
        assertEquals("P-1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D", extracted)
    }

    @Test
    fun testExtractReceiptIdFromUrlOrParam() {
        val url = "https://ekasa.financnasprava.sk/mdu/api/v1/opd/receipt/find?receiptId=O-8E7DB82F01B347589571D37E84B12C94&other=123"
        val extracted = EkasaReceiptService.extractReceiptId(url)
        assertEquals("O-8E7DB82F01B347589571D37E84B12C94", extracted)
    }

    @Test
    fun testInvalidQrContentReturnsNull() {
        val invalid = "https://example.com/some/random/qr/code"
        val extracted = EkasaReceiptService.extractReceiptId(invalid)
        assertNull(extracted)
    }

    @Test
    fun testNormalizeDateFormats() {
        assertEquals("2026-11-05", EkasaReceiptService.normalizeDate("05.11.2026 14:30:00"))
        assertEquals("2026-11-05", EkasaReceiptService.normalizeDate("5.11.2026"))
        assertEquals("2026-11-05", EkasaReceiptService.normalizeDate("2026-11-05T14:30:00"))
        assertEquals("2026-11-05", EkasaReceiptService.normalizeDate("2026-11-05"))
    }
}
