package ir.dastyar.ghest.backup

import ir.dastyar.ghest.data.Check
import ir.dastyar.ghest.data.CheckDirection
import ir.dastyar.ghest.data.CheckStatus
import ir.dastyar.ghest.data.DebtDirection
import ir.dastyar.ghest.data.Installment
import ir.dastyar.ghest.data.Loan
import ir.dastyar.ghest.data.PersonalDebt
import ir.dastyar.ghest.util.Jalali
import org.json.JSONArray
import org.json.JSONObject

/**
 * پشتیبان‌گیری و بازیابی کامل داده‌ها به‌صورت JSON (بدون وابستگی خارجی)
 * و خروجی CSV برای اکسل.
 */
object Backup {

    // ---------------- JSON Export ----------------

    fun exportJson(
        loans: List<Loan>,
        installments: List<Installment>,
        checks: List<Check>,
        debts: List<PersonalDebt>
    ): String {
        val root = JSONObject()
        root.put("app", "DastyarGhest")
        root.put("version", 1)

        val loansArr = JSONArray()
        loans.forEach { l ->
            loansArr.put(
                JSONObject()
                    .put("id", l.id)
                    .put("title", l.title)
                    .put("creditor", l.creditor)
                    .put("type", l.type)
                    .put("installmentAmount", l.installmentAmount)
                    .put("count", l.count)
                    .put("stepDays", l.stepDays)
                    .put("firstDueEpochDay", l.firstDueEpochDay)
                    .put("note", l.note)
                    .put("archived", l.archived)
                    .put("createdAtEpochDay", l.createdAtEpochDay)
            )
        }

        val instArr = JSONArray()
        installments.forEach { i ->
            instArr.put(
                JSONObject()
                    .put("id", i.id)
                    .put("loanId", i.loanId)
                    .put("number", i.number)
                    .put("dueEpochDay", i.dueEpochDay)
                    .put("amount", i.amount)
                    .put("isPaid", i.isPaid)
                    .put("paidEpochDay", i.paidEpochDay ?: JSONObject.NULL)
            )
        }

        val checksArr = JSONArray()
        checks.forEach { c ->
            checksArr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("direction", c.direction)
                    .put("title", c.title)
                    .put("bank", c.bank)
                    .put("number", c.number)
                    .put("sayadId", c.sayadId)
                    .put("amount", c.amount)
                    .put("dueEpochDay", c.dueEpochDay)
                    .put("status", c.status)
                    .put("note", c.note)
            )
        }

