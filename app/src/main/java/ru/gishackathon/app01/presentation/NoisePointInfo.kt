package ru.gishackathon.app01.presentation

import android.util.Log
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
import kotlin.math.abs

data class EventArea(
    val id: Long,
    val name: String?,
    val address: String?,
    val category: String?,
    val comment: String?,
    val start: String?,
    val end: String?,
    val worker: String?,
    val ring: List<GeoPoint>
)

class EventsRenderer(
    private val map: Map,
    private val onPolygonTapped: (EventArea) -> Unit,
    private val http: OkHttpClient = OkHttpClient(),
    private val endpointUrl: String = DEFAULT_URL
) {
    companion object {
        private const val TAG = "EventsRenderer"
        const val DEFAULT_URL = "https://2gis-bratskiy.ru/api/events/list?count=20"
    }

    private val manager = MapObjectManager(map)
    private val drawn: MutableList<SimpleMapObject> = mutableListOf()
    private var visible = false

    private var areas: List<EventArea> = emptyList()

    suspend fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            hide()
            return
        }
        if (visible && drawn.isNotEmpty()) return

        val items = fetchOrEmpty(endpointUrl)
        if (items.isEmpty()) return
        areas = items

        manager.removeAll()
        drawn.clear()

        val fill   = Color(113, 158, 197, 40)   // #719EC5, ~16% прозрачность
        val stroke = Color(113, 158, 197, 255)

        items.forEach { area ->
            if (area.ring.size < 3) return@forEach

            val poly = Polygon(
                PolygonOptions(

                    contours = listOf(area.ring)
                )
            ).apply {
                color = fill
                strokeColor = stroke
                strokeWidth = 2.lpx
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
        areas = emptyList()
        visible = false
    }


    fun onMapTap(point: GeoPoint): Boolean {
        if (!visible || areas.isEmpty()) return false

        for (area in areas) {
            if (pointInPolygon(point, area.ring)) {
                onPolygonTapped(area)
                return true
            }
        }
        return false
    }

    private fun pointInPolygon(p: GeoPoint, ring: List<GeoPoint>): Boolean {
        var inside = false
        var j = ring.lastIndex
        val x = p.longitude.value
        val y = p.latitude.value

        val eps = 1e-12

        for (i in ring.indices) {
            val xi = ring[i].longitude.value
            val yi = ring[i].latitude.value
            val xj = ring[j].longitude.value
            val yj = ring[j].latitude.value

            if (abs(cross(xj, yj, xi, yi, x, y)) < eps &&
                between(x, xj, xi) && between(y, yj, yi)
            ) {
                return true
            }

            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / ((yj - yi) + eps) + xi)

            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun cross(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double): Double {
        return (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1)
    }

    private fun between(v: Double, a: Double, b: Double): Boolean {
        val min = if (a < b) a else b
        val max = if (a > b) a else b
        return v + 1e-12 >= min && v - 1e-12 <= max
    }

    private suspend fun fetchOrEmpty(url: String): List<EventArea> =
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
                    parse(body)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetch events error", t)
                emptyList()
            }
        }

    private fun parse(json: String): List<EventArea> {
        val arr = JSONArray(json)
        val out = ArrayList<EventArea>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue

            val id = o.optLong("id", -1L)
            val name = o.optString("name", null)
            val address = o.optString("address", null)
            val comment = o.optString("comment", null)
            val start = o.optString("start_datetime", null)
            val end = o.optString("end_datetime", null)
            val worker = o.optString("worker", null)
            val category = o.optJSONObject("category")?.optString("name", null)

            val ring = mutableListOf<GeoPoint>()
            val geom: JSONArray? = o.optJSONArray("geom")
            if (geom != null) {
                for (j in 0 until geom.length()) {
                    val p = geom.optJSONObject(j) ?: continue
                    val lat = p.optDouble("lat", Double.NaN)
                    val lon = p.optDouble("lon", Double.NaN)
                    if (!lat.isNaN() && !lon.isNaN()) ring += GeoPoint(lat, lon)
                }
            }

            if (id >= 0 && ring.size >= 3) {
                out += EventArea(id, name, address, category, comment, start, end, worker, ring)
            }
        }
        return out
    }
}
