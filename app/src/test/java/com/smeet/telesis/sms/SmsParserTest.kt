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
    }

    @Test
    fun ignoresOtpMessages() {
        val parsed = SmsParser.parse("BANK", "Your OTP is 123456 for login", 1L)
        assertTrue(parsed is ParsedSms.Ignored)
    }

    @Test
    fun parsesCreditAsIncome() {
        val parsed = SmsParser.parse("ICICIB", "INR 2500 credited to your account XX1111 via NEFT", 1L) as ParsedSms.Transaction
        assertEquals(250_000L, parsed.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
    }

    @Test
    fun creditCardPaymentReceivedIsTransferNotIncome() {
        val parsed = SmsParser.parse(
            sender = "HDFCBK",
            body = "Payment received for your HDFC Bank Credit Card ending 1234 for Rs. 12,345.67. Thank you for your payment.",
            smsDate = 1L
        ) as ParsedSms.Transaction

        assertEquals(1_234_567L, parsed.amountPaise)
        assertEquals(TransactionType.TRANSFER, parsed.type)
        assertEquals("Credit Card Payment", parsed.merchant)
        assertEquals("Transfers", parsed.suggestedCategory)
    }

    @Test
    fun creditCardCreditedPaymentIsTransferNotIncome() {
        val parsed = SmsParser.parse(
            sender = "ICICIB",
            body = "INR 8750.00 credited to your credit card account XX9001 towards card payment. Available limit updated.",
            smsDate = 1L
        ) as ParsedSms.Transaction

        assertEquals(875_000L, parsed.amountPaise)
        assertEquals(TransactionType.TRANSFER, parsed.type)
        assertEquals("Credit Card Payment", parsed.merchant)
    }

    @Test
    fun creditCardDueReminderIsIgnored() {
        val parsed = SmsParser.parse(
            sender = "SBICRD",
            body = "Your SBI Credit Card statement is generated. Total amount due Rs. 24500.00, minimum due Rs. 1500.00, due on 20-Jul.",
            smsDate = 1L
        )

        assertTrue(parsed is ParsedSms.Ignored)
    }
}
