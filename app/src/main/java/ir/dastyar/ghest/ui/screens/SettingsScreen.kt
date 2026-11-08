package ir.dastyar.ghest.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.dastyar.ghest.ui.theme.ThemeMode
import ir.dastyar.ghest.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val pinEnabled by vm.pinEnabled.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pinDialog by remember { mutableStateOf(false) }
    var removePinDialog by remember { mutableStateOf(false) }
    var importConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }

    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) { pendingImportUri = uri; importConfirm = true } }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { vm.exportCsv(it) } }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "تنظیمات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )

            // ---------- ظاهر ----------
            SettingsCard("ظاهر") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.DarkMode, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    ThemeMode.entries.forEach { mode ->
                        FilterChipLike(
                            label = mode.label,
                            selected = theme == mode,
                            onClick = { vm.setTheme(mode) }
                        )
                    }
                }
            }

            // ---------- امنیت ----------
            SettingsCard("امنیت") {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (pinEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("قفل با رمز ۴ رقمی", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (pinEnabled) "فعال — هر بار ورود رمز می‌خواهد" else "غیرفعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = pinEnabled, onCheckedChange = { checked ->
                        if (checked) pinDialog = true else removePinDialog = true
                    })
                }
                if (pinEnabled) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { pinDialog = true }) { Text("تغییر رمز") }
                }
            }

            // ---------- داده‌ها ----------
            SettingsCard("داده‌ها") {
                ActionRow(Icons.Rounded.CloudUpload, "خروجی پشتیبان (JSON)") {
                    exportJson.launch("dastyar-ghest-backup.json")
                }
                ActionRow(Icons.Rounded.CloudDownload, "بازیابی از پشتیبان") {
                    importJson.launch(arrayOf("application/json"))
                }
                ActionRow(Icons.Rounded.Description, "خروجی اکسل (CSV)") {
                    exportCsv.launch("dastyar-ghest-report.csv")
                }
                Text(
                    "پشتیبان‌گیری را هر چند وقت یک‌بار انجام بده؛ داده‌ها فقط روی گوشی خودت ذخیره می‌شوند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---------- پشتیبانی ----------
            SettingsCard("پشتیبانی") {
                ActionRow(Icons.Rounded.SupportAgent, "ارسال پیام به پشتیبانی") {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse(
                            "mailto:mroidad2@gmail.com?subject=" +
                                Uri.encode("دست‌یار قسط — پشتیبانی")
                        )
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        scope.launch {
                            snackbar.showSnackbar(
                                "برنامه‌ی ایمیل پیدا نشد — آدرس مستقیم: mroidad2@gmail.com"
                            )
                        }
                    }
                }
            }

            // ---------- درباره ----------
            SettingsCard("درباره") {
                Text("دست‌یار قسط — نسخه ۱.۱.۰", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "مدیریت آفلاین اقساط، چک و قرض؛ بدون نیاز به اینترنت و کاملاً رایگان.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "حریم خصوصی: همه‌ی اطلاعات و عکس مدارک فقط روی حافظه‌ی خودِ گوشی ذخیره می‌شود و هیچ داده‌ای برای هیچ سروری ارسال نمی‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "توسعه‌دهنده: دست‌یار (Dastyar) — mroidad2@gmail.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (pinDialog) {
        PinSetupDialog(
            onDismiss = { pinDialog = false },
            onSave = { vm.setPin(it); pinDialog = false }
        )
    }

    if (removePinDialog) {
        AlertDialog(
            onDismissRequest = { removePinDialog = false },
            title = { Text("برداشتن قفل") },
            text = { Text("قفل رمز غیرفعال شود؟") },
            confirmButton = {
                TextButton(onClick = { vm.removePin(); removePinDialog = false }) {
                    Text("بله، بردار")
                }
            },
            dismissButton = { TextButton(onClick = { removePinDialog = false }) { Text("انصراف") } }
        )
    }

    if (importConfirm) {
        AlertDialog(
            onDismissRequest = { importConfirm = false },
            title = { Text("بازیابی پشتیبان") },
            text = { Text("همه‌ی داده‌های فعلی با محتوای فایل پشتیبان جایگزین می‌شود. ادامه بدهم؟") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.importBackup(it) }
                    importConfirm = false
                }) { Text("بله، بازیابی کن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { importConfirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun FilterChipLike(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, Modifier.padding(end = 8.dp))
        Text(label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم رمز ۴ رقمی") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("رمز جدید") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirm = it },
                    label = { Text("تکرار رمز") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    pin.length != 4 -> error = "رمز باید ۴ رقم باشد"
                    pin != confirm -> error = "تکرار رمز مطابقت ندارد"
                    else -> onSave(pin)
                }
            }) { Text("فعال‌سازی") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
