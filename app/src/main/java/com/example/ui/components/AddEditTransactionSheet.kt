package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.util.AutoTextParser
import com.example.util.MoneyCalculator
import com.example.util.PeriodFilter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    initialTransaction: TransactionItem?,
    initialAutoMode: Boolean,
    defaultCurrency: Currency,
    language: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        type: TxType,
        amountMinor: Long,
        currency: Currency,
        category: ExpenseCategory,
        paymentMethod: PaymentMethod,
        title: String,
        note: String,
        occurredAtMillis: Long
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var isAutoMode by remember { mutableStateOf(initialAutoMode) }
    var autoText by remember { mutableStateOf("") }
    var autoFeedback by remember { mutableStateOf<String?>(null) }

    var type by remember { mutableStateOf(initialTransaction?.type ?: TxType.EXPENSE) }
    var currency by remember { mutableStateOf(initialTransaction?.currency ?: defaultCurrency) }
    var amountExpression by remember {
        mutableStateOf(
            initialTransaction?.let { MoneyCalculator.formatMinorForInput(it.amountMinor, it.currency) } ?: ""
        )
    }
    var category by remember {
        mutableStateOf(
            initialTransaction?.category ?: ExpenseCategory.FOOD
        )
    }
    var paymentMethod by remember {
        mutableStateOf(
            initialTransaction?.paymentMethod ?: if (currency == Currency.KWD) PaymentMethod.KNET else PaymentMethod.CASH
        )
    }
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var occurredAtMillis by remember {
        mutableLongStateOf(initialTransaction?.occurredAtMillis ?: System.currentTimeMillis())
    }
    var showCalculatorPad by remember { mutableStateOf(true) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val evaluatedValue = remember(amountExpression) {
        MoneyCalculator.evaluateExpression(amountExpression)
    }
    val evaluatedMinor = remember(amountExpression, currency) {
        MoneyCalculator.parseExpressionToMinor(amountExpression, currency)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction == null) strings.addTransaction else strings.editTransaction,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (initialTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialTransaction.id) },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Mode Toggle: Manual Calculator vs Auto Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isAutoMode,
                    onClick = { isAutoMode = false },
                    label = { Text(strings.manualEntry) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isAutoMode,
                    onClick = { isAutoMode = true },
                    label = { Text(strings.autoEntry) },
                    leadingIcon = {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Auto Text Parser Section
            AnimatedVisibility(visible = isAutoMode) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = autoText,
                            onValueChange = {
                                autoText = it
                                autoFeedback = null
                            },
                            label = { Text(strings.autoTextInputLabel) },
                            placeholder = { Text(strings.autoTextExampleHint, style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auto_text_input"),
                            maxLines = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboard.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        autoText = clip
                                        val parsed = AutoTextParser.parse(clip, currency)
                                        if (parsed != null) {
                                            currency = parsed.currency
                                            type = parsed.type
                                            category = parsed.category
                                            paymentMethod = parsed.paymentMethod
                                            title = parsed.title
                                            amountExpression = MoneyCalculator.formatMinorForInput(parsed.amountMinor, parsed.currency)
                                            autoFeedback = strings.autoParseSuccess
                                        } else {
                                            autoFeedback = strings.autoParseFailed
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.pasteAndParse, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    val parsed = AutoTextParser.parse(autoText, currency)
                                    if (parsed != null) {
                                        currency = parsed.currency
                                        type = parsed.type
                                        category = parsed.category
                                        paymentMethod = parsed.paymentMethod
                                        title = parsed.title
                                        amountExpression = MoneyCalculator.formatMinorForInput(parsed.amountMinor, parsed.currency)
                                        autoFeedback = strings.autoParseSuccess
                                        amountError = null
                                    } else {
                                        autoFeedback = strings.autoParseFailed
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auto_parse_button")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.autoParseBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        autoFeedback?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (msg == strings.autoParseSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Transaction Type & Currency Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = type == TxType.EXPENSE,
                    onClick = {
                        type = TxType.EXPENSE
                        if (category.isIncomeCategory) category = ExpenseCategory.FOOD
                    },
                    label = { Text(strings.expense) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == TxType.INCOME,
                    onClick = {
                        type = TxType.INCOME
                        if (!category.isIncomeCategory) category = ExpenseCategory.SALARY
                    },
                    label = { Text(strings.income) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = currency == Currency.BDT,
                    onClick = { currency = Currency.BDT },
                    label = { Text("৳ BDT") }
                )
                FilterChip(
                    selected = currency == Currency.KWD,
                    onClick = { currency = Currency.KWD },
                    label = { Text("د.ك KWD") }
                )
            }

            // Amount Input + Live Calculated Total
            OutlinedTextField(
                value = amountExpression,
                onValueChange = {
                    amountExpression = it
                    amountError = null
                },
                label = { Text(strings.amountOrCalcLabel) },
                prefix = {
                    Text(
                        text = "${currency.symbol} ",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TxType.EXPENSE) ExpenseCoral else IncomeMint
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showCalculatorPad = !showCalculatorPad }) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = strings.manualEntry,
                            tint = if (showCalculatorPad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingText = {
                    if (amountError != null) {
                        Text(amountError!!, color = MaterialTheme.colorScheme.error)
                    } else if (evaluatedMinor != null && amountExpression.any { it in "+-*/×÷" }) {
                        Text(
                            text = "= ${MoneyCalculator.formatAmount(evaluatedMinor, currency, language)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                isError = amountError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
            )

            // Built-in 100% Accurate Expression Calculator Keypad
            AnimatedVisibility(visible = showCalculatorPad) {
                CalculatorKeypad(
                    onKey = { key ->
                        amountError = null
                        when (key) {
                            "C" -> amountExpression = ""
                            "⌫" -> if (amountExpression.isNotEmpty()) amountExpression = amountExpression.dropLast(1)
                            "=" -> {
                                evaluatedValue?.let { bd ->
                                    val minor = MoneyCalculator.toMinorUnits(bd, currency)
                                    if (minor > 0L) {
                                        amountExpression = MoneyCalculator.formatMinorForInput(minor, currency)
                                    }
                                }
                            }
                            else -> amountExpression += key
                        }
                    }
                )
            }

            // Title & Note
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.titleLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input")
            )

            // 3D Categories Horizontal Strip
            Text(
                text = strings.categoryLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            val categories = remember(type) {
                if (type == TxType.EXPENSE) ExpenseCategory.expenseCategories()
                else ExpenseCategory.incomeCategories()
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categories) { cat ->
                    val selected = cat == category
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(74.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { category = cat }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon3DBadge(
                            icon = cat.icon,
                            contentDescription = strings.categoryName(cat, language),
                            topColor = cat.topColor,
                            bottomColor = cat.bottomColor,
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.categoryName(cat, language),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 3D Payment Methods Horizontal Strip
            Text(
                text = strings.paymentMethodLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(PaymentMethod.entries) { method ->
                    val selected = method == paymentMethod
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { paymentMethod = method }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon3DBadge(
                            icon = method.icon,
                            contentDescription = strings.paymentMethodName(method, language),
                            topColor = method.topColor,
                            bottomColor = method.bottomColor,
                            size = 30.dp,
                            iconSize = 15.dp,
                            cornerRadius = 9.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.paymentMethodName(method, language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Date & Note Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = occurredAtMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = occurredAtMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                occurredAtMillis = updated.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = strings.dateLabel, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(PeriodFilter.formatShortDate(occurredAtMillis, language))
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.noteLabel) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Save Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        val finalMinor = MoneyCalculator.parseExpressionToMinor(amountExpression, currency)
                        if (finalMinor == null || finalMinor <= 0L) {
                            amountError = strings.invalidAmountMsg
                            return@Button
                        }
                        val finalTitle = title.ifBlank { strings.categoryName(category, language) }
                        onSave(
                            initialTransaction?.id,
                            type,
                            finalMinor,
                            currency,
                            category,
                            paymentMethod,
                            finalTitle,
                            note,
                            occurredAtMillis
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TxType.EXPENSE) MaterialTheme.colorScheme.primary else IncomeMint
                    ),
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("save_transaction_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.save, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeypad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "+", "=")
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcKeyButton(label = "C", isAccent = true, modifier = Modifier.weight(1f)) { onKey("C") }
                CalcKeyButton(label = "⌫", isAccent = true, modifier = Modifier.weight(1f)) { onKey("⌫") }
            }
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "-", "+", "=")
                        CalcKeyButton(
                            label = key,
                            isAccent = isOp,
                            modifier = Modifier.weight(1f),
                            onClick = { onKey(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKeyButton(
    label: String,
    isAccent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isAccent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val fg = if (isAccent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}
