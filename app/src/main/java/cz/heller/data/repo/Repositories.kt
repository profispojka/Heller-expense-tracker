package cz.heller.data.repo

import android.content.Context
import cz.heller.R
import cz.heller.data.db.AccountDao
import cz.heller.data.db.AccountEntity
import cz.heller.data.db.AccountType
import cz.heller.data.db.CategoryDao
import cz.heller.data.db.CategoryEntity
import cz.heller.data.db.CategoryNames
import cz.heller.data.db.CategoryType
import cz.heller.core.recurring.PlannedMatcher
import cz.heller.core.time.PlannedPayments
import cz.heller.data.db.RecordDao
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun newId(): String = UUID.randomUUID().toString()
private fun now(): Long = System.currentTimeMillis()

@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AccountDao,
) {

    fun observeActive(): Flow<List<AccountEntity>> = dao.observeActive()
    fun observeById(id: String): Flow<AccountEntity?> = dao.observeById(id)
    fun observeNetWorthMinor(): Flow<Long> = dao.observeNetWorthMinor()
    fun observeBalances(): Flow<List<cz.heller.data.db.AccountBalance>> = dao.observeBalances()

    suspend fun create(
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
        icon: String,
        isBusiness: Boolean = false,
    ): String {
        val id = newId()
        val ts = now()
        dao.upsert(
            AccountEntity(
                id = id,
                name = name.trim().ifBlank { context.getString(R.string.account_default_name) },
                type = type,
                initialBalanceMinor = initialBalanceMinor,
                icon = icon,
                isBusiness = isBusiness,
                createdAt = ts,
                updatedAt = ts,
            )
        )
        return id
    }

    suspend fun save(account: AccountEntity) = dao.update(account.copy(updatedAt = now()))
    suspend fun delete(account: AccountEntity) = dao.delete(account)
    suspend fun getById(id: String): AccountEntity? = dao.getById(id)

    suspend fun updateAccount(
        id: String,
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
        isBusiness: Boolean,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                name = name.trim().ifBlank { context.getString(R.string.account_default_name) },
                type = type,
                initialBalanceMinor = initialBalanceMinor,
                isBusiness = isBusiness,
                updatedAt = now(),
            )
        )
    }
}

@Singleton
class CategoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CategoryDao,
) {
    // Názvy přednastavených kategorií se lokalizují podle id (viz [CategoryNames]).
    private fun localize(list: List<CategoryEntity>): List<CategoryEntity> =
        list.map { CategoryNames.localized(context, it) }

    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll().map { localize(it) }
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> = dao.observeByType(type).map { localize(it) }
    suspend fun upsert(category: CategoryEntity) = dao.upsert(category)
    suspend fun getById(id: String): CategoryEntity? =
        dao.getById(id)?.let { CategoryNames.localized(context, it) }

    suspend fun create(name: String, type: CategoryType, parentId: String?, icon: String): String {
        val id = newId()
        dao.upsert(
            CategoryEntity(
                id = id,
                name = name.trim().ifBlank { context.getString(R.string.category_default_name) },
                type = type,
                parentId = parentId,
                icon = icon,
                sortOrder = 100_000, // vlastní kategorie až za přednastavené
                isDefault = false,
            )
        )
        return id
    }

    suspend fun update(id: String, name: String, parentId: String?, icon: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(name = name.trim().ifBlank { context.getString(R.string.category_default_name) }, parentId = parentId, icon = icon))
    }

    /** Smaže kategorii; je-li to skupina, i její podkategorie. */
    suspend fun deleteWithChildren(category: CategoryEntity) {
        dao.deleteChildren(category.id)
        dao.delete(category)
    }
}

@Singleton
class BudgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: cz.heller.data.db.BudgetDao,
) {
    fun observeAll(): Flow<List<cz.heller.data.db.BudgetEntity>> = dao.observeAll()

    suspend fun create(
        name: String,
        categoryGroupIds: List<String>,
        amountMinor: Long,
        period: cz.heller.data.db.BudgetPeriod,
    ) {
        val ts = now()
        dao.upsert(
            cz.heller.data.db.BudgetEntity(
                id = newId(),
                name = name.trim().ifBlank { context.getString(R.string.budget_default_name) },
                categoryGroupIds = categoryGroupIds,
                amountMinor = amountMinor,
                period = period,
                createdAt = ts,
                updatedAt = ts,
            )
        )
    }

    suspend fun delete(budget: cz.heller.data.db.BudgetEntity) = dao.delete(budget)
}

