package ir.dastyar.ghest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ir.dastyar.ghest.data.AppDatabase
import ir.dastyar.ghest.data.Check
import ir.dastyar.ghest.data.CheckDirection
import ir.dastyar.ghest.data.CheckStatus
import ir.dastyar.ghest.data.DebtDirection
import ir.dastyar.ghest.data.Installment
import ir.dastyar.ghest.data.Loan
import ir.dastyar.ghest.data.LoanType
import ir.dastyar.ghest.data.PersonalDebt
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ======================= داشبورد ======================= */

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    data class UpcomingItem(
        val kind: String, // INSTALLMENT / CHECK / DEBT
        val refId: Long,  // loanId برای قسط، id برای بقیه
        val title: String,
        val subtitle: String,
        val amount: Long,
        val dueEpochDay: Long
    )

    data class DashboardState(
        val remainingInstallments: Long = 0,
        val paidThisMonth: Long = 0,
        val overdueCount: Int = 0,
        val openChecksCount: Int = 0,
        val openChecksAmount: Long = 0,
        val netPersonalDebt: Long = 0,
        val upcoming: List<UpcomingItem> = emptyList()
    )

    val state: StateFlow<DashboardState> = combine(
        db.installmentDao().observeOpen(),
        db.checkDao().observeAll(),
        db.debtDao().observeOpen()
    ) { inst, checks, debts ->
        // [فیکس دور ۱] تاریخ امروز در هر به‌روزرسانی دوباره محاسبه می‌شود؛
        // قبلاً یک‌بار در ساخت ViewModel گرفته می‌شد و بعد از نیمه‌شب،
        // «عقب‌افتاده» و «پرداخت این ماه» تا ری‌استارت اپ غلط می‌ماند.
        val today = Jalali.todayEpochDay()
        val todayInst = Jalali.toJalali(today)
        val paidThisMonth = db.installmentDao().getAll()
            .filter { it.isPaid && it.paidEpochDay != null && Jalali.toJalali(it.paidEpochDay!!)[1] == todayInst[1] && Jalali.toJalali(it.paidEpochDay!!)[0] == todayInst[0] }
            .sumOf { it.amount }

        val items = mutableListOf<UpcomingItem>()
        inst.forEach { row ->
            val diff = row.installment.dueEpochDay - today
            if (diff <= 45) items.add(
                UpcomingItem(
                    kind = "INSTALLMENT", refId = row.installment.loanId,
                    title = row.loanTitle,
                    subtitle = "قسط " + Farsi.digit(row.installment.number) + " • " + LoanType.label(row.loanType),
                    amount = row.installment.amount, dueEpochDay = row.installment.dueEpochDay
                )
            )
        }
        checks.filter { it.status == CheckStatus.OPEN && it.dueEpochDay - today <= 45 }.forEach { c ->
            items.add(
                UpcomingItem(
                    kind = "CHECK", refId = c.id,
                    title = c.title,
                    subtitle = "چک " + CheckDirection.label(c.direction) + if (c.bank.isNotBlank()) " • ${c.bank}" else "",
                    amount = c.amount, dueEpochDay = c.dueEpochDay
                )
            )
        }
        debts.filter { d -> !d.isSettled && d.dueEpochDay != null && d.dueEpochDay - today <= 45 }.forEach { d ->
            items.add(
                UpcomingItem(
                    kind = "DEBT", refId = d.id,
                    title = d.person,
                    subtitle = DebtDirection.label(d.direction),
                    amount = d.amount, dueEpochDay = d.dueEpochDay!!
                )
            )
        }

        DashboardState(
            remainingInstallments = inst.sumOf { it.installment.amount },
            paidThisMonth = paidThisMonth,
            overdueCount = inst.count { it.installment.dueEpochDay < today } +
                checks.count { it.status == CheckStatus.OPEN && it.dueEpochDay < today },
            openChecksCount = checks.count { it.status == CheckStatus.OPEN },
            openChecksAmount = checks.filter { it.status == CheckStatus.OPEN && it.direction == CheckDirection.PAYABLE }
                .sumOf { it.amount },
            netPersonalDebt = debts.filter { !it.isSettled }.sumOf {
                if (it.direction == DebtDirection.I_OWE) it.amount else -it.amount
            },
            upcoming = items.sortedBy { it.dueEpochDay }.take(25)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

/* ======================= اقساط ======================= */

class LoansViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val loans: StateFlow<List<Loan>> = db.loanDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedLoans: StateFlow<List<Loan>> = db.loanDao().observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Map<Long, InstallmentDaoAlias.LoanStatX>> =
        combine(db.installmentDao().observeStats(), db.loanDao().observeActive()) { stats, _ ->
            // [فیکس دور ۱] آمار همه وام‌ها (فعال + بایگانی) نگه‌داری می‌شود؛
            // قبلاً فقط وام‌های فعال آمار داشتند و کارت وام‌های بایگانی‌شده
            // «۰ از ۲۴» با مانده کامل نمایش داده می‌شد.
            stats.associate {
                it.loanId to InstallmentDaoAlias.LoanStatX(it.total, it.paidCount, it.paidAmount, it.nextDue)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addLoan(
        title: String, creditor: String, type: String,
        amount: Long, count: Int, stepDays: Int,
        firstDue: Long, note: String, onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val today = Jalali.todayEpochDay()
            val loan = Loan(
                title = title.trim(), creditor = creditor.trim(), type = type,
                installmentAmount = amount, count = count, stepDays = stepDays,
                firstDueEpochDay = firstDue, note = note.trim(),
                createdAtEpochDay = today
            )
            val id = db.loanDao().insert(loan)
            // [فیکس دور ۱] محدودسازی ایمن ورودی‌ها؛ ورودی‌های غیرواقعی (مثلاً ۹۹۹۹ قسط)
            // قبلاً بی‌سروصدا هزاران ردیف می‌ساختند.
            val safeCount = count.coerceIn(1, 360)
            val safeAmount = amount.coerceIn(1L, 999_999_999_999L)
            val safeStep = stepDays.coerceIn(1, 3650)
            val items = (1..safeCount).map { n ->
                Installment(
                    loanId = id, number = n,
                    dueEpochDay = firstDue + (n - 1) * safeStep.toLong(),
                    amount = safeAmount
                )
            }
            db.installmentDao().insertAll(items)
            onDone()
        }
    }

    fun archiveLoan(id: Long, archived: Boolean) {
        viewModelScope.launch { db.loanDao().setArchived(id, archived) }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch { db.loanDao().delete(loan) }
    }
}

/** شکل ساده‌شده آمار برای UI */
object InstallmentDaoAlias {
    data class LoanStatX(val total: Int, val paidCount: Int, val paidAmount: Long, val nextDue: Long?)
}

/* ======================= جزئیات وام ======================= */

class LoanDetailViewModel(app: Application, state: SavedStateHandle) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    val loanId: Long = state.get<String>("loanId")?.toLongOrNull() ?: -1L

    data class DetailState(
        val loan: Loan? = null,
        val installments: List<Installment> = emptyList(),
        val total: Long = 0,
        val paid: Long = 0
    ) {
        val progress: Float get() = if (total <= 0) 0f else paid.toFloat() / total
        val overdueCount: Int get() = installments.count { !it.isPaid && it.dueEpochDay < Jalali.todayEpochDay() }
    }

    val state: StateFlow<DetailState> = combine(
        db.loanDao().observeById(loanId),
        db.installmentDao().observeByLoan(loanId)
    ) { loan, insts ->
        DetailState(
            loan = loan,
            installments = insts,
            total = insts.sumOf { it.amount },
            paid = insts.filter { it.isPaid }.sumOf { it.amount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    fun togglePaid(inst: Installment) {
        viewModelScope.launch {
            if (inst.isPaid) db.installmentDao().setPaid(inst.id, false, null)
            else db.installmentDao().setPaid(inst.id, true, Jalali.todayEpochDay())
        }
    }

    fun payAllOverdue() {
        viewModelScope.launch {
            db.installmentDao().payUntil(loanId, Jalali.todayEpochDay())
        }
    }

    fun archive(archived: Boolean) {
        viewModelScope.launch { db.loanDao().setArchived(loanId, archived) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            db.loanDao().getById(loanId)?.let { db.loanDao().delete(it) }
            onDone()
        }
    }
}

/* ======================= چک‌ها ======================= */

class ChecksViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val checks: StateFlow<List<Check>> = db.checkDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        id: Long?, direction: String, title: String, bank: String, number: String,
        sayadId: String, amount: Long, due: Long, note: String, status: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val item = Check(
                id = id ?: 0, direction = direction, title = title.trim(),
                bank = bank.trim(), number = number.trim(), sayadId = sayadId.trim(),
                amount = amount, dueEpochDay = due, note = note.trim(), status = status
            )
            if (id == null) db.checkDao().insert(item) else db.checkDao().update(item)
            onDone()
        }
    }

    fun setStatus(id: Long, status: String) {
        viewModelScope.launch { db.checkDao().setStatus(id, status) }
    }

    fun delete(check: Check) {
        viewModelScope.launch { db.checkDao().delete(check) }
    }
}

/* ======================= بدهی شخصی ======================= */

class DebtsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val open: StateFlow<List<PersonalDebt>> = db.debtDao().observeOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settled: StateFlow<List<PersonalDebt>> = db.debtDao().observeSettled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(id: Long?, person: String, direction: String, amount: Long, due: Long?, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val item = PersonalDebt(
                id = id ?: 0, person = person.trim(), direction = direction,
                amount = amount, dueEpochDay = due, note = note.trim(),
                isSettled = false, createdAtEpochDay = Jalali.todayEpochDay()
            )
            if (id == null) db.debtDao().insert(item) else db.debtDao().update(item)
            onDone()
        }
    }

    fun setSettled(id: Long, settled: Boolean) {
        viewModelScope.launch { db.debtDao().setSettled(id, settled, if (settled) Jalali.todayEpochDay() else null) }
    }

    fun delete(debt: PersonalDebt) {
        viewModelScope.launch { db.debtDao().delete(debt) }
    }
}