        val debtsArr = JSONArray()
        debts.forEach { d ->
            debtsArr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("person", d.person)
                    .put("direction", d.direction)
                    .put("amount", d.amount)
                    .put("dueEpochDay", d.dueEpochDay ?: JSONObject.NULL)
                    .put("note", d.note)
                    .put("isSettled", d.isSettled)
                    .put("settledEpochDay", d.settledEpochDay ?: JSONObject.NULL)
                    .put("createdAtEpochDay", d.createdAtEpochDay)
            )
        }

        root.put("loans", loansArr)
        root.put("installments", instArr)
        root.put("checks", checksArr)
        root.put("debts", debtsArr)
        return root.toString(2)
    }

    data class Parsed(
        val loans: List<Loan>,
        val installments: List<Installment>,
        val checks: List<Check>,
        val debts: List<PersonalDebt>
    )

    fun importJson(text: String): Parsed {
        val root = JSONObject(text)
        require(root.optString("app") == "DastyarGhest") { "فایل پشتیبان معتبر نیست" }

        val loans = mutableListOf<Loan>()
        root.optJSONArray("loans")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                loans.add(
                    Loan(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        creditor = o.optString("creditor"),
                        type = o.optString("type", "OTHER"),
                        installmentAmount = o.getLong("installmentAmount"),
                        count = o.getInt("count"),
                        stepDays = o.optInt("stepDays", 30),
                        firstDueEpochDay = o.getLong("firstDueEpochDay"),
                        note = o.optString("note"),
                        archived = o.optBoolean("archived"),
                        createdAtEpochDay = o.getLong("createdAtEpochDay")
                    )
                )
            }
        }

        val installments = mutableListOf<Installment>()
        root.optJSONArray("installments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                installments.add(
                    Installment(
                        id = o.getLong("id"),
                        loanId = o.getLong("loanId"),
                        number = o.getInt("number"),
                        dueEpochDay = o.getLong("dueEpochDay"),
                        amount = o.getLong("amount"),
                        isPaid = o.optBoolean("isPaid"),
                        paidEpochDay = if (o.isNull("paidEpochDay")) null else o.getLong("paidEpochDay")
                    )
                )
            }
        }

        val checks = mutableListOf<Check>()
        root.optJSONArray("checks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                checks.add(
                    Check(
                        id = o.getLong("id"),
                        direction = o.optString("direction", CheckDirection.PAYABLE),
                        title = o.getString("title"),
                        bank = o.optString("bank"),
                        number = o.optString("number"),
                        sayadId = o.optString("sayadId"),
                        amount = o.getLong("amount"),
                        dueEpochDay = o.getLong("dueEpochDay"),
                        status = o.optString("status", CheckStatus.OPEN),
                        note = o.optString("note")
                    )
                )
            }
        }

        val debts = mutableListOf<PersonalDebt>()
        root.optJSONArray("debts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                debts.add(
                    PersonalDebt(
                        id = o.getLong("id"),
                        person = o.getString("person"),
                        direction = o.optString("direction", DebtDirection.I_OWE),
                        amount = o.getLong("amount"),
                        dueEpochDay = if (o.isNull("dueEpochDay")) null else o.getLong("dueEpochDay"),
                        note = o.optString("note"),
                        isSettled = o.optBoolean("isSettled"),
                        settledEpochDay = if (o.isNull("settledEpochDay")) null else o.getLong("settledEpochDay"),
                        createdAtEpochDay = o.getLong("createdAtEpochDay")
                    )
                )
            }
        }

        return Parsed(loans, installments, checks, debts)
    }

    // ---------------- CSV Export ----------------

    private fun csvCell(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

    private fun dateStr(epochDay: Long?): String =
        if (epochDay == null) "" else Jalali.format(epochDay)

    fun exportCsv(
        loans: List<Loan>,
        installments: List<Installment>,
        checks: List<Check>,
        debts: List<PersonalDebt>
    ): String {
        val sb = StringBuilder()
        sb.append("\uFEFF") // BOM برای نمایش درست فارسی در اکسل
        sb.append("نوع,عنوان/بابت,طرف حساب,مبلغ (تومان),تاریخ سررسید,وضعیت\n")
        val loanById = loans.associateBy { it.id }
        installments.forEach { i ->
            val loan = loanById[i.loanId]
            sb.append(csvCell("قسط")).append(',')
                .append(csvCell(loan?.title ?: "")).append(',')
                .append(csvCell(loan?.creditor ?: "")).append(',')
                .append(i.amount).append(',')
                .append(csvCell(dateStr(i.dueEpochDay))).append(',')
                .append(csvCell(if (i.isPaid) "پرداخت‌شده" else "پرداخت‌نشده"))
                .append('\n')
        }
        checks.forEach { c ->
            sb.append(csvCell("چک ${CheckDirection.label(c.direction)}")).append(',')
                .append(csvCell(c.title)).append(',')
                .append(csvCell(c.bank)).append(',')
                .append(c.amount).append(',')
                .append(csvCell(dateStr(c.dueEpochDay))).append(',')
                .append(csvCell(CheckStatus.label(c.status)))
                .append('\n')
        }
        debts.forEach { d ->
            sb.append(csvCell(DebtDirection.label(d.direction))).append(',')
                .append(csvCell(d.person)).append(',')
                .append(csvCell("")).append(',')
                .append(d.amount).append(',')
                .append(csvCell(dateStr(d.dueEpochDay))).append(',')
                .append(csvCell(if (d.isSettled) "تسویه‌شده" else "باز"))
                .append('\n')
        }
        return sb.toString()
    }
}
