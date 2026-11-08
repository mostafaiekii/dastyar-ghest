package ir.dastyar.ghest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Loan::class, Installment::class, Check::class, PersonalDebt::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun checkDao(): CheckDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** نسخه ۲: افزودن ستون عکس مدرک به وام‌ها (دور ۲) */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN photoUri TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dastyar_ghest.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
