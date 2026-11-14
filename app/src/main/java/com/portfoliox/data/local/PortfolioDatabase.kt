package com.portfoliox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.portfoliox.data.local.dao.AccountDao
import com.portfoliox.data.local.dao.HoldingDao
import com.portfoliox.data.local.dao.TransactionDao
import com.portfoliox.data.local.dao.WatchlistDao
import com.portfoliox.data.local.entity.AccountEntity
import com.portfoliox.data.local.entity.HoldingEntity
import com.portfoliox.data.local.entity.TransactionEntity
import com.portfoliox.data.local.entity.WatchlistEntity

/**
 * The Room database: lists every table and hands out the DAOs.
 * Room generates the implementing class (PortfolioDatabase_Impl) at compile time.
 *
 * `version` must increase whenever the schema changes, together with a Migration that
 * upgrades existing users' data. Schema export is off while the schema is still evolving.
 */
@Database(
    entities = [
        AccountEntity::class,
        WatchlistEntity::class,
        HoldingEntity::class,
        TransactionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PortfolioDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun holdingDao(): HoldingDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "portfoliox.db"

        /**
         * Opening a database is expensive, so the app creates exactly one instance
         * (in AppContainer) and shares it. applicationContext avoids leaking an Activity.
         */
        fun create(context: Context): PortfolioDatabase =
            Room.databaseBuilder<PortfolioDatabase>(context.applicationContext, DATABASE_NAME).build()
    }
}
