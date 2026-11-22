package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY occurredAtMillis DESC")
    fun observeActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsRaw(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(txs: List<TransactionEntity>)

    @Query("UPDATE transactions SET deleted = 1, clientUpdatedAtMillis = :updatedAt, pendingSync = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, updatedAt: Long)

    @Query("UPDATE transactions SET pendingSync = 0 WHERE id = :id")
    suspend fun markTransactionSynced(id: String)

    @Query("SELECT * FROM budgets WHERE deleted = 0")
    fun observeActiveBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsRaw(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("UPDATE budgets SET pendingSync = 0 WHERE id = :id")
    suspend fun markBudgetSynced(id: String)

    @Query("SELECT * FROM recurring_templates ORDER BY dayOfMonth ASC")
    fun observeRecurringTemplates(): Flow<List<RecurringTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringTemplate(template: RecurringTemplateEntity)

    @Query("DELETE FROM recurring_templates WHERE id = :id")
    suspend fun deleteRecurringTemplate(id: String)
}
