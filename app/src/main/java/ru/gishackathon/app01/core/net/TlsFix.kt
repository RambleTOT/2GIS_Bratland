package ru.gishackathon.app01.core.net

import android.content.Context
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.security.Security

object TlsFix {
    private const val TAG = "TlsFix"

    /** Обновляем провайдер через Google Play Services. */
    fun installGmsProvider(appContext: Context) {
        try {
            ProviderInstaller.installIfNeeded(appContext)
            Log.d(TAG, "ProviderInstaller: OK")
        } catch (e: Exception) {
            Log.w(TAG, "ProviderInstaller failed: ${e.message}")
        }
    }

    /** Пытаемся добавить Conscrypt как fallback (если зависимость подключена). */
    fun tryInstallConscrypt() {
        try {
            val cls = Class.forName("org.conscrypt.Conscrypt")
            val provider = cls.getMethod("newProvider").invoke(null) as java.security.Provider
            // Ставим повыше, но не выше AndroidKeyStore
            Security.insertProviderAt(provider, 1)
            Log.d(TAG, "Conscrypt provider installed")
        } catch (t: Throwable) {
            Log.w(TAG, "Conscrypt not available (skip): ${t.message}")
        }
    }

    /** Строим OkHttp с явной конфигурацией TLS 1.3/1.2. */
    fun buildHttp(): OkHttpClient {
        val modernSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .allEnabledCipherSuites() // оставим выбор системе/провайдеру
            .build()

        return OkHttpClient.Builder()
            .connectionSpecs(listOf(modernSpec, ConnectionSpec.CLEARTEXT))
            .build()
    }
}
