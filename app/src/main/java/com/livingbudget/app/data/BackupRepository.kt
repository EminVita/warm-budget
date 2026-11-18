package com.livingbudget.app.data

import androidx.room.withTransaction
import com.livingbudget.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class Backup(val expenses: List<Expense>, val budgets: List<Budget>, val revisions: List<RevisionRow>)
class BackupRepository(private val db: BudgetDatabase) {
    suspend fun export(): String = db.withTransaction {
        JSONObject().put("schemaVersion",1)
            .put("expenses",JSONArray(db.ledger().expenses().map { encode(it.domain()) }))
            .put("budgets",JSONArray(db.ledger().budgets().map {
                JSONObject().put("month",it.month).put("amountMinor",it.amountMinor).put("recurring",it.recurring)
            })).put("revisions",JSONArray(db.ledger().revisions().map {
                JSONObject().put("id",it.id).put("expenseId",it.expenseId).put("changedAt",it.changedAt).put("snapshot",it.snapshot)
            })).toString(2)
    }
    fun parse(raw: String): Backup {
        require(raw.toByteArray().size<=10_000_000)
        val json=JSONObject(raw);require(json.getInt("schemaVersion")==1)
        val es=json.getJSONArray("expenses");val bs=json.getJSONArray("budgets")
        require(es.length()<=20000 && bs.length()<=12000)
        val expenses=(0 until es.length()).map { decode(es.getJSONObject(it)) }
        val budgets=(0 until bs.length()).map { val b=bs.getJSONObject(it)
            Budget(java.time.YearMonth.parse(b.getString("month")),b.getLong("amountMinor"),b.getBoolean("recurring")) }
        require(expenses.map { it.id }.distinct().size==expenses.size)
        require(budgets.map { it.month to it.recurring }.distinct().size==budgets.size)
        val ids=expenses.map { it.id }.toSet()
        val rs=json.optJSONArray("revisions")?:JSONArray()
        require(rs.length()<=50000)
        val revisions=(0 until rs.length()).map { val r=rs.getJSONObject(it)
            RevisionRow(r.getString("id"),r.getString("expenseId"),r.getLong("changedAt"),r.getString("snapshot")) }
        require(revisions.map { it.id }.distinct().size==revisions.size)
        require(revisions.all { it.expenseId in ids && it.changedAt>=0 && decode(JSONObject(it.snapshot)).id==it.expenseId })
        return Backup(expenses,budgets,revisions)
    }
    suspend fun restore(backup: Backup) = db.withTransaction {
        val dao=db.ledger();dao.clearRevisions();dao.clearExpenses();dao.clearBudgets();dao.clearDeliveries()
        backup.expenses.forEach { dao.saveExpense(ExpenseRow.from(it)) }
        backup.budgets.forEach { dao.saveBudget(BudgetRow(it.month.toString(),it.amountMinor,it.recurring)) }
        backup.revisions.forEach { dao.saveRevision(it) }
    }
    companion object {
        fun encode(e: Expense): JSONObject = JSONObject().put("id",e.id).put("type",e.type.name).put("title",e.title)
            .put("amountMinor",e.amountMinor).put("start",e.start).put("endExclusive",e.endExclusive)
            .put("paymentDate",e.paymentDate).put("paymentStatus",e.paymentStatus.name).put("createdAt",e.createdAt)
            .put("note",e.note).put("category",e.category).put("renewedFromId",e.renewedFromId ?: JSONObject.NULL)
            .put("reminderEnabled",e.reminderEnabled).put("reminderPurpose",e.reminderPurpose.name)
            .put("renewalDate",e.renewalDate?.toString() ?: JSONObject.NULL).put("reminderHandled",e.reminderHandled)
        fun decode(j: JSONObject): Expense {
            val id=j.getString("id");UUID.fromString(id)
            val created=j.getLong("createdAt");require(created>=0)
            return Expense(id,ExpenseType.valueOf(j.getString("type")),j.getString("title"),j.getLong("amountMinor"),
                LocalDate.parse(j.getString("start")),LocalDate.parse(j.getString("endExclusive")),LocalDate.parse(j.getString("paymentDate")),
                PaymentStatus.valueOf(j.getString("paymentStatus")),created,j.optString("note"),j.optString("category"),
                if(j.isNull("renewedFromId")) null else j.getString("renewedFromId"),j.optBoolean("reminderEnabled",true),
                ReminderPurpose.valueOf(j.optString("reminderPurpose","CANCEL")),
                if(j.isNull("renewalDate")) null else LocalDate.parse(j.getString("renewalDate")),j.optBoolean("reminderHandled",false))
        }
    }
}
