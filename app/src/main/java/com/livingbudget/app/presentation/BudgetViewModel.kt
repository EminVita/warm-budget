package com.livingbudget.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.livingbudget.app.*
import com.livingbudget.app.data.*
import com.livingbudget.app.reminders.ReminderScheduler
import com.livingbudget.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.*
import java.util.UUID

class BudgetViewModel(application: Application): AndroidViewModel(application) {
    private val app=application as BudgetApplication
    private val dao=app.database.ledger()
    private val local=MutableStateFlow(UiState())
    private val effectChannel=Channel<Effect>(Channel.BUFFERED)
    val effects=effectChannel.receiveAsFlow()
    private var pendingBackup: Backup?=null
    private val zone=ZoneId.of("Asia/Shanghai")
    val state=combine(local,dao.observeExpenses(),dao.observeBudgets(),app.preferences.flow,app.copy.flow) { ui,rows,budgets,prefs,copy ->
        val all=rows.map { it.domain() }
        ui.copy(copy=copy,expenses=all,preferences=prefs,
            summary=Ledger.summary(all,ui.month,ui.today),budget=Ledger.budget(budgets.map { it.domain() },ui.month),
            visible=Ledger.active(all,ui.today),
            history=Ledger.history(all,ui.today).filter { (ui.historyType==null || it.type==ui.historyType) &&
                (it.title.contains(ui.query,true) || it.note.contains(ui.query,true)) },
            selected=ui.selected?.let { selected -> all.find { it.id==selected.id } })
    }.stateIn(viewModelScope,SharingStarted.Eagerly,UiState())
    init { viewModelScope.launch {
        while(isActive) {
            refresh()
            val now=ZonedDateTime.now(zone)
            delay(java.time.Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(zone)).toMillis().coerceAtLeast(1))
        }
    } }
    private fun refresh() { local.update { it.copy(today=LocalDate.now(zone)) } }
    private fun mutate(block: (UiState)->UiState) { local.update(block) }
    private fun task(block: suspend ()->Unit) {
        if(local.value.busy)return
        mutate { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try { block() } catch (_: Exception) { mutate { it.copy(error="operation_failed") } }
            finally { mutate { it.copy(busy=false) } }
        }
    }
    private fun message(key: String) { mutate { it.copy(message=key) } }
    fun dispatch(action: Action) {
        when(action) {
            is Action.Navigate -> mutate { it.copy(page=action.page,panel=Panel.NONE,error=null) }
            is Action.Month -> mutate { it.copy(month=it.month.plusMonths(action.delta)) }
            Action.CurrentMonth -> mutate { it.copy(month=YearMonth.from(it.today)) }
            is Action.Filter -> mutate { it.copy(upcoming=false) } // Legacy event; future list no longer shown.
            is Action.Add -> newExpense(action.type)
            is Action.Select -> mutate { it.copy(selected=state.value.expenses.find { e->e.id==action.id },panel=Panel.DETAIL,error=null) }
            Action.Close -> { pendingBackup=null;mutate { it.copy(panel=Panel.NONE,error=null) } }
            Action.Edit -> state.value.selected?.let { e -> mutate { it.copy(draft=e.draft(),panel=Panel.EXPENSE,error=null) } }
            Action.Renew -> state.value.selected?.let { e ->
                val start=maxOf(e.endExclusive,state.value.today)
                mutate { it.copy(draft=e.draft().copy(id=null,renewedFromId=e.id,start=start.toString(),end=start.plusMonths(1).minusDays(1).toString(),
                    paymentDate=start.toString(),paid=start<=it.today,duration=DurationPreset.MONTH,renewalDate=""),panel=Panel.EXPENSE,error=null) }
            }
            Action.HandleReminder -> state.value.selected?.let { e -> task { dao.handle(e.id);ReminderScheduler.cancelExpense(app,e.id);message("reminder_handled") } }
            is Action.ChangeExpense -> changeDraft(action.field,action.value)
            is Action.Duration -> mutate { it.copy(draft=recalculate(it.draft.copy(duration=action.value))) }
            is Action.Paid -> mutate { it.copy(draft=it.draft.copy(paid=action.value)) }
            is Action.ExpenseReminder -> mutate { it.copy(draft=it.draft.copy(reminders=action.value)) }
            is Action.Purpose -> mutate { it.copy(draft=it.draft.copy(purpose=action.value)) }
            Action.SaveExpense -> saveExpense()
            Action.OpenBudget -> mutate { it.copy(panel=Panel.BUDGET,budgetInput=state.value.budget?.let(::decimal)?:"",error=null) }
            is Action.BudgetInput -> mutate { it.copy(budgetInput=action.value.take(20)) }
            is Action.BudgetRecurring -> mutate { it.copy(budgetRecurring=action.value) }
            Action.SaveBudget -> {
                val amount=Ledger.amount(local.value.budgetInput)
                if(amount==null)mutate { it.copy(error="invalid_amount") }
                else task { val s=local.value;app.database.withTransaction { dao.saveBudget(BudgetRow(s.month.toString(),amount,true));dao.saveBudget(BudgetRow(s.month.toString(),amount,false)) };mutate { it.copy(panel=Panel.NONE) } }
            }
            Action.OpenHistory -> mutate { it.copy(panel=Panel.HISTORY,query="",historyType=null) }
            is Action.Search -> mutate { it.copy(query=action.value) }
            is Action.HistoryType -> mutate { it.copy(historyType=action.type) }
            is Action.Reminders -> viewModelScope.launch {
                if(action.enabled && !state.value.notificationGranted) effectChannel.send(Effect.RequestNotifications)
                else { app.preferences.setReminders(action.enabled);ReminderScheduler.runNow(app) }
            }
            is Action.NotificationPermission -> {
                mutate { it.copy(notificationGranted=action.granted) }
                if(action.granted) viewModelScope.launch { app.preferences.setReminders(true);ReminderScheduler.runNow(app) }
                else message("permission_needed")
            }
            Action.OpenTime -> mutate { it.copy(panel=Panel.REMINDER_TIME,timeInput="%02d:%02d".format(state.value.preferences.hour,state.value.preferences.minute),error=null) }
            is Action.TimeInput -> mutate { it.copy(timeInput=action.value.take(5)) }
            Action.SaveTime -> {
                val time=runCatching { LocalTime.parse(local.value.timeInput) }.getOrNull()
                if(time==null)mutate { it.copy(error="invalid_time") }
                else task { app.preferences.setTime(time.hour,time.minute);mutate { it.copy(panel=Panel.NONE) };ReminderScheduler.runNow(app) }
            }
            Action.OpenTheme -> mutate { it.copy(panel=Panel.THEME) }
            is Action.Theme -> task { app.preferences.setTheme(action.value);mutate { it.copy(panel=Panel.NONE) } }
            is Action.File -> viewModelScope.launch { effectChannel.send(Effect.ChooseFile(action.action)) }
            Action.Restore -> pendingBackup?.let { backup -> task {
                app.backup.restore(backup);pendingBackup=null;ReminderScheduler.cancelAll(app);ReminderScheduler.runNow(app)
                mutate { it.copy(panel=Panel.NONE) };message("restored")
            } }
            Action.ResetCopy -> task { withContext(Dispatchers.IO) { app.copy.reset() };message("copy_reset") }
            Action.CheckUpdate -> if(!app.updates.configured)message("update_unconfigured") else task {
                val info=withContext(Dispatchers.IO) { app.updates.check() }
                if(info.versionCode>BuildConfig.VERSION_CODE)mutate { it.copy(update=info) } else message("latest_version")
            }
            Action.DownloadUpdate -> state.value.update?.let { update -> viewModelScope.launch { effectChannel.send(Effect.OpenUrl(update.downloadUrl)) } }
            Action.DismissMessage -> mutate { it.copy(message=null,update=null,error=null) }
            Action.Refresh -> { refresh();ReminderScheduler.runNow(app) }
        }
    }
    fun setNotificationGranted(granted: Boolean) { mutate { it.copy(notificationGranted=granted) } }
    private fun newExpense(type: ExpenseType) {
        val today=state.value.today
        mutate { it.copy(panel=Panel.EXPENSE,error=null,draft=ExpenseDraft(type=type,start=today.toString(),paymentDate=today.toString(),
            end=if(type==ExpenseType.SUBSCRIPTION)today.plusMonths(1).minusDays(1).toString() else today.toString(),
            duration=if(type==ExpenseType.SUBSCRIPTION)DurationPreset.MONTH else DurationPreset.DAY)) }
    }
    private fun changeDraft(field: ExpenseField,value: String) {
        mutate { ui -> val d=ui.draft;val v=value.take(if(field==ExpenseField.NOTE)2000 else 100)
            val next=when(field) {
                ExpenseField.TITLE->d.copy(title=v);ExpenseField.AMOUNT->d.copy(amount=v)
                ExpenseField.START->recalculate(d.copy(start=v,paymentDate=if(d.paymentDate==d.start)v else d.paymentDate))
                ExpenseField.END->d.copy(end=v,duration=DurationPreset.CUSTOM)
                ExpenseField.PAYMENT_DATE->d.copy(paymentDate=v);ExpenseField.NOTE->d.copy(note=v)
                ExpenseField.CATEGORY->d.copy(category=v);ExpenseField.RENEWAL_DATE->d.copy(renewalDate=v)
            };ui.copy(draft=next,error=null)
        }
    }
    private fun recalculate(d: ExpenseDraft): ExpenseDraft {
        val start=runCatching { LocalDate.parse(d.start) }.getOrNull()?:return d
        val end=when(d.duration) { DurationPreset.CUSTOM->return d;DurationPreset.DAY->start;else->start.plusMonths(d.duration.months!!).minusDays(1) }
        return d.copy(end=end.toString())
    }
    private fun saveExpense() {
        if(local.value.busy)return
        val d=local.value.draft;val amount=Ledger.amount(d.amount)
        if(amount==null) { mutate { it.copy(error="invalid_amount") };return }
        if(d.title.isBlank()) { mutate { it.copy(error="invalid_title") };return }
        val old=d.id?.let { id->state.value.expenses.find { it.id==id } }
        val expense=runCatching {
            val payment=LocalDate.parse(d.start)
            Expense(d.id?:UUID.randomUUID().toString(),d.type,d.title.trim(),amount,LocalDate.parse(d.start),LocalDate.parse(d.end).plusDays(1),
                payment,PaymentStatus.PAID,old?.createdAt?:System.currentTimeMillis(),d.note,"",d.renewedFromId,
                d.reminders,d.purpose,d.renewalDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                old?.let { it.reminderHandled && it.reminderBase==(d.renewalDate.takeIf(String::isNotBlank)?.let(LocalDate::parse)?:LocalDate.parse(d.end)) }?:false)
        }.getOrNull()
        if(expense==null) { mutate { it.copy(error="invalid_dates") };return }
        task {
            app.database.withTransaction {
                if(old!=null)dao.saveRevision(RevisionRow(UUID.randomUUID().toString(),old.id,System.currentTimeMillis(),BackupRepository.encode(old).toString()))
                dao.saveExpense(ExpenseRow.from(expense))
                d.renewedFromId?.let { dao.handle(it) }
            }
            ReminderScheduler.cancelExpense(app,expense.id)
            d.renewedFromId?.let { ReminderScheduler.cancelExpense(app,it) }
            ReminderScheduler.runNow(app);mutate { it.copy(panel=Panel.NONE) }
        }
    }
    fun importFile(action: FileAction,raw: String) = task {
        withContext(Dispatchers.IO) {
            when(action) {
                FileAction.IMPORT_COPY -> { app.copy.import(raw);message("copy_imported") }
                FileAction.IMPORT_BACKUP -> { val backup=app.backup.parse(raw);pendingBackup=backup;mutate { it.copy(panel=Panel.RESTORE,restoreCount=backup.expenses.size) } }
                else -> Unit
            }
        }
    }
    suspend fun exportFile(action: FileAction): String = withContext(Dispatchers.IO) {
        if(action==FileAction.EXPORT_COPY)app.copy.exportDefaults() else app.backup.export()
    }
    fun fileError() { mutate { it.copy(error="operation_failed") } }
    private fun Expense.draft()=ExpenseDraft(id,type,title,decimal(amountMinor),start.toString(),endInclusive.toString(),paymentDate.toString(),note,category,
        paymentStatus==PaymentStatus.PAID,DurationPreset.CUSTOM,reminderEnabled,reminderPurpose,renewalDate?.toString()?:"",renewedFromId)
}
fun decimal(minor: Long): String = java.math.BigDecimal.valueOf(minor,2).toPlainString()
fun money(minor: Long): String = "¥ "+String.format(java.util.Locale.CHINA,"%,.2f",java.math.BigDecimal.valueOf(minor,2))
