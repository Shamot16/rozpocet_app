package com.example

import com.example.data.model.EkasaAllocationStatus
import com.example.data.model.EkasaItemCodec
import com.example.data.model.EkasaLineItem
import com.example.data.model.SavedEkasaReceipt
import com.example.data.remote.EkasaReceiptService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun testMergeFetchedItemsPreservesAssignedCategories() {
        val existing = listOf(
            EkasaLineItem(0, "Mlieko 1l", 2.0, 2.38, "K", assignedCategory = "Potraviny"),
            EkasaLineItem(1, "Šampón", 1.0, 4.50, "K", assignedCategory = ""),
            EkasaLineItem(2, "Tričko", 1.0, 9.99, "K", assignedCategory = "Oblečenie")
        )
        val fetched = listOf(
            EkasaLineItem(0, "Mlieko 1l", 2.0, 2.38, "K"),
            EkasaLineItem(1, "Šampón", 1.0, 4.50, "K"),
            EkasaLineItem(2, "Tričko", 1.0, 9.99, "K")
        )

        val merged = EkasaReceiptService.mergeFetchedItemsWithExisting(fetched, existing)
        assertEquals(3, merged.size)
        assertEquals("Potraviny", merged[0].assignedCategory)
        assertEquals("", merged[1].assignedCategory)
        assertEquals("Oblečenie", merged[2].assignedCategory)
    }

    @Test
    fun testSavedEkasaReceiptHelpers() {
        val receipt = SavedEkasaReceipt(
            receiptId = "O-8E7DB82F01B347589571D37E84B12C94",
            organizationName = "Lidl Slovenská republika",
            issueDate = "2026-11-20",
            totalPrice = 20.00,
            items = listOf(
                EkasaLineItem(0, "Chlieb", 1.0, 2.50, assignedCategory = "Potraviny"),
                EkasaLineItem(1, "Maslo", 1.0, 3.50, assignedCategory = "Potraviny"),
                EkasaLineItem(2, "Tenisky", 1.0, 14.00, assignedCategory = "")
            ),
            allocationStatus = EkasaAllocationStatus.PARTIAL,
            lastFetchedAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals(6.00, receipt.assignedAmount, 0.001)
        assertEquals(14.00, receipt.remainingAmount, 0.001)
        assertEquals(1, receipt.unassignedCount)
        assertEquals("O-8E7D…2C94", receipt.shortReceiptId)
    }

    @Test
    fun testParseReceiptJsonAndItemCodec() {
        val sampleJson = """
            {
              "receipt": {
                "receiptId": "O-1234567890ABCDEF1234567890ABCDEF",
                "issueDate": "20.11.2026 15:42:10",
                "totalPrice": 16.48,
                "organization": { "name": "Tesco Stores SR" },
                "items": [
                  { "name": "Rožok", "quantity": 5.0, "price": 0.65, "itemType": "K" },
                  { "name": "Ponožky", "quantity": 1.0, "price": 5.99, "itemType": "K" },
                  { "name": "Prací gél", "quantity": 1.0, "price": 9.84, "itemType": "K" }
                ]
              }
            }
        """.trimIndent()

        val result = EkasaReceiptService.parseReceiptJson(sampleJson, "fallback")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Tesco Stores SR", parsed.organizationName)
        assertEquals("2026-11-20", parsed.formattedDate)
        assertEquals(16.48, parsed.totalPrice, 0.001)
        assertEquals(3, parsed.items.size)
        assertEquals("Rožok", parsed.items[0].name)
        assertEquals(5.0, parsed.items[0].quantity, 0.001)

        val updated = parsed.items.mapIndexed { i, item ->
            if (i == 1) item.copy(assignedCategory = "Oblečenie") else item
        }
        val encoded = EkasaItemCodec.encodeItems(updated)
        val decoded = EkasaItemCodec.decodeItems(encoded)
        assertEquals(3, decoded.size)
        assertEquals("Oblečenie", decoded[1].assignedCategory)
        assertEquals("", decoded[0].assignedCategory)
    }
}
