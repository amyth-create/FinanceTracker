package com.personal.financetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, Category::class, PlannedPayment::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun plannedPaymentDao(): PlannedPaymentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** v1 -> v2: add planned_payments table (existing data untouched). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `planned_payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`categoryId` INTEGER NOT NULL, " +
                        "`categoryName` TEXT NOT NULL, " +
                        "`categoryEmoji` TEXT NOT NULL, " +
                        "`categoryColor` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`plannedDate` INTEGER NOT NULL, " +
                        "`isDone` INTEGER NOT NULL, " +
                        "`doneTransactionId` INTEGER)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker.db"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories on first create
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = INSTANCE ?: return@launch
                            val dao = database.categoryDao()
                            if (dao.getCount() == 0) {
                                dao.insertAll(defaultExpenseCategories)
                                dao.insertAll(defaultIncomeCategories)
                            }
                        }
                    }
                })
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
