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
}
