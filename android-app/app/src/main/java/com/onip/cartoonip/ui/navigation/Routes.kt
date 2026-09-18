package com.onip.cartoonip.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val HOUSEHOLDS = "households"
    const val HOUSEHOLD_DETAIL = "households/{id}"
    const val AGENTS = "agents"

    fun householdDetail(id: String) = "households/$id"
}
