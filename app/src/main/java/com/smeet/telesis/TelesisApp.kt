package com.smeet.telesis

import android.app.Application
import com.smeet.telesis.core.AppLockManager
import com.smeet.telesis.data.ExpenseRepository
import com.smeet.telesis.data.TelesisDatabase

class TelesisApp : Application() {
    val database: TelesisDatabase by lazy { TelesisDatabase.get(this) }
    val repository: ExpenseRepository by lazy { ExpenseRepository(database) }
    val appLockManager: AppLockManager by lazy { AppLockManager(this) }
}
