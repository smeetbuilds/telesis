package com.smeet.telesis.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smeet.telesis.TelesisApp
import com.smeet.telesis.data.CategoryEntity
import com.smeet.telesis.data.CategorySpend
import com.smeet.telesis.data.DailySpend
import com.smeet.telesis.data.ExpenseWithCategory
import com.smeet.telesis.data.ImportBackupReport
import com.smeet.telesis.data.ImportReport
import com.smeet.telesis.data.MerchantSpend
import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.PaymentModeSpend
import com.smeet.telesis.data.RecurringExpenseEntity
import com.smeet.telesis.data.RecurringInterval
import com.smeet.telesis.data.RuleEntity
import com.smeet.telesis.data.SubscriptionEntity
import com.smeet.telesis.data.TransactionType
import com.smeet.telesis.sms.SmsReader
import com.smeet.telesis.util.DateUtils
import com.smeet.telesis.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TelesisApp
    private val repo = app.repository
    private val smsReader = SmsReader(application.contentResolver)

    private val dashboardPrimary = combine(
        repo.observeMonthlyTotal(),
        repo.observeTodayTotal(),
        repo.observeMonthlyCategorySpend(),
        repo.recentExpenses,
        repo.reviewQueue
    ) { monthSpent, todaySpent, categories, recent, review ->
        DashboardPrimary(monthSpent, todaySpent, categories, recent, review)
    }

    private val dashboardSecondary = combine(
        repo.observeTopMerchants(),
        repo.observePaymentModes(),
        repo.observeDailySpend(),
        repo.recurringExpenses,
        repo.subscriptions
    ) { merchants, paymentModes, dailySpend, recurring, subscriptions ->
        DashboardSecondary(merchants, paymentModes, dailySpend, recurring, subscriptions)
    }

    val dashboard: StateFlow<DashboardState> = combine(dashboardPrimary, dashboardSecondary) { primary, secondary ->
        DashboardState(
            monthSpentPaise = primary.monthSpentPaise,
            todaySpentPaise = primary.todaySpentPaise,
            categories = primary.categories,
            recent = primary.recent.take(12),
            review = primary.review,
            merchants = secondary.merchants,
            paymentModes = secondary.paymentModes,
            dailySpend = secondary.dailySpend,
            recurring = secondary.recurring,
            subscriptions = secondary.subscriptions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    val categories: StateFlow<List<CategoryEntity>> = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val expenses: StateFlow<List<ExpenseWithCategory>> = repo.recentExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reviewQueue: StateFlow<List<ExpenseWithCategory>> = repo.reviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rules: StateFlow<List<RuleEntity>> = repo.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recurring: StateFlow<List<RecurringExpenseEntity>> = repo.recurringExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val subscriptions: StateFlow<List<SubscriptionEntity>> = repo.subscriptions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var lastImportReport: ImportReport? = null
        private set

    init {
        viewModelScope.launch { repo.runStartupMaintenance() }
    }

    fun addExpense(amount: String, merchant: String, category: String, mode: PaymentMode, note: String, dateMillis: Long?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val paise = Money.parseToPaise(amount)
            if (paise == null || paise <= 0) {
                onDone(false, "Enter a valid amount")
                return@launch
            }
            if (dateMillis == null) {
                onDone(false, "Enter a valid date as YYYY-MM-DD")
                return@launch
            }
            repo.addManualExpense(paise, merchant, category, mode, note, dateTime = dateMillis)
            onDone(true, "Expense added")
        }
    }

    fun updateExpense(id: Long, amount: String, merchant: String, category: String, mode: PaymentMode, note: String, dateMillis: Long?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val paise = Money.parseToPaise(amount)
            if (paise == null || paise <= 0) {
                onDone(false, "Enter a valid amount")
                return@launch
            }
            if (dateMillis == null) {
                onDone(false, "Enter a valid date as YYYY-MM-DD")
                return@launch
            }
            val ok = repo.updateManualExpense(id, paise, merchant, category, mode, note, dateMillis)
            onDone(ok, if (ok) "Expense updated" else "Expense not found")
        }
    }

    fun importSms(limit: Int = 3000, onDone: (ImportReport) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val messages = smsReader.readInbox(limit)
                val report = repo.importSmsBatch(messages)
                lastImportReport = report
                onDone(report)
            } catch (t: Throwable) {
                onError(t.message ?: "SMS import failed")
            }
        }
    }

    fun approveExpense(id: Long) {
        viewModelScope.launch { repo.approveExpense(id) }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { repo.deleteExpense(id) }
    }

    fun setBudget(category: CategoryEntity, amount: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val paise = Money.parseToPaise(amount)
            if (paise == null || paise < 0) {
                onDone(false, "Enter a valid budget")
                return@launch
            }
            repo.updateCategoryBudget(category, paise)
            onDone(true, "Budget updated")
        }
    }

    fun addRule(matchText: String, category: String, merchantName: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (matchText.trim().length < 2) {
                onDone(false, "Rule text is too short")
                return@launch
            }
            repo.addRule(matchText, category, merchantName)
            onDone(true, "Rule saved")
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { repo.deleteRule(id) }
    }

    fun addRecurring(amount: String, merchant: String, category: String, mode: PaymentMode, interval: RecurringInterval, note: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val paise = Money.parseToPaise(amount)
            if (paise == null || paise <= 0) {
                onDone(false, "Enter a valid recurring amount")
                return@launch
            }
            repo.addRecurringExpense(paise, merchant, category, mode, interval, nextDate(interval), note)
            onDone(true, "Recurring expense saved")
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch { repo.deleteRecurringExpense(id) }
    }

    fun generateDueRecurring(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val count = repo.materializeDueRecurringExpenses()
            onDone(if (count == 1) "1 due recurring expense added" else "$count due recurring expenses added")
        }
    }

    fun detectSubscriptions(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val count = repo.detectSubscriptions(force = true)
            onDone(if (count == 1) "1 subscription candidate detected" else "$count subscription candidates detected")
        }
    }

    private fun nextDate(interval: RecurringInterval): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val next = when (interval) {
            RecurringInterval.WEEKLY -> today.plusWeeks(1)
            RecurringInterval.MONTHLY -> today.plusMonths(1)
            RecurringInterval.YEARLY -> today.plusYears(1)
        }
        return next.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun setPin(pin: String, onDone: (Boolean, String) -> Unit) {
        val ok = app.appLockManager.setPin(pin)
        onDone(ok, if (ok) "PIN lock enabled" else "PIN must be 4 to 8 digits")
    }

    fun disablePin(onDone: (String) -> Unit) {
        app.appLockManager.disablePin()
        onDone("PIN and biometric lock disabled")
    }

    fun setBiometric(enabled: Boolean, onDone: (Boolean, String) -> Unit) {
        val ok = app.appLockManager.setBiometricEnabled(enabled)
        onDone(ok, if (ok) "Biometric unlock ${if (enabled) "enabled" else "disabled"}" else "Set a PIN first and make sure biometric/device lock is available")
    }

    fun exportBackup(uri: Uri, context: Context, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { BackupJsonBuilder.build(repo.exportSnapshot()) }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(json) }
                    } ?: error("Unable to open selected file")
                }
                onDone(true, "JSON backup exported")
            } catch (t: Throwable) {
                onDone(false, t.message ?: "Export failed")
            }
        }
    }

    fun exportCsv(uri: Uri, context: Context, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = withContext(Dispatchers.IO) { repo.exportCsv() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(csv) }
                    } ?: error("Unable to open selected CSV file")
                }
                onDone(true, "CSV exported")
            } catch (t: Throwable) {
                onDone(false, t.message ?: "CSV export failed")
            }
        }
    }

    fun importBackup(uri: Uri, context: Context, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val declaredSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    if (declaredSize > MAX_BACKUP_BYTES) error("Backup file is too large")
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Unable to read selected backup file")
                    if (text.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) error("Backup file is too large")
                    text
                }
                val report: ImportBackupReport = withContext(Dispatchers.IO) { repo.importBackup(json) }
                onDone(true, "Restored ${report.expenses} expenses, ${report.categories} categories, ${report.rules} rules, ${report.recurring} recurring items")
            } catch (t: Throwable) {
                onDone(false, t.message ?: "Backup restore failed")
            }
        }
    }

    companion object {
        private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    }
}

