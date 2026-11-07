package ir.dastyar.ghest.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.dastyar.ghest.MainActivity
import ir.dastyar.ghest.R
import ir.dastyar.ghest.data.AppDatabase
import ir.dastyar.ghest.data.CheckStatus
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * کارگر روزانه: هر روز ساعت ۸ صبح بررسی می‌کند اقساط و چک‌های ۳ روز آینده (و عقب‌افتاده‌ها)
 * را پیدا کرده و یک نوتیفیکیشن جمع‌بندی نشان می‌دهد.
 */
class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val db = AppDatabase.getInstance(ctx)

        val today = Jalali.todayEpochDay()
        val openInstallments = db.installmentDao().openOnce()
        val openChecks = db.checkDao().getAll().filter { it.status == CheckStatus.OPEN }
        // [فیکس دور ۱] بدهی‌های شخصی با موعد مشخص هم یادآوری می‌شوند؛
        // قبلاً یادآور فقط اقساط و چک‌ها را پوشش می‌داد.
        val openDebts = db.debtDao().getAll().filter { !it.isSettled && it.dueEpochDay != null }

        val dueInstallments = openInstallments.filter {
            val diff = it.installment.dueEpochDay - today
            diff in 0..3
        }
        val overdueInstallments = openInstallments.filter { it.installment.dueEpochDay < today }
        val dueChecks = openChecks.filter { (it.dueEpochDay - today) in 0..3 }
        val overdueChecks = openChecks.filter { it.dueEpochDay < today }
        val dueDebts = openDebts.filter { (it.dueEpochDay!! - today) in 0..3 }
        val overdueDebts = openDebts.filter { it.dueEpochDay!! < today }

        if (dueInstallments.isEmpty() && dueChecks.isEmpty() && dueDebts.isEmpty() &&
            overdueInstallments.isEmpty() && overdueChecks.isEmpty() && overdueDebts.isEmpty()
        ) return Result.success()

        val parts = mutableListOf<String>()
        if (dueInstallments.isNotEmpty())
            parts.add("${Farsi.digit(dueInstallments.size)} قسط نزدیک")
        if (dueChecks.isNotEmpty())
            parts.add("${Farsi.digit(dueChecks.size)} چک نزدیک")
        if (dueDebts.isNotEmpty())
            parts.add("${Farsi.digit(dueDebts.size)} بدهی نزدیک")
        if (overdueInstallments.isNotEmpty() || overdueChecks.isNotEmpty() || overdueDebts.isNotEmpty())
            parts.add("موارد عقب‌افتاده")

        val text = parts.joinToString(" • ") + " — برای جزئیات باز کن"

        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        ensureChannel(ctx)

        val intent = Intent(ctx, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآور دست‌یار قسط")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(ctx).notify(NOTIFY_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val NOTIFY_ID = 1001
        private const val WORK_NAME = "daily_reminder"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "یادآور سررسیدها",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "یادآوری اقساط و چک‌های نزدیک"
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = now.clone() as Calendar
            next.set(Calendar.HOUR_OF_DAY, 8)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)
            if (next.timeInMillis <= now.timeInMillis) next.add(Calendar.DAY_OF_YEAR, 1)
            val delay = next.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
