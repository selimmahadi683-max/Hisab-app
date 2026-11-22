package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Currency
import com.example.model.TransactionItem
import com.example.model.TxType
import com.example.ui.components.Icon3DBadge
import com.example.ui.components.TransactionHistoryChart
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeMint
import com.example.util.MoneyCalculator
import com.example.util.PeriodFilter
import com.example.viewmodel.MainUiState
import com.example.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: MainUiState,
    strings: AppStrings,
    onOpenManualEntry: () -> Unit,
    onOpenAutoEntry: () -> Unit,
    onExportMonthlyPdf: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAdvice: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = state.preferences.activeCurrency
    val lang = state.preferences.language

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 3D Hero Wallet & Monthly Budget Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF063A2D), Color(0xFF0D5C47), Color(0xFF072B23))
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_finance),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.32f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${strings.netBalance} (${strings.thisMonth})",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFA7F3D0)
                            )
                            val net = state.currentMonthIncomeMinor - state.currentMonthExpenseMinor
                            Text(
                                text = MoneyCalculator.formatAmount(net, currency, lang),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${currency.symbol} ${currency.code}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Income vs Expense Mini Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroMetricPill(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = strings.totalIncome,
                            value = MoneyCalculator.formatAmount(state.currentMonthIncomeMinor, currency, lang),
                            accent = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                        HeroMetricPill(
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            label = strings.totalExpense,
                            value = MoneyCalculator.formatAmount(state.currentMonthExpenseMinor, currency, lang),
                            accent = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Monthly Budget Tracker Strip
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.28f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToBudgets)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (state.overallMonthlyBudgetMinor <= 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.setBudgetPrompt,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Text(
                                        text = strings.tapToSetLimit,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                val budget = state.overallMonthlyBudgetMinor
                                val spent = state.currentMonthExpenseMinor
                                val ratio = (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
                                val pct = ((spent * 100.0) / budget).roundToInt()
                                val remaining = budget - spent
                                val safeDaily = max(0L, remaining) / MainViewModel.remainingDaysInCurrentMonth()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${strings.monthlyBudget}: ${MoneyCalculator.formatAmount(budget, currency, lang)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${MoneyCalculator.localizeDigits(pct.toString(), lang)}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (spent > budget) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (spent > budget) Color(0xFFF87171) else Color(0xFF34D399),
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (remaining >= 0) {
                                            "${strings.budgetRemaining}: ${MoneyCalculator.formatAmount(remaining, currency, lang)}"
                                        } else {
                                            "${strings.budgetExceeded}: ${MoneyCalculator.formatAmount(-remaining, currency, lang)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (remaining >= 0) Color(0xFFD1FAE5) else Color(0xFFFCA5A5)
                                    )
                                    if (remaining > 0) {
                                        Text(
                                            text = "${strings.dailySafeLimit}: ${MoneyCalculator.formatAmount(safeDaily, currency, lang)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Action 3D Options Grid
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAction3DCard(
                        title = strings.manualEntry,
                        icon = Icons.Rounded.Calculate,
                        topColor = Color(0xFF34D399),
                        bottomColor = Color(0xFF047857),
                        onClick = onOpenManualEntry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_manual_entry")
                    )
                    QuickAction3DCard(
                        title = strings.autoEntry,
                        icon = Icons.Rounded.AutoAwesome,
                        topColor = Color(0xFF38BDF8),
                        bottomColor = Color(0xFF0369A1),
                        onClick = onOpenAutoEntry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_auto_entry")
                    )
                    QuickAction3DCard(
                        title = strings.pdfReport,
                        icon = Icons.Rounded.PictureAsPdf,
                        topColor = Color(0xFFF43F5E),
                        bottomColor = Color(0xFFBE123C),
                        onClick = onExportMonthlyPdf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_pdf_export")
                    )
                    QuickAction3DCard(
                        title = strings.savingsTips,
                        icon = Icons.Rounded.Lightbulb,
                        topColor = Color(0xFFFBBF24),
                        bottomColor = Color(0xFFB45309),
                        onClick = onNavigateToAdvice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_savings_tips")
                    )
                }
            }
        }

        // 3. Transaction History Interactive Graph
        item {
            TransactionHistoryChart(
                transactions = state.transactions,
                currency = currency,
                language = lang,
                strings = strings,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // 4. Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentTransactions,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${MoneyCalculator.localizeDigits(state.transactions.size.toString(), lang)} ${strings.transactionsCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Empty State or Recent Transaction Rows
        if (state.transactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_ledger),
                            contentDescription = strings.emptyTransactionsTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Text(
                            text = strings.emptyTransactionsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.emptyTransactionsSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.transactions.take(15), key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    currency = currency,
                    language = lang,
                    strings = strings,
                    onClick = { onEditTransaction(tx) },
                    modifier = Modifier.widthIn(max = 640.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAction3DCard(
    title: String,
    icon: ImageVector,
    topColor: Color,
    bottomColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon3DBadge(
                icon = icon,
                contentDescription = title,
                topColor = topColor,
                bottomColor = bottomColor,
                size = 44.dp,
                iconSize = 22.dp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionItem,
    currency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = tx.type == TxType.INCOME
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon3DBadge(
                icon = tx.category.icon,
                contentDescription = strings.categoryName(tx.category, language),
                topColor = tx.category.topColor,
                bottomColor = tx.category.bottomColor,
                size = 44.dp,
                iconSize = 22.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title.ifBlank { strings.categoryName(tx.category, language) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${strings.paymentMethodName(tx.paymentMethod, language)}  •  ${PeriodFilter.formatShortDate(tx.occurredAtMillis, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = (if (isIncome) "+" else "-") + MoneyCalculator.formatAmount(tx.amountMinor, currency, language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeMint else ExpenseCoral
            )
        }
    }
}
