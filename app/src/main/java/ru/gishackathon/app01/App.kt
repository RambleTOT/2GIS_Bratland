package ru.gishackathon.app01

import android.app.Application
import com.google.android.gms.security.ProviderInstaller
import ru.dgis.sdk.DGis
import javax.net.ssl.SSLContext

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DGis.initialize(this)
        ProviderInstaller.installIfNeededAsync(
            this,
            object : ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    // (опционально) прогреть SSLContext, чтобы убедиться, что всё ок
                    try { SSLContext.getInstance("TLSv1.3") } catch (_: Exception) {}
                }
                override fun onProviderInstallFailed(errorCode: Int, intent: android.content.Intent?) {
                    // можно залогировать, но не падать
                }
            }
        )
    }
}
