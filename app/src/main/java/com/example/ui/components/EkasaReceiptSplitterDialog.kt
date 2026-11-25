package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.data.model.CreditCard
import com.example.data.model.EkasaLineItem
import com.example.data.model.SavedEkasaReceipt
import com.example.data.model.SplitCategoryBatch
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkasaReceiptSplitterDialog(
    receipt: SavedEkasaReceipt,
    cards: List<CreditCard>,
    isRefetching: Boolean,
    onRefetch: () -> Unit,
    onDismiss: () -> Unit,
    onSaveBatches: (batches: List<SplitCategoryBatch>, cardId: String, date: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var dateText by remember(receipt.receiptId, receipt.issueDate) {
        mutableStateOf(receipt.issueDate.ifBlank { today })
    }
    var selectedCardId by remember { mutableStateOf("") }
    var cardExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val selectedIndices = remember(receipt.receiptId) { mutableStateListOf<Int>() }
    val draftBatches = remember(receipt.receiptId) { mutableStateListOf<SplitCategoryBatch>() }
    var showPreviouslyAssigned by remember { mutableStateOf(false) }

    // Items already persisted in Room from an earlier partial split
    val previouslyAssignedItems = remember(receipt.items) {
        receipt.items.filter { it.assignedCategory.isNotBlank() }
    }

    // Indices assigned in draft batches during this session
    val draftAssignedIndices = draftBatches.flatMap { it.itemIndices }.toSet()

    // Items still available to pick and categorize
    val availableItems = receipt.items.filter {
        it.assignedCategory.isBlank() && !draftAssignedIndices.contains(it.index)
    }

    val selectedItems = availableItems.filter { selectedIndices.contains(it.index) }
    val selectedSum = selectedItems.sumOf { it.price }
    val availableSum = availableItems.sumOf { it.price }
    val draftSum = draftBatches.sumOf { it.amount }
    val previouslyAssignedSum = previouslyAssignedItems.sumOf { it.price }

    val totalItemsCount = receipt.items.size.coerceAtLeast(1)
    val assignedCount = previouslyAssignedItems.size + draftAssignedIndices.size
    val progress = (assignedCount.toFloat() / totalItemsCount.toFloat()).coerceIn(0f, 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ekasa_splitter_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rozdelenie bločku",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${receipt.organizationName} · ${receipt.shortReceiptId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = BudgetConstants.formatCurrency(receipt.totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Summary Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Priradené: $assignedCount / ${receipt.items.size} pol.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Zostáva: ${BudgetConstants.formatCurrency(availableSum)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (availableSum > 0.01) WarningOrange else IncomeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 0.99f) IncomeGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (previouslyAssignedSum > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Skôr zaúčtované: ${BudgetConstants.formatCurrency(previouslyAssignedSum)} · V tomto rozdelení: ${BudgetConstants.formatCurrency(draftSum)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (receipt.items.isEmpty()) {
                // No line items fetched yet
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pre tento doklad zatiaľ nie sú načítané jednotlivé položky.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stiahnite si podrobný zoznam položiek priamo zo servera Finančnej správy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefetch,
                            enabled = !isRefetching,
                            modifier = Modifier.testTag("splitter_refetch_button")
                        ) {
                            if (isRefetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sťahujem položky...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Načítať položky z FS")
                            }
                        }
                    }
                }
            } else {
                // Step 1: Select items
                if (availableItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Označte položky (${availableItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    selectedIndices.clear()
                                    selectedIndices.addAll(availableItems.map { it.index })
                                }
                            ) {
                                Text("Vybrať všetko", style = MaterialTheme.typography.labelMedium)
                            }
                            if (selectedIndices.isNotEmpty()) {
                                TextButton(onClick = { selectedIndices.clear() }) {
                                    Text("Zrušiť", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            availableItems.forEachIndexed { idx, item ->
                                val isChecked = selectedIndices.contains(item.index)
                                EkasaSelectableItemRow(
                                    item = item,
                                    checked = isChecked,
                                    onToggle = {
                                        if (isChecked) {
                                            selectedIndices.remove(item.index)
                                        } else {
                                            selectedIndices.add(item.index)
                                        }
                                    }
                                )
                                if (idx < availableItems.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2: Pick category and add batch
                    Text(
                        text = "2. Zvoľte kategóriu a pridajte skupinu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (selectedCategory.isBlank()) "Vyberte kategóriu..." else selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategória pre označené položky") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("splitter_category_picker")
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BudgetConstants.ExpenseCategories.forEach { cat ->
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedItems.isNotEmpty() && selectedCategory.isNotBlank()) {
                                    draftBatches.add(
                                        SplitCategoryBatch(
                                            category = selectedCategory,
                                            itemIndices = selectedItems.map { it.index },
                                            itemNames = selectedItems.map { it.name },
                                            amount = selectedSum
                                        )
                                    )
                                    selectedIndices.clear()
                                    selectedCategory = ""
                                }
                            },
                            enabled = selectedItems.isNotEmpty() && selectedCategory.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("splitter_add_group_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedItems.isNotEmpty()) {
                                    "Pridať (${BudgetConstants.formatCurrency(selectedSum)})"
                                } else {
                                    "Pridať skupinu"
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedCategory.isNotBlank() && selectedItems.size < availableItems.size) {
                            OutlinedButton(
                                onClick = {
                                    draftBatches.add(
                                        SplitCategoryBatch(
                                            category = selectedCategory,
                                            itemIndices = availableItems.map { it.index },
                                            itemNames = availableItems.map { it.name },
                                            amount = availableSum
                                        )
                                    )
                                    selectedIndices.clear()
                                    selectedCategory = ""
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("splitter_add_rest_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Zvyšok (${BudgetConstants.formatCurrency(availableSum)})",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = IncomeGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Všetky položky z bločku sú priradené do kategórií.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                    }
                }

                // Step 3: Draft category batches
                AnimatedVisibility(visible = draftBatches.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Text(
                            text = "Pripravené platby podľa kategórií (${draftBatches.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        draftBatches.forEachIndexed { batchIdx, batch ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("splitter_batch_$batchIdx")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = batch.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${batch.itemIndices.size} pol.: ${batch.itemNames.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = BudgetConstants.formatCurrency(batch.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    IconButton(
                                        onClick = { draftBatches.removeAt(batchIdx) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Odstrániť skupinu",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Previously assigned items (for PARTIAL / FULL receipts)
                if (previouslyAssignedItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showPreviouslyAssigned = !showPreviouslyAssigned },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showPreviouslyAssigned) {
                                "Skryť už zaúčtované položky (${previouslyAssignedItems.size})"
                            } else {
                                "Zobraziť už zaúčtované položky (${previouslyAssignedItems.size})"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    AnimatedVisibility(visible = showPreviouslyAssigned) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            previouslyAssignedItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.name} (${item.assignedCategory})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = BudgetConstants.formatCurrency(item.price),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date & Optional Card selector for created transactions
            ClickableDateField(
                label = "Dátum platieb",
                dateValue = dateText,
                onDateSelected = { dateText = it },
                modifier = Modifier.fillMaxWidth(),
                testTag = "splitter_date_input"
            )

            if (cards.isNotEmpty()) {
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
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("splitter_card_picker")
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

            Spacer(modifier = Modifier.height(18.dp))

            // Footer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("splitter_cancel_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Zavrieť")
                }

                Button(
                    onClick = {
                        onSaveBatches(draftBatches.toList(), selectedCardId, dateText)
                    },
                    enabled = draftBatches.isNotEmpty() && draftSum > 0.0,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp)
                        .testTag("splitter_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (draftBatches.isEmpty()) {
                            "Uložiť platby"
                        } else {
                            "Uložiť (${draftBatches.size}) · ${BudgetConstants.formatCurrency(draftSum)}"
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EkasaSelectableItemRow(
    item: EkasaLineItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("splitter_item_${item.index}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.quantity != 1.0) {
                val qtyStr = if (item.quantity % 1.0 == 0.0) {
                    "${item.quantity.toInt()} ks"
                } else {
                    String.format(Locale.US, "%.2f j.", item.quantity)
                }
                Text(
                    text = qtyStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = BudgetConstants.formatCurrency(item.price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.price < 0) IncomeGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
