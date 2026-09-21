package cz.heller.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Částky vždy v minor units (haléře) jako Long. Měna je vždy CZK (jednoměnová appka).

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val initialBalanceMinor: Long,
    val icon: String,
    val excludeFromStats: Boolean = false,
    val archived: Boolean = false,
    // Podnikatelský účet: všechny příchozí platby se berou jako příjem (kategorie „Plat, mzda, fakturace").
    val isBusiness: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "categories",
    indices = [Index("parentId"), Index("type")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CategoryType,
    val parentId: String? = null,
    val icon: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
)

@Entity(
    tableName = "records",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("dateTime"),
        // Unikát na (účet, ID pohybu): Fio dává oběma stranám interního převodu stejné ID,
        // takže globální unikát by druhou nohu zahodil. Dedup tak platí jen v rámci účtu.
        Index(value = ["accountId", "fioTransactionId"], unique = true),
    ],
)
data class RecordEntity(
    @PrimaryKey val id: String,
    val type: RecordType,
    val accountId: String,
    val categoryId: String? = null,
    // amountMinor je vždy kladné; směr (příjem/výdaj) určuje [type].
    val amountMinor: Long,
    val dateTime: Long,
    val payee: String? = null,
    val note: String? = null,
    val paymentType: PaymentType? = null,
    val transferAccountId: String? = null,
    val transferRecordId: String? = null,
    // U převodu: true = odchozí noha (minus), false = příchozí (plus); null jinak.
    val transferOut: Boolean? = null,
    val source: RecordSource = RecordSource.MANUAL,
    val fioTransactionId: Long? = null,
    // Signály z banky pro kategorizaci (v9): protiúčet, variabilní symbol, typ pohybu (Fio column8).
    val counterAccount: String? = null,
    val variableSymbol: String? = null,
    val txType: String? = null,
    // true = kategorii přiřadila automatika a uživatel ji nepotvrdil; false = zařazeno/potvrzeno uživatelem.
    val categoryAuto: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "planned_payments")
data class PlannedPaymentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: RecordType, // EXPENSE nebo INCOME (TRANSFER se nepoužívá)
    val accountId: String,
    val categoryId: String? = null,
    val amountMinor: Long,
    // Interval = každých `frequencyCount` jednotek `frequencyUnit`.
    val frequencyUnit: FrequencyUnit,
    val frequencyCount: Int,
    val startEpochDay: Long,        // kotva / první výskyt (jen datum)
    val endEpochDay: Long? = null,  // volitelný konec
    val note: String? = null,
    // Do kterého výskytu (epoch day) je zaplaceno. Splatný = první výskyt po tomto datu.
    val paidThroughEpochDay: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    // ID skupin kategorií (parentId==null). Prázdné = všechny výdaje.
    val categoryGroupIds: List<String>,
    val amountMinor: Long,
    val period: BudgetPeriod = BudgetPeriod.MONTH,
    val notifyThresholdPct: Int = 80,
    val createdAt: Long,
    val updatedAt: Long,
)
