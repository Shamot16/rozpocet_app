package com.example.ui.plan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.data.model.BudgetItem
import com.example.ui.components.ClickableDateField
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    items: List<BudgetItem>,
    onAddItem: (name: String, type: String, category: String, amount: Double, frequency: String, startDate: String) -> Unit,
    onUpdateItem: (id: String, name: String, type: String, category: String, amount: Double, frequency: String, startDate: String) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<BudgetItem?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Primary Action Button to open the "Nová plánovaná položka" form
        item {
            Button(
                onClick = {
                    itemToEdit = null
                    showSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("plan_open_form_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nová plánovaná alebo pravidelná položka",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Section Title
        item {
            Text(
                text = "Zoznam naplánovaných položiek (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 3. Items List
        if (items.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Zatiaľ nemáte pridané žiadne pravidelné položky. Kliknite na tlačidlo vyššie pre pridanie prvej položky do rozpočtu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_item_${item.id}")
                        .clickable {
                            itemToEdit = item
                            showSheet = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.category} · ${BudgetConstants.FrequencyLabels[item.frequency] ?: item.frequency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "od ${item.startDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = (if (item.type == "in") "+" else "−") + BudgetConstants.formatCurrency(item.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.type == "in") IncomeGreen else ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    itemToEdit = item
                                    showSheet = true
                                },
                                modifier = Modifier.testTag("edit_item_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Upraviť položku",
                                    tint = MaterialTheme.colorScheme.primary
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

    // Modal Sheet for Adding or Editing a Plan Item
    if (showSheet) {
        PlanItemBottomSheet(
            item = itemToEdit,
            onDismiss = {
                showSheet = false
                itemToEdit = null
            },
            onSave = { name, type, category, amount, frequency, startDate ->
                val current = itemToEdit
                if (current != null) {
                    onUpdateItem(current.id, name, type, category, amount, frequency, startDate)
                } else {
                    onAddItem(name, type, category, amount, frequency, startDate)
                }
                showSheet = false
                itemToEdit = null
            },
            onDelete = { id ->
                onDeleteItem(id)
                showSheet = false
                itemToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanItemBottomSheet(
    item: BudgetItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, category: String, amount: Double, frequency: String, startDate: String) -> Unit,
    onDelete: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var name by remember(item) { mutableStateOf(item?.name ?: "") }
    var type by remember(item) { mutableStateOf(item?.type ?: "out") }
    var category by remember(item) {
        mutableStateOf(
            item?.category ?: if (type == "out") BudgetConstants.ExpenseCategories.first() else BudgetConstants.IncomeCategories.first()
        )
    }
    var amountText by remember(item) {
        mutableStateOf(
            if (item != null) {
                if (item.amount % 1.0 == 0.0) item.amount.toLong().toString() else item.amount.toString()
            } else ""
        )
    }
    var frequency by remember(item) { mutableStateOf(item?.frequency ?: "monthly") }
    var startDate by remember(item) { mutableStateOf(item?.startDate ?: today) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val categories = if (type == "out") BudgetConstants.ExpenseCategories else BudgetConstants.IncomeCategories

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("plan_item_bottom_sheet")
        ) {
            Text(
                text = if (item != null) "Upraviť plánovanú položku" else "Nová plánovaná položka",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = type == "out",
                    onClick = {
                        type = "out"
                        if (category !in BudgetConstants.ExpenseCategories) {
                            category = BudgetConstants.ExpenseCategories.first()
                        }
                    },
                    label = { Text("Výdavok (−)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        selectedLabelColor = ExpenseRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plan_type_out")
                )
                FilterChip(
                    selected = type == "in",
                    onClick = {
                        type = "in"
                        if (category !in BudgetConstants.IncomeCategories) {
                            category = BudgetConstants.IncomeCategories.first()
                        }
                    },
                    label = { Text("Príjem (+)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        selectedLabelColor = IncomeGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plan_type_in")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Názov (napr. Nájom, Výplata)") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text("Suma v €") },
                leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategória") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val freqLabel = BudgetConstants.Frequencies.firstOrNull { it.first == frequency }?.second ?: frequency
                OutlinedTextField(
                    value = freqLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frekvencia opakovania") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    BudgetConstants.Frequencies.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                frequency = key
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start date picker
            ClickableDateField(
                label = "Prvý dátum platby",
                dateValue = startDate,
                onDateSelected = { startDate = it },
                modifier = Modifier.fillMaxWidth(),
                testTag = "plan_date_input"
            )

            Text(
                text = "Dátum = prvá platba. Ďalšie sa prepočítajú podľa zvolenej frekvencie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = name.isNotBlank() && parsedAmount > 0

            // Action Buttons
            if (item != null) {
                // Editing existing item: Save Changes + Delete + Cancel
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isValid) {
                                onSave(name, type, category, parsedAmount, frequency, startDate)
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("plan_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uložiť zmeny", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("plan_delete_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zmazať položku", fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_cancel_button")
                    ) {
                        Text("Zrušiť")
                    }
                }
            } else {
                // Adding new item: Cancel + Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("plan_cancel_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Zrušiť")
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                onSave(name, type, category, parsedAmount, frequency, startDate)
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("plan_add_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pridať do plánu", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation dialog before deleting
    if (showDeleteConfirmDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Vymazať položku?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Naozaj chcete vymazať plánovanú položku '${item.name}' zo zoznamu?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(item.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.testTag("plan_confirm_delete_button")
                ) {
                    Text("Vymazať")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("plan_cancel_delete_button")
                ) {
                    Text("Zrušiť")
                }
            }
        )
    }
}
