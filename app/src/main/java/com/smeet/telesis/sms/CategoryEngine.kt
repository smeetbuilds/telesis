package com.smeet.telesis.sms

import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.TransactionType

object CategoryEngine {
    val defaultCategories = listOf(
        DefaultCategory("Food", "🍽", "gold", 600000),
        DefaultCategory("Groceries", "🛒", "emerald", 500000),
        DefaultCategory("Shopping", "🛍", "rose", 400000),
        DefaultCategory("Travel", "🚕", "blue", 300000),
        DefaultCategory("Fuel", "⛽", "orange", 300000),
        DefaultCategory("Bills", "💡", "violet", 450000),
        DefaultCategory("Subscriptions", "🔁", "cyan", 150000),
        DefaultCategory("Healthcare", "🩺", "red", 250000),
        DefaultCategory("Education", "📚", "indigo", 250000),
        DefaultCategory("Entertainment", "🎬", "pink", 250000),
        DefaultCategory("Transfers", "↔", "slate", 0),
        DefaultCategory("Cash Withdrawal", "🏧", "amber", 0),
        DefaultCategory("Income", "↗", "green", 0),
        DefaultCategory("Other", "•", "neutral", 0)
    )

    fun detect(merchant: String, body: String, mode: PaymentMode, type: TransactionType): String {
        val text = (merchant + " " + body).lowercase()
        return when {
            type == TransactionType.INCOME -> "Income"
            type == TransactionType.TRANSFER -> "Transfers"
            mode == PaymentMode.ATM -> "Cash Withdrawal"
            containsAny(text, "zomato", "swiggy", "restaurant", "cafe", "pizza", "burger", "kfc", "mcdonald", "dominos", "food") -> "Food"
            containsAny(text, "blinkit", "zepto", "bigbasket", "dmart", "grocery", "mart", "super market", "supermarket", "reliance fresh") -> "Groceries"
            containsAny(text, "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "shopping") -> "Shopping"
            containsAny(text, "uber", "ola", "rapido", "irctc", "makemytrip", "redbus", "metro", "flight", "train", "bus") -> "Travel"
            containsAny(text, "fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "shell") -> "Fuel"
            containsAny(text, "jio", "airtel", "vodafone", "vi ", "electricity", "torrent power", "gas bill", "broadband", "wifi", "recharge", "billpay") -> "Bills"
            containsAny(text, "netflix", "spotify", "prime", "hotstar", "youtube premium", "subscription", "apple.com/bill") -> "Subscriptions"
            containsAny(text, "hospital", "clinic", "pharmacy", "chemist", "apollo", "medplus", "doctor", "lab") -> "Healthcare"
            containsAny(text, "course", "udemy", "coursera", "college", "school", "exam", "tuition") -> "Education"
            containsAny(text, "bookmyshow", "pvr", "inox", "game", "steam", "playstation") -> "Entertainment"
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
