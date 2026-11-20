package com.livingbudget.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livingbudget.app.BuildConfig
import com.livingbudget.app.presentation.*
import com.livingbudget.core.*
import java.math.BigDecimal
import java.math.RoundingMode

/** These layouts only render immutable state and dispatch semantic actions. */
@Composable fun HomeScreen(s: UiState,onAction: (Action)->Unit) {
    val c=s.copy
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(c["app_name"],fontSize=28.sp,fontWeight=FontWeight.SemiBold)
                TextButton(onClick={onAction(Action.CurrentMonth)}) { Text(c.format("month_format",s.month.toString())) } }
            IconButton(onClick={onAction(Action.Month(-1))}) { LineIcon(Glyph.BACK,c["previous_month"]) }
            IconButton(onClick={onAction(Action.Month(1))}) { LineIcon(Glyph.ARROW,c["next_month"]) }
        } }
        item { WarmCard(tinted=true,modifier=Modifier.clickable { onAction(Action.OpenBudget) }) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(c["budget"],style=MaterialTheme.typography.labelMedium);LineIcon(Glyph.WALLET) }
            Text(s.budget?.let(::money)?:c["budget_unset"],fontSize=36.sp,fontWeight=FontWeight.Medium)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text(c["required"],style=MaterialTheme.typography.labelSmall);Text(money(s.summary.allocated)) }
                val remaining=s.budget?.minus(s.summary.allocated)
                Column(horizontalAlignment=Alignment.End) {
                    Text(c[if(remaining!=null && remaining<0)"over_budget" else "remaining"],style=MaterialTheme.typography.labelSmall)
                    Text(remaining?.let { money(kotlin.math.abs(it)) }?:"—")
                }
            }
            Text(c["estimate_hint"],style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Metric(c["monthly_daily_cost"],money(s.summary.monthlyDaily(s.month)),Modifier.weight(1f),true)
            Metric(c["monthly_cost"],money(s.summary.allocated),Modifier.weight(1f))
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick={onAction(Action.Add(ExpenseType.SUBSCRIPTION))},modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(12.dp)) {
                LineIcon(Glyph.PLUS);Spacer(Modifier.width(6.dp));Text(c["add_subscription"])
            }
            FilledTonalButton(onClick={onAction(Action.Add(ExpenseType.EXPENSE))},modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(12.dp)) { Text(c["add_expense"]) }
        } }
        item { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(c["expenses"],Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
        } }
        if(s.visible.isEmpty())item { Empty(c["empty"]) }
        items(s.visible,key={it.id}) { expense -> ExpenseCard(expense,s,onAction) }

    }
}
@Composable private fun Metric(label: String,value: String,modifier: Modifier,tinted: Boolean=false) {
    Surface(modifier,shape=RoundedCornerShape(22.dp),color=if(tinted)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,fontSize=22.sp,fontWeight=FontWeight.Medium)
        }
    }
}
@Composable fun ExpenseCard(e: Expense,s: UiState,onAction: (Action)->Unit,history: Boolean=false) {
    val c=s.copy
    WarmCard(modifier=Modifier.clickable { onAction(Action.Select(e.id)) }) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            LineIcon(if(e.type==ExpenseType.SUBSCRIPTION)Glyph.LEAF else Glyph.WALLET)
            Text(e.title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
            Text(money(e.amountMinor))
        }
        Text(c.format("period_format",e.start.toString(),e.endInclusive.toString()),style=MaterialTheme.typography.bodySmall)
        val daily=BigDecimal.valueOf(e.amountMinor,2).divide(BigDecimal.valueOf(e.days),2,RoundingMode.HALF_UP)
        Text(c.format("days_format",e.days)+" · "+c.format("daily_format","¥ $daily"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(history) {
            Text(c.format("elapsed_format",e.elapsed(s.today)),style=MaterialTheme.typography.labelSmall)
            Text(c.format("created_format",java.time.Instant.ofEpochMilli(e.createdAt).atZone(java.time.ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString()),style=MaterialTheme.typography.labelSmall)
            if(e.note.isNotBlank())Text(e.note,style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable fun SettingsScreen(s: UiState,onAction: (Action)->Unit) {
    val c=s.copy
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { Text(c["settings"],fontSize=28.sp,fontWeight=FontWeight.SemiBold) }
        item { WarmCard(tinted=true) { SettingRow(c["budget_settings"],Glyph.WALLET,s.budget?.let(::money)?:c["budget_unset"]) { onAction(Action.OpenBudget) } } }
        item { GroupLabel(c["general"]);WarmCard {
            SettingRow(c["history"],Glyph.HISTORY,hint=c["history_hint"]) { onAction(Action.OpenHistory) }
            HorizontalDivider()
            SettingRow(c["appearance"],Glyph.SUN,c["theme_"+s.preferences.theme]) { onAction(Action.OpenTheme) }
            HorizontalDivider()
            Row(verticalAlignment=Alignment.CenterVertically) { Text(c["reminders"],Modifier.weight(1f));Switch(checked=s.preferences.reminders && s.notificationGranted,onCheckedChange={onAction(Action.Reminders(it))}) }
            if(!s.notificationGranted)Text(c["reminder_off"],style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error)
            SettingRow(c["reminder_time"],Glyph.BELL,"%02d:%02d".format(s.preferences.hour,s.preferences.minute)) { onAction(Action.OpenTime) }
            Text(c["reminder_hint"],style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { GroupLabel(c["data"]);WarmCard {
            SettingRow(c["export_backup"],Glyph.DOWNLOAD) { onAction(Action.File(FileAction.EXPORT_BACKUP)) }
            HorizontalDivider();SettingRow(c["import_backup"],Glyph.UPLOAD) { onAction(Action.File(FileAction.IMPORT_BACKUP)) }
        } }
        item { GroupLabel(c["copy_settings"]);WarmCard {
            SettingRow(c["export_copy"],Glyph.DOWNLOAD) { onAction(Action.File(FileAction.EXPORT_COPY)) }
            SettingRow(c["import_copy"],Glyph.UPLOAD) { onAction(Action.File(FileAction.IMPORT_COPY)) }
            SettingRow(c["reset_copy"],Glyph.HISTORY) { onAction(Action.ResetCopy) }
            Text(c["copy_hint"],style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { GroupLabel(c["about"]);WarmCard {
            SettingRow(c["check_update"],Glyph.DOWNLOAD,c.format("version_format",BuildConfig.VERSION_NAME)) { onAction(Action.CheckUpdate) }
        } }
    }
}
@Composable private fun GroupLabel(text: String) { Text(text,Modifier.padding(start=4.dp,bottom=10.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Empty(text: String) { Box(Modifier.fillMaxWidth().padding(36.dp),contentAlignment=Alignment.Center) { Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
