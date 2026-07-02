package cz.heller.core.time

import android.content.Context
import cz.heller.R
import cz.heller.data.db.FrequencyUnit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Výpočty výskytů opakovaných (plánovaných) plateb. Datum-only (epoch day). */
object PlannedPayments {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d. M. yyyy", Locale.getDefault())

    fun frequencyLabel(context: Context, unit: FrequencyUnit, count: Int): String = when {
        unit == FrequencyUnit.DAY && count == 1 -> context.getString(R.string.freq_daily)
        unit == FrequencyUnit.WEEK && count == 1 -> context.getString(R.string.freq_weekly)
        unit == FrequencyUnit.MONTH && count == 1 -> context.getString(R.string.freq_monthly)
        unit == FrequencyUnit.MONTH && count == 3 -> context.getString(R.string.freq_quarterly)
        unit == FrequencyUnit.MONTH && count == 4 -> context.getString(R.string.freq_three_per_year)
        unit == FrequencyUnit.MONTH && count == 6 -> context.getString(R.string.freq_semiannual)
        unit == FrequencyUnit.YEAR && count == 1 -> context.getString(R.string.freq_yearly)
        else -> when (unit) {
            FrequencyUnit.DAY -> context.getString(R.string.freq_every_n_days, count)
            FrequencyUnit.WEEK -> context.getString(R.string.freq_every_n_weeks, count)
            FrequencyUnit.MONTH -> context.getString(R.string.freq_every_n_months, count)
            FrequencyUnit.YEAR -> context.getString(R.string.freq_every_n_years, count)
        }
    }

    private fun step(date: LocalDate, unit: FrequencyUnit, count: Int): LocalDate = when (unit) {
        FrequencyUnit.DAY -> date.plusDays(count.toLong())
        FrequencyUnit.WEEK -> date.plusWeeks(count.toLong())
        FrequencyUnit.MONTH -> date.plusMonths(count.toLong())
        FrequencyUnit.YEAR -> date.plusYears(count.toLong())
    }

    /** První výskyt v den [from] nebo později (respektuje konec). null = už neproběhne. */
    fun nextOccurrence(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        from: LocalDate = LocalDate.now(),
    ): LocalDate? {
        var d = LocalDate.ofEpochDay(startEpochDay)
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        if (d.isBefore(from)) d = fastForward(d, unit, count, from)
        // doladění (between u WEEK/MONTH/YEAR může podstřelit o jeden interval)
        var guard = 0
        while (d.isBefore(from) && guard < 1000) { d = step(d, unit, count); guard++ }
        if (end != null && d.isAfter(end)) return null
        return d
    }

    /**
     * Splatný (= první **nezaplacený**) výskyt: první výskyt po dni [paidThroughEpochDay].
     * Pokud nic není zaplaceno, je to první výskyt platby. Může padnout i do minulosti
     * (pak je „po splatnosti"). null = už nikdy (po konci).
     */
    fun dueOccurrence(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        paidThroughEpochDay: Long?,
    ): LocalDate? {
        val from = paidThroughEpochDay?.let { LocalDate.ofEpochDay(it + 1) } ?: LocalDate.ofEpochDay(startEpochDay)
        return nextOccurrence(startEpochDay, unit, count, endEpochDay, from)
    }

    /**
     * Poslední výskyt **striktně před** dnem [today] — pro inicializaci „zaplaceno do" u nově
     * založené platby (aby se staré výskyty nebraly jako po splatnosti). null = první výskyt je
     * dnes nebo později (nic zaplaceno).
     */
    fun lastOccurrenceBefore(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        today: LocalDate = LocalDate.now(),
    ): Long? {
        val start = LocalDate.ofEpochDay(startEpochDay)
        if (!start.isBefore(today)) return null
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        var d = start
        var last = start
        var guard = 0
        while (guard++ < 100_000) {
            val n = step(d, unit, count)
            if (!n.isBefore(today)) break
            if (end != null && n.isAfter(end)) break
            last = n
            d = n
        }
        return last.toEpochDay()
    }

    /** Počet výskytů platby v kalendářním měsíci [ym]. */
    fun occurrencesInMonth(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        ym: YearMonth,
    ): Int {
        val rangeStart = ym.atDay(1)
        val rangeEnd = ym.plusMonths(1).atDay(1) // exkluzivně
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        var d = LocalDate.ofEpochDay(startEpochDay)
        if (d.isBefore(rangeStart)) d = fastForward(d, unit, count, rangeStart)
        var occ = 0
        var guard = 0
        while (d.isBefore(rangeEnd) && guard < 5000) {
            if (!d.isBefore(rangeStart) && (end == null || !d.isAfter(end))) occ++
            d = step(d, unit, count)
            guard++
        }
        return occ
    }

    /** Konkrétní data všech výskytů platby v kalendářním měsíci [ym] (vzestupně). */
    fun occurrenceDatesInMonth(
        startEpochDay: Long,
        unit: FrequencyUnit,
        count: Int,
        endEpochDay: Long?,
        ym: YearMonth,
    ): List<LocalDate> {
        val rangeStart = ym.atDay(1)
        val rangeEnd = ym.plusMonths(1).atDay(1) // exkluzivně
        val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
        var d = LocalDate.ofEpochDay(startEpochDay)
        if (d.isBefore(rangeStart)) d = fastForward(d, unit, count, rangeStart)
        val out = mutableListOf<LocalDate>()
        var guard = 0
        while (d.isBefore(rangeEnd) && guard < 5000) {
            if (!d.isBefore(rangeStart) && (end == null || !d.isAfter(end))) out += d
            d = step(d, unit, count)
            guard++
        }
        return out
    }

    fun formatDate(date: LocalDate): String = dateFormatter.format(date)

    /** Skok dopředu k prvnímu výskytu >= [target] (bez kroku po jednom). */
    private fun fastForward(start: LocalDate, unit: FrequencyUnit, count: Int, target: LocalDate): LocalDate {
        val between = when (unit) {
            FrequencyUnit.DAY -> ChronoUnit.DAYS.between(start, target)
            FrequencyUnit.WEEK -> ChronoUnit.WEEKS.between(start, target)
            FrequencyUnit.MONTH -> ChronoUnit.MONTHS.between(start, target)
            FrequencyUnit.YEAR -> ChronoUnit.YEARS.between(start, target)
        }
        if (between <= 0) return start
        val steps = between / count // může podstřelit; doladí volající smyčka
        return when (unit) {
            FrequencyUnit.DAY -> start.plusDays(steps * count)
            FrequencyUnit.WEEK -> start.plusWeeks(steps * count)
            FrequencyUnit.MONTH -> start.plusMonths(steps * count)
            FrequencyUnit.YEAR -> start.plusYears(steps * count)
        }
    }
}
