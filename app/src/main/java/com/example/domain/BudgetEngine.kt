package com.example.domain

import com.example.data.model.BudgetConstants
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CardStatus
import com.example.data.model.CardsExecutiveSummary
import com.example.data.model.CashflowDayEntry
import com.example.data.model.CreditCard
import com.example.data.model.MonthForecast
import com.example.data.model.PlanVsActualRow
import com.example.data.model.Transaction
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object BudgetEngine {

    fun getDaysInMonth(year: Int, month: Int): Int {
        // month: 0-indexed (0 = Jan, 11 = Dec)
        return when (month) {
            1 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            3, 5, 8, 10 -> 30
            else -> 31
        }
    }

    /**
     * Calculates days of month on which an item occurs in a given year and month (0-indexed).
     * Exact replication of the original JavaScript formula:
     * mi = (y * 12 + m) - (ay * 12 + am - 1)
     */
    fun getOccurrences(item: BudgetItem, year: Int, month: Int): List<Int> {
        val parts = item.startDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size < 3) return emptyList()
        val ay = parts[0]
        val am = parts[1]
        val ad = parts[2]

        val dim = getDaysInMonth(year, month)
        val mi = (year * 12 + month) - (ay * 12 + am - 1)
        val dd = minOf(ad, dim)

        if (item.frequency == "once") {
            return if (mi == 0) listOf(ad) else emptyList()
        }
        if (mi < 0) return emptyList()

        return when (item.frequency) {
            "monthly" -> listOf(dd)
            "quarterly" -> if (mi % 3 == 0) listOf(dd) else emptyList()
            "yearly" -> if (mi % 12 == 0) listOf(dd) else emptyList()
            "weekly" -> {
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(ay, am - 1, ad, 0, 0, 0)
                }
                val startMillis = startCal.timeInMillis
                val result = mutableListOf<Int>()
                val checkCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                for (d in 1..dim) {
                    checkCal.clear()
                    checkCal.set(year, month, d, 0, 0, 0)
                    val diff = checkCal.timeInMillis - startMillis
                    if (diff >= 0 && (diff / 86_400_000L) % 7L == 0L) {
                        result.add(d)
                    }
                }
                result
            }
            else -> emptyList()
        }
    }

    data class PlannedEntry(
        val day: Int,
        val name: String,
        val type: String,
        val category: String,
        val amount: Double
    )

    fun getPlannedEntriesForMonth(
        items: List<BudgetItem>,
        year: Int,
        month: Int
    ): List<PlannedEntry> {
        val list = mutableListOf<PlannedEntry>()
        for (item in items) {
            val days = getOccurrences(item, year, month)
            for (day in days) {
                list.add(
                    PlannedEntry(
                        day = day,
                        name = item.name,
                        type = item.type,
                        category = item.category,
                        amount = item.amount
                    )
                )
            }
        }
        return list.sortedBy { it.day }
    }

    /**
     * Computes the 12-month forward forecast starting from the current month.
     */
    fun compute12MonthForecast(
        initialBalance: Double,
        items: List<BudgetItem>,
        currentYear: Int,
        currentMonth: Int
    ): List<MonthForecast> {
        val forecast = mutableListOf<MonthForecast>()
        var runningBal = initialBalance

        for (i in 0 until 12) {
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth + i)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val y = targetCal.get(Calendar.YEAR)
            val m = targetCal.get(Calendar.MONTH)

            val monthPlan = getPlannedEntriesForMonth(items, y, m)
            val income = monthPlan.filter { it.type == "in" }.sumOf { it.amount }
            val expense = monthPlan.filter { it.type == "out" }.sumOf { it.amount }

            val openingBal = runningBal
            runningBal += (income - expense)

            val monthName = BudgetConstants.SlovakMonths.getOrElse(m) { "" }

            forecast.add(
                MonthForecast(
                    monthIndex = i,
                    year = y,
                    month = m,
                    monthName = "$monthName $y",
                    income = income,
                    expense = expense,
                    balance = runningBal,
                    openingBalance = openingBal
                )
            )
        }
        return forecast
    }

    /**
     * Detailed daily schedule for a selected month forecast with running balance.
     */
    fun computeDailyScheduleForMonth(
        openingBalance: Double,
        items: List<BudgetItem>,
        year: Int,
        month: Int
    ): List<CashflowDayEntry> {
        val plan = getPlannedEntriesForMonth(items, year, month)
        var running = openingBalance
        return plan.map { entry ->
            if (entry.type == "in") {
                running += entry.amount
            } else {
                running -= entry.amount
            }
            CashflowDayEntry(
                day = entry.day,
                month = month,
                name = entry.name,
                type = entry.type,
                category = entry.category,
                amount = entry.amount,
                runningBalance = running
            )
        }
    }

    /**
     * Compares planned expenses/incomes with actual transactions for a given month ("YYYY-MM").
     */
    fun computePlanVsActual(
        items: List<BudgetItem>,
        transactions: List<Transaction>,
        year: Int,
        month: Int // 0-indexed
    ): List<PlanVsActualRow> {
        val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month + 1)
        val monthTx = transactions.filter { it.date.startsWith(monthPrefix) }
        val planned = getPlannedEntriesForMonth(items, year, month)

        val rows = mutableListOf<PlanVsActualRow>()

        // Expenses
        for (cat in BudgetConstants.ExpenseCategories) {
            val pl = planned.filter { it.type == "out" && it.category == cat }.sumOf { it.amount }
            val ac = monthTx.filter { it.type == "out" && it.category == cat }.sumOf { it.amount }
            if (pl > 0 || ac > 0) {
                rows.add(
                    PlanVsActualRow(
                        category = cat,
                        type = "out",
                        planned = pl,
                        actual = ac
                    )
                )
            }
        }

        // Incomes
        for (cat in BudgetConstants.IncomeCategories) {
            val pl = planned.filter { it.type == "in" && it.category == cat }.sumOf { it.amount }
            val ac = monthTx.filter { it.type == "in" && it.category == cat }.sumOf { it.amount }
            if (pl > 0 || ac > 0) {
                rows.add(
                    PlanVsActualRow(
                        category = cat,
                        type = "in",
                        planned = pl,
                        actual = ac
                    )
                )
            }
        }

        return rows
    }

    /**
     * Credit card status calculations (debt, statements, due dates, available limit).
     */
    fun computeCardStatus(
        card: CreditCard,
        transactions: List<Transaction>,
        payments: List<CardPayment>,
        nowCal: Calendar = Calendar.getInstance()
    ): CardStatus {
        val cy = nowCal.get(Calendar.YEAR)
        val cm = nowCal.get(Calendar.MONTH)
        val cd = nowCal.get(Calendar.DAY_OF_MONTH)

        val cardTxs = transactions.filter { it.cardId == card.id }
        val cardPays = payments.filter { it.cardId == card.id }

        val totalPurchases = cardTxs.sumOf { it.amount }
        val totalPayments = cardPays.sumOf { it.amount }
        val currentDebt = maxOf(0.0, card.initialDebt + totalPurchases - totalPayments)
        val availableLimit = maxOf(0.0, card.limit - currentDebt)
        val usagePercent = if (card.limit > 0) {
            minOf(100, ((currentDebt / card.limit) * 100).toInt())
        } else 0

        // Statement date determination
        val stCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, cy)
            set(Calendar.MONTH, cm)
            val dim = getDaysInMonth(cy, cm)
            set(Calendar.DAY_OF_MONTH, minOf(card.statementDay, dim))
        }

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, cy)
            set(Calendar.MONTH, cm)
            set(Calendar.DAY_OF_MONTH, cd)
        }

        if (stCal.after(todayCal)) {
            // Previous month statement
            stCal.add(Calendar.MONTH, -1)
            val prevY = stCal.get(Calendar.YEAR)
            val prevM = stCal.get(Calendar.MONTH)
            val dim = getDaysInMonth(prevY, prevM)
            stCal.set(Calendar.DAY_OF_MONTH, minOf(card.statementDay, dim))
        }

        // Due date determination relative to statement date
        val duCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, stCal.get(Calendar.YEAR))
            set(Calendar.MONTH, stCal.get(Calendar.MONTH))
            val dim = getDaysInMonth(stCal.get(Calendar.YEAR), stCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, minOf(card.dueDay, dim))
        }

        if (!duCal.after(stCal)) {
            duCal.add(Calendar.MONTH, 1)
            val nextY = duCal.get(Calendar.YEAR)
            val nextM = duCal.get(Calendar.MONTH)
            val dim = getDaysInMonth(nextY, nextM)
            duCal.set(Calendar.DAY_OF_MONTH, minOf(card.dueDay, dim))
        }

        val stDateStr = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            stCal.get(Calendar.YEAR),
            stCal.get(Calendar.MONTH) + 1,
            stCal.get(Calendar.DAY_OF_MONTH)
        )

        val txUpToStmt = cardTxs.filter { it.date <= stDateStr }.sumOf { it.amount }
        val statementAmount = maxOf(0.0, card.initialDebt + txUpToStmt - totalPayments)
        val purchasesSinceStatement = maxOf(0.0, currentDebt - statementAmount)
        val isOverdue = todayCal.after(duCal) && statementAmount > 0.01

        val stDisplay = "${stCal.get(Calendar.DAY_OF_MONTH)}.${stCal.get(Calendar.MONTH) + 1}.${stCal.get(Calendar.YEAR)}"
        val duDisplay = "${duCal.get(Calendar.DAY_OF_MONTH)}.${duCal.get(Calendar.MONTH) + 1}.${duCal.get(Calendar.YEAR)}"

        return CardStatus(
            card = card,
            currentDebt = currentDebt,
            availableLimit = availableLimit,
            usagePercent = usagePercent,
            statementDateStr = stDisplay,
            dueDateStr = duDisplay,
            statementAmount = statementAmount,
            purchasesSinceStatement = purchasesSinceStatement,
            isOverdue = isOverdue
        )
    }

    fun computeCardsExecutiveSummary(
        initialCashBalance: Double,
        cards: List<CreditCard>,
        transactions: List<Transaction>,
        payments: List<CardPayment>
    ): CardsExecutiveSummary {
        val statuses = cards.map { computeCardStatus(it, transactions, payments) }
        val totalDebt = statuses.sumOf { it.currentDebt }
        val totalLimit = cards.sumOf { it.limit }
        val totalAvailable = maxOf(0.0, totalLimit - totalDebt)
        val netCash = initialCashBalance - totalDebt

        return CardsExecutiveSummary(
            totalDebt = totalDebt,
            totalLimit = totalLimit,
            totalAvailableLimit = totalAvailable,
            netCashBalance = netCash,
            cards = statuses
        )
    }
}
