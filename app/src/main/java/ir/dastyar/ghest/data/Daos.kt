package ir.dastyar.ghest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE archived = 0 ORDER BY createdAtEpochDay DESC")
    fun observeActive(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE archived = 1 ORDER BY createdAtEpochDay DESC")
    fun observeArchived(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeById(id: Long): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): Loan?

    @Query("SELECT * FROM loans")
    suspend fun getAll(): List<Loan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("UPDATE loans SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM loans")
    suspend fun clear()
}

@Dao
interface InstallmentDao {

    /** آمار تجمعی هر وام برای لیست و گزارش */
    data class LoanStat(
        val loanId: Long,
        val total: Int,
        val paidCount: Int,
        val paidAmount: Long,
        val nextDue: Long?
    )

    data class OpenRow(
        @Embedded val installment: Installment,
        val loanTitle: String,
        val loanType: String
    )

    @Query(
        """SELECT i.*, l.title AS loanTitle, l.type AS loanType
           FROM installments i INNER JOIN loans l ON i.loanId = l.id
           WHERE i.isPaid = 0 AND l.archived = 0
           ORDER BY i.dueEpochDay ASC"""
    )
    fun observeOpen(): Flow<List<OpenRow>>

    @Query(
        """SELECT i.*, l.title AS loanTitle, l.type AS loanType
           FROM installments i INNER JOIN loans l ON i.loanId = l.id
           WHERE i.isPaid = 0 AND l.archived = 0
           ORDER BY i.dueEpochDay ASC"""
    )
    suspend fun openOnce(): List<OpenRow>

    @Query(
        """SELECT i.*, l.title AS loanTitle, l.type AS loanType
           FROM installments i INNER JOIN loans l ON i.loanId = l.id
           WHERE l.archived = 0
           ORDER BY i.dueEpochDay ASC"""
    )
    fun observeAllOfActive(): Flow<List<OpenRow>>

    @Query(
        """SELECT i.loanId AS loanId,
           COUNT(i.id) AS total,
           SUM(CASE WHEN i.isPaid = 1 THEN 1 ELSE 0 END) AS paidCount,
           SUM(CASE WHEN i.isPaid = 1 THEN i.amount ELSE 0 END) AS paidAmount,
           MIN(CASE WHEN i.isPaid = 0 THEN i.dueEpochDay END) AS nextDue
           FROM installments i INNER JOIN loans l ON i.loanId = l.id
           GROUP BY i.loanId"""
    )
    fun observeStats(): Flow<List<LoanStat>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY number ASC")
    fun observeByLoan(loanId: Long): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY number ASC")
    suspend fun getByLoan(loanId: Long): List<Installment>

    @Query("SELECT * FROM installments")
    suspend fun getAll(): List<Installment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Installment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Installment): Long

    @Update
    suspend fun update(item: Installment)

    @Delete
    suspend fun delete(item: Installment)

    @Query("UPDATE installments SET isPaid = :paid, paidEpochDay = :paidDay WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean, paidDay: Long?)

    @Query("UPDATE installments SET isPaid = 1, paidEpochDay = :paidDay WHERE isPaid = 0 AND dueEpochDay <= :paidDay AND loanId = :loanId")
    suspend fun payUntil(loanId: Long, paidDay: Long)

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteByLoan(loanId: Long)

    @Query("DELETE FROM installments")
    suspend fun clear()
}

@Dao
interface CheckDao {
    @Query("SELECT * FROM checks ORDER BY dueEpochDay ASC")
    fun observeAll(): Flow<List<Check>>

    @Query("SELECT * FROM checks ORDER BY dueEpochDay ASC")
    suspend fun getAll(): List<Check>

    @Query("SELECT * FROM checks WHERE id = :id")
    suspend fun getById(id: Long): Check?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Check): Long

    @Update
    suspend fun update(item: Check)

    @Delete
    suspend fun delete(item: Check)

    @Query("UPDATE checks SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("DELETE FROM checks")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE isSettled = 0 ORDER BY createdAtEpochDay DESC")
    fun observeOpen(): Flow<List<PersonalDebt>>

    @Query("SELECT * FROM debts WHERE isSettled = 1 ORDER BY settledEpochDay DESC")
    fun observeSettled(): Flow<List<PersonalDebt>>

    @Query("SELECT * FROM debts")
    suspend fun getAll(): List<PersonalDebt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PersonalDebt): Long

    @Update
    suspend fun update(item: PersonalDebt)

    @Delete
    suspend fun delete(item: PersonalDebt)

    @Query("UPDATE debts SET isSettled = :settled, settledEpochDay = :day WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean, day: Long?)

    @Query("DELETE FROM debts")
    suspend fun clear()
}
