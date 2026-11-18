package com.livingbudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.livingbudget.app.presentation.*

@Composable fun BudgetRoot(state: UiState,onAction: (Action)->Unit) {
    val c=state.copy
    BudgetTheme(state.preferences.theme) {
        if(c.entries.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            return@BudgetTheme
        }
        BackHandler(state.panel==Panel.NONE && state.page!=Page.HOME) { onAction(Action.Navigate(Page.HOME)) }
        Scaffold(containerColor=MaterialTheme.colorScheme.background,bottomBar={
            NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
                Page.entries.forEach { page ->
                    NavigationBarItem(selected=state.page==page,onClick={onAction(Action.Navigate(page))},
                        icon={LineIcon(if(page==Page.HOME)Glyph.HOME else Glyph.SETTINGS)},label={Text(c[if(page==Page.HOME)"home" else "settings"])})
                }
            }
        }) { padding -> Box(Modifier.padding(padding)) {
            when(state.page) { Page.HOME->HomeScreen(state,onAction);Page.SETTINGS->SettingsScreen(state,onAction) }
        } }
        PanelHost(state,onAction)
        if(state.message!=null || (state.error!=null && state.panel==Panel.NONE))AlertDialog(
            onDismissRequest={onAction(Action.DismissMessage)},text={Text(c[state.message?:state.error!!])},
            confirmButton={TextButton(onClick={onAction(Action.DismissMessage)}) { Text(c["close"]) }})
        state.update?.let { update -> AlertDialog(onDismissRequest={onAction(Action.DismissMessage)},
            title={Text(c["update_available"]+" · "+update.versionName)},text={Text(update.notes)},
            confirmButton={TextButton(onClick={onAction(Action.DownloadUpdate)}) { Text(c["download"]) }},
            dismissButton={TextButton(onClick={onAction(Action.DismissMessage)}) { Text(c["cancel"]) }}) }
    }
}
