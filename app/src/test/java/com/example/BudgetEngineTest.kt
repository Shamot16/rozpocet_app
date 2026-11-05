package com.example

import com.example.data.model.BudgetItem
import com.example.data.model.CreditCard
import com.example.data.model.Transaction
import com.example.domain.BudgetEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetEngineTest {

    @Test
    fun testMonthlyOccurrences() {
        val item = BudgetItem(
            id = "1",
            name = "Nájom",
            type = "out",
            category = "Bývanie",
            amount = 600.0,
            frequency = "monthly",
            startDate = "2026-01-01"
        )
        val occ = BudgetEngine.getOccurrences(item, 2026, 10) // Nov 2026
        assertEquals(listOf(1), occ)
    }

    @Test
    fun testOnceOccurrencesOnlyInStartMonth() {
        val item = BudgetItem(
            id = "2",
            name = "Jednorazová oprava",
            type = "out",
            category = "Iné",
            amount = 150.0,
            frequency = "once",
            startDate = "2026-11-15"
        )
        // In Nov 2026 (month 10) -> should occur on day 15
        assertEquals(listOf(15), BudgetEngine.getOccurrences(item, 2026, 10))
        // In Dec 2026 (month 11) -> should be empty
        assertTrue(BudgetEngine.getOccurrences(item, 2026, 11).isEmpty())
    }

    @Test
    fun testQuarterlyOccurrences() {
        val item = BudgetItem(
            id = "3",
            name = "Štvrťročné predplatné",
            type = "out",
            category = "Zábava a voľný čas",
            amount = 30.0,
            frequency = "quarterly",
            startDate = "2026-01-10"
        )
        // Month 0 (Jan) -> occurs
        assertEquals(listOf(10), BudgetEngine.getOccurrences(item, 2026, 0))
        // Month 1 (Feb) -> empty
        assertTrue(BudgetEngine.getOccurrences(item, 2026, 1).isEmpty())
        // Month 3 (Apr) -> occurs
        assertEquals(listOf(10), BudgetEngine.getOccurrences(item, 2026, 3))
    }

    @Test
    fun testForecastCalculation() {
        val items = listOf(
            BudgetItem("1", "Mzda", "in", "Mzda", 2000.0, "monthly", "2026-01-10"),
            BudgetItem("2", "Nájom", "out", "Bývanie", 800.0, "monthly", "2026-01-01")
        )
        val forecast = BudgetEngine.compute12MonthForecast(
            initialBalance = 1000.0,
            items = items,
            currentYear = 2026,
            currentMonth = 10 // November
        )
        assertEquals(12, forecast.size)
        // Month 0 (Nov): Income 2000, Expense 800 -> Net 1200 -> Ending Balance = 1000 + 1200 = 2200
        assertEquals(2000.0, forecast[0].income, 0.01)
        assertEquals(800.0, forecast[0].expense, 0.01)
        assertEquals(2200.0, forecast[0].balance, 0.01)
    }

    @Test
    fun testCreditCardStatusCalculation() {
        val card = CreditCard(
            id = "c1",
            name = "Test Card",
            limit = 2000.0,
            statementDay = 15,
            dueDay = 25,
            initialDebt = 0.0
        )
        val txs = listOf(
            Transaction("t1", "Nákup", "out", "Potraviny", 200.0, "2026-11-05", cardId = "c1")
        )
        val fixedCal = Calendar.getInstance().apply {
            set(2026, 10, 16) // Nov 16 (after statement day 15)
        }
        val status = BudgetEngine.computeCardStatus(card, txs, emptyList(), nowCal = fixedCal)
        assertEquals(200.0, status.currentDebt, 0.01)
        assertEquals(1800.0, status.availableLimit, 0.01)
        assertEquals(10, status.usagePercent)
    }
}
