package com.livingbudget.app.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.livingbudget.app.*
import com.livingbudget.app.data.DeliveryRow
import com.livingbudget.core.ReminderPurpose
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val CHANNEL="subscription_expiry"
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("expiry-check",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1,TimeUnit.HOURS).build())
    }
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("expiry-now",ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<ReminderWorker>().build())
    }
    fun cancelAll(context: Context) { NotificationManagerCompat.from(context).cancelAll() }
    fun cancelExpense(context: Context,id: String) {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag?.startsWith("$id:")==true }.forEach { manager.cancel(it.tag,it.id) }
    }
    fun allowed(context: Context): Boolean {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled())return false
        return context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    }
}
class ReminderWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    companion object { private val mutex=Mutex() }
    override suspend fun doWork(): Result = mutex.withLock {
        val app=applicationContext as BudgetApplication
        val prefs=app.preferences.flow.first()
        val now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        if(!prefs.reminders || !ReminderScheduler.allowed(app) || now.toLocalTime()<LocalTime.of(prefs.hour,prefs.minute))return@withLock Result.success()
        val copy=app.copy.current
        val manager=app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ReminderScheduler.CHANNEL,copy["notification_channel"],NotificationManager.IMPORTANCE_DEFAULT))
        val dao=app.database.ledger()
        try {
            for(row in dao.expenses()) {
                val e=dao.expense(row.id)?.domain()?:continue
                if(!e.shouldRemind(now.toLocalDate()))continue
                val key="${e.id}:${e.cycleKey}:${now.toLocalDate()}"
                if(dao.claim(DeliveryRow(key,System.currentTimeMillis()))==-1L)continue
                try {
                    val intent=Intent(app,MainActivity::class.java).putExtra("expenseId",e.id)
                        .setAction(key).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    val pending=PendingIntent.getActivity(app,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val days=java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(),e.reminderBase)
                    val body=copy.format(if(e.reminderPurpose==ReminderPurpose.CANCEL)"notification_cancel" else "notification_pay",e.title,days)
                    val notification=NotificationCompat.Builder(app,ReminderScheduler.CHANNEL)
                        .setSmallIcon(com.livingbudget.app.R.drawable.ic_notification)
                        .setContentTitle(copy["notification_title"]).setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pending)
                        .setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
                    manager.notify(key,1,notification)
                } catch(ex: Exception) { dao.release(key);throw ex }
            }
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}
