package ir.dastyar.ghest.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** نوع وام/تعهد */
object LoanType {
    const val BANK = "BANK"           // وام بانکی
    const val GHARZ = "GHARZ"         // قرض‌الحسنه
    const val PURCHASE = "PURCHASE"   // خرید اقساطی
    const val INSURANCE = "INSURANCE" // بیمه
    const val RENT = "RENT"           // اجاره
    const val OTHER = "OTHER"         // سایر

    val all = listOf(BANK, GHARZ, PURCHASE, INSURANCE, RENT, OTHER)
    fun label(type: String): String = when (type) {
        BANK -> "وام بانکی"
        GHARZ -> "قرض‌الحسنه"
        PURCHASE -> "خرید اقساطی"
        INSURANCE -> "بیمه"
        RENT -> "اجاره"
        else -> "سایر"
    }
}

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val creditor: String = "",
    val type: String = LoanType.OTHER,
    val installmentAmount: Long,
    val count: Int,
    val stepDays: Int = 30,
    val firstDueEpochDay: Long,
    val note: String = "",
    val archived: Boolean = false,
    val createdAtEpochDay: Long
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId"), Index("dueEpochDay"), Index("isPaid")]
)
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val number: Int,            // شماره قسط از ۱
    val dueEpochDay: Long,
    val amount: Long,
    val isPaid: Boolean = false,
    val paidEpochDay: Long? = null
)

/** جهت چک */
object CheckDirection {
    const val PAYABLE = "PAYABLE"     // پرداختی (چکی که باید پاس کنی)
    const val RECEIVABLE = "RECEIVABLE" // دریافتی (چکی که باید بگیری)

    fun label(d: String): String = if (d == PAYABLE) "پرداختی" else "دریافتی"
}

/** وضعیت چک */
object CheckStatus {
    const val OPEN = "OPEN"
    const val PASSED = "PASSED"
    const val RETURNED = "RETURNED"
    const val CANCELLED = "CANCELLED"

    val all = listOf(OPEN, PASSED, RETURNED, CANCELLED)
    fun label(s: String): String = when (s) {
        OPEN -> "در جریان"
        PASSED -> "پاس‌شده"
        RETURNED -> "برگشتی"
        else -> "باطل‌شده"
    }
}

@Entity(tableName = "checks", indices = [Index("dueEpochDay"), Index("status")])
data class Check(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String = CheckDirection.PAYABLE,
    val title: String,          // بابت
    val bank: String = "",
    val number: String = "",
    val sayadId: String = "",   // شناسه صیادی
    val amount: Long,
    val dueEpochDay: Long,
    val status: String = CheckStatus.OPEN,
    val note: String = ""
)

/** بدهی/طلب شخصی */
object DebtDirection {
    const val I_OWE = "I_OWE"         // بدهکارم
    const val THEY_OWE = "THEY_OWE"   // طلب دارم

    fun label(d: String): String = if (d == I_OWE) "بدهی من" else "طلب من"
}

@Entity(tableName = "debts", indices = [Index("isSettled")])
data class PersonalDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String = DebtDirection.I_OWE,
    val amount: Long,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val settledEpochDay: Long? = null,
    val createdAtEpochDay: Long
)

/** قسط همراه با عنوان وام (برای لیست‌های ترکیبی) */
data class OpenInstallment(
    @androidx.room.Embedded val installment: Installment,
    val loanTitle: String,
    val loanType: String
)
