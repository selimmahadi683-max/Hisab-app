package com.example.localization

import com.example.model.AppLanguage
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.ReportPeriod
import com.example.model.ThemePreference

data class AppStrings(
    val appTitle: String,
    val offlineReadyBadge: String,
    val cloudSyncedBadge: String,
    val navHome: String,
    val navAnalytics: String,
    val navBudget: String,
    val navBackup: String,
    val addTransaction: String,
    val editTransaction: String,
    val expense: String,
    val income: String,
    val netBalance: String,
    val totalIncome: String,
    val totalExpense: String,
    val thisMonth: String,
    val monthlyBudget: String,
    val budgetRemaining: String,
    val budgetExceeded: String,
    val setBudgetPrompt: String,
    val dailySafeLimit: String,
    val quickActions: String,
    val autoEntry: String,
    val manualEntry: String,
    val pdfReport: String,
    val savingsTips: String,
    val transactionHistoryGraph: String,
    val recentTransactions: String,
    val allTransactions: String,
    val emptyTransactionsTitle: String,
    val emptyTransactionsSubtitle: String,
    val searchPlaceholder: String,
    val periodWeekly: String,
    val periodMonthly: String,
    val periodYearly: String,
    val periodAllTime: String,
    val incomeVsExpenseChart: String,
    val categoryBreakdown: String,
    val paymentMethodBreakdown: String,
    val monthEndAdviceTitle: String,
    val exportPdfTitle: String,
    val exportPdfSubtitle: String,
    val generatePdfNow: String,
    val sharePdf: String,
    val amountOrCalcLabel: String,
    val titleLabel: String,
    val noteLabel: String,
    val categoryLabel: String,
    val paymentMethodLabel: String,
    val currencyLabel: String,
    val dateLabel: String,
    val autoTextInputLabel: String,
    val autoTextExampleHint: String,
    val autoParseBtn: String,
    val pasteAndParse: String,
    val autoParseSuccess: String,
    val autoParseFailed: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val overallBudgetTitle: String,
    val categoryBudgetsTitle: String,
    val recurringTemplatesTitle: String,
    val recurringSubtitle: String,
    val addRecurring: String,
    val logNow: String,
    val languageTitle: String,
    val currencyTitle: String,
    val themeTitle: String,
    val themeSystem: String,
    val themeDark: String,
    val themeLight: String,
    val cloudBackupTitle: String,
    val cloudBackupSubtitle: String,
    val signInWithGoogle: String,
    val signOut: String,
    val autoCloudSync: String,
    val syncNow: String,
    val localBackupTitle: String,
    val exportJsonBackup: String,
    val importJsonBackup: String,
    val backupExportedMsg: String,
    val backupImportedMsg: String,
    val backupFailedMsg: String,
    val pdfReadyMsg: String,
    val invalidAmountMsg: String,
    val spentLabel: String,
    val limitLabel: String,
    val noLimitSet: String,
    val tapToSetLimit: String,
    val bdtName: String,
    val kwdName: String,
    val ofBudgetUsed: String,
    val transactionsCount: String,
    val noDataForPeriod: String
) {
    fun categoryName(category: ExpenseCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.BN -> when (category) {
            ExpenseCategory.FOOD -> "খাবার ও বাজার"
            ExpenseCategory.TRANSPORT -> "যাতায়াত"
            ExpenseCategory.SHOPPING -> "কেনাকাটা"
            ExpenseCategory.BILLS -> "বিল ও রিচার্জ"
            ExpenseCategory.HOUSING -> "বাসা ভাড়া"
            ExpenseCategory.HEALTH -> "চিকিৎসা ও ঔষধ"
            ExpenseCategory.EDUCATION -> "শিক্ষা"
            ExpenseCategory.FAMILY -> "পরিবার ও উপহার"
            ExpenseCategory.ENTERTAINMENT -> "বিনোদন"
            ExpenseCategory.OTHER_EXPENSE -> "অন্যান্য খরচ"
            ExpenseCategory.SALARY -> "বেতন / আয়"
            ExpenseCategory.BUSINESS -> "ব্যবসা"
            ExpenseCategory.SAVINGS_INCOME -> "সঞ্চয় / বোনাস"
        }
        AppLanguage.AR -> when (category) {
            ExpenseCategory.FOOD -> "الطعام والبقالة"
            ExpenseCategory.TRANSPORT -> "المواصلات"
            ExpenseCategory.SHOPPING -> "التسوق"
            ExpenseCategory.BILLS -> "الفواتير والشحن"
            ExpenseCategory.HOUSING -> "السكن والإيجار"
            ExpenseCategory.HEALTH -> "الصحة والدواء"
            ExpenseCategory.EDUCATION -> "التعليم"
            ExpenseCategory.FAMILY -> "العائلة والهدايا"
            ExpenseCategory.ENTERTAINMENT -> "الترفيه"
            ExpenseCategory.OTHER_EXPENSE -> "مصروفات أخرى"
            ExpenseCategory.SALARY -> "الراتب"
            ExpenseCategory.BUSINESS -> "الأعمال"
            ExpenseCategory.SAVINGS_INCOME -> "مدخرات ومكافآت"
        }
        AppLanguage.EN -> when (category) {
            ExpenseCategory.FOOD -> "Food & Groceries"
            ExpenseCategory.TRANSPORT -> "Transport"
            ExpenseCategory.SHOPPING -> "Shopping"
            ExpenseCategory.BILLS -> "Bills & Recharge"
            ExpenseCategory.HOUSING -> "Rent & Housing"
            ExpenseCategory.HEALTH -> "Health & Medicine"
            ExpenseCategory.EDUCATION -> "Education"
            ExpenseCategory.FAMILY -> "Family & Gifts"
            ExpenseCategory.ENTERTAINMENT -> "Entertainment"
            ExpenseCategory.OTHER_EXPENSE -> "Other Expense"
            ExpenseCategory.SALARY -> "Salary"
            ExpenseCategory.BUSINESS -> "Business"
            ExpenseCategory.SAVINGS_INCOME -> "Savings & Bonus"
        }
    }

    fun paymentMethodName(method: PaymentMethod, lang: AppLanguage): String = when (lang) {
        AppLanguage.BN -> when (method) {
            PaymentMethod.CASH -> "নগদ ক্যাশ"
            PaymentMethod.BKASH -> "বিকাশ (bKash)"
            PaymentMethod.NAGAD -> "নগদ (Nagad)"
            PaymentMethod.ROCKET -> "রকেট (Rocket)"
            PaymentMethod.BANK -> "ব্যাংক / কার্ড"
            PaymentMethod.KNET -> "কেনেট (KNET)"
        }
        AppLanguage.AR -> when (method) {
            PaymentMethod.CASH -> "نقداً (كاش)"
            PaymentMethod.BKASH -> "بكاش (bKash)"
            PaymentMethod.NAGAD -> "ناغاد (Nagad)"
            PaymentMethod.ROCKET -> "روكيت (Rocket)"
            PaymentMethod.BANK -> "بنك / بطاقة"
            PaymentMethod.KNET -> "كي نت (KNET)"
        }
        AppLanguage.EN -> when (method) {
            PaymentMethod.CASH -> "Cash"
            PaymentMethod.BKASH -> "bKash"
            PaymentMethod.NAGAD -> "Nagad"
            PaymentMethod.ROCKET -> "Rocket"
            PaymentMethod.BANK -> "Bank / Card"
            PaymentMethod.KNET -> "KNET"
        }
    }

    fun periodName(period: ReportPeriod): String = when (period) {
        ReportPeriod.WEEKLY -> periodWeekly
        ReportPeriod.MONTHLY -> periodMonthly
        ReportPeriod.YEARLY -> periodYearly
        ReportPeriod.ALL_TIME -> periodAllTime
    }

    fun currencyFullName(currency: Currency): String = when (currency) {
        Currency.BDT -> bdtName
        Currency.KWD -> kwdName
    }

    fun themeName(pref: ThemePreference): String = when (pref) {
        ThemePreference.SYSTEM -> themeSystem
        ThemePreference.DARK -> themeDark
        ThemePreference.LIGHT -> themeLight
    }
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.BN -> AppStrings(
        appTitle = "হিসাব প্রো",
        offlineReadyBadge = "অফলাইন সচল",
        cloudSyncedBadge = "ক্লাউড সিঙ্কড",
        navHome = "হোম",
        navAnalytics = "রিপোর্ট",
        navBudget = "বাজেট",
        navBackup = "ব্যাকআপ",
        addTransaction = "নতুন এন্ট্রি",
        editTransaction = "হিসাব পরিবর্তন",
        expense = "খরচ",
        income = "আয়",
        netBalance = "নিট ব্যালেন্স",
        totalIncome = "মোট আয়",
        totalExpense = "মোট খরচ",
        thisMonth = "চলতি মাস",
        monthlyBudget = "মাসিক বাজেট",
        budgetRemaining = "বাজেট অবশিষ্ট",
        budgetExceeded = "বাজেট অতিক্রম করেছে",
        setBudgetPrompt = "মাসিক বাজেট সেট করুন",
        dailySafeLimit = "দৈনিক নিরাপদ খরচ",
        quickActions = "দ্রুত অপশন",
        autoEntry = "অটো টেক্সট",
        manualEntry = "ম্যানুয়াল ক্যালকুলেটর",
        pdfReport = "PDF রিপোর্ট",
        savingsTips = "সাশ্রয় পরামর্শ",
        transactionHistoryGraph = "ট্রানজ্যাকশন হিস্ট্রি গ্রাফ",
        recentTransactions = "সাম্প্রতিক লেনদেন",
        allTransactions = "সকল লেনদেন",
        emptyTransactionsTitle = "এখনো কোনো হিসাব যুক্ত হয়নি",
        emptyTransactionsSubtitle = "নিচের + বাটনে চাপ দিয়ে ম্যানুয়ালি বা অটো টেক্সটে প্রথম খরচ লিখুন।",
        searchPlaceholder = "খরচ, নোট বা মাধ্যম খুঁজুন...",
        periodWeekly = "সাপ্তাহিক",
        periodMonthly = "মাসিক",
        periodYearly = "বার্ষিক",
        periodAllTime = "সর্বকালের",
        incomeVsExpenseChart = "আয় বনাম খরচ গ্রাফ",
        categoryBreakdown = "খাতভিত্তিক খরচ",
        paymentMethodBreakdown = "পেমেন্ট মাধ্যম বিশ্লেষণ",
        monthEndAdviceTitle = "মাস শেষে খরচ কমানোর পরামর্শ",
        exportPdfTitle = "PDF রিপোর্ট এক্সপোর্ট",
        exportPdfSubtitle = "সাপ্তাহিক, মাসিক বা বার্ষিক সম্পূর্ণ হিসাবের PDF তৈরি ও শেয়ার করুন",
        generatePdfNow = "PDF তৈরি করুন",
        sharePdf = "PDF শেয়ার করুন",
        amountOrCalcLabel = "টাকার পরিমাণ বা হিসাব (যেমন: ২৫০+১২০)",
        titleLabel = "বিবরণ / খাত",
        noteLabel = "নোট (ঐচ্ছিক)",
        categoryLabel = "ক্যাটাগরি নির্বাচন করুন",
        paymentMethodLabel = "পেমেন্ট মেথড",
        currencyLabel = "মুদ্রা",
        dateLabel = "তারিখ ও সময়",
        autoTextInputLabel = "মেসেজ বা টেক্সট লিখুন/পেস্ট করুন",
        autoTextExampleHint = "উদাহরণ: \"বিকাশ বাজার ৫৫০ টাকা\" অথবা \"KNET fuel 4.500\"",
        autoParseBtn = "অটো রিড ও ফর্ম পূরণ",
        pasteAndParse = "পেস্ট ও অটো রিড",
        autoParseSuccess = "অটো টেক্সট থেকে তথ্য সফলভাবে শনাক্ত হয়েছে!",
        autoParseFailed = "পরিমাণ খুঁজে পাওয়া যায়নি। সংখ্যা লিখুন, যেমন: ৫০০",
        save = "সংরক্ষণ করুন",
        cancel = "বাতিল",
        delete = "মুছে ফেলুন",
        edit = "সম্পাদনা",
        overallBudgetTitle = "মোট মাসিক বাজেট",
        categoryBudgetsTitle = "খাতভিত্তিক কাস্টম বাজেট",
        recurringTemplatesTitle = "নিয়মিত অটো হিসাব (১-ট্যাপ)",
        recurringSubtitle = "বাসা ভাড়া, বিল বা বেতনের মতো নিয়মিত এন্ট্রি",
        addRecurring = "টেমপ্লেট যোগ করুন",
        logNow = "এখনই যোগ করুন",
        languageTitle = "ভাষা (Language)",
        currencyTitle = "প্রাথমিক মুদ্রা (BDT / KWD)",
        themeTitle = "থিম ও ডার্ক মোড",
        themeSystem = "সিস্টেম ডিফল্ট",
        themeDark = "ডার্ক মোড (চোখের আরাম)",
        themeLight = "লাইট মোড",
        cloudBackupTitle = "ঐচ্ছিক ক্লাউড ব্যাকআপ ও সিঙ্ক",
        cloudBackupSubtitle = "অফলাইনে সব সময় কাজ করবে। চাইলে Google দিয়ে নিরাপদ ক্লাউড ব্যাকআপ ও রিয়েল-টাইম সিঙ্ক চালু করুন।",
        signInWithGoogle = "Google দিয়ে সাইন-ইন করুন",
        signOut = "সাইন আউট",
        autoCloudSync = "অটো রিয়েল-টাইম ক্লাউড সিঙ্ক",
        syncNow = "এখনই ব্যাকআপ ও সিঙ্ক করুন",
        localBackupTitle = "অফলাইন ফাইল ব্যাকআপ (JSON)",
        exportJsonBackup = "ব্যাকআপ ফাইল সেভ করুন",
        importJsonBackup = "ব্যাকআপ ফাইল রিস্টোর করুন",
        backupExportedMsg = "অফলাইন ব্যাকআপ ফাইল সফলভাবে সংরক্ষিত হয়েছে।",
        backupImportedMsg = "ব্যাকআপ থেকে হিসাব সফলভাবে রিস্টোর হয়েছে।",
        backupFailedMsg = "ব্যাকআপ প্রক্রিয়ায় সমস্যা হয়েছে।",
        pdfReadyMsg = "PDF রিপোর্ট তৈরি হয়েছে!",
        invalidAmountMsg = "সঠিক পরিমাণ লিখুন (০ এর বেশি)।",
        spentLabel = "খরচ",
        limitLabel = "সীমা",
        noLimitSet = "বাজেট সেট করা হয়নি",
        tapToSetLimit = "ট্যাপ করে বাজেট সেট করুন",
        bdtName = "বাংলাদেশি টাকা (৳ BDT)",
        kwdName = "কুয়েতি দিনার (د.ك KWD)",
        ofBudgetUsed = "বাজেট ব্যবহৃত",
        transactionsCount = "টি লেনদেন",
        noDataForPeriod = "এই সময়কালে কোনো লেনদেন পাওয়া যায়নি"
    )

    AppLanguage.AR -> AppStrings(
        appTitle = "حساب برو",
        offlineReadyBadge = "يعمل أوفلاين",
        cloudSyncedBadge = "متزامن سحابياً",
        navHome = "الرئيسية",
        navAnalytics = "التقارير",
        navBudget = "الميزانية",
        navBackup = "النسخ الاحتياطي",
        addTransaction = "إضافة معاملة",
        editTransaction = "تعديل المعاملة",
        expense = "مصروف",
        income = "دخل",
        netBalance = "صافي الرصيد",
        totalIncome = "إجمالي الدخل",
        totalExpense = "إجمالي المصروفات",
        thisMonth = "هذا الشهر",
        monthlyBudget = "الميزانية الشهرية",
        budgetRemaining = "المتبقي من الميزانية",
        budgetExceeded = "تم تجاوز الميزانية",
        setBudgetPrompt = "تعيين الميزانية الشهرية",
        dailySafeLimit = "الحد اليومي الآمن",
        quickActions = "إجراءات سريعة",
        autoEntry = "إدخال تلقائي",
        manualEntry = "حاسبة يدوية",
        pdfReport = "تقرير PDF",
        savingsTips = "نصائح التوفير",
        transactionHistoryGraph = "رسم سجل المعاملات",
        recentTransactions = "أحدث المعاملات",
        allTransactions = "كل المعاملات",
        emptyTransactionsTitle = "لا توجد معاملات مسجلة بعد",
        emptyTransactionsSubtitle = "اضغط على زر + لإضافة أول مصروف يدوياً أو عبر النص التلقائي.",
        searchPlaceholder = "ابحث في المصروفات أو الملاحظات...",
        periodWeekly = "أسبوعي",
        periodMonthly = "شهري",
        periodYearly = "سنوي",
        periodAllTime = "كل الوقت",
        incomeVsExpenseChart = "الدخل مقابل المصروفات",
        categoryBreakdown = "توزيع الفئات",
        paymentMethodBreakdown = "تحليل طرق الدفع",
        monthEndAdviceTitle = "نصائح تقليل المصروفات لنهاية الشهر",
        exportPdfTitle = "تصدير تقرير PDF",
        exportPdfSubtitle = "أنشئ وشارك تقريراً مفصلاً أسبوعياً أو شهرياً أو سنوياً بصيغة PDF",
        generatePdfNow = "إنشاء PDF",
        sharePdf = "مشاركة PDF",
        amountOrCalcLabel = "المبلغ أو العملية الحسابية (مثال: 25+12.5)",
        titleLabel = "العنوان / الوصف",
        noteLabel = "ملاحظة (اختياري)",
        categoryLabel = "اختر الفئة",
        paymentMethodLabel = "طريقة الدفع",
        currencyLabel = "العملة",
        dateLabel = "التاريخ والوقت",
        autoTextInputLabel = "اكتب أو الصق رسالة المصروف",
        autoTextExampleHint = "مثال: \"KNET fuel 4.500\" أو \"بنك طعام 12.250\"",
        autoParseBtn = "تحليل تلقائي وتعبئة",
        pasteAndParse = "لصق وتحليل",
        autoParseSuccess = "تم استخراج البيانات تلقائياً بنجاح!",
        autoParseFailed = "لم يتم العثور على مبلغ صحيح.",
        save = "حفظ",
        cancel = "إلغاء",
        delete = "حذف",
        edit = "تعديل",
        overallBudgetTitle = "الميزانية الشهرية الإجمالية",
        categoryBudgetsTitle = "ميزانيات الفئات المخصصة",
        recurringTemplatesTitle = "المصروفات المتكررة (نقرة واحدة)",
        recurringSubtitle = "الإيجار والفواتير والراتب المتكرر",
        addRecurring = "إضافة قالب متكرر",
        logNow = "تسجيل الآن",
        languageTitle = "اللغة (Language)",
        currencyTitle = "العملة الافتراضية (BDT / KWD)",
        themeTitle = "المظهر والوضع الداكن",
        themeSystem = "افتراضي النظام",
        themeDark = "الوضع الداكن (مريح للعين)",
        themeLight = "الوضع الفاتح",
        cloudBackupTitle = "النسخ الاحتياطي السحابي الاختياري",
        cloudBackupSubtitle = "يعمل التطبيق بالكامل بدون إنترنت. سجل الدخول بحساب Google فقط عند رغبتك في النسخ الاحتياطي والمزامنة.",
        signInWithGoogle = "تسجيل الدخول عبر Google",
        signOut = "تسجيل الخروج",
        autoCloudSync = "مزامنة سحابية فورية تلقائية",
        syncNow = "نسخ احتياطي ومزامنة الآن",
        localBackupTitle = "نسخ احتياطي محلي (JSON)",
        exportJsonBackup = "حفظ ملف النسخ الاحتياطي",
        importJsonBackup = "استعادة من ملف",
        backupExportedMsg = "تم حفظ النسخة الاحتياطية بنجاح.",
        backupImportedMsg = "تمت استعادة البيانات بنجاح.",
        backupFailedMsg = "فشلت عملية النسخ الاحتياطي.",
        pdfReadyMsg = "تقرير PDF جاهز!",
        invalidAmountMsg = "يرجى إدخال مبلغ صحيح أكبر من 0.",
        spentLabel = "المصروف",
        limitLabel = "الحد",
        noLimitSet = "لم يتم تعيين حد",
        tapToSetLimit = "اضغط لتعيين الميزانية",
        bdtName = "تاكا بنغلاديشي (৳ BDT)",
        kwdName = "دينار كويتي (د.ك KWD)",
        ofBudgetUsed = "من الميزانية",
        transactionsCount = "معاملات",
        noDataForPeriod = "لا توجد معاملات لهذه الفترة"
    )

    AppLanguage.EN -> AppStrings(
        appTitle = "HisabPro",
        offlineReadyBadge = "Offline Ready",
        cloudSyncedBadge = "Cloud Synced",
        navHome = "Home",
        navAnalytics = "Analytics",
        navBudget = "Budgets",
        navBackup = "Backup",
        addTransaction = "New Entry",
        editTransaction = "Edit Entry",
        expense = "Expense",
        income = "Income",
        netBalance = "Net Balance",
        totalIncome = "Total Income",
        totalExpense = "Total Expense",
        thisMonth = "This Month",
        monthlyBudget = "Monthly Budget",
        budgetRemaining = "Remaining",
        budgetExceeded = "Over Budget",
        setBudgetPrompt = "Set Monthly Budget",
        dailySafeLimit = "Daily Safe Spend",
        quickActions = "Quick Actions",
        autoEntry = "Auto Text",
        manualEntry = "Calculator",
        pdfReport = "PDF Report",
        savingsTips = "Savings Tips",
        transactionHistoryGraph = "Transaction History Graph",
        recentTransactions = "Recent Transactions",
        allTransactions = "All Transactions",
        emptyTransactionsTitle = "No Transactions Yet",
        emptyTransactionsSubtitle = "Tap + below to calculate an expense manually or auto-parse from text.",
        searchPlaceholder = "Search title, note, payment...",
        periodWeekly = "Weekly",
        periodMonthly = "Monthly",
        periodYearly = "Yearly",
        periodAllTime = "All Time",
        incomeVsExpenseChart = "Income vs Expense",
        categoryBreakdown = "Category Breakdown",
        paymentMethodBreakdown = "Payment Methods",
        monthEndAdviceTitle = "Expense Reduction Advisor",
        exportPdfTitle = "Export PDF Report",
        exportPdfSubtitle = "Generate & share Weekly, Monthly, or Yearly financial PDF statements anytime",
        generatePdfNow = "Generate PDF",
        sharePdf = "Share PDF",
        amountOrCalcLabel = "Amount or expression (e.g. 250+120*2)",
        titleLabel = "Title / Merchant",
        noteLabel = "Note (Optional)",
        categoryLabel = "Select Category",
        paymentMethodLabel = "Payment Method",
        currencyLabel = "Currency",
        dateLabel = "Date & Time",
        autoTextInputLabel = "Type or paste expense SMS / note",
        autoTextExampleHint = "Example: \"bKash groceries 1250\" or \"KNET fuel 4.500\"",
        autoParseBtn = "Auto Parse & Fill",
        pasteAndParse = "Paste & Auto Parse",
        autoParseSuccess = "Auto-detected amount, category & payment method!",
        autoParseFailed = "Could not detect a valid amount.",
        save = "Save Entry",
        cancel = "Cancel",
        delete = "Delete",
        edit = "Edit",
        overallBudgetTitle = "Overall Monthly Budget",
        categoryBudgetsTitle = "Custom Category Budgets",
        recurringTemplatesTitle = "Recurring 1-Tap Entries",
        recurringSubtitle = "Rent, internet, salary & fixed bills",
        addRecurring = "Add Template",
        logNow = "Log Now",
        languageTitle = "Language",
        currencyTitle = "Active Currency (BDT / KWD)",
        themeTitle = "Appearance & Dark Mode",
        themeSystem = "System Default",
        themeDark = "Dark Mode (Eye Comfort)",
        themeLight = "Light Mode",
        cloudBackupTitle = "Optional Cloud Backup & Sync",
        cloudBackupSubtitle = "100% offline by default. Sign in with Google only when you want encrypted cloud backup & real-time sync.",
        signInWithGoogle = "Sign in with Google",
        signOut = "Sign Out",
        autoCloudSync = "Auto Real-Time Cloud Sync",
        syncNow = "Backup & Sync Now",
        localBackupTitle = "Offline JSON File Backup",
        exportJsonBackup = "Export Backup File",
        importJsonBackup = "Restore from File",
        backupExportedMsg = "Offline backup exported successfully.",
        backupImportedMsg = "Transactions & budgets restored!",
        backupFailedMsg = "Backup operation failed.",
        pdfReadyMsg = "PDF Report Ready!",
        invalidAmountMsg = "Enter a valid amount greater than 0.",
        spentLabel = "Spent",
        limitLabel = "Limit",
        noLimitSet = "No limit set",
        tapToSetLimit = "Tap to set budget",
        bdtName = "Bangladeshi Taka (৳ BDT)",
        kwdName = "Kuwaiti Dinar (د.ك KWD)",
        ofBudgetUsed = "of budget used",
        transactionsCount = "entries",
        noDataForPeriod = "No transactions recorded for this period"
    )
}
