package com.onip.cartoonip.data.model

data class RegistrationCounts(
    val today: Long,
    val thisMonth: Long,
    val thisYear: Long,
)

data class MonthlyCount(val label: String, val count: Long)

data class DashboardStatsDto(
    val total: Long,
    val countComplet: Long,
    val countBrouillon: Long,
    val countAVerifier: Long,
    val registrationCounts: RegistrationCounts,
    val sexDistribution: Map<String, Long>,
    val monthlyRegistrations: List<MonthlyCount>,
)
