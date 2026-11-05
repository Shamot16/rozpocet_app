package com.example.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onDeleteItem: (String) -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("out") }
    var category by remember { mutableStateOf(BudgetConstants.ExpenseCategories.first()) }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("monthly") }
    var startDate by remember { mutableStateOf(today) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    val categories = if (type == "out") BudgetConstants.ExpenseCategories else BudgetConstants.IncomeCategories

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_item_form_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Nová plánovaná alebo pravidelná položka",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

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
                                category = BudgetConstants.ExpenseCategories.first()
                            },
                            label = { Text("Výdavok (−)", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                                selectedLabelColor = ExpenseRed
                            ),
                            modifier = Modifier.weight(1f).testTag("plan_type_out")
                        )
                        FilterChip(
                            selected = type == "in",
                            onClick = {
                                type = "in"
                                category = BudgetConstants.IncomeCategories.first()
                            },
                            label = { Text("Príjem (+)", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                                selectedLabelColor = IncomeGreen
                            ),
                            modifier = Modifier.weight(1f).testTag("plan_type_in")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Start date
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Prvý dátum (RRRR-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_date_input")
                    )

                    Text(
                        text = "Dátum = prvá (alebo jediná) platba. Ďalšie sa prepočítajú podľa zvolenej frekvencie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank() && amt > 0) {
                                    onAddItem(name, type, category, amt, frequency, startDate)
                                    name = ""
                                    amountText = ""
                                }
                            },
                            enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier.weight(1f).testTag("plan_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Pridať do plánu")
                        }

                        OutlinedButton(
                            onClick = onLoadDemo,
                            modifier = Modifier.testTag("plan_demo_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text("Ukážka")
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Zoznam naplánovaných položiek (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Items List
        if (items.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Zatiaľ nemáte pridané žiadne pravidelné položky. Vyplňte formulár vyššie alebo kliknite na 'Ukážka' pre načítanie vzorových dát.",
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
                            IconButton(
                                onClick = { onDeleteItem(item.id) },
                                modifier = Modifier.testTag("delete_item_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Zmazať",
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
}
