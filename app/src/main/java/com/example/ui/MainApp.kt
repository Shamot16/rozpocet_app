package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.actual.ActualScreen
import com.example.ui.cards.CardsScreen
import com.example.ui.components.EkasaReceiptSplitterDialog
import com.example.ui.components.QuickAddTransactionDialog
import com.example.ui.overview.OverviewScreen
import com.example.ui.plan.PlanScreen
import com.example.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val initialBalance by viewModel.initialBalance.collectAsStateWithLifecycle()
    val initialBalanceDate by viewModel.initialBalanceDate.collectAsStateWithLifecycle()
    val forecast by viewModel.forecast.collectAsStateWithLifecycle()
    val selectedMonthIndex by viewModel.selectedOverviewMonthIndex.collectAsStateWithLifecycle()
    val dailySchedule by viewModel.selectedDailySchedule.collectAsStateWithLifecycle()
    val cardsSummary by viewModel.cardsSummary.collectAsStateWithLifecycle()
    val deficitWarning by viewModel.deficitWarningMonth.collectAsStateWithLifecycle()
    val budgetItems by viewModel.budgetItems.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    val cardPayments by viewModel.cardPayments.collectAsStateWithLifecycle()
    val selectedActualMonth by viewModel.selectedActualMonth.collectAsStateWithLifecycle()
    val planVsActual by viewModel.planVsActual.collectAsStateWithLifecycle()
    val actualTransactions by viewModel.actualMonthTransactions.collectAsStateWithLifecycle()
    val ekasaReceipts by viewModel.ekasaReceipts.collectAsStateWithLifecycle()
    val receiptToSplit by viewModel.receiptToSplit.collectAsStateWithLifecycle()
    val showQuickAdd by viewModel.showQuickAddDialog.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var isRefetchingInSplitter by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Display user messages via Snackbar
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // BackHandler: if on secondary tab, back button returns to Overview
    BackHandler(enabled = currentTab != BudgetTab.OVERVIEW) {
        viewModel.selectTab(BudgetTab.OVERVIEW)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rodinný rozpočet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == BudgetTab.OVERVIEW,
                    onClick = { viewModel.selectTab(BudgetTab.OVERVIEW) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Prehľad") },
                    label = { Text("Prehľad") },
                    modifier = Modifier.testTag("nav_tab_overview")
                )
                NavigationBarItem(
                    selected = currentTab == BudgetTab.PLAN,
                    onClick = { viewModel.selectTab(BudgetTab.PLAN) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Pravidelné") },
                    label = { Text("Pravidelné") },
                    modifier = Modifier.testTag("nav_tab_plan")
                )
                NavigationBarItem(
                    selected = currentTab == BudgetTab.ACTUAL,
                    onClick = { viewModel.selectTab(BudgetTab.ACTUAL) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Skutočnosť") },
                    label = { Text("Skutočnosť") },
                    modifier = Modifier.testTag("nav_tab_actual")
                )
                NavigationBarItem(
                    selected = currentTab == BudgetTab.CARDS,
                    onClick = { viewModel.selectTab(BudgetTab.CARDS) },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Karty") },
                    label = { Text("Karty") },
                    modifier = Modifier.testTag("nav_tab_cards")
                )
                NavigationBarItem(
                    selected = currentTab == BudgetTab.SETTINGS,
                    onClick = { viewModel.selectTab(BudgetTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Nastavenia") },
                    label = { Text("Nastavenia") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowQuickAddDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_quick_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Pridať platbu")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BudgetTab.OVERVIEW -> {
                    OverviewScreen(
                        initialBalance = initialBalance,
                        initialBalanceDate = initialBalanceDate,
                        forecast = forecast,
                        selectedMonthIndex = selectedMonthIndex,
                        dailySchedule = dailySchedule,
                        cardsSummary = cardsSummary,
                        deficitWarning = deficitWarning,
                        onNavigateToSettings = { viewModel.selectTab(BudgetTab.SETTINGS) },
                        onSelectMonthIndex = { viewModel.selectOverviewMonth(it) },
                        onNavigateToCards = { viewModel.selectTab(BudgetTab.CARDS) }
                    )
                }

                BudgetTab.PLAN -> {
                    PlanScreen(
                        items = budgetItems,
                        onAddItem = { n, t, c, a, f, d -> viewModel.addBudgetItem(n, t, c, a, f, d) },
                        onUpdateItem = { id, n, t, c, a, f, d -> viewModel.updateBudgetItem(id, n, t, c, a, f, d) },
                        onDeleteItem = { viewModel.deleteBudgetItem(it) }
                    )
                }

                BudgetTab.ACTUAL -> {
                    ActualScreen(
                        cards = creditCards,
                        selectedMonth = selectedActualMonth,
                        planVsActual = planVsActual,
                        transactions = actualTransactions,
                        ekasaReceipts = ekasaReceipts,
                        onSelectMonth = { viewModel.setSelectedActualMonth(it) },
                        onOpenAddTransaction = { viewModel.setShowQuickAddDialog(true) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onRefetchEkasaReceipt = { id, cb -> viewModel.refetchEkasaReceipt(id, cb) },
                        onOpenEkasaSplitter = { rec -> viewModel.openReceiptSplitter(rec) },
                        onDeleteEkasaReceipt = { id -> viewModel.deleteEkasaReceipt(id) }
                    )
                }

                BudgetTab.CARDS -> {
                    CardsScreen(
                        summary = cardsSummary,
                        payments = cardPayments,
                        onAddCard = { n, l, s, d, d0 -> viewModel.addCreditCard(n, l, s, d, d0) },
                        onDeleteCard = { viewModel.deleteCreditCard(it) },
                        onRecordPayment = { id, a, d -> viewModel.recordCardPayment(id, a, d) },
                        onDeletePayment = { viewModel.deleteCardPayment(it) },
                        onAddCardToPlan = { viewModel.addCardPaymentToPlan(it) }
                    )
                }

                BudgetTab.SETTINGS -> {
                    SettingsScreen(
                        initialBalance = initialBalance,
                        initialBalanceDate = initialBalanceDate,
                        onSaveInitialBalance = { bal, date -> viewModel.updateInitialBalance(bal, date) },
                        onExportJson = { viewModel.getExportJsonString() },
                        onExportExcel = { viewModel.getExportXlsxBytes() },
                        onImportJson = { json, cb -> viewModel.importJsonData(json, cb) },
                        onLoadDemo = { viewModel.loadDemoData() },
                        onClearAll = { viewModel.clearAllData() }
                    )
                }
            }

            // Quick Add Transaction Sheet
            if (showQuickAdd) {
                QuickAddTransactionDialog(
                    cards = creditCards,
                    onDismiss = { viewModel.setShowQuickAddDialog(false) },
                    onSave = { n, t, c, a, d, k, rid ->
                        viewModel.addTransaction(n, t, c, a, d, k, rid)
                    },
                    onRegisterScannedReceipt = { rid ->
                        viewModel.registerScannedReceipt(rid)
                    },
                    onFetchAndSaveReceipt = { rid ->
                        viewModel.fetchAndSaveEkasaReceipt(rid)
                    },
                    onOpenSplitter = { rec ->
                        viewModel.openReceiptSplitter(rec)
                    }
                )
            }

            // Multi-category eKasa Receipt Item Splitter Sheet
            receiptToSplit?.let { rec ->
                EkasaReceiptSplitterDialog(
                    receipt = rec,
                    cards = creditCards,
                    isRefetching = isRefetchingInSplitter,
                    onRefetch = {
                        isRefetchingInSplitter = true
                        viewModel.refetchEkasaReceipt(rec.receiptId) {
                            isRefetchingInSplitter = false
                        }
                    },
                    onDismiss = { viewModel.openReceiptSplitter(null) },
                    onSaveBatches = { batches, cardId, date ->
                        viewModel.saveSplitEkasaBatches(rec, batches, cardId, date)
                    }
                )
            }
        }
    }
}
