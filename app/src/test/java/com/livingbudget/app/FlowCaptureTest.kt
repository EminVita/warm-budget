package com.livingbudget.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.livingbudget.app.data.*
import com.livingbudget.app.presentation.*
import com.livingbudget.app.ui.BudgetRoot
import com.livingbudget.core.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.Duration

class FlowCaptureActivity: ComponentActivity()

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlowCaptureTest {
    @Test fun captureFlowScreens() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val c=CopyRepository(context).current
        val today=LocalDate.of(2026,11,18)
        val e=Expense("sample",ExpenseType.SUBSCRIPTION,"音乐订阅",3000,LocalDate.of(2026,11,1),LocalDate.of(2026,12,1),LocalDate.of(2026,11,1),PaymentStatus.PAID,1793491200000)
        val old=e.copy(id="old",title="视频订阅",start=LocalDate.of(2026,8,18),endExclusive=LocalDate.of(2026,11,18),amountMinor=6000,note="季度订阅")
        val base=UiState(copy=c,today=today,month=YearMonth.from(today),expenses=listOf(e,old),visible=listOf(e),history=listOf(old),selected=e,
            budget=300000,summary=Summary(3000,3000,3000,100),notificationGranted=true)
        val draft=ExpenseDraft(title="音乐订阅",amount="30",start="2026-11-18",end="2026-12-17",paymentDate="2026-11-18")
        val screens=linkedMapOf(
            "home" to base,"budget" to base.copy(panel=Panel.BUDGET,budgetInput="3000"),
            "subscription" to base.copy(panel=Panel.EXPENSE,draft=draft),
            "expense" to base.copy(panel=Panel.EXPENSE,draft=draft.copy(type=ExpenseType.EXPENSE,title="电费",duration=DurationPreset.DAY,end="2026-11-18")),
            "detail" to base.copy(panel=Panel.DETAIL),
            "edit" to base.copy(panel=Panel.EXPENSE,draft=draft.copy(id="sample")),
            "renew" to base.copy(panel=Panel.EXPENSE,draft=draft.copy(start="2026-12-01",end="2026-12-31",paymentDate="2026-12-01",paid=false)),
            "history" to base.copy(page=Page.SETTINGS,panel=Panel.HISTORY),
            "settings" to base.copy(page=Page.SETTINGS),
            "time" to base.copy(page=Page.SETTINGS,panel=Panel.REMINDER_TIME),
            "theme" to base.copy(page=Page.SETTINGS,panel=Panel.THEME),
            "restore" to base.copy(page=Page.SETTINGS,panel=Panel.RESTORE,restoreCount=2),
            "update" to base.copy(page=Page.SETTINGS,message="update_unconfigured")
        )
        val dir=File("../design/flow/screens").apply { mkdirs() }
        for((name,state) in screens) {
            val controller=Robolectric.buildActivity(FlowCaptureActivity::class.java).setup().visible()
            val activity=controller.get()
            activity.setContent { BudgetRoot(state) {} }
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

            val root=if(state.panel!=Panel.NONE || state.message!=null) org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView else activity.window.decorView
            root.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(844,View.MeasureSpec.EXACTLY))
            root.layout(0,0,390,844)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            val bitmap=Bitmap.createBitmap(390,844,Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            if(name in setOf("subscription","expense","edit","renew","settings")) {
                root.measure(View.MeasureSpec.makeMeasureSpec(390,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1900,View.MeasureSpec.EXACTLY))
                root.layout(0,0,390,1900)
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
                val long=Bitmap.createBitmap(390,1900,Bitmap.Config.ARGB_8888)
                root.draw(Canvas(long))
                File(dir,"$name-long.png").outputStream().use { long.compress(Bitmap.CompressFormat.PNG,100,it) }
            }
            controller.pause().stop().destroy()
        }
    }
}
