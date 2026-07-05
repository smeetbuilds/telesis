package com.smeet.telesis.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
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
import com.smeet.telesis.data.TransactionSource
import com.smeet.telesis.data.TransactionType
import com.smeet.telesis.util.DateUtils
import com.smeet.telesis.util.Money

@Composable
fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, VaultColors.Border, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Column(Modifier.padding(18.dp), content = content) }
    )
}

@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, VaultColors.Border, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ExpenseRow(item: ExpenseWithCategory, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val amountColor = when (item.type) {
        TransactionType.INCOME -> VaultColors.Emerald
        TransactionType.TRANSFER -> VaultColors.Blue
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    }
    val amountPrefix = when (item.type) {
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> "↔ "
        TransactionType.EXPENSE -> ""
    }
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
                .background(colorFromKey(item.categoryColorKey).copy(alpha = 0.18f))
                .border(1.dp, colorFromKey(item.categoryColorKey).copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(item.categoryIcon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${item.categoryName} • ${item.paymentMode} • ${DateUtils.formatShort(item.dateTime)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!item.isReviewed) SmallBadge("Review", VaultColors.Gold)
                if (item.source == TransactionSource.SMS) SmallBadge("SMS", VaultColors.Blue)
                if (item.type == TransactionType.TRANSFER) SmallBadge("Transfer", VaultColors.Blue)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amountPrefix + Money.format(item.amountPaise), fontWeight = FontWeight.Bold, color = amountColor, maxLines = 1)
            Text(item.source.name.lowercase().replaceFirstChar { it.titlecase() }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
fun CategorySpendRow(item: CategorySpend) {
    val rawPct = if (item.budgetPaise > 0) item.spentPaise.toFloat() / item.budgetPaise.toFloat() else 0f
    val pct by animateFloatAsState(targetValue = rawPct.coerceIn(0f, 1f), label = "categoryBudgetProgress")
    val overBudget = item.budgetPaise > 0 && item.spentPaise > item.budgetPaise
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.budgetPaise > 0) {
                    Text(
                        "Budget ${Money.format(item.budgetPaise, compact = true)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Text(Money.format(item.spentPaise), color = if (overBudget) VaultColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = if (overBudget) FontWeight.Bold else FontWeight.Normal)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
            color = if (overBudget) VaultColors.Danger else colorFromKey(item.colorKey),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun DonutChart(values: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val filtered = values.filter { it.second > 0 }
    val total = filtered.sumOf { it.second }.takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.size(150.dp)) {
        if (filtered.isEmpty()) {
            drawArc(
                color = Color.Gray.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                size = Size(size.width - 36.dp.toPx(), size.height - 36.dp.toPx())
            )
            return@Canvas
        }
        var start = -90f
        filtered.forEachIndexed { index, item ->
            val sweep = item.second.toFloat() / total.toFloat() * 360f
            drawArc(
                color = palette(index),
                startAngle = start,
                sweepAngle = sweep.coerceAtLeast(1.5f),
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
    val displayValues = values.takeLast(14)
    val max = displayValues.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1L
    Row(
        modifier = modifier.fillMaxWidth().height(124.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (displayValues.isEmpty()) {
            repeat(7) { index ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(palette(index).copy(alpha = 0.18f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("--", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        } else {
            displayValues.forEachIndexed { index, item ->
                val pct by animateFloatAsState(
                    targetValue = if (item.second <= 0) 0.05f else (item.second.toFloat() / max.toFloat()).coerceIn(0.08f, 1f),
                    label = "dailySpendBar"
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((96 * pct).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(palette(index).copy(alpha = if (item.second <= 0) 0.18f else 0.82f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(item.first.takeLast(2), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        }
    }
}
