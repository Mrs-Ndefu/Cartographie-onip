package com.onip.cartoonip.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CAPTURE = "capture"
    const val CAPTURE_EDIT = "capture/{householdId}"
    const val REPERAGE = "reperage"
    const val REPERAGE_FORM = "reperage/form"
    const val REPERAGE_FORM_EDIT = "reperage/form/{siteId}"
    const val JOURNAL = "journal"
    const val OVERVIEW = "overview"
    const val PROFILE = "profile"

    fun captureEdit(id: String) = "capture/$id"
    fun reperageFormEdit(id: String) = "reperage/form/$id"
}
