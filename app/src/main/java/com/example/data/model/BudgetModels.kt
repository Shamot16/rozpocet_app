package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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
    val cardId: String = "", // ID of credit card or empty string
    val ekasaReceiptId: String = "" // Linked eKasa receiptId if created from QR receipt
)

enum class EkasaAllocationStatus(val label: String) {
    SCANNED_ONLY("Naskenovaný"),
    PARTIAL("Čiastočne rozdelený"),
    FULL("Plne uložený");

    companion object {
        fun fromString(raw: String): EkasaAllocationStatus {
            return entries.firstOrNull { it.name == raw } ?: SCANNED_ONLY
        }
    }
}

data class EkasaLineItem(
    val index: Int,
    val name: String,
    val quantity: Double,
    val price: Double,
    val itemType: String = "K",
    val assignedCategory: String = ""
)

data class SavedEkasaReceipt(
    val receiptId: String,
    val organizationName: String,
    val issueDate: String,
    val totalPrice: Double,
    val items: List<EkasaLineItem>,
    val allocationStatus: EkasaAllocationStatus,
    val lastFetchedAt: Long,
    val updatedAt: Long
) {
    val assignedAmount: Double
        get() = items.filter { it.assignedCategory.isNotBlank() }.sumOf { it.price }

    val remainingAmount: Double
        get() = maxOf(0.0, totalPrice - assignedAmount)

    val unassignedCount: Int
        get() = items.count { it.assignedCategory.isBlank() }

    val shortReceiptId: String
        get() = if (receiptId.length > 12) {
            "${receiptId.take(6)}…${receiptId.takeLast(4)}"
        } else {
            receiptId
        }
}

data class SplitCategoryBatch(
    val category: String,
    val itemIndices: List<Int>,
    val itemNames: List<String>,
    val amount: Double
)

object EkasaItemCodec {
    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val listType = Types.newParameterizedType(
        List::class.java,
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )
    private val listAdapter by lazy { moshi.adapter<List<Map<String, Any?>>>(listType) }

    fun encodeItems(items: List<EkasaLineItem>): String {
        val raw = items.map { item ->
            buildMap<String, Any?> {
                put("index", item.index)
                put("name", item.name)
                put("qty", item.quantity)
                put("price", item.price)
                put("itemType", item.itemType)
                if (item.assignedCategory.isNotBlank()) {
                    put("cat", item.assignedCategory)
                }
            }
        }
        return listAdapter.toJson(raw) ?: "[]"
    }

    fun decodeItems(json: String): List<EkasaLineItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val rawList = listAdapter.fromJson(json).orEmpty()
            rawList.mapIndexed { i, obj ->
                EkasaLineItem(
                    index = (obj["index"] as? Number)?.toInt() ?: i,
                    name = (obj["name"] as? String)?.ifBlank { null } ?: "Položka ${i + 1}",
                    quantity = (obj["qty"] as? Number)?.toDouble() ?: 1.0,
                    price = (obj["price"] as? Number)?.toDouble() ?: 0.0,
                    itemType = (obj["itemType"] as? String)?.ifBlank { null } ?: "K",
                    assignedCategory = (obj["cat"] as? String) ?: ""
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

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
        return formatCurrency(amount)
    }
}
