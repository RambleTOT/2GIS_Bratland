package ramble.sokol.a2gisapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import ramble.sokol.a2gisapp.R
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.geometry.GeoPointWithElevation
import ru.dgis.sdk.map.CameraPosition
import ru.dgis.sdk.map.MapObjectManager
import ru.dgis.sdk.map.MapView
import ru.dgis.sdk.map.Marker
import ru.dgis.sdk.map.MarkerOptions
import ru.dgis.sdk.map.RoadEventSource
import ru.dgis.sdk.map.RouteEditorSource
import ru.dgis.sdk.map.TrafficSource
import ru.dgis.sdk.map.Zoom
import ru.dgis.sdk.routing.RouteEditor
import ru.dgis.sdk.routing.RouteSearchPoint

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {

    private lateinit var mapView: MapView
    private lateinit var bottomBar: BottomNavigationView
    private lateinit var layersSheet: View
    private lateinit var switchTraffic: MaterialSwitch
    private lateinit var switchRoadEvents: MaterialSwitch
    private lateinit var fabRoute: FloatingActionButton

    private var mapObjectManager: MapObjectManager? = null
    private var trafficSource: TrafficSource? = null
    private var roadEventSource: RoadEventSource? = null

    // быстрый способ построить и показать маршрут
    private var routeEditor: RouteEditor? = null
    private var routeEditorSource: RouteEditorSource? = null

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        bottomBar = view.findViewById(R.id.bottomBar)
        layersSheet = view.findViewById(R.id.layersSheet)
        switchTraffic = view.findViewById(R.id.switchTraffic)
        switchRoadEvents = view.findViewById(R.id.switchRoadEvents)
        fabRoute = view.findViewById(R.id.fabRoute)

        // ВАЖНО: MapView подписываем на lifecycle фрагмента
        viewLifecycleOwner.lifecycle.addObserver(mapView)

        ensureLocationPermissions()

        mapView.getMapAsync { map ->
            // 1) Камера на центр города
            map.camera.position = CameraPosition(
                point = GeoPoint(55.751244, 37.618423),
                zoom = Zoom(14.0F)
            )

            // 2) Менеджер динамических объектов (маркеров)
            mapObjectManager = MapObjectManager(map)   // добавляем Marker/Polyline/Polygon сюда
            addSampleMarkers()

            // 3) Переключение слоёв
            switchTraffic.setOnCheckedChangeListener { _, on ->
                if (on) {
                    if (trafficSource == null) trafficSource = TrafficSource(requireContext())
                    map.addSource(trafficSource!!)   // включили слой пробок
                } else {
                    trafficSource?.let { map.removeSource(it) }
                }
            }
            switchRoadEvents.setOnCheckedChangeListener { _, on ->
                if (on) {
                    if (roadEventSource == null) roadEventSource = RoadEventSource(requireContext())
                    map.addSource(roadEventSource!!) // включили дорожные события
                } else {
                    roadEventSource?.let { map.removeSource(it) }
                }
            }

            // 4) Нижний таб-бар
            bottomBar.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.tab_map -> { layersSheet.visibility = View.GONE; true }
                    R.id.tab_layers -> {
                        layersSheet.visibility =
                            if (layersSheet.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        true
                    }
                    R.id.tab_more -> {
                        // открой доп. экран/поиск/настройки
                        true
                    }
                    else -> false
                }
            }

            // 5) Построить маршрут A→B
            fabRoute.setOnClickListener { buildSimpleRoute(map) }
        }
    }

    private fun addSampleMarkers() {
        val mgr = mapObjectManager ?: return
        val points = listOf(
            GeoPointWithElevation(55.752425, 37.613983),
            GeoPointWithElevation(55.760186, 37.618711),
            GeoPointWithElevation(55.747795, 37.620528)
        )
        val markers = points.map { p ->
            Marker(
                MarkerOptions(
                    position = p,
                    icon = TODO()
                )
            )
        }
        mgr.addObjects(markers)       // коллекцией — быстрее
        // MapObjectManager — стандартный способ добавлять маркеры/геометрию. :contentReference[oaicite:5]{index=5}
    }

    private fun buildSimpleRoute(map: Map) {
        if (routeEditor == null) {
            routeEditor = RouteEditor(requireContext())
            routeEditorSource = RouteEditorSource(requireContext(), routeEditor!!)
            map.addSource(routeEditorSource!!)
        }
        routeEditor?.setRouteParams(
            RouteParams(
                startPoint = RouteSearchPoint(GeoPoint(55.759909, 37.618806)),
                finishPoint = RouteSearchPoint(GeoPoint(55.752425, 37.613983))
            )
        )
        // RouteEditor — часть routing API SDK. :contentReference[oaicite:6]{index=6}
    }

    private fun ensureLocationPermissions() {
        val needFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        val needCoarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        if (needFine || needCoarse) {
            requestLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
}