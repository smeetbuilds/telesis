package com.smeet.telesis.sms

import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.TransactionType

object CategoryEngine {
    val defaultCategories = listOf(
        DefaultCategory("Food", "F", "gold", 600000),
        DefaultCategory("Groceries", "G", "emerald", 500000),
        DefaultCategory("Shopping", "S", "rose", 400000),
        DefaultCategory("Travel", "T", "blue", 300000),
        DefaultCategory("Fuel", "P", "orange", 300000),
        DefaultCategory("Rent", "H", "violet", 0),
        DefaultCategory("Healthcare", "M", "red", 250000),
        DefaultCategory("Education", "E", "indigo", 250000),
        DefaultCategory("Entertainment", "N", "pink", 250000),
        DefaultCategory("Personal Care", "C", "cyan", 200000),
        DefaultCategory("Subscriptions", "R", "cyan", 150000),
        DefaultCategory("Cash Withdrawal", "A", "amber", 0),
        DefaultCategory("Salary", "W", "green", 0),
        DefaultCategory("Refunds", "B", "blue", 0),
        DefaultCategory("Income", "I", "green", 0),
        DefaultCategory("Other", "O", "neutral", 0)
    )

    fun detect(merchant: String, body: String, mode: PaymentMode, type: TransactionType): String {
        val text = (merchant + " " + body).lowercase()
        return when {
            type == TransactionType.INCOME && containsAny(text, "salary", "payroll", "stipend", "wages") -> "Salary"
            type == TransactionType.INCOME && containsAny(text, "refund", "cashback", "reversal", "reversed") -> "Refunds"
            type == TransactionType.INCOME -> "Income"
            mode == PaymentMode.ATM -> "Cash Withdrawal"
            containsAny(text, "zomato", "swiggy", "restaurant", "cafe", "pizza", "burger", "kfc", "mcdonald", "dominos", "food") -> "Food"
            containsAny(text, "blinkit", "zepto", "bigbasket", "dmart", "grocery", "mart", "super market", "supermarket", "reliance fresh") -> "Groceries"
            containsAny(text, "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "shopping") -> "Shopping"
            containsAny(text, "uber", "ola", "rapido", "irctc", "makemytrip", "redbus", "metro", "flight", "train", "bus") -> "Travel"
            containsAny(text, "fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "shell") -> "Fuel"
            containsAny(text, "rent", "landlord", "housing", "society maintenance") -> "Rent"
            containsAny(text, "netflix", "spotify", "prime", "hotstar", "youtube premium", "subscription") -> "Subscriptions"
            containsAny(text, "hospital", "clinic", "pharmacy", "chemist", "apollo", "medplus", "doctor", "lab") -> "Healthcare"
            containsAny(text, "course", "udemy", "coursera", "college", "school", "exam", "tuition") -> "Education"
            containsAny(text, "bookmyshow", "pvr", "inox", "game", "steam", "playstation") -> "Entertainment"
            containsAny(text, "salon", "spa", "beauty", "grooming") -> "Personal Care"
            else -> "Other"
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean = keywords.any { text.contains(it) }
}

data class DefaultCategory(
    val name: String,
    val icon: String,
    val colorKey: String,
    val monthlyBudgetPaise: Long
)
