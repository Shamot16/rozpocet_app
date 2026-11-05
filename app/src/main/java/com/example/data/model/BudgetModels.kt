package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

data class BudgetItem(
    val id: String,
    val name: String,
    val type: String, // "out" (Výdavok) or "in" (Príjem)
    val category: String,
    val amount: Double,
    val frequency: String, // "weekly", "monthly", "quarterly", "yearly", "once"
    val startDate: String // "YYYY-MM-DD"
)

data class Transaction(
    val id: String,
    val name: String,
    val type: String, // "out" or "in"
    val category: String,
    val amount: Double,
    val date: String, // "YYYY-MM-DD"
    val cardId: String = "" // ID of credit card or empty string
)

data class CreditCard(
    val id: String,
    val name: String,
    val limit: Double,
    val statementDay: Int, // 1-31
    val dueDay: Int, // 1-31
    val initialDebt: Double
)

data class CardPayment(
    val id: String,
    val cardId: String,
    val date: String, // "YYYY-MM-DD"
    val amount: Double
)

data class MonthForecast(
    val monthIndex: Int,
    val year: Int,
    val month: Int, // 0-indexed (0 = január)
    val monthName: String,
    val income: Double,
    val expense: Double,
    val balance: Double, // Ending balance
    val openingBalance: Double
) {
    val net: Double get() = income - expense
}

data class CashflowDayEntry(
    val day: Int,
    val month: Int, // 0-indexed
    val name: String,
    val type: String, // "in" / "out"
    val category: String,
    val amount: Double,
    val runningBalance: Double
)

data class PlanVsActualRow(
    val category: String,
    val type: String, // "in" / "out"
    val planned: Double,
    val actual: Double
) {
    val diff: Double get() = actual - planned
    // For expense: true if actual > planned (over budget)
    // For income: true if actual < planned (under income)
    val isBad: Boolean get() = if (type == "out") diff > 0.01 else diff < -0.01
}

data class CardStatus(
    val card: CreditCard,
    val currentDebt: Double,
    val availableLimit: Double,
    val usagePercent: Int,
    val statementDateStr: String,
    val dueDateStr: String,
    val statementAmount: Double,
    val purchasesSinceStatement: Double,
    val isOverdue: Boolean
)

data class CardsExecutiveSummary(
    val totalDebt: Double,
    val totalLimit: Double,
    val totalAvailableLimit: Double,
    val netCashBalance: Double, // Initial cash minus credit card debts
    val cards: List<CardStatus>
)

object BudgetConstants {
    val ExpenseCategories = listOf(
        "Bývanie",
        "Energie a internet",
        "Potraviny",
        "Doprava",
        "Deti a škola",
        "Zdravie",
        "Poistky",
        "Splátky úverov",
        "Zábava a voľný čas",
        "Oblečenie",
        "Rezerva a sporenie",
        "Iné"
    )

    val IncomeCategories = listOf(
        "Mzda",
        "Prídavky a dávky",
        "Iný príjem"
    )

    val Frequencies = listOf(
        "monthly" to "Mesačne",
        "weekly" to "Každý týždeň",
        "quarterly" to "Štvrťročne",
        "yearly" to "Ročne",
        "once" to "Jednorazovo"
    )

    val FrequencyLabels = mapOf(
        "weekly" to "každý týždeň",
        "monthly" to "mesačne",
        "quarterly" to "štvrťročne",
        "yearly" to "ročne",
        "once" to "jednorazovo"
    )

    val SlovakMonths = listOf(
        "január", "február", "marec", "apríl", "máj", "jún",
        "júl", "august", "september", "október", "november", "december"
    )

    private val slovakLocale = Locale("sk", "SK")

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(slovakLocale)
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2
        return format.format(amount)
    }

    fun formatCurrencyNoDecimals(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(slovakLocale)
        format.maximumFractionDigits = 0
        return format.format(amount)
    }
}
