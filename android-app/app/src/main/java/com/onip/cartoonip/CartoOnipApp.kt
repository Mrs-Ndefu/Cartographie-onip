package com.onip.cartoonip

import android.app.Application
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.AutoSyncManager

class CartoOnipApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        AutoSyncManager.start(this)
    }
}
