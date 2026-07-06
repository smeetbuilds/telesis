package com.smeet.telesis.data

import androidx.room.withTransaction
import com.smeet.telesis.sms.CategoryEngine
import com.smeet.telesis.sms.DeviceSms
import com.smeet.telesis.sms.ParsedSms
import com.smeet.telesis.sms.SmsParser
import com.smeet.telesis.util.DateUtils
import com.smeet.telesis.util.Security
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class ExpenseRepository(private val database: TelesisDatabase) {
    private val dao: ExpenseDao = database.expenseDao()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val recentExpenses: Flow<List<ExpenseWithCategory>> = dao.observeRecentExpenses(limit = 500)
    val reviewQueue: Flow<List<ExpenseWithCategory>> = dao.observeReviewQueue()
    val rules: Flow<List<RuleEntity>> = dao.observeRules()
    val budgets: Flow<List<BudgetEntity>> = dao.observeBudgets()
    val recurringExpenses: Flow<List<RecurringExpenseEntity>> = dao.observeRecurringExpenses()
    val subscriptions: Flow<List<SubscriptionEntity>> = dao.observeSubscriptions()
    private var lastSubscriptionDetectionAt: Long = 0L

    fun observeMonthlyTotal(): Flow<Long> = dao.observeSpentBetween(DateUtils.startOfCurrentMonth(), DateUtils.endOfCurrentMonth())
    fun observeTodayTotal(): Flow<Long> = dao.observeSpentBetween(DateUtils.startOfToday(), DateUtils.endOfToday())
    fun observeMonthlyCategorySpend(): Flow<List<CategorySpend>> = dao.observeCategorySpend(DateUtils.startOfCurrentMonth(), DateUtils.endOfCurrentMonth())
    fun observeTopMerchants(): Flow<List<MerchantSpend>> = dao.observeTopMerchants(DateUtils.startOfCurrentMonth(), DateUtils.endOfCurrentMonth())
    fun observePaymentModes(): Flow<List<PaymentModeSpend>> = dao.observePaymentModeSpend(DateUtils.startOfCurrentMonth(), DateUtils.endOfCurrentMonth())
    fun observeDailySpend(): Flow<List<DailySpend>> = dao.observeDailySpend(DateUtils.startOfCurrentMonth(), DateUtils.endOfCurrentMonth())

    suspend fun seedDefaults() {
        CategoryEngine.defaultCategories.forEach { default ->
            dao.insertCategory(
                CategoryEntity(
                    name = default.name,
                    icon = default.icon,
                    colorKey = default.colorKey,
                    monthlyBudgetPaise = default.monthlyBudgetPaise
                )
            )
        }
    }

    suspend fun runStartupMaintenance() {
        seedDefaults()
        materializeDueRecurringExpenses()
        detectSubscriptions(force = true)
    }

    suspend fun addManualExpense(
        amountPaise: Long,
        merchant: String,
        categoryName: String,
        paymentMode: PaymentMode,
        note: String,
        dateTime: Long = DateUtils.now(),
        type: TransactionType = TransactionType.EXPENSE,
        source: TransactionSource = TransactionSource.MANUAL
    ): Long {
        val category = ensureCategory(categoryName)
        val id = dao.insertExpense(
            ExpenseEntity(
                amountPaise = amountPaise,
                type = type,
                merchant = merchant.trim().ifBlank { "Manual Expense" }.take(52),
                categoryId = category.id,
                paymentMode = paymentMode,
                dateTime = dateTime,
                source = source,
                confidence = 100,
                note = note.take(240),
                isReviewed = true
            )
        )
        detectSubscriptions()
        return id
    }

    suspend fun updateManualExpense(
        id: Long,
        amountPaise: Long,
        merchant: String,
        categoryName: String,
        paymentMode: PaymentMode,
        note: String,
        dateTime: Long,
        type: TransactionType = TransactionType.EXPENSE
    ): Boolean {
        val existing = dao.getExpenseById(id) ?: return false
        val category = ensureCategory(categoryName)
        dao.updateExpense(
            existing.copy(
                amountPaise = amountPaise,
                type = type,
                merchant = merchant.trim().ifBlank { "Manual Expense" }.take(52),
                categoryId = category.id,
                paymentMode = paymentMode,
                dateTime = dateTime,
                confidence = 100,
                note = note.trim().take(240),
                isReviewed = true,
                updatedAt = DateUtils.now()
            )
        )
        detectSubscriptions(force = true)
        return true
    }

    suspend fun deleteExpense(id: Long) {
        dao.deleteExpense(id)
        detectSubscriptions(force = true)
    }

    suspend fun approveExpense(id: Long) {
        dao.markReviewed(id, DateUtils.now())
        detectSubscriptions(force = true)
    }

    suspend fun updateCategoryBudget(category: CategoryEntity, budgetPaise: Long) {
        dao.updateCategory(category.copy(monthlyBudgetPaise = budgetPaise))
        dao.upsertBudget(BudgetEntity(categoryId = category.id, monthKey = YearMonth.now().toString(), limitPaise = budgetPaise))
    }

    suspend fun addRule(matchText: String, categoryName: String, merchantName: String = "") {
        val cleanMatch = matchText.lowercase().trim().take(80)
        if (cleanMatch.length < 2) return
        val category = ensureCategory(categoryName)
        dao.upsertRule(RuleEntity(matchText = cleanMatch, categoryId = category.id, merchantName = merchantName.trim().take(52)))
    }

    suspend fun deleteRule(id: Long) = dao.deleteRule(id)

    suspend fun addRecurringExpense(
        amountPaise: Long,
        merchant: String,
        categoryName: String,
        paymentMode: PaymentMode,
        interval: RecurringInterval,
        nextDueDate: Long,
        note: String
    ) {
        val category = ensureCategory(categoryName)
        dao.upsertRecurringExpense(
            RecurringExpenseEntity(
                amountPaise = amountPaise,
                merchant = merchant.trim().ifBlank { "Recurring Expense" }.take(52),
                categoryId = category.id,
                paymentMode = paymentMode,
                intervalType = interval,
                nextDueDate = nextDueDate,
                note = note.trim().take(240)
            )
        )
    }

    suspend fun deleteRecurringExpense(id: Long) = dao.deleteRecurringExpense(id)

    suspend fun materializeDueRecurringExpenses(now: Long = DateUtils.now()): Int {
        val due = dao.getDueRecurring(now)
        var created = 0
        due.forEach { item ->
            val recurringHash = Security.sha256("recurring|${item.id}|${item.nextDueDate}|${item.amountPaise}|${item.merchant}")
            database.withTransaction {
                val category = dao.getCategoryById(item.categoryId) ?: ensureCategory("Other")
                if (dao.countBySmsHash(recurringHash) == 0) {
                    val expenseId = dao.insertExpense(
                        ExpenseEntity(
                            amountPaise = item.amountPaise,
                            type = TransactionType.EXPENSE,
                            merchant = item.merchant,
                            categoryId = category.id,
                            paymentMode = item.paymentMode,
                            dateTime = item.nextDueDate,
                            source = TransactionSource.RECURRING,
                            smsHash = recurringHash,
                            confidence = 100,
                            note = item.note.ifBlank { "Generated from recurring expense" }.take(240),
                            isReviewed = true
                        )
                    )
                    if (expenseId > 0) created++
                }
                dao.upsertRecurringExpense(
                    item.copy(
                        nextDueDate = nextDue(item.nextDueDate, item.intervalType),
                        updatedAt = DateUtils.now()
                    )
                )
            }
        }
        if (created > 0) detectSubscriptions(force = true)
        return created
    }

    suspend fun importSmsBatch(messages: List<DeviceSms>): ImportReport {
        seedDefaults()
        val cachedRules = dao.getEnabledRules()
        val seededCategories = dao.getCategories()
        val cachedCategoriesById = seededCategories.associateBy { it.id }.toMutableMap()
        val cachedCategoriesByName = seededCategories.associateBy { it.name.lowercase() }.toMutableMap()
        var imported = 0
        var review = 0
        var ignored = 0
        var duplicate = 0
        var failed = 0
        messages.forEach { sms ->
            when (importSingleSms(sms.sender, sms.body, sms.date, cachedRules, cachedCategoriesById, cachedCategoriesByName)) {
                ImportOutcome.Imported -> imported++
                ImportOutcome.Review -> review++
                ImportOutcome.Ignored -> ignored++
                ImportOutcome.Duplicate -> duplicate++
                ImportOutcome.Failed -> failed++
            }
        }
        detectSubscriptions(force = true)
        return ImportReport(imported, review, ignored, duplicate, failed)
    }

    suspend fun importSingleSms(
        sender: String,
        body: String,
        date: Long,
        cachedRules: List<RuleEntity>? = null,
        cachedCategoriesById: MutableMap<Long, CategoryEntity>? = null,
        cachedCategoriesByName: MutableMap<String, CategoryEntity>? = null
    ): ImportOutcome {
        if (body.isBlank()) return ImportOutcome.Ignored

        val legacyHash = Security.sha256(sender.trim() + "|" + body.trim() + "|" + date)
        val hash = Security.sha256(SmsParser.PARSER_VERSION + "|" + sender.trim() + "|" + body.trim() + "|" + date)
        if (dao.getSmsLogByHash(hash) != null || dao.countBySmsHash(hash) > 0) {
            return ImportOutcome.Duplicate
        }

        return when (val parsed = SmsParser.parse(sender, body, date)) {
            is ParsedSms.Ignored -> {
                if (dao.countBySmsHash(legacyHash) > 0) {
                    dao.deleteSmsExpenseByHash(legacyHash)
                }
                dao.insertSmsLog(SmsLogEntity(sender = sender.take(80), bodyHash = hash, smsDate = date, parsedStatus = SmsParsedStatus.IGNORED, reason = parsed.reason.take(160)))
                ImportOutcome.Ignored
            }
            is ParsedSms.Transaction -> {
                if (!parsed.isSmsLedgerType()) {
                    dao.insertSmsLog(
                        SmsLogEntity(
                            sender = sender.take(80),
                            bodyHash = hash,
                            smsDate = date,
                            parsedStatus = SmsParsedStatus.IGNORED,
                            rawAmountPaise = parsed.amountPaise,
                            rawMerchant = parsed.merchant.take(80),
                            confidence = parsed.confidence,
                            reason = "Ignored non-ledger SMS transaction type: ${parsed.type}"
                        )
                    )
                    return ImportOutcome.Ignored
                }
                if (dao.countBySmsHash(legacyHash) > 0) {
                    dao.insertSmsLog(SmsLogEntity(sender = sender.take(80), bodyHash = hash, smsDate = date, parsedStatus = SmsParsedStatus.DUPLICATE, reason = "Already imported by legacy parser hash"))
                    return ImportOutcome.Duplicate
                }

                val activeRules = cachedRules ?: dao.getEnabledRules()
                val rule = activeRules.firstOrNull { rule ->
                    val sourceText = (parsed.merchant + " " + body).lowercase()
                    sourceText.contains(rule.matchText.lowercase()) &&
                        (rule.senderFilter.isBlank() || sender.contains(rule.senderFilter, ignoreCase = true))
                }
                val category = if (rule != null) {
                    cachedCategoriesById?.get(rule.categoryId) ?: dao.getCategoryById(rule.categoryId) ?: ensureCategoryCached(parsed.suggestedCategory, cachedCategoriesByName, cachedCategoriesById)
                } else ensureCategoryCached(parsed.suggestedCategory, cachedCategoriesByName, cachedCategoriesById)

                val merchant = (rule?.merchantName?.takeIf { it.isNotBlank() } ?: parsed.merchant).take(52)
                val requiresReview = parsed.confidence < 78
                val row = ExpenseEntity(
                    amountPaise = parsed.amountPaise,
                    type = parsed.type,
                    merchant = merchant,
                    categoryId = category.id,
                    paymentMode = parsed.paymentMode,
                    accountHint = parsed.accountHint,
                    dateTime = parsed.smsDate,
                    source = TransactionSource.SMS,
                    smsHash = hash,
                    confidence = parsed.confidence,
                    note = if (requiresReview) "Imported from SMS. Please review." else "Imported from SMS.",
                    isReviewed = !requiresReview
                )
                val id = dao.insertExpense(row)
                val status = if (id > 0 && requiresReview) SmsParsedStatus.REVIEW else if (id > 0) SmsParsedStatus.IMPORTED else SmsParsedStatus.DUPLICATE
                dao.insertSmsLog(
                    SmsLogEntity(
                        sender = sender.take(80),
                        bodyHash = hash,
                        smsDate = date,
                        parsedStatus = status,
                        rawAmountPaise = parsed.amountPaise,
                        rawMerchant = parsed.merchant.take(80),
                        confidence = parsed.confidence
                    )
                )
                when (status) {
                    SmsParsedStatus.REVIEW -> ImportOutcome.Review
                    SmsParsedStatus.IMPORTED -> ImportOutcome.Imported
                    SmsParsedStatus.DUPLICATE -> ImportOutcome.Duplicate
                    else -> ImportOutcome.Failed
                }
            }
        }
    }

    suspend fun detectSubscriptions(force: Boolean = false): Int {
        val now = DateUtils.now()
        if (!force && now - lastSubscriptionDetectionAt < 60_000L) return dao.getAllSubscriptions().size
        lastSubscriptionDetectionAt = now
        val recentFloor = now - ChronoUnit.DAYS.duration.toMillis() * 730
        val expenses = dao.getReviewedExpensesAscending().filter { it.dateTime >= recentFloor }
        val categories = dao.getCategories().associateBy { it.id }
        val grouped = expenses
            .filter { it.type == TransactionType.EXPENSE && it.amountPaise > 0 }
            .groupBy { it.merchant.lowercase().trim() to it.amountPaise }

        dao.clearSubscriptions()
        var created = 0
        grouped.values.forEach { rows ->
            val sorted = rows.sortedBy { it.dateTime }
            val merchant = sorted.lastOrNull()?.merchant ?: return@forEach
            val categoryName = categories[sorted.last().categoryId]?.name ?: "Other"
            val knownSubscription = CategoryEngine.detect(merchant, merchant, sorted.last().paymentMode, TransactionType.EXPENSE) == "Subscriptions"
            if (sorted.size < 2 && !knownSubscription) return@forEach

            val intervals = sorted.zipWithNext { a, b -> daysBetween(a.dateTime, b.dateTime) }.filter { it > 0 }
            val interval = inferInterval(intervals, knownSubscription) ?: return@forEach
            val confidence = when {
                sorted.size >= 4 -> 94
                sorted.size == 3 -> 86
                knownSubscription -> 78
                else -> 68
            }
            val first = sorted.first().dateTime
            val last = sorted.last().dateTime
            val next = nextDue(last, interval)
            dao.upsertSubscription(
                SubscriptionEntity(
                    merchant = merchant,
                    amountPaise = sorted.last().amountPaise,
                    categoryName = categoryName,
                    paymentMode = sorted.last().paymentMode,
                    frequency = interval,
                    firstSeen = first,
                    lastSeen = last,
                    nextExpectedDate = next,
                    transactionCount = sorted.size,
                    confidence = confidence
                )
            )
            created++
        }
        return created
    }

    private fun inferInterval(intervalDays: List<Long>, knownSubscription: Boolean): RecurringInterval? {
        if (intervalDays.isEmpty()) return if (knownSubscription) RecurringInterval.MONTHLY else null
        val avg = intervalDays.average()
        return when {
            avg in 5.0..9.0 -> RecurringInterval.WEEKLY
            avg in 25.0..36.0 -> RecurringInterval.MONTHLY
            avg in 330.0..400.0 -> RecurringInterval.YEARLY
            knownSubscription -> RecurringInterval.MONTHLY
            else -> null
        }
    }

    private fun daysBetween(a: Long, b: Long): Long {
        val zone = ZoneId.systemDefault()
        val da = Instant.ofEpochMilli(a).atZone(zone).toLocalDate()
        val db = Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
        return abs(ChronoUnit.DAYS.between(da, db))
    }

    private fun nextDue(date: Long, interval: RecurringInterval): Long {
        val zone = ZoneId.systemDefault()
        val base = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
        val next = when (interval) {
            RecurringInterval.WEEKLY -> base.plusWeeks(1)
            RecurringInterval.MONTHLY -> base.plusMonths(1)
            RecurringInterval.YEARLY -> base.plusYears(1)
        }
        return next.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private suspend fun ensureCategoryCached(
        name: String,
        cacheByName: MutableMap<String, CategoryEntity>?,
        cacheById: MutableMap<Long, CategoryEntity>?
    ): CategoryEntity {
        val key = name.ifBlank { "Other" }.lowercase()
        cacheByName?.get(key)?.let { return it }
        val category = ensureCategory(name)
        cacheByName?.put(category.name.lowercase(), category)
        cacheById?.put(category.id, category)
        return category
    }

    private suspend fun ensureCategory(name: String): CategoryEntity {
        val cleanName = name.ifBlank { "Other" }.take(48)
        dao.getCategoryByName(cleanName)?.let { return it }
        val default = CategoryEngine.defaultCategories.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        val categoryName = default?.name ?: cleanName
        val id = dao.insertCategory(
            CategoryEntity(
                name = categoryName,
                icon = default?.icon ?: "•",
                colorKey = default?.colorKey ?: "neutral",
                monthlyBudgetPaise = default?.monthlyBudgetPaise ?: 0
            )
        )
        return if (id > 0) dao.getCategoryByName(categoryName)!! else dao.getCategoryByName(categoryName) ?: dao.getCategoryByName("Other")!!
    }

    suspend fun exportSnapshot(): ExportSnapshot = ExportSnapshot(
        exportedAt = DateUtils.now(),
        categories = dao.getCategories(),
        expenses = dao.getAllExpenses(),
        rules = dao.getAllRules(),
        budgets = dao.getAllBudgets(),
        recurring = dao.getAllRecurringExpenses(),
        subscriptions = dao.getAllSubscriptions()
    )

    suspend fun exportCsv(): String {
        val categories = dao.getCategories().associateBy { it.id }
        val rows = dao.getAllExpenses()
        return buildString {
            appendLine("id,date,type,amount,merchant,category,payment_mode,source,confidence,reviewed,note")
            rows.forEach { e ->
                val category = categories[e.categoryId]?.name ?: "Other"
                appendCsv(e.id.toString()); append(',')
                appendCsv(Instant.ofEpochMilli(e.dateTime).toString()); append(',')
                appendCsv(e.type.name); append(',')
                appendCsv((e.amountPaise / 100.0).toString()); append(',')
                appendCsv(e.merchant); append(',')
                appendCsv(category); append(',')
                appendCsv(e.paymentMode.name); append(',')
                appendCsv(e.source.name); append(',')
                appendCsv(e.confidence.toString()); append(',')
                appendCsv(e.isReviewed.toString()); append(',')
                appendCsv(e.note)
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendCsv(value: String) {
        val safe = value.trimStart().let { trimmed ->
            if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) "'" + value else value
        }
        append('"')
        append(safe.replace("\"", "\"\""))
        append('"')
    }

    suspend fun importBackup(json: String): ImportBackupReport {
        val obj = JSONObject(json)
        validateBackupEnvelope(obj)
        val categories = obj.optJSONArray("categories") ?: JSONArray()
        val expenseArray = obj.optJSONArray("expenses") ?: JSONArray()
        val ruleArray = obj.optJSONArray("rules") ?: JSONArray()
        val recArray = obj.optJSONArray("recurring") ?: JSONArray()
        validateBackupSize(categories, expenseArray, ruleArray, recArray)

        return database.withTransaction {
            seedDefaults()
            val idMap = mutableMapOf<Long, Long>()
            for (i in 0 until categories.length()) {
                val c = categories.getJSONObject(i)
                val oldId = c.optLong("id", 0)
                val rawName = c.optString("name", "Other").trim().take(48)
                val default = CategoryEngine.defaultCategories.firstOrNull { it.name.equals(rawName, true) }
                val categoryName = default?.name ?: rawName.ifBlank { "Other" }
                val insertedId = dao.insertCategory(
                    CategoryEntity(
                        name = categoryName,
                        icon = c.optString("icon", default?.icon ?: "•").take(8),
                        colorKey = c.optString("colorKey", default?.colorKey ?: "neutral").take(24),
                        monthlyBudgetPaise = validNonNegativeAmount(c.optLong("monthlyBudgetPaise", default?.monthlyBudgetPaise ?: 0)) ?: 0
                    )
                )
                val actual = dao.getCategoryByName(categoryName) ?: dao.getCategoryByName("Other")!!
                if (oldId > 0) idMap[oldId] = if (insertedId > 0) insertedId else actual.id
            }

            var expenses = 0
            for (i in 0 until expenseArray.length()) {
                val e = expenseArray.getJSONObject(i)
                val amountPaise = validPositiveAmount(e.optLong("amountPaise")) ?: continue
                val type = enumValueOr(e.optString("type"), TransactionType.EXPENSE)
                val merchant = cleanImportedText(e.optString("merchant", "Imported Expense"), 52).ifBlank { "Imported Expense" }
                val paymentMode = enumValueOr(e.optString("paymentMode"), PaymentMode.UNKNOWN)
                val dateTime = validDateTime(e.optLong("dateTime", DateUtils.now())) ?: continue
                val note = cleanImportedText(e.optString("note", "Restored from backup"), 240)
                val categoryId = idMap[e.optLong("categoryId")] ?: ensureCategory("Other").id
                val restoredHash = Security.sha256("restore|$amountPaise|$type|$merchant|$categoryId|$paymentMode|$dateTime|$note")
                if (dao.countBySmsHash(restoredHash) == 0) {
                    val id = dao.insertExpense(
                        ExpenseEntity(
                            amountPaise = amountPaise,
                            type = type,
                            merchant = merchant,
                            categoryId = categoryId,
                            paymentMode = paymentMode,
                            accountHint = cleanImportedText(e.optString("accountHint", ""), 24),
                            dateTime = dateTime,
                            source = TransactionSource.IMPORT,
                            smsHash = restoredHash,
                            confidence = e.optInt("confidence", 100).coerceIn(0, 100),
                            note = note,
                            isReviewed = e.optBoolean("isReviewed", true)
                        )
                    )
                    if (id > 0) expenses++
                }
            }

            var rules = 0
            for (i in 0 until ruleArray.length()) {
                val r = ruleArray.getJSONObject(i)
                val matchText = cleanImportedText(r.optString("matchText"), 80).lowercase()
                if (matchText.length < 2) continue
                val categoryId = idMap[r.optLong("categoryId")] ?: ensureCategory("Other").id
                val id = dao.upsertRule(
                    RuleEntity(
                        matchText = matchText,
                        senderFilter = cleanImportedText(r.optString("senderFilter", ""), 40),
                        categoryId = categoryId,
                        merchantName = cleanImportedText(r.optString("merchantName", ""), 52),
                        enabled = r.optBoolean("enabled", true)
                    )
                )
                if (id >= 0) rules++
            }

            var recurring = 0
            for (i in 0 until recArray.length()) {
                val r = recArray.getJSONObject(i)
                val amountPaise = validPositiveAmount(r.optLong("amountPaise")) ?: continue
                val categoryId = idMap[r.optLong("categoryId")] ?: ensureCategory("Other").id
                val nextDueDate = validDateTime(r.optLong("nextDueDate", DateUtils.now())) ?: DateUtils.now()
                val id = dao.upsertRecurringExpense(
                    RecurringExpenseEntity(
                        amountPaise = amountPaise,
                        merchant = cleanImportedText(r.optString("merchant", "Recurring Expense"), 52).ifBlank { "Recurring Expense" },
                        categoryId = categoryId,
                        paymentMode = enumValueOr(r.optString("paymentMode"), PaymentMode.UNKNOWN),
                        intervalType = enumValueOr(r.optString("intervalType"), RecurringInterval.MONTHLY),
                        nextDueDate = nextDueDate,
                        note = cleanImportedText(r.optString("note", ""), 240),
                        enabled = r.optBoolean("enabled", true)
                    )
                )
                if (id >= 0) recurring++
            }
            detectSubscriptions(force = true)
            ImportBackupReport(categories = categories.length(), expenses = expenses, rules = rules, recurring = recurring)
        }
    }

    private fun validateBackupEnvelope(obj: JSONObject) {
        val appName = obj.optString("app", "Telesis")
        if (appName.isNotBlank() && appName !in setOf("Telesis", "SpendVault")) {
            throw IllegalArgumentException("Selected file is not a Telesis backup")
        }
    }

    private fun validateBackupSize(categories: JSONArray, expenses: JSONArray, rules: JSONArray, recurring: JSONArray) {
        require(categories.length() <= 1_000) { "Backup has too many categories" }
        require(expenses.length() <= 100_000) { "Backup has too many expenses" }
        require(rules.length() <= 2_000) { "Backup has too many rules" }
        require(recurring.length() <= 2_000) { "Backup has too many recurring items" }
    }

    private fun validPositiveAmount(value: Long): Long? = value.takeIf { it in 1..99_999_999_999L }
    private fun validNonNegativeAmount(value: Long): Long? = value.takeIf { it in 0..99_999_999_999L }

    private fun validDateTime(value: Long): Long? {
        val earliest = Instant.parse("2000-01-01T00:00:00Z").toEpochMilli()
        val latest = DateUtils.now() + ChronoUnit.DAYS.duration.toMillis() * 730
        return value.takeIf { it in earliest..latest }
    }

    private fun cleanImportedText(value: String, max: Int): String = value
        .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(max)

    private fun ParsedSms.Transaction.isSmsLedgerType(): Boolean = type == TransactionType.EXPENSE || type == TransactionType.INCOME

    private inline fun <reified T : Enum<T>> enumValueOr(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}

data class ImportReport(
    val imported: Int,
    val review: Int,
    val ignored: Int,
    val duplicate: Int,
    val failed: Int
) {
    val totalProcessed: Int get() = imported + review + ignored + duplicate + failed
}

enum class ImportOutcome { Imported, Review, Ignored, Duplicate, Failed }

data class ExportSnapshot(
    val exportedAt: Long,
    val categories: List<CategoryEntity>,
    val expenses: List<ExpenseEntity>,
    val rules: List<RuleEntity>,
    val budgets: List<BudgetEntity>,
    val recurring: List<RecurringExpenseEntity>,
    val subscriptions: List<SubscriptionEntity>
)

data class ImportBackupReport(
    val categories: Int,
    val expenses: Int,
    val rules: Int,
    val recurring: Int
)
