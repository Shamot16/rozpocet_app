package com.example

import com.example.data.export.ExcelExporter
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CreditCard
import com.example.data.model.Transaction
import com.example.domain.BudgetEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ExcelExporterTest {

    @Test
    fun `buildWorkbookBytes produces valid xlsx zip with well-formed XML sheets`() {
        val items = listOf(
            BudgetItem(
                id = "1",
                name = "Výplata & Odmeny",
                type = "in",
                category = "Mzda",
                amount = 1850.50,
                frequency = "monthly",
                startDate = "2026-11-10"
            ),
            BudgetItem(
                id = "2",
                name = "Nájom <Bývanie>",
                type = "out",
                category = "Bývanie",
                amount = 650.00,
                frequency = "monthly",
                startDate = "2026-11-01"
            )
        )
        val cards = listOf(
            CreditCard(
                id = "card1",
                name = "Tatra Visa",
                limit = 2000.0,
                statementDay = 15,
                dueDay = 5,
                initialDebt = 150.0
            )
        )
        val transactions = listOf(
            Transaction(
                id = "tx1",
                name = "Lidl potraviny",
                type = "out",
                category = "Potraviny",
                amount = 42.35,
                date = "2026-11-05",
                cardId = "card1"
            )
        )
        val payments = listOf(
            CardPayment(
                id = "pay1",
                cardId = "card1",
                date = "2026-11-06",
                amount = 100.0
            )
        )

        val forecast = BudgetEngine.compute12MonthForecast(1200.0, items, 2026, 10)
        val cardsSummary = BudgetEngine.computeCardsExecutiveSummary(1200.0, cards, transactions, payments)

        val bytes = ExcelExporter.buildWorkbookBytes(
            initialBalance = 1200.0,
            initialBalanceDate = "2026-11-01",
            forecast = forecast,
            cardsSummary = cardsSummary,
            items = items,
            transactions = transactions,
            cards = cards,
            cardPayments = payments
        )

        assertTrue(bytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val content = zis.readBytes().toString(StandardCharsets.UTF_8)
                entries[entry.name] = content
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val expectedFiles = listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/_rels/workbook.xml.rels",
            "xl/workbook.xml",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml",
            "xl/worksheets/sheet3.xml",
            "xl/worksheets/sheet4.xml",
            "xl/worksheets/sheet5.xml"
        )
        expectedFiles.forEach { fileName ->
            assertTrue("Missing $fileName in xlsx", entries.containsKey(fileName))
        }

        // Verify every XML file parses cleanly
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val builder = factory.newDocumentBuilder()
        entries.forEach { (name, xml) ->
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
            assertTrue("Empty DOM for $name", doc.documentElement != null)
        }

        val workbookXml = entries.getValue("xl/workbook.xml")
        assertTrue(workbookXml.contains("Prehľad a prognóza"))
        assertTrue(workbookXml.contains("Skutočné platby"))
        assertTrue(workbookXml.contains("Pravidelné položky"))
        assertTrue(workbookXml.contains("Kreditné karty"))
        assertTrue(workbookXml.contains("Splátky kariet"))

        val transactionsXml = entries.getValue("xl/worksheets/sheet2.xml")
        assertTrue(transactionsXml.contains("Lidl potraviny"))
        assertTrue(transactionsXml.contains("42.35"))
        assertTrue(transactionsXml.contains("Tatra Visa"))

        val planXml = entries.getValue("xl/worksheets/sheet3.xml")
        assertTrue(planXml.contains("Výplata &amp; Odmeny"))
        assertTrue(planXml.contains("Nájom &lt;Bývanie&gt;"))
        assertEquals(10, entries.size)
    }
}
