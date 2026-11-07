package ir.dastyar.ghest.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.dastyar.ghest.ui.components.DueChip
import ir.dastyar.ghest.ui.components.EmptyState
import ir.dastyar.ghest.ui.components.StatCard
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = viewModel(),
    onOpenReports: () -> Unit,
    onOpenLoan: (Long) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { Jalali.todayEpochDay() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "دست‌یار قسط",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "امروز " + Jalali.formatWithWeekday(today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenReports) {
                    Icon(
                        Icons.Rounded.BarChart, contentDescription = "گزارش‌ها",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "باقی‌مانده اقساط",
                    value = Farsi.moneyShort(state.remainingInstallments),
                    icon = Icons.Rounded.Savings,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "عقب‌افتاده",
                    value = if (state.overdueCount == 0) "نداری" else Farsi.digit(state.overdueCount) + " مورد",
                    icon = Icons.Rounded.Warning,
                    tint = if (state.overdueCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "چک‌های در جریان",
                    value = Farsi.digit(state.openChecksCount) + " چک",
                    subtitle = Farsi.moneyShort(state.openChecksAmount) + " پرداختی",
                    icon = Icons.Rounded.ReceiptLong,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "خالص بدهی شخصی",
                    value = if (state.netPersonalDebt >= 0) Farsi.moneyShort(state.netPersonalDebt)
                            else Farsi.moneyShort(-state.netPersonalDebt),
                    subtitle = if (state.netPersonalDebt >= 0) "بدهکار هستی" else "طلبکار هستی",
                    icon = Icons.Rounded.Person,
                    tint = if (state.netPersonalDebt >= 0) DangerTint else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.paidThisMonth > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Payments, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "این ماه " + Farsi.money(state.paidThisMonth) + " پرداخت کردی",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { SectionTitleRow("سررسیدهای پیش‌رو") }

        if (state.upcoming.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Savings,
                    title = "هنوز چیزی ثبت نکردی",
                    subtitle = "اولین وام، چک یا بدهی‌ات را از تب‌های پایین اضافه کن؛\nاز این به بعد یادآوری‌اش با ماست."
                )
            }
        } else {
            items(state.upcoming, key = { it.kind + "_" + it.refId }) { row ->
                UpcomingRow(row, today) {
                    if (row.kind == "INSTALLMENT") onOpenLoan(row.refId)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SectionTitleRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun UpcomingRow(
    item: DashboardViewModel.UpcomingItem,
    today: Long,
    onClick: () -> Unit
) {
    val overdue = item.dueEpochDay < today
    val icon: ImageVector = when (item.kind) {
        "INSTALLMENT" -> Icons.Rounded.Payments
        "CHECK" -> Icons.Rounded.ReceiptLong
        else -> Icons.Rounded.Person
    }
    val tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.12f)) {
                Icon(
                    icon, contentDescription = null, tint = tint,
                    modifier = Modifier.padding(8.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Farsi.moneyShort(item.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                DueChip(item.dueEpochDay, today)
            }
        }
    }
}

private val DangerTint = Color(0xFFDC2626)
