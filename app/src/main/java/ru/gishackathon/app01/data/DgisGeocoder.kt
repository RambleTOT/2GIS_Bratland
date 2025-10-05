package ru.gishackathon.app01.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import ru.gishackathon.app01.core.net.Http

class DgisGeocoder(
    private val apiKey: String
) {
    suspend fun geocodeFirst(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // Собираем URL через HttpUrl.Builder — правильное экранирование + SNI по домену
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("catalog.api.2gis.com")
            .addPathSegments("3.0/items/geocode")
            .addQueryParameter("q", query)
            .addQueryParameter("fields", "items.point")
            .addQueryParameter("key", apiKey)
            .build()

        val req = Request.Builder()
            .url(url)
            .get()
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val j = JSONObject(body)
            val items = j.optJSONObject("result")?.optJSONArray("items")
            if (items == null || items.length() == 0) return@withContext null

            val p = items.getJSONObject(0).optJSONObject("point") ?: return@withContext null
            val lat = p.optDouble("lat")
            val lon = p.optDouble("lon")
            lat to lon
        }
    }
}
