package ir.dastyar.ghest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.dastyar.ghest.data.Check
import ir.dastyar.ghest.data.CheckDirection
import ir.dastyar.ghest.data.CheckStatus
import ir.dastyar.ghest.ui.components.DueChip
import ir.dastyar.ghest.ui.components.EmptyState
import ir.dastyar.ghest.ui.components.JalaliDateField
import ir.dastyar.ghest.ui.components.MoneyField
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.ChecksViewModel

@Composable
fun ChecksScreen(vm: ChecksViewModel = viewModel()) {
    val checks by vm.checks.collectAsStateWithLifecycle()
    val today = remember { Jalali.todayEpochDay() }
    var filter by remember { mutableStateOf<String?>(null) } // null = همه
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Check?>(null) }
    var deleting by remember { mutableStateOf<Check?>(null) }

    val visible = if (filter == null) checks else checks.filter { it.status == filter }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "چک جدید")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "چک‌ها",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("همه") }
                        )
                    }
                    items(CheckStatus.all) { s ->
                        FilterChip(
                            selected = filter == s,
                            onClick = { filter = s },
                            label = { Text(CheckStatus.label(s)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "چکی اینجا نیست",
                    subtitle = "چک‌های دریافتی و پرداختی را ثبت کن\nتا سررسیدشان یادت نرود."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { check ->
                        CheckCard(
                            check = check,
                            today = today,
                            onMarkPassed = { vm.setStatus(check.id, CheckStatus.PASSED) },
                            onEdit = { editing = check },
                            onDelete = { deleting = check },
                            onStatus = { vm.setStatus(check.id, it) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        AddCheckSheet(
            initial = editing,
            onDismiss = { showAdd = false; editing = null },
            onSave = { direction, title, bank, number, sayad, amount, due, note ->
                vm.save(
                    id = editing?.id, direction = direction, title = title, bank = bank,
                    number = number, sayadId = sayad, amount = amount, due = due,
                    note = note, status = editing?.status ?: CheckStatus.OPEN
                ) {
                    showAdd = false; editing = null
                }
            }
        )
    }

    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف چک") },
            text = { Text("چک «${d.title}» حذف شود؟") },
            confirmButton = {
                TextButton(onClick = { vm.delete(d); deleting = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun CheckCard(
    check: Check,
    today: Long,
    onMarkPassed: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatus: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val payable = check.direction == CheckDirection.PAYABLE
    val accent = if (payable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                Icon(
                    if (payable) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        check.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                        Text(
                            CheckDirection.label(check.direction),
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        check.bank.ifBlank { null },
                        if (check.number.isNotBlank()) "شماره " + Farsi.digit(check.number) else null,
                        if (check.sayadId.isNotBlank()) "صیادی" else null
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Farsi.moneyShort(check.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    if (check.status == CheckStatus.OPEN) DueChip(check.dueEpochDay, today)
                }
                if (check.status != CheckStatus.OPEN) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        CheckStatus.label(check.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (check.status) {
                            CheckStatus.PASSED -> MaterialTheme.colorScheme.primary
                            CheckStatus.RETURNED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Rounded.Check, contentDescription = "عملیات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (check.status == CheckStatus.OPEN) {
                        DropdownMenuItem(
                            text = { Text("پاس شد") },
                            onClick = { onMarkPassed(); menuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("برگشتی") },
                            onClick = { onStatus(CheckStatus.RETURNED); menuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("ابطال") },
                            onClick = { onStatus(CheckStatus.CANCELLED); menuOpen = false }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("بازگشت به در جریان") },
                            onClick = { onStatus(CheckStatus.OPEN); menuOpen = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("ویرایش") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { onEdit(); menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); menuOpen = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCheckSheet(
    initial: Check?,
    onDismiss: () -> Unit,
    onSave: (direction: String, title: String, bank: String, number: String, sayad: String, amount: Long, due: Long, note: String) -> Unit
) {
    var direction by remember { mutableStateOf(initial?.direction ?: CheckDirection.PAYABLE) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var bank by remember { mutableStateOf(initial?.bank ?: "") }
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var sayad by remember { mutableStateOf(initial?.sayadId ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.let { Farsi.number(it) } ?: "") }
    var due by remember { mutableStateOf(initial?.dueEpochDay) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                if (initial == null) "ثبت چک جدید" else "ویرایش چک",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == CheckDirection.PAYABLE,
                    onClick = { direction = CheckDirection.PAYABLE },
                    label = { Text("پرداختی") }
                )
                FilterChip(
                    selected = direction == CheckDirection.RECEIVABLE,
                    onClick = { direction = CheckDirection.RECEIVABLE },
                    label = { Text("دریافتی") }
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("بابت") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            MoneyField(amountText, { amountText = it }, "مبلغ چک")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = bank, onValueChange = { bank = it },
                label = { Text("بانک — اختیاری") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = number, onValueChange = { number = it },
                label = { Text("شماره چک — اختیاری") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = sayad, onValueChange = { sayad = it },
                label = { Text("شناسه صیادی — اختیاری") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            JalaliDateField(label = "تاریخ سررسید", epochDay = due, onPick = { due = it })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("یادداشت — اختیاری") },
                modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val amount = Farsi.parseNumber(amountText)
                    when {
                        title.isBlank() -> error = "بابت چک را بنویس"
                        amount <= 0 -> error = "مبلغ چک را وارد کن"
                        due == null -> error = "تاریخ سررسید را انتخاب کن"
                        else -> onSave(direction, title, bank, number, sayad, amount, due!!, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (initial == null) "ثبت چک" else "ذخیره تغییرات", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
