package com.example.ui.actual

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.data.model.CreditCard
import com.example.data.model.PlanVsActualRow
import com.example.data.model.Transaction
import com.example.data.remote.EkasaReceiptService
import com.example.ui.components.QrCodeScannerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActualScreen(
    cards: List<CreditCard>,
    selectedMonth: String,
    planVsActual: List<PlanVsActualRow>,
    transactions: List<Transaction>,
    onSelectMonth: (String) -> Unit,
    onAddTransaction: (name: String, type: String, category: String, amount: Double, date: String, cardId: String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("out") }
    var category by remember { mutableStateOf(BudgetConstants.ExpenseCategories.first()) }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(today) }
    var selectedCardId by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }

    // eKasa scanner & loading states
    var showScanner by remember { mutableStateOf(false) }
    var isLoadingReceipt by remember { mutableStateOf(false) }
    var scannerErrorMessage by remember { mutableStateOf<String?>(null) }

    val categories = if (type == "out") BudgetConstants.ExpenseCategories else BudgetConstants.IncomeCategories

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("actual_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Add Transaction Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_transaction_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Zapísať skutočnú platbu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scan eKasa button
                    OutlinedButton(
                        onClick = {
                            scannerErrorMessage = null
                            showScanner = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_scan_ekasa_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Naskenovať eKasa bloček", fontWeight = FontWeight.SemiBold)
                    }

                    // Loading indicator for eKasa request
                    if (isLoadingReceipt) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sťahujem údaje zo servera Finančnej správy...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Error indicator
                    scannerErrorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ExpenseRed.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Type Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = type == "out",
                            onClick = {
                                type = "out"
                                if (!BudgetConstants.ExpenseCategories.contains(category)) {
                                    category = ""
                                }
                            },
                            label = { Text("Výdavok (−)", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                                selectedLabelColor = ExpenseRed
                            ),
                            modifier = Modifier.weight(1f).testTag("actual_type_out")
                        )
                        FilterChip(
                            selected = type == "in",
                            onClick = {
                                type = "in"
                                if (!BudgetConstants.IncomeCategories.contains(category)) {
                                    category = ""
                                }
                                selectedCardId = ""
                            },
                            label = { Text("Príjem (+)", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                                selectedLabelColor = IncomeGreen
                            ),
                            modifier = Modifier.weight(1f).testTag("actual_type_in")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Popis (napr. Tesco, Čerpacia stanica)") },
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.replace(',', '.') },
                        label = { Text("Suma v €") },
                        leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (category.isBlank()) "Vyberte kategóriu..." else category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategória *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Dátum (RRRR-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_date_input")
                    )

                    // Credit card choice for expenses
                    if (type == "out" && cards.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ExposedDropdownMenuBox(
                            expanded = cardExpanded,
                            onExpandedChange = { cardExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val cardLabel = if (selectedCardId.isEmpty()) "Bez karty (bežný účet / hotovosť)"
                            else "💳 " + (cards.firstOrNull { it.id == selectedCardId }?.name ?: "")
                            OutlinedTextField(
                                value = cardLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Karta") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = cardExpanded,
                                onDismissRequest = { cardExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bez karty (bežný účet / hotovosť)") },
                                    onClick = {
                                        selectedCardId = ""
                                        cardExpanded = false
                                    }
                                )
                                cards.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("💳 ${c.name}") },
                                        onClick = {
                                            selectedCardId = c.id
                                            cardExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val finalCat = if (category.isBlank()) {
                                if (type == "out") BudgetConstants.ExpenseCategories.first() else BudgetConstants.IncomeCategories.first()
                            } else category

                            if (name.isNotBlank() && amt > 0) {
                                onAddTransaction(name, type, finalCat, amt, dateText, selectedCardId)
                                name = ""
                                amountText = ""
                            }
                        },
                        enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && category.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Pridať platbu")
                    }
                }
            }
        }

        // 2. Month Filter Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sledovaný mesiac:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = onSelectMonth,
                        label = { Text("RRRR-MM") },
                        singleLine = true,
                        modifier = Modifier
                            .size(width = 140.dp, height = 56.dp)
                            .testTag("actual_month_input")
                    )
                }
            }
        }

        // 3. Plan vs. Actual Table
        item {
            Text(
                text = "Plán vs. skutočnosť za mesiac $selectedMonth",
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
                    // Header
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
                                    text = BudgetConstants.formatCurrencyNoDecimals(row.planned),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = BudgetConstants.formatCurrencyNoDecimals(row.actual),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val diffText = (if (row.diff > 0) "+" else "") + BudgetConstants.formatCurrencyNoDecimals(row.diff)
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

        // 4. Actual Transactions List
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
                                text = "${tx.category} · ${tx.date}" + (if (!cardName.isNullOrEmpty()) " · $cardName" else ""),
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

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Camera scanner dialog
    if (showScanner) {
        QrCodeScannerDialog(
            onDismiss = { showScanner = false },
            onReceiptIdFound = { receiptId ->
                showScanner = false
                isLoadingReceipt = true
                scannerErrorMessage = null
                coroutineScope.launch {
                    val result = EkasaReceiptService.fetchReceipt(receiptId)
                    result.fold(
                        onSuccess = { data ->
                            name = data.organizationName
                            amountText = if (data.totalPrice > 0) {
                                String.format(Locale.US, "%.2f", data.totalPrice)
                            } else ""
                            dateText = data.formattedDate
                            type = "out"
                            category = "" // User chooses category as requested
                            isLoadingReceipt = false
                        },
                        onFailure = { error ->
                            isLoadingReceipt = false
                            scannerErrorMessage = "Chyba overenia bločku: ${error.localizedMessage}"
                        }
                    )
                }
            }
        )
    }
}
