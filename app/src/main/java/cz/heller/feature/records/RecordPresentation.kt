package cz.heller.feature.records

import android.content.Context
import cz.heller.R
import cz.heller.data.db.AccountEntity
import cz.heller.data.db.CategoryEntity
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordType

/** Tip kategorie pro nezařazený záznam (chip na jeden tap). */
data class CategorySuggestion(val id: String, val name: String)

/** Připravený řádek záznamu pro UI. amountMinor je znaménkové (příjem +, výdaj −). */
data class RecordRowUi(
    val id: String,
    val iconKey: String,
    val title: String,
    val subtitle: String?,
    val amountMinor: Long,
    /** Tipy pro nezařazený příjem/výdaj; prázdné u zařazených a převodů. */
    val suggestions: List<CategorySuggestion> = emptyList(),
)

fun RecordEntity.toRowUi(
    categories: Map<String, CategoryEntity>,
    accounts: Map<String, AccountEntity>,
    context: Context,
    suggestions: List<CategorySuggestion> = emptyList(),
): RecordRowUi {
    val category = categoryId?.let { categories[it] }
    val accountName = accounts[accountId]?.name
    val noCategory = context.getString(R.string.no_category)
    // Automaticky přiřazená (nepotvrzená) kategorie je označená, ať jde na první pohled zkontrolovat.
    val categoryLabel = category?.name?.let { name ->
        if (categoryAuto) "$name · ${context.getString(R.string.category_auto)}" else name
    }
    return when (type) {
        RecordType.EXPENSE -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "more_horiz",
            // Titulek = obchodník; pod ním kategorie · účet. U ručních (bez plátce) titulek = kategorie.
            title = payee ?: category?.name ?: noCategory,
            subtitle = if (payee != null) {
                listOfNotNull(categoryLabel ?: noCategory, accountName).joinToString(" · ")
            } else {
                note ?: accountName
            },
            amountMinor = -amountMinor,
            suggestions = if (categoryId == null) suggestions else emptyList(),
        )
        RecordType.INCOME -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "payments",
            title = payee ?: category?.name ?: context.getString(R.string.type_income),
            subtitle = if (payee != null) {
                listOfNotNull(categoryLabel ?: noCategory, accountName).joinToString(" · ")
            } else {
                note ?: accountName
            },
            amountMinor = amountMinor,
            suggestions = if (categoryId == null) suggestions else emptyList(),
        )
        RecordType.TRANSFER -> {
            val here = accounts[accountId]?.name ?: "?"
            // Fio import je jednostranný (bez protiúčtu) — ukaž plátce / poznámku.
            val subtitle = if (transferAccountId == null) {
                payee ?: note ?: here
            } else {
                val there = accounts[transferAccountId]?.name ?: "?"
                if (transferOut == true) "$here → $there" else "$there → $here"
            }
            RecordRowUi(
                id = id,
                iconKey = "more_horiz",
                title = context.getString(R.string.record_transfer),
                subtitle = subtitle,
                amountMinor = if (transferOut == true) -amountMinor else amountMinor,
            )
        }
    }
}
