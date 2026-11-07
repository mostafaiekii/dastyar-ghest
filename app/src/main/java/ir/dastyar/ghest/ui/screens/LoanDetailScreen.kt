package ir.dastyar.ghest.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ir.dastyar.ghest.data.LoanType
import ir.dastyar.ghest.ui.components.DueChip
import ir.dastyar.ghest.ui.components.ProgressRing
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.LoanDetailViewModel

@Composable
fun LoanDetailScreen(
    vm: LoanDetailViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { Jalali.todayEpochDay() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val loan = state.loan

    Column(Modifier.fillMaxSize()) {
        // نوار بالا
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                loan?.title ?: "جزئیات",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "بیشتر")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (loan?.archived == true) "خروج از بایگانی" else "بایگانی") },
                        leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                        onClick = { vm.archive(loan?.archived != true); menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف کامل") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { confirmDelete = true; menuOpen = false }
                    )
                }
            }
        }

        if (loan == null) {
            Text(
                "این وام پیدا نشد",
                Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.padding(18.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                LoanType.label(loan.type) + if (loan.creditor.isNotBlank()) " • " + loan.creditor else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                Farsi.money(state.paid) + " از " + Farsi.money(state.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "مانده: " + Farsi.money(state.total - state.paid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (loan.note.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    loan.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        ProgressRing(
                            progress = state.progress,
                            color = MaterialTheme.colorScheme.primary,
                            track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        ) {
                            Text(
                                Farsi.digit((state.progress * 100).toInt()) + "٪",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (state.overdueCount > 0) {
                item {
                    Button(
                        onClick = { vm.payAllOverdue() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            "پرداخت همه‌ی " + Farsi.digit(state.overdueCount) + " قسط عقب‌افتاده",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(state.installments, key = { it.id }) { inst ->
                InstallmentRow(
                    number = inst.number,
                    due = inst.dueEpochDay,
                    amount = inst.amount,
                    isPaid = inst.isPaid,
                    today = today,
                    onToggle = { vm.togglePaid(inst) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف وام") },
            text = { Text("همه‌ی اقساط این وام هم حذف می‌شود. مطمئنی؟") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onDone = onBack) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun InstallmentRow(
    number: Int,
    due: Long,
    amount: Long,
    isPaid: Boolean,
    today: Long,
    onToggle: () -> Unit
) {
    val overdue = !isPaid && due < today
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (isPaid) MaterialTheme.colorScheme.primary
                else if (overdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    Farsi.digit(number),
                    Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isPaid) MaterialTheme.colorScheme.onPrimary
                    else if (overdue) MaterialTheme.colorScheme.onError
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Jalali.format(due),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isPaid) "پرداخت‌شده" else Farsi.money(amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!isPaid) DueChip(due, today)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onToggle) {
                Icon(
                    if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                    contentDescription = if (isPaid) "لغو پرداخت" else "پرداخت شد",
                    tint = if (isPaid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
