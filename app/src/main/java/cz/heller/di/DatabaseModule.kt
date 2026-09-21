package cz.heller.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.heller.data.db.AccountDao
import cz.heller.data.db.CategoryAliases
import cz.heller.data.db.HellerDatabase
import cz.heller.data.db.CategoryDao
import cz.heller.data.db.DefaultCategories
import cz.heller.data.db.RecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // v5 → v6: plánovaná platba si pamatuje, do kdy je zaplaceno (párování s transakcemi).
    // Nedestruktivní — jen přidá sloupec, existující data (záznamy, platby) zůstanou.
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE planned_payments ADD COLUMN paidThroughEpochDay INTEGER")
        }
    }

    // v6 → v7: příznak podnikatelského účtu (příchozí platby = příjem). Nedestruktivní.
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN isBusiness INTEGER NOT NULL DEFAULT 0")
        }
    }

    // v7 → v8: dedup ID pohybu jen v rámci účtu (interní převod má na obou účtech stejné ID).
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_records_fioTransactionId")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_records_accountId_fioTransactionId " +
                    "ON records (accountId, fioTransactionId)",
            )
        }
    }

    // v8 → v9: signály z banky pro kategorizaci (protiúčet, VS, typ pohybu) a příznak, že kategorii
    // přiřadila automatika. Existující zařazené záznamy zůstávají „potvrzené" (categoryAuto = 0),
    // takže se z nich model rovnou učí. Nedestruktivní.
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE records ADD COLUMN counterAccount TEXT")
            db.execSQL("ALTER TABLE records ADD COLUMN variableSymbol TEXT")
            db.execSQL("ALTER TABLE records ADD COLUMN txType TEXT")
            db.execSQL("ALTER TABLE records ADD COLUMN categoryAuto INTEGER NOT NULL DEFAULT 0")
        }
    }

    // v9 → v10: zjednodušený strom kategorií (13 skupin, 35 kategorií místo 82). Staré přednastavené
    // kategorie se přemapují přes CategoryAliases v záznamech, plánovaných platbách, rozpočtech i
    // u rodičů uživatelských kategorií; pak se staré presety smažou a založí nové. Uživatelské
    // kategorie (isDefault = 0) zůstávají. Nedestruktivní pro data uživatele.
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            for ((old, new) in CategoryAliases.LEGACY) {
                db.execSQL("UPDATE records SET categoryId = ? WHERE categoryId = ?", arrayOf(new, old))
                db.execSQL("UPDATE planned_payments SET categoryId = ? WHERE categoryId = ?", arrayOf(new, old))
                db.execSQL("UPDATE categories SET parentId = ? WHERE parentId = ? AND isDefault = 0", arrayOf(new, old))
            }
            // Rozpočty drží ID skupin jako CSV — přemapuj a odstraň duplicity/neexistující skupiny.
            val groups = DefaultCategories.groupIds()
            db.query("SELECT id, categoryGroupIds FROM budgets").use { c ->
                val updates = ArrayList<Pair<String, String>>()
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val csv = c.getString(1) ?: ""
                    val mapped = csv.split(',').filter { it.isNotBlank() }
                        .map { CategoryAliases.toCurrent(it) }
                        .filter { it in groups }
                        .distinct()
                    updates += id to mapped.joinToString(",")
                }
                for ((id, csv) in updates) db.execSQL("UPDATE budgets SET categoryGroupIds = ? WHERE id = ?", arrayOf(csv, id))
            }
            db.execSQL("DELETE FROM categories WHERE isDefault = 1")
            insertCategories(db, DefaultCategories.all())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HellerDatabase =
        Room.databaseBuilder(context, HellerDatabase::class.java, HellerDatabase.NAME)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedCategories(db)
                }

                // Při dev destruktivní migraci se tabulky znovu vytvoří — naseeduj znovu.
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    super.onDestructiveMigration(db)
                    seedCategories(db)
                }

                // Doplní kategorie přidané po v5 (např. „Práce / Podnikání") i do existující DB.
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    insertCategories(db, DefaultCategories.extras())
                }
            })
            // Produkce: ŽÁDNÁ destruktivní migrace — při změně schématu musí přibýt Migration,
            // jinak Room radši spadne, než aby smazal uživatelská data. Viz addMigrations výše.
            .build()

    private fun seedCategories(db: SupportSQLiteDatabase) = insertCategories(db, DefaultCategories.all())

    private fun insertCategories(db: SupportSQLiteDatabase, categories: List<cz.heller.data.db.CategoryEntity>) {
        categories.forEach { c ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, type, parentId, icon, sortOrder, isDefault) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    c.id, c.name, c.type.name, c.parentId, c.icon, c.sortOrder,
                    if (c.isDefault) 1 else 0,
                ),
            )
        }
    }

    @Provides
    fun provideAccountDao(db: HellerDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: HellerDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideRecordDao(db: HellerDatabase): RecordDao = db.recordDao()

    @Provides
    fun provideBudgetDao(db: HellerDatabase): cz.heller.data.db.BudgetDao = db.budgetDao()

    @Provides
    fun providePlannedPaymentDao(db: HellerDatabase): cz.heller.data.db.PlannedPaymentDao =
        db.plannedPaymentDao()
}