private data class DashboardPrimary(
    val monthSpentPaise: Long,
    val todaySpentPaise: Long,
    val categories: List<CategorySpend>,
    val recent: List<ExpenseWithCategory>,
    val review: List<ExpenseWithCategory>
)

private data class DashboardSecondary(
    val merchants: List<MerchantSpend>,
    val paymentModes: List<PaymentModeSpend>,
    val dailySpend: List<DailySpend>,
    val recurring: List<RecurringExpenseEntity>,
    val subscriptions: List<SubscriptionEntity>
)

data class DashboardState(
    val monthSpentPaise: Long = 0,
    val todaySpentPaise: Long = 0,
    val categories: List<CategorySpend> = emptyList(),
    val recent: List<ExpenseWithCategory> = emptyList(),
    val review: List<ExpenseWithCategory> = emptyList(),
    val merchants: List<MerchantSpend> = emptyList(),
    val paymentModes: List<PaymentModeSpend> = emptyList(),
    val dailySpend: List<DailySpend> = emptyList(),
    val recurring: List<RecurringExpenseEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList()
) {
    val budgetPaise: Long get() = categories.sumOf { it.budgetPaise }
    val remainingPaise: Long get() = (budgetPaise - monthSpentPaise).coerceAtLeast(0)
    val topCategory: CategorySpend? get() = categories.filter { it.spentPaise > 0 }.maxByOrNull { it.spentPaise }
    val fixedCommitmentsPaise: Long get() = subscriptions.sumOf { it.amountPaise } + recurring.sumOf { it.amountPaise }
}

