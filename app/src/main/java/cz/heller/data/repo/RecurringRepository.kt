package cz.heller.data.repo

import android.content.Context
import cz.heller.R
import cz.heller.core.categorize.MerchantText
import cz.heller.core.recurring.RecurringDetector
import cz.heller.data.db.FrequencyUnit
import cz.heller.data.db.RecordDao
import cz.heller.data.db.RecordSource
import cz.heller.data.db.RecordType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Detekce opakovaných plateb z Fio historie → návrhy na plánované platby. */
@Singleton
class RecurringRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordDao: RecordDao,
    private val categorization: CategorizationRepository,
    private val planned: PlannedPaymentRepository,
) {
    /** Nové (ještě nezaložené) kandidáty na opakovanou platbu pro daný účet. */
    suspend fun detectNew(accountId: String?): List<RecurringDetector.Candidate> {
        if (accountId == null) return emptyList()
        val recs = recordDao.observeByAccount(accountId).first().filter { it.source == RecordSource.FIO }
        val model = categorization.model()
        val existing = planned.observeAll().first()
            .map { dedupKey(it.accountId, it.amountMinor, it.name) }.toSet()
        return RecurringDetector.detect(recs, model)
            .filter { dedupKey(accountId, it.amountMinor, it.name) !in existing }
    }

    /**
     * Založí vybrané kandidáty jako měsíční plánované platby; vrátí počet **skutečně přidaných**.
     * Idempotentní — kandidáta, který už mezi plánovanými platbami je, přeskočí (žádné duplicity).
     */
    suspend fun addAsPlanned(accountId: String, candidates: List<RecurringDetector.Candidate>): Int {
        val existing = planned.observeAll().first()
            .map { dedupKey(it.accountId, it.amountMinor, it.name) }.toMutableSet()
        var added = 0
        candidates.forEach { c ->
            val k = dedupKey(accountId, c.amountMinor, c.name)
            if (k in existing) return@forEach
            existing += k
            planned.create(
                name = c.name.trim().ifBlank { context.getString(R.string.record_default_payee) },
                type = RecordType.EXPENSE,
                accountId = accountId,
                categoryId = c.categoryId,
                amountMinor = c.amountMinor,
                frequencyUnit = FrequencyUnit.MONTH,
                frequencyCount = 1,
                startEpochDay = c.nextStartEpochDay,
                endEpochDay = null,
                note = context.getString(R.string.recurring_note_from_fio),
            )
            added++
        }
        return added
    }

    private fun dedupKey(accountId: String?, amountMinor: Long, name: String?): String =
        "$accountId|$amountMinor|${MerchantText.key(name)}"
}
