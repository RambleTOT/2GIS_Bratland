package ru.gishackathon.app01.presentation

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import ru.dgis.sdk.Color
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.Map
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.Polygon
import ru.dgis.sdk.map.PolygonOptions
import ru.dgis.sdk.map.SimpleMapObject
import ru.dgis.sdk.map.lpx
import kotlin.math.cos
import kotlin.math.sin

class NoisePointsRenderer(
    private val map: Map,
    private val http: OkHttpClient = OkHttpClient(),
    private val endpointUrl: String = DEFAULT_URL
) {
    companion object {
        private const val TAG = "NoisePoints"
        const val DEFAULT_URL = "https://2gis-bratskiy.ru/api/noise/points"
    }

    private val manager = MapObjectManager(map)
    private val drawn: MutableList<SimpleMapObject> = mutableListOf()
    private var visible = false

    suspend fun setEnabled(enabled: Boolean, count: Int = 20) {
        if (!enabled) { hide(); return }
        if (visible && drawn.isNotEmpty()) return

        val points = fetchPoints(endpointUrl, count)
        if (points.isEmpty()) {
            Log.w(TAG, "Нет точек для отображения")
            return
        }

        manager.removeAll()
        drawn.clear()

        val fill   = Color(83, 178, 175, 96)
        val stroke = Color(83, 178, 175, 255)

        points.forEach { (gp, payload) ->
            val ring = makeCircleRing(center = gp, radiusMeters = 12.0, vertices = 20)

            val poly = Polygon(
                PolygonOptions(contours = listOf(ring))
            ).apply {
                color = fill
                strokeColor = stroke
                strokeWidth = 2.lpx
                userData = payload
            }
            drawn += poly
        }

        if (drawn.isNotEmpty()) {
            manager.addObjects(drawn)
            visible = true
            Log.d(TAG, "На карту добавлено точек (как полигоны): ${drawn.size}")
        }
    }

    fun hide() {
        manager.removeAll()
        drawn.clear()
        visible = false
    }

    private suspend fun fetchPoints(url: String, count: Int): List<Pair<GeoPoint, JSONObject>> =
        withContext(Dispatchers.IO) {
            try {
                val httpUrl = url.toHttpUrlOrNull()!!
                    .newBuilder()
                    .addQueryParameter("count", count.toString())
                    .build()

                val req = Request.Builder()
                    .url(httpUrl)
                    .header("Accept", "application/json")
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "HTTP ${resp.code} ${resp.message} url=${resp.request.url}")
                        return@withContext emptyList()
                    }
                    parsePoints(resp.body?.string().orEmpty())
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetchPoints error: ${t.message}", t)
                emptyList()
            }
        }

    private fun parsePoints(json: String): List<Pair<GeoPoint, JSONObject>> {
        if (json.isBlank()) return emptyList()
        val arr = try { JSONArray(json) } catch (_: Throwable) { return emptyList() }

        val out = ArrayList<Pair<GeoPoint, JSONObject>>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val lat = obj.optDouble("latitude", Double.NaN)
            val lon = obj.optDouble("longitude", Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) out += GeoPoint(lat, lon) to obj
        }
        return out
    }

    private fun makeCircleRing(center: GeoPoint, radiusMeters: Double, vertices: Int): List<GeoPoint> {
        val lat = center.latitude.value
        val lon = center.longitude.value

        val latRad = Math.toRadians(lat)
        val dLat = radiusMeters / 111_320.0
        val dLon = radiusMeters / (111_320.0 * cos(latRad))

        val ring = ArrayList<GeoPoint>(vertices + 1)
        val step = 2.0 * Math.PI / vertices
        var a = 0.0
        repeat(vertices) {
            val y = lat + dLat * sin(a) // lat
            val x = lon + dLon * cos(a) // lon
            ring += GeoPoint(y, x)
            a += step
        }
        ring += ring.first()
        return ring
    }
}
