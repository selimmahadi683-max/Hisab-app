package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AdviceSeverity
import com.example.model.ReportPeriod
import com.example.model.SavingsAdvice
import com.example.model.TransactionItem
import com.example.ui.components.CategoryDonutChartCard
import com.example.ui.components.Icon3DBadge
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.PaymentMethodBreakdownCard
import com.example.ui.components.TransactionHistoryChart
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeMint
import com.example.ui.theme.WarningAmber
import com.example.util.MoneyCalculator
import com.example.viewmodel.MainUiState

@Composable
fun AnalyticsScreen(
    state: MainUiState,
    strings: AppStrings,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onSearchChange: (String) -> Unit,
    onGeneratePdf: (ReportPeriod) -> Unit,
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
        // 1. Period Switcher (Weekly / Monthly / Yearly / All-Time)
        item {
            LazyRow(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReportPeriod.entries) { period ->
                    FilterChip(
                        selected = period == state.selectedPeriod,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(strings.periodName(period)) }
                    )
                }
            }
        }

        // 2. Period KPI Row
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = strings.totalIncome,
                    value = MoneyCalculator.formatAmount(state.periodIncomeMinor, currency, lang),
                    accent = IncomeMint,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = strings.totalExpense,
                    value = MoneyCalculator.formatAmount(state.periodExpenseMinor, currency, lang),
                    accent = ExpenseCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Transaction History Interactive Graph
        item {
            TransactionHistoryChart(
                transactions = state.filteredPeriodTransactions,
                currency = currency,
                language = lang,
                strings = strings,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // 4. Income vs Expense Bar Chart
        item {
            IncomeExpenseBarChart(
                buckets = state.periodChartBuckets,
                currency = currency,
                language = lang,
                strings = strings,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // 5. Category Donut Chart
        item {
            CategoryDonutChartCard(
                expensesByCategory = state.periodExpensesByCategory,
                totalExpenseMinor = state.periodExpenseMinor,
                currency = currency,
                language = lang,
                strings = strings,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // 6. Payment Methods Breakdown
        item {
            PaymentMethodBreakdownCard(
                byMethod = state.periodExpensesByMethod,
                totalMinor = state.periodExpenseMinor,
                currency = currency,
                language = lang,
                strings = strings,
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        // 7. Month-End Expense Reduction Advisor
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.monthEndAdviceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.adviceList.forEach { advice ->
                    SavingsAdviceCard(advice = advice)
                }
            }
        }

        // 8. Instant PDF Report Export Banner (Weekly / Monthly / Yearly)
        item {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_pdf_report),
                            contentDescription = strings.exportPdfTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.exportPdfTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.exportPdfSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onGeneratePdf(ReportPeriod.WEEKLY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.periodWeekly, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { onGeneratePdf(ReportPeriod.MONTHLY) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_pdf_monthly_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.periodMonthly, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { onGeneratePdf(ReportPeriod.YEARLY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.periodYearly, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 9. Searchable Full Transaction History for Selected Period
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.allTransactions,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(state.filteredPeriodTransactions, key = { it.id }) { tx ->
            TransactionRowCard(
                tx = tx,
                currency = currency,
                language = lang,
                strings = strings,
                onClick = { onEditTransaction(tx) },
                modifier = Modifier.widthIn(max = 640.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavingsAdviceCard(advice: SavingsAdvice) {
    val (topCol, botCol, icon) = when (advice.severity) {
        AdviceSeverity.CRITICAL -> Triple(Color(0xFFF87171), Color(0xFFB91C1C), Icons.Rounded.Warning)
        AdviceSeverity.WARNING -> Triple(WarningAmber, Color(0xFFB45309), Icons.Rounded.TipsAndUpdates)
        AdviceSeverity.GOOD -> Triple(IncomeMint, Color(0xFF047857), Icons.Rounded.CheckCircle)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon3DBadge(
                icon = advice.category?.icon ?: icon,
                contentDescription = advice.title,
                topColor = topCol,
                bottomColor = botCol,
                size = 42.dp,
                iconSize = 20.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = advice.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                advice.potentialSavingFormatted?.let { saveAmt ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = saveAmt,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = topCol,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(topCol.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
