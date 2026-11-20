package com.livingbudget.core

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ExpenseType { SUBSCRIPTION, EXPENSE }
enum class PaymentStatus { PAID, PLANNED }
enum class ReminderPurpose { CANCEL, PAY }
enum class ExpenseStatus { UPCOMING, ACTIVE, ENDED }

data class Expense(
    val id: String,
    val type: ExpenseType,
    val title: String,
    val amountMinor: Long,
    val start: LocalDate,
    val endExclusive: LocalDate,
    val paymentDate: LocalDate,
    val paymentStatus: PaymentStatus,
    val createdAt: Long,
    val note: String = "",
    val category: String = "",
    val renewedFromId: String? = null,
    val reminderEnabled: Boolean = true,
    val reminderPurpose: ReminderPurpose = ReminderPurpose.CANCEL,
    val renewalDate: LocalDate? = null,
    val reminderHandled: Boolean = false
) {
    init {
        require(title.isNotBlank() && title.length <= 100)
        require(amountMinor in 0..9_999_999_999L)
        require(endExclusive > start)
        require(note.length <= 2000 && category.length <= 100)
    }
    val days: Long get() = ChronoUnit.DAYS.between(start, endExclusive)
    val endInclusive: LocalDate get() = endExclusive.minusDays(1)
    val reminderBase: LocalDate get() = renewalDate ?: endInclusive
    val cycleKey: String get() = reminderBase.toString()
    fun status(today: LocalDate): ExpenseStatus = when {
        today < start -> ExpenseStatus.UPCOMING
        today >= endExclusive -> ExpenseStatus.ENDED
        else -> ExpenseStatus.ACTIVE
    }
    fun elapsed(today: LocalDate): Long = (ChronoUnit.DAYS.between(start, today) + 1).coerceIn(0, days)
    fun daily(date: LocalDate): Long {
        if (date < start || date >= endExclusive) return 0
        return prefix(date.plusDays(1)) - prefix(date)
    }
    private fun prefix(date: LocalDate): Long = BigDecimal.valueOf(amountMinor)
        .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(start, date)))
        .divide(BigDecimal.valueOf(days), 0, java.math.RoundingMode.HALF_UP).longValueExact()
    // Prefix sums avoid walking every day of a long subscription.
    fun allocated(from: LocalDate, until: LocalDate): Long {
        val first = maxOf(start, from)
        val last = minOf(endExclusive, until)
        if (last <= first) return 0
        return prefix(last) - prefix(first)
    }
    fun shouldRemind(today: LocalDate): Boolean =
        reminderEnabled && !reminderHandled &&
            status(today) != ExpenseStatus.ENDED &&
            ChronoUnit.DAYS.between(today, reminderBase) in 1..3
}

data class Budget(val month: YearMonth, val amountMinor: Long, val recurring: Boolean) {
    init { require(amountMinor in 0..9_999_999_999L) }
}

data class Summary(val required: Long, val paid: Long, val allocated: Long, val daily: Long) {
    val pending: Long get() = required - paid
    /** Calendar-month average in minor units, not the amount incurred on a specific day. */
    fun monthlyDaily(month: YearMonth): Long = BigDecimal.valueOf(allocated)
        .divide(BigDecimal.valueOf(month.lengthOfMonth().toLong()),0,java.math.RoundingMode.HALF_UP)
        .longValueExact()
}

object Ledger {
    fun active(expenses: List<Expense>,today: LocalDate): List<Expense> =
        expenses.filter { it.status(today)==ExpenseStatus.ACTIVE }
    fun amount(raw: String): Long? = try {
        if (!raw.matches(Regex("\\d{1,8}(\\.\\d{1,2})?"))) null
        else BigDecimal(raw).movePointRight(2).longValueExact().takeIf { it in 0..9_999_999_999L }
    } catch (_: ArithmeticException) { null }

    fun budget(budgets: List<Budget>, month: YearMonth): Long? =
        budgets.firstOrNull { it.month == month && !it.recurring }?.amountMinor
            ?: budgets.filter { it.recurring && it.month <= month }.maxByOrNull { it.month }?.amountMinor

    fun summary(expenses: List<Expense>, month: YearMonth, today: LocalDate): Summary {
        val payments = expenses.filter { YearMonth.from(it.paymentDate) == month }
        return Summary(
            required = payments.sumOf { it.amountMinor },
            paid = payments.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.amountMinor },
            allocated = expenses.sumOf { it.allocated(month.atDay(1), month.plusMonths(1).atDay(1)) },
            daily = expenses.sumOf { it.daily(today) }
        )
    }
    fun history(expenses: List<Expense>, today: LocalDate): List<Expense> =
        expenses.filter { it.status(today) == ExpenseStatus.ENDED }
            .sortedWith(compareByDescending<Expense> { it.createdAt }.thenBy { it.id })
}
