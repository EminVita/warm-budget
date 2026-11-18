package com.livingbudget.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livingbudget.app.presentation.*
import com.livingbudget.core.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable fun PanelHost(s: UiState,onAction: (Action)->Unit) {
    if(s.panel==Panel.NONE)return
    val c=s.copy
    val title=when(s.panel) {
        Panel.EXPENSE->if(s.draft.id!=null)"edit" else if(s.draft.type==ExpenseType.SUBSCRIPTION)"add_subscription" else "add_expense"
        Panel.DETAIL->"expenses";Panel.BUDGET->"budget_settings";Panel.HISTORY->"history"
        Panel.REMINDER_TIME->"reminder_time";Panel.THEME->"appearance";Panel.RESTORE->"restore_title";Panel.NONE->""
    }
    Dialog(onDismissRequest={if(!s.busy)onAction(Action.Close)},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={onAction(Action.Close)},enabled=!s.busy) { LineIcon(Glyph.BACK,c["back"]) }
                    Text(c[title],Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    if(s.busy)CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
                }
                if(s.error!=null)Text(c[s.error],Modifier.padding(20.dp),color=MaterialTheme.colorScheme.error)
                when(s.panel) {
                    Panel.HISTORY->HistoryPanel(s,onAction)
                    else->Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                        when(s.panel) {
                            Panel.EXPENSE->ExpenseForm(s,onAction)
                            Panel.DETAIL->s.selected?.let { e ->
                                ExpenseCard(e,s,onAction,history=true)
                                Text(c["edit_hint"],style=MaterialTheme.typography.bodySmall)
                                Button(onClick={onAction(Action.Edit)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) { Text(c["edit"]) }
                                if(e.type==ExpenseType.SUBSCRIPTION) {
                                    OutlinedButton(onClick={onAction(Action.Renew)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) { Text(c["renew"]) }

                                if(!e.reminderHandled)TextButton(onClick={onAction(Action.HandleReminder)},enabled=!s.busy) { Text(c["handled"]) }
                                }
                            }
                            Panel.BUDGET->{
                                Text(s.month.toString())
                                Input(c["budget_amount"],s.budgetInput,{onAction(Action.BudgetInput(it))},numeric=true)
                                Save(s) { onAction(Action.SaveBudget) }
                            }
                            Panel.REMINDER_TIME->{
                                Input(c["time_label"],s.timeInput,{onAction(Action.TimeInput(it))},c["time_hint"])
                                Text(c["reminder_hint"],style=MaterialTheme.typography.bodySmall)
                                Save(s) { onAction(Action.SaveTime) }
                            }
                            Panel.THEME->listOf("system","light","dark").forEach { theme ->
                                ChoiceRow(c["theme_$theme"],s.preferences.theme==theme) { onAction(Action.Theme(theme)) }
                            }
                            Panel.RESTORE->{
                                Text(c.format("restore_count",s.restoreCount));Text(c["restore_warning"])
                                Button(onClick={onAction(Action.Restore)},enabled=!s.busy) { Text(c["confirm"]) }
                            }
                            else->Unit
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ExpenseForm(s: UiState,onAction: (Action)->Unit) {
    val c=s.copy;val d=s.draft
    fun change(field: ExpenseField): (String)->Unit = { onAction(Action.ChangeExpense(field,it)) }
    Input(c["title"],d.title,change(ExpenseField.TITLE))
    Input(c["amount"],d.amount,change(ExpenseField.AMOUNT),numeric=true)
    Text(c["duration"],style=MaterialTheme.typography.labelMedium)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        DurationPreset.entries.forEach { preset ->
            FilterChip(selected=d.duration==preset,onClick={onAction(Action.Duration(preset))},label={Text(c["duration_${preset.name.lowercase()}"])})
        }
    }
    Input(c[if(d.duration==DurationPreset.DAY)"date" else "start_date"],d.start,change(ExpenseField.START),c["date_hint"])
    if(d.duration==DurationPreset.CUSTOM) {
        Input(c["end_date"],d.end,change(ExpenseField.END),c["date_hint"])
    } else if(d.duration!=DurationPreset.DAY) {
        Text(c["end_date"]+" · "+d.end,style=MaterialTheme.typography.bodySmall)
    }
    val days=runCatching { ChronoUnit.DAYS.between(LocalDate.parse(d.start),LocalDate.parse(d.end))+1 }.getOrNull()
    if(days!=null && days>0 && d.duration!=DurationPreset.DAY)Text(c.format("days_format",days),style=MaterialTheme.typography.labelMedium)
    Input(c["note"],d.note,change(ExpenseField.NOTE),multiline=true)
    run {
        WarmCard {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(c["reminder_enabled"],Modifier.weight(1f));Switch(checked=d.reminders,onCheckedChange={onAction(Action.ExpenseReminder(it))})
            }
            Text(c["purpose"],style=MaterialTheme.typography.labelSmall)
            ChoiceRow(c["purpose_cancel"],d.purpose==ReminderPurpose.CANCEL) { onAction(Action.Purpose(ReminderPurpose.CANCEL)) }
            ChoiceRow(c["purpose_pay"],d.purpose==ReminderPurpose.PAY) { onAction(Action.Purpose(ReminderPurpose.PAY)) }
            Input(c["renewal_date"],d.renewalDate,change(ExpenseField.RENEWAL_DATE),c["date_hint"])
            Text(c["renewal_hint"],style=MaterialTheme.typography.bodySmall)
        }
    }
    Save(s) { onAction(Action.SaveExpense) }
}
@Composable private fun HistoryPanel(s: UiState,onAction: (Action)->Unit) {
    val c=s.copy
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text(c["history_sort"],style=MaterialTheme.typography.labelMedium) }
        item { Input(c["search"],s.query,{onAction(Action.Search(it))}) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(null,ExpenseType.SUBSCRIPTION,ExpenseType.EXPENSE).forEach { type ->
                FilterChip(selected=s.historyType==type,onClick={onAction(Action.HistoryType(type))},
                    label={Text(c[when(type) { null->"all_types";ExpenseType.SUBSCRIPTION->"subscription";else->"expense" }])})
            }
        } }
        if(s.history.isEmpty())item { Empty(c["empty"]) }
        items(s.history,key={it.id}) { ExpenseCard(it,s,onAction,history=true) }
    }
}
@Composable private fun Input(label: String,value: String,onChange: (String)->Unit,hint: String?=null,numeric: Boolean=false,multiline: Boolean=false) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},placeholder=if(hint!=null){{ Text(hint) }}else null,
        modifier=Modifier.fillMaxWidth(),singleLine=!multiline,minLines=if(multiline)3 else 1,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))
}
@Composable private fun ChoiceRow(label: String,selected: Boolean,onClick: ()->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically) { RadioButton(selected=selected,onClick=onClick);TextButton(onClick=onClick) { Text(label) } }
}
@Composable private fun Save(s: UiState,onClick: ()->Unit) {
    Button(onClick=onClick,enabled=!s.busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)) { Text(s.copy["save"]) }
}
