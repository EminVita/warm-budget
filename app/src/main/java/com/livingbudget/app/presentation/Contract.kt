package com.livingbudget.app.presentation

import com.livingbudget.app.data.*
import com.livingbudget.core.*
import java.time.LocalDate
import java.time.YearMonth

enum class Page { HOME, SETTINGS }
enum class Panel { NONE, EXPENSE, DETAIL, BUDGET, HISTORY, REMINDER_TIME, THEME, RESTORE }
enum class ExpenseField { TITLE, AMOUNT, START, END, PAYMENT_DATE, NOTE, CATEGORY, RENEWAL_DATE }
enum class DurationPreset(val months: Long?) { DAY(null), MONTH(1), QUARTER(3), YEAR(12), CUSTOM(null) }
enum class FileAction { EXPORT_BACKUP, IMPORT_BACKUP, EXPORT_COPY, IMPORT_COPY }
data class ExpenseDraft(
    val id: String?=null, val type: ExpenseType=ExpenseType.SUBSCRIPTION,
    val title: String="", val amount: String="", val start: String="", val end: String="",
    val paymentDate: String="", val note: String="", val category: String="",
    val paid: Boolean=true, val duration: DurationPreset=DurationPreset.MONTH,
    val reminders: Boolean=true, val purpose: ReminderPurpose=ReminderPurpose.CANCEL,
    val renewalDate: String="", val renewedFromId: String?=null
)
data class UiState(
    val copy: CopyBook=CopyBook(emptyMap()), val page: Page=Page.HOME, val panel: Panel=Panel.NONE,
    val today: LocalDate=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")),
    val month: YearMonth=YearMonth.from(today), val expenses: List<Expense> = emptyList(),
    val visible: List<Expense> = emptyList(), val history: List<Expense> = emptyList(),
    val summary: Summary=Summary(0,0,0,0), val budget: Long?=null,
    val upcoming: Boolean=false, val query: String="", val historyType: ExpenseType?=null,
    val selected: Expense?=null, val draft: ExpenseDraft=ExpenseDraft(),
    val budgetInput: String="", val budgetRecurring: Boolean=true,
    val timeInput: String="09:00", val preferences: Preferences=Preferences(),
    val notificationGranted: Boolean=false, val busy: Boolean=false, val error: String?=null,
    val message: String?=null, val update: UpdateInfo?=null, val restoreCount: Int=0
)
sealed interface Action {
    data class Navigate(val page: Page): Action
    data class Month(val delta: Long): Action
    data object CurrentMonth: Action
    data class Filter(val upcoming: Boolean): Action
    data class Add(val type: ExpenseType): Action
    data class Select(val id: String): Action
    data object Close: Action
    data object Edit: Action
    data object Renew: Action
    data object HandleReminder: Action
    data class ChangeExpense(val field: ExpenseField,val value: String): Action
    data class Duration(val value: DurationPreset): Action
    data class Paid(val value: Boolean): Action
    data class ExpenseReminder(val value: Boolean): Action
    data class Purpose(val value: ReminderPurpose): Action
    data object SaveExpense: Action
    data object OpenBudget: Action
    data class BudgetInput(val value: String): Action
    data class BudgetRecurring(val value: Boolean): Action
    data object SaveBudget: Action
    data object OpenHistory: Action
    data class Search(val value: String): Action
    data class HistoryType(val type: ExpenseType?): Action
    data class Reminders(val enabled: Boolean): Action
    data object OpenTime: Action
    data class TimeInput(val value: String): Action
    data object SaveTime: Action
    data object OpenTheme: Action
    data class Theme(val value: String): Action
    data class File(val action: FileAction): Action
    data object Restore: Action
    data object ResetCopy: Action
    data object CheckUpdate: Action
    data object DownloadUpdate: Action
    data object DismissMessage: Action
    data class NotificationPermission(val granted: Boolean): Action
    data object Refresh: Action
}
sealed interface Effect {
    data class ChooseFile(val action: FileAction): Effect
    data object RequestNotifications: Effect
    data class OpenUrl(val url: String): Effect
}
data class UpdateInfo(val versionCode: Long,val versionName: String,val downloadUrl: String,val notes: String)
