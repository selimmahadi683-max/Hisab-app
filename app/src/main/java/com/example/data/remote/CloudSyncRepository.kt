package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.BudgetEntity
import com.example.data.local.TransactionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTransactions(userId: String): Flow<List<CloudTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<CloudBudget>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/budgets"
        emitAll(
            db.collection("users").document(uid).collection("budgets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertTransaction(userId: String, tx: TransactionEntity): Result<Unit> {
        var docPath = "users/$userId/transactions/${tx.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(tx.id.isNotBlank() && tx.amountMinor > 0L) { "Invalid transaction payload" }
            val docRef = db.collection("users").document(uid)
                .collection("transactions").document(tx.id)
            docPath = docRef.path

            val safeTitle = tx.title.take(120)
            val safeNote = tx.note.take(300)
            val safeCat = tx.categoryId.take(40).ifBlank { "other_expense" }
            val safePay = tx.paymentMethodId.take(40).ifBlank { "cash" }
            val safeCurrency = if (tx.currencyCode == "KWD") "KWD" else "BDT"
            val safeType = if (tx.type == "INCOME") "INCOME" else "EXPENSE"

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "type" to safeType,
                        "amountMinor" to tx.amountMinor,
                        "currencyCode" to safeCurrency,
                        "categoryId" to safeCat,
                        "paymentMethodId" to safePay,
                        "title" to safeTitle,
                        "note" to safeNote,
                        "occurredAtMillis" to tx.occurredAtMillis.coerceAtLeast(1L),
                        "clientUpdatedAtMillis" to tx.clientUpdatedAtMillis.coerceAtLeast(1L),
                        "deleted" to tx.deleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "type" to safeType,
                        "amountMinor" to tx.amountMinor,
                        "currencyCode" to safeCurrency,
                        "categoryId" to safeCat,
                        "paymentMethodId" to safePay,
                        "title" to safeTitle,
                        "note" to safeNote,
                        "occurredAtMillis" to tx.occurredAtMillis.coerceAtLeast(1L),
                        "clientUpdatedAtMillis" to tx.clientUpdatedAtMillis.coerceAtLeast(1L),
                        "deleted" to tx.deleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(userId: String, budget: BudgetEntity): Result<Unit> {
        var docPath = "users/$userId/budgets/${budget.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(budget.id.isNotBlank() && budget.limitMinor >= 0L) { "Invalid budget payload" }
            val docRef = db.collection("users").document(uid)
                .collection("budgets").document(budget.id)
            docPath = docRef.path

            val safeCurrency = if (budget.currencyCode == "KWD") "KWD" else "BDT"
            val safeCat = budget.categoryId.take(40).ifBlank { "OVERALL" }

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "currencyCode" to safeCurrency,
                        "categoryId" to safeCat,
                        "limitMinor" to budget.limitMinor,
                        "clientUpdatedAtMillis" to budget.clientUpdatedAtMillis.coerceAtLeast(1L),
                        "deleted" to budget.deleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "currencyCode" to safeCurrency,
                        "categoryId" to safeCat,
                        "limitMinor" to budget.limitMinor,
                        "clientUpdatedAtMillis" to budget.clientUpdatedAtMillis.coerceAtLeast(1L),
                        "deleted" to budget.deleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun fetchAllTransactions(userId: String): Result<List<CloudTransaction>> {
        val path = "users/$userId/transactions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val snap = db.collection("users").document(uid).collection("transactions").get().await()
            val list = snap.toObjects(
                CloudTransaction::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllBudgets(userId: String): Result<List<CloudBudget>> {
        val path = "users/$userId/budgets"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val snap = db.collection("users").document(uid).collection("budgets").get().await()
            val list = snap.toObjects(
                CloudBudget::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }
}
