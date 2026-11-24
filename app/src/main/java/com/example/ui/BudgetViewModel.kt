package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BudgetConstants
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CardsExecutiveSummary
import com.example.data.model.CashflowDayEntry
import com.example.data.model.CreditCard
import com.example.data.model.MonthForecast
import com.example.data.model.PlanVsActualRow
import com.example.data.model.Transaction
import com.example.data.repository.BudgetRepository
import com.example.domain.BudgetEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class BudgetTab(val label: String) {
    OVERVIEW("Prehľad"),
    PLAN("Pravidelné"),
    ACTUAL("Skutočnosť"),
    CARDS("Karty"),
    SETTINGS("Nastavenia")
}

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository = BudgetRepository(
        AppDatabase.getInstance(application).budgetDao()
    )

    // Current navigation tab
    private val _currentTab = MutableStateFlow(BudgetTab.OVERVIEW)
    val currentTab: StateFlow<BudgetTab> = _currentTab.asStateFlow()

    fun selectTab(tab: BudgetTab) {
        _currentTab.value = tab
    }

    // Quick add transaction dialog
    private val _showQuickAddDialog = MutableStateFlow(false)
    val showQuickAddDialog: StateFlow<Boolean> = _showQuickAddDialog.asStateFlow()

    fun setShowQuickAddDialog(show: Boolean) {
        _showQuickAddDialog.value = show
    }

    // User feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Data streams from Room
    val initialBalance: StateFlow<Double> = repository.initialBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val initialBalanceDate: StateFlow<String> = repository.initialBalanceDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val budgetItems: StateFlow<List<BudgetItem>> = repository.budgetItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCard>> = repository.creditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cardPayments: StateFlow<List<CardPayment>> = repository.cardPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current date helpers
    private val currentCal = Calendar.getInstance()
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH) // 0-indexed

    // 12-Month Outlook Forecast
    val forecast: StateFlow<List<MonthForecast>> = combine(
        initialBalance,
        budgetItems
    ) { balance, items ->
        BudgetEngine.compute12MonthForecast(balance, items, currentYear, currentMonth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected month in Overview (0 to 11)
    private val _selectedOverviewMonthIndex = MutableStateFlow(0)
    val selectedOverviewMonthIndex: StateFlow<Int> = _selectedOverviewMonthIndex.asStateFlow()

    fun selectOverviewMonth(index: Int) {
        _selectedOverviewMonthIndex.value = index
    }

    // Daily schedule for selected month in Overview ("Čo a kedy")
    val selectedDailySchedule: StateFlow<List<CashflowDayEntry>> = combine(
        forecast,
        budgetItems,
        _selectedOverviewMonthIndex
    ) { fcList, items, selIndex ->
        if (fcList.isEmpty()) return@combine emptyList()
        val targetMonth = fcList.getOrElse(selIndex) { fcList.first() }
        BudgetEngine.computeDailyScheduleForMonth(
            openingBalance = targetMonth.openingBalance,
            items = items,
            year = targetMonth.year,
            month = targetMonth.month
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected month in Actual screen ("YYYY-MM")
    private val defaultActualMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    private val _selectedActualMonth = MutableStateFlow(defaultActualMonth)
    val selectedActualMonth: StateFlow<String> = _selectedActualMonth.asStateFlow()

    fun setSelectedActualMonth(monthStr: String) {
        _selectedActualMonth.value = monthStr
    }

    // Plan vs. Actual comparison
    val planVsActual: StateFlow<List<PlanVsActualRow>> = combine(
        budgetItems,
        transactions,
        _selectedActualMonth
    ) { items, txs, monthStr ->
        val parts = monthStr.split("-").mapNotNull { it.toIntOrNull() }
        val y = parts.getOrNull(0) ?: currentYear
        val m = (parts.getOrNull(1) ?: (currentMonth + 1)) - 1
        BudgetEngine.computePlanVsActual(items, txs, y, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions filtered by selected actual month
    val actualMonthTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        _selectedActualMonth
    ) { txs, monthStr ->
        txs.filter { it.date.startsWith(monthStr) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Credit cards summary
    val cardsSummary: StateFlow<CardsExecutiveSummary> = combine(
        initialBalance,
        creditCards,
        transactions,
        cardPayments
    ) { bal, cards, txs, pays ->
        BudgetEngine.computeCardsExecutiveSummary(bal, cards, txs, pays)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CardsExecutiveSummary(0.0, 0.0, 0.0, 0.0, emptyList())
    )

    // Deficit warning (balance < 0 in any of the next 12 months)
    val deficitWarningMonth: StateFlow<String?> = forecast.combine(_selectedOverviewMonthIndex) { fcList, _ ->
        val firstNegative = fcList.firstOrNull { it.balance < -0.01 }
        firstNegative?.let { "${it.monthName}" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User Actions
    fun updateInitialBalance(balance: Double, date: String = "") {
        viewModelScope.launch {
            repository.setInitialBalance(balance, date)
            _userMessage.value = "Počiatočný zostatok bol uložený."
        }
    }

    fun addBudgetItem(
        name: String,
        type: String,
        category: String,
        amount: Double,
        frequency: String,
        startDate: String
    ) {
        if (name.isBlank() || amount <= 0) return
        viewModelScope.launch {
            val item = BudgetItem(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                category = category,
                amount = amount,
                frequency = frequency,
                startDate = startDate
            )
            repository.addBudgetItem(item)
            _userMessage.value = "Položka '${item.name}' pridaná do plánu."
        }
    }

    fun updateBudgetItem(
        id: String,
        name: String,
        type: String,
        category: String,
        amount: Double,
        frequency: String,
        startDate: String
    ) {
        if (name.isBlank() || amount <= 0) return
        viewModelScope.launch {
            val item = BudgetItem(
                id = id,
                name = name.trim(),
                type = type,
                category = category,
                amount = amount,
                frequency = frequency,
                startDate = startDate
            )
            repository.updateBudgetItem(item)
            _userMessage.value = "Položka '${item.name}' bola upravená."
        }
    }

    fun deleteBudgetItem(id: String) {
        viewModelScope.launch {
            repository.deleteBudgetItem(id)
            _userMessage.value = "Položka bola vymazaná."
        }
    }

    fun addTransaction(
        name: String,
        type: String,
        category: String,
        amount: Double,
        date: String,
        cardId: String
    ) {
        if (name.isBlank() || amount <= 0) return
        viewModelScope.launch {
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                category = category,
                amount = amount,
                date = date,
                cardId = if (type == "out") cardId else ""
            )
            repository.addTransaction(tx)
            // Auto update month selector if needed
            val mStr = if (date.length >= 7) date.substring(0, 7) else defaultActualMonth
            _selectedActualMonth.value = mStr
            _userMessage.value = "Platba '${tx.name}' zapísaná."
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addCreditCard(
        name: String,
        limit: Double,
        statementDay: Int,
        dueDay: Int,
        initialDebt: Double
    ) {
        if (name.isBlank() || limit <= 0) return
        viewModelScope.launch {
            val card = CreditCard(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                limit = limit,
                statementDay = statementDay.coerceIn(1, 31),
                dueDay = dueDay.coerceIn(1, 31),
                initialDebt = maxOf(0.0, initialDebt)
            )
            repository.addCreditCard(card)
            _userMessage.value = "Karta '${card.name}' pridaná."
        }
    }

    fun deleteCreditCard(id: String) {
        viewModelScope.launch {
            repository.deleteCreditCard(id)
            _userMessage.value = "Karta a jej záznamy zmazané."
        }
    }

    fun recordCardPayment(cardId: String, amount: Double, date: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            val payment = CardPayment(
                id = UUID.randomUUID().toString(),
                cardId = cardId,
                date = date,
                amount = amount
            )
            repository.addCardPayment(payment)
            _userMessage.value = "Splátka vo výške ${BudgetConstants.formatCurrency(amount)} zaznamenaná."
        }
    }

    fun deleteCardPayment(id: String) {
        viewModelScope.launch {
            repository.deleteCardPayment(id)
        }
    }

    /**
     * Adds the current statement repayment to the recurring plan as a one-time expense,
     * identical to "Pridať do plánu" in the HTML app.
     */
    fun addCardPaymentToPlan(cardId: String) {
        val summary = cardsSummary.value
        val cardStatus = summary.cards.firstOrNull { it.card.id == cardId } ?: return
        if (cardStatus.statementAmount <= 0.01) return

        viewModelScope.launch {
            // Find due date as YYYY-MM-DD
            val parts = cardStatus.dueDateStr.split(".").mapNotNull { it.toIntOrNull() }
            val formattedDate = if (parts.size >= 3) {
                String.format(Locale.US, "%04d-%02d-%02d", parts[2], parts[1], parts[0])
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }

            val item = BudgetItem(
                id = UUID.randomUUID().toString(),
                name = "Splátka karty ${cardStatus.card.name}",
                type = "out",
                category = "Splátky úverov",
                amount = Math.round(cardStatus.statementAmount * 100.0) / 100.0,
                frequency = "once",
                startDate = formattedDate
            )
            repository.addBudgetItem(item)
            _userMessage.value = "Splátka karty zaradená do plánu na $formattedDate."
            _currentTab.value = BudgetTab.PLAN
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoData()
            _userMessage.value = "Ukážkové dáta boli úspešne načítané."
            _currentTab.value = BudgetTab.OVERVIEW
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _userMessage.value = "Všetky dáta boli zmazané."
        }
    }

    suspend fun getExportJsonString(): String {
        return repository.exportJson()
    }

    suspend fun getExportXlsxBytes(): ByteArray {
        return repository.exportXlsxBytes()
    }

    fun importJsonData(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    _userMessage.value = "Úspešne načítaných $count položiek."
                    _currentTab.value = BudgetTab.OVERVIEW
                    onResult(true, "Úspešne načítaných $count položiek.")
                },
                onFailure = { err ->
                    onResult(false, err.localizedMessage ?: "Neplatný formát zálohy.")
                }
            )
        }
    }
}
