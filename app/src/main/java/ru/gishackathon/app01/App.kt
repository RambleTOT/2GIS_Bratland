package ru.gishackathon.app01

import android.app.Application
import ru.dgis.sdk.DGis

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DGis.initialize(this)
    }
}
