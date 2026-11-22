package com.example.util

import com.example.model.AppLanguage
import com.example.model.Currency
import com.example.model.ReportPeriod
import com.example.model.TransactionItem
import com.example.model.TxType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PeriodChartBucket(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long
)

object PeriodFilter {

    fun filterByPeriod(
        transactions: List<TransactionItem>,
        period: ReportPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TransactionItem> {
        if (period == ReportPeriod.ALL_TIME) return transactions
        val startMillis = periodStartMillis(period, nowMillis)
        return transactions.filter { it.occurredAtMillis >= startMillis }
    }

    fun periodStartMillis(period: ReportPeriod, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (period) {
            ReportPeriod.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, -6)
            ReportPeriod.MONTHLY -> cal.set(Calendar.DAY_OF_MONTH, 1)
            ReportPeriod.YEARLY -> cal.set(Calendar.DAY_OF_YEAR, 1)
            ReportPeriod.ALL_TIME -> return 0L
        }
        return cal.timeInMillis
    }

    fun buildChartBuckets(
        transactions: List<TransactionItem>,
        currency: Currency,
        period: ReportPeriod,
        language: AppLanguage,
        nowMillis: Long = System.currentTimeMillis()
    ): List<PeriodChartBucket> {
        val currencyTxs = transactions.filter { it.currency == currency }
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val locale = when (language) {
            AppLanguage.BN -> Locale.Builder().setLanguage("bn").setRegion("BD").build()
            AppLanguage.AR -> Locale.Builder().setLanguage("ar").setRegion("KW").build()
            AppLanguage.EN -> Locale.ENGLISH
        }

        return when (period) {
            ReportPeriod.WEEKLY -> {
                val dayFmt = SimpleDateFormat("EEE", locale)
                (6 downTo 0).map { daysAgo ->
                    val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                    val y = c.get(Calendar.YEAR)
                    val d = c.get(Calendar.DAY_OF_YEAR)
                    val dayTxs = currencyTxs.filter {
                        val tc = Calendar.getInstance().apply { timeInMillis = it.occurredAtMillis }
                        tc.get(Calendar.YEAR) == y && tc.get(Calendar.DAY_OF_YEAR) == d
                    }
                    PeriodChartBucket(
                        label = dayFmt.format(c.time),
                        incomeMinor = dayTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor },
                        expenseMinor = dayTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
                    )
                }
            }
            ReportPeriod.MONTHLY -> {
                // 4 week buckets for the current month
                val monthTxs = filterByPeriod(currencyTxs, ReportPeriod.MONTHLY, nowMillis)
                val weekRanges = listOf(1..7, 8..14, 15..21, 22..31)
                weekRanges.mapIndexed { idx, range ->
                    val bucketTxs = monthTxs.filter {
                        val tc = Calendar.getInstance().apply { timeInMillis = it.occurredAtMillis }
                        tc.get(Calendar.DAY_OF_MONTH) in range
                    }
                    val labelPrefix = when (language) {
                        AppLanguage.BN -> "স-${MoneyCalculator.localizeDigits((idx + 1).toString(), language)}"
                        AppLanguage.AR -> "أ${MoneyCalculator.localizeDigits((idx + 1).toString(), language)}"
                        AppLanguage.EN -> "W${idx + 1}"
                    }
                    PeriodChartBucket(
                        label = labelPrefix,
                        incomeMinor = bucketTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor },
                        expenseMinor = bucketTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
                    )
                }
            }
            ReportPeriod.YEARLY -> {
                val monthFmt = SimpleDateFormat("MMM", locale)
                val year = cal.get(Calendar.YEAR)
                (0..11).map { monthIdx ->
                    val c = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, monthIdx)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val mTxs = currencyTxs.filter {
                        val tc = Calendar.getInstance().apply { timeInMillis = it.occurredAtMillis }
                        tc.get(Calendar.YEAR) == year && tc.get(Calendar.MONTH) == monthIdx
                    }
                    PeriodChartBucket(
                        label = monthFmt.format(c.time).take(3),
                        incomeMinor = mTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor },
                        expenseMinor = mTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
                    )
                }
            }
            ReportPeriod.ALL_TIME -> {
                val years = currencyTxs.map {
                    Calendar.getInstance().apply { timeInMillis = it.occurredAtMillis }.get(Calendar.YEAR)
                }.distinct().sorted()
                val activeYears = years.ifEmpty { listOf(cal.get(Calendar.YEAR)) }
                activeYears.map { yr ->
                    val yTxs = currencyTxs.filter {
                        Calendar.getInstance().apply { timeInMillis = it.occurredAtMillis }.get(Calendar.YEAR) == yr
                    }
                    PeriodChartBucket(
                        label = MoneyCalculator.localizeDigits(yr.toString(), language),
                        incomeMinor = yTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor },
                        expenseMinor = yTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
                    )
                }
            }
        }
    }

    fun formatShortDate(millis: Long, language: AppLanguage): String {
        val locale = when (language) {
            AppLanguage.BN -> Locale.Builder().setLanguage("bn").setRegion("BD").build()
            AppLanguage.AR -> Locale.Builder().setLanguage("ar").setRegion("KW").build()
            AppLanguage.EN -> Locale.ENGLISH
        }
        return SimpleDateFormat("dd MMM, hh:mm a", locale).format(Date(millis))
    }
}
