package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDao
import com.example.data.local.BudgetEntity
import com.example.data.local.RecurringTemplateEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.CloudBudget
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.CloudTransaction
import com.example.model.AppLanguage
import com.example.model.BudgetItem
import com.example.model.Currency
import com.example.model.ExpenseCategory
import com.example.model.PaymentMethod
import com.example.model.RecurringTemplate
import com.example.model.ThemePreference
import com.example.model.TransactionItem
import com.example.model.TxType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FinanceRepository(
    private val dao: AppDao,
    private val prefsRepo: UserPreferencesRepository,
    private val cloudRepo: CloudSyncRepository
) {
    val preferences: Flow<UserPreferences> = prefsRepo.preferencesFlow

    val transactions: Flow<List<TransactionItem>> = dao.observeActiveTransactions().map { list ->
        list.map { it.toDomain() }
    }

    val budgets: Flow<List<BudgetItem>> = dao.observeActiveBudgets().map { list ->
        list.map { it.toDomain() }
    }

    val recurringTemplates: Flow<List<RecurringTemplate>> = dao.observeRecurringTemplates().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun setLanguage(language: AppLanguage) = prefsRepo.setLanguage(language)
    suspend fun setActiveCurrency(currency: Currency) = prefsRepo.setActiveCurrency(currency)
    suspend fun setThemePreference(theme: ThemePreference) = prefsRepo.setThemePreference(theme)
    suspend fun setAutoCloudSync(enabled: Boolean) = prefsRepo.setAutoCloudSync(enabled)

    suspend fun saveTransaction(
        existingId: String?,
        type: TxType,
        amountMinor: Long,
        currency: Currency,
        category: ExpenseCategory,
        paymentMethod: PaymentMethod,
        title: String,
        note: String,
        occurredAtMillis: Long,
        autoSyncIfSignedIn: Boolean
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        val id = existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val entity = TransactionEntity(
            id = id,
            type = type.name,
            amountMinor = amountMinor,
            currencyCode = currency.code,
            categoryId = category.id,
            paymentMethodId = paymentMethod.id,
            title = title.trim().take(120),
            note = note.trim().take(300),
            occurredAtMillis = occurredAtMillis,
            clientUpdatedAtMillis = now,
            deleted = false,
            pendingSync = true
        )
        dao.upsertTransaction(entity)

        if (autoSyncIfSignedIn) {
            val uid = Firebase.auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val res = cloudRepo.upsertTransaction(uid, entity)
                if (res.isSuccess) {
                    dao.markTransactionSynced(entity.id)
                    prefsRepo.setLastCloudSyncMillis(System.currentTimeMillis())
                }
            }
        }
        return entity
    }

    suspend fun deleteTransaction(id: String, autoSyncIfSignedIn: Boolean) {
        val now = System.currentTimeMillis()
        dao.softDeleteTransaction(id, now)
        if (autoSyncIfSignedIn) {
            val uid = Firebase.auth.currentUser?.uid
            val updated = dao.getTransactionById(id)
            if (!uid.isNullOrBlank() && updated != null) {
                val res = cloudRepo.upsertTransaction(uid, updated)
                if (res.isSuccess) {
                    dao.markTransactionSynced(id)
                    prefsRepo.setLastCloudSyncMillis(System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun saveBudget(
        currency: Currency,
        categoryId: String,
        limitMinor: Long,
        autoSyncIfSignedIn: Boolean
    ) {
        val id = "${currency.code}_${categoryId}"
        val now = System.currentTimeMillis()
        val entity = BudgetEntity(
            id = id,
            currencyCode = currency.code,
            categoryId = categoryId,
            limitMinor = limitMinor.coerceAtLeast(0L),
            clientUpdatedAtMillis = now,
            deleted = false,
            pendingSync = true
        )
        dao.upsertBudget(entity)
        if (autoSyncIfSignedIn) {
            val uid = Firebase.auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val res = cloudRepo.upsertBudget(uid, entity)
                if (res.isSuccess) {
                    dao.markBudgetSynced(id)
                    prefsRepo.setLastCloudSyncMillis(System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun saveRecurringTemplate(
        type: TxType,
        amountMinor: Long,
        currency: Currency,
        category: ExpenseCategory,
        paymentMethod: PaymentMethod,
        title: String,
        dayOfMonth: Int
    ) {
        val entity = RecurringTemplateEntity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            amountMinor = amountMinor,
            currencyCode = currency.code,
            categoryId = category.id,
            paymentMethodId = paymentMethod.id,
            title = title.trim().take(120),
            dayOfMonth = dayOfMonth.coerceIn(1, 28)
        )
        dao.upsertRecurringTemplate(entity)
    }

    suspend fun deleteRecurringTemplate(id: String) = dao.deleteRecurringTemplate(id)

    /**
     * Bidirectional offline-first sync with Firestore for the signed-in user.
     * Uses clientUpdatedAtMillis for conflict resolution so newer offline or cloud edits win cleanly.
     */
    suspend fun syncWithCloud(uid: String): Result<Long> {
        return try {
            val remoteTxResult = cloudRepo.fetchAllTransactions(uid)
            if (remoteTxResult.isFailure) return Result.failure(remoteTxResult.exceptionOrNull()!!)
            val remoteBudgetResult = cloudRepo.fetchAllBudgets(uid)
            if (remoteBudgetResult.isFailure) return Result.failure(remoteBudgetResult.exceptionOrNull()!!)

            val remoteTxs = remoteTxResult.getOrDefault(emptyList()).associateBy { it.id }
            val localTxs = dao.getAllTransactionsRaw().associateBy { it.id }

            // Pull newer remote transactions into Room
            val toSaveLocally = mutableListOf<TransactionEntity>()
            for ((id, remote) in remoteTxs) {
                val local = localTxs[id]
                if (local == null || remote.clientUpdatedAtMillis > local.clientUpdatedAtMillis) {
                    toSaveLocally.add(remote.toEntity())
                }
            }
            if (toSaveLocally.isNotEmpty()) {
                dao.upsertTransactions(toSaveLocally)
            }

            // Push newer or unsynced local transactions to Firestore
            for ((id, local) in localTxs) {
                val remote = remoteTxs[id]
                if (remote == null || local.clientUpdatedAtMillis > remote.clientUpdatedAtMillis || local.pendingSync) {
                    val pushRes = cloudRepo.upsertTransaction(uid, local)
                    if (pushRes.isSuccess) dao.markTransactionSynced(id)
                }
            }

            // Sync Budgets
            val remoteBudgets = remoteBudgetResult.getOrDefault(emptyList()).associateBy { it.id }
            val localBudgets = dao.getAllBudgetsRaw().associateBy { it.id }

            val budgetsToSaveLocally = mutableListOf<BudgetEntity>()
            for ((id, remote) in remoteBudgets) {
                val local = localBudgets[id]
                if (local == null || remote.clientUpdatedAtMillis > local.clientUpdatedAtMillis) {
                    budgetsToSaveLocally.add(remote.toEntity())
                }
            }
            if (budgetsToSaveLocally.isNotEmpty()) {
                dao.upsertBudgets(budgetsToSaveLocally)
            }

            for ((id, local) in localBudgets) {
                val remote = remoteBudgets[id]
                if (remote == null || local.clientUpdatedAtMillis > remote.clientUpdatedAtMillis || local.pendingSync) {
                    val pushRes = cloudRepo.upsertBudget(uid, local)
                    if (pushRes.isSuccess) dao.markBudgetSynced(id)
                }
            }

            val now = System.currentTimeMillis()
            prefsRepo.setLastCloudSyncMillis(now)
            Result.success(now)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyRemoteTransactionsSnapshot(remoteList: List<CloudTransaction>) {
        val localMap = dao.getAllTransactionsRaw().associateBy { it.id }
        val updates = mutableListOf<TransactionEntity>()
        for (remote in remoteList) {
            if (remote.id.isBlank()) continue
            val local = localMap[remote.id]
            if (local == null || remote.clientUpdatedAtMillis > local.clientUpdatedAtMillis) {
                updates.add(remote.toEntity())
            }
        }
        if (updates.isNotEmpty()) dao.upsertTransactions(updates)
    }

    suspend fun applyRemoteBudgetsSnapshot(remoteList: List<CloudBudget>) {
        val localMap = dao.getAllBudgetsRaw().associateBy { it.id }
        val updates = mutableListOf<BudgetEntity>()
        for (remote in remoteList) {
            if (remote.id.isBlank()) continue
            val local = localMap[remote.id]
            if (local == null || remote.clientUpdatedAtMillis > local.clientUpdatedAtMillis) {
                updates.add(remote.toEntity())
            }
        }
        if (updates.isNotEmpty()) dao.upsertBudgets(updates)
    }

    fun observeRemoteTransactions(uid: String) = cloudRepo.observeTransactions(uid)
    fun observeRemoteBudgets(uid: String) = cloudRepo.observeBudgets(uid)

    suspend fun exportLocalJsonBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val txs = dao.getAllTransactionsRaw()
            val budgets = dao.getAllBudgetsRaw()
            val root = JSONObject().apply {
                put("version", 1)
                put("exportedAtMillis", System.currentTimeMillis())
                put("transactions", JSONArray().apply {
                    txs.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("type", t.type)
                            put("amountMinor", t.amountMinor)
                            put("currencyCode", t.currencyCode)
                            put("categoryId", t.categoryId)
                            put("paymentMethodId", t.paymentMethodId)
                            put("title", t.title)
                            put("note", t.note)
                            put("occurredAtMillis", t.occurredAtMillis)
                            put("clientUpdatedAtMillis", t.clientUpdatedAtMillis)
                            put("deleted", t.deleted)
                        })
                    }
                })
                put("budgets", JSONArray().apply {
                    budgets.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("currencyCode", b.currencyCode)
                            put("categoryId", b.categoryId)
                            put("limitMinor", b.limitMinor)
                            put("clientUpdatedAtMillis", b.clientUpdatedAtMillis)
                            put("deleted", b.deleted)
                        })
                    }
                })
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Cannot open output stream")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importLocalJsonBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("Cannot read backup file")
            val root = JSONObject(raw)
            val txArr = root.optJSONArray("transactions") ?: JSONArray()
            val txEntities = mutableListOf<TransactionEntity>()
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
                txEntities.add(
                    TransactionEntity(
                        id = id,
                        type = o.optString("type", "EXPENSE"),
                        amountMinor = o.optLong("amountMinor", 0L),
                        currencyCode = o.optString("currencyCode", "BDT"),
                        categoryId = o.optString("categoryId", "other_expense"),
                        paymentMethodId = o.optString("paymentMethodId", "cash"),
                        title = o.optString("title", ""),
                        note = o.optString("note", ""),
                        occurredAtMillis = o.optLong("occurredAtMillis", System.currentTimeMillis()),
                        clientUpdatedAtMillis = o.optLong("clientUpdatedAtMillis", System.currentTimeMillis()),
                        deleted = o.optBoolean("deleted", false),
                        pendingSync = true
                    )
                )
            }
            val budgetArr = root.optJSONArray("budgets") ?: JSONArray()
            val budgetEntities = mutableListOf<BudgetEntity>()
            for (i in 0 until budgetArr.length()) {
                val o = budgetArr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isNotBlank()) {
                    budgetEntities.add(
                        BudgetEntity(
                            id = id,
                            currencyCode = o.optString("currencyCode", "BDT"),
                            categoryId = o.optString("categoryId", "OVERALL"),
                            limitMinor = o.optLong("limitMinor", 0L),
                            clientUpdatedAtMillis = o.optLong("clientUpdatedAtMillis", System.currentTimeMillis()),
                            deleted = o.optBoolean("deleted", false),
                            pendingSync = true
                        )
                    )
                }
            }
            if (txEntities.isNotEmpty()) dao.upsertTransactions(txEntities)
            if (budgetEntities.isNotEmpty()) dao.upsertBudgets(budgetEntities)
            Result.success(txEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TransactionEntity.toDomain() = TransactionItem(
        id = id,
        type = TxType.fromName(type),
        amountMinor = amountMinor,
        currency = Currency.fromCode(currencyCode),
        category = ExpenseCategory.fromId(categoryId),
        paymentMethod = PaymentMethod.fromId(paymentMethodId),
        title = title,
        note = note,
        occurredAtMillis = occurredAtMillis,
        clientUpdatedAtMillis = clientUpdatedAtMillis
    )

    private fun BudgetEntity.toDomain() = BudgetItem(
        id = id,
        currency = Currency.fromCode(currencyCode),
        categoryId = categoryId,
        limitMinor = limitMinor,
        clientUpdatedAtMillis = clientUpdatedAtMillis
    )

    private fun RecurringTemplateEntity.toDomain() = RecurringTemplate(
        id = id,
        type = TxType.fromName(type),
        amountMinor = amountMinor,
        currency = Currency.fromCode(currencyCode),
        category = ExpenseCategory.fromId(categoryId),
        paymentMethod = PaymentMethod.fromId(paymentMethodId),
        title = title,
        dayOfMonth = dayOfMonth
    )

    private fun CloudTransaction.toEntity() = TransactionEntity(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        title = title,
        note = note,
        occurredAtMillis = occurredAtMillis,
        clientUpdatedAtMillis = clientUpdatedAtMillis,
        deleted = deleted,
        pendingSync = false
    )

    private fun CloudBudget.toEntity() = BudgetEntity(
        id = id,
        currencyCode = currencyCode,
        categoryId = categoryId,
        limitMinor = limitMinor,
        clientUpdatedAtMillis = clientUpdatedAtMillis,
        deleted = deleted,
        pendingSync = false
    )
}
