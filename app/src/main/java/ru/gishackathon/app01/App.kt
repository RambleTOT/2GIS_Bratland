package ru.gishackathon.app01

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ru.dgis.sdk.DGis.initialize(this)
    }
}
