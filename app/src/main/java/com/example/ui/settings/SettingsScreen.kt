package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetConstants
import com.example.ui.components.ClickableDateField
import com.example.ui.theme.ExpenseRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    initialBalance: Double,
    initialBalanceDate: String,
    onSaveInitialBalance: (Double, String) -> Unit,
    onExportJson: suspend () -> String,
    onExportExcel: suspend () -> ByteArray,
    onImportJson: (String, (Boolean, String) -> Unit) -> Unit,
    onLoadDemo: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var balanceInput by remember(initialBalance) {
        mutableStateOf(
            if (initialBalance != 0.0) {
                if (initialBalance % 1.0 == 0.0) initialBalance.toLong().toString() else initialBalance.toString()
            } else ""
        )
    }
    var balanceDate by remember(initialBalanceDate) {
        mutableStateOf(if (initialBalanceDate.isNotBlank()) initialBalanceDate else today)
    }

    var manualJsonText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showConfirmDemoDialog by remember { mutableStateOf(false) }

    // SAF Document Creators and Openers
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonContent = onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                    statusMessage = "Záloha bola úspešne uložená do súboru."
                } catch (e: Exception) {
                    statusMessage = "Chyba pri ukladaní: ${e.localizedMessage}"
                }
            }
        }
    }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val xlsxBytes = withContext(Dispatchers.IO) { onExportExcel() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(xlsxBytes)
                        }
                    }
                    statusMessage = "Excel export (.xlsx) bol úspešne uložený do súboru."
                } catch (e: Exception) {
                    statusMessage = "Chyba pri ukladaní Excelu: ${e.localizedMessage}"
                }
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!content.isNullOrBlank()) {
                    onImportJson(content) { success, msg ->
                        statusMessage = if (success) "Úspech: $msg" else "Chyba: $msg"
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Chyba pri čítaní súboru: ${e.localizedMessage}"
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status notice
        statusMessage?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Initial Balance & Date Settings Card (Moved to Settings as requested)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_initial_balance_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Počiatočný stav financií",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hotovosť + bežné účty k zvolenému dátumu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tento počiatočný stav slúži ako štartovací bod pre 12-mesačnú prognózu a denný harmonogram cashflow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Balance Amount Field
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it.replace(',', '.') },
                        label = { Text("Počiatočný zostatok v €") },
                        leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_balance_amount_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Field with Calendar Picker
                    ClickableDateField(
                        label = "Platné k dátumu",
                        dateValue = balanceDate,
                        onDateSelected = { balanceDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_balance_date_input"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsed = balanceInput.toDoubleOrNull() ?: 0.0
                            onSaveInitialBalance(parsed, balanceDate)
                            statusMessage = "Počiatočný zostatok ${BudgetConstants.formatCurrency(parsed)} k dátumu $balanceDate bol úspešne uložený."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_save_balance_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uložiť počiatočný zostatok", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Google Drive / Android File Backup & Export
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_backup_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Zálohovanie a synchronizácia (Google Disk)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Všetky vaše dáta sú uložené 100 % súkromne vo vašom telefóne. Pre zálohu na Google Disk, odoslanie partnerovi alebo prenos do iného zariadenia vytvorte záložný súbor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                createDocLauncher.launch("rodinny-rozpocet-zaloha.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_to_drive_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uložiť zálohu (.json)")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val jsonContent = onExportJson()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Záloha rodinného rozpočtu")
                                        putExtra(Intent.EXTRA_TEXT, jsonContent)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Zdieľať / Odoslať na Google Disk"))
                                }
                            },
                            modifier = Modifier.testTag("share_backup_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zdieľať")
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "Export tabuliek do Excelu (.xlsx)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Vytvorí prehľadný Excel zošit s hárkami pre Prehľad a prognózu, Skutočné platby, Pravidelné položky, Kreditné karty aj Splátky.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                createExcelLauncher.launch("rodinny-rozpocet-export.xlsx")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export do Excelu (.xlsx)")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val shareUri = withContext(Dispatchers.IO) {
                                            val xlsxBytes = onExportExcel()
                                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                            val exportFile = File(exportDir, "rodinny-rozpocet-export.xlsx")
                                            exportFile.writeBytes(xlsxBytes)
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                exportFile
                                            )
                                        }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                            putExtra(Intent.EXTRA_SUBJECT, "Rodinný rozpočet – Export do Excelu")
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Zdieľať Excel súbor"))
                                    } catch (e: Exception) {
                                        statusMessage = "Chyba pri zdieľaní Excelu: ${e.localizedMessage}"
                                    }
                                }
                            },
                            modifier = Modifier.testTag("share_excel_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zdieľať")
                        }
                    }
                }
            }
        }

        // 3. Import & Restore from Google Drive / file
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_backup_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Obnova zo zálohy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vyberte záložný súbor (.json) z Google Disku alebo zo svojho zariadenia. Môžete naimportovať aj zálohu priamo z vašej pôvodnej HTML aplikácie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_file_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Načítať súbor z Google Disku / úložiska")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Alebo vložte text zálohy priamo:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = manualJsonText,
                        onValueChange = { manualJsonText = it },
                        placeholder = { Text("{\"bal\": 0, \"items\": [...], \"tx\": [...], ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("manual_json_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (manualJsonText.isNotBlank()) {
                                onImportJson(manualJsonText) { success, msg ->
                                    statusMessage = if (success) "Úspech: $msg" else "Chyba: $msg"
                                    if (success) manualJsonText = ""
                                }
                            }
                        },
                        enabled = manualJsonText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_manual_json_button")
                    ) {
                        Text("Obnoviť rozpočet z textu zálohy")
                    }
                }
            }
        }

        // 4. Demo Data and Reset
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Správa dát",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showConfirmDemoDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_demo_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Načítať ukážkové dáta")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showConfirmDelete = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zmazať všetky dáta a začať nanovo")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showConfirmDemoDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDemoDialog = false },
            title = { Text("Načítať ukážkové dáta?") },
            text = { Text("Naozaj chcete prepísať existujúce dáta v aplikácii ukážkovými testovacími dátami?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLoadDemo()
                        showConfirmDemoDialog = false
                    }
                ) {
                    Text("Prepísať a načítať", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDemoDialog = false }) {
                    Text("Zrušiť")
                }
            }
        )
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Zmazať všetky dáta?") },
            text = { Text("Naozaj chcete zmazať všetky položky, skutočné platby aj kreditné karty? Táto akcia je nevratná.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showConfirmDelete = false
                    }
                ) {
                    Text("Zmazať všetko", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Zrušiť")
                }
            }
        )
    }
}
