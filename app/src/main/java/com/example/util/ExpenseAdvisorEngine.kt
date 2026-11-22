package com.example.util

import com.example.localization.stringsFor
import com.example.model.AdviceSeverity
import com.example.model.AppLanguage
import com.example.model.BudgetItem
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.SavingsAdvice
import com.example.model.TransactionItem
import com.example.model.TxType
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * 100% offline deterministic advisor that inspects current month spending, category budgets,
 * month-over-month spikes, and frequent small discretionary expenses to give concrete savings advice.
 */
object ExpenseAdvisorEngine {

    fun generateAdvice(
        transactions: List<TransactionItem>,
        budgets: List<BudgetItem>,
        currency: Currency,
        language: AppLanguage,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SavingsAdvice> {
        val strings = stringsFor(language)
        val currencyTxs = transactions.filter { it.currency == currency }
        if (currencyTxs.isEmpty()) {
            return listOf(
                SavingsAdvice(
                    id = "empty",
                    title = when (language) {
                        AppLanguage.BN -> "নতুন হিসাব দিয়ে শুরু করুন"
                        AppLanguage.AR -> "ابدأ بتسجيل معاملاتك"
                        AppLanguage.EN -> "Start Logging to Unlock Insights"
                    },
                    description = when (language) {
                        AppLanguage.BN -> "প্রতিদিনের খরচ ও বাজেট যোগ করলে মাস শেষে কোন খাতে কত টাকা সাশ্রয় করা যাবে তার নিখুঁত পরামর্শ এখানে দেখতে পাবেন।"
                        AppLanguage.AR -> "عند تسجيل مصروفاتك اليومية، سنحلل إنفاقك ونقدم لك خططاً عملية لتقليل النفقات بنهاية الشهر."
                        AppLanguage.EN -> "Add your daily expenses and monthly budget to unlock personalized end-of-month savings strategies."
                    },
                    potentialSavingFormatted = null,
                    severity = AdviceSeverity.GOOD
                )
            )
        }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = max(1, daysInMonth - dayOfMonth + 1)

        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        val currentMonthExpenses = currencyTxs.filter {
            it.type == TxType.EXPENSE && isSameYearMonth(it.occurredAtMillis, curYear, curMonth)
        }
        val prevMonthExpenses = currencyTxs.filter {
            it.type == TxType.EXPENSE && isSameYearMonth(it.occurredAtMillis, prevYear, prevMonth)
        }
        val currentMonthIncome = currencyTxs.filter {
            it.type == TxType.INCOME && isSameYearMonth(it.occurredAtMillis, curYear, curMonth)
        }.sumOf { it.amountMinor }

        val totalSpent = currentMonthExpenses.sumOf { it.amountMinor }
        val prevTotalSpent = prevMonthExpenses.sumOf { it.amountMinor }
        val adviceList = mutableListOf<SavingsAdvice>()

        val overallBudget = budgets.firstOrNull { it.currency == currency && it.categoryId == "OVERALL" }?.limitMinor ?: 0L

        // 1. Overall Budget Check & Daily Burn Rate Projection
        if (overallBudget > 0L) {
            val pct = ((totalSpent * 100.0) / overallBudget).roundToLong()
            val projectedMonthSpend = if (dayOfMonth > 0) (totalSpent * daysInMonth) / dayOfMonth else totalSpent
            if (totalSpent > overallBudget) {
                val over = totalSpent - overallBudget
                val overStr = MoneyCalculator.formatAmount(over, currency, language)
                adviceList.add(
                    SavingsAdvice(
                        id = "overall_over",
                        title = when (language) {
                            AppLanguage.BN -> "মাসিক বাজেট অতিক্রম ($pct%)"
                            AppLanguage.AR -> "تم تجاوز الميزانية ($pct%)"
                            AppLanguage.EN -> "Monthly Budget Exceeded ($pct%)"
                        },
                        description = when (language) {
                            AppLanguage.BN -> "চলতি মাসে নির্ধারিত বাজেটের চেয়ে $overStr বেশি খরচ হয়েছে। পরবর্তী ৭ দিন অপ্রয়োজনীয় কেনাকাটা ও বাইরে খাওয়া বন্ধ রাখুন।"
                            AppLanguage.AR -> "لقد تجاوزت ميزانيتك بمقدار $overStr. أوقف المشتريات غير الضرورية خلال الأيام المتبقية."
                            AppLanguage.EN -> "You are $overStr above your monthly budget. Pause discretionary shopping and dining out for the rest of the month."
                        },
                        potentialSavingFormatted = overStr,
                        severity = AdviceSeverity.CRITICAL
                    )
                )
            } else if (projectedMonthSpend > overallBudget) {
                val projectedOver = projectedMonthSpend - overallBudget
                val safeDaily = (overallBudget - totalSpent) / remainingDays
                val safeStr = MoneyCalculator.formatAmount(safeDaily, currency, language)
                val saveStr = MoneyCalculator.formatAmount(projectedOver, currency, language)
                adviceList.add(
                    SavingsAdvice(
                        id = "burn_rate",
                        title = when (language) {
                            AppLanguage.BN -> "মাস শেষে বাজেট ছাড়িয়ে যাওয়ার ঝুঁকি"
                            AppLanguage.AR -> "معدل الإنفاق الحالي سيتجاوز الميزانية"
                            AppLanguage.EN -> "Spending Pace Alert"
                        },
                        description = when (language) {
                            AppLanguage.BN -> "বর্তমান হারে খরচ চললে মাস শেষে $saveStr বেশি খরচ হবে। বাকি $remainingDays দিন দৈনিক খরচ $safeStr-এর মধ্যে রাখুন।"
                            AppLanguage.AR -> "بهذا المعدل ستتجاوز ميزانيتك بمقدار $saveStr. التزم بإنفاق يومي لا يتجاوز $safeStr لبقية الشهر."
                            AppLanguage.EN -> "At your current pace, you'll exceed budget by $saveStr. Keep daily spending under $safeStr for the next $remainingDays days."
                        },
                        potentialSavingFormatted = saveStr,
                        severity = AdviceSeverity.WARNING
                    )
                )
            }
        }

        // 2. Category Over-Budget or Dominant Category Analysis
        val categoryTotals = currentMonthExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }

        val categoryBudgets = budgets.filter { it.currency == currency && it.categoryId != "OVERALL" }
            .associateBy { it.categoryId }

        for ((cat, spent) in categoryTotals) {
            val catLimit = categoryBudgets[cat.id]?.limitMinor ?: 0L
            val catName = strings.categoryName(cat, language)
            if (catLimit > 0L && spent > catLimit) {
                val excess = spent - catLimit
                val excessStr = MoneyCalculator.formatAmount(excess, currency, language)
                adviceList.add(
                    SavingsAdvice(
                        id = "cat_over_${cat.id}",
                        title = when (language) {
                            AppLanguage.BN -> "$catName খাতে অতিরিক্ত খরচ"
                            AppLanguage.AR -> "تجاوز في فئة $catName"
                            AppLanguage.EN -> "$catName Over Budget"
                        },
                        description = when (language) {
                            AppLanguage.BN -> "$catName খাতে বাজেটের চেয়ে $excessStr বেশি ব্যয় হয়েছে। আগামী মাসে এই খাতে ১০-১৫% খরচ কমানোর লক্ষ্য নিন।"
                            AppLanguage.AR -> "أنفقت $excessStr أكثر من المخصص لفئة $catName. حاول خفض هذا البند بنسبة 15%."
                            AppLanguage.EN -> "You spent $excessStr above your $catName limit. Target a 15% trim in this category."
                        },
                        potentialSavingFormatted = excessStr,
                        severity = AdviceSeverity.CRITICAL,
                        category = cat
                    )
                )
            }
        }

        // 3. Top Discretionary Category 15% Savings Target
        val topDiscretionary = categoryTotals.firstOrNull {
            it.key != ExpenseCategory.HOUSING && it.key != ExpenseCategory.EDUCATION && it.key != ExpenseCategory.HEALTH
        }
        if (topDiscretionary != null && totalSpent > 0L) {
            val cat = topDiscretionary.key
            val spent = topDiscretionary.value
            val sharePct = ((spent * 100.0) / totalSpent).roundToLong()
            val save15 = (spent * 15L) / 100L
            if (sharePct >= 22L && save15 > 0L) {
                val catName = strings.categoryName(cat, language)
                val spentStr = MoneyCalculator.formatAmount(spent, currency, language)
                val saveStr = MoneyCalculator.formatAmount(save15, currency, language)
                adviceList.add(
                    SavingsAdvice(
                        id = "top_cat_${cat.id}",
                        title = when (language) {
                            AppLanguage.BN -> "সর্বোচ্চ ব্যয়ের খাত: $catName ($sharePct%)"
                            AppLanguage.AR -> "أعلى فئة إنفاق: $catName ($sharePct%)"
                            AppLanguage.EN -> "Top Spending Driver: $catName ($sharePct%)"
                        },
                        description = when (language) {
                            AppLanguage.BN -> "মোট খরচের $sharePct% ($spentStr) গেছে $catName খাতে। শুধু এই খাতে ১৫% নিয়ন্ত্রণ করলে প্রতি মাসে $saveStr বাঁচানো সম্ভব।"
                            AppLanguage.AR -> "تستحوذ فئة $catName على $sharePct% ($spentStr) من مصروفاتك. تقليصها بنسبة 15% يوفر لك $saveStr شهرياً."
                            AppLanguage.EN -> "$catName accounts for $sharePct% ($spentStr) of your expenses. Trimming just 15% saves $saveStr every month."
                        },
                        potentialSavingFormatted = saveStr,
                        severity = AdviceSeverity.WARNING,
                        category = cat
                    )
                )
            }
        }

        // 4. Month-over-Month Spike Check
        if (prevTotalSpent > 0L && totalSpent > (prevTotalSpent * 115L) / 100L) {
            val spike = totalSpent - prevTotalSpent
            val spikeStr = MoneyCalculator.formatAmount(spike, currency, language)
            val pctIncrease = (((totalSpent - prevTotalSpent) * 100.0) / prevTotalSpent).roundToLong()
            adviceList.add(
                SavingsAdvice(
                    id = "mom_spike",
                    title = when (language) {
                        AppLanguage.BN -> "গত মাসের তুলনায় $pctIncrease% খরচ বেড়েছে"
                        AppLanguage.AR -> "ارتفاع الإنفاق بنسبة $pctIncrease% عن الشهر الماضي"
                        AppLanguage.EN -> "Spending Up $pctIncrease% vs Last Month"
                    },
                    description = when (language) {
                        AppLanguage.BN -> "গত মাসের চেয়ে এই মাসে $spikeStr বেশি খরচ হয়েছে। অপ্রয়োজনীয় সাবস্ক্রিপশন ও ছোট ছোট বাড়তি খরচ যাচাই করুন।"
                        AppLanguage.AR -> "أنفقت $spikeStr أكثر من الشهر السابق. راجع المشتريات الصغيرة والاشتراكات غير الضرورية."
                        AppLanguage.EN -> "You've spent $spikeStr more than last month. Review recurring subscriptions and impulse purchases."
                    },
                    potentialSavingFormatted = spikeStr,
                    severity = AdviceSeverity.WARNING
                )
            )
        }

        // 5. Positive Savings / 20% Rule Feedback
        if (currentMonthIncome > 0L) {
            val savings = currentMonthIncome - totalSpent
            val savingsRate = ((savings * 100.0) / currentMonthIncome).roundToLong()
            if (savingsRate >= 20L) {
                val savStr = MoneyCalculator.formatAmount(savings, currency, language)
                adviceList.add(
                    SavingsAdvice(
                        id = "good_savings",
                        title = when (language) {
                            AppLanguage.BN -> "দারুণ সঞ্চয়ের হার ($savingsRate%)!"
                            AppLanguage.AR -> "معدل ادخار ممتاز ($savingsRate%)!"
                            AppLanguage.EN -> "Healthy Savings Rate ($savingsRate%)!"
                        },
                        description = when (language) {
                            AppLanguage.BN -> "এই মাসে আপনার নিট সঞ্চয় $savStr। আয়ের ২০%+ সঞ্চয় ধরে রাখা আর্থিক নিরাপত্তার জন্য অত্যন্ত ভালো।"
                            AppLanguage.AR -> "صافي مدخراتك هذا الشهر $savStr. الحفاظ على ادخار 20% فأكثر يعزز استقرارك المالي."
                            AppLanguage.EN -> "You are saving $savStr this month. Maintaining a 20%+ savings rate builds strong financial resilience."
                        },
                        potentialSavingFormatted = null,
                        severity = AdviceSeverity.GOOD
                    )
                )
            } else if (totalSpent > 0L) {
                val targetSave = (currentMonthIncome * 20L) / 100L
                val gap = max(0L, targetSave - savings)
                if (gap > 0L) {
                    val gapStr = MoneyCalculator.formatAmount(gap, currency, language)
                    adviceList.add(
                        SavingsAdvice(
                            id = "savings_goal_20",
                            title = when (language) {
                                AppLanguage.BN -> "২০% সঞ্চয় লক্ষ্যমাত্রা"
                                AppLanguage.AR -> "هدف الادخار 20%"
                                AppLanguage.EN -> "20% Monthly Savings Goal"
                            },
                            description = when (language) {
                                AppLanguage.BN -> "মাসিক আয়ের অন্তত ২০% সঞ্চয় করতে আর $gapStr খরচ কমানোর চেষ্টা করুন।"
                                AppLanguage.AR -> "لتحقيق ادخار 20% من دخلك، حاول تقليل النفقات بمقدار $gapStr."
                                AppLanguage.EN -> "Reduce expenses by $gapStr to reach the recommended 20% monthly savings benchmark."
                            },
                            potentialSavingFormatted = gapStr,
                            severity = AdviceSeverity.WARNING
                        )
                    )
                }
            }
        }

        if (adviceList.isEmpty() && totalSpent > 0L) {
            val cut10 = (totalSpent * 10L) / 100L
            val cutStr = MoneyCalculator.formatAmount(cut10, currency, language)
            adviceList.add(
                SavingsAdvice(
                    id = "baseline_10",
                    title = when (language) {
                        AppLanguage.BN -> "খরচ নিয়ন্ত্রণে আছে — আরও ১০% সাশ্রয় করুন"
                        AppLanguage.AR -> "الإنفاق مستقر — وفر 10% إضافية"
                        AppLanguage.EN -> "Spending on Track — Save 10% More"
                    },
                    description = when (language) {
                        AppLanguage.BN -> "আপনার হিসাব স্বাভাবিক আছে। প্রতি মাসে ছোট ছোট খরচে ১০% কমালে অতিরিক্ত $cutStr সঞ্চয় হবে।"
                        AppLanguage.AR -> "وضعك المالي مستقر. خفض المصروفات اليومية بنسبة 10% يضيف $cutStr لمدخراتك."
                        AppLanguage.EN -> "Your budget is on track. Trimming 10% across discretionary items adds $cutStr to savings."
                    },
                    potentialSavingFormatted = cutStr,
                    severity = AdviceSeverity.GOOD
                )
            )
        }

        return adviceList
    }

    private fun isSameYearMonth(millis: Long, year: Int, month: Int): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }
}
