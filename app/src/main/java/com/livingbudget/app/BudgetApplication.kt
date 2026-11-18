package com.livingbudget.app

import android.app.Application
import androidx.room.Room
import com.livingbudget.app.data.*
import com.livingbudget.app.reminders.ReminderScheduler

class BudgetApplication: Application() {
    val database by lazy { Room.databaseBuilder(this,BudgetDatabase::class.java,"ledger.db").build() }
    val preferences by lazy { PreferenceRepository(this) }
    val copy by lazy { CopyRepository(this) }
    val backup by lazy { BackupRepository(database) }
    val updates by lazy { UpdateRepository(this) }
    override fun onCreate() { super.onCreate();ReminderScheduler.schedule(this) }
}