/* ======================= گزارش ======================= */

class ReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    data class ReportState(
        val year: Int = currentYearInit,
        val dueByMonth: List<Long> = List(12) { 0L },   // اقساط سررسید هر ماه
        val checksByMonth: List<Long> = List(12) { 0L }, // چک‌های پرداختی هر ماه
        val totalDue: Long = 0,
        val totalPaid: Long = 0,
        val perLoan: List<Triple<String, Long, Long>> = emptyList() // عنوان، پرداخت‌شده، کل
    ) {
        val remaining: Long get() = totalDue - totalPaid
        val progress: Float get() = if (totalDue <= 0) 0f else totalPaid.toFloat() / totalDue
    }

    private val year: kotlinx.coroutines.flow.MutableStateFlow<Int> =
        kotlinx.coroutines.flow.MutableStateFlow(currentYearInit)

    val state: StateFlow<ReportState> = combine(
        db.installmentDao().observeAllOfActive(),
        db.checkDao().observeAll(),
        year
    ) { insts, checks, y ->
        val due = MutableList(12) { 0L }
        val chk = MutableList(12) { 0L }
        insts.forEach { row ->
            val j = Jalali.toJalali(row.installment.dueEpochDay)
            if (j[0] == y) due[j[1] - 1] += row.installment.amount
        }
        checks.filter { it.direction == CheckDirection.PAYABLE && it.status != CheckStatus.CANCELLED }.forEach { c ->
            val j = Jalali.toJalali(c.dueEpochDay)
            if (j[0] == y) chk[j[1] - 1] += c.amount
        }
        val perLoan = insts.groupBy { it.loanTitle }.map { (title, rows) ->
            Triple(
                title,
                rows.filter { it.installment.isPaid }.sumOf { it.installment.amount },
                rows.sumOf { it.installment.amount }
            )
        }.sortedByDescending { it.second }
        ReportState(
            year = y,
            dueByMonth = due,
            checksByMonth = chk,
            totalDue = insts.sumOf { it.installment.amount },
            totalPaid = insts.filter { it.installment.isPaid }.sumOf { it.installment.amount },
            perLoan = perLoan
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())

    companion object {
        val currentYearInit: Int = Jalali.todayJalali()[0]
    }

    fun prevYear() { year.value = (year.value - 1).coerceAtLeast(1350) }
    fun nextYear() { year.value = (year.value + 1).coerceAtMost(Jalali.todayJalali()[0] + 1) }
}
