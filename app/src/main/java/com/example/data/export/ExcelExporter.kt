package com.example.data.export

import com.example.data.model.BudgetConstants
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CardsExecutiveSummary
import com.example.data.model.CreditCard
import com.example.data.model.MonthForecast
import com.example.data.model.Transaction
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Lightweight, dependency-free OpenXML (.xlsx) workbook generator.
 * Produces standard multi-sheet Excel workbooks compatible with Microsoft Excel,
 * Google Sheets, and LibreOffice Calc.
 */
object ExcelExporter {

    private sealed class CellValue {
        data class Text(val value: String, val style: Int = STYLE_TEXT) : CellValue()
        data class Number(val value: Double, val style: Int = STYLE_CURRENCY) : CellValue()
        data class Integer(val value: Int, val style: Int = STYLE_INT) : CellValue()
    }

    private data class SheetData(
        val name: String,
        val rows: List<List<CellValue>>
    )

    private const val STYLE_DEFAULT = 0
    private const val STYLE_HEADER = 1
    private const val STYLE_TEXT = 2
    private const val STYLE_CURRENCY = 3
    private const val STYLE_BOLD_LABEL = 4
    private const val STYLE_BOLD_CURRENCY = 5
    private const val STYLE_INT = 6

    fun buildWorkbookBytes(
        initialBalance: Double,
        initialBalanceDate: String,
        forecast: List<MonthForecast>,
        cardsSummary: CardsExecutiveSummary,
        items: List<BudgetItem>,
        transactions: List<Transaction>,
        cards: List<CreditCard>,
        cardPayments: List<CardPayment>
    ): ByteArray {
        val cardMap = cards.associateBy { it.id }

        val sheets = listOf(
            buildOverviewSheet(initialBalance, initialBalanceDate, forecast, cardsSummary),
            buildTransactionsSheet(transactions, cardMap),
            buildPlannedItemsSheet(items),
            buildCreditCardsSheet(cards, cardsSummary),
            buildCardPaymentsSheet(cardPayments, cardMap)
        )

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size))
            writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheets.size))
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets))
            writeZipEntry(zip, "xl/styles.xml", buildStylesXml())
            sheets.forEachIndexed { index, sheet ->
                writeZipEntry(zip, "xl/worksheets/sheet${index + 1}.xml", buildWorksheetXml(sheet))
            }
        }
        return baos.toByteArray()
    }

    private fun buildOverviewSheet(
        initialBalance: Double,
        initialBalanceDate: String,
        forecast: List<MonthForecast>,
        cardsSummary: CardsExecutiveSummary
    ): SheetData {
        val rows = mutableListOf<List<CellValue>>()

        rows.add(
            listOf(
                CellValue.Text("Rodinný rozpočet – Prehľad", STYLE_HEADER),
                CellValue.Text("Hodnota", STYLE_HEADER)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Počiatočný zostatok", STYLE_BOLD_LABEL),
                CellValue.Number(initialBalance, STYLE_BOLD_CURRENCY)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Platnosť počiatočného zostatku", STYLE_TEXT),
                CellValue.Text(if (initialBalanceDate.isNotBlank()) initialBalanceDate else "Neuvedené", STYLE_TEXT)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Aktuálny dlh na kreditných kartách", STYLE_TEXT),
                CellValue.Number(cardsSummary.totalDebt, STYLE_CURRENCY)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Čistá finančná pozícia (Zostatok − dlh)", STYLE_BOLD_LABEL),
                CellValue.Number(cardsSummary.netCashBalance, STYLE_BOLD_CURRENCY)
            )
        )
        rows.add(emptyList())

        // 12-Month forecast header
        rows.add(
            listOf(
                CellValue.Text("Mesiac", STYLE_HEADER),
                CellValue.Text("Otvárací zostatok (€)", STYLE_HEADER),
                CellValue.Text("Príjmy (€)", STYLE_HEADER),
                CellValue.Text("Výdavky (€)", STYLE_HEADER),
                CellValue.Text("Čistý cashflow (€)", STYLE_HEADER),
                CellValue.Text("Konečný zostatok (€)", STYLE_HEADER)
            )
        )

        for (m in forecast) {
            rows.add(
                listOf(
                    CellValue.Text(m.monthName, STYLE_TEXT),
                    CellValue.Number(m.openingBalance, STYLE_CURRENCY),
                    CellValue.Number(m.income, STYLE_CURRENCY),
                    CellValue.Number(m.expense, STYLE_CURRENCY),
                    CellValue.Number(m.net, STYLE_CURRENCY),
                    CellValue.Number(m.balance, STYLE_CURRENCY)
                )
            )
        }

        if (forecast.isNotEmpty()) {
            val totalIncome = forecast.sumOf { it.income }
            val totalExpense = forecast.sumOf { it.expense }
            val totalNet = totalIncome - totalExpense
            rows.add(
                listOf(
                    CellValue.Text("Spolu za 12 mesiacov", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Number(totalIncome, STYLE_BOLD_CURRENCY),
                    CellValue.Number(totalExpense, STYLE_BOLD_CURRENCY),
                    CellValue.Number(totalNet, STYLE_BOLD_CURRENCY),
                    CellValue.Number(forecast.last().balance, STYLE_BOLD_CURRENCY)
                )
            )
        }

        return SheetData("Prehľad a prognóza", rows)
    }

    private fun buildTransactionsSheet(
        transactions: List<Transaction>,
        cardMap: Map<String, CreditCard>
    ): SheetData {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("Dátum", STYLE_HEADER),
                CellValue.Text("Názov", STYLE_HEADER),
                CellValue.Text("Typ", STYLE_HEADER),
                CellValue.Text("Kategória", STYLE_HEADER),
                CellValue.Text("Suma (€)", STYLE_HEADER),
                CellValue.Text("Kreditná karta", STYLE_HEADER)
            )
        )

        val sorted = transactions.sortedByDescending { it.date }
        for (tx in sorted) {
            val typeLabel = if (tx.type == "in") "Príjem" else "Výdavok"
            val cardName = if (tx.cardId.isNotBlank()) cardMap[tx.cardId]?.name ?: tx.cardId else "Hotovosť / Účet"
            rows.add(
                listOf(
                    CellValue.Text(tx.date, STYLE_TEXT),
                    CellValue.Text(tx.name, STYLE_TEXT),
                    CellValue.Text(typeLabel, STYLE_TEXT),
                    CellValue.Text(tx.category, STYLE_TEXT),
                    CellValue.Number(tx.amount, STYLE_CURRENCY),
                    CellValue.Text(cardName, STYLE_TEXT)
                )
            )
        }

        if (sorted.isNotEmpty()) {
            val totalIn = sorted.filter { it.type == "in" }.sumOf { it.amount }
            val totalOut = sorted.filter { it.type == "out" }.sumOf { it.amount }
            rows.add(emptyList())
            rows.add(
                listOf(
                    CellValue.Text("Príjmy spolu", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Number(totalIn, STYLE_BOLD_CURRENCY),
                    CellValue.Text("", STYLE_BOLD_LABEL)
                )
            )
            rows.add(
                listOf(
                    CellValue.Text("Výdavky spolu", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Text("", STYLE_BOLD_LABEL),
                    CellValue.Number(totalOut, STYLE_BOLD_CURRENCY),
                    CellValue.Text("", STYLE_BOLD_LABEL)
                )
            )
        }

        return SheetData("Skutočné platby", rows)
    }

    private fun buildPlannedItemsSheet(items: List<BudgetItem>): SheetData {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("Názov", STYLE_HEADER),
                CellValue.Text("Typ", STYLE_HEADER),
                CellValue.Text("Kategória", STYLE_HEADER),
                CellValue.Text("Suma (€)", STYLE_HEADER),
                CellValue.Text("Frekvencia", STYLE_HEADER),
                CellValue.Text("Prvý dátum platby", STYLE_HEADER)
            )
        )

        for (item in items) {
            val typeLabel = if (item.type == "in") "Príjem" else "Výdavok"
            val freqLabel = BudgetConstants.FrequencyLabels[item.frequency]
                ?.replaceFirstChar { it.uppercase(Locale.getDefault()) }
                ?: item.frequency
            rows.add(
                listOf(
                    CellValue.Text(item.name, STYLE_TEXT),
                    CellValue.Text(typeLabel, STYLE_TEXT),
                    CellValue.Text(item.category, STYLE_TEXT),
                    CellValue.Number(item.amount, STYLE_CURRENCY),
                    CellValue.Text(freqLabel, STYLE_TEXT),
                    CellValue.Text(item.startDate, STYLE_TEXT)
                )
            )
        }

        return SheetData("Pravidelné položky", rows)
    }

    private fun buildCreditCardsSheet(
        cards: List<CreditCard>,
        summary: CardsExecutiveSummary
    ): SheetData {
        val statusMap = summary.cards.associateBy { it.card.id }
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("Názov karty", STYLE_HEADER),
                CellValue.Text("Úverový limit (€)", STYLE_HEADER),
                CellValue.Text("Aktuálny dlh (€)", STYLE_HEADER),
                CellValue.Text("Voľný limit (€)", STYLE_HEADER),
                CellValue.Text("Počiatočný dlh (€)", STYLE_HEADER),
                CellValue.Text("Deň výpisu", STYLE_HEADER),
                CellValue.Text("Deň splatnosti", STYLE_HEADER)
            )
        )

        for (card in cards) {
            val st = statusMap[card.id]
            rows.add(
                listOf(
                    CellValue.Text(card.name, STYLE_TEXT),
                    CellValue.Number(card.limit, STYLE_CURRENCY),
                    CellValue.Number(st?.currentDebt ?: card.initialDebt, STYLE_CURRENCY),
                    CellValue.Number(st?.availableLimit ?: maxOf(0.0, card.limit - card.initialDebt), STYLE_CURRENCY),
                    CellValue.Number(card.initialDebt, STYLE_CURRENCY),
                    CellValue.Integer(card.statementDay, STYLE_INT),
                    CellValue.Integer(card.dueDay, STYLE_INT)
                )
            )
        }

        return SheetData("Kreditné karty", rows)
    }

    private fun buildCardPaymentsSheet(
        payments: List<CardPayment>,
        cardMap: Map<String, CreditCard>
    ): SheetData {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("Dátum splátky", STYLE_HEADER),
                CellValue.Text("Karta", STYLE_HEADER),
                CellValue.Text("Suma splátky (€)", STYLE_HEADER)
            )
        )

        val sorted = payments.sortedByDescending { it.date }
        for (p in sorted) {
            val cardName = cardMap[p.cardId]?.name ?: p.cardId
            rows.add(
                listOf(
                    CellValue.Text(p.date, STYLE_TEXT),
                    CellValue.Text(cardName, STYLE_TEXT),
                    CellValue.Number(p.amount, STYLE_CURRENCY)
                )
            )
        }

        return SheetData("Splátky kariet", rows)
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun buildRootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun buildWorkbookXml(sheets: List<SheetData>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("<sheets>")
        sheets.forEachIndexed { index, sheet ->
            val safeName = sanitizeSheetName(sheet.name)
            append("""<sheet name="${escapeXml(safeName)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
        }
        append("</sheets>")
        append("</workbook>")
    }

    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <numFmts count="1">
            <numFmt numFmtId="164" formatCode="#,##0.00&quot; €&quot;"/>
          </numFmts>
          <fonts count="3">
            <font><sz val="11"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
          </fonts>
          <fills count="4">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FF1565C0"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFEFF6FF"/></patternFill></fill>
          </fills>
          <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border>
              <left style="thin"><color rgb="FFCBD5E1"/></left>
              <right style="thin"><color rgb="FFCBD5E1"/></right>
              <top style="thin"><color rgb="FFCBD5E1"/></top>
              <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
              <diagonal/>
            </border>
          </borders>
          <cellStyleXfs count="1">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
          </cellStyleXfs>
          <cellXfs count="7">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment vertical="center"/></xf>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment vertical="center"/></xf>
            <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
            <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"><alignment vertical="center"/></xf>
            <xf numFmtId="164" fontId="2" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
            <xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"><alignment horizontal="right" vertical="center"/></xf>
          </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun buildWorksheetXml(sheet: SheetData): String = buildString {
        val maxCols = sheet.rows.maxOfOrNull { it.size } ?: 1
        val colWidths = IntArray(maxCols) { 14 }

        for (row in sheet.rows) {
            row.forEachIndexed { colIdx, cell ->
                val len = when (cell) {
                    is CellValue.Text -> cell.value.length + 4
                    is CellValue.Number -> 16
                    is CellValue.Integer -> 12
                }
                if (len > colWidths[colIdx]) {
                    colWidths[colIdx] = len.coerceIn(14, 42)
                }
            }
        }

        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("<cols>")
        colWidths.forEachIndexed { idx, w ->
            val colNum = idx + 1
            append("""<col min="$colNum" max="$colNum" width="$w" customWidth="1"/>""")
        }
        append("</cols>")
        append("<sheetData>")

        sheet.rows.forEachIndexed { rowIdx, row ->
            val rowNum = rowIdx + 1
            if (row.isEmpty()) {
                append("""<row r="$rowNum"/>""")
            } else {
                append("""<row r="$rowNum">""")
                row.forEachIndexed { colIdx, cell ->
                    val ref = "${columnLetters(colIdx)}$rowNum"
                    when (cell) {
                        is CellValue.Text -> {
                            append("""<c r="$ref" s="${cell.style}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(cell.value)}</t></is></c>""")
                        }
                        is CellValue.Number -> {
                            val safeNum = if (cell.value.isFinite()) cell.value else 0.0
                            val formatted = String.format(Locale.US, "%.2f", safeNum)
                            append("""<c r="$ref" s="${cell.style}"><v>$formatted</v></c>""")
                        }
                        is CellValue.Integer -> {
                            append("""<c r="$ref" s="${cell.style}"><v>${cell.value}</v></c>""")
                        }
                    }
                }
                append("</row>")
            }
        }

        append("</sheetData>")
        append("</worksheet>")
    }

    private fun columnLetters(zeroBasedIndex: Int): String {
        var idx = zeroBasedIndex
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.append(('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun sanitizeSheetName(name: String): String {
        val cleaned = name.replace(Regex("[:\\\\/?*\\[\\]]"), " ").trim()
        return if (cleaned.length > 31) cleaned.substring(0, 31) else cleaned.ifEmpty { "Hárok" }
    }

    private fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                '\t', '\n', '\r' -> sb.append(ch)
                else -> if (ch.code >= 0x20) sb.append(ch)
            }
        }
        return sb.toString()
    }
}
