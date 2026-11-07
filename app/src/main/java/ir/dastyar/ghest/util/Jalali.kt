package ir.dastyar.ghest.util

import java.util.Calendar
import kotlin.math.abs

/**
 * تبدیل تقویم شمسی <-> روز شمسی (epochDay)
 * الگوریتم jalaali (پورت دقیق و تست‌شده روی ۹ تاریخ مرجع + گردش ۱۰۰ سال).
 * تاریخ‌ها در دیتابیس به‌صورت Long epochDay ذخیره می‌شوند تا مقایسه و مرتب‌سازی دقیق باشد.
 */
object Jalali {

    const val JDN_EPOCH_OFFSET = 2440588

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    // epochDay=0 یعنی 1970/01/01 = پنجشنبه
    private val WEEK_DAYS = listOf(
        "پنجشنبه", "جمعه", "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه"
    )

    private fun div(a: Int, b: Int): Int = a / b // همان برش به سمت صفرِ JS

    private fun mod(a: Int, b: Int): Int = a - div(a, b) * b

    /** خروجی: [leap, gy, march] */
    private fun jalCal(jy: Int): IntArray {
        var jp = BREAKS[0]
        var leapJ = -14
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val gy = jy + 621
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        var nn = n
        if (jump - nn < 6) nn = nn - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(nn + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return intArrayOf(leap, gy, march)
    }

    fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    /** خروجی: [gy, gm, gd] */
    fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return intArrayOf(gy, gm, gd)
    }

    /** شمسی به epochDay */
    fun j2d(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        return (g2d(r[1], 3, r[2]) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1 - JDN_EPOCH_OFFSET).toLong()
    }

    /** epochDay به شمسی: [jy, jm, jd] */
    fun toJalali(epochDay: Long): IntArray {
        val jdn = epochDay.toInt() + JDN_EPOCH_OFFSET
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(r[1], 3, r[2])
        var k = jdn - jdn1f
        var jm: Int
        var jd: Int
        if (k >= 0) {
            if (k <= 185) {
                jm = 1 + div(k, 31)
                jd = mod(k, 31) + 1
                return intArrayOf(jy, jm, jd)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r[0] == 1) k += 1
        }
        jm = 7 + div(k, 30)
        jd = mod(k, 30) + 1
        return intArrayOf(jy, jm, jd)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy)[0] == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun todayEpochDay(): Long {
        val c = Calendar.getInstance()
        return (g2d(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)) - JDN_EPOCH_OFFSET).toLong()
    }

    /** [jy, jm, jd] امروز */
    fun todayJalali(): IntArray = toJalali(todayEpochDay())

    fun weekdayName(epochDay: Long): String =
        WEEK_DAYS[(((epochDay % 7) + 7) % 7).toInt()]

    /** مثال: ۱۶ آبان ۱۴۰۵ */
    fun format(epochDay: Long): String {
        val j = toJalali(epochDay)
        return "${Farsi.digit(j[2])} ${MONTH_NAMES[j[1] - 1]} ${Farsi.digit(j[0])}"
    }

    /** مثال: شنبه ۱۶ آبان */
    fun formatWithWeekday(epochDay: Long): String {
        val j = toJalali(epochDay)
        return "${weekdayName(epochDay)} ${Farsi.digit(j[2])} ${MONTH_NAMES[j[1] - 1]}"
    }

    /** اختلاف سال-ماه برای محور گزارش */
    fun jalaliMonthIndex(epochDay: Long, baseYear: Int): Int {
        val j = toJalali(epochDay)
        return (j[0] - baseYear) * 12 + (j[1] - 1)
    }

    fun daysInCurrentMonth(jy: Int, jm: Int): Int = monthLength(jy, jm)

    fun absDays(diff: Long): Long = abs(diff)
}
