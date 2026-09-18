package com.onip.cartoonip.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val CAPTURE = "capture"
    const val CAPTURE_EDIT = "capture/{householdId}"
    const val JOURNAL = "journal"
    const val OVERVIEW = "overview"

    fun captureEdit(id: String) = "capture/$id"
}
