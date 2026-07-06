package com.smeet.telesis.sms

import com.smeet.telesis.data.PaymentMode
import com.smeet.telesis.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryEngineTest {
    @Test
    fun mapsIncomeToIncomeSpecificCategories() {
        assertEquals("Salary", CategoryEngine.detect("Employer", "Salary INR 50000 credited", PaymentMode.BANK, TransactionType.INCOME))
        assertEquals("Refunds", CategoryEngine.detect("Amazon", "Refund credited for order", PaymentMode.BANK, TransactionType.INCOME))
        assertEquals("Income", CategoryEngine.detect("Bank", "INR 100 credited", PaymentMode.BANK, TransactionType.INCOME))
    }

    @Test
    fun mapsCommonExpensesToExpectedCategories() {
        assertEquals("Food", CategoryEngine.detect("Zomato", "Rs 450 debited at ZOMATO", PaymentMode.UPI, TransactionType.EXPENSE))
        assertEquals("Groceries", CategoryEngine.detect("DMart", "Rs 1200 debited at DMART", PaymentMode.CARD, TransactionType.EXPENSE))
        assertEquals("Fuel", CategoryEngine.detect("HPCL", "Rs 2000 debited at HPCL fuel station", PaymentMode.CARD, TransactionType.EXPENSE))
    }

    @Test
    fun mapsAtmExpenseToCashWithdrawal() {
        assertEquals("Cash Withdrawal", CategoryEngine.detect("ATM Withdrawal", "Rs 5000 withdrawn from ATM", PaymentMode.ATM, TransactionType.EXPENSE))
    }
}
