package com.example.data.repository

import com.example.data.export.ExcelExporter
import com.example.data.local.BudgetDao
import com.example.data.local.BudgetItemEntity
import com.example.data.local.CardPaymentEntity
import com.example.data.local.ConfigEntity
import com.example.data.local.CreditCardEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CreditCard
import com.example.data.model.Transaction
import com.example.domain.BudgetEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BudgetRepository(private val dao: BudgetDao) {

    val initialBalance: Flow<Double> = dao.getConfigFlow("initial_balance")
        .map { entity -> entity?.value?.toDoubleOrNull() ?: 0.0 }

    val initialBalanceDate: Flow<String> = dao.getConfigFlow("initial_balance_date")
        .map { entity -> entity?.value ?: "" }

    val budgetItems: Flow<List<BudgetItem>> = dao.getAllBudgetItems()
        .map { list -> list.map { it.toDomain() } }

    val transactions: Flow<List<Transaction>> = dao.getAllTransactions()
        .map { list -> list.map { it.toDomain() } }

    val creditCards: Flow<List<CreditCard>> = dao.getAllCreditCards()
        .map { list -> list.map { it.toDomain() } }

    val cardPayments: Flow<List<CardPayment>> = dao.getAllCardPayments()
        .map { list -> list.map { it.toDomain() } }

    suspend fun setInitialBalance(balance: Double, date: String = "") {
        dao.setConfig(ConfigEntity("initial_balance", balance.toString()))
        if (date.isNotBlank()) {
            dao.setConfig(ConfigEntity("initial_balance_date", date))
        }
    }

    suspend fun addBudgetItem(item: BudgetItem) {
        dao.insertBudgetItem(BudgetItemEntity.fromDomain(item))
    }

    suspend fun updateBudgetItem(item: BudgetItem) {
        dao.insertBudgetItem(BudgetItemEntity.fromDomain(item))
    }

    suspend fun deleteBudgetItem(id: String) {
        dao.deleteBudgetItem(id)
    }

    suspend fun addTransaction(tx: Transaction) {
        dao.insertTransaction(TransactionEntity.fromDomain(tx))
    }

    suspend fun deleteTransaction(id: String) {
        dao.deleteTransaction(id)
    }

    suspend fun addCreditCard(card: CreditCard) {
        dao.insertCreditCard(CreditCardEntity.fromDomain(card))
    }

    suspend fun deleteCreditCard(id: String) {
        dao.deleteCardAndRelated(id)
    }

    suspend fun addCardPayment(payment: CardPayment) {
        dao.insertCardPayment(CardPaymentEntity.fromDomain(payment))
    }

    suspend fun deleteCardPayment(id: String) {
        dao.deleteCardPayment(id)
    }

    suspend fun clearAllData() {
        dao.clearAllData()
    }

    suspend fun loadDemoData() {
        val now = Calendar.getInstance()
        val y = now.get(Calendar.YEAR)
        val m = now.get(Calendar.MONTH) + 1
        val d = now.get(Calendar.DAY_OF_MONTH)

        fun dateStr(day: Int): String {
            val validDay = minOf(day, 28)
            return String.format(Locale.US, "%04d-%02d-%02d", y, m, validDay)
        }

        val demoItems = listOf(
            BudgetItem(UUID.randomUUID().toString(), "Mzda", "in", "Mzda", 1800.0, "monthly", dateStr(10)),
            BudgetItem(UUID.randomUUID().toString(), "Prídavky na deti", "in", "Prídavky a dávky", 100.0, "monthly", dateStr(20)),
            BudgetItem(UUID.randomUUID().toString(), "Nájom", "out", "Bývanie", 600.0, "monthly", dateStr(1)),
            BudgetItem(UUID.randomUUID().toString(), "Elektrina a plyn", "out", "Energie a internet", 140.0, "monthly", dateStr(15)),
            BudgetItem(UUID.randomUUID().toString(), "Nákup potravín", "out", "Potraviny", 90.0, "weekly", dateStr(d)),
            BudgetItem(UUID.randomUUID().toString(), "Poistenie auta", "out", "Poistky", 280.0, "yearly", dateStr(25)),
            BudgetItem(UUID.randomUUID().toString(), "Rezerva", "out", "Rezerva a sporenie", 150.0, "monthly", dateStr(11))
        )

        val demoCards = listOf(
            CreditCard(UUID.randomUUID().toString(), "Hlavná kreditka", 1500.0, 15, 25, 0.0)
        )

        dao.clearAllData()
        setInitialBalance(1200.0)
        dao.insertBudgetItems(demoItems.map { BudgetItemEntity.fromDomain(it) })
        dao.insertCreditCards(demoCards.map { CreditCardEntity.fromDomain(it) })
    }

    suspend fun exportJson(): String {
        val bal = dao.getConfig("initial_balance")?.value?.toDoubleOrNull() ?: 0.0
        val balDate = dao.getConfig("initial_balance_date")?.value ?: ""
        val items = dao.getAllBudgetItemsDirect()
        val txs = dao.getAllTransactionsDirect()
        val cards = dao.getAllCreditCardsDirect()
        val pays = dao.getAllCardPaymentsDirect()

        val root = JSONObject()
        root.put("bal", bal)
        if (balDate.isNotBlank()) {
            root.put("balDate", balDate)
        }

        val itemsArr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("type", item.type)
            obj.put("cat", item.category)
            obj.put("amt", item.amount)
            obj.put("freq", item.frequency)
            obj.put("date", item.startDate)
            itemsArr.put(obj)
        }
        root.put("items", itemsArr)

        val txArr = JSONArray()
        for (tx in txs) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("name", tx.name)
            obj.put("type", tx.type)
            obj.put("cat", tx.category)
            obj.put("amt", tx.amount)
            obj.put("date", tx.date)
            obj.put("card", tx.cardId)
            txArr.put(obj)
        }
        root.put("tx", txArr)

        val cardArr = JSONArray()
        for (c in cards) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("limit", c.limit)
            obj.put("stmt", c.statementDay)
            obj.put("due", c.dueDay)
            obj.put("debt0", c.initialDebt)
            cardArr.put(obj)
        }
        root.put("cards", cardArr)

        val paysArr = JSONArray()
        for (p in pays) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("card", p.cardId)
            obj.put("date", p.date)
            obj.put("amt", p.amount)
            paysArr.put(obj)
        }
        root.put("pays", paysArr)

        return root.toString(2)
    }

    suspend fun exportXlsxBytes(): ByteArray {
        val bal = dao.getConfig("initial_balance")?.value?.toDoubleOrNull() ?: 0.0
        val balDate = dao.getConfig("initial_balance_date")?.value ?: ""
        val items = dao.getAllBudgetItemsDirect().map { it.toDomain() }
        val txs = dao.getAllTransactionsDirect().map { it.toDomain() }
        val cards = dao.getAllCreditCardsDirect().map { it.toDomain() }
        val pays = dao.getAllCardPaymentsDirect().map { it.toDomain() }

        val cal = Calendar.getInstance()
        val forecast = BudgetEngine.compute12MonthForecast(
            initialBalance = bal,
            items = items,
            currentYear = cal.get(Calendar.YEAR),
            currentMonth = cal.get(Calendar.MONTH)
        )
        val cardsSummary = BudgetEngine.computeCardsExecutiveSummary(
            initialCashBalance = bal,
            cards = cards,
            transactions = txs,
            payments = pays
        )

        return ExcelExporter.buildWorkbookBytes(
            initialBalance = bal,
            initialBalanceDate = balDate,
            forecast = forecast,
            cardsSummary = cardsSummary,
            items = items,
            transactions = txs,
            cards = cards,
            cardPayments = pays
        )
    }

    suspend fun importJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            val bal = root.optDouble("bal", 0.0)
            val balDate = root.optString("balDate", "")

            val itemsList = mutableListOf<BudgetItemEntity>()
            val itemsArr = root.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    itemsList.add(
                        BudgetItemEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Položka"),
                            type = obj.optString("type", "out"),
                            category = obj.optString("cat", "Iné"),
                            amount = obj.optDouble("amt", 0.0),
                            frequency = obj.optString("freq", "monthly"),
                            startDate = obj.optString("date", todayStr())
                        )
                    )
                }
            }

            val txList = mutableListOf<TransactionEntity>()
            val txArr = root.optJSONArray("tx")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Platba"),
                            type = obj.optString("type", "out"),
                            category = obj.optString("cat", "Iné"),
                            amount = obj.optDouble("amt", 0.0),
                            date = obj.optString("date", todayStr()),
                            cardId = obj.optString("card", "")
                        )
                    )
                }
            }

            val cardList = mutableListOf<CreditCardEntity>()
            val cardArr = root.optJSONArray("cards")
            if (cardArr != null) {
                for (i in 0 until cardArr.length()) {
                    val obj = cardArr.getJSONObject(i)
                    cardList.add(
                        CreditCardEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Karta"),
                            limit = obj.optDouble("limit", 1000.0),
                            statementDay = obj.optInt("stmt", 1),
                            dueDay = obj.optInt("due", 1),
                            initialDebt = obj.optDouble("debt0", 0.0)
                        )
                    )
                }
            }

            val paysList = mutableListOf<CardPaymentEntity>()
            val paysArr = root.optJSONArray("pays")
            if (paysArr != null) {
                for (i in 0 until paysArr.length()) {
                    val obj = paysArr.getJSONObject(i)
                    paysList.add(
                        CardPaymentEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            cardId = obj.optString("card", ""),
                            date = obj.optString("date", todayStr()),
                            amount = obj.optDouble("amt", 0.0)
                        )
                    )
                }
            }

            dao.replaceFullData(
                initialBalance = bal,
                initialBalanceDate = balDate,
                items = itemsList,
                transactions = txList,
                cards = cardList,
                payments = paysList
            )

            val totalImported = itemsList.size + txList.size + cardList.size
            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun todayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
