package com.portfoliox.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.portfoliox.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {

    /** Emits null until the first trade creates the account row (see PortfolioRepositoryImpl). */
    @Query("SELECT cashBalance FROM account WHERE id = ${AccountEntity.SINGLE_ACCOUNT_ID}")
    fun observeCash(): Flow<BigDecimal?>

    @Query("SELECT cashBalance FROM account WHERE id = ${AccountEntity.SINGLE_ACCOUNT_ID}")
    suspend fun getCash(): BigDecimal?

    @Upsert
    suspend fun upsert(entity: AccountEntity)

    /** With no row, the repository reports the starting cash again. */
    @Query("DELETE FROM account")
    suspend fun deleteAll()
}
