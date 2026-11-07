package ir.dastyar.ghest.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.dastyar.ghest.backup.Backup
import ir.dastyar.ghest.data.AppDatabase
import ir.dastyar.ghest.ui.theme.ThemeMode
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "ghest_settings")

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = app.settingsStore
    private val db = AppDatabase.getInstance(app)

    private val KEY_THEME = intPreferencesKey("theme_mode")
    private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")

    val themeMode: StateFlow<ThemeMode> = store.data.map { p ->
        when (p[KEY_THEME] ?: 0) {
            1 -> ThemeMode.LIGHT
            2 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val pinEnabled: StateFlow<Boolean> = store.data.map { p -> p[KEY_PIN_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pinHash: StateFlow<String> = store.data.map { p -> p[KEY_PIN_HASH] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** پیام‌های وضعیت برای Snackbar */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            store.edit { it[KEY_THEME] = when (mode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            } }
        }
    }

    fun verifyPin(pin: String): Boolean =
        Farsi.sha256(pin) == pinHash.value && pinHash.value.isNotEmpty()

    fun setPin(pin: String) {
        viewModelScope.launch {
            store.edit {
                it[KEY_PIN_HASH] = Farsi.sha256(pin)
                it[KEY_PIN_ENABLED] = true
            }
            messages.emit("قفل رمز فعال شد")
        }
    }

    fun removePin() {
        viewModelScope.launch {
            store.edit {
                it[KEY_PIN_ENABLED] = false
                it.remove(KEY_PIN_HASH)
            }
            messages.emit("قفل رمز برداشته شد")
        }
    }

    /* ---------------- پشتیبان‌گیری ---------------- */

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val loans = db.loanDao().getAll()
                val insts = db.installmentDao().getAll()
                val checks = db.checkDao().getAll()
                val debts = db.debtDao().getAll()
                val json = Backup.exportJson(loans, insts, checks, debts)
                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                messages.emit("پشتیبان ذخیره شد")
            } catch (e: Exception) {
                messages.emit("خطا در ذخیره پشتیبان: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val text = ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch
                val parsed = Backup.importJson(text)
                db.withTransaction {
                    db.loanDao().clear()
                    db.installmentDao().clear()
                    db.checkDao().clear()
                    db.debtDao().clear()
                    parsed.loans.forEach { db.loanDao().insert(it) }
                    // [فیکس دور ۱] اقساطی که وامشان در فایل پشتیبان نیست نادیده گرفته می‌شوند؛
                    // قبلاً یک قسط بی‌صاحب، کل تراکنش بازیابی را با خطای کلید خارجی می‌شکست.
                    val loanIds = parsed.loans.map { it.id }.toSet()
                    val orphans = parsed.installments.count { it.loanId !in loanIds }
                    db.installmentDao().insertAll(parsed.installments.filter { it.loanId in loanIds })
                    parsed.checks.forEach { db.checkDao().insert(it) }
                    parsed.debts.forEach { db.debtDao().insert(it) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "بازیابی با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                }
                messages.emit("بازیابی انجام شد")
            } catch (e: Exception) {
                messages.emit("فایل معتبر نیست: ${e.message}")
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val csv = Backup.exportCsv(
                    db.loanDao().getAll(), db.installmentDao().getAll(),
                    db.checkDao().getAll(), db.debtDao().getAll()
                )
                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csv.toByteArray(Charsets.UTF_8))
                }
                messages.emit("خروجی CSV ذخیره شد")
            } catch (e: Exception) {
                messages.emit("خطا در خروجی CSV: ${e.message}")
            }
        }
    }

    fun todayText(): String = Jalali.format(Jalali.todayEpochDay())
}
