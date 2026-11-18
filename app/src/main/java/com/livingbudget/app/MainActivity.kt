package com.livingbudget.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.livingbudget.app.data.readBytesLimited
import com.livingbudget.app.presentation.*
import com.livingbudget.app.reminders.ReminderScheduler
import com.livingbudget.app.ui.BudgetRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity: ComponentActivity() {
    private val model: BudgetViewModel by viewModels()
    private var pendingFile: FileAction?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        model.dispatch(Action.NotificationPermission(it && ReminderScheduler.allowed(this)))
    }
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val action=pendingFile;pendingFile=null
        if(uri!=null && action!=null)lifecycleScope.launch {
            try {
                val text=model.exportFile(action)
                withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { it.write(text) } }
            } catch (_: Exception) { model.fileError() }
        }
    }
    private val import=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val action=pendingFile;pendingFile=null
        if(uri!=null && action!=null)lifecycleScope.launch {
            try {
                val text=withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)!!.use {
                    it.readBytesLimited(if(action==FileAction.IMPORT_COPY)256_000 else 10_000_000).toString(Charsets.UTF_8) } }
                model.importFile(action,text)
            } catch (_: Exception) { model.fileError() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingFile=savedInstanceState?.getString("pendingFile")?.let { FileAction.valueOf(it) }
        setContent { val state by model.state.collectAsStateWithLifecycle();BudgetRoot(state,model::dispatch) }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.effects.collect { effect -> when(effect) {
                is Effect.ChooseFile -> {
                    pendingFile=effect.action
                    when(effect.action) {
                        FileAction.EXPORT_BACKUP->export.launch("living-budget-backup.json")
                        FileAction.EXPORT_COPY->export.launch("copy.zh-CN.json")
                        else->import.launch(arrayOf("application/json","text/*","application/octet-stream"))
                    }
                }
                Effect.RequestNotifications -> if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else if(ReminderScheduler.allowed(this@MainActivity))model.dispatch(Action.NotificationPermission(true))
                    else { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName)) }
                is Effect.OpenUrl -> runCatching { startActivity(Intent(Intent.ACTION_VIEW,effect.url.toUri())) }.onFailure { model.fileError() }
            } }
        } }
        intent?.getStringExtra("expenseId")?.let { id ->
            lifecycleScope.launch { model.state.first { it.expenses.any { e->e.id==id } };model.dispatch(Action.Select(id)) }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);intent.getStringExtra("expenseId")?.let { model.dispatch(Action.Select(it)) } }
    override fun onResume() { super.onResume();model.setNotificationGranted(ReminderScheduler.allowed(this));model.dispatch(Action.Refresh) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingFile",pendingFile?.name);super.onSaveInstanceState(outState) }
}
