package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.BudgetItem
import com.example.model.Currency
import com.example.model.ReportPeriod
import com.example.model.TransactionItem
import com.example.model.TxType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedPdfResult(
    val shareUri: Uri,
    val fileName: String,
    val savedToDownloads: Boolean
)

/**
 * Generates clean, multi-page A4 PDF financial reports 100% offline using Android's PdfDocument.
 * Uses FileProvider in app cacheDir + optional MediaStore.Downloads save (zero storage permissions needed).
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 points
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateReport(
        context: Context,
        transactions: List<TransactionItem>,
        budgets: List<BudgetItem>,
        currency: Currency,
        period: ReportPeriod,
        language: AppLanguage
    ): GeneratedPdfResult {
        val strings = stringsFor(language)
        val filtered = PeriodFilter.filterByPeriod(transactions, period)
            .filter { it.currency == currency }
            .sortedByDescending { it.occurredAtMillis }

        val totalIncome = filtered.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
        val totalExpense = filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
        val net = totalIncome - totalExpense
        val overallBudget = budgets.firstOrNull { it.currency == currency && it.categoryId == "OVERALL" }?.limitMinor ?: 0L
        val adviceList = ExpenseAdvisorEngine.generateAdvice(transactions, budgets, currency, language)

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        val headerBgPaint = Paint().apply { color = Color.parseColor("#0E7C5B") }
        val cardBgPaint = Paint().apply { color = Color.parseColor("#F0F7F4") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#D1E5DD")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1FAE5")
            textSize = 11f
        }
        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F241D")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = 10.5f
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
        }
        val incomePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val expensePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        // Top Branded Header
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 68f),
            12f, 12f, headerBgPaint
        )
        canvas.drawText("${strings.appTitle} — ${strings.periodName(period)} ${strings.pdfReport}", MARGIN + 16f, y + 28f, titlePaint)
        val generatedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())
        canvas.drawText("${currency.code} (${currency.symbol})  •  $generatedDate", MARGIN + 16f, y + 48f, subtitlePaint)
        y += 84f

        // KPI Summary Cards
        val boxWidth = (PAGE_WIDTH - 2 * MARGIN - 16f) / 3f
        val kpis = listOf(
            Triple(strings.totalIncome, MoneyCalculator.formatAmount(totalIncome, currency, language), incomePaint),
            Triple(strings.totalExpense, MoneyCalculator.formatAmount(totalExpense, currency, language), expensePaint),
            Triple(strings.netBalance, MoneyCalculator.formatAmount(net, currency, language), if (net >= 0) incomePaint else expensePaint)
        )
        kpis.forEachIndexed { idx, (label, value, valuePaint) ->
            val left = MARGIN + idx * (boxWidth + 8f)
            val rect = RectF(left, y, left + boxWidth, y + 54f)
            canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            canvas.drawText(label, left + 10f, y + 20f, mutedPaint)
            canvas.drawText(value, left + 10f, y + 40f, valuePaint)
        }
        y += 70f

        if (overallBudget > 0L) {
            val budgetStr = "${strings.monthlyBudget}: ${MoneyCalculator.formatAmount(overallBudget, currency, language)}"
            canvas.drawText(budgetStr, MARGIN, y, bodyPaint)
            y += 18f
        }

        // Category Summary
        val expensesByCategory = filtered.filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }

        if (expensesByCategory.isNotEmpty()) {
            ensureSpace(32f)
            canvas.drawText(strings.categoryBreakdown, MARGIN, y + 10f, sectionPaint)
            y += 22f
            for ((cat, sum) in expensesByCategory) {
                ensureSpace(18f)
                val pct = if (totalExpense > 0L) ((sum * 100.0) / totalExpense).toInt() else 0
                val catLabel = "${strings.categoryName(cat, language)} ($pct%)"
                val amtStr = MoneyCalculator.formatAmount(sum, currency, language)
                canvas.drawText(catLabel, MARGIN + 6f, y, bodyPaint)
                canvas.drawText(amtStr, PAGE_WIDTH - MARGIN - expensePaint.measureText(amtStr), y, bodyPaint)
                y += 16f
            }
            y += 10f
        }

        // Savings Advice
        if (adviceList.isNotEmpty()) {
            ensureSpace(40f)
            canvas.drawText(strings.monthEndAdviceTitle, MARGIN, y + 10f, sectionPaint)
            y += 22f
            for (advice in adviceList.take(3)) {
                val textWidth = (PAGE_WIDTH - 2 * MARGIN - 16f).toInt()
                val layout = StaticLayout.Builder.obtain(
                    "• ${advice.title}: ${advice.description}",
                    0,
                    "• ${advice.title}: ${advice.description}".length,
                    bodyPaint,
                    textWidth
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                ensureSpace(layout.height + 10f)
                canvas.save()
                canvas.translate(MARGIN + 6f, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 8f
            }
            y += 8f
        }

        // Transaction Ledger
        ensureSpace(40f)
        canvas.drawText("${strings.allTransactions} (${filtered.size})", MARGIN, y + 10f, sectionPaint)
        y += 24f

        val dateFmt = SimpleDateFormat("dd MMM yy, HH:mm", Locale.ENGLISH)
        for (tx in filtered) {
            ensureSpace(26f)
            val dateStr = dateFmt.format(Date(tx.occurredAtMillis))
            val catStr = strings.categoryName(tx.category, language)
            val pmStr = strings.paymentMethodName(tx.paymentMethod, language)
            val sign = if (tx.type == TxType.INCOME) "+" else "-"
            val amtStr = "$sign${MoneyCalculator.formatAmount(tx.amountMinor, currency, language)}"
            val amtPaint = if (tx.type == TxType.INCOME) incomePaint else expensePaint

            canvas.drawText(tx.title.take(34), MARGIN + 4f, y, bodyPaint)
            canvas.drawText(amtStr, PAGE_WIDTH - MARGIN - amtPaint.measureText(amtStr), y, amtPaint)
            y += 12f
            canvas.drawText("$dateStr  •  $catStr  •  $pmStr", MARGIN + 4f, y, mutedPaint)
            y += 14f
        }

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val fileName = "HisabPro_${period.name.lowercase()}_${currency.code}_$stamp.pdf"
        val outFile = File(reportsDir, fileName)
        FileOutputStream(outFile).use { fos -> document.writeTo(fos) }

        val savedToDownloads = saveCopyToDownloads(context, outFile, fileName)
        document.close()

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        return GeneratedPdfResult(
            shareUri = shareUri,
            fileName = fileName,
            savedToDownloads = savedToDownloads
        )
    }

    private fun saveCopyToDownloads(context: Context, sourceFile: File, fileName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/HisabPro")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
