package cz.heller.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Zůstatek jednoho účtu (počáteční ± jeho záznamy vč. převodů). */
data class AccountBalance(val accountId: String, val balanceMinor: Long)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT a.id AS accountId,
          a.initialBalanceMinor + IFNULL(SUM(CASE
            WHEN r.type = 'INCOME' THEN r.amountMinor
            WHEN r.type = 'EXPENSE' THEN -r.amountMinor
            WHEN r.type = 'TRANSFER' AND r.transferOut = 0 THEN r.amountMinor
            WHEN r.type = 'TRANSFER' AND r.transferOut = 1 THEN -r.amountMinor
            ELSE 0 END), 0) AS balanceMinor
        FROM accounts a
        LEFT JOIN records r ON r.accountId = a.id
        WHERE a.archived = 0
        GROUP BY a.id
        """
    )
    fun observeBalances(): Flow<List<AccountBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    /** Čisté jmění = počáteční zůstatky nevyloučených účtů ± jejich záznamy. */
    @Query(
        """
        SELECT
          (SELECT IFNULL(SUM(initialBalanceMinor), 0) FROM accounts WHERE excludeFromStats = 0 AND archived = 0)
          + (SELECT IFNULL(SUM(CASE WHEN type = 'INCOME' THEN amountMinor
                                    WHEN type = 'EXPENSE' THEN -amountMinor
                                    WHEN type = 'TRANSFER' AND transferOut = 0 THEN amountMinor
                                    WHEN type = 'TRANSFER' AND transferOut = 1 THEN -amountMinor
                                    ELSE 0 END), 0)
             FROM records
             WHERE accountId IN (SELECT id FROM accounts WHERE excludeFromStats = 0 AND archived = 0))
        """
    )
    fun observeNetWorthMinor(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    /** Nastaví počáteční zůstatek (po Fio syncu dopočítaný, ať zobrazený = skutečný zůstatek z banky). */
    @Query("UPDATE accounts SET initialBalanceMinor = :initial, updatedAt = :ts WHERE id = :id")
    suspend fun setInitialBalance(id: String, initial: Long, ts: Long)

    @Delete
    suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, name")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT id FROM categories")
    suspend fun getAllIds(): List<String>

    @Query("SELECT id FROM categories WHERE type = 'INCOME'")
    suspend fun getIncomeIds(): List<String>

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE parentId = :parentId")
    suspend fun deleteChildren(parentId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface PlannedPaymentDao {
    @Query("SELECT * FROM planned_payments ORDER BY startEpochDay")
    fun observeAll(): Flow<List<PlannedPaymentEntity>>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    fun observeById(id: String): Flow<PlannedPaymentEntity?>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    suspend fun getById(id: String): PlannedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PlannedPaymentEntity)

    /** Posune „zaplaceno do" (epoch day) — výskyt(y) do tohoto data zmizí z nadcházejících. */
    @Query("UPDATE planned_payments SET paidThroughEpochDay = :throughEpochDay, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPaidThrough(id: String, throughEpochDay: Long?, updatedAt: Long)

    @Delete
    suspend fun delete(payment: PlannedPaymentEntity)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY dateTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE accountId = :accountId ORDER BY dateTime DESC")
    fun observeByAccount(accountId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id")
    fun observeById(id: String): Flow<RecordEntity?>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: String): RecordEntity?

    /** Nezařazené příjmy/výdaje (bez kategorie) — pro dávkovou kategorizaci. */
    @Query("SELECT * FROM records WHERE categoryId IS NULL AND type IN ('EXPENSE', 'INCOME')")
    suspend fun getUncategorized(): List<RecordEntity>

    /** Trénovací data kategorizace: záznamy zařazené/potvrzené uživatelem, chronologicky. */
    @Query(
        "SELECT * FROM records WHERE categoryId IS NOT NULL AND categoryAuto = 0 " +
            "AND type IN ('EXPENSE', 'INCOME') ORDER BY dateTime ASC",
    )
    suspend fun getConfirmedCategorized(): List<RecordEntity>

    @Query("UPDATE records SET categoryId = :categoryId, categoryAuto = :auto, updatedAt = :ts WHERE id = :id")
    suspend fun setCategory(id: String, categoryId: String?, auto: Boolean, ts: Long)

    /** Doplní bankovní signály (v9) k dříve importovaným Fio záznamům, které je ještě nemají. */
    @Query(
        "UPDATE records SET counterAccount = :counterAccount, variableSymbol = :variableSymbol, txType = :txType " +
            "WHERE accountId = :accountId AND fioTransactionId = :fioTransactionId " +
            "AND counterAccount IS NULL AND variableSymbol IS NULL AND txType IS NULL",
    )
    suspend fun backfillFioSignals(
        accountId: String,
        fioTransactionId: Long,
        counterAccount: String?,
        variableSymbol: String?,
        txType: String?,
    )

    /** Idempotentní vložení Fio záznamu (dedup přes unikátní fioTransactionId). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: RecordEntity): Long

    /** Znaménkový součet záznamů účtu (stejná logika jako balance dotaz) — pro dopočet zůstatku z Fia. */
    @Query(
        """
        SELECT IFNULL(SUM(CASE
          WHEN type = 'INCOME' THEN amountMinor
          WHEN type = 'EXPENSE' THEN -amountMinor
          WHEN type = 'TRANSFER' AND transferOut = 0 THEN amountMinor
          WHEN type = 'TRANSFER' AND transferOut = 1 THEN -amountMinor
          ELSE 0 END), 0)
        FROM records WHERE accountId = :accountId
        """
    )
    suspend fun signedSumForAccount(accountId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RecordEntity)

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)
}
