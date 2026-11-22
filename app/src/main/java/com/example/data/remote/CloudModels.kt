package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "EXPENSE",
    val amountMinor: Long = 0L,
    val currencyCode: String = "BDT",
    val categoryId: String = "other_expense",
    val paymentMethodId: String = "cash",
    val title: String = "",
    val note: String = "",
    val occurredAtMillis: Long = 0L,
    val clientUpdatedAtMillis: Long = 0L,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currencyCode: String = "BDT",
    val categoryId: String = "OVERALL",
    val limitMinor: Long = 0L,
    val clientUpdatedAtMillis: Long = 0L,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
