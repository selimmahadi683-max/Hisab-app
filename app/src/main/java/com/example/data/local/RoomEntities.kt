package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAtMillis"]),
        Index(value = ["currencyCode"]),
        Index(value = ["deleted"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String,
    val paymentMethodId: String,
    val title: String,
    val note: String,
    val occurredAtMillis: Long,
    val clientUpdatedAtMillis: Long,
    val deleted: Boolean = false,
    val pendingSync: Boolean = true
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["currencyCode"])]
)
data class BudgetEntity(
    @PrimaryKey val id: String, // e.g., BDT_OVERALL, BDT_food
    val currencyCode: String,
    val categoryId: String,
    val limitMinor: Long,
    val clientUpdatedAtMillis: Long,
    val deleted: Boolean = false,
    val pendingSync: Boolean = true
)

@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String,
    val paymentMethodId: String,
    val title: String,
    val dayOfMonth: Int
)
