package com.example.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.data.model.CardPayment
import com.example.data.model.CardStatus
import com.example.data.model.CardsExecutiveSummary
import com.example.data.model.CreditCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CardsScreen(
    summary: CardsExecutiveSummary,
    payments: List<CardPayment>,
    onAddCard: (name: String, limit: Double, stmt: Int, due: Int, debt0: Double) -> Unit,
    onDeleteCard: (String) -> Unit,
    onRecordPayment: (cardId: String, amount: Double, date: String) -> Unit,
    onDeletePayment: (String) -> Unit,
    onAddCardToPlan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    // New card form states
    var cardName by remember { mutableStateOf("") }
    var cardLimitText by remember { mutableStateOf("") }
    var stmtDayText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("") }
    var debt0Text by remember { mutableStateOf("") }

    // Confirmation dialog for card deletion
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("cards_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Executive Total Debt Card
        if (summary.cards.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cards_total_debt_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Dlh na kartách spolu",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = BudgetConstants.formatCurrency(summary.totalDebt),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.totalDebt > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Zostatok z Prehľadu − dlh na kartách: ${BudgetConstants.formatCurrency(summary.netCashBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (summary.netCashBalance < 0) ExpenseRed else IncomeGreen
                        )

                        Text(
                            text = "Celkový voľný limit: ${BudgetConstants.formatCurrency(summary.totalAvailableLimit)} z ${BudgetConstants.formatCurrency(summary.totalLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Individual Cards List
        items(summary.cards, key = { it.card.id }) { status ->
            CreditCardItemCard(
                status = status,
                today = today,
                onDelete = { cardToDelete = status.card },
                onRecordPayment = { amt -> onRecordPayment(status.card.id, amt, today) },
                onAddToPlan = { onAddCardToPlan(status.card.id) }
            )
        }

        // 3. New Card Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_card_form")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Nová kreditná karta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text("Názov karty (napr. Visa Gold, Mastercard)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_card_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cardLimitText,
                        onValueChange = { cardLimitText = it.replace(',', '.') },
                        label = { Text("Úverový limit v €") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_card_limit_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stmtDayText,
                            onValueChange = { stmtDayText = it },
                            label = { Text("Deň výpisu (1–31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_card_stmt_input")
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { dueDayText = it },
                            label = { Text("Deň splatnosti (1–31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_card_due_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = debt0Text,
                        onValueChange = { debt0Text = it.replace(',', '.') },
                        label = { Text("Súčasný dlh v € (ak existuje)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_card_debt0_input")
                    )

                    Text(
                        text = "Nákupy kartou zapisujte v záložke Skutočnosť výberom tejto karty. Dlh = počiatočný dlh + nákupy − splátky.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Button(
                        onClick = {
                            val limit = cardLimitText.toDoubleOrNull() ?: 0.0
                            val stmt = stmtDayText.toIntOrNull() ?: 1
                            val due = dueDayText.toIntOrNull() ?: 1
                            val debt0 = debt0Text.toDoubleOrNull() ?: 0.0
                            if (cardName.isNotBlank() && limit > 0) {
                                onAddCard(cardName, limit, stmt, due, debt0)
                                cardName = ""
                                cardLimitText = ""
                                stmtDayText = ""
                                dueDayText = ""
                                debt0Text = ""
                            }
                        },
                        enabled = cardName.isNotBlank() && (cardLimitText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.fillMaxWidth().testTag("add_card_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Pridať kartu")
                    }
                }
            }
        }

        // 4. Recent Repayments List
        if (payments.isNotEmpty()) {
            item {
                Text(
                    text = "Posledné splátky kariet (${payments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(payments, key = { it.id }) { p ->
                val cardName = summary.cards.firstOrNull { it.card.id == p.cardId }?.card?.name ?: "Karta"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Splátka · $cardName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = p.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "−" + BudgetConstants.formatCurrency(p.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            IconButton(
                                onClick = { onDeletePayment(p.id) },
                                modifier = Modifier.testTag("delete_payment_${p.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Zmazať splátku",
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

    // Delete Confirmation Dialog
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Zmazať kartu ${card.name}?") },
            text = { Text("Naozaj chcete zmazať túto kartu aj všetky jej zaznamenané splátky? Nákupy touto kartou zostanú v záznamoch bez priradenia karty.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCard(card.id)
                        cardToDelete = null
                    }
                ) {
                    Text("Zmazať", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Zrušiť")
                }
            }
        )
    }
}

@Composable
private fun CreditCardItemCard(
    status: CardStatus,
    today: String,
    onDelete: () -> Unit,
    onRecordPayment: (Double) -> Unit,
    onAddToPlan: () -> Unit
) {
    var paymentAmountText by remember(status.statementAmount) {
        mutableStateOf(if (status.statementAmount > 0) String.format(Locale.US, "%.2f", status.statementAmount) else "")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_item_${status.card.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💳 ${status.card.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Zmazať kartu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debt & limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dlh: " + BudgetConstants.formatCurrency(status.currentDebt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (status.currentDebt > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Voľný limit: ${BudgetConstants.formatCurrency(status.availableLimit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (status.usagePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (status.usagePercent > 80) ExpenseRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Statement & due dates info
            Text(
                text = "Posledný výpis (${status.statementDateStr}): ≈ ${BudgetConstants.formatCurrency(status.statementAmount)} · splatnosť ${status.dueDateStr}" +
                    if (status.isOverdue) " ⚠️ PO TERMÍNE" else if (status.statementAmount <= 0.01) " (vyrovnané)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (status.isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Nákupy od výpisu (pôjdu na ďalší výpis): ${BudgetConstants.formatCurrency(status.purchasesSinceStatement)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Repayment field & buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it.replace(',', '.') },
                    label = { Text("Suma splátky €") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onRecordPayment(amt)
                            paymentAmountText = ""
                        }
                    },
                    enabled = (paymentAmountText.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Zaplatiť")
                }
            }

            if (status.statementAmount > 0.01 && !status.isOverdue) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddToPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pridať splátku do plánu cashflow")
                }
            }
        }
    }
}
