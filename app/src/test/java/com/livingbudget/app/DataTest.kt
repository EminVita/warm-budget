package com.livingbudget.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.livingbudget.app.data.*
import com.livingbudget.core.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class DataTest {
    private lateinit var db: BudgetDatabase
    private lateinit var backup: BackupRepository
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),BudgetDatabase::class.java).build()
        backup=BackupRepository(db)
    }
    @After fun close() { db.close() }
    private fun expense()=Expense(UUID.randomUUID().toString(),ExpenseType.SUBSCRIPTION,"Example",100,
        LocalDate.of(2026,11,1),LocalDate.of(2026,12,1),LocalDate.of(2026,11,1),PaymentStatus.PAID,1)
    @Test fun backupRoundTripKeepsAuditAndMoney()=runBlocking {
        val e=expense();db.ledger().saveExpense(ExpenseRow.from(e))
        val revision=RevisionRow(UUID.randomUUID().toString(),e.id,2,BackupRepository.encode(e.copy(title="Before")).toString())
        db.ledger().saveRevision(revision)
        val raw=backup.export();backup.restore(backup.parse(raw))
        assertEquals(e,db.ledger().expenses().single().domain())
        assertEquals(revision,db.ledger().revisions().single())
    }
    @Test fun invalidImportDoesNotTouchExistingData()=runBlocking {
        val e=expense();db.ledger().saveExpense(ExpenseRow.from(e))
        val invalid=JSONObject(backup.export())
        invalid.getJSONArray("expenses").getJSONObject(0).put("endExclusive","2026-10-01")
        assertThrows(IllegalArgumentException::class.java) { backup.parse(invalid.toString()) }
        assertEquals(e,db.ledger().expenses().single().domain())
    }
    @Test fun deliveryClaimIsUnique()=runBlocking {
        assertTrue(db.ledger().claim(DeliveryRow("id:cycle:today",1))!=-1L)
        assertEquals(-1L,db.ledger().claim(DeliveryRow("id:cycle:today",2)))
    }
    @Test fun copyEditsAreIsolatedAndRejectBrokenTemplates() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val copy=CopyRepository(context);copy.reset()
        val original=copy.current["notification_cancel"]
        copy.import("""{"home":"Custom"}""")
        assertEquals("Custom",copy.current["home"])
        assertEquals(original,copy.current["notification_cancel"])
        assertThrows(IllegalArgumentException::class.java) { copy.import("""{"notification_cancel":"Broken"}""") }
        assertEquals("Custom",copy.current["home"])
        assertThrows(IllegalArgumentException::class.java) { copy.import("""{"not_a_key":"x"}""") }
        copy.reset()
    }
}
