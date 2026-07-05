package com.smeet.telesis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("UPDATE expenses SET isReviewed = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markReviewed(id: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE smsHash = :smsHash")
    suspend fun countBySmsHash(smsHash: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSmsLog(log: SmsLogEntity): Long

    @Query("SELECT COUNT(*) FROM sms_logs WHERE bodyHash = :hash")
    suspend fun countSmsLog(hash: String): Int

    @Query(
        """
        SELECT e.id, e.amountPaise, e.type, e.merchant, e.categoryId,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorKey AS categoryColorKey,
               e.paymentMode, e.accountHint, e.dateTime, e.source, e.confidence, e.note, e.isReviewed
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY e.dateTime DESC, e.id DESC
        """
    )
    fun observeAllExpenses(): Flow<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT e.id, e.amountPaise, e.type, e.merchant, e.categoryId,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorKey AS categoryColorKey,
               e.paymentMode, e.accountHint, e.dateTime, e.source, e.confidence, e.note, e.isReviewed
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        ORDER BY e.dateTime DESC, e.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentExpenses(limit: Int = 300): Flow<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT e.id, e.amountPaise, e.type, e.merchant, e.categoryId,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorKey AS categoryColorKey,
               e.paymentMode, e.accountHint, e.dateTime, e.source, e.confidence, e.note, e.isReviewed
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.isReviewed = 0
        ORDER BY e.dateTime DESC, e.id DESC
        """
    )
    fun observeReviewQueue(): Flow<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT e.id, e.amountPaise, e.type, e.merchant, e.categoryId,
               c.name AS categoryName, c.icon AS categoryIcon, c.colorKey AS categoryColorKey,
               e.paymentMode, e.accountHint, e.dateTime, e.source, e.confidence, e.note, e.isReviewed
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.dateTime BETWEEN :start AND :end
        ORDER BY e.dateTime DESC, e.id DESC
        """
    )
    fun observeExpensesBetween(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    @Query(
        """
        SELECT COALESCE(SUM(amountPaise), 0)
        FROM expenses
        WHERE type = 'EXPENSE' AND dateTime BETWEEN :start AND :end AND isReviewed = 1
        """
    )
    fun observeSpentBetween(start: Long, end: Long): Flow<Long>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS name, c.icon AS icon, c.colorKey AS colorKey,
               COALESCE(SUM(CASE WHEN e.type = 'EXPENSE' AND e.isReviewed = 1 THEN e.amountPaise ELSE 0 END), 0) AS spentPaise,
               c.monthlyBudgetPaise AS budgetPaise
        FROM categories c
        LEFT JOIN expenses e ON e.categoryId = c.id AND e.dateTime BETWEEN :start AND :end
        GROUP BY c.id
        ORDER BY spentPaise DESC, c.name ASC
        """
    )
    fun observeCategorySpend(start: Long, end: Long): Flow<List<CategorySpend>>

    @Query(
        """
        SELECT merchant, COALESCE(SUM(amountPaise), 0) AS spentPaise, COUNT(*) AS count
        FROM expenses
        WHERE type = 'EXPENSE' AND isReviewed = 1 AND dateTime BETWEEN :start AND :end
        GROUP BY merchant
        ORDER BY spentPaise DESC
        LIMIT :limit
        """
    )
    fun observeTopMerchants(start: Long, end: Long, limit: Int = 10): Flow<List<MerchantSpend>>

    @Query(
        """
        SELECT paymentMode, COALESCE(SUM(amountPaise), 0) AS spentPaise
        FROM expenses
        WHERE type = 'EXPENSE' AND isReviewed = 1 AND dateTime BETWEEN :start AND :end
        GROUP BY paymentMode
        ORDER BY spentPaise DESC
        """
    )
    fun observePaymentModeSpend(start: Long, end: Long): Flow<List<PaymentModeSpend>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', dateTime / 1000, 'unixepoch', 'localtime') AS dayKey,
               COALESCE(SUM(amountPaise), 0) AS spentPaise
        FROM expenses
        WHERE type = 'EXPENSE' AND isReviewed = 1 AND dateTime BETWEEN :start AND :end
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """
    )
    fun observeDailySpend(start: Long, end: Long): Flow<List<DailySpend>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY matchText ASC")
    fun observeRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY matchText ASC")
    suspend fun getEnabledRules(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: RuleEntity): Long

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM recurring_expenses WHERE enabled = 1 ORDER BY nextDueDate ASC")
    fun observeRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE enabled = 1 AND nextDueDate <= :now ORDER BY nextDueDate ASC")
    suspend fun getDueRecurring(now: Long): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringExpense(item: RecurringExpenseEntity): Long

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpense(id: Long)

    @Query("SELECT * FROM subscriptions WHERE active = 1 ORDER BY nextExpectedDate ASC, confidence DESC")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(subscription: SubscriptionEntity): Long

    @Query("DELETE FROM subscriptions")
    suspend fun clearSubscriptions()

    @Query("SELECT * FROM expenses ORDER BY dateTime DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE type = 'EXPENSE' AND isReviewed = 1 ORDER BY dateTime ASC")
    suspend fun getReviewedExpensesAscending(): List<ExpenseEntity>

    @Query("SELECT * FROM rules ORDER BY matchText ASC")
    suspend fun getAllRules(): List<RuleEntity>

    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC")
    suspend fun getAllRecurringExpenses(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM subscriptions ORDER BY nextExpectedDate ASC")
    suspend fun getAllSubscriptions(): List<SubscriptionEntity>

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()
}
