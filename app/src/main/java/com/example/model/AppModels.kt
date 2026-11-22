package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.automirrored.rounded.SendToMobile
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    BN("bn", "বাংলা", false),
    EN("en", "English", false),
    AR("ar", "العربية", true);

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: BN
    }
}

enum class Currency(
    val code: String,
    val symbol: String,
    val decimals: Int,
    val minorFactor: Long
) {
    BDT("BDT", "৳", 2, 100L),
    KWD("KWD", "د.ك", 3, 1000L);

    companion object {
        fun fromCode(code: String?): Currency =
            entries.firstOrNull { it.code == code } ?: BDT
    }
}

enum class TxType {
    EXPENSE,
    INCOME;

    companion object {
        fun fromName(raw: String?): TxType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class ReportPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME
}

enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT;

    companion object {
        fun fromName(raw: String?): ThemePreference =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class ExpenseCategory(
    val id: String,
    val isIncomeCategory: Boolean,
    val icon: ImageVector,
    val topColor: Color,
    val bottomColor: Color
) {
    FOOD("food", false, Icons.Rounded.Restaurant, Color(0xFFFF8A65), Color(0xFFD84315)),
    TRANSPORT("transport", false, Icons.Rounded.DirectionsCar, Color(0xFF4FC3F7), Color(0xFF0277BD)),
    SHOPPING("shopping", false, Icons.Rounded.ShoppingBag, Color(0xFFF06292), Color(0xFFC2185B)),
    BILLS("bills", false, Icons.Rounded.Bolt, Color(0xFFFFD54F), Color(0xFFEF6C00)),
    HOUSING("housing", false, Icons.Rounded.Home, Color(0xFF9575CD), Color(0xFF512DA8)),
    HEALTH("health", false, Icons.Rounded.LocalHospital, Color(0xFFEF5350), Color(0xFFB71C1C)),
    EDUCATION("education", false, Icons.Rounded.School, Color(0xFF4DB6AC), Color(0xFF00695C)),
    FAMILY("family", false, Icons.Rounded.FamilyRestroom, Color(0xFFBA68C8), Color(0xFF7B1FA2)),
    ENTERTAINMENT("entertainment", false, Icons.Rounded.SportsEsports, Color(0xFF7986CB), Color(0xFF303F9F)),
    OTHER_EXPENSE("other_expense", false, Icons.Rounded.MoreHoriz, Color(0xFF90A4AE), Color(0xFF455A64)),
    SALARY("salary", true, Icons.Rounded.Payments, Color(0xFF34D399), Color(0xFF047857)),
    BUSINESS("business", true, Icons.Rounded.BusinessCenter, Color(0xFF38BDF8), Color(0xFF0369A1)),
    SAVINGS_INCOME("savings_income", true, Icons.Rounded.Savings, Color(0xFFFBBF24), Color(0xFFB45309));

    companion object {
        fun fromId(id: String?): ExpenseCategory =
            entries.firstOrNull { it.id == id } ?: OTHER_EXPENSE

        fun expenseCategories(): List<ExpenseCategory> =
            entries.filter { !it.isIncomeCategory }

        fun incomeCategories(): List<ExpenseCategory> =
            entries.filter { it.isIncomeCategory }
    }
}

enum class PaymentMethod(
    val id: String,
    val icon: ImageVector,
    val topColor: Color,
    val bottomColor: Color
) {
    CASH("cash", Icons.Rounded.AccountBalanceWallet, Color(0xFF4ADE80), Color(0xFF15803D)),
    BKASH("bkash", Icons.Rounded.PhoneAndroid, Color(0xFFF43F5E), Color(0xFFBE123C)),
    NAGAD("nagad", Icons.AutoMirrored.Rounded.SendToMobile, Color(0xFFFB923C), Color(0xFFC2410C)),
    ROCKET("rocket", Icons.Rounded.RocketLaunch, Color(0xFFA855F7), Color(0xFF6B21A8)),
    BANK("bank", Icons.Rounded.AccountBalance, Color(0xFF38BDF8), Color(0xFF0369A1)),
    KNET("knet", Icons.Rounded.CreditCard, Color(0xFF2DD4BF), Color(0xFF0F766E));

    companion object {
        fun fromId(id: String?): PaymentMethod =
            entries.firstOrNull { it.id == id } ?: CASH
    }
}

enum class AdviceSeverity {
    CRITICAL,
    WARNING,
    GOOD
}

data class SavingsAdvice(
    val id: String,
    val title: String,
    val description: String,
    val potentialSavingFormatted: String?,
    val severity: AdviceSeverity,
    val category: ExpenseCategory? = null
)

data class TransactionItem(
    val id: String,
    val type: TxType,
    val amountMinor: Long,
    val currency: Currency,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    val title: String,
    val note: String,
    val occurredAtMillis: Long,
    val clientUpdatedAtMillis: Long
)

data class BudgetItem(
    val id: String,
    val currency: Currency,
    val categoryId: String, // "OVERALL" or ExpenseCategory.id
    val limitMinor: Long,
    val clientUpdatedAtMillis: Long
)

data class RecurringTemplate(
    val id: String,
    val type: TxType,
    val amountMinor: Long,
    val currency: Currency,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
    val title: String,
    val dayOfMonth: Int
)

sealed interface CloudSyncStatus {
    data object OfflineLocalOnly : CloudSyncStatus
    data object Idle : CloudSyncStatus
    data object Syncing : CloudSyncStatus
    data class Synced(val timestampMillis: Long) : CloudSyncStatus
    data class Error(val message: String) : CloudSyncStatus
}
