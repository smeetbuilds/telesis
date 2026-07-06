package com.smeet.telesis.sms

import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.TransactionType
import com.smeet.telesis.util.Money

object SmsParser {
    const val PARSER_VERSION = "sms-parser-v3-expense-income-only"

    private val amountRegexes = listOf(
        Regex("(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE),
        Regex("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:INR|Rs\\.?|₹)", RegexOption.IGNORE_CASE)
    )

    private val debitWords = listOf(
        "debited", "debit", "spent", "purchase", "withdrawn", "deducted", "charged", "used at", "paid to"
    )
    private val creditWords = listOf("credited", "credit", "received", "deposited", "salary", "refund", "cashback", "reversal")
    private val ownTransferWords = listOf(
        "self transfer", "transferred to your", "sent to self", "own account", "between your accounts",
        "payment received for your credit card", "credited to your credit card", "credited in your credit card",
        "credit card payment", "card payment received"
    )
    private val failedTransactionWords = listOf(
        "failed", "declined", "unsuccessful", "not processed", "could not be processed", "reversed due to failure",
        "transaction failed", "payment failed", "txn failed", "request failed"
    )
    private val billRechargeWords = listOf(
        "recharge", "mobile recharge", "prepaid recharge", "postpaid bill", "mobile bill", "dth recharge",
        "electricity bill", "power bill", "gas bill", "water bill", "broadband bill", "wifi bill",
        "landline bill", "utility bill", "bbps", "billpay", "bill payment", "bill paid", "payment of bill",
        "paid bill", "autopay bill", "billdesk", "biller", "operator recharge", "plan validity", "data pack"
    )
    private val creditCardContextWords = listOf(
        "credit card", "cc", "card ending", "card no", "card number", "card account", "card payment",
        "card bill", "bill payment", "outstanding", "statement balance", "total amount due"
    )
    private val creditCardPaymentWords = listOf(
        "payment received", "payment of", "payment towards", "payment for", "paid towards", "paid for",
        "bill paid", "bill payment", "card payment", "credited to your credit card", "credited in your credit card",
        "credited to card", "received towards", "received for your credit card", "thank you for your payment"
    )
    private val ignoreWords = listOf(
        "otp", "one time password", "password", "verification", "login", "offer", "discount",
        "sale", "statement", "bill due", "due on", "minimum due", "available balance", "balance is",
        "pre-approved", "loan", "insurance", "kyc", "mandate request", "reward points", "voucher"
    )

    private val accountRegexes = listOf(
        Regex("(?:a/c|acct|account|card|xx|x{2,}|ending|no\\.)\\s*[-:]?\\s*([xX*]*[0-9]{3,6})", RegexOption.IGNORE_CASE),
        Regex("(?:UPI|VPA)\\s*[-:]?\\s*([a-z0-9._-]+@[a-z0-9.-]+)", RegexOption.IGNORE_CASE)
    )

    fun parse(sender: String, body: String, smsDate: Long): ParsedSms {
        val normalized = body.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        val lower = normalized.lowercase()

        if (failedTransactionWords.any { lower.contains(it) }) {
            return ParsedSms.Ignored("Ignored failed or declined transaction SMS")
        }
        if (isBillOrRechargePayment(lower)) {
            return ParsedSms.Ignored("Ignored bill, recharge, or utility-payment SMS")
        }
        if (isCreditCardDueReminder(lower)) {
            return ParsedSms.Ignored("Ignored credit-card bill due reminder")
        }
        if (isCreditCardPayment(lower) || ownTransferWords.any { lower.contains(it) }) {
            return ParsedSms.Ignored("Ignored transfer, card-settlement, or own-account movement SMS")
        }
        if (ignoreWords.any { lower.contains(it) } && !hasRealDebitOrCreditSignal(lower)) {
            return ParsedSms.Ignored("Ignored OTP, promotional, due reminder, or non-transaction SMS")
        }

        val amount = extractAmount(normalized)
            ?: return ParsedSms.Ignored("No amount found")

        val type = when {
            creditWords.any { lower.contains(it) } && debitWords.none { lower.contains(it) } -> TransactionType.INCOME
            debitWords.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> return ParsedSms.Ignored("Ignored SMS without clear debit or credit direction")
        }

        val paymentMode = detectPaymentMode(lower)
        val merchant = cleanMerchant(extractMerchant(normalized, lower, sender, paymentMode))
        val category = CategoryEngine.detect(merchant, normalized, paymentMode, type)
        val accountHint = accountRegexes.firstNotNullOfOrNull { it.find(normalized)?.groupValues?.getOrNull(1) } ?: ""
        val confidence = score(lower, merchant, amount, paymentMode, type)
        return ParsedSms.Transaction(
            amountPaise = amount,
            type = type,
            merchant = merchant,
            suggestedCategory = category,
            paymentMode = paymentMode,
            accountHint = accountHint.take(24),
            smsDate = smsDate,
            confidence = confidence
        )
    }

    private fun hasRealDebitOrCreditSignal(lower: String): Boolean =
        debitWords.any { lower.contains(it) } || creditWords.any { lower.contains(it) }

    private fun isBillOrRechargePayment(lower: String): Boolean {
        if (!billRechargeWords.any { lower.contains(it) }) return false
        val isSalaryOrRefund = listOf("salary", "refund", "cashback", "reversal").any { lower.contains(it) }
        return !isSalaryOrRefund
    }

    private fun isCreditCardDueReminder(lower: String): Boolean {
        val hasCardContext = creditCardContextWords.any { lower.contains(it) }
        val hasDueLanguage = listOf("due", "minimum amount due", "total amount due", "payment due", "statement generated", "bill generated").any { lower.contains(it) }
        val hasActualPaymentLanguage = creditCardPaymentWords.any { lower.contains(it) } || lower.contains("paid")
        return hasCardContext && hasDueLanguage && !hasActualPaymentLanguage
    }

    private fun isCreditCardPayment(lower: String): Boolean {
        val hasCardContext = creditCardContextWords.any { lower.contains(it) }
        val hasPaymentLanguage = creditCardPaymentWords.any { lower.contains(it) }
        val hasIssuerPaymentPattern = lower.contains("credit card") && (lower.contains("credited") || lower.contains("received") || lower.contains("paid"))
        val isRewardOrReversal = listOf("cashback", "reward", "points", "reversal", "refund", "chargeback").any { lower.contains(it) }
        return hasCardContext && (hasPaymentLanguage || hasIssuerPaymentPattern) && !isRewardOrReversal
    }

    private fun extractAmount(text: String): Long? {
        val lower = text.lowercase()
        val candidates = amountRegexes.flatMap { regex ->
            regex.findAll(text).mapNotNull { match ->
                val raw = match.groupValues.getOrNull(1) ?: return@mapNotNull null
                val paise = Money.parseToPaise(raw) ?: return@mapNotNull null
                val windowStart = (match.range.first - 48).coerceAtLeast(0)
                val windowEnd = (match.range.last + 48).coerceAtMost(text.lastIndex)
                val window = lower.substring(windowStart, windowEnd + 1)
                var score = 0
                if (debitWords.any { window.contains(it) } || creditWords.any { window.contains(it) }) score += 40
                if (creditCardPaymentWords.any { window.contains(it) }) score += 45
                if (window.contains("amt") || window.contains("amount") || window.contains("rs") || window.contains("inr") || window.contains("₹")) score += 20
                if (window.contains("bal") || window.contains("available") || window.contains("limit") || window.contains("outstanding")) score -= 45
                if (window.contains("minimum due") || window.contains("total amount due")) score -= 35
                if (paise in 1..99_999_999L) score += 15
                AmountCandidate(paise, score, match.range.first)
            }.toList()
        }.distinctBy { it.amountPaise to it.position }
        return candidates.maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenByDescending { -it.position })?.amountPaise
    }