object BackupJsonBuilder {
    fun build(snapshot: com.smeet.telesis.data.ExportSnapshot): String {
        val root = JSONObject()
            .put("app", "Telesis")
            .put("version", "1.0.0")
            .put("exportedAt", Instant.ofEpochMilli(snapshot.exportedAt).toString())
            .put("categories", JSONArray().also { arr ->
                snapshot.categories.forEach { c ->
                    arr.put(JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("icon", c.icon)
                        .put("colorKey", c.colorKey)
                        .put("monthlyBudgetPaise", c.monthlyBudgetPaise))
                }
            })
            .put("expenses", JSONArray().also { arr ->
                snapshot.expenses.forEach { e ->
                    arr.put(JSONObject()
                        .put("id", e.id)
                        .put("amountPaise", e.amountPaise)
                        .put("type", e.type.name)
                        .put("merchant", e.merchant)
                        .put("categoryId", e.categoryId)
                        .put("paymentMode", e.paymentMode.name)
                        .put("accountHint", e.accountHint)
                        .put("dateTime", e.dateTime)
                        .put("source", e.source.name)
                        .put("confidence", e.confidence)
                        .put("note", e.note)
                        .put("isReviewed", e.isReviewed))
                }
            })
            .put("rules", JSONArray().also { arr ->
                snapshot.rules.forEach { r ->
                    arr.put(JSONObject()
                        .put("id", r.id)
                        .put("matchText", r.matchText)
                        .put("senderFilter", r.senderFilter)
                        .put("categoryId", r.categoryId)
                        .put("merchantName", r.merchantName)
                        .put("enabled", r.enabled))
                }
            })
            .put("recurring", JSONArray().also { arr ->
                snapshot.recurring.forEach { r ->
                    arr.put(JSONObject()
                        .put("id", r.id)
                        .put("amountPaise", r.amountPaise)
                        .put("merchant", r.merchant)
                        .put("categoryId", r.categoryId)
                        .put("paymentMode", r.paymentMode.name)
                        .put("intervalType", r.intervalType.name)
                        .put("nextDueDate", r.nextDueDate)
                        .put("note", r.note)
                        .put("enabled", r.enabled))
                }
            })
            .put("subscriptions", JSONArray().also { arr ->
                snapshot.subscriptions.forEach { sub ->
                    arr.put(JSONObject()
                        .put("id", sub.id)
                        .put("merchant", sub.merchant)
                        .put("amountPaise", sub.amountPaise)
                        .put("categoryName", sub.categoryName)
                        .put("paymentMode", sub.paymentMode.name)
                        .put("frequency", sub.frequency.name)
                        .put("firstSeen", sub.firstSeen)
                        .put("lastSeen", sub.lastSeen)
                        .put("nextExpectedDate", sub.nextExpectedDate)
                        .put("transactionCount", sub.transactionCount)
                        .put("confidence", sub.confidence)
                        .put("active", sub.active))
                }
            })
        return root.toString(2) + "\n"
    }
}
