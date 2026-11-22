package com.example

import com.example.model.AppLanguage
import com.example.model.BudgetItem
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.TransactionItem
import com.example.model.TxType
import com.example.util.AutoTextParser
import com.example.util.ExpenseAdvisorEngine
import com.example.util.MoneyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceLogicTest {

    @Test
    fun moneyCalculator_exactExpressionPrecision_BDT_and_KWD() {
        // Exact arithmetic expression with Bengali digits
        val bdtMinor = MoneyCalculator.parseExpressionToMinor("২৫০+১২০*২-৩০", Currency.BDT)
        assertEquals(46000L, bdtMinor) // 250 + 240 - 30 = 460.00 BDT

        // Exact 3-decimal KWD arithmetic
        val kwdMinor = MoneyCalculator.parseExpressionToMinor("4.500+1.255", Currency.KWD)
        assertEquals(5755L, kwdMinor) // 5.755 KWD = 5755 fils

        val formattedKwd = MoneyCalculator.formatAmount(5755L, Currency.KWD, AppLanguage.EN)
        assertEquals("د.ك 5.755", formattedKwd)
    }

    @Test
    fun autoTextParser_parsesBengaliBkashAndKuwaitKnet() {
        val bnParsed = AutoTextParser.parse("বিকাশ বাজার ৫৫০ টাকা", Currency.BDT)
        assertNotNull(bnParsed)
        assertEquals(Currency.BDT, bnParsed!!.currency)
        assertEquals(55000L, bnParsed.amountMinor)
        assertEquals(PaymentMethod.BKASH, bnParsed.paymentMethod)
        assertEquals(ExpenseCategory.FOOD, bnParsed.category)

        val kwParsed = AutoTextParser.parse("KNET fuel 4.500", Currency.BDT)
        assertNotNull(kwParsed)
        assertEquals(Currency.KWD, kwParsed!!.currency)
        assertEquals(4500L, kwParsed.amountMinor)
        assertEquals(PaymentMethod.KNET, kwParsed.paymentMethod)
        assertEquals(ExpenseCategory.TRANSPORT, kwParsed.category)
    }

    @Test
    fun expenseAdvisor_detectsBudgetOverrun() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionItem(
                id = "1",
                type = TxType.EXPENSE,
                amountMinor = 1500000L, // 15,000 BDT
                currency = Currency.BDT,
                category = ExpenseCategory.FOOD,
                paymentMethod = PaymentMethod.BKASH,
                title = "Groceries",
                note = "",
                occurredAtMillis = now,
                clientUpdatedAtMillis = now
            )
        )
        val budgets = listOf(
            BudgetItem(
                id = "BDT_OVERALL",
                currency = Currency.BDT,
                categoryId = "OVERALL",
                limitMinor = 1000000L, // 10,000 BDT
                clientUpdatedAtMillis = now
            )
        )
        val advice = ExpenseAdvisorEngine.generateAdvice(txs, budgets, Currency.BDT, AppLanguage.BN, now)
        assertTrue(advice.any { it.id == "overall_over" })
    }
}
