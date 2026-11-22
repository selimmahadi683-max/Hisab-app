package com.example.util

import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.TxType
import java.util.Locale

data class ParsedExpenseInput(
    val amountMinor: Long,
    val currency: Currency,
    val type: TxType,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    val title: String
)

/**
 * 100% offline intelligent parser for Bengali, Arabic, and English expense texts/SMS.
 * Supports patterns like:
 * - "বিকাশ বাজার ৫৫০ টাকা"
 * - "bKash groceries 1250"
 * - "KNET fuel 4.500"
 * - "বেতন ৪৫০০০ ব্যাংক"
 * - "مطعم كاش 3.250 دك"
 */
object AutoTextParser {

    fun parse(rawText: String, defaultCurrency: Currency): ParsedExpenseInput? {
        val normalizedDigits = MoneyCalculator.normalizeNumericText(rawText)
        val lower = normalizedDigits.lowercase(Locale.ENGLISH)

        val currency = when {
            lower.contains("kwd") || lower.contains("kd") || lower.contains("dinar") ||
                rawText.contains("د.ك") || rawText.contains("دينار") || rawText.contains("দিনার") ||
                lower.contains("knet") -> Currency.KWD
            lower.contains("bdt") || lower.contains("tk") || lower.contains("taka") ||
                rawText.contains("৳") || rawText.contains("টাকা") || rawText.contains("تاكا") ||
                lower.contains("bkash") || lower.contains("nagad") || lower.contains("rocket") ||
                rawText.contains("বিকাশ") || rawText.contains("নগদ") || rawText.contains("রকেট") -> Currency.BDT
            else -> defaultCurrency
        }

        val amountRegex = Regex("""(\d+(?:\.\d{1,3})?)""")
        val matches = amountRegex.findAll(normalizedDigits).toList()
        if (matches.isEmpty()) return null

        // Prefer decimal value or first meaningful number
        val chosenMatch = matches.firstOrNull { it.value.contains(".") } ?: matches.first()
        val amountMinor = MoneyCalculator.parseExpressionToMinor(chosenMatch.value, currency) ?: return null

        val type = when {
            containsAny(
                lower, rawText,
                "salary", "income", "bonus", "received", "profit", "credit",
                "বেতন", "আয়", "বোনাস", "লাভ", "পেয়েছি", "راتب", "دخل", "مكافأة"
            ) -> TxType.INCOME
            else -> TxType.EXPENSE
        }

        val paymentMethod = when {
            containsAny(lower, rawText, "bkash", "বিকাশ", "بكاش") -> PaymentMethod.BKASH
            containsAny(lower, rawText, "nagad", "নগদ ডিজিটাল", "ناغاد") -> PaymentMethod.NAGAD
            containsAny(lower, rawText, "rocket", "রকেট", "روكيت") -> PaymentMethod.ROCKET
            containsAny(lower, rawText, "knet", "কেনেট", "كي نت") -> PaymentMethod.KNET
            containsAny(lower, rawText, "bank", "card", "visa", "master", "transfer", "ব্যাংক", "কার্ড", "بنك", "بطاقة") -> PaymentMethod.BANK
            else -> PaymentMethod.CASH
        }

        val category = if (type == TxType.INCOME) {
            when {
                containsAny(lower, rawText, "business", "sale", "client", "ব্যবসা", "বিক্রি", "عمل", "تجارة") -> ExpenseCategory.BUSINESS
                containsAny(lower, rawText, "bonus", "saving", "বোনাস", "সঞ্চয়", "مكافأة", "ادخار") -> ExpenseCategory.SAVINGS_INCOME
                else -> ExpenseCategory.SALARY
            }
        } else {
            when {
                containsAny(lower, rawText, "food", "lunch", "dinner", "breakfast", "tea", "coffee", "bazar", "grocery", "rice", "fish", "meat", "restaurant", "খাবার", "বাজার", "নাস্তা", "চা", "ভাত", "মাছ", "রেস্টুরেন্ট", "طعام", "مطعم", "غداء", "بقالة", "قهوة") -> ExpenseCategory.FOOD
                containsAny(lower, rawText, "uber", "pathao", "bus", "rickshaw", "fuel", "petrol", "diesel", "taxi", "train", "metro", "ভাড়া গাড়ি", "রিক্সা", "বাস", "গাড়ি", "তেল", "সিএনজি", "উবার", "পাঠাও", "مواصلات", "تاكسي", "بنزين", "وقود") -> ExpenseCategory.TRANSPORT
                containsAny(lower, rawText, "rent", "house", "flat", "apartment", "বাসা", "ভাড়া", "রুম", "إيجار", "سكن", "شقة") -> ExpenseCategory.HOUSING
                containsAny(lower, rawText, "bill", "electric", "gas", "water", "wifi", "internet", "recharge", "mobile", "flexi", "বিল", "বিদ্যুৎ", "গ্যাস", "পানি", "ইন্টারনেট", "রিচার্জ", "নেট", "فاتورة", "كهرباء", "ماء", "إنترنت", "شحن") -> ExpenseCategory.BILLS
                containsAny(lower, rawText, "doctor", "medicine", "pharmacy", "hospital", "clinic", "health", "ডাক্তার", "ঔষধ", "ওষুধ", "হাসপাতাল", "চিকিৎসা", "طبيب", "دواء", "مستشفى", "صيدلية") -> ExpenseCategory.HEALTH
                containsAny(lower, rawText, "shop", "shirt", "shoe", "clothes", "dress", "market", "mall", "কেনাকাটা", "জামা", "জুতা", "শপিং", "تسوق", "ملابس") -> ExpenseCategory.SHOPPING
                containsAny(lower, rawText, "school", "college", "book", "tuition", "course", "fee", "বই", "স্কুল", "কলেজ", "কোর্স", "শিক্ষা", "مدرسة", "كتاب", "تعليم") -> ExpenseCategory.EDUCATION
                containsAny(lower, rawText, "family", "gift", "parents", "eid", "home send", "পরিবার", "উপহার", "মা", "বাবা", "বাড়ি", "عائلة", "هدية") -> ExpenseCategory.FAMILY
                containsAny(lower, rawText, "movie", "game", "netflix", "tour", "travel", "বিনোদন", "সিনেমা", "গেম", "ভ্রমণ", "ترفيه", "سفر") -> ExpenseCategory.ENTERTAINMENT
                else -> ExpenseCategory.OTHER_EXPENSE
            }
        }

        val cleanedTitle = rawText
            .replace(Regex("""[০-৯٠-٩0-9]+(?:[.,٫][০-৯٠-٩0-9]+)?"""), "")
            .replace(Regex("""(?i)\b(bdt|kwd|tk|taka|kd|dinar)\b"""), "")
            .replace("৳", "")
            .replace("টাকা", "")
            .replace("দ.ক", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(60)
            .ifBlank { category.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } }

        return ParsedExpenseInput(
            amountMinor = amountMinor,
            currency = currency,
            type = type,
            category = category,
            paymentMethod = paymentMethod,
            title = cleanedTitle
        )
    }

    private fun containsAny(lower: String, raw: String, vararg terms: String): Boolean {
        return terms.any { term -> lower.contains(term) || raw.contains(term) }
    }
}
