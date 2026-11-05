package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetConstants
import com.example.data.model.CreditCard
import com.example.data.remote.EkasaReceiptService
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionDialog(
    cards: List<CreditCard>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, category: String, amount: Double, date: String, cardId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("out") } // "out" or "in"
    var selectedCategory by remember { mutableStateOf(BudgetConstants.ExpenseCategories.first()) }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var selectedCardId by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var cardExpanded by remember { mutableStateOf(false) }

    // Scanner & eKasa loading state
    var showScanner by remember { mutableStateOf(false) }
    var isLoadingReceipt by remember { mutableStateOf(false) }
    var scannerErrorMessage by remember { mutableStateOf<String?>(null) }

    val categories = if (type == "out") BudgetConstants.ExpenseCategories else BudgetConstants.IncomeCategories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("quick_add_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Záznam platby",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scanner button
            OutlinedButton(
                onClick = {
                    scannerErrorMessage = null
                    showScanner = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_ekasa_button"),
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

            // Loading indicator for eKasa API request
            if (isLoadingReceipt) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Sťahujem údaje zo servera Finančnej správy...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error notice
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Type selector: Výdavok vs Príjem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = type == "out",
                    onClick = {
                        type = "out"
                        if (!BudgetConstants.ExpenseCategories.contains(selectedCategory)) {
                            selectedCategory = ""
                        }
                    },
                    label = { Text("Výdavok (−)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        selectedLabelColor = ExpenseRed
                    ),
                    modifier = Modifier.weight(1f).testTag("quick_add_chip_expense")
                )
                FilterChip(
                    selected = type == "in",
                    onClick = {
                        type = "in"
                        if (!BudgetConstants.IncomeCategories.contains(selectedCategory)) {
                            selectedCategory = ""
                        }
                        selectedCardId = ""
                    },
                    label = { Text("Príjem (+)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        selectedLabelColor = IncomeGreen
                    ),
                    modifier = Modifier.weight(1f).testTag("quick_add_chip_income")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Popis (napr. Tesco, Nájom, Benzín)") },
                leadingIcon = {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text("Suma v €") },
                leadingIcon = {
                    Icon(Icons.Default.Euro, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedCategory.isBlank()) "Vyberte kategóriu..." else selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategória *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("quick_add_category_picker")
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date picker
            ClickableDateField(
                label = "Dátum platby",
                dateValue = dateText,
                onDateSelected = { dateText = it },
                modifier = Modifier.fillMaxWidth(),
                testTag = "quick_add_date_input"
            )

            // Credit Card selector (only for expenses if cards exist)
            if (type == "out" && cards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ExposedDropdownMenuBox(
                    expanded = cardExpanded,
                    onExpandedChange = { cardExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cardLabel = if (selectedCardId.isEmpty()) {
                        "Bez karty (bežný účet / hotovosť)"
                    } else {
                        "💳 " + (cards.firstOrNull { it.id == selectedCardId }?.name ?: "")
                    }
                    OutlinedTextField(
                        value = cardLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Platba kartou") },
                        leadingIcon = {
                            Icon(Icons.Default.CreditCard, contentDescription = null)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("quick_add_card_picker")
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

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("quick_add_cancel_button")
                ) {
                    Text("Zrušiť")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val finalCat = if (selectedCategory.isBlank()) {
                            if (type == "out") BudgetConstants.ExpenseCategories.first() else BudgetConstants.IncomeCategories.first()
                        } else selectedCategory

                        if (name.isNotBlank() && amt > 0) {
                            onSave(name, type, finalCat, amt, dateText, selectedCardId)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && selectedCategory.isNotBlank(),
                    modifier = Modifier.testTag("quick_add_save_button")
                ) {
                    Text("Uložiť platbu")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
                            selectedCategory = "" // User chooses category as requested
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
