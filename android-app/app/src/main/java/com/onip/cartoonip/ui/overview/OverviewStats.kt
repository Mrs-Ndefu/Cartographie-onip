package com.onip.cartoonip.ui.overview

import com.onip.cartoonip.data.model.CapturedHousehold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DailyCount(val label: String, val count: Int)

data class OverviewStats(
    val total: Int,
    val today: Int,
    val thisMonth: Int,
    val thisYear: Int,
    val male: Int,
    val female: Int,
    val dailyHistory: List<DailyCount>,
)

private val DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM")

/** Calculé localement à partir des ménages capturés sur l'appareil — pas d'appel serveur. */
fun computeOverviewStats(households: List<CapturedHousehold>, daysBack: Int = 14): OverviewStats {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val dayBuckets = LinkedHashMap<LocalDate, Int>()
    for (i in (daysBack - 1) downTo 0) {
        dayBuckets[today.minusDays(i.toLong())] = 0
    }

    var todayCount = 0
    var monthCount = 0
    var yearCount = 0
    var male = 0
    var female = 0

    households.forEach { h ->
        val createdDate = runCatching { Instant.parse(h.createdAt).atZone(zone).toLocalDate() }.getOrNull()
        if (createdDate != null) {
            if (createdDate == today) todayCount++
            if (createdDate.year == today.year && createdDate.month == today.month) monthCount++
            if (createdDate.year == today.year) yearCount++
            if (dayBuckets.containsKey(createdDate)) {
                dayBuckets[createdDate] = (dayBuckets[createdDate] ?: 0) + 1
            }
        }

        when (h.chefSexe) {
            "M" -> male++
            "F" -> female++
        }
        h.membres.forEach { m ->
            when (m.sexe) {
                "M" -> male++
                "F" -> female++
            }
        }
    }

    val dailyHistory = dayBuckets.map { (date, count) -> DailyCount(DAY_LABEL.format(date), count) }

    return OverviewStats(
        total = households.size,
        today = todayCount,
        thisMonth = monthCount,
        thisYear = yearCount,
        male = male,
        female = female,
        dailyHistory = dailyHistory,
    )
}
