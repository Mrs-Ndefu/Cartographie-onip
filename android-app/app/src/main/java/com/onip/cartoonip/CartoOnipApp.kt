package com.onip.cartoonip

import android.app.Application
import com.onip.cartoonip.data.AppContainer

class CartoOnipApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
