package com.example.ui.actual

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.data.model.CreditCard
import com.example.data.model.EkasaAllocationStatus
import com.example.data.model.PlanVsActualRow
import com.example.data.model.SavedEkasaReceipt
import com.example.data.model.Transaction
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.formatYearMonthSlovak
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Calendar
import java.util.Locale

@Composable
fun ActualScreen(
    cards: List<CreditCard>,
    selectedMonth: String,
    planVsActual: List<PlanVsActualRow>,
    transactions: List<Transaction>,
    ekasaReceipts: List<SavedEkasaReceipt>,
    onSelectMonth: (String) -> Unit,
    onOpenAddTransaction: () -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onRefetchEkasaReceipt: (String, (Boolean) -> Unit) -> Unit,
    onOpenEkasaSplitter: (SavedEkasaReceipt) -> Unit,
    onDeleteEkasaReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var activeSection by remember { mutableIntStateOf(0) } // 0 = Platby, 1 = História bločkov
    var refetchingIds by remember { mutableStateOf(setOf<String>()) }
    var receiptToConfirmDelete by remember { mutableStateOf<SavedEkasaReceipt?>(null) }
    var fullDuplicateToConfirmOpen by remember { mutableStateOf<SavedEkasaReceipt?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("actual_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Button to open the "Zapísať skutočnú platbu" modal
        item {
            Button(
                onClick = onOpenAddTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("actual_open_form_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zapísať skutočnú platbu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Segmented Toggle: Platby vs História bločkov
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("actual_section_toggle")
            ) {
                SegmentedButton(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("actual_tab_payments")
                ) {
                    Text("Platby (${transactions.size})", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("actual_tab_receipts")
                ) {
                    Text("História bločkov (${ekasaReceipts.size})", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (activeSection == 0) {
            // 3. Month Filter Selector with Name and Year & Calendar Picker
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actual_month_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val parts = selectedMonth.split("-").mapNotNull { it.toIntOrNull() }
                                if (parts.size >= 2) {
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, parts[0])
                                        set(Calendar.MONTH, parts[1] - 1)
                                        add(Calendar.MONTH, -1)
                                    }
                                    val newYm = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                                    onSelectMonth(newYm)
                                }
                            },
                            modifier = Modifier.testTag("month_prev_arrow")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Predchádzajúci mesiac")
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthYearPicker = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("actual_month_picker_trigger"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Sledovaný mesiac",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatYearMonthSlovak(selectedMonth).replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val parts = selectedMonth.split("-").mapNotNull { it.toIntOrNull() }
                                if (parts.size >= 2) {
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, parts[0])
                                        set(Calendar.MONTH, parts[1] - 1)
                                        add(Calendar.MONTH, 1)
                                    }
                                    val newYm = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                                    onSelectMonth(newYm)
                                }
                            },
                            modifier = Modifier.testTag("month_next_arrow")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Nasledujúci mesiac")
                        }
                    }
                }
            }

            // 4. Plan vs. Actual Table
            item {
                Text(
                    text = "Plán vs. skutočnosť: ${formatYearMonthSlovak(selectedMonth)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_vs_actual_card")
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Kategória",
                                modifier = Modifier.weight(1.4f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Plán",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Skutočnosť",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Rozdiel",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (planVsActual.isEmpty()) {
                            Text(
                                text = "Za tento mesiac nie sú žiadne plánované ani skutočné záznamy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            planVsActual.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text(
                                            text = row.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (row.type == "in") {
                                            Text(
                                                text = "(príjem)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = BudgetConstants.formatCurrency(row.planned),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = BudgetConstants.formatCurrency(row.actual),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val diffText = (if (row.diff > 0) "+" else "") + BudgetConstants.formatCurrency(row.diff)
                                    val diffColor = when {
                                        Math.abs(row.diff) < 0.01 -> MaterialTheme.colorScheme.onSurfaceVariant
                                        row.isBad -> ExpenseRed
                                        else -> IncomeGreen
                                    }
                                    Text(
                                        text = diffText,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = diffColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Actual Transactions List
            item {
                Text(
                    text = "Zoznam skutočných platieb (${transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "V tomto mesiaci zatiaľ nie sú zapísané žiadne platby.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val cardName = cards.firstOrNull { it.id == tx.cardId }?.name
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_tx_${tx.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!cardName.isNullOrEmpty()) {
                                        Text(
                                            text = "💳 ",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = tx.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${tx.category} · ${tx.date}" +
                                        (if (!cardName.isNullOrEmpty()) " · $cardName" else "") +
                                        (if (tx.ekasaReceiptId.isNotBlank()) " · eKasa" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (if (tx.type == "in") "+" else "−") + BudgetConstants.formatCurrency(tx.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "in") IncomeGreen else ExpenseRed
                                )
                                IconButton(
                                    onClick = { onDeleteTransaction(tx.id) },
                                    modifier = Modifier.testTag("delete_tx_${tx.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Zmazať platbu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Section 1: História eKasa bločkov
            item {
                Text(
                    text = "História naskenovaných eKasa bločkov (${ekasaReceipts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ekasaReceipts.isEmpty()) {
                item {
                    EkasaReceiptHistoryEmptyCard()
                }
            } else {
                items(ekasaReceipts, key = { it.receiptId }) { receipt ->
                    EkasaReceiptHistoryCard(
                        receipt = receipt,
                        isRefetching = refetchingIds.contains(receipt.receiptId),
                        onRefetch = {
                            refetchingIds = refetchingIds + receipt.receiptId
                            onRefetchEkasaReceipt(receipt.receiptId) { _ ->
                                refetchingIds = refetchingIds - receipt.receiptId
                            }
                        },
                        onOpenSplitter = {
                            if (receipt.allocationStatus == EkasaAllocationStatus.FULL) {
                                fullDuplicateToConfirmOpen = receipt
                            } else {
                                onOpenEkasaSplitter(receipt)
                            }
                        },
                        onDelete = {
                            receiptToConfirmDelete = receipt
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Month-Year Picker Dialog
    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentYearMonth = selectedMonth,
            onYearMonthSelected = { newYm ->
                onSelectMonth(newYm)
            },
            onDismiss = { showMonthYearPicker = false }
        )
    }

    // Confirmation dialog before deleting a receipt from history
    receiptToConfirmDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { receiptToConfirmDelete = null },
            title = { Text("Odstrániť bloček z histórie?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Doklad '${rec.organizationName}' (${rec.shortReceiptId}) sa odstráni z histórie bločkov. Už zapísané platby zostanú zachované.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEkasaReceipt(rec.receiptId)
                        receiptToConfirmDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Odstrániť")
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToConfirmDelete = null }) {
                    Text("Zrušiť")
                }
            }
        )
    }

    // Duplicate warning when opening an already FULL receipt from history
    fullDuplicateToConfirmOpen?.let { rec ->
        AlertDialog(
            onDismissRequest = { fullDuplicateToConfirmOpen = null },
            title = { Text("Doklad už bol plne uložený", fontWeight = FontWeight.Bold) },
            text = {
                Text("Tento eKasa doklad (${rec.organizationName}, ${BudgetConstants.formatCurrency(rec.totalPrice)}) už bol predtým plne zaúčtovaný. Chcete ho napriek tomu otvoriť?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = rec
                        fullDuplicateToConfirmOpen = null
                        onOpenEkasaSplitter(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Napriek tomu otvoriť")
                }
            },
            dismissButton = {
                TextButton(onClick = { fullDuplicateToConfirmOpen = null }) {
                    Text("Zrušiť")
                }
            }
        )
    }
}
