package ir.dastyar.ghest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import ir.dastyar.ghest.data.DebtDirection
import ir.dastyar.ghest.data.PersonalDebt
import ir.dastyar.ghest.ui.components.EmptyState
import ir.dastyar.ghest.ui.components.JalaliDateField
import ir.dastyar.ghest.ui.components.MoneyField
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.DebtsViewModel

@Composable
fun DebtsScreen(vm: DebtsViewModel = viewModel()) {
    val open by vm.open.collectAsStateWithLifecycle()
    val settled by vm.settled.collectAsStateWithLifecycle()
    val today = remember { Jalali.todayEpochDay() }
    var tab by remember { mutableStateOf(0) } // 0=باز 1=تسویه‌شده
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PersonalDebt?>(null) }

    val iOwe = open.filter { it.direction == DebtDirection.I_OWE }.sumOf { it.amount }
    val theyOwe = open.filter { it.direction == DebtDirection.THEY_OWE }.sumOf { it.amount }
    val visible = if (tab == 0) open else settled

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "بدهی جدید")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "قرض و بدهی شخصی",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            "بدهکارم: " + Farsi.moneyShort(iOwe),
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "طلب دارم: " + Farsi.moneyShort(theyOwe),
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("باز") })
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        label = { Text("تسویه‌شده (" + Farsi.digit(settled.size) + ")") }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Groups,
                    title = if (tab == 0) "قرض بازی بینتان نیست" else "چیزی تسویه نشده",
                    subtitle = if (tab == 0) "قرض‌های شخصی بین خود و دوستان را ثبت کن\nتا هیچ حساب‌کتابی گم نشود."
                    else "موارد تسویه‌شده اینجا می‌مانند"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            today = today,
                            onSettle = { vm.setSettled(debt.id, true) },
                            onReopen = { vm.setSettled(debt.id, false) },
                            onDelete = { deleting = debt }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddDebtSheet(
            onDismiss = { showAdd = false },
            onSave = { person, direction, amount, due, note ->
                vm.save(null, person, direction, amount, due, note) { showAdd = false }
            }
        )
    }

    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف") },
            text = { Text("«${d.person}» حذف شود؟") },
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
private fun DebtCard(
    debt: PersonalDebt,
    today: Long,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit
) {
    val iOwe = debt.direction == DebtDirection.I_OWE
    val accent = if (iOwe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val overdue = !debt.isSettled && debt.dueEpochDay != null && debt.dueEpochDay < today

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                Text(
                    debt.person.take(1),
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(debt.person, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    DebtDirection.label(debt.direction) +
                        if (debt.dueEpochDay != null) " • سررسید " + Jalali.format(debt.dueEpochDay!!) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (debt.note.isNotBlank()) {
                    Text(
                        debt.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Farsi.moneyShort(debt.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accent
                )
                if (debt.isSettled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "تسویه‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = { if (debt.isSettled) onReopen() else onSettle() }) {
                Icon(
                    if (debt.isSettled) Icons.Rounded.Refresh else Icons.Rounded.Check,
                    contentDescription = if (debt.isSettled) "بازگردانی" else "تسویه شد",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete, contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    onDismiss: () -> Unit,
    onSave: (person: String, direction: String, amount: Long, due: Long?, note: String) -> Unit
) {
    var person by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(DebtDirection.I_OWE) }
    var amountText by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            Text("ثبت قرض شخصی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = person, onValueChange = { person = it },
                label = { Text("اسم شخص") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == DebtDirection.I_OWE,
                    onClick = { direction = DebtDirection.I_OWE },
                    label = { Text("به او بدهکارم") }
                )
                FilterChip(
                    selected = direction == DebtDirection.THEY_OWE,
                    onClick = { direction = DebtDirection.THEY_OWE },
                    label = { Text("به من بدهکارن") }
                )
            }
            Spacer(Modifier.height(10.dp))

            MoneyField(amountText, { amountText = it }, "مبلغ")
            Spacer(Modifier.height(10.dp))
            JalaliDateField(label = "موعد برگشت — اختیاری", epochDay = due, onPick = { due = it })
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
                        person.isBlank() -> error = "اسم شخص را بنویس"
                        amount <= 0 -> error = "مبلغ را وارد کن"
                        else -> onSave(person, direction, amount, due, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("ثبت", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
