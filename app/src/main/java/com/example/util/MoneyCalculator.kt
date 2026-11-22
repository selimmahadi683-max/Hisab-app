package com.example.util

import com.example.model.AppLanguage
import com.example.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 100% deterministic financial calculation engine using BigDecimal and integer minor units
 * (Poisha for BDT [100], Fils for KWD [1000]) so floating-point precision drift never happens.
 */
object MoneyCalculator {

    /**
     * Converts Bengali (০-৯) and Arabic-Indic (٠-٩) digits to ASCII digits (0-9)
     * and normalizes calculator symbols (× -> *, ÷ -> /).
     */
    fun normalizeNumericText(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '০'..'৯' -> sb.append(('0'.code + (ch.code - '০'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                '×' -> sb.append('*')
                '÷' -> sb.append('/')
                '−' -> sb.append('-')
                '٫' -> sb.append('.')
                ',' -> {} // strip thousand separators
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Evaluates an arithmetic expression like "250+120*2-30" or "4.500+1.250"
     * and returns exact minor units (Long), or null if invalid / <= 0.
     */
    fun parseExpressionToMinor(rawExpression: String, currency: Currency): Long? {
        val expr = normalizeNumericText(rawExpression).replace(" ", "")
        if (expr.isBlank()) return null
        val value = evaluateBigDecimal(expr) ?: return null
        if (value <= BigDecimal.ZERO) return null
        return toMinorUnits(value, currency)
    }

    /**
     * Evaluates arithmetic expression without requiring > 0 (e.g. for live preview).
     */
    fun evaluateExpression(rawExpression: String): BigDecimal? {
        val expr = normalizeNumericText(rawExpression).replace(" ", "")
        if (expr.isBlank()) return null
        return evaluateBigDecimal(expr)
    }

    fun toMinorUnits(amount: BigDecimal, currency: Currency): Long {
        val scaled = amount.multiply(BigDecimal(currency.minorFactor))
            .setScale(0, RoundingMode.HALF_UP)
        return scaled.toLong()
    }

    fun fromMinorUnits(minor: Long, currency: Currency): BigDecimal {
        return BigDecimal(minor).divide(BigDecimal(currency.minorFactor), currency.decimals, RoundingMode.HALF_UP)
    }

    fun formatMinorForInput(minor: Long, currency: Currency): String {
        val bd = fromMinorUnits(minor, currency)
        return if (currency == Currency.BDT && minor % 100L == 0L) {
            bd.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
        } else {
            bd.stripTrailingZeros().toPlainString().let { str ->
                if (currency == Currency.KWD && str.contains(".")) {
                    bd.setScale(3, RoundingMode.HALF_UP).toPlainString()
                } else {
                    str
                }
            }
        }
    }

    fun formatAmount(
        minor: Long,
        currency: Currency,
        language: AppLanguage = AppLanguage.EN,
        includeSymbol: Boolean = true
    ): String {
        val negative = minor < 0
        val absMinor = kotlin.math.abs(minor)
        val bd = fromMinorUnits(absMinor, currency)

        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = when (currency) {
            Currency.KWD -> "#,##0.000"
            Currency.BDT -> if (absMinor % 100L == 0L) "#,##0" else "#,##0.00"
        }
        val formattedNumber = DecimalFormat(pattern, symbols).format(bd)
        val localizedDigits = localizeDigits(formattedNumber, language)
        val sign = if (negative) "-" else ""
        return if (includeSymbol) {
            "$sign${currency.symbol} $localizedDigits"
        } else {
            "$sign$localizedDigits"
        }
    }

    fun localizeDigits(text: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.BN -> text.map { ch ->
                if (ch in '0'..'9') ('০'.code + (ch.code - '0'.code)).toChar() else ch
            }.joinToString("")
            AppLanguage.AR -> text.map { ch ->
                if (ch in '0'..'9') ('٠'.code + (ch.code - '0'.code)).toChar() else ch
            }.joinToString("")
            AppLanguage.EN -> text
        }
    }

    private fun evaluateBigDecimal(expr: String): BigDecimal? {
        return try {
            val parser = object {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    pos++
                    ch = if (pos < expr.length) expr[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): BigDecimal? {
                    nextChar()
                    val x = parseExpression() ?: return null
                    if (pos < expr.length) return null
                    return x
                }

                fun parseExpression(): BigDecimal? {
                    var x = parseTerm() ?: return null
                    while (true) {
                        x = when {
                            eat('+'.code) -> x.add(parseTerm() ?: return null)
                            eat('-'.code) -> x.subtract(parseTerm() ?: return null)
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): BigDecimal? {
                    var x = parseFactor() ?: return null
                    while (true) {
                        x = when {
                            eat('*'.code) -> x.multiply(parseFactor() ?: return null)
                            eat('/'.code) -> {
                                val denom = parseFactor() ?: return null
                                if (denom.compareTo(BigDecimal.ZERO) == 0) return null
                                x.divide(denom, 6, RoundingMode.HALF_UP)
                            }
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): BigDecimal? {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return parseFactor()?.negate()

                    val startPos = pos
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    if (pos == startPos) return null
                    val numStr = expr.substring(startPos, pos)
                    if (numStr.count { it == '.' } > 1 || numStr == ".") return null
                    return numStr.toBigDecimalOrNull()
                }
            }
            parser.parse()
        } catch (_: Exception) {
            null
        }
    }
}
