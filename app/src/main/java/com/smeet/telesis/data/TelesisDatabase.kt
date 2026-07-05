package com.smeet.telesis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        SmsLogEntity::class,
        RuleEntity::class,
        BudgetEntity::class,
        RecurringExpenseEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TelesisDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: TelesisDatabase? = null

        fun get(context: Context): TelesisDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                TelesisDatabase::class.java,
                "telesis.db"
            )
                .build()
                .also { INSTANCE = it }
        }
    }
}