    private fun detectPaymentMode(lower: String): PaymentMode = when {
        lower.contains("upi") || lower.contains("vpa") || lower.contains("imps") -> PaymentMode.UPI
        lower.contains("debit card") || lower.contains("credit card") || lower.contains("card") || lower.contains("pos") -> PaymentMode.CARD
        lower.contains("paytm") || lower.contains("phonepe wallet") || lower.contains("wallet") || lower.contains("amazon pay") -> PaymentMode.WALLET
        lower.contains("atm") || lower.contains("cash withdrawal") -> PaymentMode.ATM
        lower.contains("net banking") || lower.contains("netbanking") -> PaymentMode.NETBANKING
        lower.contains("neft") || lower.contains("rtgs") || lower.contains("bank transfer") -> PaymentMode.BANK
        else -> PaymentMode.UNKNOWN
    }

    private fun extractMerchant(text: String, lower: String, sender: String, mode: PaymentMode): String {
        val patterns = listOf(
            Regex("(?:at|to|towards|for|on)\\s+([A-Z0-9][A-Za-z0-9 ._&@/-]{2,48}?)(?:\\s+(?:on|via|using|from|Ref|UPI|Txn|transaction|Info|Avl|available)|[.,;]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:merchant|payee)[:\\s-]+([A-Za-z0-9 ._&@/-]{2,48})(?:[.,;]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:VPA|UPI ID)[:\\s-]+([a-z0-9._-]+@[a-z0-9.-]+)", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val found = regex.find(text)?.groupValues?.getOrNull(1)?.trim()
            if (!found.isNullOrBlank() && found.length >= 3) return found
        }
        val known = KnownMerchants.entries.firstOrNull { lower.contains(it.key) }?.value
        if (known != null) return known
        return when (mode) {
            PaymentMode.ATM -> "ATM Withdrawal"
            PaymentMode.UPI -> "UPI Payment"
            PaymentMode.CARD -> "Card Purchase"
            else -> sender.ifBlank { "Unknown Merchant" }
        }
    }

    private fun cleanMerchant(raw: String): String {
        return raw
            .replace(Regex("\\b(UPI|Txn|transaction|ref|reference|info|avl|available|balance|debited|credited)\\b.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^A-Za-z0-9 ._&@/-]"), "")
            .trim(' ', '-', '.', '/')
            .replace(Regex("\\s+"), " ")
            .ifBlank { "Unknown Merchant" }
            .take(52)
            .split(" ")
            .joinToString(" ") { token ->
                if (token.all { it.isUpperCase() || it.isDigit() } && token.length <= 5) token
                else token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun score(lower: String, merchant: String, amount: Long, paymentMode: PaymentMode, type: TransactionType): Int {
        var score = 40
        if (amount > 0) score += 25
        if (debitWords.any { lower.contains(it) } || creditWords.any { lower.contains(it) }) score += 15
        if (paymentMode != PaymentMode.UNKNOWN) score += 10
        if (merchant != "Unknown Merchant" && merchant.length >= 3) score += 10
        if (type == TransactionType.INCOME) score += 5
        return score.coerceIn(0, 100)
    }

    private data class AmountCandidate(val amountPaise: Long, val score: Int, val position: Int)
}

sealed class ParsedSms {
    data class Transaction(
        val amountPaise: Long,
        val type: TransactionType,
        val merchant: String,
        val suggestedCategory: String,
        val paymentMode: PaymentMode,
        val accountHint: String,
        val smsDate: Long,
        val confidence: Int
    ) : ParsedSms()

    data class Ignored(val reason: String) : ParsedSms()
}

private val KnownMerchants = mapOf(
    "zomato" to "Zomato",
    "swiggy" to "Swiggy",
    "blinkit" to "Blinkit",
    "zepto" to "Zepto",
    "bigbasket" to "BigBasket",
    "dmart" to "DMart",
    "amazon" to "Amazon",
    "flipkart" to "Flipkart",
    "myntra" to "Myntra",
    "uber" to "Uber",
    "ola" to "Ola",
    "rapido" to "Rapido",
    "irctc" to "IRCTC",
    "netflix" to "Netflix",
    "spotify" to "Spotify",
    "hotstar" to "Disney+ Hotstar",
    "prime" to "Amazon Prime"
)
