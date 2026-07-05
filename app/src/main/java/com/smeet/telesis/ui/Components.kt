package com.smeet.telesis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smeet.telesis.data.CategorySpend
import com.smeet.telesis.data.ExpenseWithCategory
import com.smeet.telesis.util.DateUtils
import com.smeet.telesis.util.Money

@Composable
fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VaultColors.Border, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = VaultColors.Panel),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Column(Modifier.padding(18.dp), content = content) }
    )
}

@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (trailing != null) Text(trailing, color = VaultColors.Muted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VaultColors.Glass)
            .border(1.dp, VaultColors.Border, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Text(label, color = VaultColors.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun ExpenseRow(item: ExpenseWithCategory, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colorFromKey(item.categoryColorKey).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(item.categoryIcon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.categoryName} • ${item.paymentMode} • ${DateUtils.formatShort(item.dateTime)}", color = VaultColors.Muted, fontSize = 12.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.format(item.amountPaise), fontWeight = FontWeight.Bold)
            if (!item.isReviewed) Text("Review", color = VaultColors.Gold, fontSize = 11.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun CategorySpendRow(item: CategorySpend) {
    val pct = if (item.budgetPaise > 0) (item.spentPaise.toFloat() / item.budgetPaise.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(item.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(Money.format(item.spentPaise), color = VaultColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
            color = colorFromKey(item.colorKey),
            trackColor = VaultColors.Glass,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun DonutChart(values: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val total = values.sumOf { it.second }.takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.size(150.dp)) {
        var start = -90f
        values.forEachIndexed { index, item ->
            val sweep = item.second.toFloat() / total.toFloat() * 360f
            drawArc(
                color = palette(index),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                size = Size(size.width - 36.dp.toPx(), size.height - 36.dp.toPx())
            )
            start += sweep
        }
    }
}

fun colorFromKey(key: String): Color = when (key.lowercase()) {
    "gold" -> VaultColors.Gold
    "emerald", "green" -> VaultColors.Emerald
    "blue" -> VaultColors.Blue
    "rose", "red" -> VaultColors.Danger
    "orange", "amber" -> Color(0xFFFFB35B)
    "violet", "indigo" -> Color(0xFFB59CFF)
    "cyan" -> Color(0xFF69E2FF)
    "pink" -> Color(0xFFFF8FD1)
    else -> VaultColors.Muted
}

fun palette(index: Int): Color = listOf(
    VaultColors.Gold, VaultColors.Emerald, VaultColors.Blue, Color(0xFFFFB35B), Color(0xFFB59CFF), VaultColors.Danger
)[index % 6]


@Composable
fun MiniBarChart(values: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val max = values.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1L
    Row(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.takeLast(14).forEachIndexed { index, item ->
            val pct = (item.second.toFloat() / max.toFloat()).coerceIn(0.04f, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((96 * pct).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(palette(index).copy(alpha = 0.82f))
                )
                Spacer(Modifier.height(6.dp))
                Text(item.first.takeLast(2), color = VaultColors.Muted, fontSize = 9.sp)
            }
        }
    }
}
