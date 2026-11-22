package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceRepository
import com.example.data.local.UserPreferences
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.BudgetItem
import com.example.model.CloudSyncStatus
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.RecurringTemplate
import com.example.model.ReportPeriod
import com.example.model.SavingsAdvice
import com.example.model.ThemePreference
import com.example.model.TransactionItem
import com.example.model.TxType
import com.example.util.ExpenseAdvisorEngine
import com.example.util.GeneratedPdfResult
import com.example.util.PdfReportGenerator
import com.example.util.PeriodChartBucket
import com.example.util.PeriodFilter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

data class MainUiState(
    val preferences: UserPreferences = UserPreferences(),
    val transactions: List<TransactionItem> = emptyList(),
    val budgets: List<BudgetItem> = emptyList(),
    val recurringTemplates: List<RecurringTemplate> = emptyList(),
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val searchQuery: String = "",
    val currentMonthIncomeMinor: Long = 0L,
    val currentMonthExpenseMinor: Long = 0L,
    val overallMonthlyBudgetMinor: Long = 0L,
    val periodIncomeMinor: Long = 0L,
    val periodExpenseMinor: Long = 0L,
    val periodExpensesByCategory: List<Pair<ExpenseCategory, Long>> = emptyList(),
    val periodExpensesByMethod: List<Pair<PaymentMethod, Long>> = emptyList(),
    val periodChartBuckets: List<PeriodChartBucket> = emptyList(),
    val filteredPeriodTransactions: List<TransactionItem> = emptyList(),
    val adviceList: List<SavingsAdvice> = emptyList(),
    val cloudUserEmail: String? = null,
    val cloudUserName: String? = null,
    val isSignedIn: Boolean = false,
    val syncStatus: CloudSyncStatus = CloudSyncStatus.OfflineLocalOnly
)

sealed interface UiEvent {
    data class ShowMessage(val text: String) : UiEvent
    data class PdfGenerated(val result: GeneratedPdfResult) : UiEvent
}

class MainViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _searchQuery = MutableStateFlow("")
    private val _cloudUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.OfflineLocalOnly)

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var cloudListenersJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _cloudUser.value = user
        if (user == null) {
            stopCloudRealtimeSync()
            _syncStatus.value = CloudSyncStatus.OfflineLocalOnly
        } else {
            onUserAuthenticated(user.uid)
        }
    }

    init {
        Firebase.auth.addAuthStateListener(authListener)
        Firebase.auth.currentUser?.let { user ->
            onUserAuthenticated(user.uid)
        }
    }

    private val coreDataFlow = combine(
        repository.preferences,
        repository.transactions,
        repository.budgets,
        repository.recurringTemplates
    ) { prefs, txs, budgets, templates ->
        Quad(prefs, txs, budgets, templates)
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreDataFlow,
        _selectedPeriod,
        _searchQuery,
        _cloudUser,
        _syncStatus
    ) { (prefs, txs, budgets, templates), period, query, user, syncStatus ->
        val currency = prefs.activeCurrency
        val lang = prefs.language

        val currencyTxs = txs.filter { it.currency == currency }
        val monthTxs = PeriodFilter.filterByPeriod(currencyTxs, ReportPeriod.MONTHLY)
        val monthIncome = monthTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
        val monthExpense = monthTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }

        val overallBudget = budgets
            .firstOrNull { it.currency == currency && it.categoryId == "OVERALL" }
            ?.limitMinor ?: 0L

        val periodTxs = PeriodFilter.filterByPeriod(currencyTxs, period)
        val periodIncome = periodTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
        val periodExpense = periodTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }

        val byCategory = periodTxs
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }

        val byMethod = periodTxs
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.paymentMethod }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }
            .map { it.key to it.value }

        val buckets = PeriodFilter.buildChartBuckets(txs, currency, period, lang)

        val trimmedQ = query.trim().lowercase()
        val searchedPeriodTxs = if (trimmedQ.isBlank()) {
            periodTxs
        } else {
            val strings = stringsFor(lang)
            periodTxs.filter { tx ->
                tx.title.lowercase().contains(trimmedQ) ||
                    tx.note.lowercase().contains(trimmedQ) ||
                    strings.categoryName(tx.category, lang).lowercase().contains(trimmedQ) ||
                    strings.paymentMethodName(tx.paymentMethod, lang).lowercase().contains(trimmedQ)
            }
        }

        val advice = ExpenseAdvisorEngine.generateAdvice(txs, budgets, currency, lang)

        MainUiState(
            preferences = prefs,
            transactions = currencyTxs,
            budgets = budgets,
            recurringTemplates = templates,
            selectedPeriod = period,
            searchQuery = query,
            currentMonthIncomeMinor = monthIncome,
            currentMonthExpenseMinor = monthExpense,
            overallMonthlyBudgetMinor = overallBudget,
            periodIncomeMinor = periodIncome,
            periodExpenseMinor = periodExpense,
            periodExpensesByCategory = byCategory,
            periodExpensesByMethod = byMethod,
            periodChartBuckets = buckets,
            filteredPeriodTransactions = searchedPeriodTxs,
            adviceList = advice,
            cloudUserEmail = user?.email,
            cloudUserName = user?.displayName,
            isSignedIn = user != null,
            syncStatus = if (user == null) {
                CloudSyncStatus.OfflineLocalOnly
            } else if (syncStatus == CloudSyncStatus.OfflineLocalOnly && prefs.lastCloudSyncMillis > 0L) {
                CloudSyncStatus.Synced(prefs.lastCloudSyncMillis)
            } else {
                syncStatus
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setActiveCurrency(currency: Currency) {
        viewModelScope.launch { repository.setActiveCurrency(currency) }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun setAutoCloudSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoCloudSync(enabled)
            val user = Firebase.auth.currentUser
            if (enabled && user != null) {
                startCloudRealtimeSync(user.uid)
            } else {
                stopCloudRealtimeSync()
            }
        }
    }

    fun saveTransaction(
        existingId: String?,
        type: TxType,
        amountMinor: Long,
        currency: Currency,
        category: ExpenseCategory,
        paymentMethod: PaymentMethod,
        title: String,
        note: String,
        occurredAtMillis: Long
    ) {
        viewModelScope.launch {
            val autoSync = uiState.value.preferences.autoCloudSync && uiState.value.isSignedIn
            repository.saveTransaction(
                existingId = existingId,
                type = type,
                amountMinor = amountMinor,
                currency = currency,
                category = category,
                paymentMethod = paymentMethod,
                title = title,
                note = note,
                occurredAtMillis = occurredAtMillis,
                autoSyncIfSignedIn = autoSync
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val autoSync = uiState.value.preferences.autoCloudSync && uiState.value.isSignedIn
            repository.deleteTransaction(id, autoSync)
        }
    }

    fun saveBudget(currency: Currency, categoryId: String, limitMinor: Long) {
        viewModelScope.launch {
            val autoSync = uiState.value.preferences.autoCloudSync && uiState.value.isSignedIn
            repository.saveBudget(currency, categoryId, limitMinor, autoSync)
        }
    }

    fun saveRecurringTemplate(
        type: TxType,
        amountMinor: Long,
        currency: Currency,
        category: ExpenseCategory,
        paymentMethod: PaymentMethod,
        title: String,
        dayOfMonth: Int
    ) {
        viewModelScope.launch {
            repository.saveRecurringTemplate(type, amountMinor, currency, category, paymentMethod, title, dayOfMonth)
        }
    }

    fun logRecurringTemplateNow(template: RecurringTemplate) {
        saveTransaction(
            existingId = null,
            type = template.type,
            amountMinor = template.amountMinor,
            currency = template.currency,
            category = template.category,
            paymentMethod = template.paymentMethod,
            title = template.title,
            note = "Recurring",
            occurredAtMillis = System.currentTimeMillis()
        )
    }

    fun deleteRecurringTemplate(id: String) {
        viewModelScope.launch { repository.deleteRecurringTemplate(id) }
    }

    fun triggerManualCloudSync() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _syncStatus.value = CloudSyncStatus.Syncing
            val res = repository.syncWithCloud(uid)
            _syncStatus.value = res.fold(
                onSuccess = { CloudSyncStatus.Synced(it) },
                onFailure = { CloudSyncStatus.Error(it.localizedMessage ?: "Sync error") }
            )
        }
    }

    fun onCloudSignedIn() {
        val user = Firebase.auth.currentUser ?: return
        _cloudUser.value = user
        onUserAuthenticated(user.uid)
    }

    fun onCloudSignedOut() {
        stopCloudRealtimeSync()
        _cloudUser.value = null
        _syncStatus.value = CloudSyncStatus.OfflineLocalOnly
    }

    private fun onUserAuthenticated(uid: String) {
        viewModelScope.launch {
            _syncStatus.value = CloudSyncStatus.Syncing
            val syncRes = repository.syncWithCloud(uid)
            _syncStatus.value = syncRes.fold(
                onSuccess = { CloudSyncStatus.Synced(it) },
                onFailure = { CloudSyncStatus.Error(it.localizedMessage ?: "Sync failed") }
            )
            if (uiState.value.preferences.autoCloudSync) {
                startCloudRealtimeSync(uid)
            }
        }
    }

    private fun startCloudRealtimeSync(uid: String) {
        stopCloudRealtimeSync()
        cloudListenersJob = viewModelScope.launch {
            launch {
                repository.observeRemoteTransactions(uid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing cloud transactions", rootError)
                        _syncStatus.value = CloudSyncStatus.Error(rootError.message ?: "Cloud sync error")
                    }
                    .collect { remoteTxs ->
                        repository.applyRemoteTransactionsSnapshot(remoteTxs)
                        _syncStatus.value = CloudSyncStatus.Synced(System.currentTimeMillis())
                    }
            }
            launch {
                repository.observeRemoteBudgets(uid)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing cloud budgets", rootError)
                        _syncStatus.value = CloudSyncStatus.Error(rootError.message ?: "Cloud sync error")
                    }
                    .collect { remoteBudgets ->
                        repository.applyRemoteBudgetsSnapshot(remoteBudgets)
                    }
            }
        }
    }

    fun stopCloudRealtimeSync() {
        cloudListenersJob?.cancel()
        cloudListenersJob = null
    }

    fun generatePdfReport(context: Context, period: ReportPeriod) {
        val snapshot = uiState.value
        val appContext = context.applicationContext
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfReportGenerator.generateReport(
                    context = appContext,
                    transactions = snapshot.transactions,
                    budgets = snapshot.budgets,
                    currency = snapshot.preferences.activeCurrency,
                    period = period,
                    language = snapshot.preferences.language
                )
            }
            _events.emit(UiEvent.PdfGenerated(result))
        }
    }

    fun exportJsonBackup(context: Context, uri: Uri) {
        val strings = stringsFor(uiState.value.preferences.language)
        viewModelScope.launch {
            val res = repository.exportLocalJsonBackup(context.applicationContext, uri)
            _events.emit(
                UiEvent.ShowMessage(
                    if (res.isSuccess) strings.backupExportedMsg else strings.backupFailedMsg
                )
            )
        }
    }

    fun importJsonBackup(context: Context, uri: Uri) {
        val strings = stringsFor(uiState.value.preferences.language)
        viewModelScope.launch {
            val res = repository.importLocalJsonBackup(context.applicationContext, uri)
            _events.emit(
                UiEvent.ShowMessage(
                    if (res.isSuccess) strings.backupImportedMsg else strings.backupFailedMsg
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authListener)
        stopCloudRealtimeSync()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val TAG = "MainViewModel"

        fun remainingDaysInCurrentMonth(): Int {
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            return (maxDays - today + 1).coerceAtLeast(1)
        }
    }
}
