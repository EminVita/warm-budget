package com.livingbudget.core

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LedgerTest {
    private fun expense(start: String, end: String, amount: Long = 100) = Expense(
        "id", ExpenseType.SUBSCRIPTION, "test", amount, LocalDate.parse(start), LocalDate.parse(end),
        LocalDate.parse(start), PaymentStatus.PAID, 1
    )
    @Test fun remainderIsConserved() {
        val e = expense("2026-11-18", "2026-11-21")
        assertEquals(listOf(33L,34L,33L), (0..2).map { e.daily(e.start.plusDays(it.toLong())) })
        assertEquals(100L, e.allocated(e.start, e.endExclusive))
        assertEquals(0L, e.daily(e.endExclusive))
    }
    @Test fun crossMonthAndPaymentAreSeparate() {
        val e = expense("2026-11-30", "2026-12-03", 3000)
        val nov = Ledger.summary(listOf(e), YearMonth.of(2026,11), e.start)
        val dec = Ledger.summary(listOf(e), YearMonth.of(2026,12), e.start)
        assertEquals(1000L,nov.allocated); assertEquals(2000L,dec.allocated)
        assertEquals(3000L,nov.required); assertEquals(0L,dec.required)
    }
    @Test fun expiryAndHistoryOrder() {
        val e = expense("2026-11-01","2026-11-19")
        assertEquals(ExpenseStatus.ACTIVE,e.status(LocalDate.parse("2026-11-18")))
        val laterAdded = e.copy(id="second", createdAt=2, endExclusive=LocalDate.parse("2026-11-10"))
        assertEquals("second",Ledger.history(listOf(e,laterAdded),LocalDate.parse("2026-11-20")).first().id)
        assertEquals(e.days,e.elapsed(LocalDate.parse("2026-12-01")))
    }
    @Test fun remindersOnlyInThreeDayWindow() {
        val e = expense("2026-11-01", "2026-12-01")
        assertEquals(listOf(false,true,true,true,false), (26..30).map { e.shouldRemind(LocalDate.of(2026,11,it)) })
        assertFalse(e.copy(reminderHandled=true).shouldRemind(LocalDate.of(2026,11,28)))
        assertTrue(e.copy(type=ExpenseType.EXPENSE).shouldRemind(LocalDate.of(2026,11,28)))
    }
    @Test fun budgetChangesDoNotRewriteEarlierMonths() {
        val bs = listOf(Budget(YearMonth.of(2026,10),10000,true), Budget(YearMonth.of(2026,12),20000,true), Budget(YearMonth.of(2026,11),15000,false))
        assertEquals(10000L,Ledger.budget(bs,YearMonth.of(2026,10)))
        assertEquals(15000L,Ledger.budget(bs,YearMonth.of(2026,11)))
        assertEquals(20000L,Ledger.budget(bs,YearMonth.of(2027,1)))
    }
    @Test fun moneyValidation() {
        assertEquals(123L,Ledger.amount("1.23"))
        listOf("-1","1.234","NaN","1e3","100000000").forEach { assertNull(Ledger.amount(it)) }
    }
    @Test fun leapYearAndCalendarMonth() {
        assertEquals(LocalDate.of(2025,2,28),LocalDate.of(2024,2,29).plusMonths(12))
        val e=expense("2024-02-29","2025-02-28",36500)
        assertEquals(365L,e.days)
    }
    @Test fun allocationConservationAcrossDurations() {
        for (days in 1..200) for (amount in listOf(0L,1L,99L,10001L)) {
            val start=LocalDate.of(2026,1,1)
            val e=expense(start.toString(),start.plusDays(days.toLong()).toString(),amount)
            assertEquals(amount,(0 until days).sumOf { e.daily(start.plusDays(it.toLong())) })
        }
    }
    @Test fun marchAprilAllocationAndExpiryBoundary() {
        val e=expense("2026-03-20","2026-04-03",30000)
        assertEquals(14L,e.days)
        val march=Ledger.summary(listOf(e),YearMonth.of(2026,3),LocalDate.of(2026,3,20))
        val april=Ledger.summary(listOf(e),YearMonth.of(2026,4),LocalDate.of(2026,4,3))
        assertEquals(25714L,march.allocated)
        assertEquals(4286L,april.allocated)
        assertTrue(e.daily(LocalDate.of(2026,4,2))>0)
        assertEquals(0L,april.daily)
        assertEquals(30000L,march.allocated+april.allocated)
    }
    @Test fun singleDayExpenseIsAveragedAcrossCalendarMonth() {
        val e=expense("2026-03-20","2026-03-21",6000)
        val before=Ledger.summary(listOf(e),YearMonth.of(2026,3),LocalDate.of(2026,3,19))
        val after=Ledger.summary(listOf(e),YearMonth.of(2026,3),LocalDate.of(2026,3,21))
        assertEquals(6000L,after.allocated)
        assertEquals(194L,after.monthlyDaily(YearMonth.of(2026,3)))
        assertEquals(before.monthlyDaily(YearMonth.of(2026,3)),after.monthlyDaily(YearMonth.of(2026,3)))
        assertEquals(0L,after.daily)
    }
    @Test fun monthlyAverageUsesRealMonthLengthIncludingLeapYear() {
        val total=Summary(0,0,6000,0)
        assertEquals(200L,total.monthlyDaily(YearMonth.of(2026,11)))
        assertEquals(194L,total.monthlyDaily(YearMonth.of(2026,3)))
        assertEquals(214L,total.monthlyDaily(YearMonth.of(2026,2)))
        assertEquals(207L,total.monthlyDaily(YearMonth.of(2024,2)))
        assertEquals(0L,Summary(0,0,0,0).monthlyDaily(YearMonth.of(2026,11)))
    }
    @Test fun crossMonthAverageKeepsHistoricalAllocationAfterExpiry() {
        val e=expense("2026-03-20","2026-04-03",30000)
        val march=Ledger.summary(listOf(e),YearMonth.of(2026,3),LocalDate.of(2026,4,3))
        val april=Ledger.summary(listOf(e),YearMonth.of(2026,4),LocalDate.of(2026,4,3))
        assertEquals(829L,march.monthlyDaily(YearMonth.of(2026,3)))
        assertEquals(143L,april.monthlyDaily(YearMonth.of(2026,4)))
        assertEquals(30000L,march.allocated+april.allocated)
    }
    @Test fun futureRecordsAreRetainedButAppearOnlyOnStartDate() {
        val future=expense("2026-11-20","2026-11-22",3000)
        assertTrue(Ledger.active(listOf(future),LocalDate.of(2026,11,19)).isEmpty())
        assertEquals(listOf(future),Ledger.active(listOf(future),LocalDate.of(2026,11,20)))
        assertEquals(listOf(future),Ledger.active(listOf(future),LocalDate.of(2026,11,21)))
        assertTrue(Ledger.active(listOf(future),LocalDate.of(2026,11,22)).isEmpty())
        assertEquals(listOf(future),Ledger.history(listOf(future),LocalDate.of(2026,11,22)))
    }
}
