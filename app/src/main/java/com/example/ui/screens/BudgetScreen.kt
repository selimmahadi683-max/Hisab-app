package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.RecurringTemplate
import com.example.model.ReportPeriod
import com.example.model.TxType
import com.example.ui.components.Icon3DBadge
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeMint
import com.example.util.MoneyCalculator
import com.example.util.PeriodFilter
import com.example.viewmodel.MainUiState

@Composable
fun BudgetScreen(
    state: MainUiState,
    strings: AppStrings,
    onSaveBudget: (Currency, String, Long) -> Unit,
    onSaveRecurringTemplate: (TxType, Long, Currency, ExpenseCategory, PaymentMethod, String, Int) -> Unit,
    onLogRecurringNow: (RecurringTemplate) -> Unit,
    onDeleteRecurringTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = state.preferences.activeCurrency
    val lang = state.preferences.language

    var editingBudgetTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // categoryId to label
    var budgetAmountInput by remember { mutableStateOf("") }
    var showAddRecurringDialog by remember { mutableStateOf(false) }

    val monthExpenses = remember(state.transactions, currency) {
        PeriodFilter.filterByPeriod(state.transactions, ReportPeriod.MONTHLY)
            .filter { it.currency == currency && it.type == TxType.EXPENSE }
    }
    val spentByCategory = remember(monthExpenses) {
        monthExpenses.groupBy { it.category.id }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
    }

    val budgetMap = remember(state.budgets, currency) {
        state.budgets.filter { it.currency == currency }.associateBy { it.categoryId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Overall Monthly Budget Card
        item {
            val overallLimit = budgetMap["OVERALL"]?.limitMinor ?: 0L
            val totalSpent = state.currentMonthExpenseMinor
            val ratio = if (overallLimit > 0L) (totalSpent.toFloat() / overallLimit.toFloat()).coerceIn(0f, 1f) else 0f

            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        editingBudgetTarget = "OVERALL" to strings.overallBudgetTitle
                        budgetAmountInput = if (overallLimit > 0L) {
                            MoneyCalculator.formatMinorForInput(overallLimit, currency)
                        } else ""
                    }
                    .testTag("overall_budget_card"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon3DBadge(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = strings.overallBudgetTitle,
                                topColor = Color(0xFF34D399),
                                bottomColor = Color(0xFF047857),
                                size = 46.dp,
                                iconSize = 22.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.overallBudgetTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (overallLimit > 0L) {
                                        "${strings.limitLabel}: ${MoneyCalculator.formatAmount(overallLimit, currency, lang)}"
                                    } else {
                                        strings.tapToSetLimit
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = strings.edit,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (overallLimit > 0L) {
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = if (totalSpent > overallLimit) ExpenseCoral else IncomeMint,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.spentLabel}: ${MoneyCalculator.formatAmount(totalSpent, currency, lang)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            val rem = overallLimit - totalSpent
                            Text(
                                text = if (rem >= 0) {
                                    "${strings.budgetRemaining}: ${MoneyCalculator.formatAmount(rem, currency, lang)}"
                                } else {
                                    "${strings.budgetExceeded}: ${MoneyCalculator.formatAmount(-rem, currency, lang)}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rem >= 0) IncomeMint else ExpenseCoral
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Budgets Header
        item {
            Text(
                text = strings.categoryBudgetsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            )
        }

        items(ExpenseCategory.expenseCategories()) { cat ->
            val limit = budgetMap[cat.id]?.limitMinor ?: 0L
            val spent = spentByCategory[cat.id] ?: 0L
            val ratio = if (limit > 0L) (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
            val catName = strings.categoryName(cat, lang)

            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        editingBudgetTarget = cat.id to catName
                        budgetAmountInput = if (limit > 0L) {
                            MoneyCalculator.formatMinorForInput(limit, currency)
                        } else ""
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon3DBadge(
                            icon = cat.icon,
                            contentDescription = catName,
                            topColor = cat.topColor,
                            bottomColor = cat.bottomColor,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (limit > 0L) {
                                    "${MoneyCalculator.formatAmount(spent, currency, lang)} / ${MoneyCalculator.formatAmount(limit, currency, lang)}"
                                } else {
                                    "${strings.spentLabel}: ${MoneyCalculator.formatAmount(spent, currency, lang)} • ${strings.noLimitSet}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = strings.edit,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (limit > 0L) {
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (spent > limit) ExpenseCoral else cat.topColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Recurring 1-Tap Templates Section
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.recurringTemplatesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.recurringSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showAddRecurringDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addRecurring)
                }
            }
        }

        items(state.recurringTemplates, key = { it.id }) { tpl ->
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon3DBadge(
                        icon = Icons.Rounded.Repeat,
                        contentDescription = tpl.title,
                        topColor = tpl.category.topColor,
                        bottomColor = tpl.category.bottomColor,
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tpl.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = MoneyCalculator.formatAmount(tpl.amountMinor, tpl.currency, lang),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tpl.type == TxType.INCOME) IncomeMint else ExpenseCoral
                        )
                    }
                    Button(
                        onClick = { onLogRecurringNow(tpl) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.logNow)
                    }
                    IconButton(onClick = { onDeleteRecurringTemplate(tpl.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    // Edit Custom Budget Dialog
    editingBudgetTarget?.let { (catId, label) ->
        AlertDialog(
            onDismissRequest = { editingBudgetTarget = null },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = budgetAmountInput,
                    onValueChange = { budgetAmountInput = it },
                    label = { Text("${strings.limitLabel} (${currency.symbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val minor = if (budgetAmountInput.isBlank()) 0L
                    else MoneyCalculator.parseExpressionToMinor(budgetAmountInput, currency) ?: 0L
                    onSaveBudget(currency, catId, minor)
                    editingBudgetTarget = null
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBudgetTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Recurring Template Dialog
    if (showAddRecurringDialog) {
        var rTitle by remember { mutableStateOf("") }
        var rAmount by remember { mutableStateOf("") }
        var rCat by remember { mutableStateOf(ExpenseCategory.HOUSING) }
        AlertDialog(
            onDismissRequest = { showAddRecurringDialog = false },
            title = { Text(strings.addRecurring) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rTitle,
                        onValueChange = { rTitle = it },
                        label = { Text(strings.titleLabel) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rAmount,
                        onValueChange = { rAmount = it },
                        label = { Text("${strings.amountOrCalcLabel} (${currency.symbol})") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val minor = MoneyCalculator.parseExpressionToMinor(rAmount, currency)
                    if (minor != null && minor > 0L) {
                        onSaveRecurringTemplate(
                            TxType.EXPENSE,
                            minor,
                            currency,
                            rCat,
                            if (currency == Currency.KWD) PaymentMethod.KNET else PaymentMethod.BKASH,
                            rTitle.ifBlank { strings.categoryName(rCat, lang) },
                            1
                        )
                        showAddRecurringDialog = false
                    }
                }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRecurringDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
