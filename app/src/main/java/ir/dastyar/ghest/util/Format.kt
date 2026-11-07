package ir.dastyar.ghest.util

/** ابزار اعداد فارسی، جداکننده هزارگان و فرمت پول */
object Farsi {

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** تبدیل ارقام لاتین به فارسی */
    fun digit(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(if (c in '0'..'9') faDigits[c - '0'] else c)
        }
        return sb.toString()
    }

    fun digit(n: Long): String = digit(n.toString())
    fun digit(n: Int): String = digit(n.toString())

    private fun group(s: String): String {
        if (s.length <= 3) return s
        val sb = StringBuilder()
        val len = s.length
        for (i in s.indices) {
            sb.append(s[i])
            val remaining = len - i - 1
            if (remaining in 1..len && remaining % 3 == 0) sb.append('٬')
        }
        return sb.toString()
    }

    /** عدد با جداکننده هزارگان فارسی: ۱۲٬۵۰۰ */
    fun number(n: Long): String {
        if (n < 0) return "−" + group((-n).toString())
        return group(n.toString())
    }

    fun number(n: Int): String = number(n.toLong())

    /** ۱۲٬۵۰۰ تومان */
    fun money(n: Long): String = number(n) + " تومان"

    /** نسخه کوتاه برای کارت‌ها: ۱٫۲ میلیون تومان */
    fun moneyShort(n: Long): String {
        val abs = if (n < 0) -n else n
        val sign = if (n < 0) "−" else ""
        val txt = when {
            abs >= 1_000_000_000 -> trim1(abs / 100_000_000.0) + " میلیارد"
            abs >= 1_000_000 -> trim1(abs / 100_000.0) + " میلیون"
            abs >= 1_000 -> (abs / 1000).toString() + " هزار"
            else -> abs.toString()
        }
        return sign + digit(txt) + " تومان"
    }

    private fun trim1(x: Double): String {
        val v = (x / 10.0)
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0").replace('.', '٫')
    }

    private fun String.removeSuffix(s: String): String =
        if (this.endsWith(s)) this.substring(0, this.length - s.length) else this

    /** استخراج رقم از ورودی کاربر (فارسی/عربی/لاتین) */
    fun parseNumber(s: String): Long {
        if (s.isBlank()) return 0L
        val sb = StringBuilder()
        for (c in s) {
            when {
                c in '0'..'9' -> sb.append(c)
                c in '۰'..'۹' -> sb.append(c - '۰')
                c in '٠'..'٩' -> sb.append(c - '٠')
                c == '٫' || c == '.' -> {}
                else -> {}
            }
        }
        return sb.toString().toLongOrNull() ?: 0L
    }

    /** ورودی کاربر را به عدد گروه‌بندی‌شده فارسی تبدیل می‌کند */
    fun formatInput(raw: String): String {
        val n = parseNumber(raw)
        return if (n == 0L) "" else number(n)
    }

    fun sha256(text: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((text + "|dastyar-ghest").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
