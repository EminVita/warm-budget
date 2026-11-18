package com.livingbudget.app.data

import androidx.room.*
import com.livingbudget.core.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

@Entity(tableName = "expenses")
data class ExpenseRow(
    @PrimaryKey val id: String, val type: String, val title: String, val amountMinor: Long,
    val start: String, val endExclusive: String, val paymentDate: String, val paymentStatus: String,
    val createdAt: Long, val note: String, val category: String, val renewedFromId: String?,
    val reminderEnabled: Boolean, val reminderPurpose: String, val renewalDate: String?, val reminderHandled: Boolean
) {
    fun domain() = Expense(id,ExpenseType.valueOf(type),title,amountMinor,LocalDate.parse(start),LocalDate.parse(endExclusive),
        LocalDate.parse(paymentDate),PaymentStatus.valueOf(paymentStatus),createdAt,note,category,renewedFromId,
        reminderEnabled,ReminderPurpose.valueOf(reminderPurpose),renewalDate?.let(LocalDate::parse),reminderHandled)
    companion object {
        fun from(e: Expense) = ExpenseRow(e.id,e.type.name,e.title,e.amountMinor,e.start.toString(),e.endExclusive.toString(),
            e.paymentDate.toString(),e.paymentStatus.name,e.createdAt,e.note,e.category,e.renewedFromId,
            e.reminderEnabled,e.reminderPurpose.name,e.renewalDate?.toString(),e.reminderHandled)
    }
}
@Entity(tableName = "budgets", primaryKeys = ["month", "recurring"])
data class BudgetRow(val month: String, val amountMinor: Long, val recurring: Boolean) {
    fun domain() = Budget(YearMonth.parse(month),amountMinor,recurring)
}
@Entity(tableName = "revisions")
data class RevisionRow(@PrimaryKey val id: String, val expenseId: String, val changedAt: Long, val snapshot: String)
@Entity(tableName = "deliveries")
data class DeliveryRow(@PrimaryKey val key: String, val sentAt: Long)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC, id") fun observeExpenses(): Flow<List<ExpenseRow>>
    @Query("SELECT * FROM budgets") fun observeBudgets(): Flow<List<BudgetRow>>
    @Query("SELECT * FROM expenses") suspend fun expenses(): List<ExpenseRow>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<BudgetRow>
    @Query("SELECT * FROM revisions") suspend fun revisions(): List<RevisionRow>
    @Query("SELECT * FROM expenses WHERE id = :id") suspend fun expense(id: String): ExpenseRow?
    @Upsert suspend fun saveExpense(row: ExpenseRow)
    @Upsert suspend fun saveBudget(row: BudgetRow)
    @Insert suspend fun saveRevision(row: RevisionRow)
    @Query("UPDATE expenses SET reminderHandled = 1 WHERE id = :id") suspend fun handle(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claim(row: DeliveryRow): Long
    @Query("DELETE FROM deliveries WHERE `key` = :key") suspend fun release(key: String)
    @Query("DELETE FROM deliveries") suspend fun clearDeliveries()
    @Query("DELETE FROM revisions") suspend fun clearRevisions()
    @Query("DELETE FROM expenses") suspend fun clearExpenses()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
}

@Database(entities=[ExpenseRow::class,BudgetRow::class,RevisionRow::class,DeliveryRow::class],version=1,exportSchema=true)
abstract class BudgetDatabase: RoomDatabase() { abstract fun ledger(): LedgerDao }
