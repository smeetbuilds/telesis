package com.smeet.telesis.sms

import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {
    @Test
    fun parsesDebitExpenseAmountNearDebitKeywordInsteadOfBalance() {
        val parsed = SmsParser.parse(
            sender = "HDFCBK",
            body = "Rs.450.00 debited from A/c XX1234 at ZOMATO via UPI. Avl Bal INR 10123.50",
            smsDate = 1_700_000_000_000L
        ) as ParsedSms.Transaction

        assertEquals(45_000L, parsed.amountPaise)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(PaymentMode.UPI, parsed.paymentMode)
        assertEquals("Zomato", parsed.merchant)
        assertEquals("Food", parsed.suggestedCategory)
    }

    @Test
    fun ignoresOtpMessages() {
        val parsed = SmsParser.parse("BANK", "Your OTP is 123456 for login", 1L)
        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresFailedDebitMessages() {
        val parsed = SmsParser.parse(
            sender = "HDFCBK",
            body = "Your UPI transaction of Rs. 750.00 failed and was not processed. Please try again later.",
            smsDate = 1L
        )
        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun parsesCreditAsIncome() {
        val parsed = SmsParser.parse("ICICIB", "INR 2500 credited to your account XX1111 via NEFT", 1L) as ParsedSms.Transaction
        assertEquals(250_000L, parsed.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("Income", parsed.suggestedCategory)
    }

    @Test
    fun parsesSalaryCreditAsSalaryIncome() {
        val parsed = SmsParser.parse("BANK", "Salary INR 50000 credited to your account XX1111", 1L) as ParsedSms.Transaction
        assertEquals(5_000_000L, parsed.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("Salary", parsed.suggestedCategory)
    }

    @Test
    fun ignoresGenericTransactionWithoutClearDirection() {
        val parsed = SmsParser.parse(
            sender = "BANK",
            body = "Transaction of INR 999.00 on account XX1111 was processed successfully. Ref 12345.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresCreditCardPaymentReceived() {
        val parsed = SmsParser.parse(
            sender = "HDFCBK",
            body = "Payment received for your HDFC Bank Credit Card ending 1234 for Rs. 12,345.67. Thank you for your payment.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresCreditCardCreditedPayment() {
        val parsed = SmsParser.parse(
            sender = "ICICIB",
            body = "INR 8750.00 credited to your credit card account XX9001 towards card payment. Available limit updated.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresCreditCardDueReminder() {
        val parsed = SmsParser.parse(
            sender = "SBICRD",
            body = "Your SBI Credit Card statement is generated. Total amount due Rs. 24500.00, minimum due Rs. 1500.00, due on 20-Jul.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresMobileRechargePayment() {
        val parsed = SmsParser.parse(
            sender = "JIOINF",
            body = "Your mobile recharge of Rs.299 is successful. Plan validity is 28 days.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresUtilityBillPayment() {
        val parsed = SmsParser.parse(
            sender = "TXNPWR",
            body = "Electricity bill payment of Rs.1250.00 paid successfully via UPI. BBPS reference 12345.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun ignoresOwnAccountTransfer() {
        val parsed = SmsParser.parse(
            sender = "BANK",
            body = "Rs. 10000 transferred to your own account XX2222 from account XX1111.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }
}
