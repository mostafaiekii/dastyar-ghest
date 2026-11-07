package ir.dastyar.ghest.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ir.dastyar.ghest.data.Loan
import ir.dastyar.ghest.data.LoanType
import ir.dastyar.ghest.ui.components.DueChip
import ir.dastyar.ghest.ui.components.EmptyState
import ir.dastyar.ghest.ui.components.JalaliDateField
import ir.dastyar.ghest.ui.components.MoneyField
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.InstallmentDaoAlias
import ir.dastyar.ghest.viewmodel.LoansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    vm: LoansViewModel = viewModel(),
    onOpen: (Long) -> Unit
) {
    val loans by vm.loans.collectAsStateWithLifecycle()
    val archivedLoans by vm.archivedLoans.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var showArchived by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val visible = if (showArchived) archivedLoans else loans

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "وام جدید")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "اقساط و وام‌ها",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("فعال") }
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("بایگانی") }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.AccountBalance,
                    title = if (showArchived) "بایگانی خالی است" else "هیچ وامی ثبت نشده",
                    subtitle = if (showArchived) "وام‌های تمام‌شده اینجا آرشیو می‌شوند"
                    else "وام، خرید اقساطی یا بیمه‌ات را اضافه کن\nتا اقساطش را قدم‌به‌قدم برایت پیگیری کنیم."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visible, key = { it.id }) { loan ->
                        val st = stats[loan.id]
                        LoanCard(loan, st) { onOpen(loan.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddLoanSheet(
            onDismiss = { showAdd = false },
            onSave = { title, creditor, type, amount, count, step, due, note ->
                vm.addLoan(title, creditor, type, amount, count, step, due, note) {
                    showAdd = false
                }
            }
        )
    }
}

@Composable
private fun LoanCard(loan: Loan, st: InstallmentDaoAlias.LoanStatX?, onClick: () -> Unit) {
    val today = Jalali.todayEpochDay()
    val total = st?.total ?: loan.count
    val paidCount = st?.paidCount ?: 0
    val progress = if (total <= 0) 0f else paidCount.toFloat() / total
    val paidAmount = st?.paidAmount ?: 0L
    val remaining = loan.installmentAmount * (total - paidCount).coerceAtLeast(0)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(loan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        LoanType.label(loan.type) + if (loan.creditor.isNotBlank()) " • " + loan.creditor else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            "${Farsi.digit(paidCount)} از ${Farsi.digit(total)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "مانده: " + Farsi.moneyShort(remaining),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "پرداخت‌شده: " + Farsi.moneyShort(paidAmount) +
                            " از " + Farsi.moneyShort(loan.installmentAmount * total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                st?.nextDue?.let {
                    DueChip(it, today)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLoanSheet(
    onDismiss: () -> Unit,
    onSave: (title: String, creditor: String, type: String, amount: Long, count: Int, stepDays: Int, firstDue: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var creditor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LoanType.BANK) }
    var amountText by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("") }
    var stepChoice by remember { mutableStateOf(0) } // index در STEP_OPTIONS، سفارشی = -1
    var customStep by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val stepOptions = listOf("ماهانه (۳۰ روز)" to 30, "دوماهانه (۶۰ روز)" to 60, "سه‌ماهانه (۹۰ روز)" to 90, "سفارشی" to -1)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("ثبت وام / تعهد جدید", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان (مثلاً وام مسکن)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = creditor, onValueChange = { creditor = it },
                label = { Text("طرف حساب (بانک / شخص) — اختیاری") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("نوع", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LoanType.all) { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(LoanType.label(t)) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            MoneyField(amountText, { amountText = it }, "مبلغ هر قسط")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = countText,
                onValueChange = { countText = Farsi.formatInput(it) },
                label = { Text("تعداد اقساط") },
                suffix = { Text("قسط", style = MaterialTheme.typography.labelMedium) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("فاصله اقساط", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stepOptions.size) { idx ->
                    FilterChip(
                        selected = stepChoice == idx,
                        onClick = { stepChoice = idx },
                        label = { Text(stepOptions[idx].first) }
                    )
                }
            }
            if (stepChoice == 3) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customStep,
                    onValueChange = { customStep = Farsi.formatInput(it) },
                    label = { Text("هر چند روز یک قسط؟") },
                    suffix = { Text("روز", style = MaterialTheme.typography.labelMedium) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))

            JalaliDateField(label = "اولین سررسید", epochDay = due, onPick = { due = it })
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
                    val count = Farsi.parseNumber(countText).toInt()
                    val step = if (stepChoice == 3) Farsi.parseNumber(customStep).toInt() else stepOptions[stepChoice].second
                    when {
                        title.isBlank() -> error = "عنوان را بنویس"
                        amount <= 0 -> error = "مبلغ قسط را وارد کن"
                        count < 1 -> error = "تعداد اقساط حداقل ۱ است"
                        step <= 0 -> error = "فاصله اقساط را درست وارد کن"
                        due == null -> error = "تاریخ اولین سررسید را انتخاب کن"
                        else -> {
                            onSave(title, creditor, type, amount, count, step, due!!, note)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("ثبت وام", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