@Singleton
class PlannedPaymentRepository @Inject constructor(
    private val dao: cz.heller.data.db.PlannedPaymentDao,
    private val recordDao: RecordDao,
) {
    fun observeAll(): Flow<List<cz.heller.data.db.PlannedPaymentEntity>> = dao.observeAll()
    fun observeById(id: String): Flow<cz.heller.data.db.PlannedPaymentEntity?> = dao.observeById(id)
    suspend fun getById(id: String): cz.heller.data.db.PlannedPaymentEntity? = dao.getById(id)

    suspend fun update(
        id: String,
        name: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        frequencyUnit: cz.heller.data.db.FrequencyUnit,
        frequencyCount: Int,
        startEpochDay: Long,
        endEpochDay: Long?,
        note: String?,
    ) {
        val existing = dao.getById(id) ?: return
        dao.upsert(
            existing.copy(
                name = name.trim().ifBlank { "Platba" },
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                frequencyUnit = frequencyUnit,
                frequencyCount = frequencyCount,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                note = note?.trim()?.ifBlank { null },
                updatedAt = now(),
            )
        )
    }

    suspend fun create(
        name: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        frequencyUnit: cz.heller.data.db.FrequencyUnit,
        frequencyCount: Int,
        startEpochDay: Long,
        endEpochDay: Long?,
        note: String?,
    ) {
        val ts = now()
        // Starší výskyty (před dneškem) ber jako vyřízené, ať nová platba nehází falešné „po splatnosti".
        val paidThrough = PlannedPayments.lastOccurrenceBefore(
            startEpochDay, frequencyUnit, frequencyCount, endEpochDay,
        )
        dao.upsert(
            cz.heller.data.db.PlannedPaymentEntity(
                id = newId(),
                name = name.trim().ifBlank { "Platba" },
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                frequencyUnit = frequencyUnit,
                frequencyCount = frequencyCount,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                note = note?.trim()?.ifBlank { null },
                paidThroughEpochDay = paidThrough,
                createdAt = ts,
                updatedAt = ts,
            )
        )
    }

    /** Označí splatný výskyt jako zaplacený (ručně napojený na transakci) — zmizí z nadcházejících. */
    suspend fun markPaid(id: String, throughEpochDay: Long) = dao.setPaidThrough(id, throughEpochDay, now())

    /**
     * Projde všechny plánované platby a podle existujících transakcí posune „zaplaceno do"
     * (auto-párování). Volá se po Fio synchronizaci.
     */
    suspend fun reconcileAll() {
        val payments = dao.observeAll().first()
        if (payments.isEmpty()) return
        val records = recordDao.observeAll().first()
        val today = LocalDate.now()
        payments.forEach { p ->
            val newPaid = PlannedMatcher.reconcile(p, records, today)
            if (newPaid != null && newPaid != p.paidThroughEpochDay) {
                dao.setPaidThrough(p.id, newPaid, now())
            }
        }
    }

    suspend fun delete(payment: cz.heller.data.db.PlannedPaymentEntity) = dao.delete(payment)
}

@Singleton
class RecordRepository @Inject constructor(
    private val dao: RecordDao,
    private val categorization: CategorizationRepository,
) {

    fun observeAll(): Flow<List<RecordEntity>> = dao.observeAll()
    fun observeRecent(limit: Int): Flow<List<RecordEntity>> = dao.observeRecent(limit)
    fun observeByAccount(accountId: String): Flow<List<RecordEntity>> = dao.observeByAccount(accountId)
    fun observeById(id: String): Flow<RecordEntity?> = dao.observeById(id)
    suspend fun getById(id: String): RecordEntity? = dao.getById(id)

    /** Příjem nebo výdaj. amountMinor je vždy kladné, směr nese [type]. */
    suspend fun addEntry(
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        dateTime: Long = now(),
        payee: String? = null,
        note: String? = null,
    ) {
        val ts = now()
        dao.upsert(
            RecordEntity(
                id = newId(),
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                dateTime = dateTime,
                payee = payee?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                createdAt = ts,
                updatedAt = ts,
            )
        )
        categorization.learn(payee, categoryId)
    }

    /** Převod mezi účty = dva propojené záznamy (odchozí + příchozí). */
    suspend fun addTransfer(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        dateTime: Long = now(),
        note: String? = null,
    ) {
        val ts = now()
        val outId = newId()
        val inId = newId()
        dao.upsert(
            RecordEntity(
                id = outId, type = RecordType.TRANSFER, accountId = fromAccountId,
                categoryId = null, amountMinor = amountMinor, dateTime = dateTime,
                note = note?.trim()?.ifBlank { null },
                transferAccountId = toAccountId, transferRecordId = inId, transferOut = true,
                createdAt = ts, updatedAt = ts,
            )
        )
        dao.upsert(
            RecordEntity(
                id = inId, type = RecordType.TRANSFER, accountId = toAccountId,
                categoryId = null, amountMinor = amountMinor, dateTime = dateTime,
                note = note?.trim()?.ifBlank { null },
                transferAccountId = fromAccountId, transferRecordId = outId, transferOut = false,
                createdAt = ts, updatedAt = ts,
            )
        )
    }

    /**
     * Úprava příjmu/výdaje (původ zůstává). Změna typu na TRANSFER = označení jako převod mezi
     * vlastními účty: jednostranný převod bez kategorie, směr převezme z původního záznamu
     * (výdaj = odchozí) — zůstatek účtu sedí a do výdajů/příjmů se nepočítá.
     */
    suspend fun updateEntry(
        id: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        dateTime: Long,
        payee: String? = null,
        note: String? = null,
    ) {
        val existing = dao.getById(id) ?: return
        val isTransfer = type == RecordType.TRANSFER
        val transferOut = when {
            !isTransfer -> null
            existing.type == RecordType.TRANSFER -> existing.transferOut
            else -> existing.type == RecordType.EXPENSE
        }
        val category = if (isTransfer) null else categoryId
        dao.update(
            existing.copy(
                type = type,
                accountId = accountId,
                categoryId = category,
                amountMinor = amountMinor,
                dateTime = dateTime,
                payee = payee?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                transferOut = transferOut,
                updatedAt = now(),
            )
        )
        categorization.learn(payee, category)
    }

    suspend fun delete(record: RecordEntity) = dao.delete(record)

    /** Smaže záznam; u převodu i jeho druhou nohu. */
    suspend fun deleteWithPartner(record: RecordEntity) {
        record.transferRecordId?.let { partnerId ->
            dao.getById(partnerId)?.let { dao.delete(it) }
        }
        dao.delete(record)
    }
}
