package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.TransactionItem
import com.example.model.TxType
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeMint
import com.example.util.MoneyCalculator
import com.example.util.PeriodChartBucket
import com.example.util.PeriodFilter
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Interactive Transaction History Line + Area Graph plotting individual chronological transactions
 * with tap-to-inspect tooltip showing exact title, amount, and date.
 */
@Composable
fun TransactionHistoryChart(
    transactions: List<TransactionItem>,
    currency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val chronological = remember(transactions, currency) {
        transactions
            .filter { it.currency == currency }
            .sortedBy { it.occurredAtMillis }
            .takeLast(24)
    }

    var selectedIdx by remember(chronological) {
        mutableIntStateOf(if (chronological.isNotEmpty()) chronological.lastIndex else -1)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.transactionHistoryGraph,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (chronological.isNotEmpty()) {
                    Text(
                        text = "${MoneyCalculator.localizeDigits(chronological.size.toString(), language)} ${strings.transactionsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (chronological.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noDataForPeriod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val selectedTx = chronological.getOrNull(selectedIdx)
                if (selectedTx != null) {
                    val isIncome = selectedTx.type == TxType.INCOME
                    val badgeColor = if (isIncome) IncomeMint else ExpenseCoral
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedTx.title.ifBlank { strings.categoryName(selectedTx.category, language) },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = PeriodFilter.formatShortDate(selectedTx.occurredAtMillis, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (isIncome) "+" else "-") + MoneyCalculator.formatAmount(selectedTx.amountMinor, currency, language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val maxVal = max(1L, chronological.maxOf { it.amountMinor })
                val primaryColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .pointerInput(chronological) {
                            detectTapGestures { offset ->
                                if (chronological.size == 1) {
                                    selectedIdx = 0
                                } else {
                                    val pad = 16.dp.toPx()
                                    val usableW = (size.width - pad * 2).coerceAtLeast(1f)
                                    val step = usableW / (chronological.size - 1)
                                    val idx = ((offset.x - pad) / step).roundToInt()
                                        .coerceIn(0, chronological.lastIndex)
                                    selectedIdx = idx
                                }
                            }
                        }
                ) {
                    val padX = 16.dp.toPx()
                    val padY = 14.dp.toPx()
                    val chartW = (size.width - padX * 2).coerceAtLeast(1f)
                    val chartH = (size.height - padY * 2).coerceAtLeast(1f)

                    // Horizontal subtle grid lines
                    for (i in 0..3) {
                        val gy = padY + (chartH / 3f) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(padX, gy),
                            end = Offset(size.width - padX, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val points = chronological.mapIndexed { idx, tx ->
                        val x = if (chronological.size == 1) size.width / 2f
                        else padX + (chartW * idx) / (chronological.size - 1)
                        val ratio = tx.amountMinor.toFloat() / maxVal.toFloat()
                        val y = padY + chartH * (1f - ratio)
                        Offset(x, y)
                    }

                    if (points.size > 1) {
                        val linePath = Path()
                        val areaPath = Path()
                        points.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                linePath.moveTo(pt.x, pt.y)
                                areaPath.moveTo(pt.x, padY + chartH)
                                areaPath.lineTo(pt.x, pt.y)
                            } else {
                                val prev = points[idx - 1]
                                val cx = (prev.x + pt.x) / 2f
                                linePath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                areaPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                            }
                        }
                        areaPath.lineTo(points.last().x, padY + chartH)
                        areaPath.close()

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.34f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = padY,
                                endY = padY + chartH
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val tx = chronological[idx]
                        val dotColor = if (tx.type == TxType.INCOME) IncomeMint else ExpenseCoral
                        val isSelected = idx == selectedIdx
                        if (isSelected) {
                            drawLine(
                                color = dotColor.copy(alpha = 0.4f),
                                start = Offset(pt.x, padY),
                                end = Offset(pt.x, padY + chartH),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = dotColor.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = pt
                            )
                        }
                        drawCircle(
                            color = dotColor,
                            radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 2.2.dp.toPx() else 1.2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    buckets: List<PeriodChartBucket>,
    currency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.incomeVsExpenseChart,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = IncomeMint, label = strings.income)
                    LegendDot(color = ExpenseCoral, label = strings.expense)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val maxVal = max(1L, buckets.maxOfOrNull { max(it.incomeMinor, it.expenseMinor) } ?: 1L)
            val animProgress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500),
                label = "bar_anim"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                val count = buckets.size.coerceAtLeast(1)
                val groupWidth = size.width / count
                val barWidth = (groupWidth * 0.28f).coerceAtMost(18.dp.toPx())
                val gap = 4.dp.toPx()
                val chartH = size.height - 8.dp.toPx()

                buckets.forEachIndexed { idx, b ->
                    val centerX = groupWidth * idx + groupWidth / 2f
                    val incH = (b.incomeMinor.toFloat() / maxVal.toFloat()) * chartH * animProgress
                    val expH = (b.expenseMinor.toFloat() / maxVal.toFloat()) * chartH * animProgress

                    if (incH > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(IncomeMint, IncomeMint.copy(alpha = 0.65f))),
                            topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incH),
                            size = Size(barWidth, incH.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                    if (expH > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(ExpenseCoral, ExpenseCoral.copy(alpha = 0.65f))),
                            topLeft = Offset(centerX + gap / 2f, size.height - expH),
                            size = Size(barWidth, expH.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                buckets.forEach { bucket ->
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChartCard(
    expensesByCategory: List<Pair<ExpenseCategory, Long>>,
    totalExpenseMinor: Long,
    currency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.categoryBreakdown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (expensesByCategory.isEmpty() || totalExpenseMinor <= 0L) {
                Text(
                    text = strings.noDataForPeriod,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(136.dp)) {
                            val strokeW = 24.dp.toPx()
                            var startAngle = -90f
                            expensesByCategory.forEach { (cat, amt) ->
                                val sweep = (amt.toFloat() / totalExpenseMinor.toFloat()) * 360f
                                drawArc(
                                    color = cat.topColor,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 2f).coerceAtLeast(1.5f),
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.expense,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = MoneyCalculator.formatAmount(totalExpenseMinor, currency, language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expensesByCategory.take(5).forEach { (cat, amt) ->
                            val pct = ((amt * 100.0) / totalExpenseMinor).roundToInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(cat.topColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.categoryName(cat, language),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${MoneyCalculator.localizeDigits(pct.toString(), language)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodBreakdownCard(
    byMethod: List<Pair<PaymentMethod, Long>>,
    totalMinor: Long,
    currency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.paymentMethodBreakdown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (byMethod.isEmpty() || totalMinor <= 0L) {
                Text(
                    text = strings.noDataForPeriod,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    byMethod.forEach { (method, amt) ->
                        val ratio = (amt.toFloat() / totalMinor.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon3DBadge(
                                icon = method.icon,
                                contentDescription = strings.paymentMethodName(method, language),
                                topColor = method.topColor,
                                bottomColor = method.bottomColor,
                                size = 36.dp,
                                iconSize = 18.dp,
                                cornerRadius = 11.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.paymentMethodName(method, language),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = MoneyCalculator.formatAmount(amt, currency, language),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = method.topColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
