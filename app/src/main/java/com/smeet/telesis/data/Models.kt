package com.smeet.telesis.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.YearMonth

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorKey: String,
    val monthlyBudgetPaise: Long = 0,
    val createdAt: Long = Instant.now().toEpochMilli()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["dateTime"]),
        Index(value = ["categoryId"]),
        Index(value = ["merchant"]),
        Index(value = ["smsHash"], unique = true)
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String,
    val categoryId: Long,
    val paymentMode: PaymentMode,
    val accountHint: String = "",
    val dateTime: Long,
    val source: TransactionSource,
    val smsHash: String? = null,
    val confidence: Int = 100,
    val note: String = "",
    val isReviewed: Boolean = true,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

@Entity(tableName = "sms_logs", indices = [Index(value = ["bodyHash"], unique = true)])
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val bodyHash: String,
    val smsDate: Long,
    val parsedStatus: SmsParsedStatus,
    val rawAmountPaise: Long? = null,
    val rawMerchant: String? = null,
    val confidence: Int = 0,
    val reason: String = "",
    val createdAt: Long = Instant.now().toEpochMilli()
)

@Entity(tableName = "rules", indices = [Index(value = ["matchText"], unique = true)])
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchText: String,
    val senderFilter: String = "",
    val categoryId: Long,
    val merchantName: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = Instant.now().toEpochMilli()
)

@Entity(tableName = "budgets", indices = [Index(value = ["categoryId", "monthKey"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthKey: String = YearMonth.now().toString(),
    val limitPaise: Long
)

@Entity(tableName = "recurring_expenses", indices = [Index(value = ["merchant", "amountPaise", "intervalType"], unique = true)])
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val merchant: String,
    val categoryId: Long,
    val paymentMode: PaymentMode,
    val intervalType: RecurringInterval,
    val nextDueDate: Long,
    val note: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

@Entity(tableName = "subscriptions", indices = [Index(value = ["merchant", "amountPaise"], unique = true)])
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amountPaise: Long,
    val categoryName: String,
    val paymentMode: PaymentMode,
    val frequency: RecurringInterval,
    val firstSeen: Long,
    val lastSeen: Long,
    val nextExpectedDate: Long,
    val transactionCount: Int,
    val confidence: Int,
    val active: Boolean = true,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

enum class TransactionType { EXPENSE, INCOME, TRANSFER }
enum class PaymentMode { UPI, CARD, BANK, WALLET, CASH, ATM, NETBANKING, UNKNOWN }
enum class TransactionSource { SMS, MANUAL, IMPORT, RECURRING }
enum class SmsParsedStatus { IMPORTED, REVIEW, IGNORED, DUPLICATE, FAILED }
enum class RecurringInterval { WEEKLY, MONTHLY, YEARLY }

data class ExpenseWithCategory(
    val id: Long,
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorKey: String,
    val paymentMode: PaymentMode,
    val accountHint: String,
    val dateTime: Long,
    val source: TransactionSource,
    val confidence: Int,
    val note: String,
    val isReviewed: Boolean
)

data class CategorySpend(
    val categoryId: Long,
    val name: String,
    val icon: String,
    val colorKey: String,
    val spentPaise: Long,
    val budgetPaise: Long
)

data class MerchantSpend(
    val merchant: String,
    val spentPaise: Long,
    val count: Int
)

data class PaymentModeSpend(
    val paymentMode: PaymentMode,
    val spentPaise: Long
)

data class DailySpend(
    val dayKey: String,
    val spentPaise: Long
)
