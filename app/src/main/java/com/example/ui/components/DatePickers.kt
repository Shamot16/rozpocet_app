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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetConstants
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Formats "YYYY-MM" to Slovak representation, e.g. "2026-11" -> "november 2026"
 */
fun formatYearMonthSlovak(yearMonthStr: String): String {
    val parts = yearMonthStr.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size >= 2) {
        val y = parts[0]
        val m = parts[1] - 1
        val monthName = BudgetConstants.SlovakMonths.getOrElse(m) { "" }
        return "$monthName $y"
    }
    return yearMonthStr
}

/**
 * Standard Material 3 DatePickerDialog that converts selected date into "YYYY-MM-DD"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateStr) {
        val parts = initialDateStr.split("-").mapNotNull { it.toIntOrNull() }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (parts.size >= 3) {
            cal.set(parts[0], parts[1] - 1, parts[2], 12, 0, 0)
        }
        cal.timeInMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        val y = cal.get(Calendar.YEAR)
                        val m = cal.get(Calendar.MONTH) + 1
                        val d = cal.get(Calendar.DAY_OF_MONTH)
                        val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
                        onDateSelected(formatted)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("date_picker_confirm_btn")
            ) {
                Text("Vybrať", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušiť")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Interactive Dialog for selecting a specific month and year.
 */
@Composable
fun MonthYearPickerDialog(
    currentYearMonth: String,
    onYearMonthSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = remember(currentYearMonth) {
        val p = currentYearMonth.split("-").mapNotNull { it.toIntOrNull() }
        val now = Calendar.getInstance()
        val y = p.getOrNull(0) ?: now.get(Calendar.YEAR)
        val m = (p.getOrNull(1) ?: (now.get(Calendar.MONTH) + 1)) - 1
        Pair(y, m)
    }

    var selectedYear by remember { mutableIntStateOf(initialParts.first) }
    var selectedMonth by remember { mutableIntStateOf(initialParts.second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Výber mesiaca a roka",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year switcher row with previous and next buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear -= 1 },
                        modifier = Modifier.testTag("year_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Predchádzajúci rok")
                    }

                    Text(
                        text = "$selectedYear",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("selected_year_label")
                    )

                    IconButton(
                        onClick = { selectedYear += 1 },
                        modifier = Modifier.testTag("year_next_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Nasledujúci rok")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 12 Months Grid (3 columns x 4 rows)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(230.dp)
                ) {
                    itemsIndexed(BudgetConstants.SlovakMonths) { index, monthName ->
                        val isSelected = index == selectedMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedMonth = index }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .testTag("month_chip_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthName.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
                    onYearMonthSelected(formatted)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_month_year_button")
            ) {
                Text("Potvrdiť výber")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušiť")
            }
        }
    )
}

/**
 * Clickable Date Field wrapper displaying a calendar icon and opening AppDatePickerDialog
 */
@Composable
fun ClickableDateField(
    label: String,
    dateValue: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "clickable_date_field"
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = dateValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        // Transparent overlay catching clicks across the entire component
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AppDatePickerDialog(
            initialDateStr = dateValue,
            onDateSelected = onDateSelected,
            onDismiss = { showDialog = false }
        )
    }
}
