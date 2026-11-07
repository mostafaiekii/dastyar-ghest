package ir.dastyar.ghest

import android.app.Application
import ir.dastyar.ghest.notify.ReminderWorker

class GhestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.ensureChannel(this)
        ReminderWorker.schedule(this)
    }
}
