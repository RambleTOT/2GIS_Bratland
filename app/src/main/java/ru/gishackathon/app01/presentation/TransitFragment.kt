// presentation/TransitFragment.kt
package ru.gishackathon.app01.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import ru.dgis.sdk.coordinates.GeoPoint
import ru.gishackathon.app01.R
import ru.gishackathon.app01.databinding.FragmentTransitBinding
import ru.gishackathon.app01.core.net.TlsFix

class TransitFragment : Fragment(R.layout.fragment_transit) {

    private var _binding: FragmentTransitBinding? = null
    private val binding get() = _binding!!

    private val mapHost: MapHostFragment?
        get() = requireActivity().supportFragmentManager.findFragmentById(R.id.mapHost) as? MapHostFragment


    private var startPoint: GeoPoint? = null
    private var destPoint: GeoPoint?  = null

    private var mode: MapHostFragment.TravelMode = MapHostFragment.TravelMode.TRANSIT

    private companion object { const val TAG = "Transit" }

    private val dgisApiKey = "c4ef43c4-dc29-4f58-beb1-482f41e0a34e"


    private val http by lazy { TlsFix.buildHttp() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTransitBinding.bind(view)

        TlsFix.installGmsProvider(requireContext().applicationContext)
        TlsFix.tryInstallConscrypt()

        binding.etFrom.apply {
            setText(getString(R.string.my_location))
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isLongClickable = false
        }

        mapHost?.centerOnMyLocationOnce()
        startPoint = mapHost?.getMyLocation()
        Log.d(TAG, "Initial startPoint: $startPoint")

        binding.btnTransit.setOnClickListener {
            binding.btnTransit.isSelected = true
            binding.btnWalk.isSelected = false
            mode = MapHostFragment.TravelMode.TRANSIT
            tryBuild()
        }
        binding.btnWalk.setOnClickListener {
            binding.btnTransit.isSelected = false
            binding.btnWalk.isSelected = true
            mode = MapHostFragment.TravelMode.WALK
            tryBuild()
        }
        binding.btnTransit.callOnClick()

        binding.etTo.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.etTo.text?.toString()?.trim().orEmpty()
                if (query.isNotBlank()) {
                    v.clearFocus()
                    (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager
                            ).hideSoftInputFromWindow(v.windowToken, 0)
                    geocodeAndRoute(query)
                }
                true
            } else false
        }
    }

    private fun geocodeAndRoute(query: String) {
        if (startPoint == null) {
            mapHost?.centerOnMyLocationOnce()
            startPoint = mapHost?.getMyLocation()
        }
        Log.d(TAG, "Start geocode: '$query', startPoint=$startPoint")

        viewLifecycleOwner.lifecycleScope.launch {
            val dest = geocodeFirstPoint(query)
                ?: run {
                    Toast.makeText(requireContext(), "Адрес не найден. Уточните запрос.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

            destPoint = dest

            startPoint?.let { mapHost?.setStartMarker(it) }
            mapHost?.setDestinationMarker(dest)

            tryBuild()
        }
    }

    private fun tryBuild() {
        val a = startPoint ?: mapHost?.getMyLocation()
        val b = destPoint
        if (a == null) {
            Toast.makeText(requireContext(), "Моё местоположение ещё не получено", Toast.LENGTH_SHORT).show()
            mapHost?.centerOnMyLocationOnce()
            return
        }
        if (b == null) {
            Toast.makeText(requireContext(), "Укажите точку назначения", Toast.LENGTH_SHORT).show()
            return
        }
        mapHost?.setStartMarker(a)
        mapHost?.buildRoute(a, b, mode)
        Log.d(TAG, "buildRoute(a=$a -> b=$b, mode=$mode)")
    }

    private suspend fun geocodeFirstPoint(query: String): GeoPoint? = withContext(Dispatchers.IO) {
        val enc = try { URLEncoder.encode(query, "UTF-8") } catch (_: Throwable) { return@withContext null }
        val url = "https://catalog.api.2gis.com/3.0/items/geocode?q=$enc&region_id=32&fields=items.point&type=building,street,adm_div&key=$dgisApiKey"
        Log.d(TAG, "Geocode URL: $url")

        fun requestOnce(): GeoPoint? {
            val req = Request.Builder()
                .get()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "ru.gishackathon.app01/1.0 (Android)")
                .build()

            return try {
                http.newCall(req).execute().use { resp ->
                    Log.d(TAG, "HTTP ${resp.code} ${resp.message}")
                    val body = resp.body?.string() ?: return null
                    if (!resp.isSuccessful) return null
                    parsePointFromGeocode(body)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "geocode requestOnce failed", t)
                null
            }
        }

        requestOnce() ?: run {
            TlsFix.installGmsProvider(requireContext().applicationContext)
            TlsFix.tryInstallConscrypt()
            requestOnce()
        }
    }

    private fun parsePointFromGeocode(body: String): GeoPoint? {
        return try {
            val root = JSONObject(body)
            val meta = root.optJSONObject("meta")
            if (meta?.optInt("code") != 200) return null
            val items = root.optJSONObject("result")?.optJSONArray("items") ?: return null
            if (items.length() == 0) return null
            val p = items.getJSONObject(0).optJSONObject("point") ?: return null
            val lat = p.optDouble("lat", Double.NaN)
            val lon = p.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) null else GeoPoint(lat, lon)
        } catch (t: Throwable) {
            Log.e(TAG, "parsePointFromGeocode error", t); null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
