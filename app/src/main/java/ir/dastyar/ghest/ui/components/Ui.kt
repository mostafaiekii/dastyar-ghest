package ir.dastyar.ghest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.ui.theme.WarnAmber

/* ---------------- چیپ وضعیت سررسید ---------------- */

@Composable
fun DueChip(dueEpochDay: Long, today: Long, modifier: Modifier = Modifier) {
    val diff = dueEpochDay - today
    val scheme = MaterialTheme.colorScheme
    val chip: Pair<String, Color> = when {
        diff < 0 -> (if (diff == -1L) "دیروز" else "${Farsi.digit(-diff)} روز عقب") to scheme.error
        diff == 0L -> "امروز" to WarnAmber
        diff == 1L -> "فردا" to WarnAmber
        diff <= 3L -> "${Farsi.digit(diff)} روز مانده" to Color(0xFFEA580C)
        diff <= 7L -> "${Farsi.digit(diff)} روز مانده" to scheme.primary
        else -> Jalali.format(dueEpochDay) to scheme.onSurfaceVariant
    }
    val (text, color) = chip
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/* ---------------- کارت آماری داشبورد ---------------- */

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tint.copy(alpha = 0.12f)) {
                    Icon(
                        icon, contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------------- حلقه پیشرفت ---------------- */

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Float = 10f,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "ring"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(120.dp)) {
            val strokePx = stroke.dp.toPx()
            val inset = strokePx / 2
            drawArc(
                color = track,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            )
        }
        content()
    }
}

/* ---------------- فیلد مبلغ ---------------- */

@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(Farsi.formatInput(it)) },
        label = { Text(label) },
        suffix = { Text("تومان", style = MaterialTheme.typography.labelMedium) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

/* ---------------- انتخابگر تاریخ شمسی ---------------- */

@Composable
fun JalaliDateField(
    label: String,
    epochDay: Long?,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val shown = epochDay?.let { Jalali.format(it) } ?: "انتخاب تاریخ"
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Event, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(shown)
        Spacer(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        JalaliDatePickerDialog(
            initial = epochDay ?: Jalali.todayEpochDay(),
            onConfirm = { onPick(it); open = false },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun JalaliDatePickerDialog(
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val t = Jalali.toJalali(initial)
    var year by remember { mutableStateOf(t[0]) }
    var month by remember { mutableStateOf(t[1]) }
    var day by remember { mutableStateOf(t[2]) }

    val years = remember { (1380..1470).toList() }
    val daysInMonth = Jalali.monthLength(year, month)
    val clampedDay = day.coerceAtMost(daysInMonth)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(Jalali.j2d(year, month, clampedDay))
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        title = { Text("انتخاب تاریخ") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DropdownRow(
                    label = "سال",
                    value = Farsi.digit(year),
                    options = years.map { Farsi.digit(it) },
                    onSelect = { idx -> year = years[idx] }
                )
                Spacer(Modifier.height(10.dp))
                DropdownRow(
                    label = "ماه",
                    value = Jalali.MONTH_NAMES[month - 1],
                    options = Jalali.MONTH_NAMES,
                    onSelect = { idx -> month = idx + 1 }
                )
                Spacer(Modifier.height(10.dp))
                DropdownRow(
                    label = "روز",
                    value = Farsi.digit(clampedDay),
                    options = (1..daysInMonth).map { Farsi.digit(it) },
                    onSelect = { idx -> day = idx + 1 }
                )
            }
        }
    )
}

@Composable
private fun DropdownRow(label: String, value: String, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(value, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(idx); expanded = false }
                )
            }
        }
    }
}

/* ---------------- حالت خالی ---------------- */

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp).size(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/* ---------------- تاییدیه حذف ---------------- */

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "حذف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        title = { Text(title) },
        text = { Text(text) }
    )
}

/* ---------------- عنوان بخش ---------------- */

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 4.dp)
    )
}
