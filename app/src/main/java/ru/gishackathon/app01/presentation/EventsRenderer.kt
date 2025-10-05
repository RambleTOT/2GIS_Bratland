package ru.gishackathon.app01.presentation

import android.util.Log
import androidx.annotation.ColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import ru.dgis.sdk.Color
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.Map
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.Polygon
import ru.dgis.sdk.map.PolygonOptions
import ru.dgis.sdk.map.SimpleMapObject
import ru.dgis.sdk.map.lpx

class MapEventsRenderer(
    private val map: Map,
    private val http: OkHttpClient = OkHttpClient(),
    private val endpointUrl: String = DEFAULT_URL
) {
    companion object {
        private const val TAG = "MapEventsRenderer"
        const val DEFAULT_URL = "https://2gis-bratskiy.ru/api/events/list?count=20"
    }

    private val manager = MapObjectManager(map)
    private val drawn: MutableList<SimpleMapObject> = mutableListOf()
    private var visible = false

    suspend fun setEnabled(
        enabled: Boolean,
        @ColorInt fillColorArgb: Int = 0x204FC3F7,
        @ColorInt strokeColorArgb: Int = 0xFF2196F3.toInt(),
        strokeWidthLpx: Int = 2
    ) {
        if (!enabled) { hide(); return }
        if (visible && drawn.isNotEmpty()) return

        val contours = fetchContoursOrEmpty(endpointUrl)
        if (contours.isEmpty()) return

        manager.removeAll()
        drawn.clear()

        val fill = Color(
            android.graphics.Color.red(fillColorArgb),
            android.graphics.Color.green(fillColorArgb),
            android.graphics.Color.blue(fillColorArgb),
            android.graphics.Color.alpha(fillColorArgb)
        )
        val stroke = Color(
            android.graphics.Color.red(strokeColorArgb),
            android.graphics.Color.green(strokeColorArgb),
            android.graphics.Color.blue(strokeColorArgb),
            android.graphics.Color.alpha(strokeColorArgb)
        )

        contours.forEach { ring ->
            if (ring.size < 3) return@forEach
            val poly = Polygon(
                PolygonOptions(contours = listOf(ring))
            ).apply {
                color = fill
                strokeColor = stroke
                strokeWidth = strokeWidthLpx.lpx
            }
            drawn += poly
        }

        if (drawn.isNotEmpty()) {
            manager.addObjects(drawn)
            visible = true
            Log.d(TAG, "Polygons added: ${drawn.size}")
        }
    }

    fun hide() {
        manager.removeAll()
        drawn.clear()
        visible = false
    }


    private suspend fun fetchContoursOrEmpty(url: String): List<List<GeoPoint>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "HTTP ${resp.code} ${resp.message}")
                        return@withContext emptyList()
                    }
                    val body = resp.body?.string().orEmpty()
                    parseContours(body)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetchContours error", t)
                emptyList()
            }
        }


    private fun parseContours(json: String): List<List<GeoPoint>> {
        val arr = try { JSONArray(json) } catch (_: Throwable) { return emptyList() }
        val out = ArrayList<List<GeoPoint>>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val geom = obj.optJSONArray("geom") ?: continue
            val ring = ArrayList<GeoPoint>(geom.length())
            for (j in 0 until geom.length()) {
                val p = geom.optJSONObject(j) ?: continue
                val lat = p.optDouble("lat", Double.NaN)
                val lon = p.optDouble("lon", Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) ring += GeoPoint(lat, lon)
            }
            if (ring.size >= 3) out += ring
        }
        return out
    }
}
