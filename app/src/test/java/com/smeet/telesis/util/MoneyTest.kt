package com.smeet.telesis.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesRupeeValuesToPaise() {
        assertEquals(123_456L, Money.parseToPaise("1,234.56"))
        assertEquals(99_00L, Money.parseToPaise("99"))
    }

    @Test
    fun rejectsInvalidMoney() {
        assertNull(Money.parseToPaise("not-money"))
    }
}
