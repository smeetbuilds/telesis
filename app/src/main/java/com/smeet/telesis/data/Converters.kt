package com.smeet.telesis.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter fun toPaymentMode(value: String): PaymentMode = PaymentMode.valueOf(value)
    @TypeConverter fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)
    @TypeConverter fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter fun toSmsParsedStatus(value: String): SmsParsedStatus = SmsParsedStatus.valueOf(value)
    @TypeConverter fun fromSmsParsedStatus(value: SmsParsedStatus): String = value.name

    @TypeConverter fun toRecurringInterval(value: String): RecurringInterval = RecurringInterval.valueOf(value)
    @TypeConverter fun fromRecurringInterval(value: RecurringInterval): String = value.name
}
