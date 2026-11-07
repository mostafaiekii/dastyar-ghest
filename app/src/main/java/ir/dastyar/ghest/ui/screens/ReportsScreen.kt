package ir.dastyar.ghest.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.dastyar.ghest.ui.components.ProgressRing
import ir.dastyar.ghest.ui.theme.SuccessGreen
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    vm: ReportsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                }
                Text(
                    "گزارش سالانه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prevYear() }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "سال قبل")
                }
                Text(
                    "سال " + Farsi.digit(state.year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = { vm.nextYear() }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "سال بعد")
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("سررسید ماهانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Legend(primary, "اقساط")
                    Spacer(Modifier.width(4.dp))
                    Legend(tertiary, "چک پرداختی")
                    Spacer(Modifier.height(12.dp))
                    StackedBarChart(
                        a = state.dueByMonth,
                        b = state.checksByMonth,
                        colorA = primary,
                        colorB = tertiary,
                        gridColor = grid,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    MonthLabels()
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(progress = state.progress) {
                        Text(
                            Farsi.digit((state.progress * 100).toInt()) + "٪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "پرداخت‌شده: " + Farsi.moneyShort(state.totalPaid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "باقی‌مانده: " + Farsi.moneyShort(state.remaining),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "کل تعهدات: " + Farsi.moneyShort(state.totalDue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (state.perLoan.isNotEmpty()) {
            item {
                Text("پیشرفت هر تعهد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(state.perLoan.size) { idx ->
                val (title, paid, total) = state.perLoan[idx]
                val frac = if (total <= 0) 0f else paid.toFloat() / total
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                Farsi.moneyShort(paid) + " / " + Farsi.moneyShort(total),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthLabels() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("ف", "ا", "خ", "ت", "م", "ش", "م", "آ", "آ", "د", "ب", "ا").forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StackedBarChart(
    a: List<Long>,
    b: List<Long>,
    colorA: Color,
    colorB: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = ((a + b).maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier) {
        val chartHeight = size.height - 8.dp.toPx()
        val gap = 6.dp.toPx()
        val barWidth = (size.width - gap * 11) / 12f
        val corner = CornerRadius(barWidth / 3f, barWidth / 3f)

        // خطوط راهنما
        for (i in 1..3) {
            val y = chartHeight * i / 4f
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(0f, chartHeight - y),
                end = Offset(size.width, chartHeight - y),
                strokeWidth = 1.dp.toPx()
            )
        }

        for (m in 0 until 12) {
            // راست‌به‌چپ: فروردین سمت راست
            val x = size.width - (m + 1) * barWidth - m * gap
            val total = a[m] + b[m]
            if (total <= 0L) continue
            val heightA = chartHeight * (a[m].toFloat() / maxVal)
            val heightB = chartHeight * (b[m].toFloat() / maxVal)
            val totalH = heightA + heightB
            // میله اصلی
            drawRoundRect(
                color = colorA,
                topLeft = Offset(x, chartHeight - totalH),
                size = Size(barWidth, totalH.coerceAtLeast(4.dp.toPx())),
                cornerRadius = corner
            )
            // بخش چک روی میله
            if (b[m] > 0) {
                drawRoundRect(
                    color = colorB,
                    topLeft = Offset(x, chartHeight - totalH),
                    size = Size(barWidth, heightB.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = corner
                )
            }
        }
    }
}
